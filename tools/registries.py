# -*- coding: utf-8 -*-
"""Turn the five NeoForge registry classes into Fabric ones.

Registering a thing is the plainest difference between the two loaders and the least
interesting: NeoForge collects entries in a DeferredRegister and asks for them when the
bus says so, Fabric puts them straight in the registry when the class loads. What is worth
keeping is everything around the entries - which items exist, in what order they sit in
the creative tab, and the reasons written beside them - and that is why these are
translated rather than retyped.

Run after tools/carry.py when the NeoForge tree has grown an item, a block, a sound, a
particle or a component.

    python tools/registries.py
"""
import io
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = r"C:\Users\yungspider\IdeaProjects\triage\src\main\java\com\claudemods\triage"
DST = os.path.join(ROOT, "src", "main", "java", "com", "claudemods", "triage")

NOTE = """
/*
 * Translated from the NeoForge tree by tools/registries.py. What each thing is, and why,
 * is the same file there; only the four lines that hand it to a registry are different.
 */
"""


def common(text):
    """The parts every one of the five shares."""
    text = re.sub(r"import net\.neoforged[^\n]*\n", "", text)
    text = text.replace("    public static void register(IEventBus modBus) {",
                        "    /** Touching the class is what registers it; called once from the initialiser. */\n"
                        "    public static void register() {")
    text = text.replace("    public static void register(IEventBus bus) {",
                        "    /** Touching the class is what registers it; called once from the initialiser. */\n"
                        "    public static void register() {")
    # Whatever the old register() did, the class initialiser has already done.
    text = re.sub(r"(public static void register\(\) \{)[^}]*\}", r"\1\n    }", text, flags=re.S)
    return text


def imports(text, *lines):
    """Slot imports in after the last existing one, which keeps them sorted well enough."""
    last = 0
    have = set()
    for m in re.finditer(r"^import [^\n]*\n", text, flags=re.M):
        last = m.end()
        have.add(m.group(0).strip())
    fresh = [line + "\n" for line in lines if line not in have]
    return text[:last] + "".join(fresh) + text[last:]


def items(text):
    text = common(text)
    text = re.sub(r"    public static final DeferredRegister\.Items ITEMS[^;]*;\n", "", text, flags=re.S)
    text = re.sub(r"    public static final DeferredRegister<CreativeModeTab> TABS[^;]*;\n", "", text, flags=re.S)
    text = text.replace("DeferredItem<Item>", "Supplier<Item>")
    text = text.replace("DeferredItem<BlockItem>", "Supplier<BlockItem>")
    text = re.sub(r'ITEMS\.register\("(\w+)",\s*\(\) -> ', r'item("\1", ', text)
    text = re.sub(r'ITEMS\.registerSimpleBlockItem\(ModBlocks\.(\w+)\)',
                  r'blockItem(ModBlocks.\1)', text)
    text = re.sub(r'TABS\.register\("(\w+)",\s*\(\) -> ', r'tab("\1", ', text)
    # Vanilla's builder wants to be told which row and column of the creative menu it is
    # going in; both loaders have their own one-liner for "a tab of your own, at the end".
    text = text.replace("CreativeModeTab.builder()", "FabricItemGroup.builder()")
    text = imports(text,
                   "import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;",
                   "import net.minecraft.core.Registry;",
                   "import net.minecraft.core.registries.BuiltInRegistries;",
                   "import net.minecraft.world.level.block.Block;")
    helpers = '''
    private static Supplier<Item> item(String name, Item item) {
        Registry.register(BuiltInRegistries.ITEM, TriageMod.id(name), item);
        return () -> item;
    }

    /**
     * The item that places a block, named after the block so the two cannot drift apart.
     */
    private static Supplier<BlockItem> blockItem(Supplier<Block> block) {
        Block held = block.get();
        BlockItem item = new BlockItem(held, new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, BuiltInRegistries.BLOCK.getKey(held), item);
        return () -> item;
    }

    private static Supplier<CreativeModeTab> tab(String name, CreativeModeTab tab) {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TriageMod.id(name), tab);
        return () -> tab;
    }
'''
    return text.rstrip()[:-1].rstrip() + "\n" + helpers + "}\n"


