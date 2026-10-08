from __future__ import annotations

import unicodedata

from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.models.gamification import ChildDragon, DragonDefinition, DragonStage
from app.models.user import User


LINEAGES: tuple[dict[str, str], ...] = (
    {"id": "FULMIO", "founder_name": "Fulmio", "region_name": "Hautes-Tempêtes", "affinity": "tempest"},
    {"id": "SYLVYN", "founder_name": "Sylvyn", "region_name": "Sylveracine", "affinity": "root"},
    {"id": "PHENOR", "founder_name": "Phenor", "region_name": "Soléa", "affinity": "solar"},
    {"id": "LUNARYS", "founder_name": "Lunarys", "region_name": "Clairlune", "affinity": "lunar"},
    {"id": "PYRON", "founder_name": "Pyron", "region_name": "Braisecime", "affinity": "ember"},
    {"id": "CHRONYX", "founder_name": "Chronyx", "region_name": "Chronéa", "affinity": "chronos"},
    {"id": "AMBRIO", "founder_name": "Ambrio", "region_name": "Ambrelande", "affinity": "heart"},
    {"id": "CRISTAO", "founder_name": "Cristao", "region_name": "Cristalia", "affinity": "crystal"},
)
LINEAGE_BY_ID = {entry["id"]: entry for entry in LINEAGES}
STARTER_KEY = "INITIAL"
MAX_NAME_LENGTH = 32


class CompanionError(Exception):
    def __init__(self, code: str, message: str, status_code: int) -> None:
        self.code = code
        self.message = message
        self.status_code = status_code
        super().__init__(message)


def validate_companion_name(value: str) -> str:
    name = unicodedata.normalize("NFC", value).strip()
    if not name or len(name) > MAX_NAME_LENGTH:
        raise CompanionError("INVALID_NAME", "Le nom doit contenir de 1 à 32 caractères.", 422)
    for char in name:
        category = unicodedata.category(char)
        if category.startswith("C") or not (
            category.startswith(("L", "M", "N")) or char in " '-’"
        ):
            raise CompanionError("INVALID_NAME", "Le nom contient un caractère non autorisé.", 422)
    if not any(unicodedata.category(char).startswith("L") for char in name):
        raise CompanionError("INVALID_NAME", "Le nom doit contenir au moins une lettre.", 422)
    return name


def ensure_companion_catalog(db: Session) -> None:
    for lineage in LINEAGES:
        key = f"dragon_{lineage['id'].lower()}"
        if db.get(DragonDefinition, key) is None:
            db.add(DragonDefinition(key=key, title=f"Compagnon de lignée {lineage['founder_name']}", is_active=True))
    db.flush()


def lineage_payloads() -> list[dict]:
    return [dict(entry) for entry in LINEAGES]


def companion_payload(dragon: ChildDragon) -> dict:
    definition = dragon.dragon_key
    lineage = LINEAGE_BY_ID.get(dragon.lineage_id or "")
    return {
        "dragon_id": dragon.id,
        "owner_user_id": dragon.child_id,
        "dragon_key": definition,
        "lineage_id": dragon.lineage_id,
        "lineage_status": "OFFICIAL" if lineage else "LEGACY_UNKNOWN",
        "lineage": dict(lineage) if lineage else None,
        "display_name": dragon.display_name,
        "stage": dragon.stage.value,
        "progress": dragon.progress,
        "active": dragon.active_companion,
    }


def get_my_companions(db: Session, owner_user_id: int) -> dict:
    dragons = db.scalars(
        select(ChildDragon).where(ChildDragon.child_id == owner_user_id).order_by(ChildDragon.id.asc())
    ).all()
    active = [dragon for dragon in dragons if dragon.active_companion]
    return {
        "companions": [companion_payload(dragon) for dragon in dragons],
        "active_companion": companion_payload(active[0]) if len(active) == 1 else None,
        "active_state": "ACTIVE" if len(active) == 1 else ("NONE" if not active else "LEGACY_CONFLICT"),
    }


