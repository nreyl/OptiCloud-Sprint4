"""Base classes for cloud provider adapters.

`BaseCloudAdapter` implements the Microservice Chassis pattern described in the
component diagram: it owns logging, health, validation against the canonical
schema, and dispatch to the normalization service. Concrete adapters (e.g.
`AwsAdapter`) only have to implement `fetch_raw_lines()` returning the
provider-specific shape.
"""

from __future__ import annotations

import abc
import logging
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any

import requests
from django.conf import settings
from django.utils import timezone as dj_tz

from .models import AdapterRun, CloudAccount


@dataclass
class FetchWindow:
    period_start: datetime
    period_end: datetime


class DispatchError(Exception):
    def __init__(self, status: int, detail: str):
        super().__init__(f"{status}: {detail}")
        self.status = status
        self.detail = detail


class BaseCloudAdapter(abc.ABC):
    provider: str = "base"

    def __init__(self, account: CloudAccount):
        self.account = account
        self.logger = logging.getLogger(f"adapters.{self.provider}")

    # ----- chassis methods -------------------------------------------------
    def health(self) -> dict[str, Any]:
        return {
            "provider": self.provider,
            "account_id": self.account.account_id,
            "company_code": self.account.company_code,
            "active": self.account.active,
        }

    def run(self, window: FetchWindow) -> AdapterRun:
        run = AdapterRun.objects.create(
            account=self.account,
            company_code=self.account.company_code,
            provider=self.provider,
            status="OK",
        )
        try:
            self.logger.info(
                "Starting %s fetch for %s/%s window=%s..%s",
                self.provider,
                self.account.company_code,
                self.account.account_id,
                window.period_start,
                window.period_end,
            )
            raw_lines = self.fetch_raw_lines(window)
            payload = self._build_payload(raw_lines, window)
            run.lines_fetched = len(raw_lines)

            self._validate(payload)
            self._dispatch(payload)
            run.status = "OK"
        except DispatchError as exc:
            run.status = "DISPATCH_FAILED"
            run.detail = exc.detail
            self.logger.warning("Dispatch failed: %s", exc)
        except ValueError as exc:
            run.status = "VALIDATION_FAILED"
            run.detail = str(exc)
            self.logger.warning("Validation failed: %s", exc)
        except Exception as exc:  # last-resort safety net for the chassis
            run.status = "ERROR"
            run.detail = repr(exc)
            self.logger.exception("Unexpected adapter error")
        finally:
            run.finished_at = dj_tz.now()
            run.save()
        return run

    # ----- subclasses implement -------------------------------------------
    @abc.abstractmethod
    def fetch_raw_lines(self, window: FetchWindow) -> list[dict[str, Any]]:
        ...

    # ----- internal helpers -----------------------------------------------
    def _build_payload(
        self, raw_lines: list[dict[str, Any]], window: FetchWindow
    ) -> dict[str, Any]:
        return {
            "companyCode": self.account.company_code,
            "provider": self.provider,
            "accountId": self.account.account_id,
            "periodStart": window.period_start.astimezone(timezone.utc).isoformat(),
            "periodEnd": window.period_end.astimezone(timezone.utc).isoformat(),
            "source": "CLOUD_ADAPTER",
            "rawLines": raw_lines,
        }

    def _validate(self, payload: dict[str, Any]) -> None:
        required = ["companyCode", "provider", "periodStart", "periodEnd", "rawLines"]
        for key in required:
            if key not in payload:
                raise ValueError(f"missing field: {key}")
        if not isinstance(payload["rawLines"], list):
            raise ValueError("rawLines must be a list")

    def _dispatch(self, payload: dict[str, Any]) -> dict[str, Any]:
        url = f"{settings.NORMALIZATION_SERVICE_URL}/reports"
        try:
            resp = requests.post(
                url,
                json=payload,
                headers={"Content-Type": "application/json"},
                timeout=settings.REQUEST_TIMEOUT_SECONDS,
            )
        except requests.RequestException as exc:
            raise DispatchError(503, f"normalization-service unreachable: {exc}") from exc
        if resp.status_code >= 400:
            raise DispatchError(resp.status_code, resp.text)
        return resp.json() if resp.content else {}
