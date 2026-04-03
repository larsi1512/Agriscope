import pandas as pd

def create_time_index(start, end, interval):
    return pd.date_range(
        start=pd.to_datetime(start, unit="s", utc=True),
        end=pd.to_datetime(end, unit="s", utc=True),
        freq=pd.Timedelta(seconds=interval),
        inclusive="left"
    )
