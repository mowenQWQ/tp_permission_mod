package cn.autoforged.tp_permission_mod_1785686385;

import cn.autoforged.tp_permission_mod_1785686385.config.TpPermissionConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(TpPermissionMod.MOD_ID)
public class TpPermissionMod {

    public static final String MOD_ID = "tp_permission_mod_1785686385";

    public TpPermissionMod() {
        // SERVER 类型配置存放在每个世界的 serverconfig 目录下，实现"每个世界单独配置文件"；
        // 在单机(集成服务端)与专用服务端上都会随世界加载，因此客户端侧同样可用。
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, TpPermissionConfig.SPEC);
    }
}
