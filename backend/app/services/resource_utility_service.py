from collections import Counter
from datetime import datetime, timezone
import hashlib
import secrets

from sqlalchemy import func, select, update
from sqlalchemy.orm import Session

from app.models.family import FamilyMember, FamilyMemberRole
from app.models.gamification import ItemInventory
from app.models.reward_grant import RewardGrant, RewardResourceGrant
from app.models.resource_utility import ChestOpen, ChestOpenDrop, RewardResourceSpend, WishOffer, WishRequest
from app.models.user import User
from app.services.gamification_service import ITEM_CATALOG


class ResourceUtilityError(Exception):
    def __init__(self, status_code: int, message: str):
        self.status_code = status_code
        self.message = message
        super().__init__(message)


class ChestRandomSource:
    def choose(self, options: tuple[str, ...]) -> str:
        return secrets.choice(options)


CHEST_POLICY = {
    "COMMON": {"title": "Coffre commun", "crystal_cost": 3, "drop_count": 1,
               "pool": ("pomme_dragon", "petit_cristal", "plume_douce")},
    "RARE": {"title": "Coffre rare", "crystal_cost": 8, "drop_count": 3,
             "pool": ("pomme_dragon", "pierre_chaude", "rune_ancienne", "fragment_oeuf", "essence_braise")},
    "EPIC": {"title": "Coffre épique", "crystal_cost": 15, "drop_count": 6,
             "pool": ("rune_ancienne", "fragment_oeuf", "essence_lunaire", "artefact_lunaire", "artefact_racine")},
}
CHEST_RANDOM = ChestRandomSource()


def _lock_player(db: Session, user_id: int) -> None:
    # MySQL uses a row lock. SQLite has no SELECT FOR UPDATE; a no-op UPDATE
    # obtains its write reservation before balance reads, preventing overspend.
    db.scalar(select(User).where(User.id == user_id).with_for_update())
    if db.bind is not None and db.bind.dialect.name == "sqlite":
        db.execute(update(User).where(User.id == user_id).values(id=User.id))


def require_member(db: Session, family_id: int, user_id: int, *, parent: bool = False) -> FamilyMember:
    member = db.scalar(select(FamilyMember).where(FamilyMember.family_id == family_id, FamilyMember.user_id == user_id))
    if member is None or (parent and member.role != FamilyMemberRole.PARENT):
        raise ResourceUtilityError(404, "Ressource introuvable.")
    return member


def resource_balances(db: Session, *, family_id: int, user_id: int) -> dict:
    points = int(db.scalar(select(func.coalesce(func.sum(RewardGrant.points), 0)).where(
        RewardGrant.family_id == family_id, RewardGrant.beneficiary_user_id == user_id,
        RewardGrant.revoked_at.is_(None))) or 0)
    grants = {kind: int(amount) for kind, amount in db.execute(select(
        RewardResourceGrant.resource_type, func.coalesce(func.sum(RewardResourceGrant.amount), 0)
    ).where(RewardResourceGrant.family_id == family_id, RewardResourceGrant.beneficiary_user_id == user_id,
            RewardResourceGrant.revoked_at.is_(None)).group_by(RewardResourceGrant.resource_type)).all()}
    spends = {kind: int(amount) for kind, amount in db.execute(select(
        RewardResourceSpend.resource_type, func.coalesce(func.sum(RewardResourceSpend.amount), 0)
    ).where(RewardResourceSpend.family_id == family_id, RewardResourceSpend.user_id == user_id,
            RewardResourceSpend.reversed_at.is_(None)).group_by(RewardResourceSpend.resource_type)).all()}
    return {"family_id": family_id, "user_id": user_id, "taskoday_points": points,
            "flames": grants.get("FLAME", 0) - spends.get("FLAME", 0),
            "crystals": grants.get("CRYSTAL", 0) - spends.get("CRYSTAL", 0)}


