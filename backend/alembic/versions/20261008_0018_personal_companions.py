"""Add personal companion identity while preserving legacy dragon rows."""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "20261008_0018"
down_revision: Union[str, Sequence[str], None] = "20261007_0017"
branch_labels = None
depends_on = None

LINEAGES = (
    ("FULMIO", "Compagnon de lignée Fulmio"),
    ("SYLVYN", "Compagnon de lignée Sylvyn"),
    ("PHENOR", "Compagnon de lignée Phenor"),
    ("LUNARYS", "Compagnon de lignée Lunarys"),
    ("PYRON", "Compagnon de lignée Pyron"),
    ("CHRONYX", "Compagnon de lignée Chronyx"),
    ("AMBRIO", "Compagnon de lignée Ambrio"),
    ("CRISTAO", "Compagnon de lignée Cristao"),
)


def upgrade() -> None:
    with op.batch_alter_table("child_dragons") as batch_op:
        batch_op.add_column(sa.Column("lineage_id", sa.String(length=16), nullable=True))
        batch_op.add_column(sa.Column("display_name", sa.String(length=32), nullable=True))
        batch_op.add_column(sa.Column("starter_key", sa.String(length=16), nullable=True))
        batch_op.create_index("ix_child_dragons_lineage_id", ["lineage_id"], unique=False)
        batch_op.create_unique_constraint("uq_child_dragons_owner_starter", ["child_id", "starter_key"])
        batch_op.create_check_constraint(
            "ck_child_dragon_lineage_official",
            "lineage_id IS NULL OR lineage_id IN ('FULMIO','SYLVYN','PHENOR','LUNARYS','PYRON','CHRONYX','AMBRIO','CRISTAO')",
        )

    # Seed only canonical dragon definitions. No user-owned dragon, egg, or legacy
    # lineage is inferred or backfilled here.
    dragons = sa.table(
        "dragons",
        sa.column("key", sa.String),
        sa.column("title", sa.String),
        sa.column("is_active", sa.Boolean),
    )
    bind = op.get_bind()
    existing = set(bind.execute(sa.select(dragons.c.key)).scalars())
    for lineage_id, title in LINEAGES:
        key = f"dragon_{lineage_id.lower()}"
        if key not in existing:
            bind.execute(dragons.insert().values(key=key, title=title, is_active=True))


def downgrade() -> None:
    bind = op.get_bind()
    child_dragons = sa.table(
        "child_dragons",
        sa.column("dragon_key", sa.String),
        sa.column("lineage_id", sa.String),
        sa.column("display_name", sa.String),
        sa.column("starter_key", sa.String),
    )
    keys = [f"dragon_{lineage_id.lower()}" for lineage_id, _ in LINEAGES]
    used = bind.execute(sa.select(sa.func.count()).select_from(child_dragons).where(child_dragons.c.dragon_key.in_(keys))).scalar_one()
    if used:
        raise RuntimeError("Cannot downgrade 20261008_0018 while canonical companion rows exist.")

    # Keep canonical catalog rows on downgrade: some may have pre-existed this
    # revision, and deleting them could remove a historical definition.
    with op.batch_alter_table("child_dragons") as batch_op:
        batch_op.drop_constraint("uq_child_dragons_owner_starter", type_="unique")
        batch_op.drop_constraint("ck_child_dragon_lineage_official", type_="check")
        batch_op.drop_index("ix_child_dragons_lineage_id")
        batch_op.drop_column("starter_key")
        batch_op.drop_column("display_name")
        batch_op.drop_column("lineage_id")
