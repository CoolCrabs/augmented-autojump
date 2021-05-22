package io.github.coolcrabs.augmentedautojump.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import io.github.coolcrabs.augmentedautojump.AugmentedAutojump;
import net.minecraft.client.network.ClientPlayerEntity;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Shadow
    private int ticksToNextAutojump;

    @Shadow
    abstract boolean shouldAutoJump();

    @Overwrite
    public void autoJump(float dx, float dz) {
        if (shouldAutoJump()) ticksToNextAutojump = AugmentedAutojump.autojumpPlayer((ClientPlayerEntity)(Object)this, dx, dz) ? 1 : 0;
    }
}
