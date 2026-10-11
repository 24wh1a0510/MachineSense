package com.machinesense.controller;

import com.machinesense.entity.Machine;
import com.machinesense.repository.MachineRepository;
import com.machinesense.service.TelegramAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Demo-only endpoints — useful for live demonstrations.
 * Requires authentication (ADMIN or TECHNICIAN).
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final MachineRepository machineRepository;
    private final TelegramAlertService telegramAlertService;

    public DemoController(MachineRepository machineRepository,
                          TelegramAlertService telegramAlertService) {
        this.machineRepository = machineRepository;
        this.telegramAlertService = telegramAlertService;
    }

    /**
     * POST /api/demo/trigger-alert/{machineId}
     * Sends a simulated Telegram alert for the given machine.
     * Used by the frontend "Simulate Alert" demo button.
     */
    @PostMapping("/trigger-alert/{machineId}")
    public ResponseEntity<Map<String, String>> triggerAlert(@PathVariable Long machineId) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new RuntimeException("Machine not found: " + machineId));

        telegramAlertService.sendTestAlert(machine.getName(), machine.getMachineCode());

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "machine", machine.getMachineCode(),
                "message", "Demo alert sent via Telegram"
        ));
    }
}
