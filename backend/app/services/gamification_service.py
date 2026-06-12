from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone

from sqlalchemy import func, select, update
from sqlalchemy.orm import Session

from app.models.child import ChildProfile
from app.models.gamification import (
    ChestInventory,
    ChestStatus,
    ChestType,
    ChildDragon,
    ChildEgg,
    ChildEggStatus,
    ChildWallet,
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
    "pomme_dragon": {"title": "Pomme dragon", "rarity": "common", "category": "consumable"},
    "petit_cristal": {"title": "Petit cristal", "rarity": "common", "category": "material"},
    "pierre_chaude": {"title": "Pierre chaude", "rarity": "common", "category": "material"},
    "plume_douce": {"title": "Plume douce", "rarity": "common", "category": "material"},
    "rune_ancienne": {"title": "Rune ancienne", "rarity": "rare", "category": "artifact"},
    "fragment_oeuf": {"title": "Fragment d'oeuf", "rarity": "rare", "category": "fragment"},
    "essence_braise": {"title": "Essence de braise", "rarity": "rare", "category": "fragment"},
    "essence_lunaire": {"title": "Essence lunaire", "rarity": "epic", "category": "fragment"},
    "artefact_lunaire": {"title": "Artefact lunaire", "rarity": "legendary", "category": "artifact"},
    "artefact_racine": {"title": "Artefact racine", "rarity": "legendary", "category": "artifact"},
}

DRAGON_FAMILIES = {
    "braise": {
        "name": "Braise",
        "element": "fire",
        "egg_key": "oeuf_braise",
        "dragon_key": "dragon_braise",
        "legendary_artifact": "rune_ancienne",
    },
    "lunaire": {
        "name": "Lunaire",
        "element": "moon",
        "egg_key": "oeuf_lunaire",
        "dragon_key": "dragon_lunaire",
        "legendary_artifact": "artefact_lunaire",
    },
    "racine": {
        "name": "Racine",
        "element": "nature",
        "egg_key": "oeuf_racine",
        "dragon_key": "dragon_racine",
        "legendary_artifact": "artefact_racine",
    },
}

EGG_CATALOG = {family["egg_key"]: f"Oeuf {family['name'].lower()}" for family in DRAGON_FAMILIES.values()}
DRAGON_CATALOG = {family["dragon_key"]: f"Dragon {family['name'].lower()}" for family in DRAGON_FAMILIES.values()}

EGG_STATES = ("sleeping", "warm", "glowing", "cracked", "hatching")
DRAGON_STAGES = ("baby", "young", "medium", "large", "legendary")

EGG_STATE_LABELS = {
    "sleeping": "Endormi",
    "warm": "Tiède",
    "glowing": "Lumineux",
    "cracked": "Fissuré",
    "hatching": "Prêt à éclore",
}

EGG_HATCH_REQUIREMENTS = {
    "oeuf_braise": {
        "pomme_dragon": 3,
        "petit_cristal": 2,
        "pierre_chaude": 1,
    },
    "oeuf_lunaire": {
        "pomme_dragon": 6,
        "petit_cristal": 5,
        "rune_ancienne": 3,
    },
    "oeuf_racine": {
        "pomme_dragon": 4,
        "pierre_chaude": 3,
        "rune_ancienne": 2,
    },
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
        "next_stage": DragonStage.MEDIUM,
        "items": {
            "pomme_dragon": 10,
            "petit_cristal": 8,
            "rune_ancienne": 5,
        },
    },
    DragonStage.MEDIUM: {
        "next_stage": DragonStage.LARGE,
        "items": {"pomme_dragon": 15, "petit_cristal": 12, "rune_ancienne": 8},
    },
    DragonStage.LARGE: {
        "next_stage": DragonStage.LEGENDARY,
        "items": {"pomme_dragon": 20, "petit_cristal": 16, "rune_ancienne": 12},
    },
}

