import logging
from typing import Any

import requests
from django.conf import settings

logger = logging.getLogger(__name__)


class ReportsServiceError(Exception):
    def __init__(self, status: int, detail: str):
        super().__init__(f"{status}: {detail}")
        self.status = status
        self.detail = detail


def forward_report(payload: dict[str, Any], authorization: str | None) -> dict[str, Any]:
    headers = {"Content-Type": "application/json"}
    if authorization:
        headers["Authorization"] = authorization
    url = f"{settings.REPORTS_SERVICE_URL}/commands/reports"
    logger.info("Forwarding ingestion payload to %s", url)
    try:
        resp = requests.post(
            url,
            json=payload,
            headers=headers,
            timeout=settings.REQUEST_TIMEOUT_SECONDS,
        )
    except requests.RequestException as exc:
        raise ReportsServiceError(503, f"reports-service unreachable: {exc}") from exc
    if resp.status_code >= 400:
        raise ReportsServiceError(resp.status_code, resp.text)
    if resp.headers.get("content-type", "").startswith("application/json"):
        return resp.json()
    return {"raw": resp.text}
