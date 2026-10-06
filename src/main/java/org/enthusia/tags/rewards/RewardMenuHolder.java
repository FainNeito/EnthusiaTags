package org.enthusia.tags.rewards;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class RewardMenuHolder implements InventoryHolder {
    private final RewardService rewardService;
    private RewardMenuState state;
    private Inventory inventory;
    private final Map<Integer, RewardMenuAction> actions = new LinkedHashMap<>();
    private final Map<String, RewardClaimResult> notices = new LinkedHashMap<>();
    private List<String> visibleRewards = List.of();
    private int pageCount = 1;
    private boolean scheduled;
    public RewardMenuHolder(RewardService service) { this(service, RewardMenuState.dashboard()); }
    public RewardMenuHolder(RewardService service, String category, int page) {
        this(service, category == null ? RewardMenuState.dashboard().withPage(page) : RewardMenuState.category(category).withPage(page));
    }
    public RewardMenuHolder(RewardService service, RewardMenuState state) { this.rewardService = service; this.state = state; }
    public RewardService getRewardService() { return rewardService; }
    public String getCategory() { return state.category(); }
    public int getPage() { return state.page(); }
    public RewardMenuState state() { return state; }
    public void state(RewardMenuState state) { this.state = state; }
    public int pageCount() { return pageCount; }
    public List<String> visibleRewards() { return visibleRewards; }
    public void page(List<String> ids, int pages, int selectedPage) {
        visibleRewards = List.copyOf(ids); pageCount = Math.max(1, pages); state = state.withPage(selectedPage);
    }
    public void clearActions() { actions.clear(); }
    public void action(int slot, RewardMenuAction action) { actions.put(slot, action); }
    public RewardMenuAction action(int slot) { return actions.get(slot); }
    public boolean schedule() { if (scheduled) return false; scheduled = true; return true; }
    public void unschedule() { scheduled = false; }
    public RewardClaimResult notice(String id) { return notices.get(id); }
    public void notice(String id, RewardClaimResult result) { notices.put(id, result); }
    public void clearNotices() { notices.clear(); }
    public void clearNotice(String id) { notices.remove(id); }
    public void setInventory(Inventory value) { inventory = value; }
    @Override public Inventory getInventory() { return inventory; }
}
