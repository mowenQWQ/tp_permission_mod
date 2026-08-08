package cn.autoforged.tp_permission_mod_1785686385.command;

import cn.autoforged.tp_permission_mod_1785686385.config.TpPermissionConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * 控制指令 /tpperm（仅 OP 可执行，对应 hasPermission(2)）：
 *   /tpperm on                       开启 tp
 *   /tpperm off                      关闭 tp
 *   /tpperm status                   查看当前状态、模式与名单
 *   /tpperm mode [targeted|global]   查看或切换拦截模式
 *   /tpperm exempt add <玩家>         手动添加豁免玩家
 *   /tpperm exempt remove <玩家>      移除豁免玩家
 *   /tpperm exempt list              列出豁免玩家
 *   /tpperm block add <玩家>          手动添加被拦截玩家
 *   /tpperm block remove <玩家>       移除被拦截玩家
 *   /tpperm block list               列出被拦截玩家
 */
public class TpPermissionCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("tpperm")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("on").executes(ctx -> setEnabled(ctx.getSource(), true)))
                .then(Commands.literal("off").executes(ctx -> setEnabled(ctx.getSource(), false)))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(
                    Commands.literal("mode")
                        .executes(ctx -> getMode(ctx.getSource()))
                        .then(Commands.literal("targeted").executes(ctx -> setMode(ctx.getSource(), "targeted")))
                        .then(Commands.literal("global").executes(ctx -> setMode(ctx.getSource(), "global")))
                )
                .then(
                    Commands.literal("exempt")
                        .then(
                            Commands.literal("add")
                                .then(
                                    Commands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> addExempt(ctx.getSource(), StringArgumentType.getString(ctx, "player")))
                                )
                        )
                        .then(
                            Commands.literal("remove")
                                .then(
                                    Commands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> removeExempt(ctx.getSource(), StringArgumentType.getString(ctx, "player")))
                                )
                        )
                        .then(Commands.literal("list").executes(ctx -> listExempt(ctx.getSource())))
                )
                .then(
                    Commands.literal("block")
                        .then(
                            Commands.literal("add")
                                .then(
                                    Commands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> addBlocked(ctx.getSource(), StringArgumentType.getString(ctx, "player")))
                                )
                        )
                        .then(
                            Commands.literal("remove")
                                .then(
                                    Commands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> removeBlocked(ctx.getSource(), StringArgumentType.getString(ctx, "player")))
                                )
                        )
                        .then(Commands.literal("list").executes(ctx -> listBlocked(ctx.getSource())))
                )
        );
    }

    private static int setEnabled(CommandSourceStack source, boolean value) {
        if (isBanned(source)) {
            source.sendFailure(Component.translatable("tp_permission_mod.banned.self"));
            return 0;
        }
        TpPermissionConfig.setEnabled(value);
        source.sendSuccess(
            () -> Component.translatable(value ? "tp_permission_mod.enabled" : "tp_permission_mod.disabled"),
            true
        );
        return 1;
    }

    private static int status(CommandSourceStack source) {
        boolean enabled = TpPermissionConfig.isEnabled();
        String mode = TpPermissionConfig.getBlockMode();
        List<String> blocked = TpPermissionConfig.getBlockedPlayers();
        List<String> exempt = TpPermissionConfig.getExemptPlayers();
        Component state = Component.translatable(enabled ? "tp_permission_mod.status.enabled" : "tp_permission_mod.status.disabled")
            .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
        Component modeComp = Component.literal(mode).withStyle(ChatFormatting.YELLOW);
        Component blockedList = blocked.isEmpty()
            ? Component.translatable("tp_permission_mod.exempt.empty")
            : Component.literal(String.join(", ", blocked)).withStyle(ChatFormatting.AQUA);
        Component exemptList = exempt.isEmpty()
            ? Component.translatable("tp_permission_mod.exempt.empty")
            : Component.literal(String.join(", ", exempt)).withStyle(ChatFormatting.AQUA);
        source.sendSuccess(
            () -> Component.translatable("tp_permission_mod.status", state, modeComp, blockedList, exemptList),
            false
        );
        return 1;
    }

    private static int getMode(CommandSourceStack source) {
        String mode = TpPermissionConfig.getBlockMode();
        source.sendSuccess(
            () -> Component.translatable("tp_permission_mod.mode.get", Component.literal(mode).withStyle(ChatFormatting.YELLOW)),
            false
        );
        return 1;
    }

    private static int setMode(CommandSourceStack source, String mode) {
        if (isBanned(source)) {
            source.sendFailure(Component.translatable("tp_permission_mod.banned.self"));
            return 0;
        }
        TpPermissionConfig.setBlockMode(mode);
        source.sendSuccess(
            () -> Component.translatable("tp_permission_mod.mode.set", Component.literal(mode).withStyle(ChatFormatting.YELLOW)),
            true
        );
        return 1;
    }

    // --- exempt ---

    private static int addExempt(CommandSourceStack source, String player) {
        if (isBanned(source) && isSelf(source, player)) {
            source.sendFailure(Component.translatable("tp_permission_mod.banned.self"));
            return 0;
        }
        if (TpPermissionConfig.addExempt(player)) {
            source.sendSuccess(() -> Component.translatable("tp_permission_mod.exempt.added", player), true);
        } else {
            source.sendFailure(Component.translatable("tp_permission_mod.exempt.already", player));
        }
        return 1;
    }

    private static int removeExempt(CommandSourceStack source, String player) {
        if (isBanned(source) && isSelf(source, player)) {
            source.sendFailure(Component.translatable("tp_permission_mod.banned.self"));
            return 0;
        }
        if (TpPermissionConfig.removeExempt(player)) {
            source.sendSuccess(() -> Component.translatable("tp_permission_mod.exempt.removed", player), true);
        } else {
            source.sendFailure(Component.translatable("tp_permission_mod.exempt.notfound", player));
        }
        return 1;
    }

    private static int listExempt(CommandSourceStack source) {
        List<String> exempt = TpPermissionConfig.getExemptPlayers();
        if (exempt.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("tp_permission_mod.exempt.none"), false);
        } else {
            source.sendSuccess(
                () -> Component.translatable("tp_permission_mod.exempt.list", String.join(", ", exempt)),
                false
            );
        }
        return 1;
    }

    // --- blocked ---

    private static int addBlocked(CommandSourceStack source, String player) {
        if (isBanned(source) && isSelf(source, player)) {
            source.sendFailure(Component.translatable("tp_permission_mod.banned.self"));
            return 0;
        }
        if (TpPermissionConfig.addBlocked(player)) {
            source.sendSuccess(() -> Component.translatable("tp_permission_mod.block.added", player), true);
        } else {
            source.sendFailure(Component.translatable("tp_permission_mod.block.already", player));
        }
        return 1;
    }

    private static int removeBlocked(CommandSourceStack source, String player) {
        if (isBanned(source) && isSelf(source, player)) {
            source.sendFailure(Component.translatable("tp_permission_mod.banned.self"));
            return 0;
        }
        if (TpPermissionConfig.removeBlocked(player)) {
            source.sendSuccess(() -> Component.translatable("tp_permission_mod.block.removed", player), true);
        } else {
            source.sendFailure(Component.translatable("tp_permission_mod.block.notfound", player));
        }
        return 1;
    }

    private static int listBlocked(CommandSourceStack source) {
        List<String> blocked = TpPermissionConfig.getBlockedPlayers();
        if (blocked.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("tp_permission_mod.block.none"), false);
        } else {
            source.sendSuccess(
                () -> Component.translatable("tp_permission_mod.block.list", String.join(", ", blocked)),
                false
            );
        }
        return 1;
    }

    /**
     * 判断执行者是否正处"tp 被拦截"的受限状态。
     * 委托 {@link TpPermissionConfig#isTpBlockedFor} 统一判定；
     * 受限玩家不能修改自己的 tp 状态（on/off/给自己的 exempt/给自己的 block/切换 mode）。
     * 非玩家来源（控制台等）不受此限制。
     */
    private static boolean isBanned(CommandSourceStack source) {
        ServerPlayer playerEntity = source.getPlayer();
        if (playerEntity == null) {
            return false;
        }
        return TpPermissionConfig.isTpBlockedFor(playerEntity.getGameProfile().getName());
    }

    /**
     * 判断执行者是否就是目标玩家本人。
     */
    private static boolean isSelf(CommandSourceStack source, String player) {
        ServerPlayer playerEntity = source.getPlayer();
        return playerEntity != null && playerEntity.getGameProfile().getName().equals(player);
    }
}
