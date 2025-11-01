package com.brainbu.event;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;

public record PlayerAction(Type type, Entity entity, BlockState blockState, Hand hand, Direction side, BlockHitResult hitResult) {
    public enum Type {
        CrystalDestroy,
        AttackEntity, // due to Me
        TotemPop,
        ThrowPearl,    // due to Me
        InteractBlock, // due to Me
        AttackBlock;   // due to Me

        // can use Type.values()
    }

    PlayerAction(Type type, Entity entity) {
        this(type, entity, null, null, null, null);
        assert type == Type.CrystalDestroy ||
                type == Type.AttackEntity ||
                type == Type.TotemPop ||
                type == Type.ThrowPearl;
    }

    PlayerAction(Type type, BlockState blockState, Direction side) {
        this(type, null, blockState, null, side, null);
        assert type == Type.AttackBlock;
    }

    PlayerAction(Type type, Entity entity, BlockState blockState) {
        this(type, entity, blockState, null, null, null);
        assert type == Type.AttackBlock;
    }

    PlayerAction(Type type, BlockState blockState, Hand hand, BlockHitResult hitResult) {
        this(type, null, blockState, hand, hitResult.getSide(), hitResult);
        assert type == Type.AttackBlock;
    }
}
