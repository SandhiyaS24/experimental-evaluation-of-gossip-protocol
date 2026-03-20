package org.ncsu.entity;

import java.time.LocalDateTime;

public class GossipDigest {

    Long generation;
    Integer version;
    LocalDateTime timestamp;
    Boolean isAlive;

    public GossipDigest()  {}

    public GossipDigest(Long generation, Integer version, LocalDateTime timestamp, Boolean isAlive) {
        this.generation = generation;
        this.version = version;
        this.timestamp = timestamp;
        this.isAlive = isAlive;
    }

    public Long getGeneration() {
        return generation;
    }
    public void setGeneration(Long generation) {
        this.generation = generation;
    }

    public int getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getIsAlive() {
        return isAlive;
    }

    public void setIsAlive(Boolean isAlive) {
        this.isAlive = isAlive;
    }


}
