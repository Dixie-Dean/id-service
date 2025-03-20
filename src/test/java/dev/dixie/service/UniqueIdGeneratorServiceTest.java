package dev.dixie.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UniqueIdGeneratorServiceTest {

    private final static int CORRECT_ID_LENGTH = 8;
    private final static long MORE_THAN_MIN_SIZE = 10L;
    private final static long LESS_THAN_MIN_SIZE = 3L;
    private final static String KEY = "key:0";
    private final static String ID = "ABCDEFGH";

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private JedisPool jedisPool;

    @InjectMocks
    private UniqueIdGeneratorService service;

    @Test
    void generateID_NotNull() {
        var actualID = service.generateID();
        Assertions.assertNotNull(actualID);
    }

    @Test
    void generateID_CorrectIDLength() {
        var actualID = service.generateID();
        var actualIDLength = actualID.length();
        assertEquals(CORRECT_ID_LENGTH, actualIDLength);
    }

    @Test
    void retrieveID_ReturnsCorrectID() {
        try (var jedis = Mockito.mock(Jedis.class)) {
            when(jedisPool.getResource()).thenReturn(jedis);
            when(jedis.dbSize()).thenReturn(MORE_THAN_MIN_SIZE);
            when(jedis.randomKey()).thenReturn(KEY);
            when(jedis.getDel(KEY)).thenReturn(ID);

            var actualID = service.retrieveID();
            assertEquals(ID, actualID);
        }
    }

    @Test
    void retrieveID_DbSizeTriggeredAtLeastOnce() {
        try (var jedis = Mockito.mock(Jedis.class)) {
            when(jedisPool.getResource()).thenReturn(jedis);
            when(jedis.dbSize()).thenReturn(MORE_THAN_MIN_SIZE);

            service.retrieveID();

            verify(jedis, Mockito.atLeastOnce()).dbSize();
        }
    }

    @Test
    void retrieveID_RandomKeyTriggeredAtLeastOnce() {
        try (var jedis = Mockito.mock(Jedis.class)) {
            when(jedisPool.getResource()).thenReturn(jedis);
            when(jedis.dbSize()).thenReturn(MORE_THAN_MIN_SIZE);

            service.retrieveID();

            verify(jedis, Mockito.atLeastOnce()).randomKey();
        }
    }

    @Test
    void retrieveID_WhenBatchIsEmpty_ReturnsNotNull() {
        try (var jedis = Mockito.mock(Jedis.class)) {
            when(jedisPool.getResource()).thenReturn(jedis);
            when(jedis.dbSize()).thenReturn(MORE_THAN_MIN_SIZE);
            when(jedis.randomKey()).thenReturn(null);
            when(jedis.getDel(anyString())).thenReturn(ID);

            var actualID = service.retrieveID();

            assertNotNull(actualID);
        }
    }

    @Test
    void retrieveID_WhenBatchSizeIsLow_ReplenishBatchAsyncTriggered() {
        var countDownLatch = new CountDownLatch(1);
        try (var jedis = Mockito.mock(Jedis.class)) {
            when(jedisPool.getResource()).thenReturn(jedis);
            when(jedis.dbSize()).thenReturn(LESS_THAN_MIN_SIZE);

            doAnswer(invocation -> {
                countDownLatch.countDown();
                return null;
            }).when(jedis).set(anyString(), anyString());

            service.retrieveID();
            countDownLatch.await(1, TimeUnit.SECONDS);

            verify(jedis, atLeastOnce()).set(anyString(), anyString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void imagerServiceTopicListener_kafkaSendsMessages() {
        try (var jedis = Mockito.mock(Jedis.class)) {
            when(jedisPool.getResource()).thenReturn(jedis);
            when(jedis.randomKey()).thenReturn(KEY);
            when(jedis.getDel(KEY)).thenReturn(ID);

            service.imagerServiceTopicListener("request-id");

            verify(kafkaTemplate, atLeastOnce()).send(anyString(), anyString());
        }
    }

}
