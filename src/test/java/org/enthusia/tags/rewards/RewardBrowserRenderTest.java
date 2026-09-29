package org.enthusia.tags.rewards;
import java.util.*;
import org.bukkit.Material;
import org.bukkit.inventory.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RewardBrowserRenderTest {
    private static void nonItalic(Component c) {
        assertEquals(TextDecoration.State.FALSE,c.decoration(TextDecoration.ITALIC));
        c.children().forEach(RewardBrowserRenderTest::nonItalic);
    }
    @Test void dashboardUsesFixedCategorySlotsAndReadyCountsWithoutClaims() {
        try(var f=new RewardGuiFixture()) {
            for(String category:List.of("playtime","mining","combat","deaths","economy","exploration","misc"))
                f.add(category,category,60,60,RewardCriterionType.CUSTOM_COUNTER,category.equals("playtime")?RewardStatus.UNLOCKED:RewardStatus.CLAIMED);
            var inv=f.menu.create(f.player); assertEquals(45,inv.getSize());
            var holder=(RewardMenuHolder)inv.getHolder();
            for(int slot:RewardMenuModel.DASHBOARD_SLOTS) assertEquals(RewardMenuAction.Type.CATEGORY,holder.action(slot).type());
            assertTrue(RewardGuiFixture.lore(inv.getItem(4)).contains("Claimed: 6 / 7"));
            assertTrue(RewardGuiFixture.lore(inv.getItem(4)).contains("Ready to claim: 1"));
            assertEquals("General",RewardGuiFixture.plain(inv.getItem(24).getItemMeta().displayName()));
            assertEquals(RewardMenuAction.Type.READY,holder.action(40).type());
            verify(f.service,never()).claimAsync(any(),any());
        }
    }
    @Test void paddedBrowserSupportsConditionalPaginationAndStableProgressUpdates() {
        try(var f=new RewardGuiFixture()) {
            for(int i=0;i<43;i++) f.add("reward"+i,"mining",100,i,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.LOCKED);
            var inv=f.menu.createCategory(f.player,"mining"); f.current=inv;
            var h=(RewardMenuHolder)inv.getHolder(); assertEquals(54,inv.getSize());
            assertEquals(21,h.visibleRewards().size()); assertEquals(3,h.pageCount());
            assertNull(h.action(47)); assertEquals(RewardMenuAction.Type.NEXT,h.action(51).type());
            for(int slot:RewardMenuModel.REWARD_SLOTS) assertEquals(RewardMenuAction.Type.CLAIM,h.action(slot).type());
            var before=h.visibleRewards(); f.status("reward0",RewardStatus.CLAIMED); f.menu.refresh(f.player,h,true);
            assertEquals(before,h.visibleRewards()); assertTrue(RewardGuiFixture.lore(inv.getItem(19)).contains("Claimed"));
            f.menu.navigate(f.player,h,h.state().withPage(999));
            assertEquals(2,h.getPage()); assertEquals(1,h.visibleRewards().size()); assertNull(h.action(51));
            assertEquals(RewardMenuAction.Type.PREVIOUS,h.action(47).type());
            verify(f.service,never()).claimAsync(any(),any());
        }
    }
    @Test void tooltipsUseReadableHoursNonItalicTextAndExplicitClaimStates() {
        try(var f=new RewardGuiFixture()) {
            var reward=f.add("deep_dweller","playtime",1800,0,RewardCriterionType.UNDERGROUND_ACTIVE_MINUTES,RewardStatus.LOCKED);
            var inv=f.menu.createCategory(f.player,"playtime");f.current=inv;var h=(RewardMenuHolder)inv.getHolder();
            var item=inv.getItem(19);var lore=RewardGuiFixture.lore(item);
            assertTrue(lore.contains("0h 00m / 30h 00m"));assertTrue(lore.contains("Reward: $500"));assertTrue(lore.contains("Not started"));
            nonItalic(item.getItemMeta().displayName());item.getItemMeta().lore().forEach(RewardBrowserRenderTest::nonItalic);
            f.status(reward.getId(),RewardStatus.UNLOCKED);f.values.put(reward.getCriteria().getFirst(),OptionalLong.of(1800));f.menu.refresh(f.player,h,true);
            assertTrue(RewardGuiFixture.lore(inv.getItem(19)).contains("READY TO CLAIM"));assertTrue(inv.getItem(19).getItemMeta().getEnchantmentGlintOverride());
            f.status(reward.getId(),RewardStatus.CLAIMED);f.menu.refresh(f.player,h,true);
            assertEquals(Material.CLOCK,inv.getItem(19).getType());assertFalse(inv.getItem(19).getItemMeta().getEnchantmentGlintOverride());
        }
    }
    @Test void advancementTagRewardsRenderMiniMessageFormatting() {
        try(var f=new RewardGuiFixture()) {
            var registry=new org.enthusia.tags.TagRegistry();
            registry.register(new org.enthusia.tags.TagDefinition("adv_gladiator","<#ff6c37><bold>Gladiator","<#ff6c37><bold>Gladiator",Material.NAME_TAG,List.of()));
            when(f.tags.getRegistry()).thenReturn(registry);
            String category="advancements";
            f.categories.put(category,new RewardCategory(category,"Advancements",Material.NETHER_STAR));
            var criterion=new RewardCriterion(RewardCriterionType.CUSTOM_COUNTER,1,null,"test",56,"Progress");
            var reward=new RewardDefinition("adv_warzone_gladiator","The Gladiator",List.of("Win 50 Warzone Duels."),Material.NETHERITE_SWORD,List.of(criterion),List.of(new RewardAction(RewardActionType.TAG,"adv_gladiator",0,"")),category);
            f.definitions.put(reward.getId(),reward); f.values.put(criterion,OptionalLong.of(0)); f.status(reward.getId(),RewardStatus.LOCKED);
            var item=f.menu.createCategory(f.player,category).getItem(19);
            String lore=RewardGuiFixture.lore(item);
            assertTrue(lore.contains("Tag: Gladiator")); assertFalse(lore.contains("<#ff6c37>")); assertFalse(lore.contains("<bold>"));
            var line=item.getItemMeta().lore().stream().filter(c -> RewardGuiFixture.plain(c).contains("Tag: Gladiator")).findFirst().orElseThrow();
            String legacy=LegacyComponentSerializer.builder().character('&').hexColors().build().serialize(line);
            assertTrue(legacy.toLowerCase(Locale.ROOT).contains("ff6c37")); assertTrue(legacy.contains("&lGladiator"));
        }
    }
    @Test void categoryBrowserUsesSingleHeaderAndQuietUtilityStrip() {
        try(var f=new RewardGuiFixture()) {
            f.add("one","mining",100,0,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.LOCKED);
            var inv=f.menu.createCategory(f.player,"mining");
            var h=(RewardMenuHolder)inv.getHolder();
            assertEquals(RewardMenuAction.Type.REFRESH,h.action(4).type());
            assertEquals(RewardMenuAction.Type.READY,h.action(10).type());
            assertEquals(RewardMenuAction.Type.FILTER,h.action(15).type());
            assertEquals(RewardMenuAction.Type.SORT,h.action(16).type());
            for(int slot:List.of(1,2,3,5,6,7)) assertNull(h.action(slot));
            assertEquals(Material.BLACK_STAINED_GLASS_PANE,inv.getItem(1).getType());
            assertEquals(Material.ORANGE_STAINED_GLASS_PANE,inv.getItem(3).getType());
            assertEquals(Material.GRAY_STAINED_GLASS_PANE,inv.getItem(11).getType());
            assertEquals("Mining",RewardGuiFixture.plain(inv.getItem(4).getItemMeta().displayName()));
        }
    }
    @Test void unavailableProgressNeverDisplaysAZeroCounter() {
        try(var f=new RewardGuiFixture()) {
            f.add("unavailable","playtime",1800,-1,RewardCriterionType.UNDERGROUND_ACTIVE_MINUTES,RewardStatus.LOCKED);
            var inv=f.menu.createCategory(f.player,"playtime");String lore=RewardGuiFixture.lore(inv.getItem(19));
            assertTrue(lore.contains("Temporarily unavailable"));assertFalse(lore.contains("0h"));assertFalse(lore.contains("0%"));
        }
    }
    @Test void readyViewAggregatesOnlyEligibleUnclaimedRewardsAndRefreshDoesNotShuffle() {
        try(var f=new RewardGuiFixture()) {
            f.add("one","mining",60,60,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.UNLOCKED);
            f.add("two","playtime",60,60,RewardCriterionType.PLAYTIME_ACTIVE_MINUTES,RewardStatus.UNLOCKED);
            f.add("three","combat",60,60,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.CLAIMED);
            var inv=f.menu.create(f.player,RewardMenuState.ready());f.current=inv;var h=(RewardMenuHolder)inv.getHolder();
            assertEquals(Set.of("one","two"),new HashSet<>(h.visibleRewards()));var before=h.visibleRewards();
            f.status("one",RewardStatus.CLAIMED);f.menu.refresh(f.player,h,true);assertEquals(before,h.visibleRewards());
            f.menu.refresh(f.player,h,false);assertEquals(List.of("two"),h.visibleRewards());
            verify(f.service,never()).claimAsync(any(),any());
        }
    }
    @Test void dashboardExtraCategoriesRemainReachable() {
        try(var f=new RewardGuiFixture()) {
            for(int i=0;i<9;i++)f.add("r"+i,"category"+i,100,0,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.LOCKED);
            var inv=f.menu.create(f.player);f.current=inv;var h=(RewardMenuHolder)inv.getHolder();
            assertEquals(2,h.pageCount());assertEquals(RewardMenuAction.Type.NEXT,h.action(42).type());
            f.menu.navigate(f.player,h,h.state().withPage(1));assertNotNull(h.action(38));assertNull(h.action(42));
        }
    }
}
