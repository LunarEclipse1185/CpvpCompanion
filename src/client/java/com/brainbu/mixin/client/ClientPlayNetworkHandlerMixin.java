package com.brainbu.mixin.client;

import com.brainbu.event.CombatEventsManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.s2c.play.*;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow
    private ClientWorld world;

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "onEntityDamage", at = @At("HEAD"))
    public void onEntityDamageMixin(EntityDamageS2CPacket packet, CallbackInfo ci) {
        ClientPlayNetworkHandler self = (ClientPlayNetworkHandler) (Object) this;
        NetworkThreadUtils.forceMainThread(packet, self, MinecraftClient.getInstance());

//        LOGGER.info("received " + packet.toString());
        CombatEventsManager.getInstance().handleDamage(packet.createDamageSource(world), world.getEntityById(packet.entityId()));
    }

    @Inject(method = "onEntitiesDestroy", at = @At("HEAD"))
    public void onEntitiesDestroy(EntitiesDestroyS2CPacket packet, CallbackInfo ci) {
        ClientPlayNetworkHandler self = (ClientPlayNetworkHandler) (Object) this;
        NetworkThreadUtils.forceMainThread(packet, self, MinecraftClient.getInstance());

//        String acc = "";
//        for (int id : packet.getEntityIds()) {
//            Entity entity = world.getEntityById(id);
//            if (entity != null) {
//                acc += entity.getName();
//            }
//        }
//        LOGGER.info("received Entities Destroy " + acc);
        packet.getEntityIds().forEach((id) -> {
            Entity entity = world.getEntityById(id);
            if (entity instanceof EndCrystalEntity crystal) {
                CombatEventsManager.getInstance().handleCrystalDestroy(crystal);
            }
        });
    }

//    @Inject(method = "onPlaySound", at = @At("HEAD"))
//    public void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo ci) {
//        LOGGER.info("received PlaySoundS2CPacket " + packet.getSound().getIdAsString());
//    }
//
//    @Inject(method = "onPlaySoundFromEntity", at = @At("HEAD"))
//    public void onPlaySoundFromEntity(PlaySoundFromEntityS2CPacket packet, CallbackInfo ci) {
//        LOGGER.info("received PlaySoundFromEntityS2CPacket " + packet.getSound().getIdAsString());
//    }


//    @Unique
//    @Override
//    public void registerOnDamage(Consumer<EntityDamageS2CPacket> callback) {
//        LOGGER.info("Registered damage listener.");
//        onDamageCallbacks.add(callback);
//    }

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    public void onEntityStatusMixin(EntityStatusS2CPacket packet, CallbackInfo ci) {
        ClientPlayNetworkHandler self = (ClientPlayNetworkHandler) (Object) this;
        NetworkThreadUtils.forceMainThread(packet, self, MinecraftClient.getInstance());
        if (packet.getStatus() == 35) {
            CombatEventsManager.getInstance().handleTotemPop(packet.getEntity(world));
        }
    }

//    @Inject(method = "onEntity", at = @At("RETURN"))
//    public void onEntityMixin(EntityS2CPacket packet, CallbackInfo ci) {
//        onEntityUpdateCallbacks.forEach(c -> c.accept(packet));
//    }

}
