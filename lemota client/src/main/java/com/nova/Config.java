package com.nova;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nova.module.Module;
import com.nova.module.ModuleManager;
import com.nova.module.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Saves module states and settings to config/nova.json. */
public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Original "FOV Effects" value while the ESP FOV fix is active. */
	public static Double fovBackup = null;

	private Config() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("nova.json");
	}

	public static void load() {
		Path path = file();
		if (!Files.exists(path)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
			if (root.has("fovBackup") && root.get("fovBackup").isJsonPrimitive()) {
				fovBackup = root.get("fovBackup").getAsDouble();
			}
			if (!root.has("modules") || !root.get("modules").isJsonObject()) {
				return;
			}
			JsonObject modules = root.getAsJsonObject("modules");
			for (Module module : ModuleManager.all()) {
				if (!modules.has(module.id()) || !modules.get(module.id()).isJsonObject()) {
					continue;
				}
				JsonObject entry = modules.getAsJsonObject(module.id());
				if (module.persistsEnabled() && entry.has("enabled") && entry.get("enabled").isJsonPrimitive()) {
					module.setEnabled(entry.get("enabled").getAsBoolean());
				}
				if (entry.has("settings") && entry.get("settings").isJsonObject()) {
					JsonObject settings = entry.getAsJsonObject("settings");
					for (Setting setting : module.settings) {
						setting.load(settings);
					}
				}
			}
		} catch (Exception e) {
			NovaClient.LOGGER.warn("Could not read nova.json: {}", e.toString());
		}
	}

	public static void save() {
		try {
			JsonObject root = new JsonObject();
			if (fovBackup != null) {
				root.addProperty("fovBackup", fovBackup);
			}
			JsonObject modules = new JsonObject();
			for (Module module : ModuleManager.all()) {
				JsonObject entry = new JsonObject();
				if (module.persistsEnabled()) {
					entry.addProperty("enabled", module.isEnabled());
				}
				JsonObject settings = new JsonObject();
				for (Setting setting : module.settings) {
					setting.save(settings);
				}
				entry.add("settings", settings);
				modules.add(module.id(), entry);
			}
			root.add("modules", modules);
			Files.writeString(file(), GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (Exception e) {
			NovaClient.LOGGER.warn("Could not write nova.json: {}", e.toString());
		}
	}
}
