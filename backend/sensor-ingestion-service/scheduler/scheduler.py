import schedule
import time
from scheduler.jobs_current import fetch_current_weather_for_all
from scheduler.jobs_hourly import fetch_hourly_weather_for_all
from scheduler.jobs_daily import fetch_daily_weather_for_all
from weather.logger import setup_logger

logger = setup_logger(name="scheduler")

class CircuitBreaker:
    def __init__(self, failure_threshold=5, recovery_timeout=60):
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.failure_count = 0
        self.last_failure_time = None
        self.state = "CLOSED"

    def call(self, func, *args, **kwargs):
        if self.state == "OPEN":
            if time.time() - self.last_failure_time > self.recovery_timeout:
                self.state = "HALF_OPEN"
            else:
                raise Exception("Circuit breaker is OPEN")

        try:
            result = func(*args, **kwargs)
            if self.state == "HALF_OPEN":
                self.state = "CLOSED"
                self.failure_count = 0
            return result
        except Exception as e:
            self.failure_count += 1
            self.last_failure_time = time.time()
            if self.failure_count >= self.failure_threshold:
                self.state = "OPEN"
            raise e

weather_circuit_breaker = CircuitBreaker()

def safe_execute(job_func):
    try:
        weather_circuit_breaker.call(job_func)
        logger.info(f"Job {job_func.__name__} executed successfully")
    except Exception as e:
        logger.error(f"Job {job_func.__name__} failed: {e}")

def initial_data_fetch():
    logger.info("Initial data fetch")
    safe_execute(fetch_current_weather_for_all)
    safe_execute(fetch_hourly_weather_for_all)
    safe_execute(fetch_daily_weather_for_all)

def start_scheduler():
    logger.info("Scheduler started")

    initial_data_fetch()

    schedule.every(5).minutes.do(lambda: safe_execute(fetch_current_weather_for_all))
    schedule.every(1).hours.do(lambda: safe_execute(fetch_hourly_weather_for_all))
    schedule.every(12).hours.do(lambda: safe_execute(fetch_daily_weather_for_all))

    while True:
        schedule.run_pending()
        time.sleep(1)