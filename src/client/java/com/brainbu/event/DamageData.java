package com.brainbu.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.util.math.Vec3d;

public record DamageData(DamageType type, Entity receiver, Entity source, Entity attacker, Vec3d position) {
    DamageData(DamageType type, LivingEntity receiver, Entity source, Entity attacker) {
        this(type, receiver, source, attacker, null);
    }
    DamageData(DamageSource source, Entity receiver) {
        this(source.getType(), receiver, source.getSource(), source.getAttacker(), source.getPosition());
    }

    DamageType getType() {
        return type;
    }
    Entity getReceiver() {
        return receiver;
    }
//    Entity getSource() {
//        return source;
//    }
    Entity getAttacker() {
        return attacker;
    }
    Vec3d getPosition() {
        return position;
    }
}
