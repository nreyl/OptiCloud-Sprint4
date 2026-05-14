from rest_framework import status
from rest_framework.decorators import api_view
from rest_framework.response import Response

from .base import FetchWindow
from .models import AdapterRun, CloudAccount
from .registry import get_adapter
from .serializers import (
    AdapterRunSerializer,
    CloudAccountSerializer,
    CloudAccountWriteSerializer,
    TriggerRunSerializer,
)


@api_view(["GET"])
def health(_request):
    return Response({"status": "ok", "service": "cloud-adapter"})


@api_view(["GET", "POST"])
def accounts(request):
    if request.method == "GET":
        company = request.query_params.get("company")
        qs = CloudAccount.objects.all()
        if company:
            qs = qs.filter(company_code=company)
        return Response(CloudAccountSerializer(qs, many=True).data)

    serializer = CloudAccountWriteSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    account = serializer.save()
    return Response(CloudAccountSerializer(account).data, status=status.HTTP_201_CREATED)


@api_view(["POST"])
def trigger(request):
    serializer = TriggerRunSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    payload = serializer.validated_data
    try:
        account = CloudAccount.objects.get(pk=payload["account_id"], active=True)
    except CloudAccount.DoesNotExist:
        return Response({"error": "account not found or inactive"}, status=status.HTTP_404_NOT_FOUND)

    adapter = get_adapter(account)
    window = FetchWindow(payload["period_start"], payload["period_end"])
    run = adapter.run(window)
    return Response(AdapterRunSerializer(run).data, status=status.HTTP_200_OK)


@api_view(["GET"])
def runs(request):
    qs = AdapterRun.objects.order_by("-started_at")
    company = request.query_params.get("company")
    if company:
        qs = qs.filter(company_code=company)
    return Response(AdapterRunSerializer(qs[:100], many=True).data)


@api_view(["GET"])
def account_health(_request, account_id: int):
    try:
        account = CloudAccount.objects.get(pk=account_id)
    except CloudAccount.DoesNotExist:
        return Response({"error": "not found"}, status=status.HTTP_404_NOT_FOUND)
    return Response(get_adapter(account).health())
