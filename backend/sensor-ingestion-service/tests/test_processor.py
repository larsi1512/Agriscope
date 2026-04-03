import pytest
from unittest.mock import patch, MagicMock
from weather.processor import fetch_and_publish_for_farm
from weather.enums import ForecastType
import pandas as pd

@patch("weather.processor.WeatherClient")
@patch("weather.processor.publish_message")
def test_fetch_and_publish_current(mock_publish, MockClient):
    mock_client_instance = MockClient.return_value
    mock_client_instance.get_forecast.return_value = pd.DataFrame([{
        "temperature_2m": 20,
        "time": "2024-01-01"
    }])

    farm_data = {
        "id": "farm1",
        "name": "My Farm",
        "latitude": 10.0,
        "longitude": 20.0,
        "fields": [
            {"id": 1, "seedType": "WHEAT", "growthStage": "YOUNG"}
        ]
    }

    fetch_and_publish_for_farm("user1", "test@test.com", farm_data, ForecastType.CURRENT)

    mock_publish.assert_called_once()

    args, kwargs = mock_publish.call_args
    payload = args[0]
    routing_key = kwargs['routing_key']

    assert routing_key == "weather.current"
    assert payload["farm_id"] == "farm1"
    assert payload["fields"][0]["seed_type"] == "WHEAT"