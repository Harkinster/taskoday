from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies import ensure_child_access, ensure_parent_child_access, get_current_user, success_response
from app.models.task import Quest, TaskCompletion, TaskStatus, TaskType
from app.models.user import User
from app.schemas.task import QuestCreateRequest, QuestUpdateRequest
from app.services.gamification_service import award_task_completion

router = APIRouter(tags=["quests"])


@router.get("/children/{child_id}/quests")
def list_quests(child_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    ensure_child_access(db, current_user, child_id)

    quests = db.execute(select(Quest).where(Quest.child_id == child_id).order_by(Quest.id.asc())).scalars().all()
    return success_response(
        [
            {
                "id": quest.id,
                "child_id": quest.child_id,
                "title": quest.title,
                "description": quest.description,
                "xp_reward": quest.xp_reward,
                "status": quest.status.value,
                "created_by_user_id": quest.created_by_user_id,
            }
            for quest in quests
        ]
    )


@router.post("/children/{child_id}/quests")
def create_quest(
    child_id: int,
    payload: QuestCreateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    ensure_parent_child_access(db, current_user, child_id)

    quest = Quest(
        child_id=child_id,
        title=payload.title,
        description=payload.description,
        xp_reward=payload.xp_reward,
        status=TaskStatus.OPEN,
        created_by_user_id=current_user.id,
    )
    db.add(quest)
    db.commit()
    db.refresh(quest)

    return success_response(
        {
            "id": quest.id,
            "child_id": quest.child_id,
            "title": quest.title,
            "description": quest.description,
            "xp_reward": quest.xp_reward,
            "status": quest.status.value,
            "created_by_user_id": quest.created_by_user_id,
        }
    )


@router.patch("/quests/{quest_id}")
def update_quest(
    quest_id: int,
    payload: QuestUpdateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    quest = db.get(Quest, quest_id)
    if not quest:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Quete introuvable.")

    ensure_parent_child_access(db, current_user, quest.child_id)

    data = payload.model_dump(exclude_unset=True)
    if "title" in data:
        quest.title = data["title"]
    if "description" in data:
        quest.description = data["description"]
    if "xp_reward" in data:
        quest.xp_reward = data["xp_reward"]
    if "status" in data:
        quest.status = TaskStatus(data["status"])

    db.commit()
    db.refresh(quest)

    return success_response(
        {
            "id": quest.id,
            "child_id": quest.child_id,
            "title": quest.title,
            "description": quest.description,
            "xp_reward": quest.xp_reward,
            "status": quest.status.value,
            "created_by_user_id": quest.created_by_user_id,
        },
        message="Quete mise a jour.",
    )


@router.delete("/quests/{quest_id}")
def delete_quest(quest_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    quest = db.get(Quest, quest_id)
    if not quest:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Quete introuvable.")

    ensure_parent_child_access(db, current_user, quest.child_id)

    db.delete(quest)
    db.commit()

    return success_response({"id": quest_id}, message="Quete supprimee.")


@router.post("/quests/{quest_id}/complete")
def complete_quest(quest_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    quest = db.get(Quest, quest_id)
    if not quest:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Quete introuvable.")

    ensure_child_access(db, current_user, quest.child_id)

    already_completed = quest.status == TaskStatus.COMPLETED
    award = None

    if not already_completed:
        quest.status = TaskStatus.COMPLETED

        completion = db.execute(
            select(TaskCompletion).where(
                TaskCompletion.task_type == TaskType.QUEST,
                TaskCompletion.task_id == quest.id,
                TaskCompletion.child_id == quest.child_id,
            )
        ).scalars().first()
        if not completion:
            completion = TaskCompletion(
                task_type=TaskType.QUEST,
                task_id=quest.id,
                child_id=quest.child_id,
                completed_by_user_id=current_user.id,
            )
            db.add(completion)
            award = award_task_completion(
                db,
                completion=completion,
                title=quest.title,
            )

        db.commit()

        return success_response(
            {
                "quest_id": quest.id,
                "completed": True,
                "award": award,
            },
            message="Quete completee.",
        )

    return success_response(
        {
            "quest_id": quest.id,
            "completed": True,
            "award": None,
        },
        message="Quete deja completee.",
    )
