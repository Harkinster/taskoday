from fastapi import APIRouter, Depends, status
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session
from starlette.responses import JSONResponse

from app.db.session import get_db
from app.dependencies import get_current_user, success_response
from app.models.user import User
from app.schemas.common import SuccessResponse
from app.schemas.gamification import (
    CompanionMutationResponse,
    CompanionResponse,
    LineageListResponse,
    MyCompanionsResponse,
    RenameCompanionRequest,
    StarterCompanionRequest,
    StarterCompanionResponse,
)
from app.services.companion_service import (
    CompanionError,
    activate_my_companion,
    create_starter,
    get_my_active_companion,
    get_my_companions,
    lineage_payloads,
    rename_my_companion,
    validate_companion_name,
)

router = APIRouter(tags=["Chronodria companions"])


def _companion_error(error: CompanionError) -> JSONResponse:
    return JSONResponse(
        status_code=error.status_code,
        content={"success": False, "error": {"code": error.code, "message": error.message}},
    )


@router.get("/gamification/lineages", response_model=SuccessResponse[LineageListResponse])
def get_official_lineages(current_user: User = Depends(get_current_user)):
    return success_response({"lineages": lineage_payloads()})


@router.get("/me/companions", response_model=SuccessResponse[MyCompanionsResponse])
def get_my_personal_companions(
    db: Session = Depends(get_db), current_user: User = Depends(get_current_user)
):
    return success_response(get_my_companions(db, current_user.id))


@router.get("/me/companions/active", response_model=SuccessResponse[CompanionResponse | None])
def get_my_active_personal_companion(
    db: Session = Depends(get_db), current_user: User = Depends(get_current_user)
):
    return {"success": True, "data": get_my_active_companion(db, current_user.id), "message": None}


@router.post(
    "/me/companions/starter",
    status_code=status.HTTP_201_CREATED,
    response_model=SuccessResponse[StarterCompanionResponse],
)
def create_my_starter_companion(
    request: StarterCompanionRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    try:
        payload = create_starter(
            db,
            owner_user_id=current_user.id,
            lineage_id=request.lineage_id.value,
            display_name=request.display_name,
        )
        db.commit()
        return success_response(payload, message="Compagnon enregistré.")
    except CompanionError as exc:
        db.rollback()
        return _companion_error(exc)
    except IntegrityError:
        db.rollback()
        # A concurrent identical initial choice is an idempotent retry. The DB
        # unique constraint remains the final guard against duplicate starters.
        existing = get_my_companions(db, current_user.id)["companions"]
        normalized_name = validate_companion_name(request.display_name)
        match = next(
            (item for item in existing if item["lineage_id"] == request.lineage_id.value and item["display_name"] == normalized_name),
            None,
        )
        if match is not None:
            return success_response({"companion": match, "created": False, "reused_existing": True})
        return JSONResponse(
            status_code=409,
            content={"success": False, "error": {"code": "STARTER_ALREADY_ASSIGNED", "message": "Le compagnon de départ a déjà été choisi."}},
        )


@router.post("/me/companions/{dragon_id}/activate", response_model=SuccessResponse[CompanionMutationResponse])
def activate_my_personal_companion(
    dragon_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    try:
        payload = activate_my_companion(db, owner_user_id=current_user.id, dragon_id=dragon_id)
        db.commit()
        return success_response(payload, message="Compagnon actif mis à jour.")
    except CompanionError as exc:
        db.rollback()
        return _companion_error(exc)


@router.patch("/me/companions/{dragon_id}/name", response_model=SuccessResponse[CompanionMutationResponse])
def rename_my_personal_companion(
    dragon_id: int,
    request: RenameCompanionRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    try:
        payload = rename_my_companion(
            db,
            owner_user_id=current_user.id,
            dragon_id=dragon_id,
            display_name=request.display_name,
        )
        db.commit()
        return success_response(payload, message="Compagnon renommé.")
    except CompanionError as exc:
        db.rollback()
        return _companion_error(exc)
