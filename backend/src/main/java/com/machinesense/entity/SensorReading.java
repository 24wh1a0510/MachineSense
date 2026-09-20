package com.machinesense.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_readings")
public class SensorReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    private Double airTemperature;
    private Double processTemperature;
    private Double rotationalSpeed;
    private Double torque;
    private Double toolWear;
    private Double vibration;
    private Double current;
    private Double pressure;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Machine getMachine() { return machine; }
    public void setMachine(Machine machine) { this.machine = machine; }
    public Double getAirTemperature() { return airTemperature; }
    public void setAirTemperature(Double v) { this.airTemperature = v; }
    public Double getProcessTemperature() { return processTemperature; }
    public void setProcessTemperature(Double v) { this.processTemperature = v; }
    public Double getRotationalSpeed() { return rotationalSpeed; }
    public void setRotationalSpeed(Double v) { this.rotationalSpeed = v; }
    public Double getTorque() { return torque; }
    public void setTorque(Double v) { this.torque = v; }
    public Double getToolWear() { return toolWear; }
    public void setToolWear(Double v) { this.toolWear = v; }
    public Double getVibration() { return vibration; }
    public void setVibration(Double v) { this.vibration = v; }
    public Double getCurrent() { return current; }
    public void setCurrent(Double v) { this.current = v; }
    public Double getPressure() { return pressure; }
    public void setPressure(Double v) { this.pressure = v; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
