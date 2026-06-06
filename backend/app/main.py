from fastapi import FastAPI, HTTPException, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.core.config import settings
from app.routers import auth, children, families, gamification, missions, pairing, profile, quests, rewards, routines


def _error_code_from_status(status_code: int) -> str:
    mapping = {
        400: "BAD_REQUEST",
        401: "UNAUTHORIZED",
        403: "FORBIDDEN",
        404: "NOT_FOUND",
        409: "CONFLICT",
        422: "VALIDATION_ERROR",
        500: "INTERNAL_ERROR",
    }
    return mapping.get(status_code, "ERROR")


def _error_response(status_code: int, message: str) -> JSONResponse:
    return JSONResponse(
        status_code=status_code,
        content={
            "success": False,
            "error": {
                "code": _error_code_from_status(status_code),
                "message": message,
            },
        },
    )


def create_application() -> FastAPI:
    application = FastAPI(title=settings.app_name, version=settings.app_version, debug=settings.app_debug)

    application.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    @application.exception_handler(HTTPException)
    async def http_exception_handler(_: Request, exc: HTTPException):
        message = exc.detail if isinstance(exc.detail, str) else "Une erreur est survenue."
        return _error_response(exc.status_code, message)

    @application.exception_handler(RequestValidationError)
    async def validation_exception_handler(_: Request, exc: RequestValidationError):
        message = "Requete invalide."
        if exc.errors():
            first = exc.errors()[0]
            field = ".".join(str(x) for x in first.get("loc", []) if x != "body")
            detail = first.get("msg")
            if field:
                message = f"Champ '{field}': {detail}"
            elif detail:
                message = detail

        return _error_response(status.HTTP_422_UNPROCESSABLE_ENTITY, message)

    @application.exception_handler(Exception)
    async def generic_exception_handler(_: Request, __: Exception):
        return _error_response(status.HTTP_500_INTERNAL_SERVER_ERROR, "Erreur interne du serveur.")

    @application.get("/health")
    def healthcheck():
        return {
            "success": True,
            "data": {
                "status": "ok",
                "app": settings.app_name,
                "version": settings.app_version,
            },
            "message": None,
        }

    application.include_router(auth.router, prefix=settings.api_v1_prefix)
    application.include_router(families.router, prefix=settings.api_v1_prefix)
    application.include_router(pairing.router, prefix=settings.api_v1_prefix)
    application.include_router(children.router, prefix=settings.api_v1_prefix)
    application.include_router(routines.router, prefix=settings.api_v1_prefix)
    application.include_router(missions.router, prefix=settings.api_v1_prefix)
    application.include_router(quests.router, prefix=settings.api_v1_prefix)
    application.include_router(gamification.router, prefix=settings.api_v1_prefix)
    application.include_router(rewards.router, prefix=settings.api_v1_prefix)
    application.include_router(profile.router, prefix=settings.api_v1_prefix)

    return application


app = create_application()
