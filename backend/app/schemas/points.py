from datetime import date, datetime
from typing import List

from pydantic import BaseModel

from app.models.enums import PlanningItemType, PointsTransactionType


class PointsBalanceResponse(BaseModel):
    child_id: int
    balance: int


class PointsHistoryItemResponse(BaseModel):
    id: int
    item_type: PlanningItemType
    item_id: int
    completion_date: date
    transaction_type: PointsTransactionType
    amount: int
    created_at: datetime


class PointsHistoryResponse(BaseModel):
    child_id: int
    balance: int
    transactions: List[PointsHistoryItemResponse]

