package com.brainbu.event;

import com.brainbu.render.TickListManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;

import java.util.*;

import static com.brainbu.CpvpCompanionMod.LOGGER;

/**
 * manages actions, while deciding some events on the site and compute to get exact combat events when a tick1 ends.
 */
public class CombatEventsManager {

    public final Map<Long, List<PlayerAction>> playerActions = new HashMap<>();

    public final Map<Long, List<CombatEvent>> combatEvents = new HashMap<>();

    private void addCombatEvent(CombatEvent.Type type, DamageData data) {
        long currentTick = MinecraftClient.getInstance().world.getTime();
        addCombatEvent(currentTick, type, data);
    }
    private void addCombatEvent(long currentTick, CombatEvent.Type type, DamageData data) {
        combatEvents.compute(currentTick, (tick1, list) -> {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(new CombatEvent(type, data));
            return list;
        });

        if (data == null) return;
        if (!(data.getReceiver() instanceof PlayerEntity receiver)) return;

        activeDamageTicks.compute(data.getReceiver().getId(), (id, record) -> {
            combatEvents.compute(currentTick, (tick1, list) -> {
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(new CombatEvent(type, data));
                return list;
            });

            WithTick<CombatEvent> entry = new WithTick<>(currentTick, new CombatEvent(type, data));
            if (record == null || currentTick - record.tick >= 10) {
                List<WithTick<CombatEvent>> newList = new ArrayList<>();
                newList.add(entry);
//                LOGGER.info("New Damage Tick for player id " + data.getReceiver().getId());
                return new WithTick<>(currentTick, newList);
            } else {
                record.content.add(entry);
                if (currentTick - record.tick > 10) {
                    record.tick = currentTick;
                }
                return record;
            }
        });
    }

    private void addPlayerAction(PlayerAction action) {
        long currentTick = MinecraftClient.getInstance().world.getTime();
        playerActions.compute(currentTick, (tick, list) -> {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(action);
//            LOGGER.info("added " + currentTick + " " + action);
            return list;
        });
    }

    /**
     * Ongoing damage ticks. Each exists for a certain amount of currentTick after expires before cleaned.
     * Key=PlayerId, first tick1 = start currentTick, second tick1 = damage currentTick
     */
    public final Map<Integer, WithTick<List<WithTick<CombatEvent>>>> activeDamageTicks = new HashMap<>();


    private static CombatEventsManager instance = null;

    private CombatEventsManager() {
    }

    public static CombatEventsManager getInstance() {
        if (instance == null) {
            instance = new CombatEventsManager();
        }
        return instance;
    }


//    public void registerPacketListeners(MinecraftClient client) {
//        if (!(client.getNetworkHandler() instanceof INetworkMixin mixin)) return;
//
//        mixin.registerOnDamage((packet) -> {
//            ClientWorld world = MinecraftClient.getInstance().world;
//            DamageSource source = packet.createDamageSource(world);
//            dispatchDamage(world, source);
//        });
//    }


    public void endClientTick(MinecraftClient client) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return;
        long currentTick = world.getTime();
        long delta = 100; // 5s before disappearing

