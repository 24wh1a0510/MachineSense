package com.machinesense.dto;

public class TicketUpdateRequest {
    public String status;      // OPEN / ASSIGNED / IN_PROGRESS / COMPLETED
    public Long assignedToId;
    public String notes;
}
