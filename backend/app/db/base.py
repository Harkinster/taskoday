from app.db.declarative import Base


# Import explicite des modeles pour que SQLAlchemy/Alembic detecte les tables.
from app.models.child import ChildProfile  # noqa: E402,F401
from app.models.family import Family, FamilyMember  # noqa: E402,F401
from app.models.gamification import (  # noqa: E402,F401
    ChestInventory,
    ChildDragon,
    ChildEgg,
    ChildWallet,
    DragonDefinition,
    EggDefinition,
    GuardianProgress,
    ItemInventory,
)
from app.models.pairing import PairingCode  # noqa: E402,F401
from app.models.reward import ExternalReward, RewardCoupon, RewardRequest, ScaleTransaction  # noqa: E402,F401
from app.models.task import Mission, Quest, Routine, TaskCompletion  # noqa: E402,F401
from app.models.user import User  # noqa: E402,F401
from app.models.xp import XpHistory  # noqa: E402,F401
