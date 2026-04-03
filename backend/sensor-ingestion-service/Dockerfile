FROM registry.reset.inso-w.at/pub/docker/python:3.13-slim

WORKDIR /app

RUN apt-get update && apt-get install -y gcc

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ENV PYTHONUNBUFFERED=1

CMD ["sh", "-c", "sleep 10 && python main.py"]