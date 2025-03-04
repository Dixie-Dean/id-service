package dev.dixie.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class IdKafkaTopicConfig {

    @Bean
    public NewTopic idServiceTopic() {
        return TopicBuilder.name("provide-id-topic").build();
    }
}
