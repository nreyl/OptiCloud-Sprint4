from rest_framework import serializers

from .models import AdapterRun, CloudAccount


class CloudAccountSerializer(serializers.ModelSerializer):
    class Meta:
        model = CloudAccount
        fields = [
            "id",
            "company_code",
            "provider",
            "account_id",
            "region",
            "active",
            "created_at",
        ]
        read_only_fields = ["id", "created_at"]


class CloudAccountWriteSerializer(serializers.ModelSerializer):
    class Meta:
        model = CloudAccount
        fields = [
            "company_code",
            "provider",
            "account_id",
            "region",
            "access_key",
            "secret_key",
            "active",
        ]


class AdapterRunSerializer(serializers.ModelSerializer):
    class Meta:
        model = AdapterRun
        fields = [
            "id",
            "company_code",
            "provider",
            "status",
            "lines_fetched",
            "detail",
            "started_at",
            "finished_at",
        ]


class TriggerRunSerializer(serializers.Serializer):
    account_id = serializers.IntegerField()
    period_start = serializers.DateTimeField()
    period_end = serializers.DateTimeField()

    def validate(self, attrs):
        if attrs["period_end"] <= attrs["period_start"]:
            raise serializers.ValidationError("period_end must be after period_start")
        return attrs
