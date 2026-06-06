from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies import ensure_child_access, get_current_user, success_response
from app.models.gamification import ChestInventory
from app.models.task import TaskType
from app.models.user import User
from app.schemas.common import SuccessResponse
from app.schemas.gamification import (
    ActiveCompanionResponse,
    BestiaryResponse,
    ChestCatalogResponse,
    CrystalBalanceResponse,
    EggEvolutionResponse,
    InventoryResponse,
    OpenCatalogChestResponse,
)
from app.services.gamification_service import (
    GamificationInvalidStateError,
    GamificationNotFoundError,
    InsufficientCrystalsError,
    InsufficientItemsError,
    build_bestiary_payload,
    build_chest_catalog_payload,
    build_dragons_payload,
    build_eggs_payload,
    build_inventory_payload,
    build_progress_payload,
    chest_payload,
    complete_task_by_id,
    evolve_egg,
    evolve_dragon,
    get_or_create_wallet,
    hatch_egg,
    list_chests,
    open_chest,
    open_catalog_chest,
    set_active_companion,
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


@router.get("/children/{child_id}/chests/catalog", response_model=SuccessResponse[ChestCatalogResponse])
def get_child_chest_catalog(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)
    return success_response(build_chest_catalog_payload(db, child_id))


@router.post(
    "/children/{child_id}/chests/catalog/{catalog_id}/open",
    response_model=SuccessResponse[OpenCatalogChestResponse],
)
def open_child_catalog_chest(
    child_id: int,
    catalog_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_child_access(db, current_user, child_id)
    try:
        payload = open_catalog_chest(db, child_id=child_id, catalog_id=catalog_id)
    except GamificationNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Type de coffre introuvable.") from exc
    except InsufficientCrystalsError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Cristaux insuffisants: balance={exc.balance}, required={exc.required}.",
        ) from exc

    db.commit()
    return success_response(payload, message="Coffre achete et ouvert.")


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


@router.get("/children/{child_id}/inventory", response_model=SuccessResponse[InventoryResponse])
def get_child_inventory(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)
    return success_response(build_inventory_payload(db, child_id))


@router.get("/children/{child_id}/crystals", response_model=SuccessResponse[CrystalBalanceResponse])
def get_child_crystals(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)
    wallet = get_or_create_wallet(db, child_id)
    return success_response({"child_id": child_id, "balance": wallet.crystals_balance, "currency": "crystals"})


@router.get("/children/{child_id}/eggs")
def get_child_eggs(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)
    return success_response(build_eggs_payload(db, child_id))


@router.post("/children/{child_id}/eggs/{egg_id}/evolve", response_model=SuccessResponse[EggEvolutionResponse])
def evolve_child_egg(
    child_id: int,
    egg_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_child_access(db, current_user, child_id)
    try:
        payload = evolve_egg(db, child_id=child_id, egg_id=egg_id)
    except GamificationNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Oeuf introuvable.") from exc
    except InsufficientItemsError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=f"Objets insuffisants: {exc.missing_items}.") from exc
    except GamificationInvalidStateError as exc:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Oeuf deja eclos ou non evolutif.") from exc

    db.commit()
    return success_response(payload, message="Oeuf evolue.")


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
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="L'oeuf doit etre a l'etat hatching.") from exc

    db.commit()
    return success_response(payload, message="Oeuf eclos.")


@router.get("/children/{child_id}/dragons")
def get_child_dragons(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)
    return success_response(build_dragons_payload(db, child_id))


@router.post(
    "/children/{child_id}/dragons/{dragon_id}/activate",
    response_model=SuccessResponse[ActiveCompanionResponse],
)
@router.post(
    "/children/{child_id}/dragons/{dragon_id}/companion",
    response_model=SuccessResponse[ActiveCompanionResponse],
)
def activate_child_dragon(
    child_id: int,
    dragon_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_child_access(db, current_user, child_id)
    try:
        payload = set_active_companion(db, child_id=child_id, dragon_id=dragon_id)
    except GamificationNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Dragon introuvable.") from exc

    db.commit()
    return success_response(payload, message="Compagnon actif mis a jour.")


@router.get("/children/{child_id}/bestiary", response_model=SuccessResponse[BestiaryResponse])
def get_child_bestiary(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)
    return success_response(build_bestiary_payload(db, child_id))


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
