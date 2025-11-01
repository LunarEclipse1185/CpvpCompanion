package com.brainbu.config;

import com.google.gson.Gson;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Config(name = "cpvp_companion")
public class CpvpCompanionConfig implements ConfigData {
    
    @ConfigEntry.Gui.Excluded
    public static CpvpCompanionConfig INSTANCE = new CpvpCompanionConfig();
    
    // Damage Tick Visualizer
    public DamageTickConfig damageTick = new DamageTickConfig();
    
    // Block Placement Indicator
    public BlockPlacementConfig blockPlacement = new BlockPlacementConfig();
    
    // Action Warnings
    public ActionWarningsConfig behaviorWarnings = new ActionWarningsConfig();
    
    public static class DamageTickConfig {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public GuiAnchor anchor = GuiAnchor.LEFT;

        @ConfigEntry.Gui.Tooltip
        public int maxDistance = 20;

        @ConfigEntry.Gui.Tooltip
        public int marginX = 20;

        @ConfigEntry.Gui.Tooltip
        public int barWidth = 120;

        @ConfigEntry.Gui.Tooltip
        public int barHeight = 12;

        @ConfigEntry.Gui.Tooltip
        public int rowSpacing = 16;

        @ConfigEntry.Gui.Tooltip
        public int horizontalSpacing = 16;

        @ConfigEntry.Gui.Tooltip
        public float faceSizeScale = 3f;

        @ConfigEntry.Gui.Tooltip
        public float totemIconScale = 1f;

        @ConfigEntry.Gui.Tooltip
        public float damageIconScale = 1.5f;

        @ConfigEntry.Gui.Tooltip
        public boolean renderTotemFirst = false;

        @ConfigEntry.ColorPicker
        public int tickBorderColor = 0xFF000000;
        
        @ConfigEntry.ColorPicker
        public int passedTickColor = 0xFFFF0000;
        
        @ConfigEntry.ColorPicker
        public int unpassedTickColor = 0xFF808080;
    }
    
    public static class BlockPlacementConfig {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = false;
        
        @ConfigEntry.ColorPicker
        public int outlineColor = 0xFF00FFFF;

        @ConfigEntry.ColorPicker
        public int fillColor = 0x3000FFFF;
    }
    
    public static class ActionWarningsConfig {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;
        
        // No Knockback Warning
        @ConfigEntry.Gui.Tooltip
        public boolean noKnockbackEnabled = true;
        @ConfigEntry.Gui.Tooltip
        public WarningSeverity noKnockbackSeverity = WarningSeverity.MEDIUM;
        
        // In Damage Tick Warning
        @ConfigEntry.Gui.Tooltip
        public boolean inDamageTickEnabled = true;
        @ConfigEntry.Gui.Tooltip
        public WarningSeverity inDamageTickSeverity = WarningSeverity.FATAL;
        
        // Bad Block Placement Warning
        @ConfigEntry.Gui.Tooltip
        public boolean badBlockPlacementEnabled = true;
        @ConfigEntry.Gui.Tooltip
        public WarningSeverity badBlockPlacementSeverity = WarningSeverity.FATAL;
        
        // Bad Crystal Placement Warning
        @ConfigEntry.Gui.Tooltip
        public boolean badCrystalPlacementEnabled = true;
        @ConfigEntry.Gui.Tooltip
        public WarningSeverity badCrystalPlacementSeverity = WarningSeverity.FATAL;
        
        // Attack Too Fast Warning
        @ConfigEntry.Gui.Tooltip
        public boolean attackTooFastEnabled = true;
        @ConfigEntry.Gui.Tooltip
        public WarningSeverity attackTooFastSeverity = WarningSeverity.MEDIUM;
        
        // Attack Too Slow Warning
        @ConfigEntry.Gui.Tooltip
        public boolean attackTooSlowEnabled = true;
        @ConfigEntry.Gui.Tooltip
        public WarningSeverity attackTooSlowSeverity = WarningSeverity.MEDIUM;
        
