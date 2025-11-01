package com.brainbu.event;

public record CombatEvent(Type type, DamageData damageData) {
    /**
     * Any crystal explosions. Note that the attacker is not necessarily a player.
     * Damage dealt to a player by a Crystal.
     * Damage dealt to a player by an Anchor (technically possibly a bed).
     * Knockback I/II melee damage dealt to a player.
     * Melee damage dealt to a player except the previous case.
     * Player totem pops.
     * Fire.
     * Pearl / ender pearl damage.
     */
    public enum Type {
        CrystalPunch,
        AnchorBlow,

        CrystalHit,
        AnchorHit,
        KnockbackHit,
        WeakMeleeHit,
        TotemPop,
        FireDamage,
        PearlDamage;
    }
}