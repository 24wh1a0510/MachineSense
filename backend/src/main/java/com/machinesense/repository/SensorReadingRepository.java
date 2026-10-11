package com.machinesense.repository;

import com.machinesense.entity.SensorReading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {
    List<SensorReading> findByMachineIdOrderByRecordedAtDesc(Long machineId, Pageable pageable);
    SensorReading findFirstByMachineIdOrderByRecordedAtDesc(Long machineId);
}
