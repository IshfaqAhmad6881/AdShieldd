#!/usr/bin/env python3
"""
Fill each locale strings.xml from values/strings.xml: keep existing translations,
translate missing keys (Google via deep-translator).
Run: pip install deep-translator && python3 tools/sync_locale_strings.py
"""
from __future__ import annotations

import re
from typing import Optional
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
DEFAULT_FILE = RES / "values/strings.xml"

LANG_MAP: dict[str, str] = {
    "values-ar": "ar",
    "values-bn": "bn",
    "values-de": "de",
    "values-es": "es",
    "values-fa": "fa",
    "values-fr": "fr",
    "values-hi": "hi",
    "values-id": "id",
    "values-it": "it",
    "values-ja": "ja",
    "values-ko": "ko",
    "values-nl": "nl",
    "values-pl": "pl",
    "values-pt": "pt",
    "values-ru": "ru",
    "values-tr": "tr",
    "values-ur": "ur",
    "values-vi": "vi",
    "values-zh-rCN": "zh-CN",
}

NO_TRANSLATE_KEYS = frozenset({"app_name"})


def string_value(elem: ET.Element) -> str:
    return "".join(elem.itertext())


def parse_strings(path: Path) -> tuple[list[str], dict[str, str]]:
    tree = ET.parse(path)
    root = tree.getroot()
    order: list[str] = []
    data: dict[str, str] = {}
    for elem in root.findall("string"):
        name = elem.get("name")
        if not name:
            continue
        order.append(name)
        data[name] = string_value(elem)
    return order, data


def escape_xml_text(s: Optional[str]) -> str:
    if s is None:
        return ""
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def write_strings_xml(path: Path, order: list[str], data: dict[str, str]) -> None:
    lines = ['<?xml version="1.0" encoding="utf-8"?>\n', "<resources>\n"]
    for name in order:
        if name not in data:
            continue
        val = escape_xml_text(data[name])
        lines.append(f'    <string name="{name}">{val}</string>\n')
    lines.append("</resources>\n")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(lines), encoding="utf-8")


def preserve_format_placeholders(original: str, translated: str) -> str:
    if "%" not in original:
        return translated
    if original.count("%") != translated.count("%"):
        return original
    return translated


def main() -> int:
    try:
        from deep_translator import GoogleTranslator
    except ImportError:
        print("Install: pip install deep-translator", file=sys.stderr)
        return 1

    order, default_en = parse_strings(DEFAULT_FILE)
    batch_size = 18
    for folder, target in LANG_MAP.items():
        out_path = RES / folder / "strings.xml"
        existing: dict[str, str] = {}
        if out_path.exists():
            _, existing = parse_strings(out_path)
        translator = GoogleTranslator(source="en", target=target)

        keys_to_translate: list[str] = []
        for key in order:
            if key in NO_TRANSLATE_KEYS:
                continue
            if key in existing and existing[key].strip() != "":
                continue
            keys_to_translate.append(key)

        translations: dict[str, str] = {}
        for i in range(0, len(keys_to_translate), batch_size):
            batch_keys = keys_to_translate[i : i + batch_size]
            batch_src = [default_en[k] for k in batch_keys]
            try:
                if hasattr(translator, "translate_batch"):
                    batch_out = translator.translate_batch(batch_src)
                else:
                    batch_out = [translator.translate(s) for s in batch_src]
                if not batch_out or len(batch_out) != len(batch_keys):
                    raise ValueError("batch length mismatch")
                for k, src, t in zip(batch_keys, batch_src, batch_out):
                    t = t or src
                    translations[k] = preserve_format_placeholders(src, t) or src
            except Exception as exc:
                print(f"{folder} batch@{i}: {exc}", file=sys.stderr)
                for k in batch_keys:
                    translations[k] = default_en[k]
            time.sleep(0.35)

        merged: dict[str, str] = {}
        for key in order:
            if key in NO_TRANSLATE_KEYS:
                merged[key] = default_en[key]
            elif key in existing and existing[key].strip() != "":
                merged[key] = existing[key]
            else:
                merged[key] = translations.get(key, default_en[key])
        write_strings_xml(out_path, order, merged)
        print(folder)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
