from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.child import ChildProfile
from app.models.gamification import (
    ChestInventory,
    ChestStatus,
    ChestType,
    ChildDragon,
    ChildEgg,
    ChildEggStatus,
    DragonDefinition,
    DragonStage,
    EggDefinition,
    GuardianProgress,
    ItemInventory,
)
from app.models.reward import ScaleTransactionType
from app.models.task import Mission, Quest, Routine, TaskCompletion, TaskStatus, TaskType
from app.models.xp import XpHistory
from app.services.scales_service import create_scale_transaction_if_missing, get_scales_balance
from app.services.xp_service import add_xp


CHEST_POINTS_REQUIRED = 5

ITEM_CATALOG = {
    "pomme_dragon": {"title": "Pomme dragon", "rarity": "common"},
    "petit_cristal": {"title": "Petit cristal", "rarity": "common"},
    "pierre_chaude": {"title": "Pierre chaude", "rarity": "common"},
    "plume_douce": {"title": "Plume douce", "rarity": "common"},
    "rune_ancienne": {"title": "Rune ancienne", "rarity": "rare"},
    "fragment_oeuf": {"title": "Fragment d'oeuf", "rarity": "rare"},
}

EGG_CATALOG = {
    "oeuf_braise": "Oeuf braise",
    "oeuf_lunaire": "Oeuf lunaire",
    "oeuf_racine": "Oeuf racine",
}

DRAGON_CATALOG = {
    "dragon_braise": "Dragon braise",
}

EGG_HATCH_REQUIREMENTS = {
    "oeuf_braise": {
        "pomme_dragon": 3,
        "petit_cristal": 2,
        "pierre_chaude": 1,
    }
}

DRAGON_EVOLUTION_REQUIREMENTS = {
    DragonStage.BABY: {
        "next_stage": DragonStage.YOUNG,
        "items": {
            "pomme_dragon": 5,
            "petit_cristal": 4,
            "rune_ancienne": 2,
        },
    },
    DragonStage.YOUNG: {
        "next_stage": DragonStage.GUARDIAN,
        "items": {
            "pomme_dragon": 10,
            "petit_cristal": 8,
            "rune_ancienne": 5,
        },
    },
}


@dataclass(frozen=True)
class TaskReward:
    guardian_xp: int
    flammeches: int
    chest_points: int
    guaranteed_chest: ChestType | None = None


TASK_REWARDS = {
    TaskType.ROUTINE: TaskReward(guardian_xp=5, flammeches=2, chest_points=1),
    TaskType.MISSION: TaskReward(guardian_xp=15, flammeches=6, chest_points=3),
    TaskType.QUEST: TaskReward(guardian_xp=30, flammeches=12, chest_points=0, guaranteed_chest=ChestType.RARE),
}


class GamificationError(Exception):
    pass


class GamificationNotFoundError(GamificationError):
    pass


class GamificationInvalidStateError(GamificationError):
    pass


class InsufficientItemsError(GamificationError):
    def __init__(self, missing_items: dict[str, int]) -> None:
        self.missing_items = missing_items
        super().__init__(f"Missing items: {missing_items}")


def ensure_catalog_seeded(db: Session) -> None:
    for egg_key, title in EGG_CATALOG.items():
        if db.get(EggDefinition, egg_key) is None:
            db.add(EggDefinition(key=egg_key, title=title, is_active=True))

    for dragon_key, title in DRAGON_CATALOG.items():
        if db.get(DragonDefinition, dragon_key) is None:
            db.add(DragonDefinition(key=dragon_key, title=title, is_active=True))

    db.flush()


def get_or_create_progress(db: Session, child_id: int) -> GuardianProgress:
    progress = db.get(GuardianProgress, child_id)
    if progress is None:
        progress = GuardianProgress(child_id=child_id, guardian_xp=0, chest_points=0, opened_chests=0)
        db.add(progress)
        db.flush()
    return progress


