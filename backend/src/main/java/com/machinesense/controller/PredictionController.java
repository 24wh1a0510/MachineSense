package com.machinesense.controller;

import com.machinesense.entity.Prediction;
import com.machinesense.repository.PredictionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionRepository predictionRepository;

    public PredictionController(PredictionRepository predictionRepository) {
        this.predictionRepository = predictionRepository;
    }

    @GetMapping("/machine/{machineId}")
    public List<Prediction> history(@PathVariable Long machineId,
                                     @RequestParam(defaultValue = "50") int limit) {
        return predictionRepository.findByMachineIdOrderByCreatedAtDesc(machineId, PageRequest.of(0, limit));
    }

    @GetMapping("/machine/{machineId}/latest")
    public Prediction latest(@PathVariable Long machineId) {
        return predictionRepository.findFirstByMachineIdOrderByCreatedAtDesc(machineId);
    }
}
