package zonezero.advancements;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.NeoForge;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Both verified TaCZ 1.1.8 and 1.2.0 expose this public event. No dependency on the vendor JAR. */
final class GunKillBridge {
    private static Method headShot,attacker,killed,side,damageSource;
    private static Object nonArmor,armor;
    private static boolean enabled,warned;
    private record DeathMark(UUID player,WeakReference<DamageSource> source,long tick) {}
    private static final Map<LivingEntity,DeathMark> DEATHS=new WeakHashMap<>();
    private GunKillBridge() {}

    static void register() {
        try {
            ClassLoader loader=GunKillBridge.class.getClassLoader();
            Class<? extends Event> type=Class.forName("com.tacz.guns.api.event.common.EntityKillByGunEvent",false,loader).asSubclass(Event.class);
            Class<?> part=Class.forName("com.tacz.guns.api.event.common.GunDamageSourcePart",false,loader);
            headShot=type.getMethod("isHeadShot");
            attacker=type.getMethod("getAttacker");
            killed=type.getMethod("getKilledEntity");
            side=type.getMethod("getLogicalSide");
            damageSource=type.getMethod("getDamageSource",part);
            nonArmor=part.getField("NON_ARMOR_PIERCING").get(null);
            armor=part.getField("ARMOR_PIERCING").get(null);
            if (headShot.getReturnType()!=boolean.class || side.getReturnType()!=LogicalSide.class
                    || !LivingEntity.class.isAssignableFrom(attacker.getReturnType())
                    || !LivingEntity.class.isAssignableFrom(killed.getReturnType())
                    || damageSource.getReturnType()!=DamageSource.class)
                throw new NoSuchMethodException("TaCZ event signature changed");
            registerListener(type);
            enabled=true;
            LogUtils.getLogger().info("ZoneZero: TaCZ kill event bridge enabled.");
        } catch (ClassNotFoundException e) {
            LogUtils.getLogger().info("ZoneZero: TaCZ unavailable; headshot bridge disabled.");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LogUtils.getLogger().warn("ZoneZero: incompatible TaCZ kill event; headshot bridge disabled.",e);
        }
    }
    private static <T extends Event> void registerListener(Class<T> type) {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,type,GunKillBridge::onKill);
    }
    /** Called only by the already non-cancelled LivingDeathEvent handler. */
    static void noteDeath(LivingEntity victim,ServerPlayer player,DamageSource source) {
        if (!enabled || player.getServer()==null || !player.getServer().isSameThread() || victim==player) return;
        DEATHS.put(victim,new DeathMark(player.getUUID(),new WeakReference<>(source),player.getServer().overworld().getGameTime()));
    }
    private static void onKill(Event event) {
        if (!enabled) return;
        try {
            if (side.invoke(event)!=LogicalSide.SERVER || !Boolean.TRUE.equals(headShot.invoke(event))) return;
            if (!(attacker.invoke(event) instanceof ServerPlayer player)
                    || !(killed.invoke(event) instanceof LivingEntity victim)
                    || player==victim || player.getServer()==null || !player.getServer().isSameThread()
                    || !victim.isDeadOrDying()) return;
            DeathMark mark=DEATHS.get(victim);
            if (mark==null) return;
            DamageSource actual=mark.source().get();
            Object normal=damageSource.invoke(event,nonArmor);
            Object piercing=damageSource.invoke(event,armor);
            // Require the same final damage object, player and tick. A body shot,
            // explosion, corpse hit, denied damage or duplicate event cannot qualify.
            if (ZoneRules.confirmedHeadshot(true,true,true,mark.player().equals(player.getUUID()),
                    actual!=null && (actual==normal || actual==piercing),
                    mark.tick()==player.getServer().overworld().getGameTime())) {
                DEATHS.remove(victim);
                ZoneZeroAdvancements.recordExternal(player,"gun_headshot_kill");
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            if (!warned) {
                warned=true;
                LogUtils.getLogger().warn("ZoneZero: ignored an incompatible TaCZ kill event.",e);
            }
        }
    }
}
