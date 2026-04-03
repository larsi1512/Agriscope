import pytest
import time
from scheduler.scheduler import CircuitBreaker
from unittest.mock import patch, MagicMock
from scheduler.scheduler import safe_execute, initial_data_fetch
from scheduler.jobs_current import fetch_current_weather_for_all
from scheduler.jobs_hourly import fetch_hourly_weather_for_all
from scheduler.jobs_daily import fetch_daily_weather_for_all

def test_circuit_breaker_opens_on_failures():
    cb = CircuitBreaker(failure_threshold=2, recovery_timeout=1)

    def failing_job():
        raise Exception("Failing job!")

    with pytest.raises(Exception):
        cb.call(failing_job)
    assert cb.state == "CLOSED"
    assert cb.failure_count == 1

    with pytest.raises(Exception):
        cb.call(failing_job)
    assert cb.state == "OPEN"

    with pytest.raises(Exception, match="Circuit breaker is OPEN"):
        cb.call(failing_job)


@patch("scheduler.scheduler.logger")
def test_safe_execute_success(mock_logger):
    mock_job = MagicMock(__name__="test_job")

    safe_execute(mock_job)

    mock_job.assert_called_once()
    mock_logger.info.assert_called_with("Job test_job executed successfully")

@patch("scheduler.scheduler.logger")
def test_safe_execute_failure(mock_logger):
    mock_job = MagicMock(__name__="fail_job")
    mock_job.side_effect = Exception("Crash!")

    safe_execute(mock_job)

    mock_job.assert_called_once()
    args, _ = mock_logger.error.call_args
    assert "Job fail_job failed: Crash!" in args[0]


@patch("scheduler.scheduler.safe_execute")
def test_initial_data_fetch(mock_safe_execute):
    initial_data_fetch()

    assert mock_safe_execute.call_count == 3

    mock_safe_execute.assert_any_call(fetch_current_weather_for_all)
    mock_safe_execute.assert_any_call(fetch_hourly_weather_for_all)
    mock_safe_execute.assert_any_call(fetch_daily_weather_for_all)