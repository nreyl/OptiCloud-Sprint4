import logging
import sys

from django.apps import AppConfig

# Schema/management commands run before the tables exist (or don't need the
# config); only the serving process should preload externalized configuration.
_SKIP_COMMANDS = {"makemigrations", "migrate", "collectstatic", "shell", "test"}


class AdaptersConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "adapters"

    def ready(self) -> None:
        if _SKIP_COMMANDS & set(sys.argv):
            return
        from django.conf import settings

        from .config import provider_config

        try:
            provider_config.load(settings.ADAPTER_PROVIDER)
        except Exception as exc:  # DB not reachable yet — on-demand path still works
            logging.getLogger("adapters.config").warning(
                "Could not preload externalized provider config at startup: %s", exc
            )
