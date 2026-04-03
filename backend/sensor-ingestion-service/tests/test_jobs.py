import pytest
from unittest.mock import patch, MagicMock
from scheduler.jobs_current import fetch_current_weather_for_all
from scheduler.jobs_hourly import fetch_hourly_weather_for_all
from scheduler.jobs_daily import fetch_daily_weather_for_all
from weather.enums import ForecastType

MOCK_USERS = [
    {
        "id": "user1",
        "email": "user1@test.com",
        "farms": [
            {"id": "farm1", "latitude": 10.0, "longitude": 20.0},
            {"id": "farm2"}
        ]
    },
    {
        "id": "user2",
        "email": "user2@test.com",
        "farms": []
    }
]

@patch("scheduler.jobs_current.get_all_users_with_farms")
@patch("scheduler.jobs_current.fetch_and_publish_for_farm")
def test_fetch_current_weather_logic(mock_fetch, mock_get_users):
    mock_get_users.return_value = MOCK_USERS
    fetch_current_weather_for_all()
    assert mock_fetch.call_count == 1

    mock_fetch.assert_called_with(
        "user1",
        "user1@test.com",
        MOCK_USERS[0]["farms"][0],
        ForecastType.CURRENT
    )

@patch("scheduler.jobs_hourly.get_all_users_with_farms")
@patch("scheduler.jobs_hourly.fetch_and_publish_for_farm")
def test_fetch_hourly_weather_logic(mock_fetch, mock_get_users):
    mock_get_users.return_value = MOCK_USERS

    fetch_hourly_weather_for_all()

    assert mock_fetch.call_count == 1
    mock_fetch.assert_called_with(
        "user1",
        "user1@test.com",
        MOCK_USERS[0]["farms"][0],
        ForecastType.HOURLY
    )


@patch("scheduler.jobs_daily.get_all_users_with_farms")
@patch("scheduler.jobs_daily.fetch_and_publish_for_farm")
def test_fetch_daily_weather_logic(mock_fetch, mock_get_users):
    mock_get_users.return_value = MOCK_USERS

    fetch_daily_weather_for_all()

    assert mock_fetch.call_count == 1
    mock_fetch.assert_called_with(
        "user1",
        "user1@test.com",
        MOCK_USERS[0]["farms"][0],
        ForecastType.DAILY
    )