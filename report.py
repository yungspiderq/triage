# -*- coding: utf-8 -*-
"""Re-apply the 1.20.1 differences after copying files across from the 1.21.1 tree.

Two trees means every fix made on one side has to be carried to the other, and carrying
it means copying the file - which wipes whatever the port had done to that file. Doing
that by hand is how a port rots: the third time somebody forgets one line, the two builds
quietly stop being the same mod.

So the differences live here instead of in anybody's memory. Copy whatever changed, run
this, compile. Everything it does is a difference between the two platforms, and nothing
it does is a decision about the mod.
"""
import io
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "src", "main", "java")
RESOURCES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "src", "main", "resources")

# ---- data pack folders 1.21 renamed to the singular ---------------------------
# Nothing warns about these. A pack with the wrong folder name loads silently and
# ships no recipes at all, which is how the port spent a version with none.
DATA_DIRS = (
    ("advancement", "advancements"),
    ("loot_table", "loot_tables"),
    ("recipe", "recipes"),
    ("structure", "structures"),
)
TAG_DIRS = (
    ("item", "items"),
    ("block", "blocks"),
    ("entity_type", "entity_types"),
    ("fluid", "fluids"),
    ("game_event", "game_events"),
)

# ---- names that changed and nothing else --------------------------------------
SWAPS = [
    ("net.neoforged.neoforge.capabilities.", "net.minecraftforge.common.capabilities."),
    ("net.neoforged.neoforge.common.ModConfigSpec", "net.minecraftforge.common.ForgeConfigSpec"),
    ("net.neoforged.neoforge.client.event.sound.", "net.minecraftforge.client.event.sound."),
    ("net.neoforged.neoforge.client.event.", "net.minecraftforge.client.event."),
    ("net.neoforged.neoforge.client.", "net.minecraftforge.client."),
    ("net.neoforged.neoforge.event.", "net.minecraftforge.event."),
    ("net.neoforged.neoforge.network.", "net.minecraftforge.network."),
    ("net.neoforged.neoforge.registries.", "net.minecraftforge.registries."),
    ("net.neoforged.neoforge.common.", "net.minecraftforge.common."),
    ("net.neoforged.neoforge.attachment.", "net.minecraftforge.common."),
    # Where a body lives. 1.21 hangs it off the player as an attachment; here it is a
    # capability behind BodyStore, and these two lines are the whole of the seam - see
    # PORTING.md. They were hand-kept until a blanket copy from the other tree wiped them,
    # which is exactly the rot this file exists to stop.
    ("import com.claudemods.triage.data.ModAttachments;", "import com.claudemods.triage.data.BodyStore;"),
    ("player.getData(ModAttachments.BODY)", "BodyStore.of(player)"),
    ("net.neoforged.bus.api.", "net.minecraftforge.eventbus.api."),
    ("net.neoforged.api.distmarker.", "net.minecraftforge.api.distmarker."),
    ("net.neoforged.fml.common.EventBusSubscriber", "net.minecraftforge.fml.common.Mod"),
    ("net.neoforged.fml.", "net.minecraftforge.fml."),
    ("@EventBusSubscriber(", "@Mod.EventBusSubscriber("),
    ("ModConfigSpec", "ForgeConfigSpec"),
    ("ResourceLocation.withDefaultNamespace(", "new ResourceLocation("),
    ("ResourceLocation.fromNamespaceAndPath(", "new ResourceLocation("),
    ("DeferredItem<", "RegistryObject<"),
    ("DeferredBlock<", "RegistryObject<"),
    ("PacketDistributor.sendToPlayersTrackingEntityAndSelf(", "ModNetwork.trackingAndSelf("),
    ("PacketDistributor.sendToPlayer(", "ModNetwork.toPlayer("),
    ("PacketDistributor.sendToServer(", "ModNetwork.toServer("),
    ("DeferredRegister.Items ITEMS = DeferredRegister.createItems(TriageMod.MODID)",
     "DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TriageMod.MODID)"),
    ("DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TriageMod.MODID)",
     "DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TriageMod.MODID)"),
    ("ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION", "InteractionResult.PASS"),
    ("ItemInteractionResult.sidedSuccess(", "InteractionResult.sidedSuccess("),
    ("ItemInteractionResult.SUCCESS", "InteractionResult.SUCCESS"),
    ("net.minecraft.world.ItemInteractionResult", "net.minecraft.world.InteractionResult"),
    ("ItemInteractionResult", "InteractionResult"),
    ("ClientTickEvent.Post", "TickEvent.ClientTickEvent"),
    ("PlayerTickEvent.Post", "TickEvent.PlayerTickEvent"),
    ("LevelTickEvent.Post", "TickEvent.LevelTickEvent"),
    ("BlockBehaviour.Properties.ofFullCopy(", "BlockBehaviour.Properties.copy("),
    ("import net.minecraftforge.registries.DeferredBlock;",
     "import net.minecraftforge.registries.RegistryObject;"),
    ("import net.minecraftforge.registries.DeferredItem;",
     "import net.minecraftforge.registries.RegistryObject;"),
    # The armour slots were renamed when 1.21 added a body slot for animals.
    # Same four slots, same meaning, different word.
    ("EquipmentSlot.Type.HUMANOID_ARMOR", "EquipmentSlot.Type.ARMOR"),
]

