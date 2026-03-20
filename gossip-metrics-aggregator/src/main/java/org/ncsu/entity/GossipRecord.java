package org.ncsu.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
public class GossipRecord extends PanacheEntity {

    String nodeAddress;
    Long generation;
    Integer version;
    LocalDateTime timestamp;
    Boolean isAlive;

    public GossipRecord() {}

    public  GossipRecord(String nodeAddress, GossipDigest gossipDigest) {
        this.nodeAddress = nodeAddress;
        this.generation = gossipDigest.getGeneration();
        this.version = gossipDigest.getVersion();
        this.timestamp = gossipDigest.getTimestamp();
        this.isAlive = gossipDigest.getIsAlive();
    }
}