def award_task_completion(
    db: Session,
    *,
    child_id: int,
    task_type: TaskType,
    task_id: int,
    title: str,
) -> dict:
    ensure_catalog_seeded(db)
    progress = get_or_create_progress(db, child_id)
    reward = TASK_REWARDS[task_type]
    source_type = f"guardian_{task_type.value}"
    source_id = task_id

    existing_xp = db.scalar(
        select(XpHistory).where(
            XpHistory.child_id == child_id,
            XpHistory.source_type == source_type,
            XpHistory.source_id == source_id,
        )
    )
    if existing_xp is not None:
        return {"awarded": False, "progress": build_progress_payload(db, child_id)}

    profile, history = add_xp(
        db,
        child_id=child_id,
        amount=reward.guardian_xp,
        reason=f"{task_type.value}: {title}",
        source_type=source_type,
        source_id=source_id,
    )

    create_scale_transaction_if_missing(
        db,
        child_id=child_id,
        amount=reward.flammeches,
        reason=f"{task_type.value}: {title}",
        source_type=task_type.value,
        source_id=task_id,
        transaction_type=ScaleTransactionType.AWARD,
        event_key=f"{task_type.value}:{task_id}:completion",
    )

    progress.guardian_xp = profile.xp
    progress.chest_points += reward.chest_points
    granted_chests: list[ChestInventory] = []
    while progress.chest_points >= CHEST_POINTS_REQUIRED:
        progress.chest_points -= CHEST_POINTS_REQUIRED
        granted_chests.append(
            create_chest(
                db,
                child_id=child_id,
                chest_type=ChestType.SIMPLE,
                source_type="chest_points",
                source_id=task_id,
            )
        )

    if reward.guaranteed_chest is not None:
        granted_chests.append(
            create_chest(
                db,
                child_id=child_id,
                chest_type=reward.guaranteed_chest,
                source_type=task_type.value,
                source_id=task_id,
            )
        )

    db.add(progress)
    db.flush()
    return {
        "awarded": True,
        "guardian_xp_awarded": history.amount,
        "flammeches_awarded": reward.flammeches,
        "chest_points_awarded": reward.chest_points,
        "granted_chests": [chest_payload(chest) for chest in granted_chests],
        "progress": build_progress_payload(db, child_id),
    }


def create_chest(
    db: Session,
    *,
    child_id: int,
    chest_type: ChestType,
    source_type: str | None,
    source_id: int | None,
) -> ChestInventory:
    chest = ChestInventory(
        child_id=child_id,
        chest_type=chest_type,
        status=ChestStatus.UNOPENED,
        source_type=source_type,
        source_id=source_id,
    )
    db.add(chest)
    db.flush()
    return chest


def list_chests(db: Session, child_id: int) -> list[ChestInventory]:
    return list(
        db.scalars(
            select(ChestInventory)
            .where(ChestInventory.child_id == child_id)
            .order_by(ChestInventory.status.asc(), ChestInventory.created_at.desc(), ChestInventory.id.desc())
        ).all()
    )


def open_chest(db: Session, *, child_id: int, chest_id: int) -> dict:
    ensure_catalog_seeded(db)
    chest = db.get(ChestInventory, chest_id)
    if chest is None or chest.child_id != child_id:
        raise GamificationNotFoundError()
    if chest.status != ChestStatus.UNOPENED:
        raise GamificationInvalidStateError()

    loot = loot_for_chest(chest.chest_type)
    for item_key, quantity in loot.items():
        add_item(db, child_id=child_id, item_key=item_key, quantity=quantity)

    granted_egg = None
    if chest.chest_type == ChestType.RARE:
        granted_egg = grant_egg_if_missing(db, child_id=child_id, egg_key="oeuf_braise")

    chest.status = ChestStatus.OPENED
    chest.opened_at = datetime.now(timezone.utc)
    progress = get_or_create_progress(db, child_id)
    progress.opened_chests += 1
    db.add(chest)
    db.add(progress)
    db.flush()

    return {
        "chest": chest_payload(chest),
        "loot": [item_payload(item) for item in get_items_by_keys(db, child_id, loot.keys())],
        "granted_egg": child_egg_payload(granted_egg) if granted_egg else None,
        "progress": build_progress_payload(db, child_id),
    }


def loot_for_chest(chest_type: ChestType) -> dict[str, int]:
    if chest_type == ChestType.RARE:
        return {
            "pomme_dragon": 8,
            "petit_cristal": 6,
            "pierre_chaude": 1,
            "rune_ancienne": 3,
            "fragment_oeuf": 1,
        }
    return {
        "pomme_dragon": 2,
        "petit_cristal": 1,
        "plume_douce": 1,
    }


def add_item(db: Session, *, child_id: int, item_key: str, quantity: int) -> ItemInventory:
    item = db.scalar(select(ItemInventory).where(ItemInventory.child_id == child_id, ItemInventory.item_key == item_key))
    if item is None:
        item = ItemInventory(child_id=child_id, item_key=item_key, quantity=0)
        db.add(item)
    item.quantity += quantity
    db.flush()
    return item


def consume_items(db: Session, *, child_id: int, requirements: dict[str, int]) -> None:
    items = {
        item.item_key: item
        for item in db.scalars(select(ItemInventory).where(ItemInventory.child_id == child_id)).all()
    }
    missing = {
        item_key: required - items.get(item_key, ItemInventory(quantity=0)).quantity
        for item_key, required in requirements.items()
        if items.get(item_key) is None or items[item_key].quantity < required
    }
    if missing:
        raise InsufficientItemsError(missing)

    for item_key, required in requirements.items():
        items[item_key].quantity -= required
        db.add(items[item_key])
    db.flush()


