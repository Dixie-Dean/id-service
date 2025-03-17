package dev.dixie.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class UniqueIdGeneratorService implements IdService {

    private static final String LISTEN_TO_TOPIC_NAME = "request-id-topic";
    private static final String SEND_TO_TOPIC_NAME = "provide-id-topic";
    private static final int MIN_BATCH_SIZE = 5;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JedisPool jedisPool = new JedisPool("localhost", 6381);
    private final AtomicLong KEY_COUNTER = new AtomicLong();

    @Override
    public String generateId() {
        var uuidBytes = UUID.randomUUID().toString().getBytes();
        var encodedID = Base64.getUrlEncoder().encodeToString(uuidBytes);
        var id = encodedID.substring(0, 8);
        log.info("GenerateID | ID:{}", id);
        return id;
    }

    public String getIdFromBatch() {
        try (var jedis = jedisPool.getResource()) {
            ensureBatchSize(jedis);

            // TODO handle null case
            var key = jedis.randomKey();
            return jedis.getDel(key);
        }
    }

    //TODO find another way to synchronize the code
    private synchronized void ensureBatchSize(Jedis jedis) {
        if (jedis.dbSize() < MIN_BATCH_SIZE) {
            replenishBatch(jedis);
        }
    }

    public void replenishBatch(Jedis jedis) {
        for (int i = 0; i < MIN_BATCH_SIZE * 2; i++) {
            jedis.set("key:%d".formatted(KEY_COUNTER.getAndIncrement()), generateId());
        }
    }

    @KafkaListener(topics = LISTEN_TO_TOPIC_NAME, groupId = "imager")
    public void imagerServiceTopicListener(String value) {
        log.info("ImagerServiceTopicListener | message:{}, topic:{}", value, LISTEN_TO_TOPIC_NAME);
        if ("request-id".equals(value)) {
            var id = getIdFromBatch();
            kafkaTemplate.send(SEND_TO_TOPIC_NAME, id);
            log.info("ImagerServiceTopicListener | ID:{} topic:{}", id, SEND_TO_TOPIC_NAME);
        }
    }
}
