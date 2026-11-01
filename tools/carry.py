# -*- coding: utf-8 -*-
"""Carry the shared half of the mod from the Forge 1.20.1 tree into this one.

Two ports stacked on top of each other, and this takes the second one for free. The Forge
tree next door has already done the hard half - 1.21 down to 1.20.1, which is data
components back to NBT, a different damage pipeline, and a hundred renamed methods - and
every one of those decisions is already in those files. What is left for this tree is the
loader, and the loader is the same difference as in triage-fabric: registries, the event
bus, where a body is kept, how a packet is sent, and the config.

So the source here is the Forge 1.20.1 tree rather than the NeoForge one, and the rules
below are the Fabric half only.

    python tools/carry.py
"""
import io
import os
import re
import shutil

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
FORGE = r"C:\Users\yungspider\IdeaProjects\triage-1201"
SRC = os.path.join(FORGE, "src", "main", "java", "com", "claudemods", "triage")
DST = os.path.join(ROOT, "src", "main", "java", "com", "claudemods", "triage")

FOLDERS = ["body", "med", "data", "event", "net", "client/screen", "client/particle"]

# Carried folders minus the files that are nothing but seam.
SKIP = {
    "data/BodyStore.java",
    "net/ModNetwork.java",
}

FILES = [
    "Reach.java",
    # The client, minus the two files that are only registration.
    "client/ArmourWithGaps.java",
    "client/BloodLayer.java",
    "client/BodyButton.java",
    "client/BodyFigure.java",
    "client/ClientBody.java",
    "client/ClientEvents.java",
    "client/ClientHud.java",
    "client/ClientTags.java",
    "client/Deafness.java",
    "client/DressingLayer.java",
    "client/HandWithGaps.java",
    "client/Limbs.java",
    "client/NamePlate.java",
    "client/OrganFigure.java",
    "client/Phantoms.java",
    "client/ProstheticLayer.java",
    "client/SkinLayer.java",
    "client/SplintLayer.java",
    "client/Symptoms.java",
    "client/Tint.java",
    "client/Whispers.java",
    # Items and blocks - the classes, not the registries.
    "item/BloodBag.java",
    "item/BloodBagItem.java",
    "item/Bracelet.java",
    "item/SeveredLimb.java",
    "item/TriageTagItem.java",
    "item/MedicineItem.java",
    "block/MedicalTable.java",
    "block/XrayMachine.java",
    "command/TriageCommand.java",
    "village/MedicTrades.java",
    "village/VillageHouses.java",
]

