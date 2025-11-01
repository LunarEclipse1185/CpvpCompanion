package com.brainbu.render;

import com.brainbu.event.*;
import com.brainbu.config.CpvpCompanionConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;

import java.time.Instant;
import java.util.*;

import static com.brainbu.CpvpCompanionMod.MOD_ID;
import static java.lang.Math.*;
import static net.minecraft.util.math.MathHelper.square;

public class TickListManager {

    static CpvpCompanionConfig.DamageTickConfig config = CpvpCompanionConfig.INSTANCE.damageTick;

    static final int baseFaceSize = 8;
//    public static void init() {
//        NativeImage image = NativeImage.
//    }

    public static void endClientTick(MinecraftClient client) {
    }
    
    public static void renderHud(DrawContext context, float tickDelta) {
        if (!CpvpCompanionConfig.INSTANCE.damageTick.enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        List<AbstractClientPlayerEntity> players = getNearbyPlayers(client.player);

        int x = calculateAnchorX(client);
        int y = calculateAnchorY(client);

        for (AbstractClientPlayerEntity p : players) {
            renderRow(context, p, x, y);
            y += config.faceSizeScale * baseFaceSize + config.rowSpacing;
        }
    }
    
    public static List<AbstractClientPlayerEntity> getNearbyPlayers(PlayerEntity player) {
        int maxDistance = CpvpCompanionConfig.INSTANCE.damageTick.maxDistance;
        List<AbstractClientPlayerEntity> nearby =
                player.getWorld().getEntitiesByType(
                        TypeFilter.instanceOf(AbstractClientPlayerEntity.class),
                        Box.of(player.getPos(), maxDistance, maxDistance, maxDistance),
                        player1 -> square(player1.getX() - player.getX()) + square(player1.getZ() - player.getZ()) < maxDistance * maxDistance
                );
        //Box.raycast()

        // Sort by distance
        nearby.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)));
        return nearby;
    }

    private static int calculateAnchorX(MinecraftClient client) {
        int screenWidth = client.getWindow().getScaledWidth();
        int hudWidth = (int) (config.horizontalSpacing + config.faceSizeScale * baseFaceSize + config.barWidth);

        return switch (config.anchor) {
            case LEFT -> config.marginX;
            case LEFT_OF_CURSOR -> screenWidth / 2 - hudWidth - config.marginX;
            case RIGHT_OF_CURSOR -> screenWidth / 2 + config.marginX;
            case RIGHT -> screenWidth - hudWidth - config.marginX;
        };
    }
    
    private static int calculateAnchorY(MinecraftClient client) {
        int screenHeight = client.getWindow().getScaledHeight();
        int rowHeight = (int) max(config.faceSizeScale * baseFaceSize, config.barHeight);
        
        // Center vertically around cursor for cursor-relative anchors
        return screenHeight / 2 - getNearbyPlayers(client.player).size() *
                (config.rowSpacing + rowHeight) / 2 + config.rowSpacing / 2;
    }

    // the creation Real-life time of the indicator (DamageData or PlayerAction)
