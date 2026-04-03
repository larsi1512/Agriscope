import openmeteo_requests
import pandas as pd
import requests_cache
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from tenacity import retry as tenacity_retry, stop_after_attempt, wait_exponential, retry_if_exception_type
import requests

from .config import Config
from .enums import ForecastType
from .utils import create_time_index
from .logger import setup_logger

logger = setup_logger(name="weather_client")

class WeatherClient:
    def __init__(self):
        session = requests.Session()

        adapter = HTTPAdapter()
        session.mount("http://", adapter)
        session.mount("https://", adapter)

        self.client = openmeteo_requests.Client(session=session)
        self.url = Config.WEATHER_API_URL
        self.timezone = Config.WEATHER_TIMEZONE
        self.request_timeout = Config.WEATHER_TIMEOUT

    @tenacity_retry(
        stop=stop_after_attempt(2),
        wait=wait_exponential(multiplier=1, min=2, max=4),
        retry=retry_if_exception_type((requests.RequestException,))
    )
    def get_forecast(self, latitude: float, longitude: float, forecast_type: ForecastType) -> pd.DataFrame:

        try:
            responses = self.client.weather_api(
                self.url,
                params=self._build_params(latitude, longitude),
                timeout=self.request_timeout
            )
            response = responses[0]

            match forecast_type:
                case ForecastType.CURRENT:
                    df = self._parse_current(response)
                case ForecastType.HOURLY:
                    df = self._parse_hourly(response)
                case ForecastType.DAILY:
                    df = self._parse_daily(response)
                case _:
                    raise ValueError(f"Invalid forecast type: {forecast_type}")

            return df

        except Exception as e:
            logger.error(f"Weather API call failed for {latitude},{longitude}: {e}")
            raise

    def _build_params(self, lat, lon):
        return {
            "latitude": lat,
            "longitude": lon,
            "daily": [
                "temperature_2m_max", "temperature_2m_min", "showers_sum",
                "rain_sum", "snowfall_sum", "wind_speed_10m_max",
                "daylight_duration", "et0_fao_evapotranspiration"
            ],
            "hourly": [
                "temperature_2m", "soil_moisture_0_to_1cm", "soil_moisture_1_to_3cm",
                "soil_moisture_3_to_9cm", "soil_moisture_9_to_27cm", "freezing_level_height",
                "rain", "snowfall", "precipitation", "precipitation_probability",
                "apparent_temperature", "relative_humidity_2m", "sunshine_duration",
                "direct_radiation", "diffuse_radiation", "shortwave_radiation_instant",
                "wind_speed_10m", "et0_fao_evapotranspiration"
            ],
            "current": [
                "temperature_2m", "wind_speed_10m", "rain", "precipitation",
                "showers", "snowfall", "weather_code"
            ],
            "timezone": self.timezone,
        }

    def _parse_current(self, response):
        current = response.Current()
        names = ["temperature_2m", "wind_speed_10m", "rain", "precipitation",
                 "showers", "snowfall", "weather_code"]
        data = {name: current.Variables(i).Value() for i, name in enumerate(names)}
        data["time"] = pd.to_datetime(current.Time(), unit="s", utc=True)
        return pd.DataFrame([data])

    def _parse_hourly(self, response):
        hourly = response.Hourly()
        names = ["temperature_2m", "soil_moisture_0_to_1cm", "soil_moisture_1_to_3cm",
                 "soil_moisture_3_to_9cm", "soil_moisture_9_to_27cm", "freezing_level_height",
                 "rain", "snowfall", "precipitation", "precipitation_probability",
                 "apparent_temperature", "relative_humidity_2m", "sunshine_duration",
                 "direct_radiation", "diffuse_radiation", "shortwave_radiation_instant",
                 "wind_speed_10m", "et0_fao_evapotranspiration"]
        data = {name: hourly.Variables(i).ValuesAsNumpy() for i, name in enumerate(names)}
        data["date"] = create_time_index(hourly.Time(), hourly.TimeEnd(), hourly.Interval())
        return pd.DataFrame(data)

    def _parse_daily(self, response):
        daily = response.Daily()
        names = ["temperature_2m_max", "temperature_2m_min", "showers_sum", "rain_sum",
                 "snowfall_sum", "wind_speed_10m_max", "daylight_duration", "et0_fao_evapotranspiration"]
        data = {name: daily.Variables(i).ValuesAsNumpy() for i, name in enumerate(names)}
        data["date"] = create_time_index(daily.Time(), daily.TimeEnd(), daily.Interval())
        return pd.DataFrame(data)