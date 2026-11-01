# -*- coding: utf-8 -*-
"""Enough NBT to read and write a structure file, and nothing else.

Minecraft's structure files are gzipped NBT, and the shape of one is small: a size, a
palette of block states, a list of blocks naming a palette index and a position, and an
entity list that these do not use. Writing them by hand needs a reader and a writer, so
here they are - big-endian, the twelve tags, no compression tricks.

Used by _tools/clinic.py.
"""
import gzip
import io
import struct

TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12


class Tag(object):
    """A value that remembers which NBT tag it was, because the writer has to know."""

    __slots__ = ("kind", "value")

    def __init__(self, kind, value):
        self.kind = kind
        self.value = value

    def __repr__(self):
        return "Tag(%d, %r)" % (self.kind, self.value)


def byte(v):
    return Tag(TAG_BYTE, v)


def integer(v):
    return Tag(TAG_INT, v)


def string(v):
    return Tag(TAG_STRING, v)


def compound(d):
    return Tag(TAG_COMPOUND, d)


def lst(kind, items):
    return Tag(TAG_LIST, (kind, items))


class Reader(object):
    def __init__(self, data):
        self.data = data
        self.at = 0

    def take(self, count):
        chunk = self.data[self.at:self.at + count]
        self.at += count
        return chunk

    def number(self, fmt):
        size = struct.calcsize(fmt)
        return struct.unpack(">" + fmt, self.take(size))[0]

    def text(self):
        return self.take(self.number("H")).decode("utf-8")

    def payload(self, kind):
        if kind == TAG_BYTE:
            return Tag(kind, self.number("b"))
        if kind == TAG_SHORT:
            return Tag(kind, self.number("h"))
        if kind == TAG_INT:
            return Tag(kind, self.number("i"))
        if kind == TAG_LONG:
            return Tag(kind, self.number("q"))
        if kind == TAG_FLOAT:
            return Tag(kind, self.number("f"))
        if kind == TAG_DOUBLE:
            return Tag(kind, self.number("d"))
        if kind == TAG_BYTE_ARRAY:
            return Tag(kind, list(self.take(self.number("i"))))
        if kind == TAG_STRING:
            return Tag(kind, self.text())
        if kind == TAG_LIST:
            inner = self.number("b")
            count = self.number("i")
            return Tag(kind, (inner, [self.payload(inner) for _ in range(count)]))
        if kind == TAG_COMPOUND:
            out = {}
            while True:
                child = self.number("b")
                if child == TAG_END:
                    return Tag(kind, out)
                name = self.text()
                out[name] = self.payload(child)
        if kind == TAG_INT_ARRAY:
            return Tag(kind, [self.number("i") for _ in range(self.number("i"))])
        if kind == TAG_LONG_ARRAY:
            return Tag(kind, [self.number("q") for _ in range(self.number("i"))])
        raise ValueError("unknown tag %d" % kind)


def read(path):
    """@return (root name, root compound Tag)"""
    with gzip.open(path, "rb") as handle:
        data = handle.read()
    reader = Reader(data)
    kind = reader.number("b")
    name = reader.text()
    return name, reader.payload(kind)


class Writer(object):
    def __init__(self):
        self.out = io.BytesIO()

    def number(self, fmt, value):
        self.out.write(struct.pack(">" + fmt, value))

    def text(self, value):
        raw = value.encode("utf-8")
        self.number("H", len(raw))
        self.out.write(raw)

    def payload(self, tag):
        kind, value = tag.kind, tag.value
        if kind == TAG_BYTE:
            self.number("b", value)
        elif kind == TAG_SHORT:
            self.number("h", value)
        elif kind == TAG_INT:
            self.number("i", value)
        elif kind == TAG_LONG:
            self.number("q", value)
        elif kind == TAG_FLOAT:
            self.number("f", value)
        elif kind == TAG_DOUBLE:
            self.number("d", value)
        elif kind == TAG_BYTE_ARRAY:
            self.number("i", len(value))
            self.out.write(bytes(bytearray(value)))
        elif kind == TAG_STRING:
            self.text(value)
        elif kind == TAG_LIST:
            inner, items = value
            # An empty list carries TAG_END as its type, which is what vanilla writes.
            self.number("b", inner if items else TAG_END)
            self.number("i", len(items))
            for item in items:
                self.payload(item)
        elif kind == TAG_COMPOUND:
            for name, child in value.items():
                self.number("b", child.kind)
                self.text(name)
                self.payload(child)
            self.number("b", TAG_END)
        elif kind == TAG_INT_ARRAY:
            self.number("i", len(value))
            for item in value:
                self.number("i", item)
        elif kind == TAG_LONG_ARRAY:
            self.number("i", len(value))
            for item in value:
                self.number("q", item)
        else:
            raise ValueError("unknown tag %d" % kind)


def write(path, name, root):
    writer = Writer()
    writer.number("b", root.kind)
    writer.text(name)
    writer.payload(root)
    # mtime 0 so the same structure is the same bytes every run.
    with gzip.GzipFile(path, "wb", mtime=0) as handle:
        handle.write(writer.out.getvalue())