CHEST_CATALOG = {
    "common": {
        "name": "Coffre commun",
        "rarity": "common",
        "crystal_cost": 3,
        "description": "Un petit coffre de materiaux utiles.",
        "chest_type": ChestType.SIMPLE,
        "possible_rewards": ["pomme_dragon", "petit_cristal", "plume_douce"],
    },
    "rare": {
        "name": "Coffre rare",
        "rarity": "rare",
        "crystal_cost": 8,
        "description": "Un coffre rare pouvant reveler un oeuf de braise.",
        "chest_type": ChestType.RARE,
        "possible_rewards": [
            "pomme_dragon",
            "petit_cristal",
            "pierre_chaude",
            "rune_ancienne",
            "fragment_oeuf",
            "oeuf_braise",
            "essence_braise",
        ],
    },
    "epic": {
        "name": "Coffre epique",
        "rarity": "epic",
        "crystal_cost": 15,
        "description": "Un coffre epique pouvant reveler un oeuf lunaire.",
        "chest_type": ChestType.EPIC,
        "possible_rewards": [
            "pomme_dragon",
            "petit_cristal",
            "rune_ancienne",
            "fragment_oeuf",
            "artefact_lunaire",
            "oeuf_lunaire",
            "essence_lunaire",
        ],
    },
}


@dataclass(frozen=True)
class TaskReward:
    guardian_xp: int
    flammeches: int
    crystals: int
    chest_points: int
    guaranteed_chest: ChestType | None = None


