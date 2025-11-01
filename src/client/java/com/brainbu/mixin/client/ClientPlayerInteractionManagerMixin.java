package com.brainbu.mixin.client;

import com.brainbu.render.BehaviorWarningManager;
import com.brainbu.event.CombatEventsManager;
import com.brainbu.event.DamageTypeHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

//    @Inject(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket;<init>(Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;I)V"))
//    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
//        BlockPos pos = hitResult.getBlockPos();
//        ItemStack stack = player.getStackInHand(hand);
//        ActionWarningManager.onBlockPlaced(pos, stack);
//    }

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "attackEntity", at = @At(value = "HEAD"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
//        LOGGER.info("Caught Player Attack Entity " + player.getName() + " " + target.getName());

        if (target instanceof net.minecraft.entity.LivingEntity) {
            BehaviorWarningManager.onPlayerAttack((net.minecraft.entity.LivingEntity) target);
        }
        RegistryEntry<DamageType> entry = DamageTypeHelper.entryOf("player");
        CombatEventsManager.getInstance().handleAttackEntity(new DamageSource(entry, null, player), target);
    }

    @Inject(method = "interactBlock", at = @At(value = "HEAD"))
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        BlockState blockState = MinecraftClient.getInstance().world.getBlockState(hitResult.getBlockPos());

//        LOGGER.info("Caught Player Interact Block " + player.getName() + " " + hitResult.getType() + " " + blockState.getBlock().getName());

        CombatEventsManager.getInstance().handleInteractBlock(player, hand, blockState, hitResult);
    }

    @Inject(method = "attackBlock", at = @At(value = "HEAD"))
    private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BlockState blockState = MinecraftClient.getInstance().world.getBlockState(pos);

//        LOGGER.info("Caught Player Attack Block " + direction + " " + blockState.getBlock().getName());

        CombatEventsManager.getInstance().handleAttackBlock(blockState, direction);
    }
}