HOLDER = re.compile(r"DeferredHolder<\s*[^,<>]+(?:<[^<>]*>)?\s*,\s*([^<>]+?)\s*>")
PACKAGE = re.compile(r"^(package [^\n]*\n\n)")


def fix_ticks(s):
    """1.20.1 fires one tick event twice, so every handler needs a phase check."""
    s = re.sub(r"^import net\.minecraftforge\.event\.tick\..*\n", "", s, flags=re.M)
    s = re.sub(r"^import net\.minecraftforge\.client\.event\.ClientTickEvent;\n", "", s, flags=re.M)
    if "TickEvent." in s and "import net.minecraftforge.event.TickEvent;" not in s:
        s = PACKAGE.sub(r"\g<1>import net.minecraftforge.event.TickEvent;\n", s, count=1)

    guard = ("        // 1.20.1 fires this twice a tick, once each side of the game thinking.\n"
             "        if (event.phase != TickEvent.Phase.END) {\n"
             "            return;\n"
             "        }\n")

    def add(match):
        head = match.group(0)
        after = s[match.end():match.end() + 220]
        return head if "event.phase" in after else head + guard

    return re.sub(r"public static void \w+\(TickEvent\.\w+ event\) \{\n", add, s)


def fix_block_use(s):
    """What "somebody right-clicked this block with something" is called here."""
    return s.replace(
        "    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,\n"
        "                                              BlockPos pos, Player player, InteractionHand hand,\n"
        "                                              BlockHitResult hit) {",
        "    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,\n"
        "                                 InteractionHand hand, BlockHitResult hit) {\n"
        "        ItemStack stack = player.getItemInHand(hand);")


def fix_tooltip(s):
    """1.20.1 hands a level to appendHoverText, not a tooltip context."""
    return s.replace("public void appendHoverText(ItemStack stack, TooltipContext context,",
                     "public void appendHoverText(ItemStack stack, @Nullable Level level,")


def fix_config(s):
    """ForgeConfigSpec.defineList takes no supplier for the default entry."""
    return re.sub(r'defineList\("([a-z_]+)", List\.of\(\), \(\) -> "", ',
                  r'defineList("\1", List.of(), ', s)


def fix_block_item(s):
    """1.20.1 has no registerSimpleBlockItem helper; the item is made by hand."""
    return s.replace(
        "    public static final RegistryObject<BlockItem> MEDICAL_TABLE = "
        "ITEMS.registerSimpleBlockItem(ModBlocks.MEDICAL_TABLE);",
        "    public static final RegistryObject<Item> MEDICAL_TABLE = ITEMS.register(\"medical_table\",\n"
        "            () -> new BlockItem(ModBlocks.MEDICAL_TABLE.get(), new Item.Properties()));")


def fix_blocks(s):
    """1.20.1 has no registerBlock helper that takes a constructor."""
    return s.replace(
        '    public static final RegistryObject<Block> MEDICAL_TABLE = BLOCKS.registerBlock("medical_table",\n'
        '            MedicalTable::new, BlockBehaviour.Properties.copy(Blocks.CARTOGRAPHY_TABLE));',
        '    public static final RegistryObject<Block> MEDICAL_TABLE = BLOCKS.register("medical_table",\n'
        '            () -> new MedicalTable(BlockBehaviour.Properties.copy(Blocks.CARTOGRAPHY_TABLE)));')


