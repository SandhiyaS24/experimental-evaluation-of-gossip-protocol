package main

import (
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"math/rand"
	"net"
	"strings"
	"sync"
	"time"

	"github.com/segmentio/kafka-go"
)

// --- Gossip Structs ---

type NodeState struct {
	ID         string `json:"id"`
	Address    string `json:"address"`
	Data       string `json:"data"`
	Generation int64  `json:"generation"`
	Version    int64  `json:"version"`
	Timestamp  int64  `json:"timestamp"`
}

type GossipMessage struct {
	Type  string               `json:"type"`
	State map[string]NodeState `json:"state"`
}

// --- Kafka Structs ---

type GossipDigest struct {
	Generation int64  `json:"generation"`
	Version    int64  `json:"version"`
	Timestamp  string `json:"timestamp"`
	IsAlive    bool   `json:"isAlive"`
}

type KafkaEvent struct {
	NodeAddress  string       `json:"nodeAddress"`
	Strategy     string       `json:"strategy"`
	GossipDigest GossipDigest `json:"gossipDigest"`
}

// --- Node Definition ---

type Node struct {
	ID          string
	Address     string
	StateMap    map[string]NodeState
	stateLock   sync.RWMutex
	kafkaWriter *kafka.Writer
}

func NewNode(id, address string, initialPeers []string, kafkaBroker, kafkaTopic string) *Node {
	n := &Node{
		ID:       id,
		Address:  address,
		StateMap: make(map[string]NodeState),
	}

	if kafkaBroker != "" && kafkaTopic != "" {
		n.kafkaWriter = &kafka.Writer{
			Addr:     kafka.TCP(kafkaBroker),
			Topic:    kafkaTopic,
			Balancer: &kafka.LeastBytes{},
		}
		fmt.Printf("📡 [%s] Kafka producer initialized (Broker: %s, Topic: %s)\n", n.ID, kafkaBroker, kafkaTopic)
	} else {
		fmt.Printf("⚠️ [%s] No Kafka broker/topic provided. Kafka events will be disabled.\n", n.ID)
	}

	n.StateMap[id] = NodeState{
		ID:         id,
		Address:    address,
		Data:       "",
		Generation: time.Now().UnixNano(),
		Version:    0,
		Timestamp:  time.Now().UnixNano(),
	}

	for _, peerAddr := range initialPeers {
		if peerAddr != "" {
			n.StateMap[peerAddr] = NodeState{Address: peerAddr, Version: -1}
		}
	}

	return n
}

func (n *Node) UpdateOwnData(newData string) {
	n.stateLock.Lock()
	defer n.stateLock.Unlock()

	state := n.StateMap[n.ID]
	state.Data = newData
	state.Version++
	state.Timestamp = time.Now().UnixNano()
	n.StateMap[n.ID] = state

	fmt.Printf("🚀 [%s] Updated own state to: '%s' (v%d)\n", n.ID, newData, state.Version)
}

func (n *Node) StartListening() {
	addr, err := net.ResolveUDPAddr("udp", n.Address)
	if err != nil {
		fmt.Printf("❌ [%s] Failed to resolve listen address: %v\n", n.ID, err)
		return
	}

	conn, err := net.ListenUDP("udp", addr)
	if err != nil {
		fmt.Printf("❌ [%s] Failed to bind UDP listener: %v\n", n.ID, err)
		return
	}
	defer conn.Close()

	fmt.Printf("🎧 [%s] Listening for gossip on %s...\n", n.ID, n.Address)

	// CRITICAL FIX: Max UDP size is 65535. 8192 was too small and truncating payloads!
	buffer := make([]byte, 65535)

	for {
		length, senderAddr, err := conn.ReadFromUDP(buffer)
		if err != nil {
			fmt.Printf("❌ [%s] Network read error: %v\n", n.ID, err)
			continue
		}

		fmt.Printf("📥 [%s] Received %d bytes from %s\n", n.ID, length, senderAddr.String())

		var msg GossipMessage
		if err := json.Unmarshal(buffer[:length], &msg); err != nil {
			fmt.Printf("⚠️ [%s] Failed to parse JSON payload: %v\n", n.ID, err)
			continue
		}

		fmt.Printf("📬 [%s] Processed %s payload containing %d node states\n", n.ID, msg.Type, len(msg.State))

		if msg.Type == "PUSH" {
			n.mergeState(msg.State)
		}
	}
}

