import pytest
import pandas as pd
import numpy as np
from unittest.mock import MagicMock, patch
from weather.client import WeatherClient
from weather.enums import ForecastType
from tenacity import RetryError
import requests

@pytest.fixture
def mock_openmeteo_client():
    with patch("weather.client.openmeteo_requests.Client") as mock:
        yield mock

def test_get_forecast_current_success(mock_openmeteo_client):
    mock_response = MagicMock()
    mock_current = MagicMock()
    mock_current.Time.return_value = 1700000000

    mock_variable = MagicMock()
    mock_variable.Value.return_value = 25.5
    mock_current.Variables.return_value = mock_variable

    mock_response.Current.return_value = mock_current
    mock_openmeteo_client.return_value.weather_api.return_value = [mock_response]

    client = WeatherClient()
    df = client.get_forecast(48.2, 16.3, ForecastType.CURRENT)

    assert not df.empty
    assert "temperature_2m" in df.columns
    assert df.iloc[0]["temperature_2m"] == 25.5
    assert pd.api.types.is_datetime64_any_dtype(df["time"])

def test_get_forecast_hourly_success(mock_openmeteo_client):
    mock_response = MagicMock()
    mock_hourly = MagicMock()

    mock_hourly.Time.return_value = 1700000000
    mock_hourly.TimeEnd.return_value = 1700007200 # 2 hours
    mock_hourly.Interval.return_value = 3600

    mock_variable = MagicMock()
    mock_variable.ValuesAsNumpy.return_value = np.array([10.5, 11.0], dtype=np.float32)
    mock_hourly.Variables.return_value = mock_variable

    mock_response.Hourly.return_value = mock_hourly
    mock_openmeteo_client.return_value.weather_api.return_value = [mock_response]

    client = WeatherClient()

    df = client.get_forecast(48.2, 16.3, ForecastType.HOURLY)

    assert len(df) == 2
    assert "temperature_2m" in df.columns
    assert df.iloc[0]["temperature_2m"] == 10.5
    assert "date" in df.columns

def test_api_failure_propagates_exception(mock_openmeteo_client):
    instance = mock_openmeteo_client.return_value
    instance.weather_api.side_effect = requests.exceptions.ConnectionError("API Down")

    client = WeatherClient()

    with pytest.raises(RetryError):
        client.get_forecast(48.2, 16.3, ForecastType.CURRENT)

def test_invalid_forecast_type():
    client = WeatherClient()

    with pytest.raises(ValueError, match="Invalid forecast type"):
        client.get_forecast(48.2, 16.3, "INVALID_TYPE")