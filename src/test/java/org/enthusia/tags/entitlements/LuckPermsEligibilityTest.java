package org.enthusia.tags.entitlements;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LuckPermsEligibilityTest {
    @Test void resolvedContextMembershipExcludesRawNodesAndPrimaryLabel() throws Exception {
        var id = UUID.randomUUID();
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        var plugin = mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        var manager = mock(PluginManager.class);
        var services = mock(ServicesManager.class);
        var lpPlugin = mock(Plugin.class);
        when(manager.getPlugin("LuckPerms")).thenReturn(lpPlugin);
        when(lpPlugin.isEnabled()).thenReturn(true);
        var api = mock(LuckPerms.class);
        var users = mock(UserManager.class);
        var groups = mock(GroupManager.class);
        var user = mock(User.class);
        var options = mock(QueryOptions.class);
        var current = mock(Group.class);
        var excluded = mock(Node.class);
        when(excluded.getKey()).thenReturn("group.devotee");
        when(excluded.getValue()).thenReturn(false);
        when(user.getNodes()).thenReturn(Set.of(excluded));
        when(user.getPrimaryGroup()).thenReturn("devotee");
        when(user.getQueryOptions()).thenReturn(options);
        when(user.getInheritedGroups(options)).thenReturn(Set.of(current));
        when(current.getName()).thenReturn("avid");
        when(api.getUserManager()).thenReturn(users);
        when(api.getGroupManager()).thenReturn(groups);
        when(users.getUser(id)).thenReturn(user);
        when(services.load(LuckPerms.class)).thenReturn(api);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            bukkit.when(Bukkit::getServicesManager).thenReturn(services);
            var service = new EntitlementService(plugin, null, null, null);
            var lookup = EntitlementService.class.getDeclaredMethod("luckPermsGroups", Player.class);
            lookup.setAccessible(true);
            assertEquals(Set.of("avid"), lookup.invoke(service, player));
            verify(user).auditTemporaryNodes();
            verify(user).getInheritedGroups(options);
        }
    }
}
