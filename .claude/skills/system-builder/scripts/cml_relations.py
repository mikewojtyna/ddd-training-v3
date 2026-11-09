"""Minimal, dependency-free reader of Context Mapper (CML) relationships, shared by waves.py and comm_matrix.py.

It understands the relationship forms a context map normally uses:

    Up [U,OHS,PL]->[D,ACL] Down : Name        Down [D,ACL]<-[U,OHS] Up
    Up -> Down                                Down <- Up
    Supplier [U,S]->[D,C] Customer            Customer [D,C]<-[U,S] Supplier
    A [P]<->[P] B      A <-> B                A [SK]<->[SK] B
    A Partnership B    A Shared-Kernel B
    A Upstream-Downstream B   A Downstream-Upstream B
    A Supplier-Customer B     A Customer-Supplier B

It also reads `BoundedContext X { type = SYSTEM ... }` so external parties can be skipped, and
`contains X` lines of the ContextMap. It isn't a full CML parser: run the real validator for syntax.
"""

import re
from dataclasses import dataclass, field

NAME = r"[A-Za-z_][A-Za-z0-9_]*"
ROLES = r"(?:\[[^\]]*\])?"

ARROW_RE = re.compile(
    rf"^\s*(?P<a>{NAME})\s*(?P<ra>{ROLES})\s*(?P<arrow><->|->|<-)\s*(?P<rb>{ROLES})\s*(?P<b>{NAME})"
    rf"(?:\s*:\s*(?P<name>{NAME}))?"
)
KEYWORD_RE = re.compile(
    rf"^\s*(?P<a>{NAME})\s+(?P<kw>Partnership|Shared-Kernel|Upstream-Downstream|Downstream-Upstream|"
    rf"Supplier-Customer|Customer-Supplier)\s+(?P<b>{NAME})(?:\s*:\s*(?P<name>{NAME}))?"
)
CONTEXT_RE = re.compile(rf"^\s*BoundedContext\s+(?P<name>{NAME})\b")
TYPE_RE = re.compile(r"^\s*type\s*=\s*(?P<type>[A-Z_]+)")
CONTAINS_RE = re.compile(rf"^\s*contains\s+(?P<names>{NAME}(?:\s*,\s*{NAME})*)")


@dataclass
class Relationship:
    upstream: str            # for symmetric relationships: the left-hand context
    downstream: str          # for symmetric relationships: the right-hand context
    symmetric: bool          # Partnership or Shared Kernel
    kind: str                # "U/D", "C/S", "Partnership", "Shared Kernel"
    name: str = ""
    upstream_roles: str = ""
    downstream_roles: str = ""
    comment: str = ""        # the // comment lines right above the relationship
    line: int = 0

    def label(self):
        if self.symmetric:
            return f"{self.upstream} <-> {self.downstream}"
        return f"{self.upstream} -> {self.downstream}"


@dataclass
class ContextMap:
    relationships: list = field(default_factory=list)
    contexts: dict = field(default_factory=dict)      # name -> type ("FEATURE", "SYSTEM", ... or "")
    contained: list = field(default_factory=list)     # names from `contains`

    def external(self):
        return {name for name, typ in self.contexts.items() if typ == "SYSTEM"}

    def internal_contexts(self):
        names = list(dict.fromkeys(self.contained or list(self.contexts)))
        for rel in self.relationships:
            for name in (rel.upstream, rel.downstream):
                if name not in names:
                    names.append(name)
        return [n for n in names if n not in self.external()]

    def internal_relationships(self):
        ext = self.external()
        return [r for r in self.relationships if r.upstream not in ext and r.downstream not in ext]


def _strip_block_comments(text):
    # keep line numbers stable: replace comment characters with spaces, keep newlines
    return re.sub(r"/\*.*?\*/", lambda m: re.sub(r"[^\n]", " ", m.group(0)), text, flags=re.S)


def _roles(raw):
    return raw.strip("[]").replace(" ", "") if raw else ""


def parse(paths):
    cmap = ContextMap()
    for path in paths:
        with open(path, encoding="utf-8") as handle:
            text = _strip_block_comments(handle.read())
        pending_comment = []
        current_context = None
        depth = 0
        for number, raw_line in enumerate(text.splitlines(), start=1):
            stripped = raw_line.strip()
            if stripped.startswith("//"):
                pending_comment.append(stripped[2:].strip())
                continue
            line = raw_line.split("//", 1)[0]
            context_match = CONTEXT_RE.match(line)
            if context_match:
                current_context = context_match.group("name")
                cmap.contexts.setdefault(current_context, "")
                depth = 0
            if current_context is not None:
                type_match = TYPE_RE.match(line)
                if type_match and depth == 1:
                    cmap.contexts[current_context] = type_match.group("type")
                depth += line.count("{") - line.count("}")
                if depth <= 0 and "}" in line:
                    current_context = None
                pending_comment = []
                continue
            contains_match = CONTAINS_RE.match(line)
            if contains_match:
                for name in re.split(r"\s*,\s*", contains_match.group("names")):
                    if name not in cmap.contained:
                        cmap.contained.append(name)
                pending_comment = []
                continue
            rel = _relationship(line)
            if rel:
                rel.comment = " ".join(pending_comment)
                rel.line = number
                cmap.relationships.append(rel)
            if stripped:
                pending_comment = []
    return cmap


def _relationship(line):
    match = ARROW_RE.match(line)
    if match:
        a, b = match.group("a"), match.group("b")
        ra, rb = _roles(match.group("ra")), _roles(match.group("rb"))
        name = match.group("name") or ""
        arrow = match.group("arrow")
        if arrow == "<->":
            kind = "Shared Kernel" if "SK" in ra.split(",") else "Partnership"
            return Relationship(a, b, True, kind, name, ra, rb)
        if arrow == "->":
            up, down, up_roles, down_roles = a, b, ra, rb
        else:
            up, down, up_roles, down_roles = b, a, rb, ra
        kind = "C/S" if "S" in up_roles.split(",") or "C" in down_roles.split(",") else "U/D"
        return Relationship(up, down, False, kind, name, up_roles, down_roles)
    match = KEYWORD_RE.match(line)
    if match:
        a, b, kw = match.group("a"), match.group("b"), match.group("kw")
        name = match.group("name") or ""
        if kw == "Partnership":
            return Relationship(a, b, True, "Partnership", name)
        if kw == "Shared-Kernel":
            return Relationship(a, b, True, "Shared Kernel", name)
        if kw == "Upstream-Downstream":
            return Relationship(a, b, False, "U/D", name)
        if kw == "Downstream-Upstream":
            return Relationship(b, a, False, "U/D", name)
        if kw == "Supplier-Customer":
            return Relationship(a, b, False, "C/S", name)
        if kw == "Customer-Supplier":
            return Relationship(b, a, False, "C/S", name)
    return None
