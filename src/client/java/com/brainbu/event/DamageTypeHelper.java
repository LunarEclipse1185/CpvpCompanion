package com.brainbu.event;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class DamageTypeHelper {
    public static DamageType of(String identifier) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if(world == null) return null;
        Registry<DamageType> damageTypeRegistry = world.getRegistryManager().getOptional(RegistryKeys.DAMAGE_TYPE).orElse(null);
        if (damageTypeRegistry == null) return null;
        DamageType type = damageTypeRegistry.get(Identifier.of("minecraft", identifier));
        return type;
    }

    public static RegistryEntry<DamageType> entryOf(String identifier) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if(world == null) return null;
        Registry<DamageType> damageTypeRegistry = world.getRegistryManager().getOptional(RegistryKeys.DAMAGE_TYPE).orElse(null);
        if (damageTypeRegistry == null) return null;
        DamageType type = damageTypeRegistry.get(Identifier.of("minecraft", identifier));
        RegistryEntry<DamageType> entry = damageTypeRegistry.getEntry(type);
        return entry;
    }
}
