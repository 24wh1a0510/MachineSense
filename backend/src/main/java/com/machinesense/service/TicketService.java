package com.machinesense.service;

import com.machinesense.dto.TicketUpdateRequest;
import com.machinesense.entity.MaintenanceHistory;
import com.machinesense.entity.MaintenanceTicket;
import com.machinesense.entity.User;
import com.machinesense.repository.MaintenanceHistoryRepository;
import com.machinesense.repository.MaintenanceTicketRepository;
import com.machinesense.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    private final MaintenanceTicketRepository ticketRepository;
    private final MaintenanceHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public TicketService(MaintenanceTicketRepository ticketRepository,
                          MaintenanceHistoryRepository historyRepository,
                          UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    public List<MaintenanceTicket> findAll() {
        return ticketRepository.findAll();
    }

    public List<MaintenanceTicket> findByMachine(Long machineId) {
        return ticketRepository.findByMachineIdOrderByCreatedAtDesc(machineId);
    }

    public List<MaintenanceTicket> findByStatus(MaintenanceTicket.Status status) {
        return ticketRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<MaintenanceTicket> findByTechnician(Long userId) {
        return ticketRepository.findByAssignedToIdOrderByCreatedAtDesc(userId);
    }

    public MaintenanceTicket update(Long ticketId, TicketUpdateRequest req, String actingUsername) {
        MaintenanceTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        User actor = userRepository.findByUsername(actingUsername).orElse(null);

        if (req.assignedToId != null) {
            User assignee = userRepository.findById(req.assignedToId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + req.assignedToId));
            ticket.setAssignedTo(assignee);
            if (ticket.getStatus() == MaintenanceTicket.Status.OPEN) {
                ticket.setStatus(MaintenanceTicket.Status.ASSIGNED);
            }
            logHistory(ticket, "ASSIGNED", "Assigned to " + assignee.getUsername(), actor);
        }

        if (req.status != null) {
            MaintenanceTicket.Status newStatus = MaintenanceTicket.Status.valueOf(req.status);
            ticket.setStatus(newStatus);
            logHistory(ticket, "STATUS_CHANGE", "Status changed to " + newStatus, actor);
        }

        if (req.notes != null && !req.notes.isBlank()) {
            logHistory(ticket, "NOTE", req.notes, actor);
        }

        return ticketRepository.save(ticket);
    }

    private void logHistory(MaintenanceTicket ticket, String action, String notes, User actor) {
        MaintenanceHistory history = new MaintenanceHistory();
        history.setTicket(ticket);
        history.setAction(action);
        history.setNotes(notes);
        history.setPerformedBy(actor);
        historyRepository.save(history);
    }

    public List<MaintenanceHistory> history(Long ticketId) {
        return historyRepository.findByTicketIdOrderByPerformedAtDesc(ticketId);
    }
}
