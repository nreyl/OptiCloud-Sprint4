from django.urls import path

from . import views

urlpatterns = [
    path("health", views.health),
    path("accounts", views.accounts),
    path("accounts/<int:account_id>/health", views.account_health),
    path("runs", views.runs),
    path("trigger", views.trigger),
]
