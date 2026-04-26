from enum import Enum


class UserRole(str, Enum):
    PARENT = "PARENT"
    CHILD = "CHILD"


class DayPart(str, Enum):
    MATIN = "MATIN"
    MATINEE = "MATINEE"
    MIDI = "MIDI"
    APRES_MIDI = "APRES_MIDI"
    SOIR = "SOIR"
    SOIREE = "SOIREE"


class RoutineFrequency(str, Enum):
    DAILY = "DAILY"
    SELECTED_DAYS = "SELECTED_DAYS"


class PlanningItemType(str, Enum):
    ROUTINE = "routine"
    MISSION = "mission"
    QUEST = "quest"


class PointsTransactionType(str, Enum):
    AWARD = "AWARD"
    REVOKE = "REVOKE"
