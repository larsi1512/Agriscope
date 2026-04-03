import pandas as pd
from weather.utils import create_time_index

def test_create_time_index():
    start = 1700000000
    end = 1700003600
    interval = 3600

    result = create_time_index(start, end, interval)

    assert len(result) == 1
    assert isinstance(result, pd.DatetimeIndex)
    assert result[0].tzinfo is not None