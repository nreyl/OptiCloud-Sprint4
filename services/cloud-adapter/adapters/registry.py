from .aws import AwsAdapter
from .base import BaseCloudAdapter
from .models import CloudAccount


ADAPTER_REGISTRY: dict[str, type[BaseCloudAdapter]] = {
    "aws": AwsAdapter,
}


def get_adapter(account: CloudAccount) -> BaseCloudAdapter:
    adapter_cls = ADAPTER_REGISTRY.get(account.provider)
    if adapter_cls is None:
        raise ValueError(f"No adapter registered for provider '{account.provider}'")
    return adapter_cls(account)
