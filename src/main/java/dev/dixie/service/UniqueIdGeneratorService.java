package dev.dixie.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final static String LISTEN_TO_TOPIC_NAME = "imager-service";
    private final static String SEND_TO_TOPIC_NAME = "id-service";
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public String generateId() {
        byte[] randomBytes = new byte[BYTE_LENGTH];
        new Random().nextBytes(randomBytes);
        String id = Base64.getUrlEncoder().encodeToString(randomBytes);
        log.info("GenerateID | ID:{}", id);
        return id;
    }

    @KafkaListener(topics = LISTEN_TO_TOPIC_NAME, groupId = "imager")
    public void imagerServiceTopicListener(String value) {
        log.info("ImagerServiceTopicListener | message:{}, topic:{}", value, LISTEN_TO_TOPIC_NAME);
        if ("request-id".equals(value)) {
            String id = generateId();
            kafkaTemplate.send(SEND_TO_TOPIC_NAME, id);
            log.info("ImagerServiceTopicListener | ID:{} topic:{}", id, SEND_TO_TOPIC_NAME);
        }
    }
}
