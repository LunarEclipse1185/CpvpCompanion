package com.brainbu.render;

import com.brainbu.config.CpvpCompanionConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class BehaviorWarningManager {
    private static final CpvpCompanionConfig config = CpvpCompanionConfig.INSTANCE;

    private static final ArrayList<WarningMessage> messageQueue = new ArrayList<>();

//    public static void registerPacketListeners(MinecraftClient client) {
//        if (!(client.getNetworkHandler() instanceof INetworkMixin mixin)) return;
//
//        mixin.registerOnDamage((packet) -> {
//            onEntityDamage(packet.entityId(), packet.damage());
//        });
//
//        mixin.registerOnStatus((packet) -> {
//            if (packet.getStatus() == 35) { // Totem pop
//                onTotemPop(packet.getEntity(MinecraftClient.getInstance()));
//                EntityStatuses.
//            }
//        });
//
//        client.getNetworkHandler().registerListener(ExplosionS2CPacket.class, (packet, sender) -> {
//            onExplosion(packet.getX(), packet.getY(), packet.getZ());
//        });
//    }
    
//    private static void onEntityDamage(int entityId, float damage) {
//        long currentTick = MinecraftClient.getInstance().world.getTime();
//        entityDamageTicks.put(entityId, currentTick);
//        recentDamages.put(entityId, (double) 0);
//    }
//
//    private static void onTotemPop(Entity entity) {
//        if (entity != null && entity instanceof LivingEntity) {
//            addWarningMessage("Totem Pop", CpvpCompanionConfig.WarningSeverity.NORMAL);
//        }
//    }
    
    private static void onExplosion(double x, double y, double z) {
        if (!config.behaviorWarnings.enabled) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Check for "Attack Too Fast" warning
        checkAttackTooFast(client, x, y, z);
    }
    
    public static void endClientTick(MinecraftClient client) {
        if (!config.behaviorWarnings.enabled) return;
        
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        long currentTick = client.world.getTime();
        
        // Check for various warnings
        checkNoKnockbackWarning(player);
        checkTotemWarning(player, currentTick);
        checkAttackTooSlow(currentTick);
        
        // Update message animations
        updateMessages(currentTick);
    }
    
    private static void checkNoKnockbackWarning(ClientPlayerEntity player) {
        if (!config.behaviorWarnings.noKnockbackEnabled) return;
        
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand.getItem() instanceof SwordItem) return; // Holding a sword
        
        // Check if player is attacking (simplified check)
        if (player.getAttackCooldownProgress(0.0f) > 0.9f) {
            HitResult hit = player.raycast(3.0, 0.0f, false);
            if (hit.getType() == HitResult.Type.ENTITY) {
                addWarningMessage("No Knockback", config.behaviorWarnings.noKnockbackSeverity);
            }
        }
    }

    private static long lastDoubleHandTick = 0;
    private static WarningMessage doubleHandMessage = null;
    private static WarningMessage mainHandTotemMessage = null;
    private static WarningMessage offHandTotemMessage = null;

    private static void checkTotemWarning(ClientPlayerEntity player, long currentTick) {
        if (!config.behaviorWarnings.totemWarningsEnabled) return;

        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();
        
        boolean hasTotemMain = player.getInventory().getStack(config.behaviorWarnings.mainHandTotemSlot).getItem() == Items.TOTEM_OF_UNDYING;
        boolean hasTotemOff = offHand.getItem() == Items.TOTEM_OF_UNDYING;
        boolean doubleHanding = hasTotemOff && mainHand.getItem() == Items.TOTEM_OF_UNDYING;

        if (doubleHanding) {
            lastDoubleHandTick = currentTick;
            if (doubleHandMessage != null) {
                messageQueue.remove(doubleHandMessage);
                doubleHandMessage = null;
            }
        }
        if (!doubleHanding && doubleHandMessage == null && currentTick - lastDoubleHandTick > 20) {
            doubleHandMessage = addWarningMessageTracked("Double Hand!", CpvpCompanionConfig.WarningSeverity.MEDIUM);
        }

        if (mainHandTotemMessage != null && hasTotemMain) {
            messageQueue.remove(mainHandTotemMessage);
            mainHandTotemMessage = null;
        }
        if (mainHandTotemMessage == null && !hasTotemMain) {
            mainHandTotemMessage = addWarningMessageTracked("MainHand Totem!", CpvpCompanionConfig.WarningSeverity.FATAL);
        }
        if (offHandTotemMessage != null && hasTotemOff) {
            messageQueue.remove(offHandTotemMessage);
            offHandTotemMessage = null;
        }
        if (offHandTotemMessage == null && !hasTotemOff) {
            offHandTotemMessage = addWarningMessageTracked("OffHand Totem!", CpvpCompanionConfig.WarningSeverity.FATAL);
        }

        if (!hasTotemMain || !hasTotemOff) {
            lastDoubleHandTick = currentTick;
        }
    }
    
    private static void checkInDamageTickWarning(LivingEntity target) {
        if (!config.behaviorWarnings.inDamageTickEnabled) return;
        
//        Long damageTickStart = entityDamageTicks.get(target.getId());
//        if (damageTickStart != null) {
//            long currentTick = MinecraftClient.getInstance().world.getTime();
//            if (currentTick - damageTickStart < 10) {
//                addWarningMessage("In Damage Tick", config.actionWarnings.inDamageTickSeverity);
//            }
//        }
    }
    
    private static void checkBadBlockPlacement(BlockPos pos) {
        if (!config.behaviorWarnings.badBlockPlacementEnabled) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        
        // Check for entities at the placement position
        List<Entity> entities = client.world.getOtherEntities(client.player, 
            net.minecraft.util.math.Box.from(Vec3d.ofCenter(pos)).expand(0.5));
        
        if (!entities.isEmpty()) {
            addWarningMessage("Bad Block Placement", config.behaviorWarnings.badBlockPlacementSeverity);
        }
    }
    
    private static void checkBadCrystalPlacement(BlockPos pos) {
        if (!config.behaviorWarnings.badCrystalPlacementEnabled) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        
        BlockState clickedBlock = client.world.getBlockState(pos);
        BlockState aboveBlock = client.world.getBlockState(pos.up());
        BlockState aboveAboveBlock = client.world.getBlockState(pos.up(2));
        
        // Check if clicked block is obsidian or bedrock
        if (clickedBlock.getBlock() != Blocks.OBSIDIAN && clickedBlock.getBlock() != Blocks.BEDROCK) {
            addWarningMessage("Bad Crystal Placement", config.behaviorWarnings.badCrystalPlacementSeverity);
            return;
        }
        
        // Check if blocks above are replaceable
        if (!aboveBlock.isReplaceable() || !aboveAboveBlock.isReplaceable()) {
            addWarningMessage("Bad Crystal Placement", config.behaviorWarnings.badCrystalPlacementSeverity);
        }
    }
    
    private static void checkAttackTooFast(MinecraftClient client, double x, double y, double z) {
        if (!config.behaviorWarnings.attackTooFastEnabled) return;
        
//        // Check if any enemies are in damage tick range near explosion
//        for (Map.Entry<Integer, Long> entry : entityDamageTicks.entrySet()) {
//            Entity entity = client.world.getEntityById(entry.getKey());
//            if (entity instanceof LivingEntity && entity.isAlive()) {
//                long damageTickStart = entry.getValue();
//                long currentTick = client.world.getTime();
//
//                if (currentTick - damageTickStart < 10) { // Within damage tick
//                    double distance = Math.sqrt(entity.squaredDistanceTo(x, y, z));
//                    if (distance < 10.0) { // Explosion range
//                        addWarningMessage("Attack Too Fast", config.actionWarnings.attackTooFastSeverity);
//                        break;
//                    }
//                }
//            }
//        }
    }
    
    private static void checkAttackTooSlow(long currentTick) {
        if (!config.behaviorWarnings.attackTooSlowEnabled) return;
        
//        for (Map.Entry<Integer, Long> entry : entityDamageTicks.entrySet()) {
//            long damageTickStart = entry.getValue();
//            if (currentTick - damageTickStart == 10) { // Exactly when damage tick expires
//                Entity entity = MinecraftClient.getInstance().world.getEntityById(entry.getKey());
//                if (entity instanceof LivingEntity && entity.isAlive() && entity.distanceTo(MinecraftClient.getInstance().player) < 20.0) {
//                    addWarningMessage("Attack Too Slow", config.actionWarnings.attackTooSlowSeverity);
//                }
//            }
//        }
    }
    
    private static void checkWeakDamage(LivingEntity target, double damage) {
        if (!config.behaviorWarnings.weakDamageEnabled) return;
        
//        Long damageTickStart = entityDamageTicks.get(target.getId());
//        if (damageTickStart != null) {
//            long currentTick = MinecraftClient.getInstance().world.getTime();
//            if (currentTick - damageTickStart < 10) { // Target is in damage tick
//                // Check if damage is too weak (simplified threshold)
//                if (damage < 8.0) { // Less than typical crystal damage
//                    addWarningMessage("Weak Damage", config.actionWarnings.weakDamageSeverity);
//                }
//            }
//        }
    }

    private static void checkNotAnchoringWithTotem(LivingEntity target) {

    }

    private static void addWarningMessage(String message, CpvpCompanionConfig.WarningSeverity severity) {
        messageQueue.addFirst(new WarningMessage(message, severity, MinecraftClient.getInstance().world.getTime()));
    }

    private static WarningMessage addWarningMessageTracked(String message, CpvpCompanionConfig.WarningSeverity severity) {
        WarningMessage warningMessage = new WarningMessage(message, severity, Long.MAX_VALUE);
        messageQueue.addFirst(warningMessage);
        return warningMessage;
    }
    
    private static void updateMessages(long currentTick) {
        messageQueue.removeIf(message -> {
            long age = currentTick - message.creationTime;
            return age > config.behaviorWarnings.messageFadeEnd;
        });
    }
    
    public static void renderHud(DrawContext drawContext, float tickDelta) {
        if (!config.behaviorWarnings.enabled) return;
        if (messageQueue.isEmpty()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        int yOffset = 0;
        for (WarningMessage message : messageQueue) {
            int color = getSeverityColor(message.severity);
            float alpha = calculateMessageAlpha(message, client.world.getTime());
            
            if (alpha > 0) {
                drawContext.drawText(client.textRenderer, message.text, 
                        screenWidth / 2 - client.textRenderer.getWidth(message.text) / 2,
                        screenHeight / 2 + 20 + yOffset, color | ((int)(alpha * 255) << 24),
                        true);
                yOffset += 15;
            }
        }
    }
    
    private static int getSeverityColor(CpvpCompanionConfig.WarningSeverity severity) {
        return switch (severity) {
            case FATAL -> config.behaviorWarnings.fatalColor;
            case MEDIUM -> config.behaviorWarnings.mediumColor;
            case NORMAL -> config.behaviorWarnings.normalColor;
        };
    }
    
    private static float calculateMessageAlpha(WarningMessage message, long currentTick) {
        long age = currentTick - message.creationTime;
        int fadeStart = config.behaviorWarnings.messageFadeStart;
        int fadeEnd = config.behaviorWarnings.messageFadeEnd;
        
        if (age < fadeStart) return 1.0f;
        if (age >= fadeEnd) return 0.0f;
        
        return 1.0f - ((float)(age - fadeStart) / (fadeEnd - fadeStart));
    }
    
    private static class WarningMessage {
        final String text;
        final CpvpCompanionConfig.WarningSeverity severity;
        final long creationTime;
        
        WarningMessage(String text, CpvpCompanionConfig.WarningSeverity severity, long creationTime) {
            this.text = text;
            this.severity = severity;
            this.creationTime = creationTime;
        }
    }
    
    // Public methods for other managers to call
    public static void onPlayerAttack(LivingEntity target) {
        checkInDamageTickWarning(target);
    }
    
    public static void onBlockPlaced(BlockPos pos, ItemStack stack) {
        checkBadBlockPlacement(pos);
        
        if (stack.getItem() == Items.END_CRYSTAL) {
            checkBadCrystalPlacement(pos);
        }
    }
}