def grant_egg_if_missing(db: Session, *, child_id: int, egg_key: str) -> ChildEgg | None:
    ensure_catalog_seeded(db)
    existing = db.scalar(select(ChildEgg).where(ChildEgg.child_id == child_id, ChildEgg.egg_key == egg_key))
    if existing is not None:
        return None

    child_egg = ChildEgg(child_id=child_id, egg_key=egg_key, status=ChildEggStatus.AVAILABLE)
    db.add(child_egg)
    db.flush()
    return child_egg


def hatch_egg(db: Session, *, child_id: int, egg_id: int) -> dict:
    ensure_catalog_seeded(db)
    child_egg = db.get(ChildEgg, egg_id)
    if child_egg is None or child_egg.child_id != child_id:
        raise GamificationNotFoundError()
    if child_egg.status == ChildEggStatus.HATCHED:
        raise GamificationInvalidStateError()

    requirements = EGG_HATCH_REQUIREMENTS.get(child_egg.egg_key)
    if requirements is None:
        raise GamificationInvalidStateError()

    consume_items(db, child_id=child_id, requirements=requirements)
    child_egg.status = ChildEggStatus.HATCHED
    child_egg.hatched_at = datetime.now(timezone.utc)
    dragon = grant_dragon_if_missing(db, child_id=child_id, dragon_key="dragon_braise")
    db.add(child_egg)
    db.flush()
    return {
        "egg": child_egg_payload(child_egg),
        "dragon": child_dragon_payload(dragon),
        "inventory": build_inventory_payload(db, child_id),
        "progress": build_progress_payload(db, child_id),
    }


def grant_dragon_if_missing(db: Session, *, child_id: int, dragon_key: str) -> ChildDragon:
    ensure_catalog_seeded(db)
    existing = db.scalar(select(ChildDragon).where(ChildDragon.child_id == child_id, ChildDragon.dragon_key == dragon_key))
    if existing is not None:
        return existing

    dragon = ChildDragon(child_id=child_id, dragon_key=dragon_key, stage=DragonStage.BABY)
    db.add(dragon)
    db.flush()
    return dragon


def evolve_dragon(db: Session, *, child_id: int, dragon_id: int) -> dict:
    dragon = db.get(ChildDragon, dragon_id)
    if dragon is None or dragon.child_id != child_id:
        raise GamificationNotFoundError()

    evolution = DRAGON_EVOLUTION_REQUIREMENTS.get(dragon.stage)
    if evolution is None:
        raise GamificationInvalidStateError()

    consume_items(db, child_id=child_id, requirements=evolution["items"])
    dragon.stage = evolution["next_stage"]
    db.add(dragon)
    db.flush()
    return {
        "dragon": child_dragon_payload(dragon),
        "inventory": build_inventory_payload(db, child_id),
        "progress": build_progress_payload(db, child_id),
    }


