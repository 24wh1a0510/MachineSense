package com.machinesense.controller;

import com.machinesense.dto.MachineRequest;
import com.machinesense.entity.Machine;
import com.machinesense.service.MachineService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
public class MachineController {

    private final MachineService machineService;

    public MachineController(MachineService machineService) {
        this.machineService = machineService;
    }

    @GetMapping
    public List<Machine> all() {
        return machineService.findAll();
    }

    @GetMapping("/{id}")
    public Machine byId(@PathVariable Long id) {
        return machineService.findById(id);
    }

    @GetMapping("/code/{code}")
    public Machine byCode(@PathVariable String code) {
        return machineService.findByCode(code);
    }

    @PostMapping("/register")
    public Machine register(@Valid @RequestBody MachineRequest request) {
        return machineService.register(request);
    }

    @DeleteMapping("/{id}/delete")
    public void delete(@PathVariable Long id) {
        machineService.delete(id);
    }
}
