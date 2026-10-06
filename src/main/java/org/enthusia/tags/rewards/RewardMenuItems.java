package org.enthusia.tags.rewards;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.enthusia.tags.TagService;
import org.enthusia.tags.TagTextFormat;

/** Vanilla items only. No model IDs, custom textures or resource-pack dependency. */
final class RewardMenuItems {
    private final RewardService service;
    private final TagService tags;
    RewardMenuItems(RewardService service, TagService tags) { this.service = service; this.tags = tags; }
    static ItemStack item(Material material, String name, List<String> lines, boolean glint) {
        ItemStack item = new ItemStack(material == null || material == Material.AIR || material == Material.CAVE_AIR
            || material == Material.VOID_AIR ? Material.PAPER : material);
        var meta = item.getItemMeta();
        meta.displayName(RewardMenuText.component(name));
        meta.lore(lines.stream().map(RewardMenuText::component).toList());
        meta.setEnchantmentGlintOverride(glint);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }
    static ItemStack item(Material material, String name, String... lines) { return item(material, name, List.of(lines), false); }
    ItemStack category(RewardCategory category, RewardMenuModel.Summary summary, boolean selected) {
        List<String> lore = new ArrayList<>();
        lore.add("&7" + categoryDescription(category.id()));
        lore.add("");
        lore.add("&7Claimed: &f" + summary.claimed() + " &7/ &f" + summary.total());
        lore.add("&7Ready to claim: &a" + summary.ready());
        if (summary.attention() > 0) lore.add("&7Pending / needs attention: &6" + summary.attention());
        if (summary.unavailable() > 0) lore.add("&7Temporarily unavailable: &6" + summary.unavailable());
        lore.add("");
        lore.add(selected ? "&6Selected category" : "&eClick to browse");
        return item(category.icon(), (selected ? "&6" : "&f") + RewardMenuText.categoryName(category), lore, false);
    }
    private String categoryDescription(String id) {
        return switch(id.toLowerCase(java.util.Locale.ROOT)) {
            case "playtime" -> "Rewards for time spent on Enthusia.";
            case "advancements" -> "Rewards earned through custom advancements.";
            case "supporter" -> "Permanent supporter rewards and current-rank perks.";
            case "legacy" -> "Historical rewards permanently tied to your account.";
            case "events" -> "Champion and special event rewards.";
            case "mining" -> "Gather resources and reach mining milestones.";
            case "combat" -> "Complete combat challenges.";
            case "deaths" -> "Milestones from your less fortunate moments.";
            case "economy" -> "Build your wealth and economic progress.";
            case "exploration" -> "Explore the world and travel farther.";
            default -> "Browse additional server challenges.";
        };
    }
    ItemStack browserHeader(Material icon, String title, RewardMenuModel.Summary summary, boolean readyView) {
        List<String> lore = new ArrayList<>();
        lore.add("&7" + summary.total() + (summary.total() == 1 ? " reward" : " rewards"));
        lore.add("&7Claimed: &f" + summary.claimed() + " &8• &a" + summary.ready() + " ready");
        if (summary.attention() > 0) lore.add("&6" + summary.attention() + " need attention");
        lore.add("");
        lore.add("&7Reward controls are kept in the bar below.");
        lore.add("&eClick to refresh");
        return item(icon, (readyView ? "&a" : "&6") + title, lore, false);
    }
    ItemStack summary(String title, RewardMenuModel.Summary summary, boolean refresh) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Claimed: &f" + summary.claimed() + " &7/ &f" + summary.total());
        lore.add("&7Ready to claim: &a" + summary.ready());
        if (summary.attention() > 0) lore.add("&7Pending / needs attention: &6" + summary.attention());
        if (summary.unavailable() > 0) lore.add("&7Temporarily unavailable: &6" + summary.unavailable());
        lore.add("");
        lore.add("&7Claimed completion");
        lore.add(RewardMenuText.bar(summary.total() == 0 ? 0 : (double) summary.claimed() / summary.total()));
        lore.add("");
        lore.add(refresh ? "&eClick to refresh this view" : "&7Completed requirements do not auto-claim.");
        return item(Material.BOOK, "&6" + title, lore, false);
    }
    ItemStack reward(RewardMenuModel.Entry row, boolean claiming, boolean focused, RewardClaimResult notice) {
        RewardDefinition definition = row.reward();
        List<String> lore = new ArrayList<>();
        if (row.group() != RewardMenuState.Group.ALL) lore.add("&7" + row.group().label());
        for (String line : definition.getDescription()) {
            for (String wrapped : RewardMenuText.wrap(line, 40)) lore.add("&7" + wrapped);
        }
        lore.add("");
        addProgress(lore, row);
        lore.add("");
        if (definition.getActions().isEmpty()) lore.add("&7Reward: &fRecognition only");
        else if (definition.getActions().size() == 1) lore.add("&7Reward: &f" + action(definition.getActions().getFirst(), definition));
        else {
            lore.add("&7Rewards");
            for (RewardAction action : definition.getActions()) lore.add("&f  " + action(action, definition));
        }
        if (definition.getActions().stream().anyMatch(RewardAction::isGoldNetworkLimited)) {
            for (String line : RewardMenuText.wrap(service.getMessage("rewards-gold-policy"), 40)) lore.add("&7" + line);
        }
        lore.add("");
        addStatus(lore, row.displayState(), claiming);
        String last = RewardMenuText.notice(notice);
        if (!last.isBlank() && !claiming) for (String line : RewardMenuText.wrap(last,40)) lore.add("&6" + line);
        if (focused) lore.add("&7Opened from your selected goal");
        return item(definition.getIcon(), (row.claimed() ? "&7" : "&f") + RewardMenuText.plain(definition.getName()), lore, row.ready() && !claiming);
    }
    private void addProgress(List<String> lore, RewardMenuModel.Entry row) {
        if (row.claimed()) { lore.add("&aRequirements completed"); return; }
        if (row.evaluation().previouslyUnlocked() && row.reward().getCompletionMode() == RewardCompletionMode.LATCHED) {
            lore.add("&aRequirements completed previously");
            if (!row.progressKnown()) { lore.add("&7Current progress temporarily unavailable"); return; }
        }
        if (row.readings().isEmpty()) { lore.add("&6Progress temporarily unavailable"); return; }
        boolean single = row.readings().size() == 1;
        if (!single) lore.add("&7Requirements");
        for (RewardMenuModel.Reading reading : row.readings()) {
            RewardCriterion criterion = reading.criterion();
            String label = single ? "Progress" : RewardMenuText.criterionLabel(criterion);
            if (reading.value().isEmpty()) {
                lore.add("&7" + label + ": &6Temporarily unavailable");
                continue;
            }
            long current = reading.value().getAsLong();
            long goal = criterion.getAmount();
            if (criterion.getType() == RewardCriterionType.BALTOP_TOP3) {
                lore.add("&7" + (single ? RewardMenuText.criterionLabel(criterion) : label) + ": "
                    + (current >= goal ? "&aReached" : "&fNot reached"));
            } else {
                lore.add("&7" + label + ": &f" + RewardMenuText.value(Math.min(current, Math.max(0,goal)), criterion)
                    + " &7/ &f" + RewardMenuText.value(goal, criterion));
            }
            if (single && goal > 0) lore.add(RewardMenuText.bar(Math.min(1, (double) current / goal)));
        }
    }
    private static void addStatus(List<String> lore, RewardMenuModel.DisplayState state, boolean claiming) {
        if (claiming) { lore.add("&6Claiming..."); lore.add("&7Please wait; your claim is being processed."); return; }
        switch(state) {
            case CLAIMED -> lore.add("&7Claimed ✓");
            case READY -> { lore.add("&aREADY TO CLAIM"); lore.add("&eClick to claim"); }
            case NOT_STARTED -> lore.add("&7Not started");
            case IN_PROGRESS -> lore.add("&6In progress");
            case UNAVAILABLE -> { lore.add("&6Temporarily unavailable"); lore.add("&7Refresh once the provider is ready."); }
            case PENDING -> { lore.add("&6Delivery pending"); lore.add("&eClick to check / recover delivery"); }
            case QUEUED -> { lore.add("&6Item delivery queued"); lore.add("&7Free inventory space."); lore.add("&eClick to retry item delivery"); }
            case RETRY -> { lore.add("&6Delivery failed"); lore.add("&eClick to retry safely"); }
            case REVIEW -> { lore.add("&cDelivery needs review"); lore.add("&7Contact staff; do not repeat the claim."); }
            case WITHHELD -> { lore.add("&6Gold withheld by network policy"); lore.add("&7Other reward components remain separate."); }
        }
    }
    private String action(RewardAction action, RewardDefinition reward) {
        return switch(action.getType()) {
            case MONEY -> RewardMenuText.money(action.getAmount());
            case ITEM -> action.getItemAmount() + " × " + (action.getDisplayName() != null && !action.getDisplayName().isBlank()
                ? RewardMenuText.plain(action.getDisplayName())
                : action.getMaterial() == null ? "Item" : RewardMenuText.titleCase(action.getMaterial().name()));
            case TAG -> {
                var definition = tags.getRegistry().get(action.getValue());
                String display = definition == null
                    ? RewardMenuText.plain(action.getValue())
                    : TagTextFormat.legacyText(definition.getDisplayName());
                yield "Tag: " + display;
            }
            case COMMAND -> "Unlock: " + (action.getLabel() != null && !action.getLabel().isBlank()
                ? RewardMenuText.plain(action.getLabel()) : RewardMenuText.plain(reward.getName()));
            case LORE_ITEM -> action.getLabel() != null && !action.getLabel().isBlank()
                ? RewardMenuText.plain(action.getLabel()) : "Special item";
        };
    }
}
