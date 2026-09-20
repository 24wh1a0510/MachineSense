package com.machinesense.service;

import com.machinesense.dto.MachineRequest;
import com.machinesense.entity.Machine;
import com.machinesense.repository.MachineRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MachineService {

    private final MachineRepository machineRepository;

    public MachineService(MachineRepository machineRepository) {
        this.machineRepository = machineRepository;
    }

    public List<Machine> findAll() {
        return machineRepository.findAll();
    }

    public Machine findById(Long id) {
        return machineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Machine not found: " + id));
    }

    public Machine findByCode(String code) {
        return machineRepository.findByMachineCode(code)
                .orElseThrow(() -> new RuntimeException("Machine not found: " + code));
    }

    public Machine register(MachineRequest req) {
        if (machineRepository.existsByMachineCode(req.machineCode)) {
            throw new RuntimeException("Machine code already exists: " + req.machineCode);
        }
        Machine m = new Machine();
        m.setMachineCode(req.machineCode);
        m.setName(req.name);
        m.setMachineType(req.machineType);
        m.setLocation(req.location);
        if (req.installedAt != null && !req.installedAt.isBlank()) {
            m.setInstalledAt(LocalDate.parse(req.installedAt));
        }
        return machineRepository.save(m);
    }

    public void delete(Long id) {
        machineRepository.deleteById(id);
    }
}
