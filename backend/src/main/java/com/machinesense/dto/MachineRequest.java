package com.machinesense.dto;

import jakarta.validation.constraints.NotBlank;

public class MachineRequest {
    @NotBlank
    public String machineCode;
    @NotBlank
    public String name;
    @NotBlank
    public String machineType;
    public String location;
    public String installedAt; // yyyy-MM-dd
}
