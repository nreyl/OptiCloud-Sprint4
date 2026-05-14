from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", case_sensitive=False, extra="ignore")

    app_host: str = "0.0.0.0"
    app_port: int = 8080

    smtp_host: str = "smtp.gmail.com"
    smtp_port: int = 587
    smtp_user: str = ""
    smtp_password: str = ""
    smtp_use_tls: bool = True
    sender_email: str = "noreply@opticloud.local"
    sender_name: str = "OptiCloud Security"

    security_recipients: str = "security@opticloud.local"
    log_smtp_only: bool = True


settings = Settings()
