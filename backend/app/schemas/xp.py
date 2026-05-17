from datetime import datetime

from pydantic import BaseModel


class XpHistoryOut(BaseModel):
    id: int
    child_id: int
    amount: int
    reason: str
    source_type: str
    source_id: int | None
    created_at: datetime
