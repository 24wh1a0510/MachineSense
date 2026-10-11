package com.machinesense.controller;

import com.machinesense.dto.TicketUpdateRequest;
import com.machinesense.entity.MaintenanceHistory;
import com.machinesense.entity.MaintenanceTicket;
import com.machinesense.service.TicketService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<MaintenanceTicket> all() {
        return ticketService.findAll();
    }

    @GetMapping("/machine/{machineId}")
    public List<MaintenanceTicket> byMachine(@PathVariable Long machineId) {
        return ticketService.findByMachine(machineId);
    }

    @GetMapping("/status/{status}")
    public List<MaintenanceTicket> byStatus(@PathVariable String status) {
        return ticketService.findByStatus(MaintenanceTicket.Status.valueOf(status.toUpperCase()));
    }

    @GetMapping("/technician/{userId}")
    public List<MaintenanceTicket> byTechnician(@PathVariable Long userId) {
        return ticketService.findByTechnician(userId);
    }

    @PatchMapping("/{ticketId}")
    public MaintenanceTicket update(@PathVariable Long ticketId,
                                     @RequestBody TicketUpdateRequest request,
                                     @AuthenticationPrincipal UserDetails principal) {
        String username = principal != null ? principal.getUsername() : "system";
        return ticketService.update(ticketId, request, username);
    }

    @GetMapping("/{ticketId}/history")
    public List<MaintenanceHistory> history(@PathVariable Long ticketId) {
        return ticketService.history(ticketId);
    }
}
