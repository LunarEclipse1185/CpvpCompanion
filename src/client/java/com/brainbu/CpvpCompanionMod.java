package com.brainbu;

import com.brainbu.config.ConfigManager;
//import com.brainbu.render.FaceHighlight.ModBlockEntities;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;


public class CpvpCompanionMod implements ClientModInitializer {
    public static final String MOD_ID = "cpvp_companion";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        ConfigManager.init();
//        ModBlockEntities.initialize();
        ClientEvents.registerEvents();
        LOGGER.info("CPVP Companion initialized successfully!");
    }
}