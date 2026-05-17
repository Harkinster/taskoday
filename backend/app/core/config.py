from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "Taskoday API"
    app_env: str = "production"
    app_debug: bool = False
    app_version: str = "0.1.0"

    api_v1_prefix: str = "/api/v1"
    host: str = "127.0.0.1"
    port: int = 8000

    database_url: str = "mysql+pymysql://taskoday_api:CHANGE_ME@127.0.0.1:3306/taskoday"

    jwt_secret_key: str = "CHANGE_ME_LONG_RANDOM_SECRET"
    jwt_algorithm: str = "HS256"
    jwt_expire_minutes: int = 120

    pairing_code_expire_minutes: int = 10

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
