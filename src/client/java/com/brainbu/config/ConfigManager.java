package com.brainbu.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import com.brainbu.CpvpCompanionMod;

public class ConfigManager {
    private static ConfigHolder<CpvpCompanionConfig> configHolder;
    
    public static void init() {
        AutoConfig.register(CpvpCompanionConfig.class, GsonConfigSerializer::new);
        configHolder = AutoConfig.getConfigHolder(CpvpCompanionConfig.class);
        
        // Set static instance
        CpvpCompanionConfig.INSTANCE = configHolder.getConfig();
        
        CpvpCompanionMod.LOGGER.info("CPVP Companion Config initialized");
    }
    
    public static CpvpCompanionConfig getConfig() {
        return configHolder.getConfig();
    }
    
    public static void save() {
        configHolder.save();
    }
    
    public static void load() {
        configHolder.load();
    }
}