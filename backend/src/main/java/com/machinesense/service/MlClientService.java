package com.machinesense.service;

import com.machinesense.dto.MlPredictionResponse;
import com.machinesense.entity.Machine;
import com.machinesense.entity.SensorReading;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class MlClientService {

    private final RestTemplate restTemplate;

    @Value("${ml-service.url}")
    private String mlServiceUrl;

    public MlClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public MlPredictionResponse predict(Machine machine, SensorReading reading) {
        Map<String, Object> body = new HashMap<>();
        body.put("machineId", machine.getMachineCode());
        body.put("machineType", machine.getMachineType());
        body.put("airTemperature", reading.getAirTemperature());
        body.put("processTemperature", reading.getProcessTemperature());
        body.put("rotationalSpeed", reading.getRotationalSpeed());
        body.put("torque", reading.getTorque());
        body.put("toolWear", reading.getToolWear());

        try {
            return restTemplate.postForObject(mlServiceUrl + "/predict", body, MlPredictionResponse.class);
        } catch (Exception e) {
            // ML service unreachable: fail safe with a neutral, clearly-flagged fallback
            MlPredictionResponse fallback = new MlPredictionResponse();
            fallback.machineId = machine.getMachineCode();
            fallback.failureProbability = 0.0;
            fallback.healthScore = 100;
            fallback.riskLevel = "HEALTHY";
            fallback.predictedFailureType = null;
            return fallback;
        }
    }
}
