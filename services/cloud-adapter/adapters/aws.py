"""AWS implementation of `BaseCloudAdapter`.

For Sprint 4 the adapter ships with a deterministic synthetic dataset so it
can run end-to-end without real AWS credentials. The structure mirrors what
the AWS Cost Explorer API returns (Service, UsageType, UsageQuantity,
UnblendedCost) so swapping in real boto3 calls later is mechanical.
"""

from __future__ import annotations

from datetime import timedelta
from typing import Any

from .base import BaseCloudAdapter, FetchWindow


class AwsAdapter(BaseCloudAdapter):
    provider = "aws"

    _SAMPLE_SERVICES = [
        ("Amazon Elastic Compute Cloud - Compute", "BoxUsage:t3.small", "Hrs", 24.0, 0.0208),
        ("Amazon Relational Database Service", "InstanceUsage:db.t3.small", "Hrs", 24.0, 0.034),
        ("Amazon Simple Storage Service", "TimedStorage-ByteHrs", "GB-Mo", 120.0, 0.023),
        ("AWS Lambda", "Lambda-GB-Second", "GB-Second", 50000.0, 0.0000166667),
        ("Amazon CloudWatch", "CW:MetricMonitorUsage", "Metrics", 25.0, 0.30),
    ]

    def fetch_raw_lines(self, window: FetchWindow) -> list[dict[str, Any]]:
        hours = max(1.0, (window.period_end - window.period_start) / timedelta(hours=1))
        lines: list[dict[str, Any]] = []
        for service, usage_type, unit, base_qty, unit_price in self._SAMPLE_SERVICES:
            qty = base_qty * (hours / 24.0)
            cost = round(qty * unit_price, 6)
            lines.append(
                {
                    "Service": service,
                    "UsageType": usage_type,
                    "Region": self.account.region or "us-east-1",
                    "ResourceId": f"{self.account.account_id}:{usage_type}",
                    "UsageQuantity": round(qty, 4),
                    "UsageUnit": unit,
                    "UnblendedCost": {"Amount": cost, "Currency": "USD"},
                }
            )
        return lines
