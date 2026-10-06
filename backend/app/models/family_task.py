import enum
from datetime import date, datetime, time

from sqlalchemy import Boolean, CheckConstraint, Date, DateTime, Enum, ForeignKey, Index, Integer, String, Text, Time, UniqueConstraint, func, text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.declarative import Base


class FamilyTaskPriority(str, enum.Enum):
    LOW = "LOW"
    NORMAL = "NORMAL"
    HIGH = "HIGH"
    URGENT = "URGENT"


class FamilyTaskRecurrence(str, enum.Enum):
    NONE = "NONE"
    DAILY = "DAILY"
    WEEKLY = "WEEKLY"
    SELECTED_WEEKDAYS = "SELECTED_WEEKDAYS"


class FamilyTaskOccurrenceStatus(str, enum.Enum):
    TODO = "TODO"
    COMPLETED = "COMPLETED"
    PENDING_VALIDATION = "PENDING_VALIDATION"
    VALIDATED = "VALIDATED"
    SKIPPED = "SKIPPED"


class FamilyTask(Base):
    __tablename__ = "family_tasks"
    __table_args__ = (CheckConstraint("recurrence_interval >= 1", name="ck_family_tasks_recurrence_interval_positive"),)

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    family_id: Mapped[int] = mapped_column(ForeignKey("families.id", ondelete="CASCADE"), nullable=False, index=True)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    creator_user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=False, index=True)
    category: Mapped[str | None] = mapped_column(String(100), nullable=True)
    scope: Mapped[str] = mapped_column(String(16), nullable=False)
    kind: Mapped[str] = mapped_column(String(16), nullable=False)
    priority: Mapped[FamilyTaskPriority] = mapped_column(
        Enum(FamilyTaskPriority),
        default=FamilyTaskPriority.NORMAL,
        nullable=False,
        index=True,
    )
    due_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True, index=True)
    due_date: Mapped[date | None] = mapped_column(Date, nullable=True, index=True)
    end_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    due_time: Mapped[time | None] = mapped_column(Time, nullable=True)
    recurrence: Mapped[FamilyTaskRecurrence] = mapped_column(
        Enum(FamilyTaskRecurrence),
        default=FamilyTaskRecurrence.NONE,
        nullable=False,
        index=True,
    )
    selected_weekdays: Mapped[str | None] = mapped_column(String(32), nullable=True)
    recurrence_interval: Mapped[int] = mapped_column(Integer, default=1, server_default=text("1"), nullable=False)
    validation_required: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    gamification_enabled: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    assignees = relationship("FamilyTaskAssignee", back_populates="task", cascade="all, delete-orphan")
    occurrences = relationship("FamilyTaskOccurrence", back_populates="task", cascade="all, delete-orphan")


class FamilyTaskAssignee(Base):
    __tablename__ = "family_task_assignees"
    __table_args__ = (UniqueConstraint("task_id", "user_id", name="uq_family_task_assignee_task_user"),)

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    task_id: Mapped[int] = mapped_column(ForeignKey("family_tasks.id", ondelete="CASCADE"), nullable=False, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)

    task = relationship("FamilyTask", back_populates="assignees")
    user = relationship("User")


class FamilyTaskOccurrence(Base):
    __tablename__ = "family_task_occurrences"
    __table_args__ = (
        UniqueConstraint("task_id", "scheduled_date", name="uq_family_task_occurrence_task_date"),
        Index("uq_family_task_occurrence_undated", "task_id", unique=True, sqlite_where=text("scheduled_date IS NULL")),
    )

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    task_id: Mapped[int] = mapped_column(ForeignKey("family_tasks.id", ondelete="CASCADE"), nullable=False, index=True)
    category: Mapped[str] = mapped_column(String(100), nullable=False)
    scope: Mapped[str] = mapped_column(String(16), nullable=False)
    kind: Mapped[str] = mapped_column(String(16), nullable=False)
    scheduled_date: Mapped[date | None] = mapped_column(Date, nullable=True, index=True)
    status: Mapped[FamilyTaskOccurrenceStatus] = mapped_column(
        Enum(FamilyTaskOccurrenceStatus),
        default=FamilyTaskOccurrenceStatus.TODO,
        nullable=False,
        index=True,
    )
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    completed_by_user_id: Mapped[int | None] = mapped_column(
        ForeignKey("users.id", ondelete="RESTRICT"),
        nullable=True,
        index=True,
    )
    validated_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    validated_by_user_id: Mapped[int | None] = mapped_column(
        ForeignKey("users.id", ondelete="RESTRICT"),
        nullable=True,
        index=True,
    )
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    task = relationship("FamilyTask", back_populates="occurrences")
    completed_by = relationship("User", foreign_keys=[completed_by_user_id])
    validated_by = relationship("User", foreign_keys=[validated_by_user_id])


class FamilyTaskOccurrenceEvent(Base):
    """Immutable record of an effective occurrence transition."""

    __tablename__ = "family_task_occurrence_events"

    id: Mapped[int] = mapped_column(primary_key=True)
    family_id: Mapped[int] = mapped_column(ForeignKey("families.id", ondelete="RESTRICT"), nullable=False, index=True)
    task_id: Mapped[int] = mapped_column(ForeignKey("family_tasks.id", ondelete="RESTRICT"), nullable=False, index=True)
    occurrence_id: Mapped[int] = mapped_column(ForeignKey("family_task_occurrences.id", ondelete="RESTRICT"), nullable=False, index=True)
    category: Mapped[str] = mapped_column(String(100), nullable=False)
    scope: Mapped[str] = mapped_column(String(16), nullable=False)
    kind: Mapped[str] = mapped_column(String(16), nullable=False)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    scheduled_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    event_type: Mapped[str] = mapped_column(String(20), nullable=False)
    status_from: Mapped[FamilyTaskOccurrenceStatus] = mapped_column(Enum(FamilyTaskOccurrenceStatus), nullable=False)
    status_to: Mapped[FamilyTaskOccurrenceStatus] = mapped_column(Enum(FamilyTaskOccurrenceStatus), nullable=False)
    actor_user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=False)
    completed_by_user_id: Mapped[int | None] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=True)
    occurred_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, index=True)
    legacy_inferred: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)

    participants = relationship("FamilyTaskOccurrenceEventParticipant", back_populates="event", cascade="all, delete-orphan")


class FamilyTaskOccurrenceEventParticipant(Base):
    __tablename__ = "family_task_occurrence_event_participants"
    __table_args__ = (UniqueConstraint("event_id", "user_id", name="uq_family_task_event_participant"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    event_id: Mapped[int] = mapped_column(ForeignKey("family_task_occurrence_events.id", ondelete="CASCADE"), nullable=False, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"), nullable=False, index=True)

    event = relationship("FamilyTaskOccurrenceEvent", back_populates="participants")
