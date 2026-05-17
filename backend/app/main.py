from fastapi import FastAPI, HTTPException, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.core.config import settings
from app.db.base import Base
from app.db.session import engine
from app.routers import auth, children, families, missions, pairing, profile, quests, routines


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


app = FastAPI(title=settings.app_name, version=settings.app_version, debug=settings.app_debug)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
def on_startup() -> None:
    Base.metadata.create_all(bind=engine)


@app.exception_handler(HTTPException)
async def http_exception_handler(_: Request, exc: HTTPException):
    message = exc.detail if isinstance(exc.detail, str) else "Une erreur est survenue."
    return _error_response(exc.status_code, message)


@app.exception_handler(RequestValidationError)
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

    return _error_response(status.HTTP_400_BAD_REQUEST, message)


@app.exception_handler(Exception)
async def generic_exception_handler(_: Request, __: Exception):
    return _error_response(status.HTTP_500_INTERNAL_SERVER_ERROR, "Erreur interne du serveur.")


@app.get("/health")
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


app.include_router(auth.router, prefix=settings.api_v1_prefix)
app.include_router(families.router, prefix=settings.api_v1_prefix)
app.include_router(pairing.router, prefix=settings.api_v1_prefix)
app.include_router(children.router, prefix=settings.api_v1_prefix)
app.include_router(routines.router, prefix=settings.api_v1_prefix)
app.include_router(missions.router, prefix=settings.api_v1_prefix)
app.include_router(quests.router, prefix=settings.api_v1_prefix)
app.include_router(profile.router, prefix=settings.api_v1_prefix)
