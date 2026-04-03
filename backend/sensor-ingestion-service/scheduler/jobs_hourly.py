import json
import os
from weather.enums import ForecastType
from weather.processor import fetch_and_publish_for_farm
from weather.logger import setup_logger
from database.repository import get_all_users_with_farms

logger = setup_logger(name="hourly_job")

def fetch_hourly_weather_for_all():
    users = get_all_users_with_farms()
    logger.info("Fetching HOURLY weather for all users/farms")

    for user in users:
        user_id = user["id"]
        email = user["email"]
        farms = user.get("farms", [])

        if not farms:
            logger.info(f"User {email} has no farms.")
            continue

        for farm in farms:
            if farm.get("latitude") is None or farm.get("longitude") is None:
                logger.warning(f"Skipping farm {farm.get('id')} - missing coordinates")
                continue

            fetch_and_publish_for_farm(user_id, email, farm, ForecastType.HOURLY)