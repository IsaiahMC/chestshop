package me.isaiah.mods.chestshop.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import me.isaiah.mods.chestshop.interfaces.ISignPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;

@Mixin(UpdateSignC2SPacket.class)
public class MixinUpdateSignC2SPacket implements ISignPacket {

	@Shadow
    public String[] text;

    @Override
    public String[] chestshop_getText() {
        return text;
    }
    
    @Override
    public void chestshop_set_line(int i, String txt) {
    	text[i] = txt;
    }
	
}
