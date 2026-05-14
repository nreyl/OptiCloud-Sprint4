from django.db import models


class CloudAccount(models.Model):
    """A registered cloud account for a company. Stores the credentials/handles
    that the adapter needs to query the provider. For Sprint 4 we don't store
    real secrets; the access_key/secret_key fields are placeholders so the
    architecture is in place."""

    PROVIDER_CHOICES = [
        ("aws", "AWS"),
        ("azure", "Azure"),
        ("gcp", "GCP"),
    ]

    company_code = models.CharField(max_length=64, db_index=True)
    provider = models.CharField(max_length=16, choices=PROVIDER_CHOICES)
    account_id = models.CharField(max_length=128)
    region = models.CharField(max_length=64, blank=True, default="us-east-1")
    access_key = models.CharField(max_length=256, blank=True, default="")
    secret_key = models.CharField(max_length=256, blank=True, default="")
    active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ("company_code", "provider", "account_id")

    def __str__(self) -> str:
        return f"{self.company_code}/{self.provider}/{self.account_id}"


class AdapterRun(models.Model):
    """Audit trail of every dispatch performed by the adapter."""

    STATUS_CHOICES = [
        ("OK", "OK"),
        ("VALIDATION_FAILED", "VALIDATION_FAILED"),
        ("DISPATCH_FAILED", "DISPATCH_FAILED"),
        ("ERROR", "ERROR"),
    ]

    account = models.ForeignKey(
        CloudAccount, on_delete=models.CASCADE, related_name="runs", null=True, blank=True
    )
    company_code = models.CharField(max_length=64, db_index=True)
    provider = models.CharField(max_length=16)
    status = models.CharField(max_length=32, choices=STATUS_CHOICES)
    lines_fetched = models.PositiveIntegerField(default=0)
    detail = models.TextField(blank=True, default="")
    started_at = models.DateTimeField(auto_now_add=True)
    finished_at = models.DateTimeField(null=True, blank=True)
