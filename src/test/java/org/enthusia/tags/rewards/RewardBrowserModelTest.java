package org.enthusia.tags.rewards;

import java.util.*;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RewardBrowserModelTest {
    private RewardMenuModel.Entry entry(String id, String category, long goal, long value,
                                       RewardCriterionType type, RewardStatus status, boolean claimable, int index) {
        var criterion = new RewardCriterion(type, goal, null, "counter", 56, "Progress");
        var reward = new RewardDefinition(id, id, List.of(), Material.CLOCK, List.of(criterion), List.of(), category);
        var evaluation = new RewardEvaluation(status, Map.of(), status == RewardStatus.CLAIMED, claimable, "Requirements not reached");
        return new RewardMenuModel.Entry(reward, evaluation,
            List.of(new RewardMenuModel.Reading(criterion, value < 0 ? OptionalLong.empty() : OptionalLong.of(value))), index, 0);
    }
    @Test void claimsAndAvailabilityRemainDistinct() {
        var unknown = entry("x", "playtime", 60, -1, RewardCriterionType.PLAYTIME_ACTIVE_MINUTES, RewardStatus.LOCKED, false, 0);
        assertEquals(RewardMenuModel.DisplayState.UNAVAILABLE, unknown.displayState());
        assertEquals(-1, unknown.fraction());
        var ready = entry("r", "playtime", 60, 60, RewardCriterionType.PLAYTIME_ACTIVE_MINUTES, RewardStatus.UNLOCKED, true, 1);
        var claimed = entry("c", "playtime", 60, 0, RewardCriterionType.PLAYTIME_ACTIVE_MINUTES, RewardStatus.CLAIMED, false, 2);
        var list = List.of(unknown, ready, claimed);
        var summary = RewardMenuModel.summary(list);
        assertEquals(3, summary.total()); assertEquals(1, summary.ready()); assertEquals(1, summary.claimed());
        assertEquals(List.of(ready), RewardMenuModel.select(list, RewardMenuState.ready()));
        assertEquals(List.of(unknown, ready), RewardMenuModel.select(list,
            RewardMenuState.category("playtime").withFilter(RewardMenuState.Filter.UNCLAIMED)));
    }
    @Test void progressionGroupsThenOrdersTimeThresholdsWithoutStatusShuffle() {
        var longTime=entry("long", "playtime", 600, 0, RewardCriterionType.PLAYTIME_ACTIVE_MINUTES, RewardStatus.LOCKED, false, 0);
        var shortTime=entry("short", "playtime", 60, 60, RewardCriterionType.PLAYTIME_ACTIVE_MINUTES, RewardStatus.CLAIMED, false, 1);
        var total=entry("total", "playtime", 60, 1, RewardCriterionType.PLAYTIME_TOTAL_MINUTES, RewardStatus.LOCKED, false, 2);
        var underground=entry("deep", "playtime", 1800, 0, RewardCriterionType.UNDERGROUND_ACTIVE_MINUTES, RewardStatus.LOCKED, false, 3);
        assertEquals(List.of(total, shortTime, longTime, underground), RewardMenuModel.select(List.of(longTime, shortTime, underground, total), RewardMenuState.category("playtime")));
        assertEquals(List.of(shortTime, longTime), RewardMenuModel.select(List.of(longTime, total, shortTime, underground), RewardMenuState.category("playtime").withGroup(RewardMenuState.Group.ACTIVE)));
    }
    @Test void paginationClampsEmptyNegativeAndOverflowRequests() {
        var rows = new ArrayList<RewardMenuModel.Entry>();
        for(int i=0;i<43;i++) rows.add(entry("r"+i,"mining",100,i,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.LOCKED,false,i));
        assertEquals(21, RewardMenuModel.page(rows,0,21).entries().size());
        assertEquals(1, RewardMenuModel.page(rows,Integer.MAX_VALUE,21).entries().size());
        assertEquals(2, RewardMenuModel.page(rows,Integer.MAX_VALUE,21).index());
        assertEquals(0, RewardMenuModel.page(rows,-1,21).index());
        assertEquals(1, RewardMenuModel.page(List.of(),999,21).count());
        assertTrue(RewardMenuModel.page(List.of(),999,21).entries().isEmpty());
    }
    @Test void closestPlacesUnavailableAndClaimedAfterUsefulWork() {
        var unknown=entry("a","mining",100,-1,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.LOCKED,false,0);
        var near=entry("b","mining",100,90,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.LOCKED,false,1);
        var far=entry("c","mining",100,10,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.LOCKED,false,2);
        var claimed=entry("d","mining",100,100,RewardCriterionType.CUSTOM_COUNTER,RewardStatus.CLAIMED,false,3);
        assertEquals(List.of(near,far,unknown,claimed),RewardMenuModel.select(List.of(unknown,claimed,far,near),RewardMenuState.category("mining").withSort(RewardMenuState.Sort.CLOSEST)));
    }
    @Test void timeLabelsAndPercentagesDoNotInventCompletion() {
        assertEquals("0h 00m",RewardMenuText.duration(0));
        assertEquals("30h 00m",RewardMenuText.duration(1800));
        assertEquals("1h 01m",RewardMenuText.duration(61));
        assertEquals("0h 59m",RewardMenuText.duration(59));
        assertEquals(99,RewardMenuText.percent(0.999999));
        assertEquals("General",RewardMenuText.categoryName(new RewardCategory("misc","&dMisc",Material.NAME_TAG)));
    }
    @Test void readyViewCannotAccidentallyBecomeClaimAll() {
        var state=RewardMenuState.ready().withFilter(RewardMenuState.Filter.ALL);
        assertEquals(RewardMenuState.Filter.READY,state.filter());
        assertEquals(RewardMenuState.View.READY,state.view());
    }
}
