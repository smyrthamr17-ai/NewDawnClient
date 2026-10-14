package com.nova.mixin;

import com.nova.module.impl.FreeCam;
import com.nova.module.impl.FreeLook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
	/** Sends the mouse movement to the camera instead of the player while Free Look / Free Cam is on. */
	@Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
	private void nova$changeLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
		if (!FreeCam.active && !FreeLook.active) {
			return;
		}
		if ((Object) this != MinecraftClient.getInstance().player) {
			return;
		}
		if (FreeCam.active) {
			FreeCam.rotate(cursorDeltaX, cursorDeltaY);
		} else {
			FreeLook.rotate(cursorDeltaX, cursorDeltaY);
		}
		ci.cancel();
	}
}
