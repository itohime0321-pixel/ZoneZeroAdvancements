package zonezero.advancements.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zonezero.advancements.IntegrationHooks;

@Pseudo
@Mixin(targets="com.plank.meds_and_herbs.data.UseMedicine",remap=false)
public abstract class MedicineFunctionMixin {
    @Inject(method="executeFunction(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/resources/ResourceLocation;)V",
            at=@At("HEAD"),require=0,remap=false)
    private static void zz$before(LivingEntity entity,ResourceLocation function,CallbackInfo ci) {
        IntegrationHooks.medicineStart(entity,function);
    }
    @Inject(method="executeFunction(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/resources/ResourceLocation;)V",
            at=@At("RETURN"),require=0,remap=false)
    private static void zz$after(LivingEntity entity,ResourceLocation function,CallbackInfo ci) {
        IntegrationHooks.medicineFinish(entity,function);
    }
}
