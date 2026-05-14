from rest_framework import serializers


class ReportLineSerializer(serializers.Serializer):
    service = serializers.CharField(max_length=100)
    resource_id = serializers.CharField(max_length=200)
    region = serializers.CharField(max_length=64, required=False, allow_blank=True)
    usage_amount = serializers.FloatField()
    usage_unit = serializers.CharField(max_length=32)
    cost_amount = serializers.FloatField()
    cost_currency = serializers.CharField(max_length=8, default="USD")


class IngestReportSerializer(serializers.Serializer):
    company_code = serializers.CharField(max_length=64)
    provider = serializers.ChoiceField(choices=["aws", "azure", "gcp", "other"])
    period_start = serializers.DateTimeField()
    period_end = serializers.DateTimeField()
    lines = ReportLineSerializer(many=True)

    def validate(self, attrs):
        if attrs["period_end"] <= attrs["period_start"]:
            raise serializers.ValidationError("period_end must be after period_start")
        if not attrs["lines"]:
            raise serializers.ValidationError("lines must not be empty")
        return attrs
