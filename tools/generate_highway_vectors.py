#!/usr/bin/env python3
"""Build compact offline highway vectors from 2025 Census TIGER/Line files.

The generator downloads each state Primary and Secondary Roads shapefile, keeps
only Interstate, U.S., and state routes (RTTYP I/U/S), simplifies the geometry
at three display levels, and writes a small varint-encoded binary asset.

Install the pinned tooling dependency before running:

    python3 -m pip install -r tools/requirements-road-vectors.txt
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import pathlib
import re
import sqlite3
import sys
import urllib.request
import zipfile
from collections.abc import Iterable, Sequence

import shapefile


YEAR = 2025
SOURCE = "U.S. Census Bureau 2025 TIGER/Line Primary and Secondary Roads"
URL_TEMPLATE = (
    "https://www2.census.gov/geo/tiger/TIGER2025/PRISECROADS/"
    "tl_2025_{fips}_prisecroads.zip"
)
QUANTIZATION = 10_000
ROUTE_TYPES = {"I": 0, "U": 1, "S": 2}
LEVELS = (
    # Minimum scale, included route types, simplification tolerance, tile size.
    (1, frozenset({"I"}), 0.035, 360),
    (8, frozenset({"I", "U"}), 0.012, 5),
    (20, frozenset({"I", "U", "S"}), 0.002, 1),
)
STATES = {
    "01": "AL", "02": "AK", "04": "AZ", "05": "AR", "06": "CA",
    "08": "CO", "09": "CT", "10": "DE", "11": "DC", "12": "FL",
    "13": "GA", "15": "HI", "16": "ID", "17": "IL", "18": "IN",
    "19": "IA", "20": "KS", "21": "KY", "22": "LA", "23": "ME",
    "24": "MD", "25": "MA", "26": "MI", "27": "MN", "28": "MS",
    "29": "MO", "30": "MT", "31": "NE", "32": "NV", "33": "NH",
    "34": "NJ", "35": "NM", "36": "NY", "37": "NC", "38": "ND",
    "39": "OH", "40": "OK", "41": "OR", "42": "PA", "44": "RI",
    "45": "SC", "46": "SD", "47": "TN", "48": "TX", "49": "UT",
    "50": "VT", "51": "VA", "53": "WA", "54": "WV", "55": "WI",
    "56": "WY",
}


Point = tuple[float, float]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=pathlib.Path, help="binary asset to write")
    parser.add_argument("manifest", type=pathlib.Path, help="JSON build manifest to write")
    parser.add_argument(
        "--cache-dir",
        type=pathlib.Path,
        default=pathlib.Path(".cache/census-highways-2025"),
        help="download cache (default: .cache/census-highways-2025)",
    )
    parser.add_argument(
        "--states",
        help="optional comma-separated postal codes for a partial development build",
    )
    return parser.parse_args()


def download(url: str, destination: pathlib.Path) -> None:
    if destination.exists() and destination.stat().st_size > 0:
        return
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(destination.suffix + ".part")
    print(f"downloading {url}", flush=True)
    urllib.request.urlretrieve(url, temporary)
    temporary.replace(destination)


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def squared_segment_distance(point: Point, start: Point, end: Point) -> float:
    x, y = start
    dx = end[0] - x
    dy = end[1] - y
    if dx or dy:
        position = ((point[0] - x) * dx + (point[1] - y) * dy) / (dx * dx + dy * dy)
        if position > 1:
            x, y = end
        elif position > 0:
            x += dx * position
            y += dy * position
    dx = point[0] - x
    dy = point[1] - y
    return dx * dx + dy * dy


def simplify(points: Sequence[Point], tolerance: float) -> list[Point]:
    if len(points) <= 2:
        return list(points)
    squared_tolerance = tolerance * tolerance
    keep = bytearray(len(points))
    keep[0] = keep[-1] = 1
    stack = [(0, len(points) - 1)]
    while stack:
        start, end = stack.pop()
        furthest_distance = squared_tolerance
        furthest_index = 0
        for index in range(start + 1, end):
            distance = squared_segment_distance(points[index], points[start], points[end])
            if distance > furthest_distance:
                furthest_distance = distance
                furthest_index = index
        if furthest_index:
            keep[furthest_index] = 1
            stack.append((start, furthest_index))
            stack.append((furthest_index, end))
    return [point for index, point in enumerate(points) if keep[index]]


def tile_for_segment(start: Point, end: Point, tile_degrees: int) -> tuple[int, int]:
    if tile_degrees >= 360:
        return 0, 0
    longitude = (start[0] + end[0]) / 2
    latitude = (start[1] + end[1]) / 2
    return (
        math.floor((longitude + 180.0) / tile_degrees),
        math.floor((latitude + 90.0) / tile_degrees),
    )


def split_into_tiles(points: Sequence[Point], tile_degrees: int) -> Iterable[tuple[int, int, list[Point]]]:
    if len(points) < 2:
        return
    current_tile = tile_for_segment(points[0], points[1], tile_degrees)
    chunk = [points[0], points[1]]
    for index in range(1, len(points) - 1):
        next_tile = tile_for_segment(points[index], points[index + 1], tile_degrees)
        if next_tile == current_tile:
            chunk.append(points[index + 1])
        else:
            yield current_tile[0], current_tile[1], chunk
            current_tile = next_tile
            chunk = [points[index], points[index + 1]]
    yield current_tile[0], current_tile[1], chunk


def parts(shape: shapefile.Shape) -> Iterable[list[Point]]:
    boundaries = list(shape.parts) + [len(shape.points)]
    for index in range(len(boundaries) - 1):
        result = [
            (float(longitude), float(latitude))
            for longitude, latitude, *_ in shape.points[boundaries[index]:boundaries[index + 1]]
        ]
        if len(result) >= 2:
            yield result


def route_label(full_name: str, route_type: str, state: str) -> str:
    compact = " ".join((full_name or "").replace("- ", "-").split())
    if not compact:
        return ""
    number = re.search(r"\d+[A-Za-z]?", compact)
    if not number:
        return ""
    route_number = number.group(0).upper()
    if route_type == "I":
        prefix = "H-" if state == "HI" and re.search(r"\bH-?\s*\d", compact, re.I) else "I-"
        label = f"{prefix}{route_number}"
    elif route_type == "U":
        label = f"US {route_number}"
    else:
        label = f"{state} {route_number}"

    qualifiers = (
        (r"\b(?:Business|Bus)\b", "BUS"),
        (r"\b(?:Bypass|Byp)\b", "BYP"),
        (r"\b(?:Alternate|Alt)\b", "ALT"),
        (r"\bSpur\b", "SPUR"),
        (r"\bLoop\b", "LOOP"),
        (r"\bTruck\b", "TRUCK"),
    )
    for pattern, suffix in qualifiers:
        if re.search(pattern, compact, re.I):
            label += f" {suffix}"
            break
    return label


def encode_unsigned(value: int) -> bytes:
    if value < 0:
        raise ValueError(f"unsigned varint cannot encode {value}")
    encoded = bytearray()
    while value >= 0x80:
        encoded.append((value & 0x7F) | 0x80)
        value >>= 7
    encoded.append(value)
    return bytes(encoded)


def encode_signed(value: int) -> bytes:
    return encode_unsigned((value << 1) ^ (value >> 63))


def encode_points(points: Sequence[Point]) -> bytes:
    quantized: list[tuple[int, int]] = []
    for longitude, latitude in points:
        point = (round(longitude * QUANTIZATION), round(latitude * QUANTIZATION))
        if not quantized or point != quantized[-1]:
            quantized.append(point)
    if len(quantized) < 2:
        return b""
    result = bytearray()
    previous_x = previous_y = 0
    for index, (x, y) in enumerate(quantized):
        result.extend(encode_signed(x if index == 0 else x - previous_x))
        result.extend(encode_signed(y if index == 0 else y - previous_y))
        previous_x, previous_y = x, y
    return encode_unsigned(len(quantized)) + bytes(result)


def write_string(handle, value: str) -> None:
    encoded = value.encode("utf-8")
    handle.write(encode_unsigned(len(encoded)))
    handle.write(encoded)


def prepare_database(path: pathlib.Path) -> sqlite3.Connection:
    if path.exists():
        path.unlink()
    database = sqlite3.connect(path)
    database.execute("PRAGMA journal_mode=OFF")
    database.execute("PRAGMA synchronous=OFF")
    database.execute(
        "CREATE TABLE roads (lod INTEGER, tile_x INTEGER, tile_y INTEGER, "
        "class_id INTEGER, label_id INTEGER, point_data BLOB)"
    )
    database.execute("CREATE INDEX roads_tile ON roads(lod, tile_x, tile_y)")
    return database


def process_state(
    archive: pathlib.Path,
    state: str,
    database: sqlite3.Connection,
    labels: dict[str, int],
) -> dict[str, int]:
    counts = {route_type: 0 for route_type in ROUTE_TYPES}
    output_chunks = 0
    with zipfile.ZipFile(archive) as zipped:
        shape_name = next(name for name in zipped.namelist() if name.endswith(".shp"))
        database_name = next(name for name in zipped.namelist() if name.endswith(".dbf"))
        with zipped.open(shape_name) as shape_file, zipped.open(database_name) as database_file:
            reader = shapefile.Reader(shp=shape_file, dbf=database_file)
            for shape_record in reader.iterShapeRecords():
                route_type = (shape_record.record["RTTYP"] or "").strip()
                if route_type not in ROUTE_TYPES:
                    continue
                counts[route_type] += 1
                label = route_label(shape_record.record["FULLNAME"] or "", route_type, state)
                label_id = labels.setdefault(label, len(labels))
                for line in parts(shape_record.shape):
                    if state == "AK":
                        line = [
                            (longitude - 360.0 if longitude > 0.0 else longitude, latitude)
                            for longitude, latitude in line
                        ]
                    for lod, (_, included_types, tolerance, tile_degrees) in enumerate(LEVELS):
                        if route_type not in included_types:
                            continue
                        simplified = simplify(line, tolerance)
                        for tile_x, tile_y, chunk in split_into_tiles(simplified, tile_degrees):
                            point_data = encode_points(chunk)
                            if not point_data:
                                continue
                            database.execute(
                                "INSERT INTO roads VALUES (?, ?, ?, ?, ?, ?)",
                                (lod, tile_x, tile_y, ROUTE_TYPES[route_type], label_id, point_data),
                            )
                            output_chunks += 1
    database.commit()
    counts["chunks"] = output_chunks
    return counts


def write_binary(
    output: pathlib.Path,
    database: sqlite3.Connection,
    labels: dict[str, int],
) -> dict[str, object]:
    output.parent.mkdir(parents=True, exist_ok=True)
    ordered_labels = [""] * len(labels)
    for label, index in labels.items():
        ordered_labels[index] = label

    level_counts: list[dict[str, int]] = []
    with output.open("wb") as handle:
        handle.write(b"RTHW")
        handle.write(encode_unsigned(2))
        handle.write(encode_unsigned(QUANTIZATION))
        write_string(handle, SOURCE)
        handle.write(encode_unsigned(len(ordered_labels)))
        for label in ordered_labels:
            write_string(handle, label)
        handle.write(encode_unsigned(len(LEVELS)))

        for lod, (minimum_scale, _, tolerance, tile_degrees) in enumerate(LEVELS):
            tiles = database.execute(
                "SELECT tile_x, tile_y, COUNT(*) FROM roads WHERE lod = ? "
                "GROUP BY tile_x, tile_y ORDER BY tile_y, tile_x",
                (lod,),
            ).fetchall()
            handle.write(encode_unsigned(minimum_scale))
            handle.write(encode_unsigned(tile_degrees))
            handle.write(encode_unsigned(len(tiles)))
            road_count = 0
            for tile_x, tile_y, count in tiles:
                handle.write(encode_signed(tile_x))
                handle.write(encode_signed(tile_y))
                handle.write(encode_unsigned(count))
                road_count += count
                rows = database.execute(
                    "SELECT class_id, label_id, point_data FROM roads "
                    "WHERE lod = ? AND tile_x = ? AND tile_y = ? ORDER BY rowid",
                    (lod, tile_x, tile_y),
                )
                for class_id, label_id, point_data in rows:
                    handle.write(encode_unsigned(class_id))
                    handle.write(encode_unsigned(label_id))
                    handle.write(point_data)
            level_counts.append(
                {
                    "minimumScale": minimum_scale,
                    "tileDegrees": tile_degrees,
                    "toleranceDegrees": tolerance,
                    "tiles": len(tiles),
                    "roads": road_count,
                }
            )
    return {"labels": len(ordered_labels), "levels": level_counts}


def main() -> int:
    arguments = parse_args()
    selected_codes = None
    if arguments.states:
        selected_codes = {code.strip().upper() for code in arguments.states.split(",") if code.strip()}
        unknown = selected_codes - set(STATES.values())
        if unknown:
            raise ValueError(f"unknown state codes: {sorted(unknown)}")
    selected_states = {
        fips: state for fips, state in STATES.items()
        if selected_codes is None or state in selected_codes
    }
    if not selected_states:
        raise ValueError("no states selected")

    arguments.cache_dir.mkdir(parents=True, exist_ok=True)
    work_database = arguments.cache_dir / "highway-vectors.sqlite"
    database = prepare_database(work_database)
    labels = {"": 0}
    state_counts: dict[str, dict[str, int]] = {}
    sources: dict[str, dict[str, object]] = {}

    try:
        for fips, state in selected_states.items():
            url = URL_TEMPLATE.format(fips=fips)
            archive = arguments.cache_dir / pathlib.PurePosixPath(url).name
            download(url, archive)
            digest = sha256(archive)
            counts = process_state(archive, state, database, labels)
            state_counts[state] = counts
            sources[state] = {"url": url, "sha256": digest, "bytes": archive.stat().st_size}
            print(f"{state}: {counts}", flush=True)

        binary_stats = write_binary(arguments.output, database, labels)
    finally:
        database.close()

    output_digest = sha256(arguments.output)
    manifest = {
        "source": SOURCE,
        "year": YEAR,
        "routeTypes": ["I", "U", "S"],
        "quantization": QUANTIZATION,
        "states": sorted(selected_states.values()),
        "stateCounts": state_counts,
        "sources": sources,
        "binary": {
            **binary_stats,
            "bytes": arguments.output.stat().st_size,
            "sha256": output_digest,
        },
    }
    arguments.manifest.parent.mkdir(parents=True, exist_ok=True)
    arguments.manifest.write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(
        f"wrote {arguments.output} ({arguments.output.stat().st_size:,} bytes, sha256 {output_digest})",
        flush=True,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
