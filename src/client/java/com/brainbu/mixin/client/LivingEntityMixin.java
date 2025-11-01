package com.brainbu.mixin.client;

import com.brainbu.event.CombatEventsManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow @Final private static Logger LOGGER;

//    @Inject(method = "tryUseDeathProtector", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;sendEntityStatus(Lnet/minecraft/entity/Entity;B)V"))
//    public void onTotemPop(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
//        CombatEventsManager.getInstance().handleTotemPop(source, (LivingEntity) (Object) this);
//    }

//    @Inject(method = "playSound", at = @At(value = "HEAD"))
//    public void playSound(SoundEvent sound, CallbackInfo ci) {
//        if (sound == null) return;
//        LOGGER.warn(sound.id().toString());
//    }

//    @Inject(method = "applyDamage", at = @At("HEAD"))
//    private void onApplyDamage(ServerWorld world, DamageSource source, float amount, CallbackInfo ci) {
//        LOGGER.error("Entity Applied Damage!!!!!!!!!!!!!!!!!!" + source.getType().msgId() + source.getAttacker().getName() + source.getSource().getName()); // TODO
//        //LivingEntity entity = (LivingEntity) (Object) this;
//        //DamageTickManager.onPlayerDamage(entity.getId(), source, amount);
//    }

    // client cannot get this
//    @Inject(method = "setHealth", at = @At(value = "HEAD"))
//    public void onSetHealth(float health, CallbackInfo ci) {
//        if (Objects.equals(Thread.currentThread().getName(), "Server")) return;
//        LOGGER.info("Living Entity Set Health " + this.getHealth() + " " + health + " ");
//    }

    // NOTE: All Anchor damage has NULL attacker but correct damage type
    @Inject(method = "onDamaged", at = @At(value = "HEAD"))
    public void onDamaged(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity receiver = (LivingEntity) (Object) this;
//        String msg = damageSource.getName() + " source=" + damageSource.getSource() + " attacker=" + damageSource.getAttacker();
//        LOGGER.info("Living onDamaged this=" + receiver.getName() + " " + msg);

        CombatEventsManager.getInstance().handleDamage(damageSource, receiver);
    }

//    @Inject(method = "animateDamage", at = @At(value = "HEAD"))
//    public void animateDamage(float yaw, CallbackInfo ci) {
//        LOGGER.error("animateDamage DamageTick!");
//    }

}
