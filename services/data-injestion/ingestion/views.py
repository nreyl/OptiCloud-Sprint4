import logging

from rest_framework import status
from rest_framework.decorators import api_view
from rest_framework.response import Response

from .reports_client import ReportsServiceError, forward_report
from .serializers import IngestReportSerializer

logger = logging.getLogger(__name__)


@api_view(["GET"])
def health(request):
    return Response({"status": "ok", "service": "data-injestion"})


@api_view(["POST"])
def ingest_report(request):
    serializer = IngestReportSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    payload = serializer.validated_data

    payload_for_reports = {
        "companyCode": payload["company_code"],
        "provider": payload["provider"],
        "periodStart": payload["period_start"].isoformat(),
        "periodEnd": payload["period_end"].isoformat(),
        "source": "INGESTION",
        "lines": [
            {
                "service": line["service"],
                "resourceId": line["resource_id"],
                "region": line.get("region", ""),
                "usageAmount": line["usage_amount"],
                "usageUnit": line["usage_unit"],
                "costAmount": line["cost_amount"],
                "costCurrency": line["cost_currency"],
            }
            for line in payload["lines"]
        ],
    }

    auth_header = request.headers.get("Authorization")
    try:
        result = forward_report(payload_for_reports, auth_header)
    except ReportsServiceError as exc:
        logger.warning("reports-service rejected ingestion: %s", exc)
        return Response(
            {"error": "reports-service rejected payload", "detail": exc.detail},
            status=status.HTTP_502_BAD_GATEWAY if exc.status >= 500 else exc.status,
        )

    return Response(
        {"accepted": True, "lines": len(payload["lines"]), "reports_service": result},
        status=status.HTTP_202_ACCEPTED,
    )