TASK_REWARDS = {
    TaskType.ROUTINE: TaskReward(guardian_xp=5, flammeches=2, crystals=1, chest_points=1),
    TaskType.MISSION: TaskReward(guardian_xp=15, flammeches=6, crystals=3, chest_points=3),
    TaskType.QUEST: TaskReward(
        guardian_xp=30,
        flammeches=12,
        crystals=6,
        chest_points=0,
        guaranteed_chest=ChestType.RARE,
    ),
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


class InsufficientCrystalsError(GamificationError):
    def __init__(self, balance: int, required: int) -> None:
        self.balance = balance
        self.required = required
        super().__init__(f"Insufficient crystals: balance={balance}, required={required}")


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


def get_or_create_wallet(db: Session, child_id: int) -> ChildWallet:
    wallet = db.get(ChildWallet, child_id)
    if wallet is None:
        get_scales_balance(db, child_id)
        wallet = db.get(ChildWallet, child_id)
    if wallet is None:
        raise GamificationInvalidStateError()
    return wallet


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

    wallet = get_or_create_wallet(db, child_id)
    wallet.crystals_balance += reward.crystals
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
    db.add(wallet)
    db.flush()
    return {
        "awarded": True,
        "guardian_xp_awarded": history.amount,
        "flammeches_awarded": reward.flammeches,
        "crystals_awarded": reward.crystals,
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


def build_chest_catalog_payload(db: Session, child_id: int) -> dict:
    wallet = get_or_create_wallet(db, child_id)
    return {
        "child_id": child_id,
        "crystals_balance": wallet.crystals_balance,
        "chests": [chest_catalog_payload(chest_id) for chest_id in CHEST_CATALOG],
    }


def open_catalog_chest(db: Session, *, child_id: int, catalog_id: str) -> dict:
    catalog_entry = CHEST_CATALOG.get(catalog_id)
    if catalog_entry is None:
        raise GamificationNotFoundError()

    wallet = get_or_create_wallet(db, child_id)
    crystal_cost = catalog_entry["crystal_cost"]
    debit = db.execute(
        update(ChildWallet)
        .where(
            ChildWallet.child_id == child_id,
            ChildWallet.crystals_balance >= crystal_cost,
        )
        .values(crystals_balance=ChildWallet.crystals_balance - crystal_cost)
        .execution_options(synchronize_session=False)
    )
    db.refresh(wallet)
    if debit.rowcount != 1:
        raise InsufficientCrystalsError(balance=wallet.crystals_balance, required=crystal_cost)

    chest = create_chest(
        db,
        child_id=child_id,
        chest_type=catalog_entry["chest_type"],
        source_type="cavern",
        source_id=None,
    )
    result = open_chest(db, child_id=child_id, chest_id=chest.id)
    result["crystals_spent"] = crystal_cost
    result["crystals_balance"] = wallet.crystals_balance
    result["catalog_chest"] = chest_catalog_payload(catalog_id)
    db.add(wallet)
    db.flush()
    return result


def open_chest(db: Session, *, child_id: int, chest_id: int) -> dict:
    ensure_catalog_seeded(db)
    chest = db.get(ChestInventory, chest_id)
    if chest is None or chest.child_id != child_id:
        raise GamificationNotFoundError()
    if chest.status != ChestStatus.UNOPENED:
        raise GamificationInvalidStateError()

    loot = loot_for_chest(chest.chest_type)
    loot_gains = []
    for item_key, quantity in loot.items():
        item = add_item(db, child_id=child_id, item_key=item_key, quantity=quantity)
        loot_gains.append(loot_gain_payload(item, quantity=quantity))

    granted_egg = None
    duplicate_compensation = None
    egg_key = egg_key_for_chest(chest.chest_type)
    if egg_key is not None:
        granted_egg, duplicate_compensation = grant_egg_or_compensation(db, child_id=child_id, egg_key=egg_key)
        if duplicate_compensation is not None:
            compensated_item = db.scalar(
                select(ItemInventory).where(
                    ItemInventory.child_id == child_id,
                    ItemInventory.item_key == duplicate_compensation["item"]["key"],
                )
            )
            if compensated_item is not None:
                loot_gains.append(
                    loot_gain_payload(
                        compensated_item,
                        quantity=duplicate_compensation["quantity_awarded"],
                        is_duplicate_compensation=True,
                    )
                )

    chest.status = ChestStatus.OPENED
    chest.opened_at = datetime.now(timezone.utc)
    progress = get_or_create_progress(db, child_id)
    progress.opened_chests += 1
    db.add(chest)
    db.add(progress)
    db.flush()

    return {
        "chest": chest_payload(chest),
        "loot": loot_gains,
        "granted_egg": child_egg_payload(db, granted_egg) if granted_egg else None,
        "duplicate_compensation": duplicate_compensation,
        "inventory_after": build_inventory_payload(db, child_id),
        "progress": build_progress_payload(db, child_id),
    }


def loot_for_chest(chest_type: ChestType) -> dict[str, int]:
    if chest_type == ChestType.EPIC:
        return {
            "pomme_dragon": 14,
            "petit_cristal": 10,
            "rune_ancienne": 7,
            "fragment_oeuf": 3,
            "artefact_lunaire": 1,
        }
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


def egg_key_for_chest(chest_type: ChestType) -> str | None:
    if chest_type == ChestType.RARE:
        return "oeuf_braise"
    if chest_type == ChestType.EPIC:
        return "oeuf_lunaire"
    return None


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
    existing_dragon = db.scalar(
        select(ChildDragon).where(
            ChildDragon.child_id == child_id,
            ChildDragon.dragon_key == dragon_key_for_egg(egg_key),
        )
    )
    if existing is not None or existing_dragon is not None:
        return None

    child_egg = ChildEgg(
        child_id=child_id,
        egg_key=egg_key,
        status=ChildEggStatus.AVAILABLE,
        egg_state=EGG_STATES[0],
        progress=0,
    )
    db.add(child_egg)
    db.flush()
    return child_egg


def grant_egg_or_compensation(db: Session, *, child_id: int, egg_key: str) -> tuple[ChildEgg | None, dict | None]:
    granted_egg = grant_egg_if_missing(db, child_id=child_id, egg_key=egg_key)
    if granted_egg is not None:
        return granted_egg, None

    family_id = family_id_for_egg(egg_key)
    item_key = f"essence_{family_id}" if f"essence_{family_id}" in ITEM_CATALOG else "fragment_oeuf"
    item = add_item(db, child_id=child_id, item_key=item_key, quantity=5)
    return None, {
        "reason": "duplicate_egg_family",
        "family_id": family_id,
        "item": item_payload(item),
        "quantity_awarded": 5,
    }


def evolve_egg(db: Session, *, child_id: int, egg_id: int) -> dict:
    child_egg = db.get(ChildEgg, egg_id)
    if child_egg is None or child_egg.child_id != child_id:
        raise GamificationNotFoundError()
    if child_egg.status == ChildEggStatus.HATCHED:
        raise GamificationInvalidStateError()
    if child_egg.egg_state == EGG_STATES[-1]:
        return hatch_egg(db, child_id=child_id, egg_id=egg_id)

    consume_items(db, child_id=child_id, requirements={"fragment_oeuf": 1})
    next_index = EGG_STATES.index(child_egg.egg_state) + 1
    child_egg.egg_state = EGG_STATES[next_index]
    child_egg.progress = int(next_index / (len(EGG_STATES) - 1) * 100)
    if child_egg.egg_state == EGG_STATES[-1]:
        child_egg.status = ChildEggStatus.READY
    db.add(child_egg)
    db.flush()
    return {
        "egg": child_egg_payload(db, child_egg),
        "hatched": False,
        "dragon": None,
        "inventory": build_inventory_payload(db, child_id),
    }


def hatch_egg(db: Session, *, child_id: int, egg_id: int) -> dict:
    ensure_catalog_seeded(db)
    child_egg = db.get(ChildEgg, egg_id)
    if child_egg is None or child_egg.child_id != child_id:
        raise GamificationNotFoundError()
    if child_egg.status == ChildEggStatus.HATCHED or child_egg.egg_state != EGG_STATES[-1]:
        raise GamificationInvalidStateError()

    requirements = EGG_HATCH_REQUIREMENTS.get(child_egg.egg_key)
    if requirements is None:
        raise GamificationInvalidStateError()

    consume_items(db, child_id=child_id, requirements=requirements)
    child_egg.status = ChildEggStatus.HATCHED
    child_egg.egg_state = EGG_STATES[-1]
    child_egg.progress = 100
    child_egg.hatched_at = datetime.now(timezone.utc)
    dragon = grant_dragon_if_missing(db, child_id=child_id, dragon_key=dragon_key_for_egg(child_egg.egg_key))
    db.add(child_egg)
    db.flush()
    return {
        "egg": child_egg_payload(db, child_egg),
        "hatched": True,
        "dragon": child_dragon_payload(dragon),
        "inventory": build_inventory_payload(db, child_id),
        "progress": build_progress_payload(db, child_id),
    }


def grant_dragon_if_missing(db: Session, *, child_id: int, dragon_key: str) -> ChildDragon:
    ensure_catalog_seeded(db)
    existing = db.scalar(select(ChildDragon).where(ChildDragon.child_id == child_id, ChildDragon.dragon_key == dragon_key))
    if existing is not None:
        return existing

    dragon = ChildDragon(
        child_id=child_id,
        dragon_key=dragon_key,
        stage=DragonStage.BABY,
        progress=0,
        active_companion=False,
    )
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
    dragon.progress = dragon_progress_percent(dragon.stage)
    db.add(dragon)
    db.flush()
    return {
        "dragon": child_dragon_payload(dragon),
        "inventory": build_inventory_payload(db, child_id),
        "progress": build_progress_payload(db, child_id),
    }


def set_active_companion(db: Session, *, child_id: int, dragon_id: int) -> dict:
    dragon = db.get(ChildDragon, dragon_id)
    if dragon is None or dragon.child_id != child_id:
        raise GamificationNotFoundError()

    dragons = db.scalars(select(ChildDragon).where(ChildDragon.child_id == child_id)).all()
    for child_dragon in dragons:
        child_dragon.active_companion = child_dragon.id == dragon_id
        db.add(child_dragon)
    db.flush()
    return {
        "child_id": child_id,
        "active_companion": child_dragon_payload(dragon),
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
            "crystals": get_or_create_wallet(db, child_id).crystals_balance,
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
    wallet = get_or_create_wallet(db, child_id)
    items = list(
        db.scalars(select(ItemInventory).where(ItemInventory.child_id == child_id).order_by(ItemInventory.item_key.asc())).all()
    )
    chests = list_chests(db, child_id)
    return {
        "child_id": child_id,
        "currencies": {
            "flammeches": get_scales_balance(db, child_id),
            "crystals": wallet.crystals_balance,
        },
        "items": [item_payload(item) for item in items],
        "chests": [chest_payload(chest) for chest in chests if chest.status == ChestStatus.UNOPENED],
    }


def build_eggs_payload(db: Session, child_id: int) -> dict:
    ensure_catalog_seeded(db)
    eggs = list(db.scalars(select(ChildEgg).where(ChildEgg.child_id == child_id).order_by(ChildEgg.id.asc())).all())
    return {
        "child_id": child_id,
        "eggs": [child_egg_payload(db, egg) for egg in eggs],
    }


def build_dragons_payload(db: Session, child_id: int) -> dict:
    ensure_catalog_seeded(db)
    dragons = list(db.scalars(select(ChildDragon).where(ChildDragon.child_id == child_id).order_by(ChildDragon.id.asc())).all())
    return {
        "child_id": child_id,
        "dragons": [child_dragon_payload(dragon) for dragon in dragons],
        "active_companion": next(
            (child_dragon_payload(dragon) for dragon in dragons if dragon.active_companion),
            None,
        ),
    }


def build_bestiary_payload(db: Session, child_id: int) -> dict:
    ensure_catalog_seeded(db)
    eggs = {egg.egg_key: egg for egg in db.scalars(select(ChildEgg).where(ChildEgg.child_id == child_id)).all()}
    dragons = {
        dragon.dragon_key: dragon
        for dragon in db.scalars(select(ChildDragon).where(ChildDragon.child_id == child_id)).all()
    }
    items = {
        item.item_key: item.quantity
        for item in db.scalars(select(ItemInventory).where(ItemInventory.child_id == child_id)).all()
    }

    families = []
    for family_id, family in DRAGON_FAMILIES.items():
        egg = eggs.get(family["egg_key"])
        dragon = dragons.get(family["dragon_key"])
        egg_state = egg.egg_state if egg else None
        dragon_stage = dragon.stage.value if dragon else None
        artifact_key = family["legendary_artifact"]
        egg_payload = child_egg_payload(db, egg, items=items) if egg else None
        families.append(
            {
                "family_id": family_id,
                "family_name": family["name"],
                "element": family["element"],
                "discovered": egg is not None or dragon is not None,
                "egg_owned": egg is not None,
                "dragon_owned": dragon is not None,
                "current_egg_state": egg_state,
                "current_dragon_stage": dragon_stage,
                "active_companion": bool(dragon and dragon.active_companion),
                "legendary_unlocked": bool(dragon and dragon.stage == DragonStage.LEGENDARY),
                "progress_percent": bestiary_progress_percent(egg, dragon),
                "egg_progress_percent": egg.progress if egg else None,
                "next_egg_state": egg_payload["next_state"] if egg_payload else None,
                "required_resources": egg_payload["required_resources"] if egg_payload else [],
                "can_evolve": egg_payload["can_evolve"] if egg_payload else False,
                "egg": egg_payload,
                "egg_asset_key": f"egg_{family_id}_{egg_state or 'locked'}",
                "dragon_asset_key": f"dragon_{family_id}_{dragon_stage or 'locked'}",
                "egg_states": state_unlocks(EGG_STATES, egg_state),
                "dragon_stages": state_unlocks(DRAGON_STAGES, dragon_stage),
                "legendary_artifact": {
                    "item_key": artifact_key,
                    "required": 1,
                    "owned": items.get(artifact_key, 0),
                },
            }
        )
    return {"child_id": child_id, "families": families}


def get_items_by_keys(db: Session, child_id: int, item_keys) -> list[ItemInventory]:
    return list(
        db.scalars(
            select(ItemInventory)
            .where(ItemInventory.child_id == child_id, ItemInventory.item_key.in_(list(item_keys)))
            .order_by(ItemInventory.item_key.asc())
        ).all()
    )


def chest_payload(chest: ChestInventory) -> dict:
    catalog_id = catalog_id_for_chest_type(chest.chest_type)
    catalog = CHEST_CATALOG[catalog_id]
    return {
        "id": chest.id,
        "child_id": chest.child_id,
        "type": chest.chest_type.value,
        "name": catalog["name"],
        "rarity": catalog["rarity"],
        "crystal_cost": 0 if chest.source_type != "cavern" else catalog["crystal_cost"],
        "description": catalog["description"],
        "possible_rewards": catalog["possible_rewards"],
        "status": chest.status.value,
        "source_type": chest.source_type,
        "source_id": chest.source_id,
        "created_at": chest.created_at.isoformat() if chest.created_at else None,
        "opened_at": chest.opened_at.isoformat() if chest.opened_at else None,
    }


def item_payload(item: ItemInventory) -> dict:
    metadata = ITEM_CATALOG.get(item.item_key, {"title": item.item_key, "rarity": "common", "category": "material"})
    return {
        "key": item.item_key,
        "title": metadata["title"],
        "rarity": metadata["rarity"],
        "category": metadata["category"],
        "quantity": item.quantity,
    }


def loot_gain_payload(item: ItemInventory, *, quantity: int, is_duplicate_compensation: bool = False) -> dict:
    payload = item_payload(item)
    return {
        **payload,
        "name": payload["title"],
        "quantity": quantity,
        "quantity_total": item.quantity,
        "is_duplicate_compensation": is_duplicate_compensation,
    }


def child_egg_payload(db: Session, egg: ChildEgg, *, items: dict[str, int] | None = None) -> dict:
    current_index = EGG_STATES.index(egg.egg_state)
    next_state = EGG_STATES[current_index + 1] if current_index + 1 < len(EGG_STATES) else None
    requirements = egg_next_action_requirements(egg)
    if items is None:
        items = {
            item.item_key: item.quantity
            for item in db.scalars(select(ItemInventory).where(ItemInventory.child_id == egg.child_id)).all()
        }
    required_resources = [
        {
            "item_key": item_key,
            "title": ITEM_CATALOG.get(item_key, {"title": item_key})["title"],
            "owned_quantity": items.get(item_key, 0),
            "required_quantity": required_quantity,
            "is_satisfied": items.get(item_key, 0) >= required_quantity,
        }
        for item_key, required_quantity in requirements.items()
    ]
    return {
        "id": egg.id,
        "child_id": egg.child_id,
        "egg_key": egg.egg_key,
        "title": EGG_CATALOG.get(egg.egg_key, egg.egg_key),
        "status": egg.status.value,
        "state": egg.egg_state,
        "current_state": egg.egg_state,
        "current_state_label": EGG_STATE_LABELS[egg.egg_state],
        "progress_percent": egg.progress,
        "next_state": next_state,
        "next_state_label": EGG_STATE_LABELS[next_state] if next_state else None,
        "required_resources": required_resources,
        "can_evolve": egg.status != ChildEggStatus.HATCHED
        and bool(requirements)
        and all(resource["is_satisfied"] for resource in required_resources),
        "asset_key": f"{egg.egg_key}_{egg.egg_state}",
        "obtained_at": egg.obtained_at.isoformat() if egg.obtained_at else None,
        "hatched_at": egg.hatched_at.isoformat() if egg.hatched_at else None,
        "requirements": requirements,
    }


def egg_next_action_requirements(egg: ChildEgg) -> dict[str, int]:
    if egg.status == ChildEggStatus.HATCHED:
        return {}
    if egg.egg_state == EGG_STATES[-1]:
        return EGG_HATCH_REQUIREMENTS.get(egg.egg_key, {})
    return {"fragment_oeuf": 1}


def child_dragon_payload(dragon: ChildDragon) -> dict:
    return {
        "id": dragon.id,
        "child_id": dragon.child_id,
        "dragon_key": dragon.dragon_key,
        "title": DRAGON_CATALOG.get(dragon.dragon_key, dragon.dragon_key),
        "stage": dragon.stage.value,
        "progress_percent": dragon.progress,
        "active_companion": dragon.active_companion,
        "asset_key": f"{dragon.dragon_key}_{dragon.stage.value}",
        "next_evolution": DRAGON_EVOLUTION_REQUIREMENTS.get(dragon.stage),
    }


def chest_catalog_payload(catalog_id: str) -> dict:
    entry = CHEST_CATALOG[catalog_id]
    return {
        "id": catalog_id,
        "name": entry["name"],
        "rarity": entry["rarity"],
        "crystal_cost": entry["crystal_cost"],
        "description": entry["description"],
        "possible_rewards": entry["possible_rewards"],
    }


def catalog_id_for_chest_type(chest_type: ChestType) -> str:
    if chest_type == ChestType.SIMPLE:
        return "common"
    return chest_type.value


def family_id_for_egg(egg_key: str) -> str:
    for family_id, family in DRAGON_FAMILIES.items():
        if family["egg_key"] == egg_key:
            return family_id
    raise GamificationInvalidStateError()


def dragon_key_for_egg(egg_key: str) -> str:
    return DRAGON_FAMILIES[family_id_for_egg(egg_key)]["dragon_key"]


def dragon_progress_percent(stage: DragonStage) -> int:
    if stage.value not in DRAGON_STAGES:
        return 100
    return int(DRAGON_STAGES.index(stage.value) / (len(DRAGON_STAGES) - 1) * 100)


def bestiary_progress_percent(egg: ChildEgg | None, dragon: ChildDragon | None) -> int:
    if dragon is not None:
        stage = dragon.stage.value
        if stage not in DRAGON_STAGES:
            return 100
        return 50 + int(DRAGON_STAGES.index(stage) / (len(DRAGON_STAGES) - 1) * 50)
    if egg is not None:
        return egg.progress
    return 0


def state_unlocks(states: tuple[str, ...], current: str | None) -> list[dict]:
    current_index = states.index(current) if current in states else -1
    return [{"state": state, "unlocked": index <= current_index} for index, state in enumerate(states)]


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
