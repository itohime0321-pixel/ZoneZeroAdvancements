package zonezero.advancements;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;

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
            var source = event.getSource();
            if (source.getEntity() instanceof Zombie) record(player, "zombie_death", 500);
            if (source.getEntity() != null && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(source.getEntity().getType()).equals(ResourceLocation.fromNamespaceAndPath("alexsmobs", "centipede_head"))) record(player, "centipede_death", 1);
            if (source.getEntity() instanceof ServerPlayer) record(player, "player_death", 1);
            if (source.is(DamageTypeTags.IS_FALL)) record(player, "fall_death", 1);
            if (source.is(DamageTypes.DROWN)) record(player, "drown_death", 1);
            if (source.is(DamageTypeTags.IS_FIRE)) record(player, "fire_death", 1);
        }
        if (event.getEntity() instanceof Zombie && event.getSource().getEntity() instanceof ServerPlayer killer) {
            increment(killer, KILLS_KEY);
            if (event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
                if (killer.getMainHandItem().isEmpty()) record(killer, "unarmed_zombie_kill", 1);
                else record(killer, "melee_zombie_kill", 1000);
            }
            if (killer.getHealth() <= 2.0F) record(killer, "low_health_zombie_kill", 1);
        }
    }
    private void record(ServerPlayer player, String event, int... thresholds) {
        String key = "zonezero_advancements_hidden_" + event;
        increment(player, key);
        int count = player.getPersistentData().getCompound("PlayerPersisted").getInt(key);
        for (int threshold : thresholds) {
            if (count >= threshold) award(player, "hidden/" + event + "/" + threshold);
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
        long caps = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ResourceLocation.fromNamespaceAndPath("kubejs", "bottle_cap"))) {
                caps += stack.getCount();
            }
        }
        for (int threshold : new int[]{1,100,500,1000,5000,10000,50000,100000}) {
            if (caps >= threshold) award(player, "economy/" + threshold);
        }
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
