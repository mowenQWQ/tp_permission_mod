package cn.autoforged.tp_permission_mod_1785686385.event;

import cn.autoforged.tp_permission_mod_1785686385.TpPermissionMod;
import cn.autoforged.tp_permission_mod_1785686385.command.TpPermissionCommand;
import cn.autoforged.tp_permission_mod_1785686385.config.TpPermissionConfig;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * FORGE bus 事件监听器。
 * 拦截逻辑由 {@link TpPermissionConfig#isTpBlockedFor} 统一判定；
 * targeted 模式仅拦截 blockedPlayers 中的玩家，global 模式拦截除 exemptPlayers 外的所有玩家。
 * 非玩家来源（控制台 / 命令方块 / 函数）不受限制。
 */
@Mod.EventBusSubscriber(modid = TpPermissionMod.MOD_ID)
public class TpCommandEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        TpPermissionCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        ParseResults<CommandSourceStack> parseResults = event.getParseResults();
        if (parseResults == null || parseResults.getContext() == null) {
            return;
        }

        List<ParsedCommandNode<CommandSourceStack>> nodes = parseResults.getContext().getNodes();
        if (nodes.isEmpty()) {
            return;
        }

        String commandName = nodes.get(0).getNode().getName();
        if (!commandName.equals("tp") && !commandName.equals("teleport")) {
            return;
        }

        CommandSourceStack source = parseResults.getContext().getSource();
        if (source.getPlayer() == null) {
            return;
        }

        if (!source.hasPermission(2)) {
            event.setCanceled(true);
            source.sendFailure(Component.translatable("tp_permission_mod.tp_no_permission"));
            return;
        }

        String playerName = source.getPlayer().getGameProfile().getName();
        if (!TpPermissionConfig.isTpBlockedFor(playerName)) {
            return;
        }

        event.setCanceled(true);
        source.sendFailure(Component.translatable("tp_permission_mod.tp_disabled"));
    }
}
