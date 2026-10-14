package com.nova.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
	public enum Category {
		RENDER("Render"),
		MOVEMENT("Movement"),
		WORLD("World");

		public final String label;

		Category(String label) {
			this.label = label;
		}
	}

	public final String name;
	public final String description;
	public final Category category;
	public final List<Setting> settings = new ArrayList<>();

	/** Animation progress used by the GUI (0 = off, 1 = on). */
	public float anim;

	private boolean enabled;

	protected Module(String name, String description, Category category) {
		this.name = name;
		this.description = description;
		this.category = category;
	}

	protected <T extends Setting> T add(T setting) {
		settings.add(setting);
		return setting;
	}

	/** Name without spaces, used for config keys and key bindings. */
	public String id() {
		return name.replace(" ", "");
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		if (value) {
			onEnable();
		} else {
			onDisable();
		}
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	/** Whether the on/off state is remembered between game launches. */
	public boolean persistsEnabled() {
		return true;
	}

	/** ESP modules ask for the FOV effects to be switched off so the boxes line up exactly. */
	public boolean wantsStableFov() {
		return false;
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	/** Called once per client tick while the module is on and a world is loaded. */
	public void onTick() {
	}

	/** Called every frame while the module is on and a world is loaded. */
	public void onHud(DrawContext context, float tickProgress) {
	}

	protected static MinecraftClient mc() {
		return MinecraftClient.getInstance();
	}
}
