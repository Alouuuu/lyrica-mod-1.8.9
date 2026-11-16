"""Rewrites Minecraft 1.21/Fabric/LWJGL3 type references to the 1.8.9 facade package.

`net.minecraft.client.gui.DrawContext` -> `dev.relaylyrics.platform.mc.DrawContext`, etc.
The facade classes keep upstream's (Yarn) names so ported code changes as little as possible.
Usage (from legacy-1.8.9/): python -I tools/rewrite_platform_refs.py <dir-or-file>...
"""
import pathlib, re, sys

FACADE = "dev.relaylyrics.platform.mc."
PATTERNS = [
    re.compile(r"\bnet\.minecraft\.(?:[a-z0-9_]+\.)*([A-Z]\w*)"),
    re.compile(r"\bnet\.fabricmc\.(?:[a-z0-9_]+\.)*([A-Z]\w*)"),
    re.compile(r"\borg\.lwjgl\.(?:glfw|system|util\.tinyfd)\.([A-Z]\w*)"),
    re.compile(r"\bcom\.mojang\.blaze3d\.(?:[a-z0-9_]+\.)*([A-Z]\w*)"),
    re.compile(r"\borg\.joml\.([A-Z]\w*)"),
]

# Only names that have a facade class are rewritten; real 1.8.9/Forge references are left alone.
FACADE_DIR = pathlib.Path(__file__).resolve().parents[1] / "src/main/java/dev/relaylyrics/platform/mc"
KNOWN = {f.stem for f in FACADE_DIR.glob("*.java")}


def rewrite(path: pathlib.Path) -> bool:
    text = path.read_text(encoding="utf-8")
    new = text
    for pattern in PATTERNS:
        new = pattern.sub(lambda m: FACADE + m.group(1) if m.group(1) in KNOWN else m.group(0), new)
    # Collapse duplicate imports produced by different source packages.
    seen, lines = set(), []
    for line in new.split("\n"):
        if line.startswith("import " + FACADE):
            if line in seen:
                continue
            seen.add(line)
        lines.append(line)
    new = "\n".join(lines)
    if new != text:
        path.write_text(new, encoding="utf-8", newline="\n")
        return True
    return False

for arg in sys.argv[1:]:
    root = pathlib.Path(arg)
    for f in ([root] if root.is_file() else sorted(root.rglob("*.java"))):
        if rewrite(f):
            print("rewrote", f)
