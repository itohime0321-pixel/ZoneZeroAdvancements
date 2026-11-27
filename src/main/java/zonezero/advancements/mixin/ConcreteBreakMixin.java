package zonezero.advancements.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zonezero.advancements.ZoneRules;
import zonezero.advancements.ZoneZeroAdvancements;

/** Observe a successful NeoForge block removal, even if there is no harvest/drop.
 * Snapshot the hands before item durability is consumed: a broken pickaxe is not a bare hand.
 */
@Mixin(value=ServerPlayerGameMode.class,remap=false)
public abstract class ConcreteBreakMixin {
    @Shadow @Final protected ServerPlayer player;
    @Unique private static final TagKey<Block> zz$CONCRETE=TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("zonezero_advancements","concrete"));
    @Unique private BlockPos zz$barehandCandidate;

    @Inject(method="destroyBlock(Lnet/minecraft/core/BlockPos;)Z",at=@At("HEAD"),require=1,remap=false)
    private void zz$beforeDestroy(BlockPos pos,CallbackInfoReturnable<Boolean> cir) {
        zz$barehandCandidate=null;
        if (!(player instanceof FakePlayer) && player.gameMode.getGameModeForPlayer()==GameType.SURVIVAL
                && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty())
            zz$barehandCandidate=pos.immutable();
    }

    @Inject(method="removeBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Z",
            at=@At("RETURN"),require=1,remap=false)
    private void zz$afterRemoval(BlockPos pos,BlockState state,boolean canHarvest,CallbackInfoReturnable<Boolean> cir) {
        boolean candidate=pos.equals(zz$barehandCandidate);
        zz$barehandCandidate=null;
        if (!candidate || !cir.getReturnValue() || !state.is(zz$CONCRETE)) return;
        if (ZoneRules.concreteBreak(player.gameMode.getGameModeForPlayer()==GameType.SURVIVAL,
                player.getMainHandItem().isEmpty(),player.getOffhandItem().isEmpty(),
                state.is(zz$CONCRETE),true,player.serverLevel().getBlockState(pos).is(zz$CONCRETE)))
            ZoneZeroAdvancements.barehandConcreteBroken(player);
    }

    @Inject(method="destroyBlock(Lnet/minecraft/core/BlockPos;)Z",at=@At("RETURN"),require=1,remap=false)
    private void zz$clearCandidate(BlockPos pos,CallbackInfoReturnable<Boolean> cir) {
        zz$barehandCandidate=null;
    }
}
