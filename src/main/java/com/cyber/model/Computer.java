package com.cyber.model;

import com.cyber.model.enums.ComputerStatus;
import com.cyber.model.enums.ComputerZone;

import java.math.BigDecimal;

public class Computer {
    private int computerId;
    private String name;
    private ComputerZone zone;
    private String hardwareConfig;
    private ComputerStatus status;
    private BigDecimal pricePerHour;

    public Computer() {}

    public Computer(String name, ComputerZone zone, String hardwareConfig, ComputerStatus status, BigDecimal pricePerHour) {
        this.name = name;
        this.zone = zone;
        this.hardwareConfig = hardwareConfig;
        this.status = status;
        this.pricePerHour = pricePerHour;
    }

    public int getComputerId() {
        return computerId;
    }

    public void setComputerId(int computerId) {
        this.computerId = computerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ComputerZone getZone() {
        return zone;
    }

    public void setZone(ComputerZone zone) {
        this.zone = zone;
    }

    public String getHardwareConfig() {
        return hardwareConfig;
    }

    public void setHardwareConfig(String hardwareConfig) {
        this.hardwareConfig = hardwareConfig;
    }

    public ComputerStatus getStatus() {
        return status;
    }

    public void setStatus(ComputerStatus status) {
        this.status = status;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }
}