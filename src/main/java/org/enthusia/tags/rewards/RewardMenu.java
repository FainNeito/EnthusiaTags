package org.enthusia.tags.rewards;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.enthusia.tags.TagService;
import org.enthusia.tags.TagTextFormat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RewardMenu {
    private final RewardService rewardService;
    private final TagService tagService;
    private final NamespacedKey rewardKey;
    private final NamespacedKey categoryKey;
    private final NamespacedKey backKey;
    private final NamespacedKey nextKey;
    private final NamespacedKey prevKey;

    public RewardMenu(RewardService rewardService, TagService tagService) {
        this.rewardService = rewardService;
        this.tagService = tagService;
        this.rewardKey = new NamespacedKey(tagService.getPlugin(), "reward_id");
        this.categoryKey = new NamespacedKey(tagService.getPlugin(), "reward_category");
        this.backKey = new NamespacedKey(tagService.getPlugin(), "reward_back");
        this.nextKey = new NamespacedKey(tagService.getPlugin(), "reward_next");
        this.prevKey = new NamespacedKey(tagService.getPlugin(), "reward_prev");
    }

    public Inventory create(Player player) {
        return createCategory(player, null, 0);
    }

    public Inventory createCategory(Player player, String categoryId) {
        return createCategory(player, categoryId, 0);
    }

    public Inventory createFocused(Player player, RewardDefinition target) {
        int index = (int) rewardService.getConfig().categories().values().stream()
            .filter(category -> target.getCategory().equals(category.parent())).count();
        RewardCategory category = rewardService.getConfig().categories().get(target.getCategory());
        if (category != null) index += (int) category.tags().stream()
            .filter(id -> tagService.getRegistry().get(id) != null).count();
        for (RewardDefinition reward : rewardService.getRewards().values()) {
            if (!reward.getCategory().equalsIgnoreCase(target.getCategory())) continue;
            if (reward.getId().equalsIgnoreCase(target.getId())) {
                return createCategory(player, target.getCategory(), index / 45, target.getId());
            }
            index++;
        }
        return create(player);
    }

    public Inventory createCategory(Player player, String categoryId, int page) {
        return createCategory(player, categoryId, page, null);
    }

    private Inventory createCategory(Player player, String categoryId, int page, String focusedRewardId) {
        Map<String, RewardCategory> categories = rewardService.getConfig().categories();
        RewardCategory category = categoryId == null ? null : categories.get(categoryId);
        String titleText = category == null
            ? rewardService.getMessage("rewards-gui-title")
            : rewardService.getMessage("rewards-category-title").replace("{category}", category.name());
        Component title = LegacyComponentSerializer.legacyAmpersand().deserialize(titleText);
        List<java.util.function.Supplier<ItemStack>> list = new ArrayList<>();
        for (RewardCategory child : categories.values()) {
            if (java.util.Objects.equals(child.parent(), categoryId)) list.add(() -> createCategoryItem(child));
        }
        if (category != null) {
            for (String tagId : category.tags()) {
                var tag = tagService.getRegistry().get(tagId);
                if (tag != null) list.add(() -> createHolidayTagItem(player, tag));
            }
        }
        long renderStart = System.nanoTime();
        RewardService.ProgressSnapshot snapshot = rewardService.getProgressSnapshot(player);
        for (RewardDefinition reward : rewardService.getRewards().values()) {
            if (!reward.getCategory().equalsIgnoreCase(categoryId)) {
                continue;
            }
            list.add(() -> createRewardItem(player, reward, snapshot,
                reward.getId().equalsIgnoreCase(focusedRewardId == null ? "" : focusedRewardId)));
        }
        int pageSize = 45;
        int safePage = Math.min(Math.max(0, page), Math.max(0, (list.size() - 1) / pageSize));
        RewardMenuHolder holder = new RewardMenuHolder(rewardService, categoryId, safePage);
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inventory);
        int start = safePage * pageSize;
        int end = Math.min(list.size(), start + pageSize);
        int slot = 0;
        for (int i = start; i < end; i++) {
            inventory.setItem(slot++, list.get(i).get());
        }
        if (tagService.getPlugin() instanceof org.enthusia.tags.EnthusiaTagsPlugin plugin) {
            plugin.getPerformanceMonitor().add("rewards.gui.items-rendered", end - start);
            plugin.getPerformanceMonitor().recordDurationMillis("rewards.gui.render",
                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - renderStart));
        }
        if (safePage > 0) inventory.setItem(45, createPrevItem());
        if (categoryId != null) inventory.setItem(49, createBackItem());
        if (end < list.size()) inventory.setItem(53, createNextItem());
        return inventory;
    }

    public Inventory createParent(Player player, String categoryId) {
        RewardCategory category = categoryId == null ? null : rewardService.getConfig().categories().get(categoryId);
        return createCategory(player, category == null ? null : category.parent());
    }

    private ItemStack createHolidayTagItem(Player player, org.enthusia.tags.TagDefinition tag) {
        ItemStack stack = new ItemStack(tag.getIcon());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(TagTextFormat.deserializeCompat(tag.getDisplayName()));
        List<Component> lore = new ArrayList<>();
        for (String line : tag.getDescription()) lore.add(TagTextFormat.deserializeCompat(line));
        boolean owned = tagService.getPlayerData(player.getUniqueId()).getOwnedTags().contains(tag.getId().toLowerCase(Locale.ROOT));
        lore.add(TagTextFormat.deserializeCompat(rewardService.getMessage(owned ? "rewards-holiday-owned" : "rewards-holiday-locked")));
        lore.add(TagTextFormat.deserializeCompat(rewardService.getMessage("rewards-holiday-event")));
        meta.lore(lore);
        // No reward or category key: catalog clicks must never issue an event award.
        stack.setItemMeta(meta);
        return stack;
    }

    public NamespacedKey getRewardKey() {
        return rewardKey;
    }

    public NamespacedKey getCategoryKey() {
        return categoryKey;
    }

    public NamespacedKey getBackKey() {
        return backKey;
    }

    public NamespacedKey getNextKey() {
        return nextKey;
    }

    public NamespacedKey getPrevKey() {
        return prevKey;
    }

    private ItemStack createRewardItem(Player player, RewardDefinition reward, RewardService.ProgressSnapshot snapshot,
                                       boolean focused) {
        ItemStack stack = new ItemStack(reward.getIcon());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(reward.getName()));

        List<Component> lore = new ArrayList<>();
        for (String line : reward.getDescription()) {
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
        }

        RewardEvaluation evaluation = rewardService.evaluate(player, reward, snapshot);
        boolean claimed = evaluation.status() == RewardStatus.CLAIMED;
        boolean complete = evaluation.claimable();
        if (focused) {
            lore.add(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(rewardService.getMessage("rewards-status-focused")));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (claimed) {
            lore.add(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(rewardService.getMessage("rewards-status-claimed")));
        } else if (evaluation.status() == RewardStatus.ITEM_QUEUED) {
            lore.add(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(rewardService.getMessage("rewards-status-queued")));
        } else if (evaluation.status() == RewardStatus.REQUIRES_RECONCILIATION) {
            lore.add(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(rewardService.getMessage("rewards-status-reconciliation")));
        } else if (evaluation.status() == RewardStatus.DELIVERY_FAILED) {
            lore.add(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(rewardService.getMessage("rewards-status-retryable")));
        } else if (evaluation.status() == RewardStatus.CLAIM_PENDING) {
            lore.add(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(rewardService.getMessage("rewards-status-pending")));
        } else if (complete) {
            lore.add(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(rewardService.getMessage("rewards-status-claimable")));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            lore.add(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(rewardService.getMessage("rewards-status-locked")));
        }

        lore.add(LegacyComponentSerializer.legacyAmpersand()
            .deserialize(rewardService.getMessage("rewards-progress-title")));
        for (RewardCriterion criterion : reward.getCriteria()) {
            long progress = rewardService.getProgress(player, criterion, snapshot);
            boolean done = progress >= criterion.getAmount();
            String line = rewardService.getMessage("rewards-progress-line")
                .replace("{label}", criterion.getLabel())
                .replace("{color}", done ? "&a" : "&c")
                .replace("{progress}", rewardService.formatProgress(progress, criterion));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
        }

        if (!reward.getActions().isEmpty()) {
            lore.add(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(rewardService.getMessage("rewards-rewards-title")));
            for (RewardAction action : reward.getActions()) {
                lore.add(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(formatActionLine(action)));
            }
            if (reward.getActions().stream().anyMatch(RewardAction::isGoldNetworkLimited)) {
                lore.add(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(rewardService.getMessage("rewards-gold-policy")));
            }
        }

        meta.lore(lore);
        meta.getPersistentDataContainer().set(rewardKey, PersistentDataType.STRING, reward.getId().toLowerCase(Locale.ROOT));
        stack.setItemMeta(meta);
        return stack;
    }

    private String formatActionLine(RewardAction action) {
        return switch (action.getType()) {
            case TAG -> {
                String tagName = action.getValue();
                var tag = tagService.getRegistry().get(action.getValue());
                if (tag != null) {
                    tagName = tag.getDisplayName();
                }
                yield rewardService.getMessage("rewards-rewards-line-tag")
                    .replace("{tag}", tagName);
            }
            case MONEY -> rewardService.getMessage("rewards-rewards-line-money")
                .replace("{amount}", formatAmount(action.getAmount()));
            case ITEM -> rewardService.getMessage("rewards-rewards-line-item")
                .replace("{amount}", String.valueOf(action.getItemAmount()))
                .replace("{item}", itemDisplayName(action));
            case COMMAND -> rewardService.getMessage("rewards-rewards-line-unlock")
                .replace("{unlock}", actionLabel(action, "rewards-rewards-unlock-default"));
            case LORE_ITEM -> rewardService.getMessage("rewards-rewards-line-lore-item")
                .replace("{item}", actionLabel(action, "rewards-rewards-lore-item-default"));
        };
    }

    private String itemDisplayName(RewardAction action) {
        if (action.getDisplayName() != null && !action.getDisplayName().isBlank()) {
            return action.getDisplayName();
        }
        if (action.getMaterial() == null) {
            return rewardService.getMessage("rewards-rewards-item-default");
        }
        return titleCase(action.getMaterial().name());
    }

    private String actionLabel(RewardAction action, String fallbackKey) {
        if (action.getLabel() != null && !action.getLabel().isBlank()) {
            return action.getLabel();
        }
        return rewardService.getMessage(fallbackKey);
    }

    private String formatAmount(double amount) {
        return BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString();
    }

    private String titleCase(String value) {
        String[] words = value.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private ItemStack createCategoryItem(RewardCategory category) {
        ItemStack stack = new ItemStack(category.icon() == null ? org.bukkit.Material.PAPER : category.icon());
        ItemMeta meta = stack.getItemMeta();
        String name = rewardService.getMessage("rewards-category-title")
            .replace("{category}", category.name());
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
        meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, category.id());
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createBackItem() {
        ItemStack stack = new ItemStack(org.bukkit.Material.BARRIER);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand()
            .deserialize(rewardService.getMessage("rewards-back")));
        meta.getPersistentDataContainer().set(backKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createNextItem() {
        ItemStack stack = new ItemStack(org.bukkit.Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand()
            .deserialize(rewardService.getMessage("rewards-next")));
        meta.getPersistentDataContainer().set(nextKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createPrevItem() {
        ItemStack stack = new ItemStack(org.bukkit.Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand()
            .deserialize(rewardService.getMessage("rewards-prev")));
        meta.getPersistentDataContainer().set(prevKey, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }
}