def fix_bracelet(s):
    """The band, which says two sentences and keeps both of them in NBT here.

    Same treatment as the grime on an instrument: a component on 1.21, a string in the
    stack's tag here. Two tags rather than one because they are two facts and a band with a
    group on it and no allergy is a perfectly ordinary band.
    """
    s = s.replace("        Allergy allergy = stack.get(ModComponents.ALLERGY.get());",
                  "        CompoundTag tag = stack.getTag();\n"
                  "        Allergy allergy = tag == null ? null : Allergy.byId(tag.getString(ALLERGY_TAG));")
    s = s.replace("        return stack.get(ModComponents.BLOOD.get());",
                  "        CompoundTag tag = stack.getTag();\n"
                  "        return tag == null ? null : BloodType.byId(tag.getString(BLOOD_TAG));")
    s = s.replace("        stack.set(ModComponents.ALLERGY.get(), allergy);",
                  "        stack.getOrCreateTag().putString(ALLERGY_TAG, allergy.id());")
    s = s.replace("            stack.set(ModComponents.BLOOD.get(), body.bloodType());",
                  "            stack.getOrCreateTag().putString(BLOOD_TAG, body.bloodType().id());")
    s = s.replace("public class Bracelet extends Item {",
                  "public class Bracelet extends Item {\n"
                  "    /** Where the two sentences live here: tags, because components are not a\n"
                  "     * thing on 1.20.1. */\n"
                  '    private static final String ALLERGY_TAG = "triage_allergy";\n'
                  '    private static final String BLOOD_TAG = "triage_blood_group";\n')
    s = s.replace("public final class Bracelet extends Item {",
                  "public final class Bracelet extends Item {\n"
                  "    /** Where the two sentences live here: tags, because components are not a\n"
                  "     * thing on 1.20.1. */\n"
                  '    private static final String ALLERGY_TAG = "triage_allergy";\n'
                  '    private static final String BLOOD_TAG = "triage_blood_group";\n')
    if "import net.minecraft.nbt.CompoundTag;" not in s:
        s = s.replace("import net.minecraft.world.item.Item;",
                      "import net.minecraft.nbt.CompoundTag;\nimport net.minecraft.world.item.Item;", 1)
    s = re.sub(r"^import com\.claudemods\.triage\.item\.ModComponents;\n", "", s, flags=re.M)
    return s


def fix_components(s, path):
    """Data components do not exist on 1.20.1; the same values live in NBT."""
    if path.endswith("ModComponents.java"):
        return None
    if "ModComponents.ALLERGY" in s or "ModComponents.BLOOD" in s:
        s = fix_bracelet(s)
    if "ModComponents.GRIME" not in s:
        return s
    s = s.replace("Integer used = stack.get(ModComponents.GRIME.get());\n"
                  "        return used == null ? 0 : Math.max(0, used);",
                  "CompoundTag tag = stack.getTag();\n"
                  "        return tag == null ? 0 : Math.max(0, tag.getInt(GRIME_TAG));")
    s = s.replace("stack.set(ModComponents.GRIME.get(), Math.min(FILTHY + 2, grime(stack) + 1));",
                  "stack.getOrCreateTag().putInt(GRIME_TAG, Math.min(FILTHY + 2, grime(stack) + 1));")
    s = s.replace("stack.set(ModComponents.GRIME.get(), grime - 1);",
                  "stack.getOrCreateTag().putInt(GRIME_TAG, grime - 1);")
    s = s.replace("stack.remove(ModComponents.GRIME.get());",
                  "if (stack.getTag() != null) {\n"
                  "            stack.getTag().remove(GRIME_TAG);\n"
                  "        }")
    s = s.replace("    private static final int FILTHY = 6;",
                  "    private static final int FILTHY = 6;\n"
                  "    /** Where it lives here: a tag, because components are not a thing on 1.20.1. */\n"
                  '    private static final String GRIME_TAG = "triage_grime";')
    s = re.sub(r"^import com\.claudemods\.triage\.item\.ModComponents;\n", "", s, flags=re.M)
    return s


def fix_food(s):
    """What is in a food, which moved onto the stack as a component in 1.21."""
    # The mod asks one question of a food - does it carry an effect nobody would choose -
    # and the two lines answer it with different shapes: a component holding records on
    # one, and a list of pairs hanging off the item on the other.
    old = """        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) {
            return false;
        }
        for (FoodProperties.PossibleEffect possible : food.effects()) {
            if (!possible.effect().getEffect().value().isBeneficial()) {
                return true;
            }
        }
        return false;"""
    new = """        FoodProperties food = stack.getItem().getFoodProperties();
        if (food == null) {
            return false;
        }
        for (Pair<MobEffectInstance, Float> possible : food.getEffects()) {
            if (possible.getFirst() != null && !possible.getFirst().getEffect().isBeneficial()) {
                return true;
            }
        }
        return false;"""
    if old not in s:
        return s
    s = s.replace(old, new)
    s = re.sub(r"^import net\.minecraft\.core\.component\.DataComponents;\n", "", s, flags=re.M)
    for imp in ("import com.mojang.datafixers.util.Pair;",
                "import net.minecraft.world.effect.MobEffectInstance;"):
        if imp not in s:
            s = PACKAGE.sub(lambda m, i=imp: m.group(1) + i + chr(10), s, count=1)
    return s


