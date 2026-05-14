from datetime import datetime
from typing import Literal, Optional
from pydantic import BaseModel, Field


class SecurityIncident(BaseModel):
    incident_type: Literal[
        "UNAUTHORIZED_ACCESS",
        "INVALID_TOKEN",
        "COMPANY_MISMATCH",
        "EXCESSIVE_FAILED_LOGIN",
    ]
    company_id: Optional[str] = None
    user_id: Optional[str] = None
    source_ip: Optional[str] = None
    path: Optional[str] = None
    detail: str = Field(default="")
    occurred_at: datetime = Field(default_factory=datetime.utcnow)


class IncidentAck(BaseModel):
    status: str = "queued"
    dispatched_to: list[str]