//    private record DamageIndicator(long parent, Object event, DamageIcon icon, int offset, long time) {
//        public boolean isOf(Object o) {
//            return (o instanceof PlayerAction || o instanceof CombatEvent) && event == o;
//        }
//    }
//    private static final List<DamageIndicator> indicators = new ArrayList<>();
    private static void renderRow(DrawContext context, AbstractClientPlayerEntity player, int x, int y) {
        int faceSize = (int) (config.faceSizeScale * baseFaceSize);
        int rowHeight = max(faceSize, config.barHeight);

        int barX = x + faceSize + config.horizontalSpacing;
        int barY = y + (rowHeight - config.barHeight) / 2;
        int barWidth = config.barWidth;
        int barMaxX = barX + barWidth;
        int barHeight = config.barHeight;
        int barMaxY = barY + barHeight;

        int faceX = x;
        int faceY = y + (rowHeight - faceSize) / 2;

        PlayerSkinDrawer.draw(context, player.getSkinTextures().texture(), faceX+1, faceY+1, faceSize, false, false, 0xFF303030);
        PlayerSkinDrawer.draw(context, player.getSkinTextures().texture(), faceX, faceY, faceSize, false, false, -1);

        int playerId = player.getId();
        WithTick<List<WithTick<CombatEvent>>> tickData = CombatEventsManager.getInstance().activeDamageTicks
                .getOrDefault(playerId, null);
        if (tickData == null) return;

        long currentTick = MinecraftClient.getInstance().world.getTime();
        int ticksPassed = (int) (currentTick - tickData.tick);
//        Instant instant = Instant.now();
//        long currentTime = instant.toEpochMilli();
        // NOTICE use Util.getMeasuringTimeMs() instead!


        // Shadow
        context.fill(barX+1, barY+1+barHeight/4, barMaxX + 1, barMaxY+1-barHeight/4, 0xFF303030);
        // Render bar
        int borderColor = config.tickBorderColor;
        for (int i = 0; i < 10; ++i) {
            int tickX = barX + barWidth * i / 9;
            int tickMaxX = barX + barWidth * (i + 1) / 9;
            boolean isPassed = i < ticksPassed;
            int color = isPassed ? config.passedTickColor : config.unpassedTickColor;

            // Draw background
            context.fill(tickX, barY + barHeight/4, barMaxX, barMaxY - barHeight/4, color);
            // separator
            context.fill(tickX, barY, tickX + 1, barMaxY, borderColor);
        }

        if (config.renderTotemFirst) {
            renderTotems(context, playerId, tickData, barX, barY, barWidth, barHeight);
            renderDamageIcons(context, playerId, tickData, barX, barY, barWidth, barHeight);
        } else {
            renderDamageIcons(context, playerId, tickData, barX, barY, barWidth, barHeight);
            renderTotems(context, playerId, tickData, barX, barY, barWidth, barHeight);
        }
    }

    private static void renderTotems(DrawContext context,
                                     int playerId,
                                     WithTick<List<WithTick<CombatEvent>>> tickData,
                                     int barX, int barY,
                                     int barWidth, int barHeight) {
        // Render totem pops
        float totemScale = config.totemIconScale;

        CombatEventsManager.getInstance().playerActions.forEach((tick, list) -> {
            list.forEach(action -> {
                if (action.type() != PlayerAction.Type.TotemPop) return;
                if (action.entity().getId() != playerId) return;
                DamageIcon icon = DamageIcon.TOTEM;
                int tickDelta = (int) (tick - tickData.tick);
                if (tickDelta >= 0 && tickDelta < 10) {
//                    if(indicators.stream().noneMatch(indicator -> indicator.isOf(action))) {
//                        indicators.add(new DamageIndicator(tick, action, icon, tickDelta, currentTime));
//                    }
//                    context.getMatrices().translate(0, 0, -1);
                    renderTextureScaledCentered16(context, icon.texture, barX + tickDelta * barWidth / 9 + 1, barY + barHeight / 2 + 1,
                            totemScale, 0xFF303030);
//                    context.getMatrices().translate(0, 0, 1);
                    renderTextureScaledCentered16(context, icon.texture, barX + tickDelta * barWidth / 9, barY + barHeight / 2,
                            totemScale, -1);
                }
            });
        });
    }

    private static void renderDamageIcons(DrawContext context,
                                          int playerId,
                                          WithTick<List<WithTick<CombatEvent>>> tickData,
                                          int barX, int barY,
                                          int barWidth, int barHeight) {
        // Render damage icons
        float damageIconScale = config.damageIconScale;


        for (WithTick<CombatEvent> event : tickData.content) {
            if (event.content.damageData().receiver().getId() != playerId) return;
            int tickDelta = (int) (event.tick - tickData.tick);
            if (tickDelta >= 0 && tickDelta < 10) {
                DamageIcon icon = getDamageIcon(event.content.type());
                if (icon == null) continue;
//                int tint = event.content.type() == CombatEvent.Type.OtherMeleeHit ? 0xFFFF0000 : -1;
//                context.getMatrices().translate(0, 0, -1);
                renderTextureScaledCentered16(context, icon.texture, barX + tickDelta * barWidth / 9 + 1, barY + barHeight / 2 + 1,
                        damageIconScale, 0xFF303030);
//                context.getMatrices().translate(0, 0, 1);
                renderTextureScaledCentered16(context, icon.texture, barX + tickDelta * barWidth / 9, barY + barHeight / 2,
                        damageIconScale, -1);
//                if (indicators.stream().noneMatch(indicator -> indicator.isOf(event.content))) {
//                    indicators.add(new DamageIndicator(tickData.tick, event.content, icon, tickDelta, currentTime));
//                }
            }
        }
    }

    private static void renderAnimatedIcons() {
//        final long durationMs = 200;
//        indicators.removeIf(indicator ->
//                CombatEventsManager.getInstance().activeDamageTicks.values().stream().noneMatch(entry ->
//                        entry.tick == indicator.parent)
//        );
//        for (DamageIndicator indicator : indicators) {
//            if (indicator.event instanceof CombatEvent) {
//                float progress = (float) (currentTime - indicator.time) / durationMs;
//                float scale = clamp(lerp(progress, 0.6f, 0.4f), 0.4f, 0.6f);
//                int color = 0xFFFFFF | (clamp(lerp(progress, 0xCF, 0xFF), 0xCF, 0xFF) << 6);
//                renderTextureScaledCentered16(context, indicator.icon.texture, barX + indicator.offset * barWidth / 9, barY + 4,
//                        0.5f, -1);
//            }
//            if (indicator.event instanceof PlayerAction action && action.type() == PlayerAction.Type.TotemPop
////                    && indicator.time + durationMs > currentTime
//            ) {
//                float progress = (float) (currentTime - indicator.time) / durationMs;
//                float scale = clamp(lerp(progress, 0.6f, 1f), 0.6f, 1f);
//                int color = 0xFFFFFF | (clamp(lerp(progress, 0xFF, 0x00), 0x00, 0xFF) << 6);
//                renderTextureScaledCentered16(context, indicator.icon.texture, barX + indicator.offset * barWidth / 9, barY + 4,
//                        1f, -1);
//            }
//        }
    }

    private static DamageIcon getDamageIcon(CombatEvent.Type type) {
        return switch (type) {
            case CrystalPunch, CrystalHit -> DamageIcon.END_CRYSTAL;
            case AnchorBlow, AnchorHit -> DamageIcon.ANCHOR_TOP;
            case KnockbackHit -> DamageIcon.SWORD;
            case WeakMeleeHit -> DamageIcon.WEAK;
            case FireDamage -> DamageIcon.FIRE;
            case PearlDamage -> DamageIcon.PEARL;
            default -> null;
        };
    }

    public static void renderTextureScaled(DrawContext context, Identifier textureId, int x, int y, float scale, int tint) {
        final int textureSize = 16;
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0.0);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawTexture(
                RenderLayer::getGuiTextured,
                textureId,
                0, // x
                0, // y
                0, // u
                0, // v
                textureSize, // width of region to draw
                textureSize, // height of region to draw
                textureSize, // full width of the texture file
                textureSize, // full height of the texture file
                tint
        );
        context.getMatrices().pop();
    }
    public static void renderTextureScaledCentered16(DrawContext context, Identifier textureId, int x, int y, float scale, int tint) {
        final int textureSize = 16;
        renderTextureScaled(context, textureId, round(x - scale * textureSize / 2), round(y - scale * textureSize / 2), scale, tint);
    }

    private enum DamageIcon {
        END_CRYSTAL("end_crystal.png"),
        ANCHOR_TOP("anchor.png"),
        SWORD("strength.png"),
        WEAK("weakness.png"),
        TOTEM("totem.png"),
        FIRE("fire.png"),
        PEARL("pearl.png");

        public final Identifier texture;

        private DamageIcon(String fileName) {
            texture = Identifier.of(MOD_ID, "textures/damage_icon/" + fileName);
        }
    }
}