def fix_block_registry(s):
    """Blocks and their items register differently, and a block is used differently.

    1.21 has DeferredRegister.Blocks with registerBlock(name, factory, properties) and
    registerSimpleBlockItem; here there is a plain RegistryObject and the item has to be
    spelled out. And a block that is right-clicked with an empty hand overrides use()
    rather than useWithoutItem(), with the older argument order.
    """
    s = re.sub(
        r"BLOCKS\.registerBlock\(\"([a-z_]+)\",\s*(\w+)::new,\s*([^;]+)\);",
        lambda m: ("BLOCKS.register(\"%s\",\n            () -> new %s(%s));"
                   % (m.group(1), m.group(2), " ".join(m.group(3).split()))),
        s)
    s = re.sub(
        r"ITEMS\.registerSimpleBlockItem\(ModBlocks\.([A-Z_]+)\);",
        lambda m: ("ITEMS.register(\"%s\",\n            () -> new BlockItem(ModBlocks.%s.get(), "
                   "new Item.Properties()));" % (m.group(1).lower(), m.group(1))),
        s)
    s = s.replace("DeferredItem<BlockItem> XRAY_MACHINE", "RegistryObject<Item> XRAY_MACHINE")

    # And the interaction itself.
    s = s.replace("""    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {""",
                  """    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, net.minecraft.world.InteractionHand hand,
                                 BlockHitResult hit) {""")
    return s


def fix_screens(s):
    """Screen methods that gained arguments in 1.21 and have not got them here."""
    s = re.sub(r"renderBackground\((\w+), \w+, \w+, \w+\)",
               lambda m: "renderBackground(" + m.group(1) + ")", s)
    s = s.replace("public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {",
                  "public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {")
    s = s.replace("super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)",
                  "super.mouseScrolled(mouseX, mouseY, scrollY)")
    return s


def fix_tick_accessors(s):
    """The tick events carry their subject in a field here, not behind a getter.

    Only the tick events. Every other event still answers getEntity(), and on some of
    them the field this would reach for is private or is not there at all - so the swap
    has to know which handler it is standing in, not just what the line says.
    """
    # Undo first, so a file that has been through an earlier, blunter version of this
    # comes out the same as one that has never been touched.
    s = s.replace("event.player instanceof ServerPlayer", "event.getEntity() instanceof ServerPlayer")
    s = s.replace("event.level instanceof ServerLevel", "event.getLevel() instanceof ServerLevel")

    out = []
    event_type = ""
    for line in s.split("\n"):
        stripped = line.strip()
        if stripped.startswith("public static void ") and " event" in stripped:
            event_type = stripped.split("(", 1)[1].split(" event", 1)[0].strip()
        if event_type.endswith("PlayerTickEvent"):
            line = line.replace("event.getEntity() instanceof ServerPlayer",
                                "event.player instanceof ServerPlayer")
        elif event_type.endswith("LevelTickEvent"):
            line = line.replace("event.getLevel() instanceof ServerLevel",
                                "event.level instanceof ServerLevel")
        out.append(line)
    return "\n".join(out)


OLD_TOGGLE = """    private static void toggle(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (amount == 0.0) {
            instance.removeModifier(id);
        } else {
            instance.addOrUpdateTransientModifier(
                    new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }"""

NEW_TOGGLE = """    private static void toggle(ServerPlayer player, Attribute attribute, Named named, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        // No addOrUpdate on this version, so the old one comes off by hand first.
        instance.removeModifier(named.id());
        if (amount != 0.0) {
            instance.addTransientModifier(new AttributeModifier(named.id(), named.label(), amount,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    /**
     * What names a modifier here.
     *
     * <p>1.21 names them with a resource location. 1.20.1 wants a UUID and a string, and
     * the UUID has to be the same one every time or a modifier can never be taken off
     * again - so it is derived from the name rather than generated.
     */
    private record Named(UUID id, String label) {
        static Named of(String label) {
            return new Named(UUID.nameUUIDFromBytes(("triage:" + label).getBytes(StandardCharsets.UTF_8)),
                    label);
        }
    }"""


