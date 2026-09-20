package com.machinesense.repository;

import com.machinesense.entity.Prediction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    List<Prediction> findByMachineIdOrderByCreatedAtDesc(Long machineId, Pageable pageable);
    Prediction findFirstByMachineIdOrderByCreatedAtDesc(Long machineId);
}
