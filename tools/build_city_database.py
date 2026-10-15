#!/usr/bin/env python3
"""Build the bundled offline city search database from a GeoNames cities500.zip."""

from __future__ import annotations

import argparse
import hashlib
import sqlite3
import urllib.request
import zipfile
from pathlib import Path

SOURCE_URL = "https://download.geonames.org/export/dump/cities500.zip"
DEFAULT_OUTPUT = Path("app/src/main/assets/cities500.sqlite")


def is_arabic(text: str) -> bool:
    return any("\u0600" <= character <= "\u06ff" for character in text)


def is_searchable_alias(text: str) -> bool:
    # English/transliterated names and Arabic-script names cover this app's supported languages.
    return text.isascii() or is_arabic(text)


def build(source: Path, output: Path) -> int:
    output.parent.mkdir(parents=True, exist_ok=True)
    output.unlink(missing_ok=True)
    database_file = output
    database_file.unlink(missing_ok=True)

    database = sqlite3.connect(database_file)
    try:
        database.execute("PRAGMA journal_mode=OFF")
        database.execute("PRAGMA synchronous=OFF")
        database.execute("PRAGMA page_size=4096")
        database.execute(
            "CREATE TABLE city ("
            "id INTEGER PRIMARY KEY, name TEXT NOT NULL, ascii_name TEXT NOT NULL, "
            "arabic_name TEXT, country_code TEXT NOT NULL, latitude REAL NOT NULL, "
            "longitude REAL NOT NULL, timezone_id TEXT NOT NULL, population INTEGER NOT NULL)"
        )
        database.execute(
            "CREATE TABLE city_alias (name TEXT NOT NULL COLLATE NOCASE, city_id INTEGER NOT NULL)"
        )

        city_sql = "INSERT INTO city VALUES (?,?,?,?,?,?,?,?,?)"
        alias_sql = "INSERT INTO city_alias VALUES (?,?)"
        cities: list[tuple[object, ...]] = []
        aliases: list[tuple[str, int]] = []
        count = 0

        with zipfile.ZipFile(source) as archive:
            with archive.open("cities500.txt") as rows:
                for raw_row in rows:
                    fields = raw_row.decode("utf-8").rstrip("\n").split("\t")
                    if len(fields) < 18:
                        continue
                    try:
                        geoname_id = int(fields[0])
                        latitude = float(fields[4])
                        longitude = float(fields[5])
                        population = int(fields[14] or 0)
                    except ValueError:
                        continue

                    aliases_for_search = list(
                        dict.fromkeys(
                            alias
                            for alias in fields[3].split(",")
                            if alias
                            and is_searchable_alias(alias)
                            and alias.casefold() not in {fields[1].casefold(), fields[2].casefold()}
                        )
                    )
                    arabic_names = [alias for alias in aliases_for_search if is_arabic(alias)]
                    cities.append(
                        (
                            geoname_id,
                            fields[1],
                            fields[2],
                            arabic_names[0] if arabic_names else None,
                            fields[8],
                            latitude,
                            longitude,
                            fields[17],
                            population,
                        )
                    )
                    aliases.extend((alias, geoname_id) for alias in aliases_for_search)
                    count += 1

                    if len(cities) >= 2000:
                        database.executemany(city_sql, cities)
                        database.executemany(alias_sql, aliases)
                        cities.clear()
                        aliases.clear()

                if cities:
                    database.executemany(city_sql, cities)
                    database.executemany(alias_sql, aliases)

        database.execute("CREATE INDEX city_name ON city(name COLLATE NOCASE)")
        database.execute("CREATE INDEX city_ascii_name ON city(ascii_name COLLATE NOCASE)")
        database.execute("CREATE INDEX city_alias_name ON city_alias(name COLLATE NOCASE, city_id)")
        database.execute("ANALYZE")
        database.commit()
        return count
    finally:
        database.close()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--source",
        type=Path,
        help="GeoNames cities500.zip; downloads the current file if omitted",
    )
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    arguments = parser.parse_args()

    source = arguments.source
    downloaded = None
    if source is None:
        downloaded = Path("cities500.zip")
        urllib.request.urlretrieve(SOURCE_URL, downloaded)
        source = downloaded

    try:
        count = build(source, arguments.output)
        version_file = arguments.output.with_name("cities500.version")
        version_file.write_text(sha256_file(source) + "\n", encoding="ascii")
        print(f"Built {arguments.output} with {count:,} populated places.")
    finally:
        if downloaded is not None:
            downloaded.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