def fix_payloads(s):
    """There is no CustomPacketPayload here; a packet is an object the channel knows."""
    dead = "import net.minecraft.network.protocol.common.custom.CustomPacketPayload;"
    kept = [line for line in s.split("\n") if line.strip() != dead]
    return "\n".join(kept).replace("CustomPacketPayload payload", "Object payload")


def fix_inventory(s):
    """An inventory here cannot be asked a question about its contents."""
    # 1.21 takes a predicate; 1.20.1 only knows how to look for an item or a tag, so
    # the one predicate the mod uses is spelled out against a stack instead. Written as
    # a rule rather than a swap because the call is wrapped across three lines.
    pattern = re.compile(r"getInventory\(\)\.contains\(\s*stack -> stack\.is\(([^)]*)\)\)")
    out = pattern.sub(r"getInventory().contains(new ItemStack(\1))", s)
    if out == s:
        return s
    # And the type the rewritten call needs, which the 1.21 file had no reason to
    # import. Put beside the first minecraft import so the order stays sane.
    want = "import net.minecraft.world.item.ItemStack;"
    if want not in out:
        anchor = "import net.minecraft.client.Minecraft;"
        if anchor in out:
            out = out.replace(anchor, anchor + chr(10) + want, 1)
    return out


def fix_tint(s):
    """Tinting a model part takes four floats here, not a packed int."""
    # The colours upstairs stay written as 0xFFB01008 in both builds, which is the point of
    # the port being invisible from above - so the call goes through Tint, which unpacks it
    # once, in one place, instead of four times at four chances to shift by the wrong number.
    # The receiver is written as any expression rather than as a bare name: the file that
    # broke this was indexing an array of parts, which compiles perfectly on the other tree
    # and reaches here as a call this rule could not see. A difference discovered on the
    # wrong tree is the one thing this script exists to prevent.
    pattern = re.compile(
        r"([\w.]+(?:\[[^\]]*\])?(?:\.\w+)*)\.render\(poseStack,\s*buffer\.getBuffer\(RenderType\."
        r"(entityCutoutNoCull|entitySolid|entityTranslucent)\(([^)]*)\)\),\s*"
        r"light,\s*OverlayTexture\.NO_OVERLAY,\s*([^;]*?)\);",
        re.S)

    def swap(m):
        piece, kind, texture, colour = m.group(1), m.group(2), m.group(3), m.group(4)
        translucent = "true" if kind == "entityTranslucent" else "false"
        colour = " ".join(colour.split())
        return ("Tint.render(%s, poseStack, buffer, %s, %s, light, %s);"
                % (piece, texture.strip(), translucent, colour))

    return pattern.sub(swap, s)


def fix_layer_access(s):
    """The layer list on a renderer is protected here and public on the other line."""
    # Opened by reflection rather than by an access transformer: ForgeGradle does not apply
    # the mod's AT on this toolchain, which is why Reach exists at all.
    dead = ("((LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>)"
            " renderer).layers")
    live = ("com.claudemods.triage.Reach.get(com.claudemods.triage.Reach.open("
            "LivingEntityRenderer.class, " + chr(34) + "layers" + chr(34) + "), renderer)")
    return s.replace(dead, live)


def fix_player_skin(s):
    """A player model variant is two strings here, not an enum."""
    # PORTING.md has had this on its list of small differences since 1.5.0 and this
    # script never knew it, which is why the one file that uses it was kept by hand -
    # and why copying the other line version over it broke everything at once.
    if "PlayerSkin" not in s:
        return s
    s = s.replace("import net.minecraft.client.resources.PlayerSkin;" + chr(10), "")
    s = s.replace("for (PlayerSkin.Model skin : event.getSkins())",
                  "for (String skin : event.getSkins())")
    s = s.replace("PlayerSkin.Model skin, boolean inner)", "String skin, boolean inner)")
    s = s.replace("skin == PlayerSkin.Model.SLIM", "skin.equals(" + chr(34) + "slim" + chr(34) + ")")
    return s