def spend_resource(db: Session, *, family_id: int, user_id: int, resource_type: str, amount: int,
                   purpose_type: str, purpose_id: int | None, idempotency_key: str) -> RewardResourceSpend:
    if amount < 1 or resource_type not in {"FLAME", "CRYSTAL"}:
        raise ResourceUtilityError(422, "Dépense invalide.")
    # Serialize every spend for one player. The row lock is honored by production MySQL;
    # the unique idempotency constraint remains the final retry guard.
    _lock_player(db, user_id)
    existing = db.scalar(select(RewardResourceSpend).where(
        RewardResourceSpend.user_id == user_id, RewardResourceSpend.idempotency_key == idempotency_key))
    if existing is not None:
        if (existing.family_id, existing.resource_type, existing.amount, existing.purpose_type, existing.purpose_id) != (
            family_id, resource_type, amount, purpose_type, purpose_id
        ):
            raise ResourceUtilityError(409, "Clé de dépense déjà utilisée.")
        return existing
    balance = resource_balances(db, family_id=family_id, user_id=user_id)["flames" if resource_type == "FLAME" else "crystals"]
    if balance < amount:
        raise ResourceUtilityError(409, "Solde insuffisant.")
    spend = RewardResourceSpend(family_id=family_id, user_id=user_id, resource_type=resource_type,
        amount=amount, purpose_type=purpose_type, purpose_id=purpose_id, idempotency_key=idempotency_key)
    db.add(spend)
    db.flush()
    return spend


def create_offer(db: Session, *, family_id: int, user_id: int, title: str,
                 description: str | None, flame_cost: int) -> WishOffer:
    offer = WishOffer(family_id=family_id, created_by=user_id, title=title.strip(),
        description=description.strip() if description else None, flame_cost=flame_cost, active=True)
    db.add(offer); db.flush()
    return offer


def create_wish_request(db: Session, *, family_id: int, user_id: int, offer_id: int) -> WishRequest:
    offer = db.scalar(select(WishOffer).where(WishOffer.id == offer_id, WishOffer.family_id == family_id, WishOffer.active.is_(True)))
    if offer is None:
        raise ResourceUtilityError(404, "Souhait introuvable.")
    request = WishRequest(family_id=family_id, offer_id=offer.id, user_id=user_id,
        title_snapshot=offer.title, flame_cost=offer.flame_cost, status="PENDING")
    db.add(request); db.flush()
    return request


def decide_wish_request(db: Session, *, family_id: int, request_id: int, actor_user_id: int,
                        approve: bool) -> WishRequest:
    request = db.scalar(select(WishRequest).where(WishRequest.id == request_id,
        WishRequest.family_id == family_id).with_for_update())
    if request is None:
        raise ResourceUtilityError(404, "Demande introuvable.")
    if db.bind is not None and db.bind.dialect.name == "sqlite":
        db.execute(update(WishRequest).where(WishRequest.id == request.id).values(id=WishRequest.id))
        db.refresh(request)
    if request.status == "APPROVED" and approve:
        return request
    if request.status == "REJECTED" and not approve:
        return request
    if request.status != "PENDING":
        raise ResourceUtilityError(409, "Cette demande a déjà été traitée.")
    offer = db.get(WishOffer, request.offer_id)
    if offer is None or offer.family_id != family_id:
        raise ResourceUtilityError(404, "Souhait introuvable.")
    if approve:
        spend = spend_resource(db, family_id=family_id, user_id=request.user_id,
            resource_type="FLAME", amount=request.flame_cost, purpose_type="WISH_REQUEST",
            purpose_id=request.id, idempotency_key=f"wish-request:{request.id}:approve")
        request.spend_id = spend.id
        request.status = "APPROVED"
    else:
        request.status = "REJECTED"
    request.decided_by = actor_user_id
    request.decided_at = datetime.now(timezone.utc)
    db.flush()
    return request


def parent_obtain_wish(db: Session, *, family_id: int, user_id: int, offer_id: int,
                       idempotency_key: str) -> WishRequest:
    existing_spend = db.scalar(select(RewardResourceSpend).where(
        RewardResourceSpend.user_id == user_id, RewardResourceSpend.idempotency_key == idempotency_key))
    if existing_spend is not None and existing_spend.purpose_id is not None:
        existing = db.get(WishRequest, existing_spend.purpose_id)
        if existing is not None:
            if existing.family_id != family_id or existing.offer_id != offer_id or existing_spend.purpose_type != "WISH_PARENT_OBTAIN":
                raise ResourceUtilityError(409, "Clé d’obtention déjà utilisée.")
            return existing
    offer = db.scalar(select(WishOffer).where(WishOffer.id == offer_id,
        WishOffer.family_id == family_id, WishOffer.active.is_(True)))
    if offer is None:
        raise ResourceUtilityError(404, "Souhait introuvable.")
    request = WishRequest(family_id=family_id, offer_id=offer.id, user_id=user_id,
        title_snapshot=offer.title, flame_cost=offer.flame_cost, status="APPROVED",
        decided_by=user_id, decided_at=datetime.now(timezone.utc))
    db.add(request); db.flush()
    spend = spend_resource(db, family_id=family_id, user_id=user_id, resource_type="FLAME",
        amount=offer.flame_cost, purpose_type="WISH_PARENT_OBTAIN", purpose_id=request.id,
        idempotency_key=idempotency_key)
    request.spend_id = spend.id
    db.flush()
    return request


