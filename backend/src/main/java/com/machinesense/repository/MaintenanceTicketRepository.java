package com.machinesense.repository;

import com.machinesense.entity.MaintenanceTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {
    List<MaintenanceTicket> findByMachineIdOrderByCreatedAtDesc(Long machineId);
    List<MaintenanceTicket> findByStatusOrderByCreatedAtDesc(MaintenanceTicket.Status status);
    List<MaintenanceTicket> findByAssignedToIdOrderByCreatedAtDesc(Long userId);
    long countByStatusIn(List<MaintenanceTicket.Status> statuses);
}
