package org.enthusia.tags.rewards;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.enthusia.tags.EnthusiaTagsPlugin;
import org.enthusia.tags.Messages;
import org.enthusia.tags.TagService;

import java.util.Collections;
import java.util.List;

public final class RewardsCommand implements CommandExecutor, TabCompleter {
    private static final String GUIDE_COMMAND = "guide";
    private static final int TARGET_ARGUMENTS = 2;
    private final RewardMenu rewardMenu;
    private final Messages messages;
    private final EnthusiaTagsPlugin plugin;
    private final RewardService rewardService;
    private final RewardGuide guide;

    public RewardsCommand(RewardService rewardService, TagService tagService, Messages messages, EnthusiaTagsPlugin plugin) {
        this.rewardMenu = new RewardMenu(rewardService, tagService);
        this.rewardService = rewardService;
        this.messages = messages;
        this.plugin = plugin;
        this.guide = new RewardGuide(plugin, rewardService);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!rewardService.isAvailable()) {
            sender.sendMessage(message("rewards-service-unavailable"));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("enthusia.tags.admin")) {
                sender.sendMessage(message("no-permission"));
                return true;
            }
            plugin.reloadAllFiles();
            sender.sendMessage(message("config-reloaded"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(message("players-only"));
            return true;
        }
        return handlePlayerCommand(player, args);
    }

    private boolean handlePlayerCommand(Player player, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase(GUIDE_COMMAND)) {
            guide.open(player, args.length == 1 ? null : args[1]);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("retryitems")) {
            rewardService.retryQueuedItems(player);
            player.sendMessage(Component.text("Queued item delivery retry requested."));
            return true;
        }
        if (args.length == TARGET_ARGUMENTS && args[0].equalsIgnoreCase("open")) {
            openFocused(player, args[1]);
            return true;
        }
        player.openInventory(rewardMenu.create(player));
        return true;
    }

    private void openFocused(Player player, String id) {
        RewardDefinition reward = rewardService.getRewards().get(id.toLowerCase(java.util.Locale.ROOT));
        if (reward == null) {
            player.sendMessage(Component.text("Unknown reward."));
        } else {
            player.openInventory(rewardMenu.createFocused(player, reward));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player player && guide.allowed(player)) {
            if (args.length == TARGET_ARGUMENTS && args[0].equalsIgnoreCase(GUIDE_COMMAND)) return List.of("build", "social", "combat");
            if (args.length == 1) return sender.hasPermission("enthusia.tags.admin")
                ? List.of("reload", "open", "retryitems", GUIDE_COMMAND) : List.of("retryitems", GUIDE_COMMAND);
        }
        if (args.length == 1) {
            return sender.hasPermission("enthusia.tags.admin")
                ? List.of("reload", "open", "retryitems") : List.of("retryitems");
        }
        return Collections.emptyList();
    }

    public RewardMenu getRewardMenu() {
        return rewardMenu;
    }

    private Component message(String key) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(messages.get(key));
    }
}
