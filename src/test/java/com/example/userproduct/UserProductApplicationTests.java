package com.example.userproduct;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserProductApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate; // Uses the real embedded broker

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    void testPipeline() throws Exception {
        // Send a message through the real embedded broker channel
        kafkaTemplate.send("my-topic", "Integration Test Data");

        // Allow some time for the async consumer thread to read the record
        Thread.sleep(1000);
    }
}
