package io.castelo.main_server.sensor_data;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorDataRepository extends MongoRepository<SensorData, String> {
    List<SensorData> findByMetaField_EndDeviceMacAndMetaField_SensorNumber(String endDeviceMac, int sensorNumber);
    Optional<SensorData> findFirstByMetaField_EndDeviceMacAndMetaField_SensorNumber(String endDeviceMac, int sensorNumber );
    List<SensorData> findByMetaField_EndDeviceMac(String endDeviceMac);
}
