package com.brainbu.mixin.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ControlsOptionsScreen.class)
public abstract class ExampleMixin extends GameOptionsScreen {

	public ExampleMixin(Screen parent, GameOptions gameOptions, Text title) {
		super(parent, gameOptions, title);
	}
}