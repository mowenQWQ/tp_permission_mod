package cn.autoforged.tp_permission_mod_1785686385.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * 传送指令权限管理的运行时配置。
 * 默认不拦截任何玩家（enabled=true）；blockMode 默认 targeted，仅拦截 blockedPlayers 中的玩家；
 * global 模式（保留为附加功能）拦截除 exemptPlayers 外的所有玩家。
 * 配置以 SERVER 类型注册，持久化到每个世界的 serverconfig/tp_permission_mod_1785686385-server.toml，
 * 因此每个世界拥有独立的配置文件；单机(集成服务端)同样加载该文件，客户端侧也可用。运行时通过指令修改并落盘。
 */
public class TpPermissionConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXEMPT_PLAYERS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCKED_PLAYERS;
    private static final ForgeConfigSpec.ConfigValue<String> BLOCK_MODE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ENABLED = builder
            .comment(
                "Whether the /tp (and /teleport) command is enabled for players.",
                "When false, the block mode determines who is blocked.",
                "Toggled at runtime via /tpperm on | /tpperm off"
            )
            .define("enabled", true);
        BLOCK_MODE = builder
            .comment(
                "Block mode: 'targeted' (default) only blocks players in blockedPlayers.",
                "'global' blocks everyone except those in exemptPlayers."
            )
            .define("blockMode", "targeted",
                v -> v instanceof String s && ("targeted".equals(s) || "global".equals(s)));
        BLOCKED_PLAYERS = builder
            .comment(
                "Player names specifically blocked from using /tp when blockMode is 'targeted'.",
                "Managed at runtime via /tpperm block add <player> | remove <player> | list"
            )
            .defineListAllowEmpty("blockedPlayers", List.of(), obj -> obj instanceof String);
        EXEMPT_PLAYERS = builder
            .comment(
                "Player names exempt from the disabled restriction.",
                "Used in global mode and as universal override in targeted mode.",
                "Managed at runtime via /tpperm exempt add <player> | remove <player> | list"
            )
            .defineListAllowEmpty("exemptPlayers", List.of(), obj -> obj instanceof String);
        SPEC = builder.build();
    }

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    public static void setEnabled(boolean value) {
        ENABLED.set(value);
        SPEC.save();
    }

    public static boolean isGlobalMode() {
        return "global".equals(BLOCK_MODE.get());
    }

    public static boolean isTargetedMode() {
        return "targeted".equals(BLOCK_MODE.get());
    }

    public static String getBlockMode() {
        return BLOCK_MODE.get();
    }

    public static void setBlockMode(String mode) {
        BLOCK_MODE.set(mode);
        SPEC.save();
    }

    /**
     * Whether /tp is currently blocked for the given player.
     * Unified rule: enabled=false → check mode → exempt always overrides.
     */
    public static boolean isTpBlockedFor(String playerName) {
        if (isEnabled()) {
            return false;
        }
        if (isExempt(playerName)) {
            return false;
        }
        if (isGlobalMode()) {
            return true;
        }
        return isBlocked(playerName);
    }

    // --- exempt ---

    public static boolean isExempt(String playerName) {
        return EXEMPT_PLAYERS.get().contains(playerName);
    }

    public static List<String> getExemptPlayers() {
        return List.copyOf(EXEMPT_PLAYERS.get());
    }

    /** @return true if added; false if already present */
    public static boolean addExempt(String playerName) {
        List<String> current = new ArrayList<>(EXEMPT_PLAYERS.get());
        if (current.contains(playerName)) {
            return false;
        }
        current.add(playerName);
        EXEMPT_PLAYERS.set(current);
        SPEC.save();
        return true;
    }

    /** @return true if removed; false if not present */
    public static boolean removeExempt(String playerName) {
        List<String> current = new ArrayList<>(EXEMPT_PLAYERS.get());
        if (current.remove(playerName)) {
            EXEMPT_PLAYERS.set(current);
            SPEC.save();
            return true;
        }
        return false;
    }

    // --- blocked ---

    public static boolean isBlocked(String playerName) {
        return BLOCKED_PLAYERS.get().contains(playerName);
    }

    public static List<String> getBlockedPlayers() {
        return List.copyOf(BLOCKED_PLAYERS.get());
    }

    /** @return true if added; false if already present */
    public static boolean addBlocked(String playerName) {
        List<String> current = new ArrayList<>(BLOCKED_PLAYERS.get());
        if (current.contains(playerName)) {
            return false;
        }
        current.add(playerName);
        BLOCKED_PLAYERS.set(current);
        SPEC.save();
        return true;
    }

    /** @return true if removed; false if not present */
    public static boolean removeBlocked(String playerName) {
        List<String> current = new ArrayList<>(BLOCKED_PLAYERS.get());
        if (current.remove(playerName)) {
            BLOCKED_PLAYERS.set(current);
            SPEC.save();
            return true;
        }
        return false;
    }
}
