package org.enthusia.tags.rewards;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.enthusia.tags.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HolidayMenuTest {
    @Test void rootsChildrenOwnershipAndParentNavigationDoNotClaimAwards() {
        var categories = new LinkedHashMap<String, RewardCategory>();
        for (int i = 0; i < 7; i++) categories.put("old" + i, new RewardCategory("old" + i, "Old", Material.PAPER));
        categories.put("holidays", new RewardCategory("holidays", "Holidays", Material.FIREWORK_ROCKET));
        categories.put("halloween", new RewardCategory("halloween", "Halloween", Material.PUMPKIN, "holidays", List.of("pumpkin_hunter")));
        categories.put("christmas", new RewardCategory("christmas", "Christmas", Material.CHEST, "holidays", List.of()));
        var fixture = new Fixture(categories);
        try (var bukkit = fixture.bukkit(); var items = fixture.items()) {
            fixture.menu.create(fixture.player);
            assertEquals(8, fixture.slots.size(), "Holidays must remain visible after seven existing roots");
            fixture.menu.createCategory(fixture.player, "holidays");
            assertEquals(Set.of(0, 1, 49), fixture.slots.keySet());
            fixture.menu.createCategory(fixture.player, "halloween");
            var lockedMeta = fixture.slots.get(0).getItemMeta();
            verify(lockedMeta.getPersistentDataContainer(), never()).set(eq(fixture.menu.getRewardKey()), any(), any());
            verify(lockedMeta).lore(argThat(lines -> lines.stream().anyMatch(line ->
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line).contains("Not earned"))));
            fixture.data.getOwnedTags().add("pumpkin_hunter");
            fixture.menu.createCategory(fixture.player, "halloween");
            verify(fixture.slots.get(0).getItemMeta()).lore(argThat(lines -> lines.stream().anyMatch(line ->
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line).contains("Earned"))));
            fixture.menu.createParent(fixture.player, "halloween");
            assertEquals("holidays", fixture.holder.getCategory());
            fixture.menu.createParent(fixture.player, "holidays");
            assertNull(fixture.holder.getCategory());
            verify(fixture.service, never()).claimAsync(any(), any());
        }
    }

    @Test void categoryPaginationClampsAndShowsBothNavigationDirections() {
        var categories = new LinkedHashMap<String, RewardCategory>();
        for (int i = 0; i < 50; i++) categories.put("c" + i, new RewardCategory("c" + i, "Category", Material.PAPER));
        var fixture = new Fixture(categories);
        try (var bukkit = fixture.bukkit(); var items = fixture.items()) {
            fixture.menu.createCategory(fixture.player, null, -2);
            assertEquals(0, fixture.holder.getPage());
            assertTrue(fixture.slots.containsKey(53));
            fixture.menu.createCategory(fixture.player, null, Integer.MAX_VALUE);
            assertEquals(1, fixture.holder.getPage());
            assertEquals(Set.of(0, 1, 2, 3, 4, 45), fixture.slots.keySet());
        }
    }

    private static final class Fixture {
        final RewardService service = mock(RewardService.class);
        final Player player = mock(Player.class);
        final PlayerTagData data = new PlayerTagData();
        final Map<Integer, ItemStack> slots = new HashMap<>();
        final RewardMenu menu;
        RewardMenuHolder holder;
        Fixture(Map<String, RewardCategory> categories) {
            var plugin = mock(EnthusiaTagsPlugin.class);
            when(plugin.getName()).thenReturn("EnthusiaTags");
            when(plugin.namespace()).thenReturn("enthusiatags");
            when(plugin.getPerformanceMonitor()).thenReturn(new PerformanceMonitor(null));
            var tags = mock(TagService.class);
            when(tags.getPlugin()).thenReturn(plugin);
            var registry = new TagRegistry();
            registry.register(new TagDefinition("pumpkin_hunter", "<bold>Pumpkin Hunter", "<bold>Pumpkin Hunter", Material.PUMPKIN, List.of("Find 15 pumpkins")));
            when(tags.getRegistry()).thenReturn(registry);
            when(tags.getPlayerData(any())).thenReturn(data);
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            var config = mock(RewardsConfig.class);
            when(config.categories()).thenReturn(categories);
            when(service.getConfig()).thenReturn(config);
            when(service.getRewards()).thenReturn(Map.of());
            when(service.getMessage(anyString())).thenAnswer(call -> switch ((String) call.getArgument(0)) {
                case "rewards-category-title" -> "{category} Rewards";
                case "rewards-holiday-locked" -> "Not earned yet";
                case "rewards-holiday-owned" -> "Earned — Equip with /tags";
                default -> "Rewards";
            });
            menu = new RewardMenu(service, tags);
        }
        org.mockito.MockedStatic<Bukkit> bukkit() {
            var bukkit = mockStatic(Bukkit.class);
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(54), any(net.kyori.adventure.text.Component.class)))
                .thenAnswer(call -> {
                    slots.clear(); holder = call.getArgument(0);
                    var inventory = mock(Inventory.class);
                    doAnswer(set -> { slots.put(set.getArgument(0), set.getArgument(1)); return null; }).when(inventory).setItem(anyInt(), any());
                    return inventory;
                });
            return bukkit;
        }
        org.mockito.MockedConstruction<ItemStack> items() {
            return mockConstruction(ItemStack.class, (item, context) -> {
                var meta = mock(ItemMeta.class);
                when(meta.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
                when(item.getItemMeta()).thenReturn(meta);
            });
        }
    }
}
