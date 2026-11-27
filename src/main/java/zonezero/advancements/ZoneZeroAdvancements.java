package zonezero.advancements;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.resources.ResourceLocation;

@Mod("zonezero_advancements")
public final class ZoneZeroAdvancements {
    private static final int[] DEATHS = {1,10,30,50,75,100,150,200,250,300,350,400,450,486,500,550,600,650,700,750,800,850,900,950,1000};
    public ZoneZeroAdvancements() {
        NeoForge.EVENT_BUS.addListener(this::onTick);
    }
    private void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 100 != 0) return;
        int count = player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
        for (int threshold : DEATHS) {
            if (count < threshold) break;
            var advancement = player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("zonezero_advancements", "death/" + threshold));
            if (advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                player.getAdvancements().award(advancement, "milestone");
            }
        }
    }
}
