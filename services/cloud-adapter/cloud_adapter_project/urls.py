from django.conf import settings
from django.urls import include, path

# This container serves exactly one provider, mounted under its own path
# segment (e.g. /adapters/aws/...). Kong routes /adapters/<provider>/* to the
# matching container, so a new provider is a new container + a new Kong route,
# with no change to the routes of the existing ones.
urlpatterns = [
    path(f"adapters/{settings.ADAPTER_PROVIDER}/", include("adapters.urls")),
]
