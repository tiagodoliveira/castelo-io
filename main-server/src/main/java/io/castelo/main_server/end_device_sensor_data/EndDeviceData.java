package io.castelo.main_server.end_device_sensor_data;

import io.castelo.main_server.utils.MACAddressValidator;

import java.util.List;

public record EndDeviceData(
        String endDeviceMac,
        List<Sensor> sensors,
        List<Switch> switches
) {
        public EndDeviceData {
                // Validate and normalize endDeviceMac
                if (endDeviceMac == null || endDeviceMac.isBlank()) {
                        throw new IllegalArgumentException("End Device Mac must not be blank");
                }
                endDeviceMac = MACAddressValidator.normalizeMACAddress(endDeviceMac);
        }
}
