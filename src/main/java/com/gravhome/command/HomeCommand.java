package com.gravhome.command;

import com.gravhome.data.HomeData;
import com.gravhome.data.HomeState;
import com.gravhome.util.PermissionUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public class HomeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("home")
                .then(CommandManager.literal("help")
                        .executes(HomeCommand::executeHelp))
                .then(CommandManager.literal("info")
                        .executes(HomeCommand::executeInfoAll)
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .executes(HomeCommand::executeInfoSingle)))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .executes(HomeCommand::executeSet)))
                .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .executes(HomeCommand::executeDelete)))
                .then(CommandManager.argument("name", StringArgumentType.string())
                        .executes(HomeCommand::executeTeleport))
                .executes(HomeCommand::executeHelp));
    }

    private static int executeHelp(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        player.sendMessage(Text.literal("§e--- GravHome コマンド一覧 ---"));
        player.sendMessage(Text.literal("§b/home help §7- このヘルプを表示"));
        player.sendMessage(Text.literal("§b/home info §7- 登録済みホーム一覧を表示"));
        player.sendMessage(Text.literal("§b/home info <name> §7- 指定ホームの詳細情報を表示"));
        player.sendMessage(Text.literal("§b/home set <name> §7- 現在位置をホームとして登録"));
        player.sendMessage(Text.literal("§b/home <name> §7- 指定ホームへテレポート"));
        player.sendMessage(Text.literal("§b/home delete <name> §7- 指定ホームを削除"));
        return 1;
    }

    private static int executeInfoAll(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        HomeState state = HomeState.getServerState(player.getServer());
        Map<String, HomeData> homes = state.getPlayerHomes(player.getUuid());

        int limit = PermissionUtil.getHomeLimit(player);
        String limitText = limit == Integer.MAX_VALUE ? "無制限" : String.valueOf(limit);

        if (homes.isEmpty()) {
            player.sendMessage(Text.literal("§c登録されているホームはありません。 (上限: " + limitText + ")"));
            return 1;
        }

        player.sendMessage(Text.literal("§e--- 登録済みホーム一覧 (" + homes.size() + "/" + limitText + ") ---"));
        for (String name : homes.keySet()) {
            player.sendMessage(Text.literal("§7- §a" + name));
        }
        return 1;
    }

    private static int executeInfoSingle(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(ctx, "name");

        HomeState state = HomeState.getServerState(player.getServer());
        Map<String, HomeData> homes = state.getPlayerHomes(player.getUuid());
        HomeData data = homes.get(name);

        if (data == null) {
            player.sendMessage(Text.literal("§cホーム '" + name + "' は見つかりません。"));
            return 0;
        }

        player.sendMessage(Text.literal("§e--- ホーム '" + name + "' の詳細 ---"));
        player.sendMessage(Text.literal("§7ワールド: §a" + data.getDimension()));
        player.sendMessage(
                Text.literal(String.format("§7座標: §aX:%.1f, Y:%.1f, Z:%.1f", data.getX(), data.getY(), data.getZ())));
        return 1;
    }

    private static int executeSet(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(ctx, "name");

        HomeState state = HomeState.getServerState(player.getServer());
        Map<String, HomeData> homes = state.getPlayerHomes(player.getUuid());

        int limit = PermissionUtil.getHomeLimit(player);
        if (!homes.containsKey(name) && homes.size() >= limit) {
            player.sendMessage(Text.literal("§cホームの登録数が上限 (" + limit + ") に達しています。"));
            return 0;
        }

        boolean isOverwrite = homes.containsKey(name);

        HomeData data = new HomeData(
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(),
                player.getWorld().getRegistryKey().getValue().toString());

        state.addHome(player.getUuid(), name, data);

        if (isOverwrite) {
            player.sendMessage(Text.literal("§aホーム '" + name + "' を現在地で上書きしました。"));
        } else {
            player.sendMessage(Text.literal("§aホーム '" + name + "' を現在地で登録しました。"));
        }
        return 1;
    }

    private static int executeDelete(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(ctx, "name");

        HomeState state = HomeState.getServerState(player.getServer());
        if (state.removeHome(player.getUuid(), name)) {
            player.sendMessage(Text.literal("§aホーム '" + name + "' を削除しました。"));
            return 1;
        } else {
            player.sendMessage(Text.literal("§cホーム '" + name + "' は見つかりません。"));
            return 0;
        }
    }

    private static int executeTeleport(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(ctx, "name");

        HomeState state = HomeState.getServerState(player.getServer());
        Map<String, HomeData> homes = state.getPlayerHomes(player.getUuid());
        HomeData data = homes.get(name);

        if (data == null) {
            player.sendMessage(Text.literal("§cホーム '" + name + "' は見つかりません。"));
            return 0;
        }

        Identifier dimId = Identifier.of(data.getDimension());
        RegistryKey<net.minecraft.world.World> registryKey = RegistryKey.of(RegistryKeys.WORLD, dimId);
        ServerWorld targetWorld = player.getServer().getWorld(registryKey);

        if (targetWorld == null) {
            player.sendMessage(Text.literal("§c対象のワールドが存在しません。ホームの削除(/home delete " + name + ")をおすすめします。"));
            return 0;
        }

        if (!isSafe(targetWorld, data.getX(), data.getY(), data.getZ())) {
            player.sendMessage(Text.literal("§cテレポート先が安全ではありません。(ブロックに埋まっているか、危険な場所です)"));
            return 0;
        }

        player.teleport(targetWorld, data.getX(), data.getY(), data.getZ(), data.getYaw(), data.getPitch());
        player.sendMessage(Text.literal("§aホーム '" + name + "' にテレポートしました。"));
        return 1;
    }

    private static boolean isSafe(ServerWorld world, double x, double y, double z) {
        if (y < world.getBottomY() || y > world.getTopY()) {
            return false;
        }

        BlockPos feet = BlockPos.ofFloored(x, y, z);
        BlockPos head = BlockPos.ofFloored(x, y + 1, z);

        boolean feetClear = world.getBlockState(feet).getCollisionShape(world, feet).isEmpty();
        boolean headClear = world.getBlockState(head).getCollisionShape(world, head).isEmpty();

        if (!feetClear || !headClear) {
            return false;
        }

        if (world.getBlockState(feet).getFluidState().isOf(net.minecraft.fluid.Fluids.LAVA)) {
            return false;
        }
        if (world.getBlockState(head).getFluidState().isOf(net.minecraft.fluid.Fluids.LAVA)) {
            return false;
        }

        return true;
    }
}
