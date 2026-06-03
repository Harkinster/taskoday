import enum
from datetime import datetime

from sqlalchemy import Boolean, DateTime, Enum, ForeignKey, Integer, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class ChestType(str, enum.Enum):
    SIMPLE = "simple"
    RARE = "rare"


class ChestStatus(str, enum.Enum):
    UNOPENED = "unopened"
    OPENED = "opened"


class ChildEggStatus(str, enum.Enum):
    LOCKED = "locked"
    AVAILABLE = "available"
    READY = "ready"
    HATCHED = "hatched"


class DragonStage(str, enum.Enum):
    BABY = "baby"
    YOUNG = "young"
    GUARDIAN = "guardian"


class GuardianProgress(Base):
    __tablename__ = "guardian_progress"

    child_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    guardian_xp: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    chest_points: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    opened_chests: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )


class ChildWallet(Base):
    __tablename__ = "child_wallet"

    child_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    flammeches_balance: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )


class ChestInventory(Base):
    __tablename__ = "chest_inventory"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    child_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    chest_type: Mapped[ChestType] = mapped_column(Enum(ChestType), nullable=False, index=True)
    status: Mapped[ChestStatus] = mapped_column(Enum(ChestStatus), default=ChestStatus.UNOPENED, nullable=False, index=True)
    source_type: Mapped[str | None] = mapped_column(String(50), nullable=True)
    source_id: Mapped[int | None] = mapped_column(Integer, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    opened_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)


class ItemInventory(Base):
    __tablename__ = "item_inventory"
    __table_args__ = (UniqueConstraint("child_id", "item_key", name="uq_item_inventory_child_item"),)

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    child_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    item_key: Mapped[str] = mapped_column(String(80), nullable=False, index=True)
    quantity: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )


class EggDefinition(Base):
    __tablename__ = "eggs"

    key: Mapped[str] = mapped_column(String(80), primary_key=True)
    title: Mapped[str] = mapped_column(String(120), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)


class ChildEgg(Base):
    __tablename__ = "child_eggs"
    __table_args__ = (UniqueConstraint("child_id", "egg_key", name="uq_child_eggs_child_egg"),)

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    child_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    egg_key: Mapped[str] = mapped_column(ForeignKey("eggs.key", ondelete="CASCADE"), nullable=False, index=True)
    status: Mapped[ChildEggStatus] = mapped_column(Enum(ChildEggStatus), default=ChildEggStatus.AVAILABLE, nullable=False)
    obtained_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    hatched_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)


class DragonDefinition(Base):
    __tablename__ = "dragons"

    key: Mapped[str] = mapped_column(String(80), primary_key=True)
    title: Mapped[str] = mapped_column(String(120), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)


class ChildDragon(Base):
    __tablename__ = "child_dragons"
    __table_args__ = (UniqueConstraint("child_id", "dragon_key", name="uq_child_dragons_child_dragon"),)

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    child_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    dragon_key: Mapped[str] = mapped_column(ForeignKey("dragons.key", ondelete="CASCADE"), nullable=False, index=True)
    stage: Mapped[DragonStage] = mapped_column(Enum(DragonStage), default=DragonStage.BABY, nullable=False)
    unlocked_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )
