package io.github.tral909.spring.boot.validation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Bean
    public ValidAnnotationCheckBeanPostProcessor validAnnotationCheckBeanPostProcessor() {
        return new ValidAnnotationCheckBeanPostProcessor();
    }
}