def open_chest(db: Session, *, family_id: int, user_id: int, chest_type: str,
               idempotency_key: str, random_source: ChestRandomSource | None = None) -> ChestOpen:
    policy = CHEST_POLICY.get(chest_type)
    if policy is None:
        raise ResourceUtilityError(422, "Type de coffre invalide.")
    existing = db.scalar(select(ChestOpen).where(ChestOpen.user_id == user_id,
        ChestOpen.idempotency_key == idempotency_key))
    if existing is not None:
        if existing.family_id != family_id or existing.chest_type != chest_type:
            raise ResourceUtilityError(409, "Clé d’ouverture déjà utilisée.")
        return existing
    spend = spend_resource(db, family_id=family_id, user_id=user_id, resource_type="CRYSTAL",
        amount=policy["crystal_cost"], purpose_type="CHEST_OPEN", purpose_id=None,
        idempotency_key="chest:" + hashlib.sha256(idempotency_key.encode()).hexdigest())
    # A concurrent retry may have waited on the player's spend lock.
    existing = db.scalar(select(ChestOpen).where(ChestOpen.user_id == user_id,
        ChestOpen.idempotency_key == idempotency_key))
    if existing is not None:
        return existing
    chest = ChestOpen(family_id=family_id, user_id=user_id, chest_type=chest_type,
        crystal_cost=policy["crystal_cost"], drop_count=policy["drop_count"],
        idempotency_key=idempotency_key, spend_id=spend.id)
    db.add(chest); db.flush()
    rng = random_source or CHEST_RANDOM
    counts = Counter(rng.choose(tuple(policy["pool"])) for _ in range(policy["drop_count"]))
    for key, quantity in counts.items():
        db.add(ChestOpenDrop(chest_open_id=chest.id, collectible_key=key, quantity=quantity))
        item = db.scalar(select(ItemInventory).where(ItemInventory.child_id == user_id, ItemInventory.item_key == key))
        if item is None:
            item = ItemInventory(child_id=user_id, item_key=key, quantity=0)
            db.add(item)
        item.quantity += quantity
    db.flush()
    return chest


def chest_payload(db: Session, chest: ChestOpen) -> dict:
    drops = db.scalars(select(ChestOpenDrop).where(ChestOpenDrop.chest_open_id == chest.id).order_by(ChestOpenDrop.collectible_key)).all()
    return {"id": chest.id, "chest_type": chest.chest_type, "crystal_cost": chest.crystal_cost,
        "created_at": chest.created_at, "drops": [{"collectible_key": row.collectible_key,
            "title": ITEM_CATALOG.get(row.collectible_key, {}).get("title", row.collectible_key),
            "quantity": row.quantity} for row in drops]}


def list_chest_opens(db: Session, *, family_id: int, user_id: int, limit: int = 20) -> list[ChestOpen]:
    return list(db.scalars(select(ChestOpen).where(ChestOpen.family_id == family_id,
        ChestOpen.user_id == user_id).order_by(ChestOpen.created_at.desc(), ChestOpen.id.desc()).limit(limit)).all())


def collection_payload(db: Session, *, family_id: int, user_id: int) -> dict:
    rows = db.scalars(select(ItemInventory).where(ItemInventory.child_id == user_id,
        ItemInventory.quantity > 0).order_by(ItemInventory.item_key)).all()
    return {"family_id": family_id, "user_id": user_id, "items": [
        {"collectible_key": row.item_key,
         "title": ITEM_CATALOG.get(row.item_key, {}).get("title", row.item_key),
         "quantity": row.quantity} for row in rows]}
