package zonezero.advancements;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.lang.reflect.Method;
import java.util.Optional;

/** Read-only adapter for the exact API exposed by Puffish Skills 0.19.2.
 * Reflection keeps the server mod independent of any redistributed vendor JAR.
 */
final class SkillsBridge {
    private static Method lookup, spent;
    private static boolean initialized, unavailable, warned;
    static Integer spent(ServerPlayer player, String namespace, String path) {
        if (unavailable) return null;
        try {
            if (!initialized) {
                Class<?> api=Class.forName("net.puffish.skillsmod.api.SkillsAPI");
                Class<?> category=Class.forName("net.puffish.skillsmod.api.Category");
                lookup=api.getMethod("getCategory",ResourceLocation.class);
                spent=category.getMethod("getSpentPoints",ServerPlayer.class);
                initialized=true;
            }
            Optional<?> category=(Optional<?>)lookup.invoke(null,ResourceLocation.fromNamespaceAndPath(namespace,path));
            if (category.isEmpty()) return null;
            return (Integer)spent.invoke(category.get(),player);
        } catch (ClassNotFoundException e) {
            unavailable=true;
            LogUtils.getLogger().warn("ZoneZero: Puffish Skills is not installed; skill milestones disabled.");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            if (!warned) {
                warned=true;
                LogUtils.getLogger().warn("ZoneZero: unable to read Puffish Skills points. No skill milestones will be guessed.",e);
            }
        }
        return null;
    }
}