def blocks(text):
    text = common(text)
    text = re.sub(r"    public static final DeferredRegister\.Blocks BLOCKS[^;]*;\n", "", text, flags=re.S)
    text = text.replace("DeferredBlock<Block>", "Supplier<Block>")
    text = re.sub(r'BLOCKS\.registerBlock\("(\w+)",\s*(\w+)::new,\s*(.*?)\);',
                  r'block("\1", new \2(\3));', text, flags=re.S)
    text = imports(text,
                   "import com.claudemods.triage.TriageMod;",
                   "import java.util.function.Supplier;",
                   "import net.minecraft.core.Registry;",
                   "import net.minecraft.core.registries.BuiltInRegistries;")
    helpers = '''
    private static Supplier<Block> block(String name, Block block) {
        Registry.register(BuiltInRegistries.BLOCK, TriageMod.id(name), block);
        return () -> block;
    }
'''
    return text.rstrip()[:-1].rstrip() + "\n" + helpers + "}\n"


def sounds(text):
    text = common(text)
    text = re.sub(r"    public static final DeferredRegister<SoundEvent> SOUNDS[^;]*;\n", "", text, flags=re.S)
    text = text.replace("DeferredHolder<SoundEvent, SoundEvent>", "Supplier<SoundEvent>")
    text = re.sub(r'SOUNDS\.register\("(\w+)",\s*\(\) -> ', r'sound("\1", ', text)
    text = imports(text,
                   "import java.util.function.Supplier;",
                   "import net.minecraft.core.Registry;",
                   "import net.minecraft.core.registries.BuiltInRegistries;")
    helpers = '''
    private static Supplier<SoundEvent> sound(String name, SoundEvent event) {
        Registry.register(BuiltInRegistries.SOUND_EVENT, TriageMod.id(name), event);
        return () -> event;
    }
'''
    return text.rstrip()[:-1].rstrip() + "\n" + helpers + "}\n"


def particles(text):
    text = common(text)
    text = re.sub(r"    public static final DeferredRegister<ParticleType<\?>> PARTICLES[^;]*;\n", "", text, flags=re.S)
    text = text.replace("DeferredHolder<ParticleType<?>, SimpleParticleType>", "Supplier<SimpleParticleType>")
    text = re.sub(r'PARTICLES\.register\("(\w+)",\s*\(\) -> ', r'particle("\1", ', text)
    # The constructor is not public in vanilla; NeoForge opens it, Fabric has a factory.
    text = text.replace("new SimpleParticleType(false)", "FabricParticleTypes.simple(false)")
    text = imports(text,
                   "import java.util.function.Supplier;",
                   "import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;",
                   "import net.minecraft.core.Registry;",
                   "import net.minecraft.core.registries.BuiltInRegistries;")
    helpers = '''
    private static Supplier<SimpleParticleType> particle(String name, SimpleParticleType type) {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, TriageMod.id(name), type);
        return () -> type;
    }
'''
    return text.rstrip()[:-1].rstrip() + "\n" + helpers + "}\n"


def components(text):
    text = common(text)
    text = re.sub(r"    public static final DeferredRegister<DataComponentType<\?>> TYPES[^;]*;\n", "", text, flags=re.S)
    text = re.sub(r"DeferredHolder<DataComponentType<\?>, (DataComponentType<\w+>)>", r"Supplier<\1>", text)
    text = re.sub(r'TYPES\.register\("(\w+)",\s*\(\) -> ', r'component("\1", ', text)
    text = imports(text,
                   "import java.util.function.Supplier;",
                   "import net.minecraft.core.Registry;",
                   "import net.minecraft.core.registries.BuiltInRegistries;")
    helpers = '''
    private static <T> Supplier<DataComponentType<T>> component(String name, DataComponentType<T> type) {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, TriageMod.id(name), type);
        return () -> type;
    }
'''
    return text.rstrip()[:-1].rstrip() + "\n" + helpers + "}\n"


JOBS = [
    ("item/ModItems.java", items),
    ("block/ModBlocks.java", blocks),
    ("ModSounds.java", sounds),
    ("ModParticles.java", particles),
    ("item/ModComponents.java", components),
]

for rel, transform in JOBS:
    src = os.path.join(SRC, rel.replace("/", os.sep))
    dst = os.path.join(DST, rel.replace("/", os.sep))
    text = transform(io.open(src, encoding="utf-8").read())
    head, body = text.split("\n", 1)
    text = head + "\n" + NOTE + body
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    io.open(dst, "w", encoding="utf-8", newline="\n").write(text)
    left = re.findall(r"Deferred\w*|IEventBus", text)
    print("%-28s %s" % (rel, "ok" if not left else "STILL NEOFORGE: " + ", ".join(sorted(set(left)))))
