package com.agriscope.rule_engine.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String WEATHER_EXCHANGE = "weather_exchange";
    public static final String WEATHER_QUEUE = "weather_rule_queue";

    public static final String ALERT_EXCHANGE = "alert_exchange";
    public static final String ALERT_QUEUE = "alert_queue";
    public static final String ALERT_ROUTING_KEY = "alert.recommendation";

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }


    @Bean
    public TopicExchange weatherExchange() {
        return new TopicExchange(WEATHER_EXCHANGE, true, false);
    }

    @Bean
    public Queue weatherQueue() {
        return new Queue(WEATHER_QUEUE, true);
    }

    @Bean
    public Binding weatherBinding(Queue weatherQueue, TopicExchange weatherExchange) {
        return BindingBuilder.bind(weatherQueue).to(weatherExchange).with("weather.#");
    }

    @Bean
    public TopicExchange alertExchange() {
        return new TopicExchange(ALERT_EXCHANGE, true, false);
    }

    @Bean
    public Queue alertQueue() {
        return new Queue(ALERT_QUEUE, true);
    }

    @Bean
    public Binding alertBinding(Queue alertQueue, TopicExchange alertExchange) {
        return BindingBuilder.bind(alertQueue).to(alertExchange).with(ALERT_ROUTING_KEY);
    }
}
