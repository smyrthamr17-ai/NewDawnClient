package com.nova.mixin;

import com.nova.module.impl.FreeCam;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
	/** Your player ignores the movement keys while Free Cam is on (the keys move the camera instead). */
	@Inject(method = "tick", at = @At("TAIL"))
	private void nova$freeze(CallbackInfo ci) {
		if (FreeCam.active) {
			this.playerInput = PlayerInput.DEFAULT;
			this.movementVector = Vec2f.ZERO;
		}
	}
}
