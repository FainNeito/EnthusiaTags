package org.enthusia.tags.rewards;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RewardGuiRenderTest {
    private static RewardMenuHolder holder(Inventory inventory) { return (RewardMenuHolder) inventory.getHolder(); }
    private static void noItalic(Component component) {
        assertEquals(TextDecoration.State.FALSE, component.decoration(TextDecoration.ITALIC));
        component.children().forEach(RewardGuiRenderTest::noItalic);
    }
    @Test void dashboardUsesFiveRowsWithDistinctClaimedAndReadyCounts() {
        try(var f=new RewardGuiFixture()) {
            for(String c:List.of("playtime","mining","combat","deaths","economy","exploration","misc"))
                f.add(c,c,100,20,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.LOCKED);
            f.status("playtime",RewardStatus.CLAIMED); f.status("mining",RewardStatus.UNLOCKED);
            var inv=f.menu.create(f.player); assertEquals(45,inv.getSize());
            assertTrue(RewardGuiFixture.lore(inv.getItem(4)).contains("Claimed: 1 / 7"));
            assertTrue(RewardGuiFixture.lore(inv.getItem(4)).contains("Ready to claim: 1"));
            assertEquals("General",RewardGuiFixture.plain(inv.getItem(24).getItemMeta().displayName()));
            assertEquals(RewardMenuAction.Type.READY,holder(inv).action(40).type());
            assertEquals(RewardMenuAction.Type.TAGS,holder(inv).action(37).type());
            assertEquals(RewardMenuAction.Type.COSMETICS,holder(inv).action(43).type());
            assertNull(holder(inv).action(38));assertNull(holder(inv).action(42));
            verify(f.service,never()).claimAsync(any(),any());
        }
    }
    @Test void rewardGridHasTwentyOnePaddedSlotsAndConditionalPagination() {
        try(var f=new RewardGuiFixture()) {
            for(int i=0;i<43;i++)f.add("r"+i,"mining",100,i,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.LOCKED);
            var inv=f.menu.createCategory(f.player,"mining");var h=holder(inv);
            assertEquals(54,inv.getSize());assertEquals(21,h.visibleRewards().size());assertEquals(3,h.pageCount());
            assertNull(h.action(47));assertEquals(RewardMenuAction.Type.NEXT,h.action(51).type());
            for(int slot:RewardMenuModel.REWARD_SLOTS)assertEquals(RewardMenuAction.Type.CLAIM,h.action(slot).type());
            f.menu.navigate(f.player,h,h.state().withPage(2));
            assertEquals(List.of("r42"),h.visibleRewards());assertNull(h.action(51));assertNotNull(h.action(47));
            assertEquals(RewardMenuAction.Type.BACK,h.action(45).type());assertEquals(Material.BARRIER,inv.getItem(53).getType());
        }
    }
    @Test void readyViewCollectsAcrossCategoriesWithoutShufflingAfterClaim() {
        try(var f=new RewardGuiFixture()) {
            f.add("readyA","mining",100,100,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.UNLOCKED);
            f.add("readyB","combat",100,100,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.UNLOCKED);
            f.add("locked","mining",100,10,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.LOCKED);
            var inv=f.menu.create(f.player,RewardMenuState.ready());var h=holder(inv);
            assertEquals(List.of("readya","readyb"),h.visibleRewards());
            f.status("readyA",RewardStatus.CLAIMED);f.menu.refresh(f.player,h,true);
            assertEquals(List.of("readya","readyb"),h.visibleRewards());assertTrue(RewardGuiFixture.lore(inv.getItem(19)).contains("Claimed ✓"));
            f.menu.refresh(f.player,h,false);assertEquals(List.of("readyb"),h.visibleRewards());
        }
    }
    @Test void timeUnitsAreReadableAndAllTextExplicitlyDisablesItalics() {
        try(var f=new RewardGuiFixture()) {
            var reward=f.add("deep","playtime",1800,60,RewardCriterionType.UNDERGROUND_ACTIVE_MINUTES,RewardStatus.LOCKED);
            var inv=f.menu.createCategory(f.player,"playtime");var item=inv.getItem(19);
            assertTrue(RewardGuiFixture.lore(item).contains("1h 00m / 30h 00m"));
            assertTrue(RewardGuiFixture.lore(item).contains("Reward: $500"));
            noItalic(item.getItemMeta().displayName());item.getItemMeta().lore().forEach(RewardGuiRenderTest::noItalic);
            noItalic(RewardMenuText.component("&oItalic &c&lred &oagain"));
            assertFalse(item.getItemMeta().getEnchantmentGlintOverride());
        }
    }
    @Test void unknownProgressIsNotPresentedAsZeroAndReadyGlintIsExclusive() {
        try(var f=new RewardGuiFixture()) {
            f.add("unknown","mining",100,-1,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.LOCKED);
            f.add("ready","mining",100,100,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.UNLOCKED);
            f.add("claimed","mining",100,100,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.CLAIMED);
            var inv=f.menu.createCategory(f.player,"mining");
            String unknown=RewardGuiFixture.lore(inv.getItem(19));
            assertTrue(unknown.contains("Temporarily unavailable"));assertFalse(unknown.contains("0 / 100"));assertFalse(unknown.contains("0%"));
            assertFalse(inv.getItem(19).getItemMeta().getEnchantmentGlintOverride());
            assertTrue(inv.getItem(20).getItemMeta().getEnchantmentGlintOverride());
            assertFalse(inv.getItem(21).getItemMeta().getEnchantmentGlintOverride());
            assertEquals(Material.CLOCK,inv.getItem(21).getType());
        }
    }
    @Test void playtimeGroupsAndFiltersPreserveTheRequestedView() {
        try(var f=new RewardGuiFixture()) {
            f.add("long","playtime",600,0,RewardCriterionType.PLAYTIME_ACTIVE_MINUTES,RewardStatus.LOCKED);
            f.add("short","playtime",60,60,RewardCriterionType.PLAYTIME_ACTIVE_MINUTES,RewardStatus.UNLOCKED);
            f.add("total","playtime",60,0,RewardCriterionType.PLAYTIME_TOTAL_MINUTES,RewardStatus.LOCKED);
            var inv=f.menu.createCategory(f.player,"playtime");var h=holder(inv);
            f.menu.navigate(f.player,h,h.state().withGroup(RewardMenuState.Group.ACTIVE).withFilter(RewardMenuState.Filter.UNCLAIMED));
            assertEquals(List.of("short","long"),h.visibleRewards());
            assertEquals(RewardMenuState.Group.ACTIVE,h.state().group());assertNotNull(h.action(12));
            f.menu.refresh(f.player,h,true);assertEquals(RewardMenuState.Group.ACTIVE,h.state().group());
        }
    }
    @Test void emptyCatalogAndMoreThanSevenCategoriesRemainNavigable() {
        try(var f=new RewardGuiFixture()) {
            var empty=f.menu.create(f.player);assertTrue(RewardGuiFixture.plain(empty.getItem(22).getItemMeta().displayName()).contains("No rewards"));
            for(int i=0;i<9;i++)f.add("c"+i,"c"+i,10,0,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.LOCKED);
            var inv=f.menu.create(f.player);var h=holder(inv);assertEquals(2,h.pageCount());assertNotNull(h.action(42));
            f.menu.navigate(f.player,h,h.state().withPage(1));assertNotNull(h.action(38));assertNull(h.action(42));
            assertEquals("c7",h.action(10).value());assertEquals("c8",h.action(12).value());
        }
    }
}
