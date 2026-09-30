#!/usr/bin/env python3
"""Validate APK ZIP and ELF alignment for Android 16 KB page-size readiness."""

from __future__ import annotations

import struct
import sys
import zipfile
from pathlib import Path

MIN_ALIGN = 0x4000


def check_elf(name: str, data: bytes) -> list[str]:
    problems: list[str] = []
    if len(data) < 64 or data[:4] != b"\x7fELF":
        return [f"{name}: not an ELF binary"]

    elf_class = data[4]
    if elf_class == 2:
        phoff = struct.unpack_from("<Q", data, 32)[0]
        phentsize = struct.unpack_from("<H", data, 54)[0]
        phnum = struct.unpack_from("<H", data, 56)[0]
        for index in range(phnum):
            offset = phoff + index * phentsize
            p_type = struct.unpack_from("<I", data, offset)[0]
            if p_type == 1:
                p_align = struct.unpack_from("<Q", data, offset + 48)[0]
                if p_align < MIN_ALIGN:
                    problems.append(f"{name}: PT_LOAD p_align={p_align:#x}")
    elif elf_class == 1:
        phoff = struct.unpack_from("<I", data, 28)[0]
        phentsize = struct.unpack_from("<H", data, 42)[0]
        phnum = struct.unpack_from("<H", data, 44)[0]
        for index in range(phnum):
            offset = phoff + index * phentsize
            p_type = struct.unpack_from("<I", data, offset)[0]
            if p_type == 1:
                p_align = struct.unpack_from("<I", data, offset + 28)[0]
                if p_align < MIN_ALIGN:
                    problems.append(f"{name}: PT_LOAD p_align={p_align:#x}")
    else:
        problems.append(f"{name}: unsupported ELF class {elf_class}")

    return problems


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: check_apk_16kb.py <apk>")
        return 2

    apk = Path(sys.argv[1])
    if not apk.is_file():
        print(f"APK not found: {apk}")
        return 2

    with zipfile.ZipFile(apk) as bundle:
        so_entries = [
            name
            for name in bundle.namelist()
            if name.startswith("lib/") and name.endswith(".so")
        ]
        problems: list[str] = []
        for name in so_entries:
            problems.extend(check_elf(name, bundle.read(name)))

    print(f"Native libraries found: {len(so_entries)}")
    if problems:
        print("16 KB ELF alignment failures:")
        for problem in problems:
            print(f" - {problem}")
        return 1

    print("All packaged native libraries satisfy 16 KB PT_LOAD alignment.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
