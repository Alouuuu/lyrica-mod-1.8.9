"""Summarizes javac errors from a Gradle log: groups by message/symbol/location.

Usage: python -I tools/errsum.py build.log [--files]
"""
import collections
import re
import sys

log = open(sys.argv[1], encoding="utf-8", errors="replace").read()
blocks = re.split(r"\n(?=\S.*?\.java:\d+: error:)", log)
by_key = collections.Counter()
by_file = collections.Counter()
for block in blocks:
    m = re.match(r"(\S+?\.java):(\d+): error: (.*)", block)
    if not m:
        continue
    name = re.split(r"[\\/]", m.group(1))[-1]
    by_file[name] += 1
    sym = re.search(r"symbol:\s+(.*)", block)
    loc = re.search(r"location:\s+(.*)", block)
    key = m.group(3)
    if sym:
        key += " | " + sym.group(1).strip()
    if loc:
        key += " @ " + re.sub(r"variable \w+ of type ", "", loc.group(1).strip())
    by_key[key] += 1

if "--files" in sys.argv:
    for name, n in by_file.most_common():
        print(f"{n:4} {name}")
else:
    for key, n in by_key.most_common():
        print(f"{n:4} {key}")
