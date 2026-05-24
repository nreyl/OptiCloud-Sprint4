"""Externalized Configuration (ASR 3).

The adapter's provider credentials and account handles live in PostgreSQL, not
in code or config files. They are loaded once at container startup into this
in-memory cache (see ``AdaptersConfig.ready``), so each deployment reads its
configuration at boot rather than reaching into the database cold on every
request. Registering/rotating a provider account is a database change picked
up by ``reload()`` (the accounts endpoint calls it) — never a code change.
"""

from __future__ import annotations

import logging
from threading import RLock
from typing import Any

logger = logging.getLogger("adapters.config")


class ProviderConfig:
    """Process-wide snapshot of the active accounts for this container's provider."""

    def __init__(self) -> None:
        self._lock = RLock()
        self._accounts: dict[int, dict[str, Any]] = {}
        self._loaded = False

    def load(self, provider: str) -> dict[int, dict[str, Any]]:
        from .models import CloudAccount

        with self._lock:
            accounts = CloudAccount.objects.filter(provider=provider, active=True)
            self._accounts = {
                a.id: {
                    "account_id": a.account_id,
                    "company_code": a.company_code,
                    "region": a.region,
                    "has_credentials": bool(a.access_key),
                }
                for a in accounts
            }
            self._loaded = True
        logger.info(
            "Loaded %d externalized %s account(s) from PostgreSQL at startup",
            len(self._accounts),
            provider,
        )
        return self._accounts

    def reload(self, provider: str) -> dict[int, dict[str, Any]]:
        return self.load(provider)

    @property
    def loaded(self) -> bool:
        return self._loaded

    @property
    def count(self) -> int:
        return len(self._accounts)


# Single shared instance for the process.
provider_config = ProviderConfig()
