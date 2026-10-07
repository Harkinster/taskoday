from fastapi import APIRouter, Depends, Header, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.db.session import get_db
from app.dependencies import get_current_user, success_response
from app.models.family import FamilyMemberRole
from app.models.resource_utility import WishOffer, WishRequest
from app.models.user import User
from app.schemas.resource_utility import (
    ChestCatalogResponse, ChestOpenListResponse, ChestOpenRequest, ChestOpenResponse, CollectionResponse,
    ResourceBalanceResponse, WishOfferCreate, WishOfferListResponse, WishOfferResponse,
    WishOfferUpdate, WishObtainRequest, WishRequestCreate, WishRequestListResponse,
    WishRequestResponse,
)
from app.schemas.common import SuccessResponse
from app.services.resource_utility_service import (
    CHEST_POLICY, ResourceUtilityError, chest_payload, collection_payload, create_offer,
    create_wish_request, decide_wish_request, list_chest_opens, open_chest, parent_obtain_wish,
    resource_balances,
)
from app.services.family_task_service import ensure_family_member, ensure_family_parent

router = APIRouter(tags=["resource utility"])


def _raise(exc: ResourceUtilityError):
    raise HTTPException(status_code=exc.status_code, detail=exc.message) from exc


def _request_payload(db: Session, row: WishRequest) -> dict:
    user = row.requester
    return {"id": row.id, "offer_id": row.offer_id, "user_id": row.user_id,
        "requester_name": user.display_name if user and user.display_name else "Membre",
        "title": row.title_snapshot, "flame_cost": row.flame_cost, "status": row.status,
        "requested_at": row.requested_at, "decided_by": row.decided_by, "decided_at": row.decided_at}


