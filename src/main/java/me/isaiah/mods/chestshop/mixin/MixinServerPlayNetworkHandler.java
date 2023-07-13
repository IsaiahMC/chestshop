package me.isaiah.mods.chestshop.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.isaiah.mods.chestshop.ChestShopSign;
import me.isaiah.mods.chestshop.interfaces.ISignPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {

	@Shadow
    public ServerPlayerEntity player;
	
	@Inject(at = @At("HEAD"), method="onUpdateSign")
	public void chestshop_sign_update(UpdateSignC2SPacket packet, CallbackInfo ci) {
		String[] txt = packet.getText();
		boolean valid = ChestShopSign.isValid_no_name(txt);
		if (valid) {
			ISignPacket sign = (ISignPacket) packet;
			String name = txt[0].trim();
			
			if (name.equalsIgnoreCase("adminshop")) {
				if (!player.isCreativeLevelTwoOp()) {
					sign.chestshop_set_line(0, "");
					return;
				} else {
					return;
				}
			}

			if (name.trim().length() == 0) {
				sign.chestshop_set_line(0, player.getEntityName());
				name = player.getEntityName();
			}

			String p_name = player.getEntityName();
			if (!name.equalsIgnoreCase(p_name)) {
				sign.chestshop_set_line(0, p_name);
				return;
			}			
		}
	}
	
}