package com.claudemods.triage.village;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.item.ModItems;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import com.claudemods.triage.village.VillagerTradesEvent;
import net.minecraft.world.entity.npc.VillagerTrades;

/**
 * What the medic sells.
 *
 * <p>Crafting was the only way to get any of this, which meant a player who never went
 * looking for nether wart simply had no antibiotics and no way to know that was the
 * problem. The medic is a second end to that supply line, not a shortcut round it: the
 * cheap things are cheap, and the two that actually decide a fight - a surgical kit and
 * a way to move blood - are the last things they will sell you.
 *
 * <p>They buy the raw materials as well. A village medic wants cloth and leather, and it
 * gives a player with a sheep farm something to do with it.
 */
public final class MedicTrades {
    private MedicTrades() {
    }

    public static void onTrades(VillagerTradesEvent event) {
        if (!event.getType().equals(ModVillagers.MEDIC)) {
            return;
        }

        // Novice: cloth in, dressings out.
        List<VillagerTrades.ItemListing> novice = event.getTrades().get(1);
        novice.add(buy(Items.STRING, 20, 1, 16, 2));
        novice.add(sell(2, ModItems.BANDAGE.get(), 3, 16, 2));
        // Paper and string, sold by the handful. A medic who cannot write on people is
        // not a medic, and a village is exactly where a party restocks before going out.
        novice.add(sell(1, ModItems.TRIAGE_TAG.get(), 6, 16, 2));

        // Apprentice: the things that hold a body together.
        List<VillagerTrades.ItemListing> apprentice = event.getTrades().get(2);
        apprentice.add(buy(Items.LEATHER, 12, 1, 16, 5));
        apprentice.add(sell(3, ModItems.SPLINT.get(), 2, 12, 5));
        apprentice.add(sell(4, ModItems.TOURNIQUET.get(), 1, 12, 5));
        apprentice.add(sell(6, ModItems.SCISSORS.get(), 1, 3, 5));
        // Beside the scissors, and for the same reason: both are the difference between
        // doing a job and doing it with the thing it is done with.
        apprentice.add(sell(8, ModItems.FORCEPS.get(), 1, 3, 5));

        // Journeyman: cleaning and dressing wounds properly.
        List<VillagerTrades.ItemListing> journeyman = event.getTrades().get(3);
        journeyman.add(buy(Items.SUGAR_CANE, 10, 1, 16, 10));
        journeyman.add(sell(5, ModItems.ANTISEPTIC.get(), 1, 12, 10));
        journeyman.add(sell(4, ModItems.PAINKILLERS.get(), 2, 12, 10));

        // Expert: everything that answers an illness rather than a wound.
        List<VillagerTrades.ItemListing> expert = event.getTrades().get(4);
        expert.add(sell(7, ModItems.MEDKIT.get(), 1, 8, 15));
        expert.add(sell(6, ModItems.BURN_SALVE.get(), 1, 8, 15));
        expert.add(sell(9, ModItems.ANTIBIOTICS.get(), 1, 6, 15));
        // A village medic is the one person for miles who would have either of these
        // lying about, and finding one for sale is a great deal kinder than finding
        // out what they are for.
        expert.add(sell(14, ModItems.WOODEN_LEG.get(), 1, 3, 15));
        expert.add(sell(14, ModItems.HOOK.get(), 1, 3, 15));

        // Master: the two that decide whether somebody gets up again.
        List<VillagerTrades.ItemListing> master = event.getTrades().get(5);
        master.add(sell(12, ModItems.SALINE.get(), 1, 4, 30));
        master.add(sell(10, ModItems.BLOOD_BAG.get(), 2, 4, 30));
        master.add(sell(22, ModItems.SURGICAL_KIT.get(), 1, 3, 30));
        master.add(sell(18, ModItems.BONE_SAW.get(), 1, 2, 30));
        master.add(sell(16, ModItems.ANAESTHETIC.get(), 1, 3, 30));
        master.add(sell(28, ModItems.JOINTED_LEG.get(), 1, 2, 30));
        master.add(sell(28, ModItems.MECHANICAL_HAND.get(), 1, 2, 30));
    }

    /** Emeralds for the player's goods. */
    private static VillagerTrades.ItemListing buy(Item wanted, int count, int emeralds, int maxUses, int xp) {
        return (entity, random) -> new MerchantOffer(
                new ItemStack(wanted, count), new ItemStack(Items.EMERALD, emeralds), maxUses, xp, 0.05F);
    }

    /** Goods for the player's emeralds. */
    private static VillagerTrades.ItemListing sell(int emeralds, Item offered, int count, int maxUses, int xp) {
        return (entity, random) -> new MerchantOffer(
                new ItemStack(Items.EMERALD, emeralds), new ItemStack(offered, count), maxUses, xp, 0.05F);
    }
}
