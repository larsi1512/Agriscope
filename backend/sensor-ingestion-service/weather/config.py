import os

class Config:
    WEATHER_API_URL = os.getenv("WEATHER_API_URL", "https://api.open-meteo.com/v1/forecast")
    WEATHER_TIMEZONE = os.getenv("WEATHER_TIMEZONE", "Europe/Berlin")
    WEATHER_TIMEOUT = int(os.getenv("WEATHER_TIMEOUT", "30"))
    WEATHER_MAX_RETRIES = int(os.getenv("WEATHER_MAX_RETRIES", "3"))