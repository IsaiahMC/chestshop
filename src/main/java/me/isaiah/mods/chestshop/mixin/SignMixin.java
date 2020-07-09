package me.isaiah.mods.chestshop.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import me.isaiah.mods.chestshop.interfaces.ISign;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.text.Text;

@Mixin(SignBlockEntity.class)
public class SignMixin implements ISign {

    @Shadow
    @Final
    private Text[] text;

    @Override
    public Text[] getText() {
        return text;
    }

}