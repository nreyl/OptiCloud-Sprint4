FROM python:3.12-slim

WORKDIR /app

RUN apt-get update -y && apt-get install -y --no-install-recommends build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ENV PYTHONUNBUFFERED=1
ENV DJANGO_SETTINGS_MODULE=ingestion_project.settings
EXPOSE 8080

CMD ["sh", "-c", "python manage.py migrate --noinput && gunicorn ingestion_project.wsgi:application -b 0.0.0.0:8080 --workers 2"]
