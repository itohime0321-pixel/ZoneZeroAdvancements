package zonezero.advancements.mixin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zonezero.advancements.IntegrationHooks;

@Pseudo
@Mixin(targets="com.covalscy.bleedout.BleedOutHandler",remap=false)
public abstract class BleedOutMixin {
    @Shadow @Final private static Map<UUID,?> bleedingPlayers;
    @Shadow @Final private static Map<UUID,?> revivingPlayers;
    @Shadow @Final private static Set<UUID> dyingPlayers;
    @Shadow public static boolean isBleeding(Player player) { throw new AssertionError(); }

    // BleedOut's HIGHEST priority listener clears both markers before its normal
    // death handling returns. Observe the old state and let our LOWEST listener
    // decide if the same death event was actually allowed.
    @Inject(method="onPlayerDeath(Lnet/neoforged/neoforge/event/entity/living/LivingDeathEvent;)V",
            at=@At("HEAD"),require=0,remap=false)
    private static void zz$downedDeath(LivingDeathEvent event,CallbackInfo ci) {
        if (event.getEntity() instanceof ServerPlayer p &&
            (bleedingPlayers.containsKey(p.getUUID()) || dyingPlayers.contains(p.getUUID())))
            IntegrationHooks.markDownedDeath(event,p);
    }

    // Only this validated tick callsite completes normal multiplayer rescue.
    // The OP self/other revive commands use a different callsite and never count.
    @Inject(method="onPlayerTick(Lnet/neoforged/neoforge/event/tick/PlayerTickEvent$Post;)V",
            at=@At(value="INVOKE",target="Lcom/covalscy/bleedout/BleedOutHandler;revivePlayer(Lnet/minecraft/server/level/ServerPlayer;)V",shift=At.Shift.BEFORE,remap=false),
            require=0,remap=false)
    private static void zz$beforeRevive(PlayerTickEvent.Post event,CallbackInfo ci) {
        IntegrationHooks.clearRevive();
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Object rescue=revivingPlayers.get(p.getUUID());
        if (rescue instanceof BleedOutReviveDataAccessor data && bleedingPlayers.containsKey(p.getUUID()))
            IntegrationHooks.startRevive(p,data.zz$getReviverId());
    }
    @Inject(method="onPlayerTick(Lnet/neoforged/neoforge/event/tick/PlayerTickEvent$Post;)V",
            at=@At(value="INVOKE",target="Lcom/covalscy/bleedout/BleedOutHandler;revivePlayer(Lnet/minecraft/server/level/ServerPlayer;)V",shift=At.Shift.AFTER,remap=false),
            require=0,remap=false)
    private static void zz$afterRevive(PlayerTickEvent.Post event,CallbackInfo ci) {
        if (event.getEntity() instanceof ServerPlayer p)
            IntegrationHooks.finishRevive(p,!isBleeding(p));
        else IntegrationHooks.clearRevive();
    }
}
