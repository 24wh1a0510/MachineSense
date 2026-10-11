package com.machinesense.controller;

import com.machinesense.dto.SensorIngestRequest;
import com.machinesense.entity.Prediction;
import com.machinesense.entity.SensorReading;
import com.machinesense.repository.SensorReadingRepository;
import com.machinesense.service.SensorIngestionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private final SensorIngestionService ingestionService;
    private final SensorReadingRepository sensorReadingRepository;

    public SensorController(SensorIngestionService ingestionService,
                             SensorReadingRepository sensorReadingRepository) {
        this.ingestionService = ingestionService;
        this.sensorReadingRepository = sensorReadingRepository;
    }

    /** Called by the sensor simulator (or real PLC/gateway) - no auth required so the demo works out of the box. */
    @PostMapping("/ingest")
    public Prediction ingest(@Valid @RequestBody SensorIngestRequest request) {
        return ingestionService.ingest(request);
    }

    @GetMapping("/machine/{machineId}")
    public List<SensorReading> history(@PathVariable Long machineId,
                                        @RequestParam(defaultValue = "50") int limit) {
        return sensorReadingRepository.findByMachineIdOrderByRecordedAtDesc(machineId, PageRequest.of(0, limit));
    }
}
