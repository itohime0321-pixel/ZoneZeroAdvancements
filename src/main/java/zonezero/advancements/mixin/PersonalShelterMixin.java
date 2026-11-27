package zonezero.advancements.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zonezero.advancements.IntegrationHooks;

@Pseudo
@Mixin(targets="com.ruadou.personaldimension.dimension.PersonalDimensionService",remap=false)
public abstract class PersonalShelterMixin {
    @Inject(method="createPersonalDimension(Lnet/minecraft/server/level/ServerPlayer;Lcom/ruadou/personaldimension/dimension/DimensionPreset;Lcom/ruadou/personaldimension/dimension/template/DimensionTemplate;)Z",
            at=@At("RETURN"),require=0,remap=false)
    private static void zz$created(ServerPlayer player,@Coerce Object preset,@Coerce Object template,CallbackInfoReturnable<Boolean> cir) {
        IntegrationHooks.shelterCreated(player,Boolean.TRUE.equals(cir.getReturnValue()));
    }
}
