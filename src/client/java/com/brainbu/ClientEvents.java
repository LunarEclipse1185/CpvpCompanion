package com.brainbu;

import com.brainbu.event.CombatEventsManager;
import com.brainbu.render.BehaviorWarningManager;
import com.brainbu.render.BlockPlacementManager;
import com.brainbu.render.TickListManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.util.Identifier;

import static com.brainbu.CpvpCompanionMod.MOD_ID;

public class ClientEvents {
    public static void registerEvents() {
        // HUD rendering
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
                layeredDrawer.attachLayerAfter(IdentifiedLayer.MISC_OVERLAYS,
                        Identifier.of(MOD_ID, "layer"),
                        (context, tickCounter) -> {
                            TickListManager.renderHud(context, tickCounter.getTickDelta(false));
                            BehaviorWarningManager.renderHud(context, tickCounter.getTickDelta(true));
                        })
        );
        
        // World rendering
        WorldRenderEvents.AFTER_ENTITIES.register((worldRenderContext) -> {
            try {
                BlockPlacementManager.renderWorld(worldRenderContext);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        // Network events for combat tracking
//        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
//            CombatEventsManager.getInstance().registerPacketListeners(client); // before all else so the lists are ready

            // TickListManager.registerPacketListeners(client);
//            BehaviorWarningManager.registerPacketListeners(client);
//        });

        // Client tick events
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CombatEventsManager.getInstance().endClientTick(client);

            TickListManager.endClientTick(client);
            BlockPlacementManager.endClientTick(client);
            BehaviorWarningManager.endClientTick(client);
        });
    }
}