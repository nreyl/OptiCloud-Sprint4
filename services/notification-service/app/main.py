import logging
from fastapi import BackgroundTasks, FastAPI

from .config import settings
from .incident_handler import incident_handler
from .schemas import IncidentAck, SecurityIncident

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")

app = FastAPI(title="OptiCloud Notification Service", version="1.0.0")


@app.get("/notifications/health")
def health() -> dict:
    return {"status": "ok"}


@app.post("/notifications/incidents", response_model=IncidentAck, status_code=202)
def report_incident(incident: SecurityIncident, background_tasks: BackgroundTasks) -> IncidentAck:
    return incident_handler.handle(incident, background_tasks)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host=settings.app_host, port=settings.app_port)