REWRITES = [
    # Nothing subscribes to anything: Events.java and TriageClient call the handlers.
    ("import net.minecraftforge.eventbus.api.SubscribeEvent;\n", ""),
    ("import net.minecraftforge.fml.common.Mod;\n", ""),
    ("import net.minecraftforge.api.distmarker.Dist;\n", ""),
    ("@Mod.EventBusSubscriber(modid = TriageMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)\n", ""),
    ("@Mod.EventBusSubscriber(modid = TriageMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)\n", ""),
    ("@Mod.EventBusSubscriber(modid = TriageMod.MODID, value = Dist.CLIENT)\n", ""),
    ("@Mod.EventBusSubscriber(modid = TriageMod.MODID)\n", ""),
    ("    @SubscribeEvent\n", ""),
    # An unused import left over from the other port; sending goes through ModNetwork.
    ("import net.minecraftforge.network.PacketDistributor;\n", ""),
    # The wrappers the handlers read. Same idea as in the 1.21.1 Fabric tree, wearing
    # Forge's names here because these files were written against Forge's. See
    # event/Event.java.
    ("import net.minecraftforge.network.NetworkEvent;",
     "import com.claudemods.triage.net.NetworkEvent;"),
    ("import net.minecraftforge.event.entity.player.PlayerInteractEvent;",
     "import com.claudemods.triage.event.PlayerInteractEvent;"),
    ("import net.minecraftforge.event.entity.player.PlayerEvent;",
     "import com.claudemods.triage.event.PlayerEvent;"),
    ("import net.minecraftforge.event.entity.player.AttackEntityEvent;",
     "import com.claudemods.triage.event.LivingEvents.AttackEntityEvent;"),
    ("import net.minecraftforge.event.entity.living.LivingHurtEvent;",
     "import com.claudemods.triage.event.LivingEvents.LivingHurtEvent;"),
    ("import net.minecraftforge.event.entity.living.LivingHealEvent;",
     "import com.claudemods.triage.event.LivingEvents.LivingHealEvent;"),
    ("import net.minecraftforge.event.entity.living.LivingDeathEvent;",
     "import com.claudemods.triage.event.LivingEvents.LivingDeathEvent;"),
    ("import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;",
     "import com.claudemods.triage.event.LivingEvents.LivingChangeTargetEvent;"),
    ("import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;",
     "import com.claudemods.triage.event.LivingEvents.LivingEntityUseItemEvent;"),
    ("import net.minecraftforge.event.level.ExplosionEvent;",
     "import com.claudemods.triage.event.LivingEvents.ExplosionEvent;"),
    ("import net.minecraftforge.event.entity.EntityLeaveLevelEvent;",
     "import com.claudemods.triage.client.ClientHooks.EntityLeaveLevelEvent;"),
    # Forge has no getter for where a blast was; this tree opens the field instead.
    ("explosion.getPosition()", "com.claudemods.triage.compat.Blasts.centre(explosion)"),
    # Ticking and unloading carry no decision, so they take what Fabric hands over.
    ("import net.minecraftforge.event.TickEvent;\n", ""),
    ("import net.minecraftforge.event.level.LevelEvent;\n", ""),
    # Forge fires each of these twice a tick and hands over a phase to tell the halves
    # apart; Fabric has one callback for the end of a tick and nothing to ask. So the
    # phase guard goes, comment and all - it is answering a question this tree is not
    # asked - and what is left takes the player or the level directly.
    (re.compile(r"    public static void onTick\(TickEvent\.PlayerTickEvent event\) \{\n"
                r"(?:\s*//[^\n]*\n)*"
                r"\s*if \(event\.phase != TickEvent\.Phase\.END\) \{\n\s*return;\n\s*\}\n"
                r"\s*if \(!\(event\.player instanceof ServerPlayer player\) \|\| player\.tickCount % INTERVAL != 0\) \{"),
     "    public static void onTick(ServerPlayer player) {\n"
     "        if (player.tickCount % INTERVAL != 0) {"),
    (re.compile(r"    public static void onLevelTick\(TickEvent\.LevelTickEvent event\) \{\n"
                r"(?:\s*//[^\n]*\n)*"
                r"\s*if \(event\.phase != TickEvent\.Phase\.END\) \{\n\s*return;\n\s*\}\n"
                r"\s*if \(event\.level instanceof ServerLevel level && level\.getGameTime\(\) % INTERVAL == 0\) \{"),
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
    (re.compile(r"    public static void on(Client)?Tick\(TickEvent\.ClientTickEvent event\) \{\n"
                r"(?:\s*//[^\n]*\n)*"
                r"\s*if \(event\.phase != TickEvent\.Phase\.END\) \{\n\s*return;\n\s*\}\n"),
     lambda m: "    public static void on%sTick() {\n" % (m.group(1) or "")),
    # The client's own wrappers.
    ("import net.minecraftforge.client.event.RenderPlayerEvent;",
     "import com.claudemods.triage.client.ClientHooks.RenderPlayerEvent;"),
    ("import net.minecraftforge.client.event.RenderArmEvent;",
     "import com.claudemods.triage.client.ClientHooks.RenderArmEvent;"),
    ("import net.minecraftforge.client.event.RenderHandEvent;",
     "import com.claudemods.triage.client.ClientHooks.RenderHandEvent;"),
    ("import net.minecraftforge.client.event.RenderNameTagEvent;",
     "import com.claudemods.triage.client.ClientHooks.RenderNameTagEvent;"),
    ("import net.minecraftforge.client.event.sound.PlaySoundEvent;",
     "import com.claudemods.triage.client.ClientHooks.PlaySoundEvent;"),
    ("import net.minecraftforge.client.event.ViewportEvent;",
     "import com.claudemods.triage.client.ClientHooks.ViewportEvent;"),
    ("import net.minecraftforge.client.event.MovementInputUpdateEvent;",
     "import com.claudemods.triage.client.ClientHooks.MovementInputUpdateEvent;"),
    ("import net.minecraftforge.client.event.InputEvent;",
     "import com.claudemods.triage.client.ClientHooks.InteractionKeyMappingTriggered;"),
    ("InputEvent.InteractionKeyMappingTriggered", "InteractionKeyMappingTriggered"),
    ("import net.minecraftforge.client.event.ScreenEvent;",
     "import com.claudemods.triage.client.ClientHooks.ScreenInit;"),
    ("ScreenEvent.Init.Post event", "ScreenInit event"),
    ("import net.minecraftforge.client.event.ClientPlayerNetworkEvent;\n", ""),
    ("onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)", "onLoggingOut()"),
    # The HUD is two plain classes here; Fabric's callback calls them in order.
    ("import net.minecraftforge.client.gui.overlay.ForgeGui;\n", ""),
    ("import net.minecraftforge.client.gui.overlay.IGuiOverlay;\n", ""),
    (" implements IGuiOverlay {", " {"),
    ("    @Override\n    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick,",
     "    public void render(GuiGraphics graphics, float partialTick,"),
    # A forced pose is Forge's addition to every entity; a mixin on Player here.
    (re.compile(r"([\w.]+)\.setForcedPose\("), r"com.claudemods.triage.compat.Poses.force(\1, "),
    # And one of Forge's item hooks with no Fabric counterpart.
    ("    @Override\n    public boolean onEntityItemUpdate(", "    public boolean onEntityItemUpdate("),
    # NeoForge and Forge both put getters on the inventory window's corner.
    (".getGuiLeft()", ".leftPos"),
    (".getGuiTop()", ".topPos"),
    # The village.
    ("import net.minecraftforge.event.village.VillagerTradesEvent;",
     "import com.claudemods.triage.village.VillagerTradesEvent;"),
    ("if (!event.getType().equals(ModVillagers.MEDIC.get()))", "if (!event.getType().equals(ModVillagers.MEDIC))"),
    ("import net.minecraftforge.event.server.ServerAboutToStartEvent;",
     "import net.minecraft.server.MinecraftServer;"),
    ("public static void onServerAboutToStart(ServerAboutToStartEvent event) {",
     "public static void onServerAboutToStart(MinecraftServer server) {"),
    ("event.getServer().registryAccess()", "server.registryAccess()"),
    # Commands arrive through a callback rather than an event.
    ("import net.minecraftforge.event.RegisterCommandsEvent;\n", ""),
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
    if "CommandDispatcher<CommandSourceStack> dispatcher" in text \
            and "import com.mojang.brigadier.CommandDispatcher;" not in text:
        text = text.replace("import com.mojang.brigadier.",
                            "import com.mojang.brigadier.CommandDispatcher;\nimport com.mojang.brigadier.", 1)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    io.open(dst, "w", encoding="utf-8", newline="\n").write(text)
    return text != before


def resources():
    """The game's own formats, carried whole. Only Forge's mods.toml is left behind."""
    src = os.path.join(FORGE, "src", "main", "resources")
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

stray = []
for base, _, files in os.walk(DST):
    for name in files:
        path = os.path.join(base, name)
        if name.endswith(".java") and "net.minecraftforge" in io.open(path, encoding="utf-8").read():
            stray.append(os.path.relpath(path, DST))

print("carried %d files, %d of them with the seam rewritten" % (len(names), touched))
print("and %d resources" % resources())
if stray:
    print("still Forge in this tree, port or remove by hand:")
    for name in sorted(stray):
        print("   " + name)
