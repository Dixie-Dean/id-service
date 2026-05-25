package dev.dixie.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class UniqueIdGeneratorService implements IdService {

    private static final AtomicBoolean IS_REPLENISHING = new AtomicBoolean(false);
    private static final String ID_KEY = "available-ids";
    private static final int MIN_BATCH_SIZE = 5;
    private static final String LISTEN_TO_TOPIC_NAME = "request-id-topic";
    private static final String SEND_TO_TOPIC_NAME = "provide-id-topic";

    private final JedisPool jedisPool;
    private final ThreadPoolTaskExecutor executorService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public String generateID() {
        var uuidBytes = UUID.randomUUID().toString().getBytes();
        var encodedID = Base64.getUrlEncoder().encodeToString(uuidBytes);
        var id = encodedID.substring(0, 8);
        log.info("GenerateID | ID:{}", id);
        return id;
    }

    protected String retrieveID() {
        try (var jedis = jedisPool.getResource()) {
            if (isBatchSizeLessThanMin(jedis)) {
                replenishBatchAsync();
            }
            var id = jedis.spop(ID_KEY);

            if (id == null) {
                id = generateID();
            }
            return id;
        } catch (Exception exception) {
            log.error("retrieveID | exception:{}", exception.getMessage());
            return generateID();
        }
    }

    private boolean isBatchSizeLessThanMin(Jedis jedis) {
        var checkResult = jedis.scard(ID_KEY) < MIN_BATCH_SIZE;
        log.info("isBatchSizeLessThanMin | result:{}", checkResult);
        return checkResult;
    }

    private void replenishBatchAsync() {
        log.info("replenishBatchAsync | called");

        if (!IS_REPLENISHING.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
                    try (var jedis = jedisPool.getResource()) {
                        if (isBatchSizeLessThanMin(jedis)) {
                            replenishBatch(jedis);
                        }
                    } finally {
                        IS_REPLENISHING.set(false);
                    }
                }, executorService)
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
        jedis.sadd(ID_KEY, generateID());
    }

    @KafkaListener(topics = LISTEN_TO_TOPIC_NAME, groupId = "imager")
    public void imagerServiceTopicListener(String value, @Header("correlationID") String correlationID) {
        log.info("ID-SERVICE from IMAGER-SERVICE | message:{}, topic:{}, correlationID:{}", value, LISTEN_TO_TOPIC_NAME, correlationID);
        if ("request-id".equals(value)) {
            var id = retrieveID();
            ProducerRecord<String, String> record = new ProducerRecord<>(SEND_TO_TOPIC_NAME, id);
            record.headers().add("correlationID", correlationID.getBytes(StandardCharsets.UTF_8));
            kafkaTemplate.send(record);
            log.info("imagerServiceTopicListener | ID:{} topic:{}", id, SEND_TO_TOPIC_NAME);
        }
    }
}