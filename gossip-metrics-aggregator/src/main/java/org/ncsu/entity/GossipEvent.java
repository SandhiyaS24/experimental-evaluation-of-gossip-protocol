package org.ncsu.entity;

public class GossipEvent {

    String nodeAddress;
    String strategy;
    GossipDigest gossipDigest;

    public GossipEvent() {
    }

    public GossipEvent(String nodeAddress, GossipDigest gossipDigest) {
        this.nodeAddress = nodeAddress;
        this.gossipDigest = gossipDigest;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }

    public void setNodeAddress(String nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public GossipDigest getGossipDigest() {
        return gossipDigest;
    }

    public void setGossipDigest(GossipDigest gossipDigest) {
        this.gossipDigest = gossipDigest;
    }
}