def fix_overlay_layer(s):
    """A thing drawn over the game implements a different interface on each line.

    1.21 hands a LayeredDraw.Layer a DeltaTracker; 1.20.1 hands an IGuiOverlay the gui,
    a partial tick and the screen size. Only the partial tick is wanted, and only for
    how far the frame is between two ticks, so it arrives under a different name and the
    rest are ignored.
    """
    if "LayeredDraw.Layer" not in s:
        return s
    # Whatever the frame time is called here, it is called `delta` by the time the rules
    # below run. They were written against one file that happened to name it that, and a
    # second file naming it `deltaTracker` came through half-ported and did not compile -
    # on the other tree, which is the only place it could have shown up.
    named = re.search(r"public void render\(GuiGraphics \w+, DeltaTracker (\w+)\)", s)
    if named and named.group(1) != "delta":
        s = re.sub(r"\b" + named.group(1) + r"\b", "delta", s)
    s = s.replace("import net.minecraft.client.DeltaTracker;\n", "")
    s = s.replace("import net.minecraft.client.gui.LayeredDraw;\n",
                  "import net.minecraftforge.client.gui.overlay.ForgeGui;\n"
                  "import net.minecraftforge.client.gui.overlay.IGuiOverlay;\n")
    s = s.replace("implements LayeredDraw.Layer", "implements IGuiOverlay")
    s = s.replace("public void render(GuiGraphics graphics, DeltaTracker delta) {",
                  "public void render(ForgeGui gui, GuiGraphics graphics, float partialTick,\n"
                  "                       int screenWidth, int screenHeight) {")
    # What the tracker was asked for is how much of a tick went by since the last frame,
    # and the argument handed to an overlay here is not that: partialTick is where we are
    # inside the current tick, which sawtooths between zero and one and averages a half
    # however fast the machine is. Using it as a delta would run every fade and the pulse
    # itself half again too fast on a quick computer and correctly on a slow one.
    #
    # getDeltaFrameTime is the real one, and it is the same quantity by another name.
    s = s.replace("delta.getGameTimeDeltaTicks()", "Minecraft.getInstance().getDeltaFrameTime()")
    # And nothing further down may take one either. Written as two patterns rather than
    # as a list of method names, because the next thing that wants the frame time will be
    # written on the other line without knowing this rule exists.
    s = re.sub(r"(\w+)\(([^()]*?), delta\);", r"\1(\2);", s)
    s = re.sub(r"(private static [\w<>]+ \w+\([^()]*?), DeltaTracker delta\)", r"\1)", s)
    # And the same two again for a frame time that is not the last thing in the list.
    # Keeping it last was a house style on the other tree that nobody had written down,
    # and a method that broke it compiled perfectly there and not at all here - which is
    # a difference discovered on the wrong tree, which is what this file exists to stop.
    s = re.sub(r"(private static [\w<>]+ \w+\([^()]*?), DeltaTracker delta,", r"\1,", s)
    s = re.sub(r"(\w+\([^()]*?), delta,", r"\1,", s)
    return s


def fix_stack_nbt(s):
    """Saving an item stack, which is the whole of what a lodged weapon needs.

    1.21 writes a stack against the registries - a component can hold a registry entry, so
    they have to be resolvable - and hands back a Tag. 1.20.1 has no components: a stack is
    an id, a count and a tag, and it writes itself into a CompoundTag you give it.

    Two methods, nothing else. Everything above them - the injury, the bleeding that stops
    because something is in the hole, the extraction, the chart, the thing drawn on the
    model - is written once and carried across without a word changed.
    """
    if "class Stuck" not in s:
        return s
    s = s.replace(
        "        Tag saved = stack.save(registries);\n"
        "        return saved instanceof CompoundTag tag ? tag : new CompoundTag();",
        "        return stack.save(new CompoundTag());")
    s = s.replace("        return ItemStack.parse(registries, tag).orElse(ItemStack.EMPTY);",
                  "        return ItemStack.of(tag);")
    # And the import only the other line needs.
    s = s.replace("import net.minecraft.nbt.Tag;\n", "")
    return s


def fix_sound_holders(s):
    """Sounds are holders in 1.21 and plain events here, so nothing is unwrapped."""
    return s.replace("SoundEvents.ARMOR_EQUIP_LEATHER.value()", "SoundEvents.ARMOR_EQUIP_LEATHER")


def fix_hud(s):
    """The HUD is a layer on the other line and an overlay here."""
    # Different event, different interface, different registration call - the one
    # difference on PORTING.md that needed a method rewritten rather than a rename.
    if "RegisterGuiLayersEvent" not in s:
        return s
    s = s.replace("import net.minecraftforge.client.event.RegisterGuiLayersEvent;",
                  "import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;")
    s = s.replace("import net.minecraftforge.client.gui.VanillaGuiLayers;",
                  "import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;")
    s = s.replace("public static void registerLayers(RegisterGuiLayersEvent event) {",
                  "public static void registerLayers(RegisterGuiOverlaysEvent event) {")
    s = s.replace(
        "event.registerAbove(VanillaGuiLayers.HOTBAR, TriageMod.id(" + chr(34) + "body" + chr(34) + "), new ClientHud());",
        "event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), " + chr(34) + "body" + chr(34) + ", new ClientHud());")
    # There is no "above everything" here, so the symptom overlay goes above the last
    # thing vanilla draws that anybody would want it over. The chat panel is that thing:
    # a screen that has gone black should take the chat with it.
    s = s.replace(
        "event.registerAboveAll(TriageMod.id(" + chr(34) + "symptoms" + chr(34) + "), new Symptoms());",
        "event.registerAbove(VanillaGuiOverlay.CHAT_PANEL.id(), " + chr(34) + "symptoms" + chr(34) + ", new Symptoms());")
    return s


