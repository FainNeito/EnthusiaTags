package org.enthusia.tags;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.enthusia.tags.cosmetics.*;
import org.enthusia.tags.entitlements.EntitlementDefinition;
import org.enthusia.tags.entitlements.EntitlementService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class CustomizationGuiRenderTest {
    @Test
    void tagsUseStructuredContentAreaAndAdminPreviewShowsCatalogWithoutChangingOwnership() {
        try (Fixture f = new Fixture()) {
            TagRegistry registry = new TagRegistry();
            registry.register(new TagDefinition("owned", "&aOwned", "&aOwned", Material.DIAMOND, List.of("&7Owned tag")));
            registry.register(new TagDefinition("legacy", "&6Legacy", "&6Legacy", Material.ECHO_SHARD, List.of("&7Legacy tag")));
            PlayerTagData data = new PlayerTagData();
            data.getOwnedTags().add("owned");
            data.setSelectedTag("owned");
            when(f.tags.getRegistry()).thenReturn(registry);
            when(f.tags.getPlayerData(f.id)).thenReturn(data);
            when(f.entitlements.definitionsForTag("owned")).thenReturn(List.of());
            when(f.entitlements.definitionsForTag("legacy")).thenReturn(List.of(
                new EntitlementDefinition("legacy_test", "Legacy: Test", true, Set.of("legacy"), Set.of(), Set.of(), "", Set.of())));

            TagMenu menu = new TagMenu(f.tags);
            Inventory normal = menu.create(f.player);
            assertEquals(54, normal.getSize());
            assertEquals(Material.BLACK_STAINED_GLASS_PANE, normal.getItem(1).getType());
            assertEquals(Material.ORANGE_STAINED_GLASS_PANE, normal.getItem(3).getType());
            assertEquals(Material.GRAY_STAINED_GLASS_PANE, normal.getItem(11).getType());
            assertEquals(Material.DIAMOND, normal.getItem(19).getType());
            assertNull(normal.getItem(20));
            assertEquals(Material.CHEST, normal.getItem(45).getType());
            assertEquals(Material.FEATHER, normal.getItem(47).getType());
            assertEquals(Material.BARRIER, normal.getItem(51).getType());

            Inventory preview = menu.create(f.player, true);
            TagMenuHolder holder = (TagMenuHolder) preview.getHolder();
            assertTrue(holder.isPreview());
            assertEquals(2, TagMenu.CONTENT_SLOTS.stream().map(preview::getItem).filter(Objects::nonNull).count());
            assertEquals(Material.SPYGLASS, preview.getItem(17).getType());
            assertEquals("all", holder.getFilter());
            assertEquals(0, holder.getPage());
            assertEquals(Set.of("owned"), data.getOwnedTags(), "preview rendering must not alter ownership");
        }
    }

    @Test
    void cosmeticsUseDashboardContentRowsAndReadOnlyPreviewState() {
        try (Fixture f = new Fixture()) {
            CosmeticsCategory trail = new CosmeticsCategory("trail", "&bTrails", Material.FEATHER);
            when(f.cosmetics.getCategories()).thenReturn(new LinkedHashMap<>(Map.of("trail", trail)));
            CosmeticDefinition owned = cosmetic("owned_trail", "trail", "&aOwned Trail");
            CosmeticDefinition locked = cosmetic("legacy_trail", "trail", "&6Legacy Trail");
            Map<String, CosmeticDefinition> cosmetics = new LinkedHashMap<>();
            cosmetics.put(owned.getId(), owned);
            cosmetics.put(locked.getId(), locked);
            when(f.cosmetics.getCosmetics()).thenReturn(cosmetics);
            when(f.cosmetics.canUseCosmetic(f.player, owned)).thenReturn(true);
            when(f.cosmetics.canUseCosmetic(f.player, locked)).thenReturn(false);
            when(f.cosmetics.getSelection(f.id, "trail")).thenReturn("owned_trail");
            when(f.entitlements.definitionsForCosmetic("owned_trail")).thenReturn(List.of());
            when(f.entitlements.definitionsForCosmetic("legacy_trail")).thenReturn(List.of(
                new EntitlementDefinition("legacy", "Legacy: Test", true, Set.of(), Set.of("legacy_trail"), Set.of(), "", Set.of())));
            when(f.entitlements.isActiveOnlyCosmetic(anyString())).thenReturn(false);

            CosmeticsMenu menu = new CosmeticsMenu(f.cosmetics, f.tags, f.messages);
            Inventory dashboard = menu.createMain(f.player, true);
            CosmeticsMenuHolder dashboardHolder = (CosmeticsMenuHolder) dashboard.getHolder();
            assertTrue(dashboardHolder.isPreview());
            assertEquals(Material.BLACK_STAINED_GLASS_PANE, dashboard.getItem(1).getType());
            assertEquals(Material.ORANGE_STAINED_GLASS_PANE, dashboard.getItem(3).getType());
            assertEquals(Material.GRAY_STAINED_GLASS_PANE, dashboard.getItem(11).getType());
            assertEquals(Material.FEATHER, dashboard.getItem(19).getType());
            assertEquals(Material.SPYGLASS, dashboard.getItem(14).getType());
            assertEquals(Material.CHEST, dashboard.getItem(45).getType());
            assertEquals(Material.NAME_TAG, dashboard.getItem(47).getType());

            Inventory category = menu.createCategory(f.player, "trail", 0, true);
            CosmeticsMenuHolder categoryHolder = (CosmeticsMenuHolder) category.getHolder();
            assertTrue(categoryHolder.isPreview());
            assertEquals(Material.FEATHER, category.getItem(19).getType());
            assertEquals(Material.FEATHER, category.getItem(20).getType());
            assertEquals(Material.BOOK, category.getItem(45).getType());
            verify(f.cosmetics, never()).toggleCosmetic(any(), any());
        }
    }

    private static CosmeticDefinition cosmetic(String id, String category, String name) {
        return new CosmeticDefinition(id, name, category, CosmeticType.TRAIL_PARTICLE,
            Material.FEATHER, org.bukkit.Particle.HAPPY_VILLAGER, null, null, null,
            "test." + id, 1, 0.1, 0, 0);
    }

    private static final class Fixture implements AutoCloseable {
        final EnthusiaTagsPlugin plugin = mock(EnthusiaTagsPlugin.class);
        final TagService tags = mock(TagService.class);
        final CosmeticsService cosmetics = mock(CosmeticsService.class);
        final EntitlementService entitlements = mock(EntitlementService.class);
        final Messages messages = mock(Messages.class);
        final Player player = mock(Player.class);
        final UUID id = UUID.randomUUID();
        final MockedStatic<Bukkit> bukkit;
        final MockedConstruction<ItemStack> stacks;

        Fixture() {
            when(plugin.getName()).thenReturn("EnthusiaTags");
            when(plugin.namespace()).thenReturn("enthusiatags");
            when(plugin.getEntitlementService()).thenReturn(entitlements);
            when(plugin.getCosmeticsService()).thenReturn(cosmetics);
            when(plugin.getMessages()).thenReturn(messages);
            when(tags.getPlugin()).thenReturn(plugin);
            when(tags.getMessages()).thenReturn(messages);
            when(player.getUniqueId()).thenReturn(id);
            when(player.hasPermission("enthusia.tags.admin")).thenReturn(true);
            when(messages.get(anyString())).thenAnswer(i -> i.getArgument(0));

            bukkit = mockStatic(Bukkit.class);
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(54), any(Component.class)))
                .thenAnswer(i -> inventory(i.getArgument(0), 54));

            stacks = mockConstruction(ItemStack.class, (stack, context) -> {
                Material material = (Material) context.arguments().getFirst();
                ItemMeta meta = mock(ItemMeta.class);
                AtomicReference<Component> name = new AtomicReference<>();
                AtomicReference<List<Component>> lore = new AtomicReference<>(List.of());
                AtomicReference<Boolean> glint = new AtomicReference<>(false);
                when(stack.getType()).thenReturn(material);
                when(stack.getItemMeta()).thenReturn(meta);
                when(stack.hasItemMeta()).thenReturn(true);
                when(meta.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
                doAnswer(i -> { name.set(i.getArgument(0)); return null; }).when(meta).displayName(any(Component.class));
                when(meta.displayName()).thenAnswer(i -> name.get());
                doAnswer(i -> { lore.set(i.getArgument(0)); return null; }).when(meta).lore(anyList());
                when(meta.lore()).thenAnswer(i -> lore.get());
                doAnswer(i -> { glint.set(i.getArgument(0)); return null; }).when(meta).setEnchantmentGlintOverride(anyBoolean());
                when(meta.getEnchantmentGlintOverride()).thenAnswer(i -> glint.get());
            });
        }

        private Inventory inventory(InventoryHolder holder, int size) {
            Inventory inventory = mock(Inventory.class);
            ItemStack[] contents = new ItemStack[size];
            when(inventory.getHolder()).thenReturn(holder);
            when(inventory.getSize()).thenReturn(size);
            when(inventory.getItem(anyInt())).thenAnswer(i -> contents[(int) i.getArgument(0)]);
            when(inventory.getContents()).thenAnswer(i -> contents.clone());
            doAnswer(i -> { contents[(int) i.getArgument(0)] = i.getArgument(1); return null; })
                .when(inventory).setItem(anyInt(), any());
            return inventory;
        }

        @Override public void close() {
            stacks.close();
            bukkit.close();
        }
    }
}
