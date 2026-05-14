import logging
import smtplib
from email.message import EmailMessage
from typing import Iterable

from .config import settings
from .schemas import SecurityIncident

logger = logging.getLogger("notification.email")


class EmailDispatcher:
    """Sends incident emails in the background. Falls back to logging when
    SMTP credentials are not configured, so the service is safe to deploy in
    development without external dependencies."""

    def __init__(self) -> None:
        self._configured = bool(settings.smtp_user and settings.smtp_password)
        if not self._configured:
            logger.warning(
                "SMTP credentials not configured. EmailDispatcher will log incidents instead of sending them."
            )

    def dispatch(self, incident: SecurityIncident, recipients: Iterable[str]) -> None:
        subject = f"[OptiCloud] Security incident: {incident.incident_type}"
        body = self._render_body(incident)
        recipients = list(recipients)

        if not self._configured or settings.log_smtp_only:
            logger.warning("Security incident (logged, not emailed): subject=%s body=%s recipients=%s", subject, body, recipients)
            return

        msg = EmailMessage()
        msg["Subject"] = subject
        msg["From"] = f"{settings.sender_name} <{settings.sender_email}>"
        msg["To"] = ", ".join(recipients)
        msg.set_content(body)

        try:
            with smtplib.SMTP(settings.smtp_host, settings.smtp_port, timeout=10) as smtp:
                if settings.smtp_use_tls:
                    smtp.starttls()
                smtp.login(settings.smtp_user, settings.smtp_password)
                smtp.send_message(msg)
            logger.info("Incident email sent to %s", recipients)
        except Exception:
            logger.exception("Failed to send incident email")

    @staticmethod
    def _render_body(incident: SecurityIncident) -> str:
        return (
            "A security incident has been detected in OptiCloud.\n\n"
            f"Type:        {incident.incident_type}\n"
            f"Occurred at: {incident.occurred_at.isoformat()}\n"
            f"Company:     {incident.company_id or '-'}\n"
            f"User:        {incident.user_id or '-'}\n"
            f"Source IP:   {incident.source_ip or '-'}\n"
            f"Path:        {incident.path or '-'}\n"
            f"Detail:      {incident.detail}\n"
        )


email_dispatcher = EmailDispatcher()
