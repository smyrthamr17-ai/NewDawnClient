package com.nova.module;

import com.google.gson.JsonObject;

public class BoolSetting extends Setting {
	private boolean value;

	public BoolSetting(String name, boolean defaultValue) {
		super(name);
		this.value = defaultValue;
	}

	public boolean get() {
		return value;
	}

	public void set(boolean value) {
		this.value = value;
	}

	public void toggle() {
		this.value = !this.value;
	}

	@Override
	public void save(JsonObject out) {
		out.addProperty(name, value);
	}

	@Override
	public void load(JsonObject in) {
		if (in.has(name) && in.get(name).isJsonPrimitive()) {
			try {
				value = in.get(name).getAsBoolean();
			} catch (RuntimeException ignored) {
				// keep the default value
			}
		}
	}
}
