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
    private static final String LUCKPERMS_API_CLASS = "net.luckperms.api.LuckPerms";

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
            return statusFromLoadedUser(uuid, permission);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            plugin.getLogger().warning("Portable LuckPerms entitlement verification failed: "
                + exception.getMessage());
            return Status.UNKNOWN;
        }
    }

    private Status statusFromLoadedUser(UUID uuid, String permission) throws ReflectiveOperationException {
        Object api = api();
        Object userManager = call(api, "getUserManager");
        Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, uuid);
        if (user == null) {
            return Status.UNKNOWN;
        }
        Collection<?> nodes = nodes(user);
        return ownsExactGlobalNode(nodes, permission) ? Status.PRESENT : Status.ABSENT;
    }

    private Collection<?> nodes(Object user) throws ReflectiveOperationException {
        Object nodesValue = call(user, "getNodes");
        if (nodesValue instanceof Collection<?> nodes) {
            return nodes;
        }
        throw new ReflectiveOperationException("LuckPerms getNodes returned unexpected type");
    }

    private boolean ownsExactGlobalNode(Collection<?> nodes, String permission)
            throws ReflectiveOperationException {
        for (Object node : nodes) {
            if (isExactGlobalPositiveNode(node, permission)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExactGlobalPositiveNode(Object node, String permission)
            throws ReflectiveOperationException {
        Object key = call(node, "getKey");
        Object value = call(node, "getValue");
        Object contexts = call(node, "getContexts");
        Object empty = call(contexts, "isEmpty");
        return permission.equalsIgnoreCase(String.valueOf(key))
            && Boolean.TRUE.equals(value)
            && Boolean.TRUE.equals(empty);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object api() throws ReflectiveOperationException {
        for (Class<?> serviceClass : Bukkit.getServicesManager().getKnownServices()) {
            if (!LUCKPERMS_API_CLASS.equals(serviceClass.getName())) {
                continue;
            }
            RegisteredServiceProvider registration =
                Bukkit.getServicesManager().getRegistration((Class) serviceClass);
            if (registration != null && registration.getProvider() != null) {
                return registration.getProvider();
            }
        }
        throw new ReflectiveOperationException("LuckPerms service unavailable");
    }

    private Object call(Object target, String method) throws ReflectiveOperationException {
        return target.getClass().getMethod(method).invoke(target);
    }
}
