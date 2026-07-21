#!/usr/bin/env python3
"""Convert the Census 1:5m state KML into compact app vector data."""

from __future__ import annotations

import json
import pathlib
import sys
import xml.etree.ElementTree as ET


STATE_CODES = {
    "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL", "GA", "HI", "ID",
    "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO",
    "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA",
    "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY",
}
KML = {"k": "http://www.opengis.net/kml/2.2"}
SOURCE = "https://www2.census.gov/geo/tiger/GENZ2025/kml/cb_2025_us_state_5m.zip"


def coordinates(element: ET.Element | None) -> list[list[float]]:
    if element is None or not element.text:
        return []
    points: list[list[float]] = []
    for token in element.text.split():
        longitude, latitude, *_ = token.split(",")
        point = [round(float(longitude), 4), round(float(latitude), 4)]
        if not points or points[-1] != point:
            points.append(point)
    if len(points) > 1 and points[0] == points[-1]:
        points.pop()
    return points


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: generate_state_vectors.py input.kml output.json", file=sys.stderr)
        return 2

    input_path = pathlib.Path(sys.argv[1])
    output_path = pathlib.Path(sys.argv[2])
    root = ET.parse(input_path).getroot()
    states: list[dict[str, object]] = []

    for placemark in root.findall(".//k:Placemark", KML):
        code_element = placemark.find(".//k:SimpleData[@name='STUSPS']", KML)
        code = code_element.text.strip() if code_element is not None and code_element.text else ""
        if code not in STATE_CODES:
            continue

        polygons: list[dict[str, object]] = []
        for polygon in placemark.findall(".//k:Polygon", KML):
            outer = coordinates(
                polygon.find("./k:outerBoundaryIs/k:LinearRing/k:coordinates", KML)
            )
            holes = [
                ring
                for ring in (
                    coordinates(element)
                    for element in polygon.findall(
                        "./k:innerBoundaryIs/k:LinearRing/k:coordinates", KML
                    )
                )
                if len(ring) >= 3
            ]
            if len(outer) >= 3:
                polygons.append({"outer": outer, "holes": holes})

        if polygons:
            states.append({"code": code, "polygons": polygons})

    states.sort(key=lambda state: state["code"])
    if {state["code"] for state in states} != STATE_CODES:
        missing = sorted(STATE_CODES - {state["code"] for state in states})
        raise RuntimeError(f"Census vectors did not contain the expected jurisdictions: {missing}")

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(
        json.dumps({"source": SOURCE, "states": states}, separators=(",", ":")),
        encoding="utf-8",
    )
    print(f"wrote {len(states)} jurisdictions to {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
