package com.brainbu.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerMixin {
    // TODO health change (actually only Me), Damage Tick Start
}
