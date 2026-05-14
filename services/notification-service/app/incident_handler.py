import logging
from fastapi import BackgroundTasks

from .config import settings
from .email_dispatcher import email_dispatcher
from .schemas import IncidentAck, SecurityIncident

logger = logging.getLogger("notification.incident")


class SecurityIncidentHandler:
    """Decides what to do with an incoming security incident: who to notify
    and through which channels. For Sprint 4 the only channel is email."""

    def handle(self, incident: SecurityIncident, background_tasks: BackgroundTasks) -> IncidentAck:
        recipients = [r.strip() for r in settings.security_recipients.split(",") if r.strip()]
        logger.info(
            "Handling incident type=%s company=%s user=%s",
            incident.incident_type,
            incident.company_id,
            incident.user_id,
        )
        background_tasks.add_task(email_dispatcher.dispatch, incident, recipients)
        return IncidentAck(dispatched_to=recipients)


incident_handler = SecurityIncidentHandler()
