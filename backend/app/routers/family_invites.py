from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies import get_current_user, success_response
from app.models.user import User
from app.services.family_invite_service import (
    accept_parent_invite,
    accepted_invite_payload,
    create_parent_invite,
    invite_payload,
)

router = APIRouter(tags=["family-invites"])


@router.post("/families/{family_id}/parent-invites", status_code=status.HTTP_201_CREATED)
def create_family_parent_invite(
    family_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    invite, token = create_parent_invite(db, family_id=family_id, user=current_user)
    db.commit()
    db.refresh(invite)
    return success_response(invite_payload(db, invite, token), message="Invitation parent creee.")


@router.post("/family-invites/{code}/accept")
def accept_family_parent_invite(
    code: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    invite = accept_parent_invite(db, code=code, user=current_user)
    db.commit()
    db.refresh(invite)
    return success_response(accepted_invite_payload(db, invite), message="Invitation acceptee.")