def fix_bus_enum(s):
    """The enum inside the annotation, without doubling a prefix that is already there."""
    # The annotation rename leaves the enum pointing at the old outer class, and a blind
    # swap turns the files that already say Mod. in front of it into Mod.Mod. Every rule
    # here has to be safe to run twice, because the whole tree is run through it.
    return re.sub(r"(?<!Mod\.)EventBusSubscriber\.Bus\.", "Mod.EventBusSubscriber.Bus.", s)


def fix_stream_codec(s):
    """1.20.1 has no StreamCodec; the packets here write themselves by hand."""
    s = re.sub(r"^import net\.minecraft\.network\.codec\..*\n", "", s, flags=re.M)
    s = re.sub(r"^import io\.netty\.buffer\.ByteBuf;\n", "", s, flags=re.M)
    return re.sub(r"[ ]*public static final StreamCodec<[^>]+> STREAM_CODEC =[^;]*;\n", "", s)


def fix_attributes(s):
    """An attribute modifier is named by a UUID here, not by a resource location.

    And there is no block-break-speed attribute at all, which is why the port works that
    one out on the BreakSpeed event instead. Neither is reachable by renaming a symbol,
    so both are spelled out.
    """
    if "Holder<Attribute> attribute" not in s:
        return s

    s = re.sub(r'private static final ResourceLocation (\w+) = TriageMod\.id\("([a-z_]+)"\);',
               r'private static final Named \1 = Named.of("\2");', s)
    # The one attribute 1.20.1 does not have. Its name and its line both go.
    s = re.sub(r'[ ]*private static final Named ARM_BREAK_SPEED = Named\.of\("arm_break_speed"\);\n', "", s)
    s = re.sub(r"[ ]*toggle\(player, Attributes\.BLOCK_BREAK_SPEED, ARM_BREAK_SPEED[^;]*;\n", "", s)

    s = s.replace(OLD_TOGGLE, NEW_TOGGLE)

    s = re.sub(r"^import net\.minecraft\.core\.Holder;\n", "", s, flags=re.M)
    for imp in ("import java.nio.charset.StandardCharsets;",
                "import java.util.UUID;",
                "import net.minecraft.world.entity.ai.attributes.Attribute;"):
        if imp not in s:
            s = PACKAGE.sub(lambda m, i=imp: m.group(1) + i + "\n", s, count=1)
    return s


def fix_imports(s):
    """Imports 1.20.1 wants, added only to the files that ended up wanting them."""
    wanted = []
    # The swaps above turn PacketDistributor calls into ModNetwork ones, and a file that
    # never had to name the channel before now does.
    if "new BlockItem(" in s:
        wanted.append("import net.minecraft.world.item.BlockItem;")
    if "ModNetwork." in s and "class ModNetwork" not in s:
        wanted.append("import com.claudemods.triage.net.ModNetwork;")
    if "ForgeRegistries." in s:
        wanted.append("import net.minecraftforge.registries.ForgeRegistries;")
    if "RegistryObject<" in s:
        wanted.append("import net.minecraftforge.registries.RegistryObject;")
    if "@Nullable Level level" in s:
        wanted.append("import org.jetbrains.annotations.Nullable;")
        wanted.append("import net.minecraft.world.level.Level;")
    if "CompoundTag tag = stack.getTag()" in s:
        wanted.append("import net.minecraft.nbt.CompoundTag;")
    for imp in wanted:
        if imp not in s:
            s = PACKAGE.sub(lambda m, i=imp: m.group(1) + i + "\n", s, count=1)
    return s


