import threading

from scheduler.scheduler import start_scheduler
from health import check_dil_health
from weather.logger import setup_logger
from listeners.farm_event_listener import start_listening

logger = setup_logger(name="main")
setup_logger(name="farm_listener")
setup_logger(name="rabbitmq")

if __name__ == "__main__":
    logger.info("AGRISCOPE starting")

    health = check_dil_health()
    if health["status"] == "unhealthy":
        logger.error("System unhealthy, cannot start scheduler")
        exit(1)

    try:
        listener_thread = threading.Thread(target=start_listening, daemon=True)
        listener_thread.start()
        logger.info("RabbitMQ Listener thread started")

        start_scheduler()

    except KeyboardInterrupt:
        logger.info("Service stopped by user")
    except Exception as e:
        logger.error(f"Service crashed: {e}")
        exit(1)