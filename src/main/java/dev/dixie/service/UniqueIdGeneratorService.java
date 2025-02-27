package dev.dixie.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class UniqueIdGeneratorService implements IdService {

    private static final int BYTE_LENGTH = 6;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final Marker kafkaMarker = MarkerFactory.getMarker("KAFKA");
    private final Marker serviceMarker = MarkerFactory.getMarker("SERVICE");

    @Override
    public String generateId() {
        byte[] randomBytes = new byte[BYTE_LENGTH];
        new Random().nextBytes(randomBytes);
        String id = Base64.getUrlEncoder().encodeToString(randomBytes);
        log.info(serviceMarker, "Generated ID:{}", id);
        return id;
    }

    @KafkaListener(topics = "imager-service", groupId = "imager")
    public void imagerServiceTopicListener(String value) {
        log.info(kafkaMarker, "Received message:{} from topic:{}", value, "imager-service");
        if ("request-id".equals(value)) {
            String id = generateId();
            kafkaTemplate.send("id-service", id);
            log.info(kafkaMarker, "Sent ID:{} to {}", id, "id-service");
        }
    }
}
