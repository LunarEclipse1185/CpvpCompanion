package com.brainbu;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CpvpCompanionMod implements ClientModInitializer {
    public static final String MOD_ID = "cpvp_companion";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("CPVP Companion is initializing!");
    }
}