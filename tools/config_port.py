# -*- coding: utf-8 -*-
"""Turn the NeoForge config spec into the Fabric one.

NeoForge hands a mod a config system; Fabric hands it a folder. The spec in the other
tree is not only a list of numbers - it is where every key name, default, range and
comment lives, and there are three hundred of them. Rewriting that by hand would be a
week of typing and a permanent second copy to keep in step.

So it is read instead. The builder chain in Config.java is completely regular, and
everything a TOML file needs is already in it; this pulls out the table and writes the
Fabric Config.java around a small reader and writer. Run it again whenever the NeoForge
config grows a setting.

The point of going to this trouble rather than inventing a Fabric-shaped config: the file
that lands in config/triage-server.toml is then the same file, key for key and comment for
comment, as the one NeoForge writes. A pack that tunes the mod tunes it once.

    python tools/config_port.py
"""
import io
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = r"C:\Users\yungspider\IdeaProjects\triage\src\main\java\com\claudemods\triage\Config.java"
DST = os.path.join(os.path.dirname(HERE), "src", "main", "java", "com", "claudemods", "triage", "Config.java")


def strip_comments(text):
    """Drop // and /* */ without touching string literals."""
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == '"':
            out.append(c)
            i += 1
            while i < n:
                out.append(text[i])
                if text[i] == "\\":
                    i += 1
                    if i < n:
                        out.append(text[i])
                    i += 1
                    continue
                if text[i] == '"':
                    i += 1
                    break
                i += 1
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                i += 1
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "*":
            i += 2
            while i + 1 < n and not (text[i] == "*" and text[i + 1] == "/"):
                i += 1
            i += 2
            continue
        out.append(c)
        i += 1
    return "".join(out)


def split_args(text):
    """Split a comma-separated argument list, respecting nesting and strings."""
    args = []
    depth = 0
    start = 0
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == '"':
            i += 1
            while i < n:
                if text[i] == "\\":
                    i += 2
                    continue
                if text[i] == '"':
                    break
                i += 1
        elif c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        elif c == "," and depth == 0:
            args.append(text[start:i].strip())
            start = i + 1
        i += 1
    tail = text[start:].strip()
    if tail:
        args.append(tail)
    return args


def call_args(text, name):
    """The argument text of the first `.name(` call in text, balanced."""
    marker = "." + name + "("
    at = text.find(marker)
    if at < 0:
        return None
    i = at + len(marker)
    depth = 1
    start = i
    n = len(text)
    while i < n:
        c = text[i]
        if c == '"':
            i += 1
            while i < n:
                if text[i] == "\\":
                    i += 2
                    continue
                if text[i] == '"':
                    break
                i += 1
        elif c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
            if depth == 0:
                return text[start:i]
        i += 1
    return None


def statements(text):
    """Split Java statements on the semicolons that are outside string literals.

    Splitting on every semicolon looks like it works until a comment in the spec ends a
    sentence with one, which cuts a statement in half and loses the section it opened.
    """
    out = []
    depth = 0
    start = 0
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == '"':
            i += 1
            while i < n:
                if text[i] == "\\":
                    i += 2
                    continue
                if text[i] == '"':
                    break
                i += 1
        elif c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        elif c == ";" and depth == 0:
            out.append(text[start:i])
            start = i + 1
        i += 1
    out.append(text[start:])
    return out


def unquote(literal):
    literal = literal.strip()
    if not (literal.startswith('"') and literal.endswith('"')):
        raise ValueError("not a string literal: " + literal)
    return literal[1:-1]


source = io.open(SRC, encoding="utf-8").read()
clean = strip_comments(source)

# ---- the Values fields, for their types ----------------------------------
values_at = clean.index("final class Values")
values_text = clean[values_at:]
field_type = {}
for m in re.finditer(r"public (float|int|boolean|double|long) ([a-zA-Z0-9_]+)\s*=", values_text):
    field_type[m.group(2)] = m.group(1)

# ---- bake(), for which spec constant fills which field -------------------
bake_at = clean.index("public static void bake()")
bake_text = clean[bake_at:clean.index("private static List<ItemMatcher> parse(")]
const_field = {}
for m in re.finditer(r"baked\.([a-zA-Z0-9_]+)\s*=\s*([A-Z0-9_]+)\.get\(\)", bake_text):
    const_field[m.group(2)] = m.group(1)
const_kind = {}
for m in re.finditer(r"baked\.medicine\.put\(MedicineKind\.([A-Z_]+),\s*parse\(([A-Z0-9_]+)\.get\(\)\)\)", bake_text):
    const_kind[m.group(2)] = m.group(1)

# ---- the builder chain, for keys, defaults, ranges and comments -----------
# Past the opening brace: the splitter counts brackets, and a block that starts one
# deep never comes back to the top level where the statements are.
static_at = clean.index("    static {") + len("    static {")
static_text = clean[static_at:clean.index("SPEC = BUILDER.build();")]

