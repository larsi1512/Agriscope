package com.agriscope.notification_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ALERT_QUEUE = "alert_queue";
    public static final String ALERT_EXCHANGE = "alert_exchange";
    public static final String ALERT_ROUTING_KEY = "alert.recommendation";

    public static final String WEATHER_EXCHANGE = "weather_exchange";
    public static final String NOTIFICATION_WEATHER_QUEUE = "weather_notification_queue";
    public static final String WEATHER_ROUTING_KEY = "weather.current";

    public static final String EMAIL_QUEUE = "email_queue";
    public static final String EMAIL_EXCHANGE = "email_exchange";
    public static final String EMAIL_ROUTING_KEY = "email.generic";

    public static final String FARM_EVENTS_EXCHANGE = "farm_events";
    public static final String HARVEST_QUEUE = "notification_harvest_queue";
    public static final String HARVEST_ROUTING_KEY = "field.harvested";

    public static final String USER_REGISTERED_QUEUE = "user_registered_queue";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";

    @Bean
    public Queue userRegisteredQueue() {
        return new Queue(USER_REGISTERED_QUEUE, true);
    }

    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(userRegisteredQueue)
                .to(emailExchange)
                .with(USER_REGISTERED_ROUTING_KEY);
    }


    @Bean
    public Queue alertQueue() {
        return new Queue(ALERT_QUEUE, true);
    }

    @Bean
    public TopicExchange alertExchange() {
        return new TopicExchange(ALERT_EXCHANGE, true, false);
    }

    @Bean
    public Binding alertBinding(Queue alertQueue, TopicExchange alertExchange) {
        return BindingBuilder.bind(alertQueue).to(alertExchange).with(ALERT_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange weatherExchange() {
        return new TopicExchange(WEATHER_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationWeatherQueue() {
        return new Queue(NOTIFICATION_WEATHER_QUEUE, true);
    }

    @Bean
    public Binding notificationWeatherBinding(Queue notificationWeatherQueue, TopicExchange weatherExchange) {
        return BindingBuilder.bind(notificationWeatherQueue).to(weatherExchange).with(WEATHER_ROUTING_KEY);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EMAIL_EXCHANGE, true, false);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(emailQueue).to(emailExchange).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public TopicExchange farmEventsExchange() {
        return new TopicExchange(FARM_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue harvestQueue() {
        return new Queue(HARVEST_QUEUE, true);
    }

    @Bean
    public Binding harvestBinding(Queue harvestQueue, TopicExchange farmEventsExchange) {
        return BindingBuilder.bind(harvestQueue).to(farmEventsExchange).with(HARVEST_ROUTING_KEY);
    }
}