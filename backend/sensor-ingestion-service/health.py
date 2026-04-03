import requests
from datetime import datetime
import pika
import json
from weather.enums import ForecastType
from weather.client import WeatherClient
from weather.logger import setup_logger

logger = setup_logger(name="health_check")

def check_dil_health():
    health_status = {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "components": {}
    }

    try:
        connection = pika.BlockingConnection(
            pika.ConnectionParameters(host='localhost')
        )
        channel = connection.channel()
        health_status["components"]["rabbitmq"] = {
            "status": "connected",
            "channels": "active"
        }
        connection.close()
    except Exception as e:
        health_status["components"]["rabbitmq"] = {
            "status": "error",
            "error": str(e)
        }
        health_status["status"] = "degraded"

    try:
        client = WeatherClient()
        test_df = client.get_forecast(48.2085, 16.3721, ForecastType.CURRENT)
        health_status["components"]["weather_api"] = {
            "status": "responsive",
            "response_time": "ok"
        }
    except Exception as e:
        health_status["components"]["weather_api"] = {
            "status": "error",
            "error": str(e)
        }
        health_status["status"] = "unhealthy"



    return health_status