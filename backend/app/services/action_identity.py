"""Canonical FamilyTask identity and legacy category compatibility."""

from fastapi import HTTPException, status

LEGACY_TO_IDENTITY = {
    "TASKODAY_PERSONAL_ROUTINE": ("PERSONAL", "ROUTINE"),
    "TASKODAY_PERSONAL_MISSION": ("PERSONAL", "MISSION"),
    "TASKODAY_HOUSE_QUEST": ("HOUSE", "QUEST"),
    "TASKODAY_HOUSE_ROUTINE": ("HOUSE", "ROUTINE"),
    "TASKODAY_HOUSE_MISSION": ("HOUSE", "MISSION"),
}
IDENTITY_TO_CATEGORY = {value: key for key, value in LEGACY_TO_IDENTITY.items()}


def identity_from_category(category: str | None) -> tuple[str, str]:
    normalized = category.strip().upper() if category is not None else ""
    if normalized in {"", "MAISON"}:
        return "HOUSE", "QUEST"
    if normalized not in LEGACY_TO_IDENTITY:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="Categorie d'action inconnue.")
    return LEGACY_TO_IDENTITY[normalized]


def resolve_action_identity(
    *, category: str | None, scope: str | None, kind: str | None,
) -> tuple[str, str, str]:
    if scope is None and kind is None:
        scope, kind = identity_from_category(category)
    elif scope is None or kind is None:
        raise HTTPException(status_code=422, detail="scope et kind doivent etre fournis ensemble.")
    if (scope, kind) not in IDENTITY_TO_CATEGORY:
        raise HTTPException(status_code=422, detail="Combinaison scope/kind non supportee.")
    canonical = IDENTITY_TO_CATEGORY[(scope, kind)]
    if category is not None and identity_from_category(category) != (scope, kind):
        raise HTTPException(status_code=422, detail="category ne correspond pas a scope/kind.")
    return scope, kind, canonical


def validate_identity_recurrence(*, kind: str, recurrence: str) -> None:
    if kind == "ROUTINE" and recurrence == "NONE":
        raise HTTPException(status_code=422, detail="Une routine doit avoir une recurrence.")
    if kind == "MISSION" and recurrence != "NONE":
        raise HTTPException(status_code=422, detail="Une mission ne peut pas etre recurrente.")
