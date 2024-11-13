package io.castelo.main_server.end_device_sensor_data;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

@Service
public class EndDeviceDataService {

    @Value("${spring.data.mongodb.collections.sensor_data}")
    private String sensorDataCollection;

    private final SensorValueRepository sensorValueRepository;
    private final SwitchValueRepository switchValueRepository;

    private final MongoTemplate mongoTemplate;

    @Autowired
    public EndDeviceDataService(SensorValueRepository sensorValueRepository, SwitchValueRepository switchValueRepository, MongoTemplate mongoTemplate) {
        this.sensorValueRepository = sensorValueRepository;
        this.switchValueRepository = switchValueRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public List<SensorValueDBEntry> findSensorValuesByEndDeviceMac(String endDeviceMac) {
        return sensorValueRepository.findByMetaField_EndDeviceMac(endDeviceMac);
    }

    public List<SwitchValueDBEntry> findSwitchValuesByEndDeviceMac(String endDeviceMac) {
        return switchValueRepository.findByEndDeviceMac(endDeviceMac);
    }

    public SensorValueDBEntry getLatestEntry() {
        return sensorValueRepository.findFirstByOrderByTimestampDesc();
    }

    public void saveSensorValues(List<SensorValueDBEntry> sensorValues) {
        sensorValueRepository.saveAll(sensorValues);
    }

    public void saveSwitchValues(List<SwitchValueDBEntry> switchValues) {
        switchValueRepository.saveAll(switchValues);
    }

    public void saveSensorValue(SensorValueRequest sensorValueRequest) {
        mongoTemplate.save(SensorValueDBEntry.fromSensorValueRequest(sensorValueRequest), sensorDataCollection);
    }

    public void saveSwitchValue(SwitchValueDBEntry switchValue) {
        switchValueRepository.save(switchValue);
    }

    public void saveEndDeviceData(EndDeviceData endDeviceData) {
        // Process and save sensor values
        endDeviceData.sensors().forEach(sensorData ->
                sensorData.sensorValues().forEach(sensorValue -> {
                    SensorValueDBEntry entry = new SensorValueDBEntry(null,
                            new MetaField(endDeviceData.endDeviceMac(), sensorData.sensorNumber()),
                            sensorValue.timestamp(),
                            sensorValue.value()
                    );
                    sensorValueRepository.save(entry);
                })
        );

        // Process and save switch values
        endDeviceData.switches().forEach(switchData ->
                switchData.switchValues().forEach(switchValue -> {
                    SwitchValueDBEntry entry = new SwitchValueDBEntry(
                            null,
                            endDeviceData.endDeviceMac(),
                            switchData.switchNumber(),
                            switchValue.timestamp(),
                            switchValue.value()
                    );
                    switchValueRepository.save(entry);
                })
        );
    }

    public EndDeviceData findByEndDeviceMac(String endDeviceMac) {
        List<SensorValueDBEntry> sensorValues = findSensorValuesByEndDeviceMac(endDeviceMac);
        List<SwitchValueDBEntry> switchValues = findSwitchValuesByEndDeviceMac(endDeviceMac);

        Map<Integer, List<SensorValue>> sensors = sensorValues.stream()
                .collect(Collectors.groupingBy(
                        sensorValue -> sensorValue.metaField().sensorNumber(),
                        Collectors.mapping(
                                sensorValue -> new SensorValue(sensorValue.timestamp(), sensorValue.value()),
                                Collectors.toList()
                        )
                ));

        Map<Integer, List<SwitchValue>> switches = switchValues.stream()
                .collect(Collectors.groupingBy(
                        SwitchValueDBEntry::switchNumber,
                        Collectors.mapping(
                                switchValue -> new SwitchValue(switchValue.timestamp(), switchValue.value()),
                                Collectors.toList()
                        )
                ));

        List<Sensor> sensorDataList = sensors.entrySet().stream()
                .map(entry -> new Sensor(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        List<Switch> switchDataList = switches.entrySet().stream()
                .map(entry -> new Switch(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return new EndDeviceData(endDeviceMac, sensorDataList, switchDataList);
    }
}