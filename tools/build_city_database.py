#!/usr/bin/env python3
"""Build the bundled offline city catalog from GeoNames city and localized-name exports."""

from __future__ import annotations

import argparse
import hashlib
import sqlite3
import unicodedata
import urllib.request
import zipfile
from pathlib import Path

SOURCE_URL = "https://download.geonames.org/export/dump/cities500.zip"
ALTERNATE_NAMES_URL = "https://download.geonames.org/export/dump/alternateNamesV2.zip"
DEFAULT_OUTPUT = Path("app/src/main/assets/cities500.sqlite")
SUPPORTED_LOCALIZED_LANGUAGES = {"ar"}


def contains_arabic_letter(text: str) -> bool:
    return any(
        unicodedata.category(character).startswith("L")
        and "ARABIC" in unicodedata.name(character, "")
        for character in text
    )


def is_arabic_language_name(text: str) -> bool:
    """Reject empty/mark-only values and mixed Latin-script data mislabeled by script alone."""
    if not contains_arabic_letter(text):
        return False
    return not any(
        unicodedata.category(character).startswith("L")
        and "LATIN" in unicodedata.name(character, "")
        for character in text
    )


def is_arabic_script(text: str) -> bool:
    return any("\u0600" <= character <= "\u06ff" for character in text)


def is_searchable_alias(text: str) -> bool:
    return text.isascii() or is_arabic_script(text)


def build(source: Path, output: Path, alternate_names_source: Path | None = None) -> int:
    output.parent.mkdir(parents=True, exist_ok=True)
    output.unlink(missing_ok=True)

    database = sqlite3.connect(output)
    try:
        database.execute("PRAGMA journal_mode=OFF")
        database.execute("PRAGMA synchronous=OFF")
        database.execute("PRAGMA page_size=4096")
        database.execute(
            "CREATE TABLE city ("
            "id INTEGER PRIMARY KEY, name TEXT NOT NULL, ascii_name TEXT NOT NULL, "
            "country_code TEXT NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, "
            "timezone_id TEXT NOT NULL, population INTEGER NOT NULL)"
        )
        database.execute(
            "CREATE TABLE city_localized_name ("
            "city_id INTEGER NOT NULL, language_tag TEXT NOT NULL, name TEXT NOT NULL, "
            "is_preferred INTEGER NOT NULL DEFAULT 0, "
            "PRIMARY KEY(city_id, language_tag, name))"
        )
        database.execute(
            "CREATE TABLE city_alias (name TEXT NOT NULL COLLATE NOCASE, city_id INTEGER NOT NULL)"
        )

        city_sql = "INSERT INTO city VALUES (?,?,?,?,?,?,?,?)"
        alias_sql = "INSERT INTO city_alias VALUES (?,?)"
        localized_sql = (
            "INSERT INTO city_localized_name(city_id, language_tag, name, is_preferred) "
            "VALUES (?,?,?,?) ON CONFLICT(city_id, language_tag, name) "
            "DO UPDATE SET is_preferred = MAX(is_preferred, excluded.is_preferred)"
        )
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

                    city_name = fields[1].strip()
                    ascii_name = fields[2].strip()
                    country_code = fields[8].strip()
                    if not city_name or not country_code:
                        continue

                    aliases_for_search = list(
                        dict.fromkeys(
                            alias.strip()
                            for alias in fields[3].split(",")
                            if alias.strip()
                            and is_searchable_alias(alias.strip())
                            and alias.strip().casefold() not in {city_name.casefold(), ascii_name.casefold()}
                        )
                    )
                    cities.append(
                        (
                            geoname_id,
                            city_name,
                            ascii_name or city_name,
                            country_code,
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

        if alternate_names_source is not None:
            localized_names: list[tuple[int, str, str, int]] = []
            with zipfile.ZipFile(alternate_names_source) as archive:
                with archive.open("alternateNamesV2.txt") as rows:
                    for raw_row in rows:
                        fields = raw_row.decode("utf-8").rstrip("\n").split("\t")
                        if len(fields) < 4:
                            continue
                        try:
                            geoname_id = int(fields[1])
                        except ValueError:
                            continue

                        language_tag = fields[2].strip().lower()
                        name = fields[3].strip()
                        language = language_tag.split("-", 1)[0]
                        if language not in SUPPORTED_LOCALIZED_LANGUAGES:
                            continue
                        if language == "ar" and not is_arabic_language_name(name):
                            continue

                        is_preferred = int(len(fields) > 4 and fields[4] == "1")
                        localized_names.append((geoname_id, language, name, is_preferred))
                        if len(localized_names) >= 20_000:
                            database.executemany(localized_sql, localized_names)
                            localized_names.clear()

            if localized_names:
                database.executemany(localized_sql, localized_names)

        database.execute("CREATE INDEX city_name ON city(name COLLATE NOCASE)")
        database.execute("CREATE INDEX city_ascii_name ON city(ascii_name COLLATE NOCASE)")
        database.execute(
            "CREATE INDEX city_localized_lookup "
            "ON city_localized_name(language_tag, name COLLATE NOCASE, city_id)"
        )
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
    parser.add_argument(
        "--alternate-names-source",
        type=Path,
        help="GeoNames alternateNamesV2.zip; downloads it if omitted",
    )
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    arguments = parser.parse_args()

    source = arguments.source
    alternate_names_source = arguments.alternate_names_source
    downloaded: list[Path] = []
    if source is None:
        source = Path("cities500.zip")
        urllib.request.urlretrieve(SOURCE_URL, source)
        downloaded.append(source)
    if alternate_names_source is None:
        alternate_names_source = Path("alternateNamesV2.zip")
        urllib.request.urlretrieve(ALTERNATE_NAMES_URL, alternate_names_source)
        downloaded.append(alternate_names_source)

    try:
        count = build(source, arguments.output, alternate_names_source)
        version_input = f"{sha256_file(source)}:{sha256_file(alternate_names_source)}"
        version = hashlib.sha256(version_input.encode("ascii")).hexdigest()
        version_file = arguments.output.with_name("cities500.version")
        version_file.write_text(version + "\n", encoding="ascii")
        print(f"Built {arguments.output} with {count:,} populated places and localized names.")
    finally:
        for path in downloaded:
            path.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
