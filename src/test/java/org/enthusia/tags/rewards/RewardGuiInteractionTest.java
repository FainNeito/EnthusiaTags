package org.enthusia.tags.rewards;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.event.inventory.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RewardGuiInteractionTest {
    private InventoryClickEvent click(RewardGuiFixture f,Inventory inventory,int slot,ClickType type) {
        var e=mock(InventoryClickEvent.class);when(e.getView()).thenReturn(f.view);
        when(e.getWhoClicked()).thenReturn(f.player);when(e.getClickedInventory()).thenReturn(inventory);
        when(e.getRawSlot()).thenReturn(slot);when(e.getClick()).thenReturn(type);return e;
    }
    private RewardDefinition ready(RewardGuiFixture f) {
        var r=f.add("reward","mining",10,10,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.UNLOCKED);
        f.current=f.menu.createCategory(f.player,"mining");return r;
    }
    @Test void lowerInventoryAndUnsafeClicksCannotClaimOrMoveItems() {
        try(var f=new RewardGuiFixture()) {
            ready(f);var listener=new RewardListener(f.service,f.menu);
            var bottom=f.inventory(null,36);
            var e=click(f,bottom,54,ClickType.LEFT);listener.onInventoryClick(e);verify(e).setCancelled(true);
            for(var type:List.of(ClickType.SHIFT_LEFT,ClickType.SHIFT_RIGHT,ClickType.DOUBLE_CLICK,ClickType.NUMBER_KEY,
                    ClickType.SWAP_OFFHAND,ClickType.DROP,ClickType.CONTROL_DROP,ClickType.MIDDLE,ClickType.RIGHT)) {
                var blocked=click(f,f.current,19,type);listener.onInventoryClick(blocked);verify(blocked).setCancelled(true);
            }
            var outside=click(f,null,-999,ClickType.LEFT);listener.onInventoryClick(outside);verify(outside).setCancelled(true);
            f.tick();verify(f.service,never()).claimAsync(any(),any());assertTrue(f.nextTick.isEmpty());
        }
    }
    @Test void fillerAndForeignInventoryAreNotButtons() {
        try(var f=new RewardGuiFixture()) {
            ready(f);var listener=new RewardListener(f.service,f.menu);
            listener.onInventoryClick(click(f,f.current,18,ClickType.LEFT));f.tick();verify(f.service,never()).claimAsync(any(),any());
            f.current=f.inventory(null,54);var unrelated=click(f,f.current,19,ClickType.LEFT);
            listener.onInventoryClick(unrelated);verify(unrelated,never()).setCancelled(anyBoolean());
        }
    }
    @Test void dragIsCancelledOnlyForAnOwnedRewardInventory() {
        try(var f=new RewardGuiFixture()) {
            ready(f);var listener=new RewardListener(f.service,f.menu);
            var e=mock(InventoryDragEvent.class);when(e.getView()).thenReturn(f.view);listener.onInventoryDrag(e);verify(e).setCancelled(true);
            f.current=f.inventory(null,54);var other=mock(InventoryDragEvent.class);when(other.getView()).thenReturn(f.view);
            listener.onInventoryDrag(other);verify(other,never()).setCancelled(anyBoolean());
        }
    }
    @Test void claimRunsNextTickAndRapidRepeatsRemainOneInFlightOperation() {
        try(var f=new RewardGuiFixture()) {
            var reward=ready(f);var top=f.current;var future=new CompletableFuture<RewardClaimResult>();
            when(f.service.claimAsync(f.player,reward)).thenReturn(future);var listener=new RewardListener(f.service,f.menu);
            listener.onInventoryClick(click(f,top,19,ClickType.LEFT));listener.onInventoryClick(click(f,top,19,ClickType.LEFT));
            verify(f.service,never()).claimAsync(any(),any());assertEquals(1,f.nextTick.size());
            f.tick();assertTrue(f.menu.claiming(f.player.getUniqueId(),"reward"));
            assertTrue(RewardGuiFixture.lore(top.getItem(19)).contains("Claiming..."));
            listener.onInventoryClick(click(f,top,19,ClickType.LEFT));f.tick();verify(f.service,times(1)).claimAsync(f.player,reward);
            f.status("reward",RewardStatus.CLAIMED);future.complete(RewardClaimResult.SUCCESS);
            assertFalse(f.menu.claiming(f.player.getUniqueId(),"reward"));assertSame(top,f.current);
            assertTrue(RewardGuiFixture.lore(top.getItem(19)).contains("Claimed ✓"));verify(f.player,never()).openInventory(any(Inventory.class));
        }
    }
    @Test void closingBeforeScheduledClickPreventsTheClaim() {
        try(var f=new RewardGuiFixture()) {
            ready(f);var listener=new RewardListener(f.service,f.menu);listener.onInventoryClick(click(f,f.current,19,ClickType.LEFT));
            f.current=f.inventory(null,36);f.tick();verify(f.service,never()).claimAsync(any(),any());
        }
    }
    @Test void asyncCompletionDoesNotReopenOrRefreshAnotherInventory() {
        try(var f=new RewardGuiFixture()) {
            var reward=ready(f);var top=f.current;var future=new CompletableFuture<RewardClaimResult>();
            when(f.service.claimAsync(f.player,reward)).thenReturn(future);var listener=new RewardListener(f.service,f.menu);
            listener.onInventoryClick(click(f,top,19,ClickType.LEFT));f.tick();
            f.current=f.inventory(null,27);var other=f.current;future.complete(RewardClaimResult.SUCCESS);
            assertSame(other,f.current);verify(f.player,never()).openInventory(any(Inventory.class));
            verify(other,never()).clear();assertFalse(f.menu.claiming(f.player.getUniqueId(),"reward"));
        }
    }
    @Test void exceptionalAndSynchronousFailuresReleaseClaimLockAndPermitSafeRetry() {
        try(var f=new RewardGuiFixture()) {
            var reward=ready(f);var top=f.current;var failed=new CompletableFuture<RewardClaimResult>();
            when(f.service.claimAsync(f.player,reward)).thenReturn(failed).thenThrow(new IllegalStateException("provider stopping"));
            var listener=new RewardListener(f.service,f.menu);listener.onInventoryClick(click(f,top,19,ClickType.LEFT));f.tick();
            failed.completeExceptionally(new IllegalStateException("delivery failed"));
            assertFalse(f.menu.claiming(f.player.getUniqueId(),"reward"));
            assertTrue(RewardGuiFixture.lore(top.getItem(19)).contains("Delivery failed"));
            listener.onInventoryClick(click(f,top,19,ClickType.LEFT));f.tick();
            assertFalse(f.menu.claiming(f.player.getUniqueId(),"reward"));verify(f.service,times(2)).claimAsync(f.player,reward);
        }
    }
    @Test void disconnectDuringClaimStillReleasesTheMenuLock() {
        try(var f=new RewardGuiFixture()) {
            var reward=ready(f);var future=new CompletableFuture<RewardClaimResult>();
            when(f.service.claimAsync(f.player,reward)).thenReturn(future);var listener=new RewardListener(f.service,f.menu);
            listener.onInventoryClick(click(f,f.current,19,ClickType.LEFT));f.tick();
            when(f.player.isOnline()).thenReturn(false);future.complete(RewardClaimResult.SUCCESS);
            assertFalse(f.menu.claiming(f.player.getUniqueId(),"reward"));verify(f.player,never()).openInventory(any(Inventory.class));
        }
    }
    @Test void filtersAndPageNavigationNeverCallTheClaimService() {
        try(var f=new RewardGuiFixture()) {
            for(int i=0;i<43;i++)f.add("r"+i,"mining",10,0,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.LOCKED);
            f.current=f.menu.createCategory(f.player,"mining");var h=(RewardMenuHolder)f.current.getHolder();
            var listener=new RewardListener(f.service,f.menu);
            listener.onInventoryClick(click(f,f.current,51,ClickType.LEFT));f.tick();assertEquals(1,h.getPage());
            listener.onInventoryClick(click(f,f.current,15,ClickType.RIGHT));f.tick();assertEquals(RewardMenuState.Filter.CLAIMED,h.state().filter());
            assertEquals(0,h.getPage());assertTrue(h.visibleRewards().isEmpty());
            verify(f.service,never()).claimAsync(any(),any());
        }
    }
}
