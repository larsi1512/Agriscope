import pika
import json
import logging
import os

logger = logging.getLogger("rabbitmq")

rabbitmq_host = os.getenv('RABBITMQ_HOST', 'localhost')
_channel = None
_connection = None

def get_global_channel():
    global _channel, _connection
    if _connection is None or _connection.is_closed:
        _connection = get_connection()

    if _channel is None or _channel.is_closed:
        _channel = _connection.channel()
        _channel.basic_qos(prefetch_count=1)

    return _channel

def setup_listener(exchange_name, queue_name, routing_key, callback_function):
    try:
        channel = get_global_channel()

        channel.exchange_declare(exchange=exchange_name, exchange_type='topic', durable=True)

        result = channel.queue_declare(queue=queue_name, durable=True)
        final_queue_name = result.method.queue

        channel.queue_bind(exchange=exchange_name, queue=final_queue_name, routing_key=routing_key)

        channel.basic_consume(queue=final_queue_name, on_message_callback=callback_function)

        logger.info(f" [*] Registered listener for {routing_key} on queue {final_queue_name}")

    except Exception as e:
        logger.error(f"Failed to setup listener for {routing_key}: {e}")

def start_event_loop():
    try:
        channel = get_global_channel()
        logger.info(" [*] Waiting for messages. To exit press CTRL+C")
        channel.start_consuming()
    except Exception as e:
        logger.error(f"Event loop crashed: {e}")

def get_connection():
    connection = pika.BlockingConnection(
        pika.ConnectionParameters(host=rabbitmq_host)
    )
    return connection

def publish_message(data, routing_key, exchange="weather_exchange"):

    try:
        connection = get_connection()
        channel = connection.channel()

        channel.exchange_declare(exchange=exchange, exchange_type="topic", durable=True)

        message = json.dumps(data)
        channel.basic_publish(
            exchange=exchange,
            routing_key=routing_key,
            body=message,
            properties=pika.BasicProperties(
                delivery_mode=2,
            )
        )
        connection.close()
    except Exception as e:
        logger.error(f"Failed to publish message to {routing_key}: {e}")

def start_consuming(exchange_name, queue_name, routing_key, callback_function):

    try:
        connection = get_connection()
        channel = connection.channel()

        channel.exchange_declare(exchange=exchange_name, exchange_type='topic', durable=True)

        result = channel.queue_declare(queue=queue_name, durable=True)
        final_queue_name = result.method.queue

        channel.queue_bind(exchange=exchange_name, queue=final_queue_name, routing_key=routing_key)

        logger.info(f" [*] Waiting for messages in {final_queue_name}. To exit press CTRL+C")

        channel.basic_qos(prefetch_count=1)
        channel.basic_consume(queue=final_queue_name, on_message_callback=callback_function)

        channel.start_consuming()
    except Exception as e:
        logger.error(f"RabbitMQ Consumer failed: {e}")
