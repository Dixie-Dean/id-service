package dev.dixie.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class UniqueIdGeneratorService implements IdService {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    private static final JedisPool JEDIS_POOL = new JedisPool("localhost", 6381);
    private static final AtomicLong KEY_COUNTER = new AtomicLong();
    private static final int MIN_BATCH_SIZE = 5;
    private static final String LISTEN_TO_TOPIC_NAME = "request-id-topic";
    private static final String SEND_TO_TOPIC_NAME = "provide-id-topic";
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public String generateID() {
        var uuidBytes = UUID.randomUUID().toString().getBytes();
        var encodedID = Base64.getUrlEncoder().encodeToString(uuidBytes);
        var id = encodedID.substring(0, 8);
        log.info("GenerateID | ID:{}", id);
        return id;
    }

    private String retrieveID() {
        try (var jedis = JEDIS_POOL.getResource()) {
            if (isBatchSizeLessThanMin(jedis)) {
                replenishBatchAsync();
            }
            var key = getRandomKey(jedis).orElseGet(this::generateID);
            log.info("retrieveID | {}", key);
            return jedis.getDel(key);
        }
    }

    private Optional<String> getRandomKey(Jedis jedis) {
        return Optional.ofNullable(jedis.randomKey());
    }

    private boolean isBatchSizeLessThanMin(Jedis jedis) {
        var checkResult = jedis.dbSize() < MIN_BATCH_SIZE;
        log.info("isBatchSizeLessThanMin | result:{}", checkResult);
        return jedis.dbSize() < MIN_BATCH_SIZE;
    }

    private void replenishBatchAsync() {
        log.info("replenishBatchAsync | called");
        CompletableFuture.runAsync(() -> {
            try (var jedis = JEDIS_POOL.getResource()) {
                if (isBatchSizeLessThanMin(jedis)) {
                    replenishBatch(jedis);
                }
            }
        }, EXECUTOR_SERVICE)
                .exceptionally(exception -> {
                    log.error("replenishBatchAsync | exception:{}", exception.getMessage());
                    return null;
                });
    }

    private void replenishBatch(Jedis jedis) {
        for (int i = 0; i < MIN_BATCH_SIZE * 2; i++) {
            pushID(jedis);
        }
    }

    private void pushID(Jedis jedis) {
        jedis.set("key:%d".formatted(KEY_COUNTER.getAndIncrement()), generateID());
    }

    @KafkaListener(topics = LISTEN_TO_TOPIC_NAME, groupId = "imager")
    public void imagerServiceTopicListener(String value) {
        log.info("imagerServiceTopicListener | message:{}, topic:{}", value, LISTEN_TO_TOPIC_NAME);
        if ("request-id".equals(value)) {
            var id = retrieveID();
            kafkaTemplate.send(SEND_TO_TOPIC_NAME, id);
            log.info("imagerServiceTopicListener | ID:{} topic:{}", id, SEND_TO_TOPIC_NAME);
        }
    }
}