func (n *Node) mergeState(incoming map[string]NodeState) {
	n.stateLock.Lock()
	defer n.stateLock.Unlock()

	var newKafkaEvents []KafkaEvent

	for id, incState := range incoming {
		if id == n.ID {
			continue
		}

		localState, exists := n.StateMap[id]
		isUpdated := false

		// DYNAMIC PEER DISCOVERY: If we don't know this node, add it!
		if !exists {
			n.StateMap[id] = incState
			fmt.Printf("🔍 [%s] Discovered NEW peer ID: %s at %s\n", n.ID, id, incState.Address)
			isUpdated = true

			// Clean up temporary "address-only" keys if they exist
			if _, tempExists := n.StateMap[incState.Address]; tempExists && id != incState.Address {
				delete(n.StateMap, incState.Address)
			}
		} else {
			isNewerGeneration := incState.Generation > localState.Generation
			isNewerVersion := (incState.Generation == localState.Generation) && (incState.Version > localState.Version)

			if isNewerGeneration || isNewerVersion {
				n.StateMap[id] = incState
				fmt.Printf("🔄 [%s] Updated state for %s: '%s' (v%d)\n", n.ID, id, incState.Data, incState.Version)
				isUpdated = true

				if _, tempExists := n.StateMap[incState.Address]; tempExists && id != incState.Address {
					delete(n.StateMap, incState.Address)
				}
			}
		}

		if isUpdated {
			if n.kafkaWriter != nil {
				ts := time.Unix(0, incState.Timestamp).UTC().Format("2006-01-02T15:04:05.000Z")
				newKafkaEvents = append(newKafkaEvents, KafkaEvent{
					NodeAddress: incState.Address,
					Strategy:    "PUSH",
					GossipDigest: GossipDigest{
						Generation: incState.Generation,
						Version:    incState.Version,
						Timestamp:  ts,
						IsAlive:    true,
					},
				})
			} else {
				// We update local state, but alert that Kafka isn't configured
				fmt.Printf("⚠️ [%s] State updated for %s, but Kafka is disabled. Event dropped.\n", n.ID, id)
			}
		}
	}

	if len(newKafkaEvents) > 0 && n.kafkaWriter != nil {
		go func(events []KafkaEvent) {
			payload, _ := json.Marshal(events)

			// CRITICAL FIX: Add a timeout so it doesn't block forever if Kafka is down
			ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
			defer cancel()

			err := n.kafkaWriter.WriteMessages(ctx, kafka.Message{
				Value: payload,
			})

			if err != nil {
				fmt.Printf("⚠️ [%s] Failed to write to Kafka: %v\n", n.ID, err)
			} else {
				fmt.Printf("📨 [%s] Published %d update(s) to Kafka topic.\n", n.ID, len(events))
			}
		}(newKafkaEvents)
	}
}

func (n *Node) StartGossiping(interval time.Duration) {
	rand.Seed(time.Now().UnixNano())

	for {
		time.Sleep(interval)

		n.stateLock.RLock()

		// Create a clean map to send that excludes placeholders
		cleanStateToSend := make(map[string]NodeState)
		for k, v := range n.StateMap {
			if v.Version >= 0 && v.ID != "" { // Only share real, booted nodes
				cleanStateToSend[k] = v
			}
		}

		msg := GossipMessage{
			Type:  "PUSH",
			State: cleanStateToSend,
		}
		payload, _ := json.Marshal(msg)

		var peerAddrs []string
		for id, state := range n.StateMap {
			if id != n.ID && state.Address != "" {
				peerAddrs = append(peerAddrs, state.Address)
			}
		}
		n.stateLock.RUnlock()

		if len(peerAddrs) == 0 {
			continue
		}

		rand.Shuffle(len(peerAddrs), func(i, j int) {
			peerAddrs[i], peerAddrs[j] = peerAddrs[j], peerAddrs[i]
		})

		numToSelect := 2
		if len(peerAddrs) < 2 {
			numToSelect = len(peerAddrs)
		}

		for _, targetAddr := range peerAddrs[:numToSelect] {
			fmt.Printf("🟢 [%s] Pushing state to %s\n", n.ID, targetAddr)
			n.sendUDP(targetAddr, payload)
		}
	}
}

func (n *Node) sendUDP(targetAddress string, payload []byte) {
	addr, err := net.ResolveUDPAddr("udp", targetAddress)
	if err != nil {
		fmt.Printf("❌ [%s] Invalid peer address '%s': %v\n", n.ID, targetAddress, err)
		return
	}

	conn, err := net.DialUDP("udp", nil, addr)
	if err != nil {
		fmt.Printf("❌ [%s] Failed to connect to %s: %v\n", n.ID, targetAddress, err)
		return
	}
	defer conn.Close()

	_, err = conn.Write(payload)
	if err != nil {
		fmt.Printf("❌ [%s] Failed to send packet to %s: %v\n", n.ID, targetAddress, err)
	}
}

func main() {
	idFlag := flag.String("id", "Node-1", "Identifier for the node")
	addrFlag := flag.String("addr", "127.0.0.1:8001", "Address for this node to listen on")
	peersFlag := flag.String("peers", "", "Comma-separated list of peer addresses")
	injectFlag := flag.String("inject", "", "Message to inject to start the gossip")

	kafkaBrokerFlag := flag.String("kafka-broker", "", "Kafka broker address (e.g., localhost:9092)")
	kafkaTopicFlag := flag.String("kafka-topic", "gossip-events", "Kafka topic to publish to")

	flag.Parse()

	var initialPeers []string
	if *peersFlag != "" {
		initialPeers = strings.Split(*peersFlag, ",")
	}

	node := NewNode(*idFlag, *addrFlag, initialPeers, *kafkaBrokerFlag, *kafkaTopicFlag)

	go node.StartListening()
	go node.StartGossiping(3 * time.Second)

	if *injectFlag != "" {
		time.Sleep(1 * time.Second)
		node.UpdateOwnData(*injectFlag)
	}

	select {}
}
