from django.urls import include, path

urlpatterns = [
    path("adapters/", include("adapters.urls")),
]
