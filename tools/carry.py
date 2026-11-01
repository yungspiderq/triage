# -*- coding: utf-8 -*-
"""Carry the shared half of the mod from the NeoForge tree into this one.

Most of Triage is not about a mod loader. Injuries, illnesses, organs, blood, the
readings, the minigames and every screen are written against Minecraft itself, and this
tree is on Mojang mappings so that those files read here exactly as they read there. They
are copied rather than rewritten, and copied again whenever the other tree moves.

What cannot be copied is the seam: registries, the event bus, where a body is kept, how a
packet is sent, and the config. Those are written by hand in this tree, and the files that
are nothing but seam are not on the list below at all.

Between the two sits a handful of files that are nine tenths shared and one tenth seam.
Rather than fork them - which would mean every future fix landing twice - they are copied
and then have that tenth rewritten here, mechanically, by the rules in REWRITES. The two
shims in com.claudemods.triage.net exist for the same reason: they wear the NeoForge names
so that a file which sends a packet needs one import line changed and not a rewrite.

    python tools/carry.py
"""
import io
import os
import re
import shutil

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = r"C:\Users\yungspider\IdeaProjects\triage\src\main\java\com\claudemods\triage"
DST = os.path.join(ROOT, "src", "main", "java", "com", "claudemods", "triage")

# Whole folders. Nothing in these knows which loader it is running on - except the
# event folder, which knows nothing else and is carried for a different reason: see
# com.claudemods.triage.event.Event.
FOLDERS = ["body", "med", "data", "event", "client/screen", "client/particle"]

# Folders carried whole except for the files named here, which are seam.
SKIP = {
    "data/ModAttachments.java",
}

FILES = [
    # The client, minus the parts that hang off the event bus.
    "client/ClientBody.java",
    "client/ClientTags.java",
    "client/BodyFigure.java",
    "client/ClientHud.java",
    "client/Symptoms.java",
    "client/Whispers.java",
    "client/Phantoms.java",
    "client/BodyButton.java",
    "client/OrganFigure.java",
    "client/BloodLayer.java",
    "client/DressingLayer.java",
    "client/SkinLayer.java",
    "client/SplintLayer.java",
    "client/ProstheticLayer.java",
    "client/ArmourWithGaps.java",
    "client/HandWithGaps.java",
    "client/ClientEvents.java",
    "client/Limbs.java",
    "client/Deafness.java",
    "client/NamePlate.java",
    # Items and blocks - the classes, not the registries.
    "item/BloodBag.java",
    "item/BloodBagItem.java",
    "item/Bracelet.java",
    "item/SeveredLimb.java",
    "item/TriageTagItem.java",
    "item/MedicineItem.java",
    "block/MedicalTable.java",
    "block/XrayMachine.java",
    # The command: eight hundred lines of Brigadier and one line of registration.
    "command/TriageCommand.java",
    # The village, minus the registration of the medic themselves.
    "village/MedicTrades.java",
    "village/VillageHouses.java",
]

# Every payload record but the registration next to them: CustomPacketPayload is
# Minecraft's own class, so the wire format is identical on both loaders.
PAYLOADS = [name for name in os.listdir(os.path.join(SRC, "net")) if name != "ModNetwork.java"]
FILES += ["net/" + name for name in PAYLOADS]

