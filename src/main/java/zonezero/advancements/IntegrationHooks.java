package zonezero.advancements;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import java.util.UUID;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Optional, observational integration. Never modifies other mods' return values or gameplay. */
public final class IntegrationHooks {
    private static final String VINCA="meds_and_herbs:medicine_effects/vinca";
    private static final String BLEEDING="meds_and_herbs:internal_bleeding";
    private static final String THROMBOSIS="meds_and_herbs:thrombosis";
    // Separate from the mixin's class initializer, with a stack for nested command functions.
    private record Before(LivingEntity entity,ResourceLocation function,int duration,boolean eligible) {}
    private static final ThreadLocal<java.util.ArrayDeque<Before>> MEDICAL=new ThreadLocal<>();
    private static final Set<LivingDeathEvent> DOWNED_DEATH=Collections.newSetFromMap(new WeakHashMap<>());
    private record Rescue(UUID target,UUID rescuer) {}
    private static final ThreadLocal<Rescue> RESCUE=new ThreadLocal<>();
    private IntegrationHooks() {}
    public static void markDownedDeath(LivingDeathEvent event,ServerPlayer player) {
        if (player.getServer()!=null && player.getServer().isSameThread()) DOWNED_DEATH.add(event);
    }
    public static boolean consumeDownedDeath(LivingDeathEvent event) {
        return DOWNED_DEATH.remove(event);
    }
    public static void clearRevive() { RESCUE.remove(); }
    public static void startRevive(ServerPlayer target,UUID rescuer) {
        clearRevive();
        if (rescuer==null || rescuer.equals(target.getUUID()) || target.getServer()==null || !target.getServer().isSameThread()) return;
        RESCUE.set(new Rescue(target.getUUID(),rescuer));
    }
    public static void finishRevive(ServerPlayer target,boolean noLongerDowned) {
        Rescue rescue=RESCUE.get();
        clearRevive();
        if (rescue==null || !rescue.target().equals(target.getUUID()) || target.getServer()==null
                || !target.getServer().isSameThread()) return;
        ServerPlayer rescuer=target.getServer().getPlayerList().getPlayer(rescue.rescuer());
        if (!ZoneRules.confirmedRevive(true,!noLongerDowned,target.isAlive(),rescuer!=null && rescuer!=target)) return;
        ZoneZeroAdvancements.recordExternal(target,"revived");
        ZoneZeroAdvancements.recordExternal(rescuer,"revive_other");
    }
    public static void shelterCreated(ServerPlayer p,boolean success) {
        if (success) ZoneZeroAdvancements.recordExternal(p,"shelter_create");
    }
    public static void shelterTeleported(ServerPlayer p) {
        ZoneZeroAdvancements.recordExternal(p,"shelter_teleport");
        if (p.getHealth()>0.0F && p.getHealth()<2.0F)
            ZoneZeroAdvancements.recordExternal(p,"low_health_teleport");
    }
    private static int effectDuration(LivingEntity entity,String id) {
        for (var effect:entity.getActiveEffects()) {
            if (effect.getEffect().unwrapKey().map(k->k.location().toString().equals(id)).orElse(false))
                return effect.getDuration();
        }
        return 0; // missing effect, not infinity (-1)
    }
    public static void medicineStart(LivingEntity entity,ResourceLocation function) {
        if (!(entity instanceof ServerPlayer p) || p.getServer()==null || !p.getServer().isSameThread()) return;
        var stack=MEDICAL.get();
        if (stack==null) { stack=new java.util.ArrayDeque<>(); MEDICAL.set(stack); }
        boolean eligible=VINCA.equals(function.toString()) && effectDuration(entity,BLEEDING)==0;
        stack.push(new Before(entity,function,eligible?effectDuration(entity,THROMBOSIS):0,eligible));
    }
    public static void medicineFinish(LivingEntity entity,ResourceLocation function) {
        if (!(entity instanceof ServerPlayer p) || p.getServer()==null || !p.getServer().isSameThread()) return;
        var stack=MEDICAL.get();
        if (stack==null || stack.isEmpty()) return;
        Before before=stack.pop();
        if (stack.isEmpty()) MEDICAL.remove();
        if (before.entity()!=entity || !before.function().equals(function)) { MEDICAL.remove(); return; }
        if (before.eligible() && ZoneRules.newEffectDuration(before.duration(),effectDuration(entity,THROMBOSIS)))
            ZoneZeroAdvancements.recordExternal(p,"herb_thrombosis");
    }
}
