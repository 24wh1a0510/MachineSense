package com.machinesense.service;

import com.machinesense.entity.Machine;
import com.machinesense.entity.MaintenanceTicket;
import com.machinesense.entity.Prediction;
import com.machinesense.repository.MachineRepository;
import com.machinesense.repository.MaintenanceTicketRepository;
import com.machinesense.repository.PredictionRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final MachineRepository machineRepository;
    private final PredictionRepository predictionRepository;
    private final MaintenanceTicketRepository ticketRepository;

    public DashboardService(MachineRepository machineRepository,
                             PredictionRepository predictionRepository,
                             MaintenanceTicketRepository ticketRepository) {
        this.machineRepository = machineRepository;
        this.predictionRepository = predictionRepository;
        this.ticketRepository = ticketRepository;
    }

    public Map<String, Object> summary() {
        List<Machine> machines = machineRepository.findAll();

        int healthy = 0, warning = 0, critical = 0, unknown = 0;
        long totalScore = 0;
        int scored = 0;

        for (Machine m : machines) {
            Prediction latest = predictionRepository.findFirstByMachineIdOrderByCreatedAtDesc(m.getId());
            if (latest == null) {
                unknown++;
                continue;
            }
            scored++;
            totalScore += latest.getHealthScore();
            switch (latest.getRiskLevel()) {
                case HEALTHY -> healthy++;
                case WARNING -> warning++;
                case CRITICAL -> critical++;
            }
        }

        long activeTickets = ticketRepository.countByStatusIn(
                List.of(MaintenanceTicket.Status.OPEN, MaintenanceTicket.Status.ASSIGNED,
                        MaintenanceTicket.Status.IN_PROGRESS));

        Map<String, Object> result = new HashMap<>();
        result.put("totalMachines", machines.size());
        result.put("healthyMachines", healthy);
        result.put("warningMachines", warning);
        result.put("criticalMachines", critical);
        result.put("unknownMachines", unknown);
        result.put("activeMaintenanceTickets", activeTickets);
        result.put("overallFactoryHealth", scored > 0 ? Math.round((double) totalScore / scored) : 100);
        return result;
    }
}
