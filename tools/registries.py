# -*- coding: utf-8 -*-
"""Turn the four Forge registry classes into Fabric ones.

Registering a thing is the plainest difference between the loaders and the least
interesting: Forge collects entries in a DeferredRegister and asks for them when the bus
says so, Fabric puts them straight in the registry when the class loads. What is worth
keeping is everything around the entries - which items exist, in what order they sit in the
creative tab, and the reasons written beside them - which is why these are translated
rather than retyped.

Run after tools/carry.py when the Forge tree has grown an item, a block, a sound or a
particle.

    python tools/registries.py
"""
import io
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = r"C:\Users\yungspider\IdeaProjects\triage-1201\src\main\java\com\claudemods\triage"
DST = os.path.join(ROOT, "src", "main", "java", "com", "claudemods", "triage")

NOTE = """
/*
 * Translated from the Forge 1.20.1 tree by tools/registries.py. What each thing is, and
 * why, is the same file there; only the lines that hand it to a registry are different.
 */
"""


def common(text, holders):
    text = re.sub(r"import net\.minecraftforge[^\n]*\n", "", text)
    for old, new in holders:
        text = text.replace(old, new)
    text = re.sub(r"    public static final DeferredRegister<[^=]*=[^;]*;\n", "", text, flags=re.S)
    text = re.sub(r"    public static void register\(IEventBus \w+\) \{[^}]*\}",
                  "    /** Touching the class is what registers it; called once from the initialiser. */\n"
                  "    public static void register() {\n    }", text, flags=re.S)
    return text


def imports(text, *lines):
    last = 0
    have = set()
    for m in re.finditer(r"^import [^\n]*\n", text, flags=re.M):
        last = m.end()
        have.add(m.group(0).strip())
    fresh = [line + "\n" for line in lines if line not in have]
    return text[:last] + "".join(fresh) + text[last:]


def tail(text, helpers):
    return text.rstrip()[:-1].rstrip() + "\n" + helpers + "}\n"


def items(text):
    text = common(text, [("RegistryObject<Item>", "Supplier<Item>"),
                         ("RegistryObject<BlockItem>", "Supplier<BlockItem>")])
    text = re.sub(r'ITEMS\.register\("(\w+)",\s*\(\) -> ', r'item("\1", ', text)
    text = re.sub(r'TABS\.register\("(\w+)",\s*\(\) -> ', r'tab("\1", ', text)
    # Vanilla's builder wants to be told which row and column of the creative menu it is
    # going in; both loaders have their own one-liner for "a tab of your own, at the end".
    text = text.replace("CreativeModeTab.builder()", "FabricItemGroup.builder()")
    text = imports(text,
                   "import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;",
                   "import net.minecraft.core.Registry;",
                   "import net.minecraft.core.registries.BuiltInRegistries;")
    return tail(text, '''
    private static <T extends Item> Supplier<T> item(String name, T item) {
        Registry.register(BuiltInRegistries.ITEM, TriageMod.id(name), item);
        return () -> item;
    }

    private static Supplier<CreativeModeTab> tab(String name, CreativeModeTab tab) {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TriageMod.id(name), tab);
        return () -> tab;
    }
''')


def blocks(text):
    text = common(text, [("RegistryObject<Block>", "Supplier<Block>")])
    text = re.sub(r'BLOCKS\.register\("(\w+)",\s*\(\) -> ', r'block("\1", ', text)
    text = imports(text,
                   "import com.claudemods.triage.TriageMod;",
                   "import java.util.function.Supplier;",
                   "import net.minecraft.core.Registry;",
                   "import net.minecraft.core.registries.BuiltInRegistries;")
    return tail(text, '''
    private static Supplier<Block> block(String name, Block block) {
        Registry.register(BuiltInRegistries.BLOCK, TriageMod.id(name), block);
        return () -> block;
    }
''')


def sounds(text):
    text = common(text, [("RegistryObject<SoundEvent>", "Supplier<SoundEvent>")])
    text = re.sub(r'SOUNDS\.register\("(\w+)",\s*\(\) -> ', r'sound("\1", ', text)
    text = imports(text,
                   "import java.util.function.Supplier;",
                   "import net.minecraft.core.Registry;",
                   "import net.minecraft.core.registries.BuiltInRegistries;")
    return tail(text, '''
    private static Supplier<SoundEvent> sound(String name, SoundEvent event) {
        Registry.register(BuiltInRegistries.SOUND_EVENT, TriageMod.id(name), event);
        return () -> event;
    }
''')


def particles(text):
    text = common(text, [("RegistryObject<SimpleParticleType>", "Supplier<SimpleParticleType>")])
    text = re.sub(r'PARTICLES\.register\("(\w+)",\s*\(\) -> ', r'particle("\1", ', text)
    # The constructor is not public in vanilla; Forge opens it, Fabric has a factory.
    text = text.replace("new SimpleParticleType(false)", "FabricParticleTypes.simple(false)")
    text = imports(text,
                   "import java.util.function.Supplier;",
                   "import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;",
                   "import net.minecraft.core.Registry;",
                   "import net.minecraft.core.registries.BuiltInRegistries;")
    return tail(text, '''
    private static Supplier<SimpleParticleType> particle(String name, SimpleParticleType type) {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, TriageMod.id(name), type);
        return () -> type;
    }
''')


JOBS = [
    ("item/ModItems.java", items),
    ("block/ModBlocks.java", blocks),
    ("ModSounds.java", sounds),
    ("ModParticles.java", particles),
]

for rel, transform in JOBS:
    src = os.path.join(SRC, rel.replace("/", os.sep))
    dst = os.path.join(DST, rel.replace("/", os.sep))
    text = transform(io.open(src, encoding="utf-8").read())
    head, body = text.split("\n", 1)
    text = head + "\n" + NOTE + body
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    io.open(dst, "w", encoding="utf-8", newline="\n").write(text)
    left = re.findall(r"Deferred\w*|RegistryObject|IEventBus|ForgeRegistries", text)
    print("%-28s %s" % (rel, "ok" if not left else "STILL FORGE: " + ", ".join(sorted(set(left)))))
