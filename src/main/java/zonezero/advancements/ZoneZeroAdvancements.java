package zonezero.advancements;

import com.mojang.logging.LogUtils;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameRules;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Mod("zonezero_advancements")
public final class ZoneZeroAdvancements {
    private static final String MOD="zonezero_advancements";
    private static final String PERSISTED="PlayerPersisted";
    private static final String DATA=MOD+"_progress_v1";
    private static final TagKey<EntityType<?>> ZOMBIES=TagKey.create(Registries.ENTITY_TYPE,id("zombies"));
    private static final TagKey<Item> ALCOHOL=TagKey.create(Registries.ITEM,id("alcohol"));
    private static final TagKey<Item> MEDICAL=TagKey.create(Registries.ITEM,id("medical_use"));
    private static final TagKey<Item> MELEE=TagKey.create(Registries.ITEM,id("melee_weapons"));
    private static final Map<UUID,double[]> LAST_POSITION=new HashMap<>();
    private static final Set<String> GUAI=Set.of("guaiguai:green_guaiguai","guaiguai:yellow_guaiguai","guaiguai:red_guaiguai","guaiguai:blue_guaiguai","guaiguai:purple_guaiguai","guaiguai:black_guaiguai");
    private static final String CAP="kubejs:bottle_cap", BOX="kubejs:bottle_cap_box";
    private static final Map<String,int[]> EXTERNAL=Map.ofEntries(
        Map.entry("revived",new int[]{1}),Map.entry("revive_other",new int[]{1,100,500}),
        Map.entry("downed_death",new int[]{1}),Map.entry("gun_headshot_kill",new int[]{1}),
        Map.entry("shelter_create",new int[]{1}),Map.entry("shelter_teleport",new int[]{1,10}),
        Map.entry("low_health_teleport",new int[]{1}),Map.entry("herb_thrombosis",new int[]{1}),
        Map.entry("medical_use",new int[]{1000}));
    private static final Map<UUID,ArrayDeque<Long>> ZOMBIE_BURSTS=new HashMap<>();
    private static final Map<UUID,Long> SESSIONS=new HashMap<>();
    private static final Map<UUID,Pair> LAST_KILLS=new HashMap<>(), LAST_DEATHS=new HashMap<>();
    private record Pair(UUID other,long tick) {}
    private static int boxValue=50;
    public ZoneZeroAdvancements() {
        loadConfiguration();
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onPlace);
        NeoForge.EVENT_BUS.addListener(this::onFinishUse);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onLogin);
        NeoForge.EVENT_BUS.addListener(this::onClone);
        NeoForge.EVENT_BUS.addListener(this::onLogout);
        NeoForge.EVENT_BUS.addListener(this::onTick);
        GunKillBridge.register();
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onAdvancementEarned);
    }
    private void onServerStarted(ServerStartedEvent event) {
        // CI boots without a connected player; proactively load the game-mode class for mixin validation.
        if (Boolean.getBoolean("zonezero.verifyConcreteHook"))
            LogUtils.getLogger().info("ZoneZero test: loaded {}",net.minecraft.server.level.ServerPlayerGameMode.class.getName());
        var manager=event.getServer().getAdvancements();
        long count=manager.getAllAdvancements().stream().filter(a->MOD.equals(a.id().getNamespace())).count();
        LogUtils.getLogger().info("ZoneZero: loaded {} advancement definitions (including root).",count);
        if (manager.get(id("root"))==null)
            LogUtils.getLogger().warn("ZoneZero: root advancement is missing; inspect data pack loading.");
        if (Boolean.getBoolean("zonezero.verifyCompactLayout"))
            CompactAdvancementLayout.verify(manager.getAllAdvancements());
        if (Boolean.getBoolean("zonezero.verifyChatAnnouncements"))
            verifyChatAnnouncements(event.getServer());
    }
    /**
     * NeoForge fires this event once the advancement requirements become complete,
     * independent of the display's vanilla announce_to_chat setting.
     * Access only our own namespace, skip the invisible bootstrap root, and respect
     * the world gamerule. This also covers an administrator's manual grant.
     */
    private void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        AdvancementHolder earned=event.getAdvancement();
        if (!MOD.equals(earned.id().getNamespace()) || earned.id().getPath().equals("root")) return;
        var server=p.getServer();
        if (server==null || !p.level().getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS)) return;
        earned.value().display().ifPresent(display -> {
            server.getPlayerList().broadcastSystemMessage(formatAnnouncement(p.getDisplayName(),display),false);
        });
    }
    /** Vanilla-style green bracketed title; white description, one naturally wrapped entry. */
    private static Component formatAnnouncement(Component playerName, net.minecraft.advancements.DisplayInfo display) {
        // The second line is the mechanical unlock condition; keep it in the
        // advancement screen, not in the concise chat announcement.
        String description=display.getDescription().getString().lines().findFirst().orElse("");
        return Component.empty().append(playerName)
            .append(Component.literal(" 已完成進度 ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal("["+display.getTitle().getString()+"]").withStyle(ChatFormatting.GREEN))
            .append(Component.literal("。"+description).withStyle(ChatFormatting.WHITE));
    }
    /** Strictly opt-in isolated CI: inject earned events, not persistent player awards. */
    private static void verifyChatAnnouncements(net.minecraft.server.MinecraftServer server) {
        var fake=net.neoforged.neoforge.common.util.FakePlayerFactory.get(server.overworld(),
            new com.mojang.authlib.GameProfile(UUID.fromString("a62ab996-8b00-4a7c-9e15-2bf775862161"),"ZZ_Chat_Test"));
        var manager=server.getAdvancements();
        var ordinary=Objects.requireNonNull(manager.get(id("death/1")));
        var hidden=Objects.requireNonNull(manager.get(id("hidden/barehand_concrete_break/1")));
        var preview=formatAnnouncement(fake.getDisplayName(),ordinary.value().display().orElseThrow());
        var segments=preview.getSiblings();
        if (segments.size()!=4
                || !segments.get(1).getString().equals(" 已完成進度 ")
                || !segments.get(2).getString().equals("[初見殺]")
                || !segments.get(3).getString().equals("。歡迎來到零號禁區，新手教學的最後一課：你會死。"))
            throw new IllegalStateException("Unexpected achievement chat component structure");
        if (segments.get(1).getStyle().getColor().getValue()!=0xffffff
                || segments.get(2).getStyle().getColor().getValue()!=0x55ff55
                || segments.get(3).getStyle().getColor().getValue()!=0xffffff)
            throw new IllegalStateException("Achievement chat prefix/title/description colors differ from white/green/white");
        for (var holder:manager.getAllAdvancements()) {
            if (!MOD.equals(holder.id().getNamespace()) || holder.id().getPath().equals("root")) continue;
            var display=holder.value().display().orElseThrow();
            var lines=display.getDescription().getString().lines().toList();
            String expected=fake.getDisplayName().getString()+" 已完成進度 ["+display.getTitle().getString()+"]。"+lines.getFirst();
            if (lines.size()!=2 || !formatAnnouncement(fake.getDisplayName(),display).getString().equals(expected))
                throw new IllegalStateException("Expected concise flavor-only announcement: "+holder.id());
        }
        LogUtils.getLogger().info("ZoneZero test: verified white prefix, green bracketed title, white description.");
        LogUtils.getLogger().info("ZoneZero test: all 202 announcements contain only the flavor introduction.");
        var root=Objects.requireNonNull(manager.get(id("root")));
        var unrelated=Objects.requireNonNull(manager.get(ResourceLocation.fromNamespaceAndPath("minecraft","story/root")));
        var rule=server.overworld().getGameRules().getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS);
        boolean previous=rule.get();
        try {
            rule.set(true,server);
            NeoForge.EVENT_BUS.post(new AdvancementEvent.AdvancementEarnEvent(fake,ordinary));
            NeoForge.EVENT_BUS.post(new AdvancementEvent.AdvancementEarnEvent(fake,hidden));
            NeoForge.EVENT_BUS.post(new AdvancementEvent.AdvancementEarnEvent(fake,root));
            NeoForge.EVENT_BUS.post(new AdvancementEvent.AdvancementEarnEvent(fake,unrelated));
            rule.set(false,server);
            // If gamerule is ignored this would print a duplicate ordinary announcement.
            NeoForge.EVENT_BUS.post(new AdvancementEvent.AdvancementEarnEvent(fake,ordinary));
        } finally {
            rule.set(previous,server);
        }
        LogUtils.getLogger().info("ZoneZero test: sent ordinary, hidden, root, unrelated and muted advancement events.");
    }
    private static ResourceLocation id(String path) {return ResourceLocation.fromNamespaceAndPath(MOD,path);}
    private static void loadConfiguration() {
        Path path=FMLPaths.CONFIGDIR.get().resolve("zonezero_advancements.properties");
        Properties p=new Properties();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) Files.writeString(path,"# Zone Zero economy: 1 box = 50 bottle caps. 0 disables box conversion.\nbottleCapBoxValue=50\n");
            try (var in=Files.newInputStream(path)) {p.load(in);}
            boxValue=Math.max(0,Integer.parseInt(p.getProperty("bottleCapBoxValue","50")));
        } catch (IOException|NumberFormatException e) {
            boxValue=0;
            LogUtils.getLogger().warn("ZoneZero: invalid achievement configuration; bottle cap boxes ignored.",e);
        }
    }
    private static CompoundTag data(ServerPlayer p) {
        var root=p.getPersistentData();
        if (!root.contains(PERSISTED)) root.put(PERSISTED,new CompoundTag());
        var persisted=root.getCompound(PERSISTED);
        if (!persisted.contains(DATA)) persisted.put(DATA,new CompoundTag());
        return persisted.getCompound(DATA);
    }
    private static int add(ServerPlayer p,String key) {
        var d=data(p);
        int previous=d.getInt(key);
        int result=previous==Integer.MAX_VALUE?previous:previous+1;
        d.putInt(key,result);
        return result;
    }
    private static void record(ServerPlayer p,String event,int...thresholds) {
        int count=add(p,"hidden_"+event);
        for(int n:thresholds) if(count>=n) award(p,"hidden/"+event+"/"+n);
    }
    /** Server-side integration point: invoke only after a confirmed successful mod event.
     * No packet, client API or command is provided; external events cannot spoof counters remotely.
     */
    public static boolean recordExternal(ServerPlayer player,String event) {
        int[] thresholds=EXTERNAL.get(event);
        if (player==null || thresholds==null || player.getServer()==null || !player.getServer().isSameThread()) return false;
        record(player,event,thresholds);
        return true;
    }
    private void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        SESSIONS.put(p.getUUID(),System.nanoTime());
        LAST_POSITION.put(p.getUUID(),new double[]{p.getX(),p.getZ(),zone(p),p.getY()});
        restoreAwards(p);
        settle(p);
    }
    private void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer target)) return;
        var original=event.getOriginal().getPersistentData();
        if (!original.contains(PERSISTED)) return;
        var source=original.getCompound(PERSISTED);
        if (!source.contains(DATA)) return;
        var root=target.getPersistentData();
        if (!root.contains(PERSISTED)) root.put(PERSISTED,new CompoundTag());
        root.getCompound(PERSISTED).put(DATA,source.getCompound(DATA).copy());
    }
    private void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid=event.getEntity().getUUID();
        SESSIONS.remove(uuid);ZOMBIE_BURSTS.remove(uuid);LAST_POSITION.remove(uuid);LAST_KILLS.remove(uuid);LAST_DEATHS.remove(uuid);
    }
    private static boolean zombie(Entity e) {return e!=null && e.getType().is(ZOMBIES);}
    private static String typeId(Entity e) {return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();}
    private static int zone(Entity e) {return e.level().dimension().equals(Level.OVERWORLD)? ZoneRules.zone(e.getX(),e.getZ()):0;}
    private void onDeath(LivingDeathEvent event) {
        boolean downed=IntegrationHooks.consumeDownedDeath(event);
        if (event.isCanceled() || event.getEntity().level().isClientSide()) return;
        Entity victim=event.getEntity(), killer=event.getSource().getEntity();
        var source=event.getSource();
        long tick=victim.level().getGameTime();
        if (victim instanceof ServerPlayer p) {
            if (downed) recordExternal(p,"downed_death");
            int deaths=add(p,"deaths"); data(p).putInt("peaceful_ticks",0);
            data(p).putInt("hidden_zombie_survival_streak",0);
            ZOMBIE_BURSTS.remove(p.getUUID());
            milestones(p,"death",deaths,ZoneRules.DEATHS);
            if (zombie(killer)) record(p,"zombie_death",500);
            if (killer!=null && typeId(killer).equals("alexsmobs:centipede_head")) record(p,"centipede_death",1);
            if (killer instanceof ServerPlayer && killer!=p) record(p,"player_death",1);
            if (source.is(DamageTypeTags.IS_FALL)) record(p,"fall_death",1);
            if (source.is(DamageTypes.DROWN)) record(p,"drown_death",1);
            if (source.is(DamageTypeTags.IS_FIRE)) record(p,"fire_death",1);
            if (killer!=null && killer!=p) {
                LAST_DEATHS.put(p.getUUID(),new Pair(killer.getUUID(),tick));
                if (matches(LAST_KILLS.remove(p.getUUID()),killer,tick)) {
                    LAST_DEATHS.remove(p.getUUID()); award(p,"hidden/mutual_death/1");
                }
            }
        }
        if (killer instanceof ServerPlayer p && p!=victim) {
            GunKillBridge.noteDeath(event.getEntity(),p,source);
            data(p).putInt("peaceful_ticks",0);
            if (matches(LAST_DEATHS.remove(p.getUUID()),victim,tick)) award(p,"hidden/mutual_death/1");
            else LAST_KILLS.put(p.getUUID(),new Pair(victim.getUUID(),tick));
            if (victim.getType()==EntityType.VILLAGER) record(p,"villager_kill",1);
            if (victim.getType()==EntityType.ZOMBIE_VILLAGER) award(p,"hidden/zombie_villager_kill/1");
            if (!zombie(victim)) return;
            if (source.is(DamageTypes.ARROW)
                    && (source.getDirectEntity() instanceof Arrow || source.getDirectEntity() instanceof SpectralArrow)
                    && ZoneRules.longRangeArrow(p.distanceToSqr(victim)))
                award(p,"hidden/long_range_arrow_kill/1");
            record(p,"zombie_survival_streak",50);
            if (victim.isOnFire()) record(p,"burning_zombie_kill",25);
            if (victim.level().dimension().equals(Level.OVERWORLD)) {
                if (ZoneRules.fullMoonNight(victim.level().getDayTime())) record(p,"full_moon_zombie_kill",20);
                if (victim.level().isRainingAt(victim.blockPosition())) record(p,"rain_zombie_kill",10);
            }
            if (!done(p,"hidden/zombie_burst/5") && p.getServer()!=null) {
                var burst=ZOMBIE_BURSTS.computeIfAbsent(p.getUUID(),k->new ArrayDeque<>());
                long now=p.getServer().overworld().getGameTime();
                if (ZoneRules.addBurstKill(burst,now)) {
                    award(p,"hidden/zombie_burst/5");
                    ZOMBIE_BURSTS.remove(p.getUUID());
                }
            }
            int kills=add(p,"zombie_kills");
            if (kills<=10000 && kills%100==0) award(p,"zombie/"+kills);
            if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity()==p) {
                if (p.getMainHandItem().isEmpty()) record(p,"unarmed_zombie_kill",1,10,50);
                else if (p.getMainHandItem().is(MELEE)) record(p,"melee_zombie_kill",1000);
            }
            if (p.getHealth()<2.0F) record(p,"low_health_zombie_kill",1);
            if (zone(victim)==7 && zone(p)==7) record(p,"zone7_zombie_kill",1000);
        }
    }
    private static boolean matches(Pair pair,Entity entity,long tick) {
        return pair!=null && pair.other.equals(entity.getUUID()) && tick>=pair.tick && tick-pair.tick<=100;
    }
    private void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.isCanceled() || !(e.getEntity() instanceof ServerPlayer p)) return;
        String block=BuiltInRegistries.BLOCK.getKey(e.getPlacedBlock().getBlock()).toString();
        if (!GUAI.contains(block)) return;
        award(p,"hidden/guai_place/1");
        CompoundTag d=data(p);
        d.putBoolean("placed_"+block,true);
        if (GUAI.stream().allMatch(b->d.getBoolean("placed_"+b))) award(p,"hidden/guai_varieties/6");
    }
    private void onFinishUse(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ItemStack stack=e.getItem();
        long time=Math.floorMod(p.level().getDayTime(),24000L);
        if (stack.has(DataComponents.FOOD) && time>=17500 && time<=18500) award(p,"hidden/midnight_food/1");
        if (stack.is(ALCOHOL)) record(p,"alcohol",1,100);
        if (stack.is(MEDICAL)) record(p,"medical_use",1000);
    }
    private void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        CompoundTag d=data(p);
        int quiet=d.getInt("peaceful_ticks");
        if (quiet<24000) {
            d.putInt("peaceful_ticks",quiet+1);
            if (quiet+1==24000) award(p,"hidden/peaceful_game_day/1");
        }
        if (p.tickCount%20!=0) return;
        // Exploration is based on current position, never the highest unlocked ring.
        int zone=zone(p);
        double[] previous=LAST_POSITION.get(p.getUUID());
        if (previous==null) previous=new double[]{p.getX(),p.getZ(),zone,p.getY()};
        boolean moved=Math.abs(previous[0]-p.getX())+Math.abs(previous[1]-p.getZ())>0.1;
        boolean movedVertically=Math.abs(previous[3]-p.getY())>0.1;
        if (zone>0 && (previous[2]!=zone || moved)) {
            award(p,"exploration/"+zone);
            boolean all=true;
            for (int n=1;n<=7;n++) if (!done(p,"exploration/"+n)) {all=false;break;}
            if (all) award(p,"hidden/all_zones/7");
        }
        if ((moved || movedVertically) && p.level().dimension().equals(Level.OVERWORLD)
                && p.getY()>=300 && p.onGround() && !p.isSpectator() && !p.getAbilities().flying)
            award(p,"hidden/summit/1");
        if (moved || movedVertically || previous[2]!=zone || zone==0)
            LAST_POSITION.put(p.getUUID(),new double[]{p.getX(),p.getZ(),zone,p.getY()});
        if (p.tickCount%100==0) settle(p);
        Long start=SESSIONS.get(p.getUUID());
        if(start!=null && System.nanoTime()-start>=8L*60*60*1_000_000_000L) award(p,"hidden/session_hours/8");
    }
    private static void settle(ServerPlayer p) {
        CompoundTag d=data(p);
        int genetics=checkSkill(p,"genetics","zonezero","genetics",ZoneRules.GENETICS);
        int guns=checkSkill(p,"guns","tacz_skill_tree","tacz",ZoneRules.GUNS);
        if(genetics>=46 && guns>=93) award(p,"hidden/both_trees_full/1");
        // Record a bootstrap baseline, not the player's pre-installation wealth.
        long caps=0;
        for (int i=0;i<p.getInventory().getContainerSize();i++) {
            ItemStack stack=p.getInventory().getItem(i);
            if(stack.isEmpty()) continue;
            String item=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if(item.equals(CAP)) caps+=stack.getCount();
            else if(item.equals(BOX)) caps+=(long)stack.getCount()*boxValue;
        }
        if(!d.contains("caps_base") || !d.contains("caps_box_value") || d.getInt("caps_box_value")!=boxValue) {
            d.putLong("caps_base",caps);
            d.putInt("caps_box_value",boxValue);
        }
        long base=Math.min(d.getLong("caps_base"),caps);
        d.putLong("caps_base",base);
        for(int n:ZoneRules.ECONOMY) if(caps-base>=n) award(p,"economy/"+n);
        if(done(p,"death/1000") && done(p,"zombie/10000") && done(p,"genetics/46") && done(p,"guns/93"))
            award(p,"hidden/core_complete/1");
    }
    private static void restoreAwards(ServerPlayer p) {
        // Recover only our own post-install counters, not vanilla historical stats.
        CompoundTag d=data(p);
        milestones(p,"death",d.getInt("deaths"),ZoneRules.DEATHS);
        int kills=d.getInt("zombie_kills");
        for(int n=100;n<=Math.min(kills,10000);n+=100) award(p,"zombie/"+n);
    }
    private static int checkSkill(ServerPlayer p,String path,String namespace,String category,int[] thresholds) {
        Integer actual=SkillsBridge.spent(p,namespace,category);
        if(actual==null || actual<0) return -1;
        var d=data(p); String key="skill_base_"+path;
        int base=d.contains(key)?Math.min(d.getInt(key),actual):actual;
        d.putInt(key,base);
        int progress=ZoneRules.newProgress(base,actual);
        milestones(p,path,progress,thresholds);
        return progress;
    }
    /** Called only by the server block-removal hook after actual successful destruction. */
    public static void barehandConcreteBroken(ServerPlayer p) {
        award(p,"hidden/barehand_concrete_break/1");
    }
    private static void milestones(ServerPlayer p,String path,int count,int[] thresholds) {
        for(int n:thresholds) {if(count<n) break;award(p,path+"/"+n);}
    }
    private static AdvancementHolder lookup(ServerPlayer p,String path) {
        var server=p.getServer();return server==null?null:server.getAdvancements().get(id(path));
    }
    private static boolean done(ServerPlayer p,String path) {
        var a=lookup(p,path);return a!=null && p.getAdvancements().getOrStartProgress(a).isDone();
    }
    private static void award(ServerPlayer p,String path) {
        var a=lookup(p,path);
        if(a!=null && !p.getAdvancements().getOrStartProgress(a).isDone()) p.getAdvancements().award(a,"milestone");
    }
}
