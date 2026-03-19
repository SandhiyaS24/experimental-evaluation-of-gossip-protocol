package org.ncsu.service;

import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped // CRITICAL: Makes this a singleton service
public class GossipProcessManager {

    private final Map<Integer, Process> activeNodes = new ConcurrentHashMap<>();

    public void startNode(Integer port, String peer, String kafkaTopic, String kafkaBroker) throws Exception {
        if (activeNodes.containsKey(port) && activeNodes.get(port).isAlive()) {
            throw new IllegalStateException("Node already running on port " + port);
        }

        String peerAddress = "";
        if (peer != null && !peer.isBlank()) {
            peerAddress = peer.contains(":") ? peer : "127.0.0.1:" + peer;
        }

        List<String> command = new ArrayList<>();
        command.add("go");
        command.add("run");
        command.add("main.go");
        command.add("-id");
        command.add("Node-" + port);
        command.add("-addr");
        command.add("0.0.0.0:" + port);

        if (!peerAddress.isEmpty()) {
            command.add("-peers");
            command.add(peerAddress);
        }
        if (kafkaBroker != null && !kafkaBroker.isBlank()) {
            command.add("-kafka-broker");
            command.add(kafkaBroker);
            if (kafkaTopic != null && !kafkaTopic.isBlank()) {
                command.add("-kafka-topic");
                command.add(kafkaTopic);
            }
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        // pb.directory(new File("/path/to/your/go/project"));
        pb.inheritIO();

        Process process = pb.start();

        if (!process.isAlive() && process.exitValue() != 0) {
            throw new RuntimeException("Go process terminated immediately upon starting.");
        }

        activeNodes.put(port, process);
    }

    public boolean killNode(Integer port) {
        Process process = activeNodes.remove(port);
        if (process != null && process.isAlive()) {
            process.destroy(); // Graceful shutdown
            return true;
        }
        return false;
    }

    /**
     * Automatically cleans up orphaned processes when Quarkus shuts down.
     */
    void onStop(@Observes ShutdownEvent ev) {
        if (activeNodes.isEmpty()) return;

        System.out.println("🛑 Quarkus is shutting down. Terminating " + activeNodes.size() + " active gossip nodes...");

        for (Map.Entry<Integer, Process> entry : activeNodes.entrySet()) {
            Process process = entry.getValue();
            if (process != null && process.isAlive()) {
                System.out.println("Killing Node on port: " + entry.getKey());
                process.destroyForcibly();
            }
        }
        activeNodes.clear();
        System.out.println("✅ All gossip nodes terminated.");
    }
}