sections = []       # (path, comment lines)
entries = []        # dicts
stack = []
seen_sections = set()

for raw in statements(static_text):
    stmt = " ".join(raw.split())
    if not stmt or "BUILDER" not in stmt:
        continue
    comment = []
    args = call_args(stmt, "comment")
    if args is not None:
        comment = [unquote(a) for a in split_args(args)]
    if ".push(" in stmt:
        name = unquote(split_args(call_args(stmt, "push"))[0])
        stack.append(name)
        path = ".".join(stack)
        if path in seen_sections:
            raise SystemExit("two sections called " + path)
        seen_sections.add(path)
        sections.append((path, comment))
        continue
    if "BUILDER.pop()" in stmt:
        stack.pop()
        continue
    m = re.match(r"([A-Z0-9_]+) = BUILDER", stmt)
    if not m:
        continue
    const = m.group(1)
    path = ".".join(stack)
    if ".defineInRange(" in stmt:
        a = split_args(call_args(stmt, "defineInRange"))
        entries.append(dict(kind="num", section=path, key=unquote(a[0]), const=const,
                            default=a[1], low=a[2], high=a[3], comment=comment))
    elif ".defineList(" in stmt:
        a = split_args(call_args(stmt, "defineList"))
        entries.append(dict(kind="items", section=path, key=unquote(a[0]), const=const,
                            comment=comment))
    elif ".define(" in stmt:
        a = split_args(call_args(stmt, "define"))
        entries.append(dict(kind="flag", section=path, key=unquote(a[0]), const=const,
                            default=a[1], comment=comment))

# ---- emit ----------------------------------------------------------------
def java_string(text):
    return '"' + text.replace("\\", "\\\\").replace('"', '\\"') + '"'


def comment_args(lines):
    if not lines:
        return ""
    return ",\n                " + ",\n                ".join(java_string(l) for l in lines)


by_section = {}
for e in entries:
    by_section.setdefault(e["section"], []).append(e)

method_names = {}
used = set()
for path, _ in sections:
    name = re.sub(r"[^a-zA-Z0-9]", "", path.title())
    name = name[0].lower() + name[1:]
    while name in used:
        name += "Part"
    used.add(name)
    method_names[path] = name

blocks = []
for path, comment in sections:
    lines = ["    private static void %s() {" % method_names[path]]
    lines.append("        section(%s%s);" % (java_string(path), comment_args(comment)))
    for e in by_section.get(path, []):
        if e["kind"] == "items":
            kind = const_kind.get(e["const"])
            if kind is None:
                raise SystemExit("no MedicineKind for " + e["const"])
            lines.append("        items(%s, MedicineKind.%s%s);"
                         % (java_string(e["key"]), kind, comment_args(e["comment"])))
            continue
        field = const_field.get(e["const"])
        if field is None:
            raise SystemExit("bake() never reads " + e["const"])
        type_ = field_type.get(field)
        if type_ is None:
            raise SystemExit("Values has no field " + field)
        if e["kind"] == "flag":
            lines.append("        flag(%s, %s, v -> v.%s, (v, x) -> v.%s = x%s);"
                         % (java_string(e["key"]), e["default"], field, field,
                            comment_args(e["comment"])))
        else:
            cast = {"float": "(float) ", "int": "(int) ", "long": "(long) ", "double": ""}[type_]
            kind = {"int": "Kind.WHOLE", "long": "Kind.WHOLE",
                    "float": "Kind.SINGLE", "double": "Kind.DOUBLE"}[type_]
            lines.append("        num(%s, %s, %s, %s, %s, v -> v.%s, (v, x) -> v.%s = %sx%s);"
                         % (java_string(e["key"]), e["default"], e["low"], e["high"], kind,
                            field, field, cast, comment_args(e["comment"])))
    lines.append("    }")
    blocks.append("\n".join(lines))

# The Values class and ItemMatcher come across whole - they are plain Java with no
# NeoForge in them, and copying beats regenerating something already written.
carry_at = source.index("    /** One line of the medicine config")
carry = source[carry_at:source.rindex("}")].rstrip()

template = io.open(os.path.join(HERE, "config_template.java"), encoding="utf-8").read()
out = (template
       .replace("//<<SECTIONS>>", "\n\n".join(blocks))
       .replace("//<<CALLS>>", "\n".join("        %s();" % method_names[p] for p, _ in sections))
       .replace("//<<CARRIED>>", carry))
io.open(DST, "w", encoding="utf-8").write(out)
print("wrote %s" % DST)
print("%d sections, %d settings (%d numbers, %d switches, %d item lists)"
      % (len(sections), len(entries),
         sum(1 for e in entries if e["kind"] == "num"),
         sum(1 for e in entries if e["kind"] == "flag"),
         sum(1 for e in entries if e["kind"] == "items")))
