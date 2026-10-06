from datetime import datetime
from typing import Literal

from pydantic import BaseModel


class RewardGrantResponse(BaseModel):
    id: int
    action_id: int
    action_title: str
    occurrence_id: int
    cycle_number: int
    scope: Literal["PERSONAL", "HOUSE"]
    kind: Literal["ROUTINE", "MISSION", "QUEST"]
    points: int
    trigger_event_id: int
    created_at: datetime
    revoked_at: datetime | None


class RewardSummaryResponse(BaseModel):
    family_id: int
    user_id: int
    active_points: int
    grants: list[RewardGrantResponse]