        // Weak Damage Warning
        @ConfigEntry.Gui.Tooltip
        public boolean weakDamageEnabled = true;
        @ConfigEntry.Gui.Tooltip
        public WarningSeverity weakDamageSeverity = WarningSeverity.MEDIUM;
        
        // Totem Warning
        @ConfigEntry.Gui.Tooltip
        public boolean totemWarningsEnabled = true;
        @ConfigEntry.Gui.Tooltip
        public int mainHandTotemSlot = 4;
        
        // Warning Colors
        @ConfigEntry.ColorPicker
        public int fatalColor = 0xFFFF0000;
        
        @ConfigEntry.ColorPicker
        public int mediumColor = 0xFFFFFF00;
        
        @ConfigEntry.ColorPicker
        public int normalColor = 0xFFFFFFFF;
        
        @ConfigEntry.Gui.Tooltip
        public int messageFadeStart = 20; // ticks
        
        @ConfigEntry.Gui.Tooltip
        public int messageFadeEnd = 30; // ticks
    }
    
    public enum GuiAnchor {
        LEFT("Left"),
        LEFT_OF_CURSOR("Left of Cursor"),
        RIGHT_OF_CURSOR("Right of Cursor"),
        RIGHT("Right");
        
        private final String displayName;
        
        GuiAnchor(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    public enum WarningSeverity {
        FATAL("Fatal"),
        MEDIUM("Medium"),
        NORMAL("Normal");
        
        private final String displayName;
        
        WarningSeverity(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    public static Screen getConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.translatable("title.cpvp_companion.config"))
            .setSavingRunnable(() -> {
                Gson gson = new Gson();
                String json =  gson.toJson(INSTANCE);
                //LOGGER.info("Serialized Config:\n" + json);
                File jsonFile = new File(FabricLoader.getInstance().getConfigDir().toString(), "cpvp_companion.json");
                try {
                    FileWriter writer = new FileWriter(jsonFile);
                    writer.write(json);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        
        ConfigCategory damageTick = builder.getOrCreateCategory(Text.translatable("category.cpvp_companion.damage_tick"));
        ConfigCategory blockPlacement = builder.getOrCreateCategory(Text.translatable("category.cpvp_companion.block_placement"));
        ConfigCategory behaviorWarnings = builder.getOrCreateCategory(Text.translatable("category.cpvp_companion.action_warnings"));
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        // Add entries for Damage Tick
        damageTick.addEntry(entryBuilder.startBooleanToggle(
            Text.translatable("option.cpvp_companion.damage_tick.enabled"),
            INSTANCE.damageTick.enabled
        ).setDefaultValue(true).setSaveConsumer(newValue -> INSTANCE.damageTick.enabled = newValue).build());

        damageTick.addEntry(entryBuilder.startEnumSelector(
                Text.translatable("option.cpvp_companion.damage_tick.anchor"),
                GuiAnchor.class,
                INSTANCE.damageTick.anchor
        ).setDefaultValue(GuiAnchor.LEFT).setSaveConsumer(newValue -> INSTANCE.damageTick.anchor = newValue).build());

        damageTick.addEntry(entryBuilder.startIntSlider(
            Text.translatable("option.cpvp_companion.damage_tick.max_distance"),
            INSTANCE.damageTick.maxDistance,
            5, 50
        ).setDefaultValue(20).setSaveConsumer(newValue -> INSTANCE.damageTick.maxDistance = newValue).build());

        damageTick.addEntry(entryBuilder.startIntField(
                Text.translatable("option.cpvp_companion.damage_tick.margin_x"),
                INSTANCE.damageTick.marginX
        ).setDefaultValue(20).setSaveConsumer(newValue -> INSTANCE.damageTick.marginX = newValue).build());

        damageTick.addEntry(entryBuilder.startIntField(
                Text.translatable("option.cpvp_companion.damage_tick.bar_width"),
                INSTANCE.damageTick.barWidth
        ).setDefaultValue(120).setSaveConsumer(newValue -> INSTANCE.damageTick.barWidth = newValue).build());

        damageTick.addEntry(entryBuilder.startIntField(
                Text.translatable("option.cpvp_companion.damage_tick.bar_height"),
                INSTANCE.damageTick.barHeight
        ).setDefaultValue(12).setSaveConsumer(newValue -> INSTANCE.damageTick.barHeight = newValue).build());

        damageTick.addEntry(entryBuilder.startIntField(
                Text.translatable("option.cpvp_companion.damage_tick.row_spacing"),
                INSTANCE.damageTick.rowSpacing
        ).setDefaultValue(16).setSaveConsumer(newValue -> INSTANCE.damageTick.rowSpacing = newValue).build());

        damageTick.addEntry(entryBuilder.startIntField(
                Text.translatable("option.cpvp_companion.damage_tick.horizontal_spacing"),
                INSTANCE.damageTick.horizontalSpacing
        ).setDefaultValue(16).setSaveConsumer(newValue -> INSTANCE.damageTick.horizontalSpacing = newValue).build());

        damageTick.addEntry(entryBuilder.startIntSlider(
                        Text.translatable("option.cpvp_companion.damage_tick.face_size_scale"),
                        (int) (INSTANCE.damageTick.faceSizeScale * 2),
                        1, 16
                ).setDefaultValue(6)
                .setTextGetter(value -> Text.of("" + value / 2f))
                .setSaveConsumer(newValue -> INSTANCE.damageTick.faceSizeScale = newValue / 2f).build());

        damageTick.addEntry(entryBuilder.startIntSlider(
                        Text.translatable("option.cpvp_companion.damage_tick.totem_scale"),
                        (int) (INSTANCE.damageTick.totemIconScale * 2),
                        1, 16
                ).setDefaultValue(2)
                .setTextGetter(value -> Text.of("" + value / 2f))
                .setSaveConsumer(newValue -> INSTANCE.damageTick.totemIconScale = newValue / 2f).build());

        damageTick.addEntry(entryBuilder.startIntSlider(
                        Text.translatable("option.cpvp_companion.damage_tick.damage_icon_scale"),
                        (int) (INSTANCE.damageTick.damageIconScale * 2),
                        1, 16
                ).setDefaultValue(3)
                .setTextGetter(value -> Text.of("" + value / 2f))
                .setSaveConsumer(newValue -> INSTANCE.damageTick.damageIconScale = newValue / 2f).build());

        damageTick.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("option.cpvp_companion.damage_tick.render_totem_first"),
                INSTANCE.damageTick.renderTotemFirst
        ).setDefaultValue(false).setSaveConsumer(newValue -> INSTANCE.damageTick.renderTotemFirst = newValue).build());

        damageTick.addEntry(entryBuilder.startAlphaColorField(
                Text.translatable("option.cpvp_companion.damage_tick.tick_border_color"),
                INSTANCE.damageTick.tickBorderColor
        ).setDefaultValue(0xFF000000).setSaveConsumer(newValue -> INSTANCE.damageTick.tickBorderColor = newValue).build());

        damageTick.addEntry(entryBuilder.startAlphaColorField(
                Text.translatable("option.cpvp_companion.damage_tick.passed_tick_color"),
                INSTANCE.damageTick.passedTickColor
        ).setDefaultValue(0xFFFF0000).setSaveConsumer(newValue -> INSTANCE.damageTick.passedTickColor = newValue).build());

        damageTick.addEntry(entryBuilder.startAlphaColorField(
                Text.translatable("option.cpvp_companion.damage_tick.unpassed_tick_color"),
                INSTANCE.damageTick.unpassedTickColor
        ).setDefaultValue(0xFF808080).setSaveConsumer(newValue -> INSTANCE.damageTick.unpassedTickColor = newValue).build());


        // Add entries for Block Placement
        blockPlacement.addEntry(entryBuilder.startBooleanToggle(
            Text.translatable("option.cpvp_companion.block_placement.enabled"),
            INSTANCE.blockPlacement.enabled
        ).setDefaultValue(true).setSaveConsumer(newValue -> INSTANCE.blockPlacement.enabled = newValue).build());

        blockPlacement.addEntry(entryBuilder.startAlphaColorField(
                Text.translatable("option.cpvp_companion.block_placement.outline_color"),
                INSTANCE.blockPlacement.outlineColor
        ).setDefaultValue(0xFF00FFFF).setSaveConsumer(newValue -> INSTANCE.blockPlacement.outlineColor = newValue).build());

        blockPlacement.addEntry(entryBuilder.startAlphaColorField(
                Text.translatable("option.cpvp_companion.block_placement.fill_color"),
                INSTANCE.blockPlacement.fillColor
        ).setDefaultValue(0x3000FFFF).setSaveConsumer(newValue -> INSTANCE.blockPlacement.fillColor = newValue).build());


        // Add entries for Action Warnings
        behaviorWarnings.addEntry(entryBuilder.startBooleanToggle(
            Text.translatable("option.cpvp_companion.behavior_warnings.enabled"),
            INSTANCE.behaviorWarnings.enabled
        ).setDefaultValue(true).setSaveConsumer(newValue -> INSTANCE.behaviorWarnings.enabled = newValue).build());

        behaviorWarnings.addEntry(entryBuilder.startAlphaColorField(
                Text.translatable("option.cpvp_companion.behavior_warnings.fatal_color"),
                INSTANCE.behaviorWarnings.fatalColor
        ).setDefaultValue(0xFFFF0000).setSaveConsumer(newValue -> INSTANCE.behaviorWarnings.fatalColor = newValue).build());

        behaviorWarnings.addEntry(entryBuilder.startAlphaColorField(
                Text.translatable("option.cpvp_companion.behavior_warnings.medium_color"),
                INSTANCE.behaviorWarnings.mediumColor
        ).setDefaultValue(0xFFFFFF00).setSaveConsumer(newValue -> INSTANCE.behaviorWarnings.mediumColor = newValue).build());

        behaviorWarnings.addEntry(entryBuilder.startAlphaColorField(
                Text.translatable("option.cpvp_companion.behavior_warnings.normal_color"),
                INSTANCE.behaviorWarnings.normalColor
        ).setDefaultValue(0xFFFFFFFF).setSaveConsumer(newValue -> INSTANCE.behaviorWarnings.normalColor = newValue).build());

        behaviorWarnings.addEntry(entryBuilder.startIntField(
                Text.translatable("option.cpvp_companion.behavior_warnings.message_fade_start"),
                INSTANCE.behaviorWarnings.messageFadeStart
        ).setDefaultValue(20).setSaveConsumer(newValue -> INSTANCE.behaviorWarnings.messageFadeStart = newValue).build());

        behaviorWarnings.addEntry(entryBuilder.startIntField(
                Text.translatable("option.cpvp_companion.behavior_warnings.message_fade_end"),
                INSTANCE.behaviorWarnings.messageFadeEnd
        ).setDefaultValue(30).setSaveConsumer(newValue -> INSTANCE.behaviorWarnings.messageFadeEnd = newValue).build());

        behaviorWarnings.addEntry(entryBuilder.startBooleanToggle(
                Text.translatable("option.cpvp_companion.behavior_warnings.totem.enabled"),
                INSTANCE.behaviorWarnings.totemWarningsEnabled
        ).setDefaultValue(true).setSaveConsumer(newValue -> INSTANCE.behaviorWarnings.totemWarningsEnabled = newValue).build());

        behaviorWarnings.addEntry(entryBuilder.startIntField(
            Text.translatable("option.cpvp_companion.behavior_warnings.totem.slot"),
            INSTANCE.behaviorWarnings.mainHandTotemSlot + 1
        ).setDefaultValue(5)
                .setErrorSupplier(slot -> slot < 1 || slot > 9 ?
                        java.util.Optional.of(Text.of("Invalid Slot Number")) :
                        java.util.Optional.empty())
                .setSaveConsumer(newValue -> INSTANCE.behaviorWarnings.mainHandTotemSlot = newValue - 1).build());
        
        return builder.build();
    }
}