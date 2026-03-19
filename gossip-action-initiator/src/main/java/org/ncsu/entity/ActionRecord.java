package org.ncsu.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
public class ActionRecord extends PanacheEntity {

    String nodeAddress;
    Integer managerPort;
    Integer actionPort;
    String peer;
    String kafkaBroker;
    String topic;
    Integer status;
    LocalDateTime timestamp;
    Action action;

    public ActionRecord() {}

    public ActionRecord(String nodeAddress, Integer managerPort) {
        this.nodeAddress = nodeAddress;
        this.managerPort = managerPort;
    }

    public ActionRecord(String nodeAddress, Integer managerPort, Integer actionPort, Integer status, LocalDateTime timestamp, Action action) {
        this.nodeAddress = nodeAddress;
        this.managerPort = managerPort;
        this.actionPort = actionPort;
        this.status = status;
        this.timestamp = timestamp;
        this.action = action;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }
    public void setNodeAddress(String nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    public Integer getManagerPort() {
        return managerPort;
    }

    public void setManagerPort(Integer managerPort) {
        this.managerPort = managerPort;
    }

    public Integer getActionPort() {
        return actionPort;
    }

    public void setActionPort(Integer actionPort) {
        this.actionPort = actionPort;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getPeer() {
        return peer;
    }

    public void setPeer(String peer) {
        this.peer = peer;
    }

    public String getKafkaBroker() {
        return kafkaBroker;
    }

    public void setKafkaBroker(String kafkaBroker) {
        this.kafkaBroker = kafkaBroker;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
