package com.nova.module.impl;

import com.nova.module.BoolSetting;
import com.nova.module.Module;
import net.minecraft.client.MinecraftClient;

public class AutoWalk extends Module {
	private final BoolSetting sprint = add(new BoolSetting("Sprint", false));
	private boolean sprintHeld;

	public AutoWalk() {
		super("Auto Walk", "Holds the forward key for you. Opening any screen stops it.", Category.MOVEMENT);
	}

	@Override
	public boolean persistsEnabled() {
		return false;
	}

	@Override
	public void onTick() {
		MinecraftClient mc = mc();
		if (mc.currentScreen != null) {
			return;
		}
		mc.options.forwardKey.setPressed(true);
		if (sprint.get()) {
			mc.options.sprintKey.setPressed(true);
			sprintHeld = true;
		} else if (sprintHeld) {
			mc.options.sprintKey.setPressed(false);
			sprintHeld = false;
		}
	}

	@Override
	protected void onDisable() {
		MinecraftClient mc = mc();
		if (mc.options != null) {
			mc.options.forwardKey.setPressed(false);
			if (sprintHeld) {
				mc.options.sprintKey.setPressed(false);
			}
		}
		sprintHeld = false;
	}
}
