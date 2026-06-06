from typing import Generic, TypeVar

from pydantic import BaseModel


DataT = TypeVar("DataT")


class SuccessResponse(BaseModel, Generic[DataT]):
    success: bool = True
    data: DataT
    message: str | None = None
