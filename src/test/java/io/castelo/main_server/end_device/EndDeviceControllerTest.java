package io.castelo.main_server.end_device;

import io.castelo.main_server.end_device_model.EndDeviceModel;
import io.castelo.main_server.gateway.Gateway;
import io.castelo.main_server.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class EndDeviceControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @Autowired
    TestRestTemplate restTemplate;

    private static final String VALID_USER_ID = "cfd3ce69-1f3d-43cc-87f1-a686373a25ca";
    private static final String INVALID_USER_ID = "3af90dd5-f14f-4326-bdb9-b92db3175b36"; // Random UUID
    private static final String VALID_GATEWAY_MAC = "00:11:22:33:44:55";
    private static final String INVALID_GATEWAY_MAC = "00:00:00:00:00:01";
    private static final String VALID_END_DEVICE_MAC = "AA:BB:CC:DD:EE:FF";
    private static final String INVALID_END_DEVICE_MAC = "AA:BB:CC:DD:AA:AA";


    @Test
    void getAllEndDevices() {
        ResponseEntity<EndDevice[]> response = restTemplate.getForEntity("/end-devices", EndDevice[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        EndDevice[] endDevices = response.getBody();
        assertNotNull(endDevices);
        assertEquals(2, endDevices.length);
    }

    @Test
    void whenGettingEndDeviceThatExistsShouldReturnEndDevice() {
        ResponseEntity<EndDevice> response = restTemplate.getForEntity("/end-devices/" + VALID_END_DEVICE_MAC, EndDevice.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        EndDevice endDevice = response.getBody();
        assertNotNull(endDevice);
        assertEquals("Device 1", endDevice.getEndDeviceName()); // Assuming data loaded from JSON
    }

    @Test
    void whenGettingEndDeviceThatDoesNotExistShouldReturnNotFound() {
        ResponseEntity<EndDevice> response = restTemplate.getForEntity("/end-devices/" + INVALID_END_DEVICE_MAC, EndDevice.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldCreateValidEndDevice() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Gateway validGateway = new Gateway(VALID_GATEWAY_MAC, new User(VALID_USER_ID, null), "1234", "Gateway");

        EndDevice newEndDevice = new EndDevice(
                "AA:BB:CC:DD:EE:FA",
                "192.168.0.1",
                new EndDeviceModel(1, "v1.2.3"),
                "New Device",
                false,
                new User(VALID_USER_ID, null),
                validGateway,
                "1.0.0",
                WorkingModes.MANUAL
        );
        HttpEntity<EndDevice> request = new HttpEntity<>(newEndDevice, headers);

        ResponseEntity<EndDevice> endDeviceResponseEntity = restTemplate.exchange("/end-devices", HttpMethod.POST, request, EndDevice.class);
        assertEquals(endDeviceResponseEntity.getStatusCode(), HttpStatus.CREATED);
        assertNotNull(endDeviceResponseEntity.getBody());
        assertEquals(Objects.requireNonNull(endDeviceResponseEntity.getBody()).getEndDeviceName(), "New Device");
    }

    @Test
    void shouldReturnNotFoundIfInvalidUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        EndDevice newEndDevice = new EndDevice(
                "AA:BB:CC:DD:EE:FA",
                "192.168.0.1",
                new EndDeviceModel(1, "v1.2.3"),
                "New Device",
                false,
                new User(INVALID_USER_ID, null), // Random generated UUID
                null,
                "1.0.0",
                WorkingModes.MANUAL
        );
        HttpEntity<EndDevice> request = new HttpEntity<>(newEndDevice, headers);

        ResponseEntity<EndDevice> endDeviceResponseEntity = restTemplate.exchange("/end-devices", HttpMethod.POST, request, EndDevice.class);
        assertEquals(endDeviceResponseEntity.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnNotFoundIfInvalidGateway() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Gateway invalidGateway = new Gateway(INVALID_GATEWAY_MAC, new User(VALID_USER_ID, null), "1234", "Gateway");

        EndDevice newEndDevice = new EndDevice(
                "AA:BB:CC:DD:EE:FA",
                "192.168.0.1",
                new EndDeviceModel(1, "v1.2.3"),
                "New Device",
                false,
                new User(VALID_USER_ID, null),
                invalidGateway,
                "1.0.0",
                WorkingModes.MANUAL
        );
        HttpEntity<EndDevice> request = new HttpEntity<>(newEndDevice, headers);

        ResponseEntity<EndDevice> endDeviceResponseEntity = restTemplate.exchange("/end-devices", HttpMethod.POST, request, EndDevice.class);
        assertEquals(endDeviceResponseEntity.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    void updateEndDevice() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        EndDeviceDTO updatedEndDeviceData = new EndDeviceDTO(
                "192.168.0.199", // Updated IP Address
                new EndDeviceModel(1, "v1.2.4"), // Updated Model
                "Updated Device Name", // Updated Name
                true, // Updated Debug Mode
                new Gateway(VALID_GATEWAY_MAC, null, "192.168.1.1", "UpdatedGatewayName"), // Updated Gateway
                "1.2.3", // Updated Firmware
                WorkingModes.MANUAL // Updated Working Mode
        );

        HttpEntity<EndDeviceDTO> request = new HttpEntity<>(updatedEndDeviceData, headers);

        ResponseEntity<EndDevice> response = restTemplate.exchange("/end-devices/" + VALID_END_DEVICE_MAC, HttpMethod.PUT, request, EndDevice.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(response.getBody().getEndDeviceIp(), "192.168.0.199");
        assertEquals(response.getBody().getEndDeviceModel().getModelId(), Integer.valueOf(1));
        assertEquals(response.getBody().getEndDeviceModel().getLatestFirmwareVersion(), "v1.2.4");
        assertEquals(response.getBody().getEndDeviceName(), "Updated Device Name");
        assertTrue(response.getBody().isDebugMode());
        assertEquals(response.getBody().getFirmware(), "1.2.3");
        assertEquals(response.getBody().getWorkingMode(), WorkingModes.MANUAL);
    }

    @Test
    void deleteEndDevice() {
        ResponseEntity<Void> response = restTemplate.exchange("/end-devices/" + VALID_END_DEVICE_MAC, HttpMethod.DELETE, null, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        ResponseEntity<EndDevice> getResponse = restTemplate.getForEntity("/end-devices/" + VALID_END_DEVICE_MAC, EndDevice.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }
}