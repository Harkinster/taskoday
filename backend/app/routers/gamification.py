from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies import ensure_child_access, get_current_user, success_response
from app.models.gamification import ChestInventory
from app.models.task import TaskType
from app.models.user import User
from app.services.gamification_service import (
    GamificationInvalidStateError,
    GamificationNotFoundError,
    InsufficientItemsError,
    build_dragons_payload,
    build_eggs_payload,
    build_inventory_payload,
    build_progress_payload,
    chest_payload,
    complete_task_by_id,
    evolve_dragon,
    hatch_egg,
    list_chests,
    open_chest,
)

router = APIRouter(tags=["gamification"])


@router.get("/children/{child_id}/progress")
def get_child_progress(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)
    return success_response(build_progress_payload(db, child_id))


@router.post("/children/{child_id}/tasks/{task_id}/complete")
def complete_child_task(
    child_id: int,
    task_id: int,
    task_type: TaskType = Query(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_child_access(db, current_user, child_id)
    try:
        payload = complete_task_by_id(
            db,
            child_id=child_id,
            task_type=task_type,
            task_id=task_id,
            completed_by_user_id=current_user.id,
        )
    except GamificationNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Tache introuvable.") from exc

    db.commit()
    return success_response(payload, message="Tache completee.")


@router.get("/children/{child_id}/chests")
def get_child_chests(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)
    chests: list[ChestInventory] = list_chests(db, child_id)
    return success_response({"child_id": child_id, "chests": [chest_payload(chest) for chest in chests]})


@router.post("/children/{child_id}/chests/{chest_id}/open")
def open_child_chest(
    child_id: int,
    chest_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_child_access(db, current_user, child_id)
    try:
        payload = open_chest(db, child_id=child_id, chest_id=chest_id)
    except GamificationNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Coffre introuvable.") from exc
    except GamificationInvalidStateError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Coffre deja ouvert.") from exc

    db.commit()
    return success_response(payload, message="Coffre ouvert.")


@router.get("/children/{child_id}/inventory")
def get_child_inventory(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)
    return success_response(build_inventory_payload(db, child_id))


@router.get("/children/{child_id}/eggs")
def get_child_eggs(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)
    return success_response(build_eggs_payload(db, child_id))


@router.post("/children/{child_id}/eggs/{egg_id}/hatch")
def hatch_child_egg(
    child_id: int,
    egg_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_child_access(db, current_user, child_id)
    try:
        payload = hatch_egg(db, child_id=child_id, egg_id=egg_id)
    except GamificationNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Oeuf introuvable.") from exc
    except InsufficientItemsError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=f"Objets insuffisants: {exc.missing_items}.") from exc
    except GamificationInvalidStateError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Oeuf non incubable.") from exc

    db.commit()
    return success_response(payload, message="Oeuf eclos.")


@router.get("/children/{child_id}/dragons")
def get_child_dragons(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)
    return success_response(build_dragons_payload(db, child_id))


@router.post("/children/{child_id}/dragons/{dragon_id}/evolve")
def evolve_child_dragon(
    child_id: int,
    dragon_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_child_access(db, current_user, child_id)
    try:
        payload = evolve_dragon(db, child_id=child_id, dragon_id=dragon_id)
    except GamificationNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Dragon introuvable.") from exc
    except InsufficientItemsError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=f"Objets insuffisants: {exc.missing_items}.") from exc
    except GamificationInvalidStateError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Dragon deja au stade maximal.") from exc

    db.commit()
    return success_response(payload, message="Dragon evolue.")
