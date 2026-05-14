from django.urls import include, path

urlpatterns = [
    path("ingest/", include("ingestion.urls")),
]
