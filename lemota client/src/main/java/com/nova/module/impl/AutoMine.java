package com.nova.module.impl;

import com.nova.module.BoolSetting;
import com.nova.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;

public class AutoMine extends Module {
	private final BoolSetting onlyBlocks = add(new BoolSetting("Only When Aiming At Block", true));
	private boolean holding;

	public AutoMine() {
		super("Auto Mine", "Holds the attack button so you keep mining the block you look at.", Category.WORLD);
	}

	@Override
	public boolean persistsEnabled() {
		return false;
	}

	@Override
	public void onTick() {
		MinecraftClient mc = mc();
		if (mc.currentScreen != null) {
			release(mc);
			return;
		}
		boolean aimingAtBlock = mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK;
		if (onlyBlocks.get() && !aimingAtBlock) {
			release(mc);
			return;
		}
		mc.options.attackKey.setPressed(true);
		holding = true;
	}

	@Override
	protected void onDisable() {
		release(mc());
	}

	private void release(MinecraftClient mc) {
		if (holding && mc.options != null) {
			mc.options.attackKey.setPressed(false);
		}
		holding = false;
	}
}