def build_progress_payload(db: Session, child_id: int) -> dict:
    ensure_catalog_seeded(db)
    progress = get_or_create_progress(db, child_id)
    profile = db.scalar(select(ChildProfile).where(ChildProfile.user_id == child_id))
    if profile is not None and progress.guardian_xp != profile.xp:
        progress.guardian_xp = profile.xp
        db.add(progress)
        db.flush()

    nest_level, nest_name = resolve_nest(db, child_id=child_id, guardian_xp=progress.guardian_xp)
    unopened_chests = int(
        db.scalar(
            select(func.count(ChestInventory.id)).where(
                ChestInventory.child_id == child_id,
                ChestInventory.status == ChestStatus.UNOPENED,
            )
        )
        or 0
    )

    return {
        "child_id": child_id,
        "guardian": {
            "xp": progress.guardian_xp,
            "level": profile.level if profile is not None else max(1, progress.guardian_xp // 100 + 1),
        },
        "wallet": {
            "flammeches": get_scales_balance(db, child_id),
        },
        "nest": {
            "level": nest_level,
            "name": nest_name,
        },
        "chest_progress": {
            "points": progress.chest_points,
            "points_required": CHEST_POINTS_REQUIRED,
            "opened_chests": progress.opened_chests,
            "unopened_chests": unopened_chests,
        },
    }


def resolve_nest(db: Session, *, child_id: int, guardian_xp: int) -> tuple[int, str]:
    has_egg = (
        db.scalar(select(func.count(ChildEgg.id)).where(ChildEgg.child_id == child_id, ChildEgg.status != ChildEggStatus.LOCKED))
        or 0
    ) > 0
    has_dragon_braise = (
        db.scalar(select(func.count(ChildDragon.id)).where(ChildDragon.child_id == child_id, ChildDragon.dragon_key == "dragon_braise"))
        or 0
    ) > 0

    if guardian_xp >= 600 and has_dragon_braise:
        return 4, "Sanctuaire du dragon"
    if guardian_xp >= 300 and has_egg:
        return 3, "Grotte aux oeufs"
    if guardian_xp >= 100:
        return 2, "Nid reveille"
    return 1, "Vieux Nid"


def build_inventory_payload(db: Session, child_id: int) -> dict:
    items = list(
        db.scalars(select(ItemInventory).where(ItemInventory.child_id == child_id).order_by(ItemInventory.item_key.asc())).all()
    )
    return {
        "child_id": child_id,
        "items": [item_payload(item) for item in items],
    }


def build_eggs_payload(db: Session, child_id: int) -> dict:
    ensure_catalog_seeded(db)
    eggs = list(db.scalars(select(ChildEgg).where(ChildEgg.child_id == child_id).order_by(ChildEgg.id.asc())).all())
    return {
        "child_id": child_id,
        "eggs": [child_egg_payload(egg) for egg in eggs],
    }


def build_dragons_payload(db: Session, child_id: int) -> dict:
    ensure_catalog_seeded(db)
    dragons = list(db.scalars(select(ChildDragon).where(ChildDragon.child_id == child_id).order_by(ChildDragon.id.asc())).all())
    return {
        "child_id": child_id,
        "dragons": [child_dragon_payload(dragon) for dragon in dragons],
    }


def get_items_by_keys(db: Session, child_id: int, item_keys) -> list[ItemInventory]:
    return list(
        db.scalars(
            select(ItemInventory)
            .where(ItemInventory.child_id == child_id, ItemInventory.item_key.in_(list(item_keys)))
            .order_by(ItemInventory.item_key.asc())
        ).all()
    )


def chest_payload(chest: ChestInventory) -> dict:
    return {
        "id": chest.id,
        "child_id": chest.child_id,
        "type": chest.chest_type.value,
        "status": chest.status.value,
        "source_type": chest.source_type,
        "source_id": chest.source_id,
        "created_at": chest.created_at.isoformat() if chest.created_at else None,
        "opened_at": chest.opened_at.isoformat() if chest.opened_at else None,
    }


def item_payload(item: ItemInventory) -> dict:
    metadata = ITEM_CATALOG.get(item.item_key, {"title": item.item_key, "rarity": "common"})
    return {
        "key": item.item_key,
        "title": metadata["title"],
        "rarity": metadata["rarity"],
        "quantity": item.quantity,
    }


def child_egg_payload(egg: ChildEgg) -> dict:
    return {
        "id": egg.id,
        "child_id": egg.child_id,
        "egg_key": egg.egg_key,
        "title": EGG_CATALOG.get(egg.egg_key, egg.egg_key),
        "status": egg.status.value,
        "obtained_at": egg.obtained_at.isoformat() if egg.obtained_at else None,
        "hatched_at": egg.hatched_at.isoformat() if egg.hatched_at else None,
        "requirements": EGG_HATCH_REQUIREMENTS.get(egg.egg_key, {}),
    }


def child_dragon_payload(dragon: ChildDragon) -> dict:
    return {
        "id": dragon.id,
        "child_id": dragon.child_id,
        "dragon_key": dragon.dragon_key,
        "title": DRAGON_CATALOG.get(dragon.dragon_key, dragon.dragon_key),
        "stage": dragon.stage.value,
        "next_evolution": DRAGON_EVOLUTION_REQUIREMENTS.get(dragon.stage),
    }


def complete_task_by_id(
    db: Session,
    *,
    child_id: int,
    task_type: TaskType,
    task_id: int,
    completed_by_user_id: int,
) -> dict:
    model_by_type = {
        TaskType.ROUTINE: Routine,
        TaskType.MISSION: Mission,
        TaskType.QUEST: Quest,
    }
    item = db.get(model_by_type[task_type], task_id)
    if item is None or item.child_id != child_id:
        raise GamificationNotFoundError()

    completion = db.scalar(
        select(TaskCompletion).where(
            TaskCompletion.task_type == task_type,
            TaskCompletion.task_id == task_id,
            TaskCompletion.child_id == child_id,
        )
    )
    if completion is None:
        completion = TaskCompletion(
            task_type=task_type,
            task_id=task_id,
            child_id=child_id,
            completed_by_user_id=completed_by_user_id,
        )
        db.add(completion)
        if task_type in {TaskType.MISSION, TaskType.QUEST}:
            item.status = TaskStatus.COMPLETED
        award = award_task_completion(db, child_id=child_id, task_type=task_type, task_id=task_id, title=item.title)
    else:
        award = {"awarded": False, "progress": build_progress_payload(db, child_id)}

    db.flush()
    return {
        "task_type": task_type.value,
        "task_id": task_id,
        "completed": True,
        "award": award,
    }
