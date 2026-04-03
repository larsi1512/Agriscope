import pytest
import json
from unittest.mock import patch, MagicMock
from listeners.farm_event_listener import process_new_farm_event, process_new_field_event

@patch("listeners.farm_event_listener.fetch_and_publish_for_farm")
def test_process_new_farm_event_success(mock_fetch):
    mock_ch = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 1

    body = json.dumps({
        "user_id": "u1",
        "email": "e@e.com",
        "farm": {"id": "f1"}
    }).encode('utf-8')

    process_new_farm_event(mock_ch, mock_method, None, body)
    mock_fetch.assert_called_once()
    mock_ch.basic_ack.assert_called_with(delivery_tag=1)

@patch("listeners.farm_event_listener.fetch_and_publish_for_farm")
def test_process_new_farm_event_error_handling(mock_fetch):
    mock_fetch.side_effect = Exception("API Error")

    mock_ch = MagicMock()
    mock_method = MagicMock()

    body = json.dumps({"user_id": "u1", "farm": {"id": "f1"}}).encode('utf-8')

    process_new_farm_event(mock_ch, mock_method, None, body)
    mock_ch.basic_nack.assert_called_with(delivery_tag=mock_method.delivery_tag, requeue=False)