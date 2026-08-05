package com.example.userproduct.config;

import io.swagger.v3.core.jackson.ModelResolver;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration for SpringDoc to handle Jackson serialization properly
 */
@Configuration
public class SpringDocConfig {

    @Bean
    public ModelResolver modelResolver(ObjectMapper objectMapper) {
        return new ModelResolver(objectMapper);
    }

    /**
     * Customize SpringDoc configuration to explicitly disable actuator integration
     */
    @Bean
    public SpringDocConfigProperties springDocConfigProperties() {
        SpringDocConfigProperties config = new SpringDocConfigProperties();
        config.setShowActuator(false);
        return config;
    }
}
