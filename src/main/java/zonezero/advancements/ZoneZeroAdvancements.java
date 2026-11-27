package zonezero.advancements;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

@Mod("zonezero_advancements")
public final class ZoneZeroAdvancements {
    private static final String DEATHS_KEY = "zonezero_advancements_deaths";
    private static final String KILLS_KEY = "zonezero_advancements_zombie_kills";
    private static final int[] DEATHS = {1,10,30,50,75,100,150,200,250,300,350,400,450,486,500,550,600,650,700,750,800,850,900,950,1000};
    public ZoneZeroAdvancements() {
        NeoForge.EVENT_BUS.addListener(this::onDeath);
        NeoForge.EVENT_BUS.addListener(this::onTick);
    }
    private void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            increment(player, DEATHS_KEY);
        }
        if (event.getEntity() instanceof Zombie && event.getSource().getEntity() instanceof ServerPlayer killer) {
            increment(killer, KILLS_KEY);
        }
    }
    private void increment(ServerPlayer player, String key) {
        var data = player.getPersistentData().getCompound("PlayerPersisted");
        int count = data.getInt(key);
        if (count < Integer.MAX_VALUE) data.putInt(key, count + 1);
        player.getPersistentData().put("PlayerPersisted", data);
    }
    private void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 100 != 0) return;
        var data = player.getPersistentData().getCompound("PlayerPersisted");
        int deaths = data.getInt(DEATHS_KEY);
        int kills = data.getInt(KILLS_KEY);
        for (int threshold : DEATHS) {
            if (deaths < threshold) break;
            award(player, "death/" + threshold);
        }
        for (int threshold = 100; threshold <= Math.min(kills, 10000); threshold += 100) {
            award(player, "zombie/" + threshold);
        }
    }
    private void award(ServerPlayer player, String path) {
        var advancement = player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("zonezero_advancements", path));
        if (advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            player.getAdvancements().award(advancement, "milestone");
        }
    }
}
