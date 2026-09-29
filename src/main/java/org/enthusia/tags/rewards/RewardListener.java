package org.enthusia.tags.rewards;

import java.util.Locale;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/** Controls the browser, but delegates every claim/recovery to the existing RewardService. */
public final class RewardListener implements Listener {
    private final RewardService service;
    private final RewardMenu menu;
    public RewardListener(RewardService service, RewardMenu menu) { this.service = service; this.menu = menu; }
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Retain the established retry mechanism for already-claimed queued items.
        if(event.getPlayer() instanceof Player player) service.retryQueuedItems(player);
    }
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if(owned(event.getView().getTopInventory()) != null) event.setCancelled(true);
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        RewardMenuHolder holder = owned(top);
        if(holder == null) return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player player)) return;
        // Never interpret a player's own item, a shift-transfer, an offhand swap or a double-click as a GUI button.
        if(event.getClickedInventory() != top || event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) return;
        if(event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) return;
        RewardMenuAction action = holder.action(event.getRawSlot());
        if(action == null || (action.type() == RewardMenuAction.Type.CLAIM && event.getClick() != ClickType.LEFT)) return;
        if(!holder.schedule()) return;
        boolean reverse = event.getClick() == ClickType.RIGHT;
        try {
            menu.nextTick(() -> {
                holder.unschedule();
                if(!player.isOnline() || player.getOpenInventory().getTopInventory() != top) return;
                handle(player,holder,action,reverse);
            });
        } catch(RuntimeException error) { holder.unschedule(); throw error; }
    }
    private RewardMenuHolder owned(Inventory top) {
        return top.getHolder() instanceof RewardMenuHolder holder && holder.getRewardService() == service ? holder : null;
    }
    private void handle(Player player, RewardMenuHolder holder, RewardMenuAction action, boolean reverse) {
        RewardMenuState state = holder.state();
        switch(action.type()) {
            case CATEGORY -> menu.navigate(player,holder,RewardMenuState.category(action.value()));
            case BACK -> menu.navigate(player,holder,RewardMenuState.dashboard());
            case READY -> menu.navigate(player,holder,RewardMenuState.ready());
            case FILTER -> menu.navigate(player,holder,state.withFilter(state.filter().cycle(reverse)));
            case SORT -> menu.navigate(player,holder,state.withSort(state.sort().cycle(reverse)));
            case GROUP -> menu.navigate(player,holder,state.withGroup(state.group().cycle(reverse)));
            case PREVIOUS -> { if(state.page() > 0) menu.navigate(player,holder,state.withPage(state.page()-1)); }
            case NEXT -> { if(state.page()+1 < holder.pageCount()) menu.navigate(player,holder,state.withPage(state.page()+1)); }
            case CLOSE -> player.closeInventory();
            case REFRESH -> { holder.clearNotices(); menu.refresh(player,holder,false); }
            case TAGS -> menu.openTags(player);
            case COSMETICS -> menu.openCosmetics(player);
            case CLAIM -> claim(player,holder,action.value());
        }
    }
    private void claim(Player player, RewardMenuHolder holder, String id) {
        RewardDefinition reward = service.getRewards().get(id.toLowerCase(Locale.ROOT));
        if(reward == null) { menu.refresh(player,holder,false); return; }
        UUID playerId = player.getUniqueId();
        if(!menu.beginClaim(playerId,id)) return;
        holder.clearNotice(id);
        try {
            menu.refresh(player,holder,true);
            service.claimAsync(player,reward).whenComplete((result,error) -> {
                // This release is unconditional, even when the player disconnects or the service is stopping.
                menu.endClaim(playerId,id);
                RewardClaimResult outcome = error != null || result == null ? RewardClaimResult.DELIVERY_FAILED : result;
                service.runForOnlinePlayer(playerId,current -> {
                    current.sendMessage(RewardMenuText.component(service.getMessage(RewardMenuText.resultMessageKey(outcome))));
                    // Do not reopen a closed GUI or switch someone away from a different inventory.
                    if(current.getOpenInventory().getTopInventory() == holder.getInventory()) {
                        holder.notice(id,outcome);
                        menu.refresh(current,holder,true);
                    }
                });
            });
        } catch(RuntimeException error) {
            menu.endClaim(playerId,id);
            holder.notice(id,RewardClaimResult.DELIVERY_FAILED);
            player.sendMessage(RewardMenuText.component(service.getMessage("rewards-delivery-failed")));
            menu.refresh(player,holder,true);
        }
    }
}
