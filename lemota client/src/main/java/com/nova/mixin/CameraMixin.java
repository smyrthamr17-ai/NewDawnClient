package com.nova.mixin;

import com.nova.module.impl.FreeCam;
import com.nova.module.impl.FreeLook;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Shadow
	protected abstract void setPos(double x, double y, double z);

	@Inject(method = "update", at = @At("TAIL"))
	private void nova$update(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
			float tickProgress, CallbackInfo ci) {
		if (FreeCam.active) {
			FreeCam.step();
			setRotation(FreeCam.yaw, FreeCam.pitch);
			setPos(FreeCam.x, FreeCam.y, FreeCam.z);
		} else if (FreeLook.active) {
			setRotation(FreeLook.yaw, FreeLook.pitch);
		}
	}

	/** Shows your own player model while the camera is detached. */
	@Inject(method = "isThirdPerson", at = @At("HEAD"), cancellable = true)
	private void nova$thirdPerson(CallbackInfoReturnable<Boolean> cir) {
		if (FreeCam.active) {
			cir.setReturnValue(true);
		}
	}
}
