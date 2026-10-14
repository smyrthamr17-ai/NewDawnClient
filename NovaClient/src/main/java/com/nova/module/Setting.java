package com.nova.module;

import com.google.gson.JsonObject;

/** Base class for a module option that can be shown in the GUI and saved to disk. */
public abstract class Setting {
	public final String name;

	protected Setting(String name) {
		this.name = name;
	}

	public abstract void save(JsonObject out);

	public abstract void load(JsonObject in);
}
