package zonezero.advancements.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zonezero.advancements.IntegrationHooks;

@Pseudo
@Mixin(targets="tw.zonezero.teleport.ZoneZeroTeleport",remap=false)
public abstract class TeleportStoneMixin {
    // 1.0.4 consumes exactly one item, and only after teleportTo returned true.
    @Inject(method="finish(Lnet/minecraft/server/level/ServerPlayer;Ltw/zonezero/teleport/ZoneZeroTeleport$Cast;)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;shrink(I)V",shift=At.Shift.AFTER,remap=false),
            require=0,remap=false)
    private static void zz$teleported(ServerPlayer player,@Coerce Object cast,CallbackInfo ci) {
        IntegrationHooks.shelterTeleported(player);
    }
}
