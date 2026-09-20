package com.machinesense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SensorIngestRequest {
    @NotBlank
    public String machineCode;
    public String machineType;
    @NotNull
    public Double airTemperature;
    @NotNull
    public Double processTemperature;
    @NotNull
    public Double rotationalSpeed;
    @NotNull
    public Double torque;
    @NotNull
    public Double toolWear;
    public Double vibration;
    public Double current;
    public Double pressure;
}
