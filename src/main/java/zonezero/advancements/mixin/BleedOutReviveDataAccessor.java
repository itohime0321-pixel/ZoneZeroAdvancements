package zonezero.advancements.mixin;

import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read the actual rescuer's UUID, without changing the rescue state. */
@Pseudo
@Mixin(targets="com.covalscy.bleedout.BleedOutHandler$ReviveData",remap=false)
public interface BleedOutReviveDataAccessor {
    @Accessor(value="reviverId",remap=false)
    UUID zz$getReviverId();
}