        playerActions.remove(currentTick - delta);
        combatEvents.remove(currentTick - delta);
        activeDamageTicks.entrySet().removeIf((entry) -> currentTick - entry.getValue().tick > 20);
    }

    public void handleDamage(DamageSource source, Entity receiver) {
//        ClientWorld world = MinecraftClient.getInstance().world;
//        if (world == null) return;
        DamageData damageData = new DamageData(source, receiver);

        if (!(receiver instanceof PlayerEntity)) return;
        checkCrystalHit(damageData);
        checkAnchorHit(damageData);
        checkFireDamage(damageData);
        checkPearlDamage(damageData);
        checkMeleeHit(damageData);
    }

    public void handleCrystalDestroy(EndCrystalEntity crystal) {
        addPlayerAction(new PlayerAction(PlayerAction.Type.CrystalDestroy, crystal));
    }

    double crystalImpactRadius = 10.2;
    public void handleAttackEntity(DamageSource source, Entity receiver) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return;
        long currentTick = world.getTime();
        PlayerEntity player = MinecraftClient.getInstance().player;

        addPlayerAction(new PlayerAction(PlayerAction.Type.AttackEntity, receiver));

        // crystal hit
        if (receiver instanceof EndCrystalEntity) {
            List<AbstractClientPlayerEntity> players = TickListManager.getNearbyPlayers(player);
            players.stream()
                    .filter(player1 -> player1.getPos().distanceTo(receiver.getPos()) <= crystalImpactRadius)
                    .forEach(player1 -> {
//                        LOGGER.error("new type of crystal");
                        DamageData damageData = new DamageData(DamageTypeHelper.of("player_explosion"), player1, null, null);
                        addCombatEvent(currentTick + 1, CombatEvent.Type.CrystalHit, damageData);
                    });
            addCombatEvent(currentTick + 1, CombatEvent.Type.CrystalPunch, null);
            // plus 1 because we are catching client initiated events and predicting the result of server initiated events
            // which are 1 tick later.

        }
        // melee hit
        if (receiver instanceof PlayerEntity player1) {
            WithTick<List<WithTick<CombatEvent>>> entry = activeDamageTicks.get(player1.getId());
            DamageData damageData = new DamageData(DamageTypeHelper.of("player"), player1, null, player);
            CombatEvent.Type type = CombatEvent.Type.WeakMeleeHit;
            if (entry == null || entry.tick + 10 <= currentTick + 1) {
                ItemStack mainHandStack = player.getMainHandStack();
                if (hasKnockback(mainHandStack)) {
                    type = CombatEvent.Type.KnockbackHit;
                }
            }
            addCombatEvent(currentTick + 1, type, damageData);
        }
    }

    double anchorImpactRadius = 8.4;
    // both hands will trigger this function
    public void handleInteractBlock(PlayerEntity player, Hand hand, BlockState blockState, BlockHitResult hitResult) {
        addPlayerAction(new PlayerAction(PlayerAction.Type.InteractBlock, blockState, hand, hitResult));

        long currentTick = MinecraftClient.getInstance().world.getTime();
        ItemStack mainStack = player.getMainHandStack();
        ItemStack offStack = player.getOffHandStack();

        if (blockState.getBlock() instanceof RespawnAnchorBlock &&
                hand == Hand.MAIN_HAND && // just to pick one branch
                hitResult.getType() == HitResult.Type.BLOCK &&
                blockState.get(RespawnAnchorBlock.CHARGES) >= 1 &&
                mainStack.getItem() != Blocks.GLOWSTONE.asItem() &&
                offStack.getItem() != Blocks.GLOWSTONE.asItem()
        ) {
            List<AbstractClientPlayerEntity> players = TickListManager.getNearbyPlayers(player);
            players.stream()
                    .filter(player1 -> player1.getPos().distanceTo(hitResult.getPos()) <= anchorImpactRadius)
                    .forEach(player1 -> {
//                        LOGGER.error("new type of anchor");
                        DamageData damageData = new DamageData(DamageTypeHelper.of("bad_respawn_point"), player1, null, null);
                        addCombatEvent(currentTick + 1, CombatEvent.Type.AnchorHit, damageData);
                    });
            addCombatEvent(currentTick + 1, CombatEvent.Type.AnchorBlow, null);
        }
    }

    public void handleAttackBlock(BlockState blockState, Direction side) {
        addPlayerAction(new PlayerAction(PlayerAction.Type.AttackBlock, blockState, side));
    }

    public void handleTotemPop(Entity receiver) { // here receiver must be the client player
        addPlayerAction(new PlayerAction(PlayerAction.Type.TotemPop, receiver));
    }

    // below guaranteed received by a player
    private void checkCrystalHit(DamageData damageData) {
        if (!Objects.equals(damageData.getType().msgId(), "explosion") &&
            !Objects.equals(damageData.getType().msgId(), "explosion.player")
        ) return;
//        LOGGER.info("Crystal Hit " + damageData.getAttacker().getName() + " " + damageData.getReceiver().getName());
        addCombatEvent(CombatEvent.Type.CrystalHit, damageData);
    }

    private void checkAnchorHit(DamageData damageData) {
        if (!Objects.equals(damageData.getType().msgId(), "badRespawnPoint")) return;
//        LOGGER.info("Anchor Hit " + damageData.getAttacker() + " " + damageData.getReceiver().getName());
        addCombatEvent(CombatEvent.Type.AnchorHit, damageData);
    }

    private void checkFireDamage(DamageData damageData) {
        if (!Objects.equals(damageData.getType().msgId(), "inFire") &&
                !Objects.equals(damageData.getType().msgId(), "onFire") &&
                !Objects.equals(damageData.getType().msgId(), "lava")
        ) return;
//        LOGGER.info("Fire " + damageData.getAttacker() + " " + damageData.getReceiver().getName());
        addCombatEvent(CombatEvent.Type.FireDamage, damageData);
    }

    private void checkPearlDamage(DamageData damageData) {
        if (!Objects.equals(damageData.getType().msgId(), "enderPearl") &&
                !Objects.equals(damageData.getType().msgId(), "fall")
        ) return;
//        LOGGER.info("Pearl " + damageData.getAttacker() + " " + damageData.getReceiver().getName());
        addCombatEvent(CombatEvent.Type.PearlDamage, damageData);
    }

    private void checkMeleeHit(DamageData damageData) {
        if (!Objects.equals(damageData.getType().msgId(), "player")) return;
        ItemStack weaponStack = damageData.getAttacker().getWeaponStack();
        addCombatEvent(hasKnockback(weaponStack) ? CombatEvent.Type.KnockbackHit : CombatEvent.Type.WeakMeleeHit, damageData);
//        LOGGER.info("Melee " + level + " " + damageData.getAttacker() + " " + damageData.getReceiver().getName());
    }

    private boolean hasKnockback(ItemStack itemStack) {
        Registry<Enchantment> enchantmentRegistry = MinecraftClient.getInstance().world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT).orElse(null);
        Enchantment knockbackEnchantment = enchantmentRegistry.get(Enchantments.KNOCKBACK);
        RegistryEntry<Enchantment> knockbackEntry = enchantmentRegistry.getEntry(knockbackEnchantment);
        int level = itemStack == null ? 0 : EnchantmentHelper.getLevel(knockbackEntry, itemStack);

        return level >= 1;
    }
}
