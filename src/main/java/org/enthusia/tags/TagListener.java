package org.enthusia.tags;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.enthusia.tags.cosmetics.CosmeticsMenu;
import org.enthusia.tags.rewards.RewardMenu;
import org.enthusia.tags.rewards.RewardService;

public final class TagListener implements Listener {
    private final TagService tagService;
    private final TagMenu tagMenu;
    private final RewardMenu rewardMenu;
    private final RewardService rewardService;

    public TagListener(TagService tagService, RewardService rewardService) {
        this.tagService = tagService;
        this.tagMenu = new TagMenu(tagService);
        this.rewardMenu = new RewardMenu(rewardService, tagService);
        this.rewardService = rewardService;
    }

    @EventHandler
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        tagService.preloadPlayerBlocking(event.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        tagService.loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tagService.unloadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(tagService.getPlugin(), () -> tagService.requestNametagRefresh(event.getPlayer()));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Bukkit.getScheduler().runTask(tagService.getPlugin(), () -> tagService.requestNametagRefresh(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleportEnd(PlayerTeleportEvent event) {
        Bukkit.getScheduler().runTask(tagService.getPlugin(), () -> tagService.requestNametagRefresh(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInvisibilityChange(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getModifiedType() != PotionEffectType.INVISIBILITY) return;
        Bukkit.getScheduler().runTask(tagService.getPlugin(), () -> tagService.requestNametagRefresh(player));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder rawHolder = top.getHolder();
        if (!(rawHolder instanceof TagMenuHolder holder) || holder.getTagService() != tagService) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != top || event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();

        if (data.has(tagMenu.getRewardsKey(), PersistentDataType.BYTE)) {
            if (!rewardService.isAvailable()) {
                player.sendMessage(message("rewards-service-unavailable"));
                return;
            }
            player.openInventory(rewardMenu.create(player));
            return;
        }
        if (data.has(tagMenu.getCosmeticsKey(), PersistentDataType.BYTE)) {
            CosmeticsMenu cosmetics = new CosmeticsMenu(
                tagService.getPlugin().getCosmeticsService(), tagService, tagService.getMessages());
            player.openInventory(cosmetics.createMain(player, holder.isPreview()));
            return;
        }
        if (data.has(tagMenu.getCloseKey(), PersistentDataType.BYTE)) {
            player.closeInventory();
            return;
        }
        if (data.has(tagMenu.getPreviewKey(), PersistentDataType.BYTE)) {
            if (!player.hasPermission("enthusia.tags.admin")) {
                player.sendMessage(message("no-permission"));
                return;
            }
            player.openInventory(tagMenu.create(player, holder.getFilter(), holder.getPage(), !holder.isPreview()));
            return;
        }
        String filter = data.get(tagMenu.getFilterKey(), PersistentDataType.STRING);
        if (filter != null) {
            player.openInventory(tagMenu.create(player, filter, 0, holder.isPreview()));
            return;
        }
        if (data.has(tagMenu.getPrevKey(), PersistentDataType.BYTE)) {
            player.openInventory(tagMenu.create(player, holder.getFilter(), holder.getPage() - 1, holder.isPreview()));
            return;
        }
        if (data.has(tagMenu.getNextKey(), PersistentDataType.BYTE)) {
            player.openInventory(tagMenu.create(player, holder.getFilter(), holder.getPage() + 1, holder.isPreview()));
            return;
        }
        if (data.has(tagMenu.getClearKey(), PersistentDataType.BYTE)) {
            if (holder.isPreview()) {
                player.sendMessage(message("admin-preview-readonly"));
                return;
            }
            tagService.setSelectedTag(player, null);
            player.sendMessage(message("tag-cleared-self"));
            player.openInventory(tagMenu.create(player, holder.getFilter(), holder.getPage(), false));
            return;
        }

        String tagId = data.get(tagMenu.getTagIdKey(), PersistentDataType.STRING);
        if (tagId == null) return;
        if (holder.isPreview()) {
            player.sendMessage(message("admin-preview-readonly"));
            return;
        }
        boolean updated = tagService.setSelectedTag(player, tagId);
        player.sendMessage(updated ? message("tag-selected-self") : message("tag-not-owned-self"));
        player.openInventory(tagMenu.create(player, holder.getFilter(), holder.getPage(), false));
    }

    private Component message(String key) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(tagService.getMessages().get(key));
    }
}
