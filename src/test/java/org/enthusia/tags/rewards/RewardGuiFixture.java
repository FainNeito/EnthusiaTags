package org.enthusia.tags.rewards;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.enthusia.tags.*;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;

/** Server API fixture: exercise actual renderer code without starting or changing a Minecraft server. */
final class RewardGuiFixture implements AutoCloseable {
    final RewardService service = mock(RewardService.class);
    final TagService tags = mock(TagService.class);
    final EnthusiaTagsPlugin plugin = mock(EnthusiaTagsPlugin.class);
    final Player player = mock(Player.class);
    final InventoryView view = mock(InventoryView.class);
    final BukkitScheduler scheduler = mock(BukkitScheduler.class);
    final Queue<Runnable> nextTick = new ArrayDeque<>();
    final List<Runnable> refreshTasks = new ArrayList<>();
    final Map<String,RewardDefinition> definitions = new LinkedHashMap<>();
    final Map<String,RewardEvaluation> evaluations = new HashMap<>();
    final Map<RewardCriterion,OptionalLong> values = new IdentityHashMap<>();
    final Map<String,RewardCategory> categories = new LinkedHashMap<>();
    final Map<ItemStack,ItemMeta> metas = new IdentityHashMap<>();
    final MockedStatic<Bukkit> bukkit;
    final MockedConstruction<ItemStack> stacks;
    final RewardMenu menu;
    Inventory current;
    RewardGuiFixture() {
        when(plugin.getName()).thenReturn("EnthusiaTags");
        when(plugin.namespace()).thenReturn("enthusiatags");
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getPerformanceMonitor()).thenReturn(mock(PerformanceMonitor.class));
        when(tags.getPlugin()).thenReturn(plugin);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.getOpenInventory()).thenReturn(view);
        when(view.getTopInventory()).thenAnswer(i -> current);
        when(player.openInventory(any(Inventory.class))).thenAnswer(i -> { current=i.getArgument(0); return view; });
        when(service.getRewards()).thenReturn(definitions);
        when(service.getConfig()).thenReturn(new RewardsConfig("", "", "", "",56,false,"",categories));
        when(service.getMessage(anyString())).thenAnswer(i -> i.getArgument(0));
        when(service.getProgressSnapshot(player)).thenReturn(new RewardService.ProgressSnapshot(0,new HashMap<>()));
        when(service.evaluate(eq(player),any(RewardDefinition.class),any())).thenAnswer(i -> evaluations.get(((RewardDefinition)i.getArgument(1)).getId()));
        when(service.getVerifiedMenuProgress(eq(player),any(RewardCriterion.class),any())).thenAnswer(i -> values.getOrDefault(i.getArgument(1),OptionalLong.empty()));
        doAnswer(i -> { if(player.isOnline()) ((Consumer<Player>)i.getArgument(1)).accept(player); return null; })
            .when(service).runForOnlinePlayer(any(UUID.class),any());
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player));
        bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class),anyInt(),any(Component.class))).thenAnswer(i -> inventory(i.getArgument(0),i.getArgument(1)));
        when(scheduler.runTask(eq(plugin),any(Runnable.class))).thenAnswer(i -> { nextTick.add(i.getArgument(1)); return mock(BukkitTask.class); });
        when(scheduler.runTaskTimer(eq(plugin),any(Runnable.class),anyLong(),anyLong())).thenAnswer(i -> { refreshTasks.add(i.getArgument(1)); return mock(BukkitTask.class); });
        stacks = mockConstruction(ItemStack.class,(stack,context) -> {
            var meta=mock(ItemMeta.class); var name=new AtomicReference<Component>(); var lore=new AtomicReference<List<Component>>(List.of());
            var glint=new AtomicReference<Boolean>(false);
            when(stack.getItemMeta()).thenReturn(meta); when(stack.hasItemMeta()).thenReturn(true);
            when(stack.getType()).thenReturn((Material)context.arguments().getFirst());
            when(meta.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
            doAnswer(i -> {name.set(i.getArgument(0));return null;}).when(meta).displayName(any(Component.class));
            when(meta.displayName()).thenAnswer(i -> name.get());
            doAnswer(i -> {lore.set(i.getArgument(0));return null;}).when(meta).lore(anyList());
            when(meta.lore()).thenAnswer(i -> lore.get());
            doAnswer(i -> {glint.set(i.getArgument(0));return null;}).when(meta).setEnchantmentGlintOverride(anyBoolean());
            when(meta.getEnchantmentGlintOverride()).thenAnswer(i -> glint.get());
            metas.put(stack,meta);
        });
        try { menu = new RewardMenu(service,tags); }
        catch (RuntimeException | Error failure) { stacks.close(); bukkit.close(); throw failure; }
    }
    Inventory inventory(InventoryHolder holder,int size) {
        var inventory=mock(Inventory.class); var contents=new ItemStack[size];
        when(inventory.getHolder()).thenReturn(holder);when(inventory.getSize()).thenReturn(size);
        when(inventory.getItem(anyInt())).thenAnswer(i -> contents[(int)i.getArgument(0)]);
        when(inventory.getContents()).thenAnswer(i -> contents.clone());
        doAnswer(i -> {contents[(int)i.getArgument(0)]=i.getArgument(1);return null;}).when(inventory).setItem(anyInt(),any());
        doAnswer(i -> {Arrays.fill(contents,null);return null;}).when(inventory).clear();
        return inventory;
    }
    RewardDefinition add(String id,String category,long goal,long currentValue,RewardCriterionType type,RewardStatus status) {
        categories.putIfAbsent(category,new RewardCategory(category,RewardMenuText.titleCase(category),Material.CLOCK));
        var criterion=new RewardCriterion(type,goal,null,"test",56,"Progress");
        var reward=new RewardDefinition(id,id,List.of("Complete the challenge."),Material.CLOCK,List.of(criterion),
            List.of(new RewardAction(RewardActionType.MONEY,"",500,"")),category);
        definitions.put(id,reward); values.put(criterion,currentValue<0?OptionalLong.empty():OptionalLong.of(currentValue));
        status(id,status);return reward;
    }
    void status(String id,RewardStatus status) {
        evaluations.put(id,new RewardEvaluation(status,Map.of(),status==RewardStatus.CLAIMED,
            status==RewardStatus.UNLOCKED || status==RewardStatus.DELIVERY_FAILED,"Requirements not reached"));
    }
    void tick() { while(!nextTick.isEmpty()) nextTick.remove().run(); }
    static String plain(Component component) { return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component); }
    static String lore(ItemStack item) { return String.join("\n",item.getItemMeta().lore().stream().map(RewardGuiFixture::plain).toList()); }
    @Override public void close() { stacks.close();bukkit.close(); }
}
