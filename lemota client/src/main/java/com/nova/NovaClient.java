package com.nova;

import com.nova.gui.ClickGui;
import com.nova.module.Module;
import com.nova.module.ModuleManager;
import com.nova.render.Projection;
import com.nova.render.Tracker;
import com.nova.util.FovFix;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class NovaClient implements ClientModInitializer {
	public static final String MOD_ID = "nova";
	public static final Logger LOGGER = LoggerFactory.getLogger("Nova");

	private static KeyBinding guiKey;
	private static final Map<Module, KeyBinding> TOGGLE_KEYS = new LinkedHashMap<>();
	private static boolean firstTick = true;
	private static long lastErrorLog;

	@Override
	public void onInitializeClient() {
		ModuleManager.init();
		Config.load();

		KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
		guiKey = KeyBindingHelper.registerKeyBinding(
				new KeyBinding("key.nova.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
		for (Module module : ModuleManager.all()) {
			KeyBinding key = KeyBindingHelper.registerKeyBinding(
					new KeyBinding("key.nova.toggle." + module.id(), InputUtil.Type.KEYSYM, defaultKey(module), category));
			TOGGLE_KEYS.put(module, key);
		}

		ClientTickEvents.END_CLIENT_TICK.register(NovaClient::onTick);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of(MOD_ID, "overlay"), NovaClient::onHud);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			FovFix.restore(client);
			Config.save();
		});
	}

	/** Default toggle keys. Everything can be changed in Options > Controls. */
	private static int defaultKey(Module module) {
		return switch (module.id()) {
			case "HUD" -> GLFW.GLFW_KEY_U;
			case "PlayerESP" -> GLFW.GLFW_KEY_G;
			case "StorageESP" -> GLFW.GLFW_KEY_H;
			case "AntiVanish" -> GLFW.GLFW_KEY_J;
			case "AutoWalk" -> GLFW.GLFW_KEY_K;
			case "AutoMine" -> GLFW.GLFW_KEY_M;
			case "FreeLook" -> GLFW.GLFW_KEY_V;
			case "FreeCam" -> GLFW.GLFW_KEY_B;
			default -> GLFW.GLFW_KEY_UNKNOWN;
		};
	}

	private static void onTick(MinecraftClient client) {
		while (guiKey.wasPressed()) {
			client.setScreen(new ClickGui());
		}
		for (Map.Entry<Module, KeyBinding> entry : TOGGLE_KEYS.entrySet()) {
			while (entry.getValue().wasPressed()) {
				entry.getKey().toggle();
				Config.save();
			}
		}

		if (firstTick && client.options != null) {
			firstTick = false;
			FovFix.restoreLeftover(client);
		}

		if (client.player == null || client.world == null) {
			Tracker.clear();
			return;
		}

		Tracker.tick(client);
		boolean needStableFov = false;
		for (Module module : ModuleManager.all()) {
			if (!module.isEnabled()) {
				continue;
			}
			if (module.wantsStableFov()) {
				needStableFov = true;
			}
			try {
				module.onTick();
			} catch (Exception e) {
				logError(module.name + " (tick)", e);
			}
		}
		FovFix.update(client, needStableFov);
	}

	private static void onHud(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null) {
			return;
		}
		float tickProgress = tickCounter.getTickProgress(false);
		Projection.update(client);
		for (Module module : ModuleManager.all()) {
			if (!module.isEnabled()) {
				continue;
			}
			try {
				module.onHud(context, tickProgress);
			} catch (Exception e) {
				logError(module.name + " (render)", e);
			}
		}
	}

	private static void logError(String where, Exception e) {
		long now = System.currentTimeMillis();
		if (now - lastErrorLog > 5000) {
			lastErrorLog = now;
			LOGGER.error("Nova module error in {}", where, e);
		}
	}
}