@router.get("/families/{family_id}/resources/me", response_model=SuccessResponse[ResourceBalanceResponse])
def get_resource_balance(family_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_family_member(db, family_id=family_id, user=current_user)
    return success_response(resource_balances(db, family_id=family_id, user_id=current_user.id))


@router.get("/families/{family_id}/wishes/offers", response_model=SuccessResponse[WishOfferListResponse])
def list_wish_offers(family_id: int, include_inactive: bool = False, db: Session = Depends(get_db),
                     current_user: User = Depends(get_current_user)):
    membership = ensure_family_member(db, family_id=family_id, user=current_user)
    stmt = select(WishOffer).where(WishOffer.family_id == family_id)
    if not include_inactive or membership.role != FamilyMemberRole.PARENT:
        stmt = stmt.where(WishOffer.active.is_(True))
    rows = db.scalars(stmt.order_by(WishOffer.created_at, WishOffer.id)).all()
    return success_response({"items": [WishOfferResponse.model_validate(row).model_dump() for row in rows]})


@router.post("/families/{family_id}/wishes/offers", status_code=status.HTTP_201_CREATED, response_model=SuccessResponse[WishOfferResponse])
def add_wish_offer(family_id: int, payload: WishOfferCreate, db: Session = Depends(get_db),
                   current_user: User = Depends(get_current_user)):
    ensure_family_parent(db, family_id=family_id, user=current_user)
    try:
        row = create_offer(db, family_id=family_id, user_id=current_user.id, **payload.model_dump())
    except ResourceUtilityError as exc:
        _raise(exc)
    db.commit(); db.refresh(row)
    return success_response(WishOfferResponse.model_validate(row).model_dump())


@router.patch("/families/{family_id}/wishes/offers/{offer_id}")
def edit_wish_offer(family_id: int, offer_id: int, payload: WishOfferUpdate, db: Session = Depends(get_db),
                    current_user: User = Depends(get_current_user)):
    ensure_family_parent(db, family_id=family_id, user=current_user)
    row = db.scalar(select(WishOffer).where(WishOffer.id == offer_id, WishOffer.family_id == family_id))
    if row is None: raise HTTPException(404, "Souhait introuvable.")
    for key, value in payload.model_dump(exclude_unset=True).items():
        setattr(row, key, value.strip() if key in {"title", "description"} and value is not None else value)
    db.commit(); db.refresh(row)
    return success_response(WishOfferResponse.model_validate(row).model_dump())


@router.post("/families/{family_id}/wishes/requests", status_code=status.HTTP_201_CREATED, response_model=SuccessResponse[WishRequestResponse])
def ask_wish(family_id: int, payload: WishRequestCreate, db: Session = Depends(get_db),
             current_user: User = Depends(get_current_user)):
    ensure_family_member(db, family_id=family_id, user=current_user)
    try: row = create_wish_request(db, family_id=family_id, user_id=current_user.id, offer_id=payload.offer_id)
    except ResourceUtilityError as exc: _raise(exc)
    db.commit(); db.refresh(row)
    return success_response(_request_payload(db, row))


@router.get("/families/{family_id}/wishes/requests", response_model=SuccessResponse[WishRequestListResponse])
def list_wish_requests(family_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    membership = ensure_family_member(db, family_id=family_id, user=current_user)
    stmt = select(WishRequest).where(WishRequest.family_id == family_id)
    if membership.role != FamilyMemberRole.PARENT:
        stmt = stmt.where(WishRequest.user_id == current_user.id)
    rows = db.scalars(stmt.options(selectinload(WishRequest.requester)).order_by(WishRequest.requested_at.desc(), WishRequest.id.desc())).all()
    return success_response({"items": [_request_payload(db, row) for row in rows]})


@router.post("/families/{family_id}/wishes/requests/{request_id}/cancel", response_model=SuccessResponse[WishRequestResponse])
def cancel_wish_request(family_id: int, request_id: int, db: Session = Depends(get_db),
                        current_user: User = Depends(get_current_user)):
    ensure_family_member(db, family_id=family_id, user=current_user)
    row = db.scalar(select(WishRequest).where(WishRequest.id == request_id,
        WishRequest.family_id == family_id, WishRequest.user_id == current_user.id))
    if row is None: raise HTTPException(404, "Demande introuvable.")
    if row.status != "PENDING": raise HTTPException(409, "Cette demande ne peut plus être annulée.")
    row.status = "CANCELLED"; row.decided_by = current_user.id
    from datetime import datetime, timezone
    row.decided_at = datetime.now(timezone.utc)
    db.commit(); db.refresh(row)
    return success_response(_request_payload(db, row))


@router.post("/families/{family_id}/wishes/requests/{request_id}/approve", response_model=SuccessResponse[WishRequestResponse])
def approve_wish_request(family_id: int, request_id: int, db: Session = Depends(get_db),
                         current_user: User = Depends(get_current_user)):
    ensure_family_parent(db, family_id=family_id, user=current_user)
    try: row = decide_wish_request(db, family_id=family_id, request_id=request_id, actor_user_id=current_user.id, approve=True)
    except ResourceUtilityError as exc: _raise(exc)
    db.commit(); db.refresh(row)
    return success_response(_request_payload(db, row), message="Souhait accepté.")


@router.post("/families/{family_id}/wishes/requests/{request_id}/reject", response_model=SuccessResponse[WishRequestResponse])
def reject_wish_request(family_id: int, request_id: int, db: Session = Depends(get_db),
                        current_user: User = Depends(get_current_user)):
    ensure_family_parent(db, family_id=family_id, user=current_user)
    try: row = decide_wish_request(db, family_id=family_id, request_id=request_id, actor_user_id=current_user.id, approve=False)
    except ResourceUtilityError as exc: _raise(exc)
    db.commit(); db.refresh(row)
    return success_response(_request_payload(db, row), message="Demande refusée.")


@router.post("/families/{family_id}/wishes/obtain", response_model=SuccessResponse[WishRequestResponse])
def obtain_wish(family_id: int, payload: WishObtainRequest, db: Session = Depends(get_db),
                current_user: User = Depends(get_current_user)):
    membership = ensure_family_member(db, family_id=family_id, user=current_user)
    if membership.role != FamilyMemberRole.PARENT: raise HTTPException(403, "Action réservée au Parent.")
    try: row = parent_obtain_wish(db, family_id=family_id, user_id=current_user.id,
        offer_id=payload.offer_id, idempotency_key=payload.idempotency_key)
    except ResourceUtilityError as exc: _raise(exc)
    db.commit(); db.refresh(row)
    return success_response(_request_payload(db, row), message="Souhait obtenu.")


@router.get("/families/{family_id}/chests/catalog", response_model=SuccessResponse[ChestCatalogResponse])
def chest_catalog(family_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_family_member(db, family_id=family_id, user=current_user)
    balance = resource_balances(db, family_id=family_id, user_id=current_user.id)
    entries = [{"chest_type": key, "title": row["title"], "crystal_cost": row["crystal_cost"], "drop_count": row["drop_count"]}
        for key, row in CHEST_POLICY.items()]
    return success_response({"family_id": family_id, "user_id": current_user.id,
        "crystals": balance["crystals"], "chests": entries})


@router.post("/families/{family_id}/chests/open", response_model=SuccessResponse[ChestOpenResponse])
def open_personal_chest(family_id: int, payload: ChestOpenRequest, db: Session = Depends(get_db),
                        current_user: User = Depends(get_current_user)):
    ensure_family_member(db, family_id=family_id, user=current_user)
    try: row = open_chest(db, family_id=family_id, user_id=current_user.id,
        chest_type=payload.chest_type, idempotency_key=payload.idempotency_key)
    except ResourceUtilityError as exc: _raise(exc)
    db.commit(); db.refresh(row)
    from app.services.resource_utility_service import chest_payload
    return success_response(chest_payload(db, row), message="Coffre ouvert.")


@router.get("/families/{family_id}/chests/opens", response_model=SuccessResponse[ChestOpenListResponse])
def list_my_chest_opens(family_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_family_member(db, family_id=family_id, user=current_user)
    return success_response({"items": [chest_payload(db, row) for row in list_chest_opens(
        db, family_id=family_id, user_id=current_user.id)]})


@router.get("/families/{family_id}/chests/{open_id}", response_model=SuccessResponse[ChestOpenResponse])
def get_chest_open(family_id: int, open_id: int, db: Session = Depends(get_db),
                   current_user: User = Depends(get_current_user)):
    from app.models.resource_utility import ChestOpen
    row = db.scalar(select(ChestOpen).where(ChestOpen.id == open_id,
        ChestOpen.family_id == family_id, ChestOpen.user_id == current_user.id))
    if row is None: raise HTTPException(404, "Ouverture introuvable.")
    ensure_family_member(db, family_id=family_id, user=current_user)
    from app.services.resource_utility_service import chest_payload
    return success_response(chest_payload(db, row))


@router.get("/families/{family_id}/collection", response_model=SuccessResponse[CollectionResponse])
def get_collection(family_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_family_member(db, family_id=family_id, user=current_user)
    return success_response(collection_payload(db, family_id=family_id, user_id=current_user.id))
