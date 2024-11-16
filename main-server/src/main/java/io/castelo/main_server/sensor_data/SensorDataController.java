package io.castelo.main_server.sensor_data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/end-device/sensor-data")
public class SensorDataController {

    private final SensorDataService sensorDataService;

    @Autowired
    public SensorDataController(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    @PostMapping("/add-sensor-value")
    @ResponseStatus(HttpStatus.CREATED)
    public void addSensorValue(@RequestBody SensorData sensorData) {
        sensorDataService.insertSensorData(sensorData);
    }

    @PostMapping("/add-sensor-values")
    @ResponseStatus(HttpStatus.CREATED)
    public void addSensorValues(@RequestBody List<SensorData> sensorDataList) {
        sensorDataService.insertSensorData(sensorDataList);
    }

    @GetMapping("/{endDeviceMac}/get-all-sensor-data")
    @ResponseStatus(HttpStatus.OK)
    public List<SensorData> getAllSensorDataByEndDeviceMac(@PathVariable String endDeviceMac) {
        return sensorDataService.findAllSensorDataByEndDeviceMac(endDeviceMac);
    }

    @GetMapping("/{endDeviceMac}")
    @ResponseStatus(HttpStatus.OK)
    public List<SensorData> getSensorDataByEndDeviceMac(
            @PathVariable String endDeviceMac,
            @RequestParam(required = false, name = "maxEntries" , defaultValue = "10") Integer maxEntries) {
        return sensorDataService.findSensorDataByEndDeviceMac(endDeviceMac, maxEntries);
    }

    @GetMapping("/{endDeviceMac}/{sensorNumber}/latest")
    @ResponseStatus(HttpStatus.OK)
    public SensorData getLatestSensorValue(@PathVariable String endDeviceMac, @PathVariable int sensorNumber) {
        return sensorDataService.getLatestSensorValue(endDeviceMac, sensorNumber);
    }

    @GetMapping("/{endDeviceMac}/{sensorNumber}")
    @ResponseStatus(HttpStatus.OK)
    public List<SensorData> getSensorDataByEndDeviceMacAndSensorNumber(
            @PathVariable String endDeviceMac,
            @PathVariable int sensorNumber,
            @RequestParam(required = false, name = "maxEntries", defaultValue = "10") Integer maxEntries)
    {
        return sensorDataService.findSensorDataByEndDeviceMacAndSensorNumber(endDeviceMac, sensorNumber, maxEntries);
    }

    @GetMapping("/{endDeviceMac}/{sensorNumber}/get-all-sensor-data")
    @ResponseStatus(HttpStatus.OK)
    public List<SensorData> getAllSensorDataByEndDeviceMacAndSensorNumber(
            @PathVariable String endDeviceMac, @PathVariable int sensorNumber) {
        return sensorDataService.findAllSensorDataByEndDeviceMacAndSensorNumber(endDeviceMac, sensorNumber);
    }
}