package org.enthusia.tags;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Collection;
import java.util.UUID;

/**
 * Class-loader-safe, read-only LuckPerms adapter for portable tag ownership.
 * Only an exact, positive, context-free user node is accepted. Effective
 * wildcard/inherited permissions are intentionally ignored.
 */
final class LuckPermsPortableEntitlementGateway {
    enum Status {
        PRESENT,
        ABSENT,
        UNKNOWN
    }

    private final EnthusiaTagsPlugin plugin;

    LuckPermsPortableEntitlementGateway(EnthusiaTagsPlugin plugin) {
        this.plugin = plugin;
    }

    Status status(UUID uuid, String permission) {
        Plugin luckPerms = plugin.getServer().getPluginManager().getPlugin("LuckPerms");
        if (luckPerms == null || !luckPerms.isEnabled()) {
            return Status.UNKNOWN;
        }
        try {
            Object api = api(luckPerms);
            Object userManager = call(api, "getUserManager");
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, uuid);
            if (user == null) {
                return Status.UNKNOWN;
            }
            Object nodesValue = call(user, "getNodes");
            if (!(nodesValue instanceof Collection<?> nodes)) {
                return Status.UNKNOWN;
            }
            for (Object node : nodes) {
                Object key = call(node, "getKey");
                Object value = call(node, "getValue");
                Object contexts = call(node, "getContexts");
                Object empty = call(contexts, "isEmpty");
                if (permission.equalsIgnoreCase(String.valueOf(key))
                    && Boolean.TRUE.equals(value)
                    && Boolean.TRUE.equals(empty)) {
                    return Status.PRESENT;
                }
            }
            return Status.ABSENT;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            plugin.getLogger().warning("Portable LuckPerms entitlement verification failed: "
                + exception.getMessage());
            return Status.UNKNOWN;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object api(Plugin luckPerms) throws ReflectiveOperationException {
        Class<?> apiClass = Class.forName(
            "net.luckperms.api.LuckPerms", true, luckPerms.getClass().getClassLoader());
        RegisteredServiceProvider registration = Bukkit.getServicesManager().getRegistration((Class) apiClass);
        if (registration == null || registration.getProvider() == null) {
            throw new ReflectiveOperationException("LuckPerms service unavailable");
        }
        return registration.getProvider();
    }

    private Object call(Object target, String method) throws ReflectiveOperationException {
        return target.getClass().getMethod(method).invoke(target);
    }
}
