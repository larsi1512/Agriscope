package ase_pr_inso_01.user_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  public static final String EMAIL_QUEUE = "email_queue";
  public static final String EMAIL_EXCHANGE = "email_exchange";
  public static final String EMAIL_ROUTING_KEY = "email.generic";

  public static final String USER_REGISTERED_QUEUE = "user_registered_queue";
  public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";

  @Bean
  public TopicExchange emailExchange() {
    return new TopicExchange(EMAIL_EXCHANGE);
  }

  @Bean
  public Queue emailQueue() {
    return new Queue(EMAIL_QUEUE, true);
  }

  @Bean
  public Queue userRegisteredQueue() {
    return new Queue(USER_REGISTERED_QUEUE, true);
  }

  @Bean
  public Binding binding(Queue emailQueue, TopicExchange emailExchange) {
    return BindingBuilder.bind(emailQueue).to(emailExchange).with(EMAIL_ROUTING_KEY);
  }

  @Bean
  public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange emailExchange) {
    return BindingBuilder.bind(userRegisteredQueue)
            .to(emailExchange)
            .with(USER_REGISTERED_ROUTING_KEY);
  }

  @Bean
  public Jackson2JsonMessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}