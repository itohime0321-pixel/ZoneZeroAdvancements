package zonezero.advancements.mixin;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zonezero.advancements.CompactAdvancementLayout;
import java.util.Map;

@Mixin(value = ServerAdvancementManager.class, remap = false)
public abstract class AdvancementLayoutMixin {
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("RETURN"), require = 1)
    private void zz$compactLayout(Map<ResourceLocation, JsonElement> data, ResourceManager resources,
                                  ProfilerFiller profiler, CallbackInfo ci) {
        CompactAdvancementLayout.apply(((ServerAdvancementManager)(Object)this).getAllAdvancements());
    }
}