def get_my_active_companion(db: Session, owner_user_id: int) -> dict | None:
    payload = get_my_companions(db, owner_user_id)
    return payload["active_companion"]


def _locked_owner(db: Session, owner_user_id: int) -> User:
    # A no-op row update is a portable transaction lock on SQLite as well as
    # MySQL/PostgreSQL; SQLite ignores SELECT FOR UPDATE.
    db.execute(update(User).where(User.id == owner_user_id).values(id=User.id))
    owner = db.scalar(select(User).where(User.id == owner_user_id).with_for_update())
    if owner is None:
        raise CompanionError("OWNER_NOT_FOUND", "Utilisateur introuvable.", 404)
    return owner


def _activate(db: Session, owner_user_id: int, dragon: ChildDragon) -> None:
    db.execute(
        update(ChildDragon)
        .where(ChildDragon.child_id == owner_user_id)
        .values(active_companion=False)
    )
    dragon.active_companion = True
    db.add(dragon)
    db.flush()


def create_starter(db: Session, *, owner_user_id: int, lineage_id: str, display_name: str) -> dict:
    lineage_id = lineage_id.strip().upper()
    if lineage_id not in LINEAGE_BY_ID:
        raise CompanionError("UNKNOWN_LINEAGE", "Cette lignée n'existe pas.", 422)
    display_name = validate_companion_name(display_name)
    _locked_owner(db, owner_user_id)

    existing_starter = db.scalar(
        select(ChildDragon).where(ChildDragon.child_id == owner_user_id, ChildDragon.starter_key == STARTER_KEY)
    )
    if existing_starter is not None:
        if existing_starter.lineage_id == lineage_id and existing_starter.display_name == display_name:
            return {"companion": companion_payload(existing_starter), "created": False, "reused_existing": True}
        raise CompanionError("STARTER_ALREADY_ASSIGNED", "Le compagnon de départ a déjà été choisi.", 409)

    dragons = db.scalars(
        select(ChildDragon).where(ChildDragon.child_id == owner_user_id).order_by(ChildDragon.id.asc())
    ).all()
    if dragons:
        active = [dragon for dragon in dragons if dragon.active_companion]
        if len(active) > 1:
            raise CompanionError("ACTIVE_COMPANION_CONFLICT", "Plusieurs compagnons historiques sont actifs.", 409)
        dragon = active[0] if active else dragons[0]
        if not active:
            _activate(db, owner_user_id, dragon)
        return {"companion": companion_payload(dragon), "created": False, "reused_existing": True}

    ensure_companion_catalog(db)
    dragon = ChildDragon(
        child_id=owner_user_id,
        dragon_key=f"dragon_{lineage_id.lower()}",
        lineage_id=lineage_id,
        display_name=display_name,
        starter_key=STARTER_KEY,
        stage=DragonStage.BABY,
        progress=0,
        active_companion=True,
    )
    db.add(dragon)
    db.flush()
    return {"companion": companion_payload(dragon), "created": True, "reused_existing": False}


def activate_my_companion(db: Session, *, owner_user_id: int, dragon_id: int) -> dict:
    _locked_owner(db, owner_user_id)
    dragon = db.scalar(
        select(ChildDragon).where(ChildDragon.id == dragon_id, ChildDragon.child_id == owner_user_id)
    )
    if dragon is None:
        raise CompanionError("COMPANION_NOT_OWNED", "Compagnon introuvable.", 404)
    _activate(db, owner_user_id, dragon)
    return {"companion": companion_payload(dragon)}


def rename_my_companion(db: Session, *, owner_user_id: int, dragon_id: int, display_name: str) -> dict:
    name = validate_companion_name(display_name)
    dragon = db.scalar(
        select(ChildDragon).where(ChildDragon.id == dragon_id, ChildDragon.child_id == owner_user_id)
    )
    if dragon is None:
        raise CompanionError("COMPANION_NOT_OWNED", "Compagnon introuvable.", 404)
    dragon.display_name = name
    db.add(dragon)
    db.flush()
    return {"companion": companion_payload(dragon)}
