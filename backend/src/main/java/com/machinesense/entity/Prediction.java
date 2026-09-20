package com.machinesense.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "predictions")
public class Prediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_reading_id")
    private SensorReading sensorReading;

    private Double failureProbability;
    private Integer healthScore;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    private String predictedFailureType;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum RiskLevel { HEALTHY, WARNING, CRITICAL }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Machine getMachine() { return machine; }
    public void setMachine(Machine machine) { this.machine = machine; }
    public SensorReading getSensorReading() { return sensorReading; }
    public void setSensorReading(SensorReading sensorReading) { this.sensorReading = sensorReading; }
    public Double getFailureProbability() { return failureProbability; }
    public void setFailureProbability(Double v) { this.failureProbability = v; }
    public Integer getHealthScore() { return healthScore; }
    public void setHealthScore(Integer v) { this.healthScore = v; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel v) { this.riskLevel = v; }
    public String getPredictedFailureType() { return predictedFailureType; }
    public void setPredictedFailureType(String v) { this.predictedFailureType = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
