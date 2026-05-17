#!/usr/bin/env python3
"""Copy Taskoday data from SQLite to MariaDB using SQLAlchemy reflection.

This script is intentionally conservative:
- source and target schemas must already exist and match.
- target tables must be empty by default.
- dry-run mode is default.

Usage examples:
  python scripts/migrate_sqlite_to_mariadb.py \
    --source-url sqlite:////opt/taskoday/backend/taskoday_prod.db \
    --target-url mysql+pymysql://taskoday_api:CHANGE_ME@127.0.0.1:3306/taskoday \
    --dry-run

  python scripts/migrate_sqlite_to_mariadb.py \
    --source-url sqlite:////opt/taskoday/backend/taskoday_prod.db \
    --target-url mysql+pymysql://taskoday_api:CHANGE_ME@127.0.0.1:3306/taskoday \
    --execute
"""

from __future__ import annotations

import argparse
import sys
from typing import Iterable

from sqlalchemy import MetaData, create_engine, func, select
from sqlalchemy.engine import Connection


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Migrate Taskoday data from SQLite to MariaDB")
    parser.add_argument("--source-url", required=True, help="SQLAlchemy URL of source database (SQLite)")
    parser.add_argument("--target-url", required=True, help="SQLAlchemy URL of target database (MariaDB)")
    parser.add_argument("--batch-size", type=int, default=500, help="Rows per insert batch (default: 500)")
    parser.add_argument(
        "--allow-non-empty-target",
        action="store_true",
        help="Allow inserts into non-empty target tables (disabled by default)",
    )
    parser.add_argument(
        "--include-alembic-version",
        action="store_true",
        help="Include alembic_version table in copy (disabled by default)",
    )

    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--dry-run", action="store_true", help="Only print checks and counts (default)")
    mode.add_argument("--execute", action="store_true", help="Perform data copy")

    args = parser.parse_args()
    if not args.execute:
        args.dry_run = True
    return args


def get_tables(meta: MetaData, include_alembic_version: bool) -> list[str]:
    names = [t.name for t in meta.sorted_tables]
    if not include_alembic_version:
        names = [n for n in names if n != "alembic_version"]
    return names


def table_count(conn: Connection, table_obj) -> int:
    stmt = select(func.count()).select_from(table_obj)
    return int(conn.execute(stmt).scalar_one())


def chunked_mappings(conn: Connection, table_obj, batch_size: int) -> Iterable[list[dict]]:
    result = conn.execute(select(table_obj)).mappings()
    while True:
        batch = result.fetchmany(batch_size)
        if not batch:
            break
        yield [dict(row) for row in batch]


def main() -> int:
    args = parse_args()

    source_engine = create_engine(args.source_url)
    target_engine = create_engine(args.target_url)

    source_meta = MetaData()
    target_meta = MetaData()

    try:
        source_meta.reflect(bind=source_engine)
        target_meta.reflect(bind=target_engine)
    except Exception as exc:  # pragma: no cover
        print(f"[ERROR] Unable to reflect schemas: {exc}", file=sys.stderr)
        return 1

    source_tables = get_tables(source_meta, args.include_alembic_version)
    target_tables = get_tables(target_meta, args.include_alembic_version)

    if not source_tables:
        print("[ERROR] No source tables found.", file=sys.stderr)
        return 1

    missing_in_target = sorted(set(source_tables) - set(target_tables))
    if missing_in_target:
        print("[ERROR] Target schema is incomplete. Missing tables:", file=sys.stderr)
        for name in missing_in_target:
            print(f"  - {name}", file=sys.stderr)
        print("Run alembic upgrade head on target before copying.", file=sys.stderr)
        return 1

    ordered_names = [name for name in source_tables if name in target_meta.tables]

    print("[INFO] Pre-checking row counts...")
    checks: list[tuple[str, int, int]] = []
    with source_engine.connect() as source_conn, target_engine.connect() as target_conn:
        for name in ordered_names:
            source_table = source_meta.tables[name]
            target_table = target_meta.tables[name]

            src_count = table_count(source_conn, source_table)
            tgt_count = table_count(target_conn, target_table)
            checks.append((name, src_count, tgt_count))

            if tgt_count > 0 and not args.allow_non_empty_target:
                print(
                    f"[ERROR] Target table '{name}' is not empty ({tgt_count} rows). "
                    "Use --allow-non-empty-target only if this is intentional.",
                    file=sys.stderr,
                )
                return 1

    for name, src_count, tgt_count in checks:
        print(f"  - {name}: source={src_count}, target={tgt_count}")

    if args.dry_run:
        print("[DRY-RUN] No data was copied.")
        return 0

    print("[INFO] Copying data...")
    inserted_total = 0
    with source_engine.connect() as source_conn, target_engine.begin() as target_conn:
        for name in ordered_names:
            source_table = source_meta.tables[name]
            target_table = target_meta.tables[name]
            inserted_for_table = 0

            for batch in chunked_mappings(source_conn, source_table, args.batch_size):
                if not batch:
                    continue
                target_conn.execute(target_table.insert(), batch)
                inserted_for_table += len(batch)
                inserted_total += len(batch)

            print(f"  - copied {inserted_for_table} rows into {name}")

    print(f"[OK] Copy completed. Total rows inserted: {inserted_total}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
