package org.enthusia.tags.rewards;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RewardBrowserInteractionTest {
    private InventoryClickEvent click(RewardGuiFixture f,Inventory top,int slot,ClickType type,boolean bottom) {
        var event=mock(InventoryClickEvent.class);var clicked=bottom?mock(Inventory.class):top;
        when(event.getView()).thenReturn(f.view);when(event.getWhoClicked()).thenReturn(f.player);
        when(event.getClickedInventory()).thenReturn(clicked);when(event.getRawSlot()).thenReturn(slot);when(event.getClick()).thenReturn(type);
        return event;
    }
    private RewardDefinition setup(RewardGuiFixture f) {
        var reward=f.add("reward","mining",100,100,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.UNLOCKED);
        f.current=f.menu.createCategory(f.player,"mining");return reward;
    }
    @Test void rapidClaimsAreSerializedAndSuccessfulClaimUpdatesSameView() {
        try(var f=new RewardGuiFixture()) {
            var reward=setup(f);var future=new CompletableFuture<RewardClaimResult>();when(f.service.claimAsync(f.player,reward)).thenReturn(future);
            var listener=new RewardListener(f.service,f.menu);var original=f.current;var h=(RewardMenuHolder)original.getHolder();
            listener.onInventoryClick(click(f,original,19,ClickType.LEFT,false));listener.onInventoryClick(click(f,original,19,ClickType.LEFT,false));
            verify(f.service,never()).claimAsync(any(),any());assertEquals(1,f.nextTick.size());f.tick();
            assertTrue(f.menu.claiming(f.player.getUniqueId(),"reward"));assertTrue(RewardGuiFixture.lore(original.getItem(19)).contains("Claiming..."));
            listener.onInventoryClick(click(f,original,19,ClickType.LEFT,false));f.tick();verify(f.service,times(1)).claimAsync(f.player,reward);
            f.status("reward",RewardStatus.CLAIMED);future.complete(RewardClaimResult.SUCCESS);
            assertFalse(f.menu.claiming(f.player.getUniqueId(),"reward"));assertSame(original,f.current);
            assertEquals(List.of("reward"),h.visibleRewards());assertTrue(RewardGuiFixture.lore(original.getItem(19)).contains("Claimed"));
            verify(f.player,never()).openInventory(any(Inventory.class));
        }
    }
    @Test void transfersBottomInventoryAndNonstandardClicksNeverTriggerClaims() {
        try(var f=new RewardGuiFixture()) {
            setup(f);var listener=new RewardListener(f.service,f.menu);
            for(ClickType type:List.of(ClickType.SHIFT_LEFT,ClickType.SHIFT_RIGHT,ClickType.NUMBER_KEY,ClickType.SWAP_OFFHAND,ClickType.DOUBLE_CLICK,ClickType.MIDDLE,ClickType.DROP,ClickType.RIGHT)) {
                var event=click(f,f.current,19,type,false);listener.onInventoryClick(event);verify(event).setCancelled(true);
            }
            listener.onInventoryClick(click(f,f.current,19,ClickType.LEFT,true));
            listener.onInventoryClick(click(f,f.current,-999,ClickType.LEFT,false));f.tick();
            verify(f.service,never()).claimAsync(any(),any());
            var drag=mock(InventoryDragEvent.class);when(drag.getView()).thenReturn(f.view);listener.onInventoryDrag(drag);verify(drag).setCancelled(true);
        }
    }
    @Test void closingOrChangingMenuBeforeNextTickCancelsTheScheduledClick() {
        try(var f=new RewardGuiFixture()) {
            setup(f);var listener=new RewardListener(f.service,f.menu);var h=(RewardMenuHolder)f.current.getHolder();
            listener.onInventoryClick(click(f,f.current,19,ClickType.LEFT,false));f.current=mock(Inventory.class);f.tick();
            verify(f.service,never()).claimAsync(any(),any());assertTrue(h.schedule());h.unschedule();
        }
    }
    @Test void asyncCompletionDoesNotReopenAClosedMenuOrOverrideAnotherMenu() {
        try(var f=new RewardGuiFixture()) {
            var reward=setup(f);var future=new CompletableFuture<RewardClaimResult>();when(f.service.claimAsync(f.player,reward)).thenReturn(future);
            var listener=new RewardListener(f.service,f.menu);listener.onInventoryClick(click(f,f.current,19,ClickType.LEFT,false));f.tick();
            var other=mock(Inventory.class);f.current=other;future.complete(RewardClaimResult.SUCCESS);
            assertSame(other,f.current);assertFalse(f.menu.claiming(f.player.getUniqueId(),"reward"));
            verify(f.player,never()).openInventory(any(Inventory.class));
        }
    }
    @Test void disconnectedPlayerReleasesClaimGuardWithoutAReopen() {
        try(var f=new RewardGuiFixture()) {
            var reward=setup(f);var future=new CompletableFuture<RewardClaimResult>();when(f.service.claimAsync(f.player,reward)).thenReturn(future);
            var listener=new RewardListener(f.service,f.menu);listener.onInventoryClick(click(f,f.current,19,ClickType.LEFT,false));f.tick();
            when(f.player.isOnline()).thenReturn(false);future.complete(RewardClaimResult.SUCCESS);
            assertFalse(f.menu.claiming(f.player.getUniqueId(),"reward"));verify(f.player,never()).openInventory(any(Inventory.class));
        }
    }
    @Test void failedDeliveryStaysVisibleAndCanRetryThroughOriginalService() {
        try(var f=new RewardGuiFixture()) {
            var reward=setup(f);var future=new CompletableFuture<RewardClaimResult>();when(f.service.claimAsync(f.player,reward)).thenReturn(future);
            var listener=new RewardListener(f.service,f.menu);listener.onInventoryClick(click(f,f.current,19,ClickType.LEFT,false));f.tick();
            future.completeExceptionally(new IllegalStateException("unavailable"));
            assertFalse(f.menu.claiming(f.player.getUniqueId(),"reward"));assertTrue(RewardGuiFixture.lore(f.current.getItem(19)).contains("Delivery failed"));
            when(f.service.claimAsync(f.player,reward)).thenReturn(CompletableFuture.completedFuture(RewardClaimResult.SUCCESS));
            listener.onInventoryClick(click(f,f.current,19,ClickType.LEFT,false));f.tick();verify(f.service,times(2)).claimAsync(f.player,reward);
        }
    }
    @Test void filterAndSortControlsDoNotDeliverRewards() {
        try(var f=new RewardGuiFixture()) {
            setup(f);var listener=new RewardListener(f.service,f.menu);var h=(RewardMenuHolder)f.current.getHolder();
            listener.onInventoryClick(click(f,f.current,15,ClickType.LEFT,false));f.tick();assertEquals(RewardMenuState.Filter.READY,h.state().filter());
            listener.onInventoryClick(click(f,f.current,16,ClickType.RIGHT,false));f.tick();assertEquals(RewardMenuState.Sort.NAME,h.state().sort());
            f.menu.startRefresh();f.menu.startRefresh();assertEquals(1,f.refreshTasks.size());f.refreshTasks.getFirst().run();
            verify(f.service,never()).claimAsync(any(),any());
        }
    }
    @Test void closingRetainsTheExistingQueuedItemRecoveryHook() {
        try(var f=new RewardGuiFixture()) {
            var event=mock(InventoryCloseEvent.class);when(event.getPlayer()).thenReturn(f.player);
            new RewardListener(f.service,f.menu).onInventoryClose(event);verify(f.service).retryQueuedItems(f.player);
        }
    }
}