def port(path):
    raw = io.open(path, "rb").read().decode("utf-8")
    crlf = "\r\n" in raw
    s = raw.replace("\r\n", "\n")
    before = s

    for old, new in SWAPS:
        s = s.replace(old, new)
    s = HOLDER.sub(r"RegistryObject<\1>", s)
    s = s.replace("import net.minecraftforge.registries.DeferredHolder;",
                  "import net.minecraftforge.registries.RegistryObject;")
    s = fix_ticks(s)
    s = fix_block_use(s)
    s = fix_tooltip(s)
    s = fix_config(s)
    s = fix_blocks(s)
    s = fix_block_item(s)
    s = fix_screens(s)
    s = fix_tick_accessors(s)
    s = fix_block_registry(s)
    s = fix_overlay_layer(s)
    s = fix_sound_holders(s)
    s = fix_stack_nbt(s)
    s = fix_food(s)
    s = fix_payloads(s)
    s = fix_inventory(s)
    s = fix_tint(s)
    s = fix_layer_access(s)
    s = fix_player_skin(s)
    s = fix_hud(s)
    s = fix_bus_enum(s)
    s = fix_stream_codec(s)
    s = fix_attributes(s)
    s = fix_components(s, path)
    if s is None:
        os.remove(path)
        return "deleted"
    s = fix_imports(s)

    lines, seen, out = s.split("\n"), set(), []
    for line in lines:
        if line.startswith("import "):
            if line in seen:
                continue
            seen.add(line)
        out.append(line)
    s = "\n".join(out)

    if s != before:
        io.open(path, "wb").write((s.replace("\n", "\r\n") if crlf else s).encode("utf-8"))
        return "ported"
    return None


def merge_into(src, dst):
    """Move everything under src into dst, then take src away."""
    for base, _dirs, files in os.walk(src):
        rel = os.path.relpath(base, src)
        target = dst if rel == "." else os.path.join(dst, rel)
        if not os.path.isdir(target):
            os.makedirs(target)
        for name in files:
            os.replace(os.path.join(base, name), os.path.join(target, name))
    for base, dirs, _files in os.walk(src, topdown=False):
        for name in dirs:
            os.rmdir(os.path.join(base, name))
    os.rmdir(src)


def port_data_dirs():
    """Put the data pack back under the names 1.20.1 reads."""
    moved = []
    data = os.path.join(RESOURCES, "data")
    if not os.path.isdir(data):
        return moved
    for ns in sorted(os.listdir(data)):
        nsdir = os.path.join(data, ns)
        if not os.path.isdir(nsdir):
            continue
        for old, new in DATA_DIRS:
            here = os.path.join(nsdir, old)
            if os.path.isdir(here):
                merge_into(here, os.path.join(nsdir, new))
                moved.append("%s/%s -> %s" % (ns, old, new))
        tags = os.path.join(nsdir, "tags")
        if not os.path.isdir(tags):
            continue
        for old, new in TAG_DIRS:
            here = os.path.join(tags, old)
            if os.path.isdir(here):
                merge_into(here, os.path.join(tags, new))
                moved.append("%s/tags/%s -> %s" % (ns, old, new))
    return moved


def port_json(path):
    """What names an item here is "item". 1.21 calls it "id" in both places we use."""
    raw = io.open(path, "rb").read().decode("utf-8")
    data = json.loads(raw)
    before = json.dumps(data, sort_keys=True)

    result = data.get("result")
    if isinstance(result, dict) and "id" in result and "item" not in result:
        result["item"] = result.pop("id")
    icon = data.get("display", {}).get("icon") if isinstance(data.get("display"), dict) else None
    if isinstance(icon, dict) and "id" in icon and "item" not in icon:
        icon["item"] = icon.pop("id")

    if json.dumps(data, sort_keys=True) == before:
        return False
    io.open(path, "w", encoding="utf-8", newline=chr(10)).write(
        json.dumps(data, ensure_ascii=False, indent=2) + chr(10))
    return True


changed = 0
removed = 0
for base, _dirs, files in os.walk(ROOT):
    for name in files:
        if not name.endswith(".java"):
            continue
        result = port(os.path.join(base, name))
        if result == "ported":
            changed += 1
        elif result == "deleted":
            removed += 1

left = set()
for base, _dirs, files in os.walk(ROOT):
    for name in files:
        if name.endswith(".java"):
            body = io.open(os.path.join(base, name), "rb").read().decode("utf-8")
            if "neoforged" in body or "DataComponent" in body:
                left.add(name)
moved = port_data_dirs()
rewritten = 0
for base, _dirs, files in os.walk(os.path.join(RESOURCES, "data")):
    for name in files:
        if name.endswith(".json") and port_json(os.path.join(base, name)):
            rewritten += 1
print("ported %d files, deleted %d" % (changed, removed))
print("data folders moved: %s" % (", ".join(moved) if moved else "none"))
print("json files rewritten: %d" % rewritten)
if left:
    print("still platform-specific and not handled here:", ", ".join(sorted(left)))
    sys.exit(1)
