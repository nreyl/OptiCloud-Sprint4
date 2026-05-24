import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

SECRET_KEY = os.environ.get("DJANGO_SECRET_KEY", "change-me-in-prod")
DEBUG = os.environ.get("DJANGO_DEBUG", "false").lower() == "true"
ALLOWED_HOSTS = os.environ.get("DJANGO_ALLOWED_HOSTS", "*").split(",")

# Which cloud provider this container serves. The same image is deployed once
# per provider (adapter-aws, adapter-gcp, ...); each instance is scoped to its
# provider and exposed under /adapters/<provider>/* at the gateway. Adding a
# provider is a new container with a different ADAPTER_PROVIDER, not a change
# to any existing service (ASR 3 - Modificabilidad).
ADAPTER_PROVIDER = os.environ.get("ADAPTER_PROVIDER", "aws").lower()

INSTALLED_APPS = [
    "django.contrib.contenttypes",
    "django.contrib.auth",
    "rest_framework",
    "adapters",
]

MIDDLEWARE = [
    "django.middleware.common.CommonMiddleware",
]

ROOT_URLCONF = "cloud_adapter_project.urls"
WSGI_APPLICATION = "cloud_adapter_project.wsgi.application"

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": os.environ.get("ADAPTER_DB_NAME", "adapter_db"),
        "USER": os.environ.get("ADAPTER_DB_USER", "adapter_user"),
        "PASSWORD": os.environ.get("ADAPTER_DB_PASSWORD", "isis2503"),
        "HOST": os.environ.get("ADAPTER_DB_HOST", "localhost"),
        "PORT": os.environ.get("ADAPTER_DB_PORT", "5432"),
    }
}

USE_TZ = True
TIME_ZONE = "UTC"

REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": [],
    "DEFAULT_PERMISSION_CLASSES": ["rest_framework.permissions.AllowAny"],
}

NORMALIZATION_SERVICE_URL = os.environ.get(
    "NORMALIZATION_SERVICE_URL", "http://localhost:8081/normalize"
)
REQUEST_TIMEOUT_SECONDS = int(os.environ.get("REQUEST_TIMEOUT_SECONDS", "10"))
DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {"plain": {"format": "%(asctime)s %(levelname)s %(name)s - %(message)s"}},
    "handlers": {"console": {"class": "logging.StreamHandler", "formatter": "plain"}},
    "root": {"handlers": ["console"], "level": "INFO"},
}