REWRITES = [
    # The two shims. Same names, same call shapes, this tree's implementations.
    ("import net.neoforged.neoforge.network.PacketDistributor;",
     "import com.claudemods.triage.net.PacketDistributor;"),
    ("import net.neoforged.neoforge.network.handling.IPayloadContext;",
     "import com.claudemods.triage.net.IPayloadContext;"),
    # Attachments are Fabric's too, under another name.
    (".getData(ModAttachments.BODY)", ".getAttachedOrCreate(ModAttachments.BODY)"),
    # A forced pose is a NeoForge addition to every entity; here it is a mixin on Player,
    # reached through a helper. See compat/Poses.java.
    (re.compile(r"([\w.]+)\.setForcedPose\("), r"com.claudemods.triage.compat.Poses.force(\1, "),
    # One of NeoForge's item hooks with no Fabric counterpart: the mixin on ItemEntity
    # calls this by name instead, so it overrides nothing.
    ("    @Override\n    public boolean onEntityItemUpdate(", "    public boolean onEntityItemUpdate("),
    # Nothing subscribes to anything here: Events.java calls the handlers by name.
    ("import net.neoforged.bus.api.SubscribeEvent;\n", ""),
    ("import net.neoforged.fml.common.EventBusSubscriber;\n", ""),
    ("@EventBusSubscriber(modid = TriageMod.MODID, bus = EventBusSubscriber.Bus.MOD)\n", ""),
    ("@EventBusSubscriber(modid = TriageMod.MODID)\n", ""),
    ("    @SubscribeEvent\n", ""),
    # The event objects the handlers read are this tree's own; see event/Event.java.
    ("import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;",
     "import com.claudemods.triage.event.PlayerInteractEvent;"),
    ("import net.neoforged.neoforge.event.entity.player.PlayerEvent;",
     "import com.claudemods.triage.event.PlayerEvent;"),
    ("import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;",
     "import com.claudemods.triage.event.LivingEvents.AttackEntityEvent;"),
    ("import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;",
     "import com.claudemods.triage.event.LivingDamageEvent;"),
    ("import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;",
     "import com.claudemods.triage.event.LivingEvents.LivingDeathEvent;"),
    ("import net.neoforged.neoforge.event.entity.living.LivingHealEvent;",
     "import com.claudemods.triage.event.LivingEvents.LivingHealEvent;"),
    ("import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;",
     "import com.claudemods.triage.event.LivingEvents.LivingChangeTargetEvent;"),
    ("import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;",
     "import com.claudemods.triage.event.LivingEvents.LivingEntityUseItemEvent;"),
    ("import net.neoforged.neoforge.event.level.ExplosionEvent;",
     "import com.claudemods.triage.event.LivingEvents.ExplosionEvent;"),
    # The three ticking ones are not wrapped at all - they carry no decision, so they
    # take what Fabric hands over directly.
    ("import net.neoforged.neoforge.event.tick.PlayerTickEvent;\n", ""),
    ("import net.neoforged.neoforge.event.tick.LevelTickEvent;\n", ""),
    ("import net.neoforged.neoforge.event.level.LevelEvent;\n", ""),
    ("    public static void onTick(PlayerTickEvent.Post event) {\n"
     "        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % INTERVAL != 0) {",
     "    public static void onTick(ServerPlayer player) {\n"
     "        if (player.tickCount % INTERVAL != 0) {"),
    ("    public static void onLevelTick(LevelTickEvent.Post event) {\n"
     "        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % INTERVAL == 0) {",
     "    public static void onLevelTick(ServerLevel level) {\n"
     "        if (level.getGameTime() % INTERVAL == 0) {"),
    ("    public static void onLevelUnload(LevelEvent.Unload event) {\n"
     "        if (event.getLevel() instanceof ServerLevel level) {\n"
     "            BloodEffects.forget(level);\n"
     "        }\n"
     "    }",
     "    public static void onLevelUnload(ServerLevel level) {\n"
     "        BloodEffects.forget(level);\n"
     "    }"),
    # The client's own wrappers, in ClientHooks. Same idea as the server's, and needed
    # more: this is where Fabric has the fewest events of its own.
    ("import net.neoforged.api.distmarker.Dist;\n", ""),
    ("@EventBusSubscriber(modid = TriageMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)\n", ""),
    ("@EventBusSubscriber(modid = TriageMod.MODID, value = Dist.CLIENT)\n", ""),
    ("import net.neoforged.neoforge.client.event.RenderPlayerEvent;",
     "import com.claudemods.triage.client.ClientHooks.RenderPlayerEvent;"),
    ("import net.neoforged.neoforge.client.event.RenderArmEvent;",
     "import com.claudemods.triage.client.ClientHooks.RenderArmEvent;"),
    ("import net.neoforged.neoforge.client.event.RenderHandEvent;",
     "import com.claudemods.triage.client.ClientHooks.RenderHandEvent;"),
    ("import net.neoforged.neoforge.client.event.RenderNameTagEvent;",
     "import com.claudemods.triage.client.ClientHooks.RenderNameTagEvent;"),
    ("import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;",
     "import com.claudemods.triage.client.ClientHooks.PlaySoundEvent;"),
    ("import net.neoforged.neoforge.client.event.ViewportEvent;",
     "import com.claudemods.triage.client.ClientHooks.ViewportEvent;"),
    ("import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;",
     "import com.claudemods.triage.client.ClientHooks.MovementInputUpdateEvent;"),
    ("import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;",
     "import com.claudemods.triage.client.ClientHooks.EntityLeaveLevelEvent;"),
    ("import net.neoforged.neoforge.client.event.InputEvent;",
     "import com.claudemods.triage.client.ClientHooks.InteractionKeyMappingTriggered;"),
    ("InputEvent.InteractionKeyMappingTriggered", "InteractionKeyMappingTriggered"),
    ("import net.neoforged.neoforge.client.event.ScreenEvent;",
     "import com.claudemods.triage.client.ClientHooks.ScreenInit;"),
    ("ScreenEvent.Init.Post event", "ScreenInit event"),
    # NeoForge puts getters on these two; vanilla has the fields, opened by the widener.
    (".getGuiLeft()", ".leftPos"),
    (".getGuiTop()", ".topPos"),
    # Ticking and logging out carry nothing, so they are not wrapped at all.
    ("import net.neoforged.neoforge.client.event.ClientTickEvent;\n", ""),
    ("import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;\n", ""),
    ("onClientTick(ClientTickEvent.Post event)", "onClientTick()"),
    ("onTick(ClientTickEvent.Post event)", "onTick()"),
    ("onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)", "onLoggingOut()"),
    # The village. What the medic sells is the same list; the trade event it is written
    # against is village/VillagerTradesEvent here, in the same package.
    ("import net.neoforged.neoforge.event.village.VillagerTradesEvent;\n", ""),
    ("if (!event.getType().equals(ModVillagers.MEDIC.get()))", "if (!event.getType().equals(ModVillagers.MEDIC))"),
    # And the clinic goes into village generation at startup either way.
    ("import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;",
     "import net.minecraft.server.MinecraftServer;"),
    ("public static void onServerAboutToStart(ServerAboutToStartEvent event) {",
     "public static void onServerAboutToStart(MinecraftServer server) {"),
    ("event.getServer().registryAccess()", "server.registryAccess()"),
    # Commands arrive through a callback here rather than an event.
    ("import net.neoforged.neoforge.event.RegisterCommandsEvent;\n", ""),
    ("    public static void register(RegisterCommandsEvent event) {",
     "    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {"),
    ("event.getDispatcher()", "dispatcher"),
]


