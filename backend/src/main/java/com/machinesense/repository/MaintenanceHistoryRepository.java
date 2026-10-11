package com.machinesense.repository;

import com.machinesense.entity.MaintenanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaintenanceHistoryRepository extends JpaRepository<MaintenanceHistory, Long> {
    List<MaintenanceHistory> findByTicketIdOrderByPerformedAtDesc(Long ticketId);
}
