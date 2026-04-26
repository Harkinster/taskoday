from fastapi import FastAPI

from app.api.routes.auth import router as auth_router
from app.api.routes.children import router as children_router
from app.api.routes.families import router as families_router
from app.api.routes.health import router as health_router
from app.api.routes.planning import router as planning_router
from app.api.routes.points import router as points_router
from app.api.routes.rewards import router as rewards_router
from app.core.config import settings


def create_application() -> FastAPI:
    app = FastAPI(
        title=settings.app_name,
        debug=settings.app_debug,
        version=settings.app_version,
    )
    app.include_router(health_router)
    app.include_router(auth_router)
    app.include_router(families_router)
    app.include_router(children_router)
    app.include_router(planning_router)
    app.include_router(points_router)
    app.include_router(rewards_router)
    return app


app = create_application()