def carry(rel):
    src = os.path.join(SRC, rel.replace("/", os.sep))
    dst = os.path.join(DST, rel.replace("/", os.sep))
    text = io.open(src, encoding="utf-8").read()
    before = text
    for old, new in REWRITES:
        if hasattr(old, "sub"):
            text = old.sub(new, text)
        else:
            text = text.replace(old, new)
    if text != before and "CommandDispatcher<CommandSourceStack> dispatcher" in text \
            and "import com.mojang.brigadier.CommandDispatcher;" not in text:
        text = text.replace("import com.mojang.brigadier.", "import com.mojang.brigadier.CommandDispatcher;\nimport com.mojang.brigadier.", 1)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    io.open(dst, "w", encoding="utf-8", newline="\n").write(text)
    return text != before


def resources():
    """Textures, models, lang, recipes, advancements, structures.

    Not one of these knows what a mod loader is - they are the game's own formats, and
    the only thing in the folder that is not carried is the file that tells NeoForge the
    mod exists, which is fabric.mod.json here.
    """
    src = os.path.join(os.path.dirname(SRC.split("\\src\\main\\java")[0]), "triage", "src", "main", "resources")
    dst = os.path.join(ROOT, "src", "main", "resources")
    count = 0
    for base, dirs, files in os.walk(src):
        dirs[:] = [d for d in dirs if d != "META-INF"]
        for name in files:
            source = os.path.join(base, name)
            target = os.path.join(dst, os.path.relpath(source, src))
            os.makedirs(os.path.dirname(target), exist_ok=True)
            shutil.copyfile(source, target)
            count += 1
    return count


names = []
for folder in FOLDERS:
    base = os.path.join(SRC, folder.replace("/", os.sep))
    for name in sorted(os.listdir(base)):
        if name.endswith(".java") and folder + "/" + name not in SKIP:
            names.append(folder + "/" + name)
names += FILES

touched = 0
for rel in names:
    if carry(rel):
        touched += 1

# Anything left behind by an older list would quietly go on compiling against a NeoForge
# that is not here, so say so rather than leave it to the build.
stray = []
for base, _, files in os.walk(DST):
    for name in files:
        path = os.path.join(base, name)
        if name.endswith(".java") and "net.neoforged" in io.open(path, encoding="utf-8").read():
            stray.append(os.path.relpath(path, DST))

print("carried %d files, %d of them with the seam rewritten" % (len(names), touched))
print("and %d resources, which are the game's own formats and need nothing done to them"
      % resources())
if stray:
    print("still NeoForge in this tree, port or remove by hand:")
    for name in sorted(stray):
        print("   " + name)
