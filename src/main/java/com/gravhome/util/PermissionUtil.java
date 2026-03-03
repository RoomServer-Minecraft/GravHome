package com.gravhome.util;

import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.minecraft.server.network.ServerPlayerEntity;

public class PermissionUtil {
    public static int getHomeLimit(ServerPlayerEntity player) {
        if (!FabricLoader.getInstance().isModLoaded("luckperms")) {
            return 3;
        }

        try {
            LuckPerms api = LuckPermsProvider.get();
            User user = api.getUserManager().getUser(player.getUuid());
            if (user == null) {
                return 3;
            }

            int maxLimit = 3;
            boolean hasLimitSet = false;

            for (Node node : user.resolveInheritedNodes(api.getContextManager().getQueryOptions(user))) {
                String key = node.getKey().toLowerCase();

                if (key.equals("home.limit.unlimited")) {
                    return Integer.MAX_VALUE;
                }

                if (key.startsWith("home.limit.")) {
                    try {
                        int limit = Integer.parseInt(key.substring("home.limit.".length()));
                        if (!hasLimitSet || limit > maxLimit) {
                            maxLimit = limit;
                            hasLimitSet = true;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return maxLimit;

        } catch (NoClassDefFoundError | IllegalStateException e) {
            // LuckPerms APIがロードできない場合はデフォルトにフォールバック
            return 3;
        }
    }
}
