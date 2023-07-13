package me.isaiah.mods.chestshop.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.isaiah.mods.chestshop.ChestShopSign;
import me.isaiah.mods.chestshop.interfaces.ISign;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

@Mixin(Explosion.class)
public abstract class MixinExplosion {

	@Shadow
	@Final
	private World world;

	@Shadow
	public abstract List<BlockPos> getAffectedBlocks();

	@Inject(method = "collectBlocksAndDamageEntities", at = @At("TAIL"))
	private void chestshop_prevent_explosion(CallbackInfo info) {
		getAffectedBlocks().removeIf(pos -> {
			BlockEntity block = world.getBlockEntity(pos);

	        if (block instanceof ChestBlockEntity) {
	            // Lock the Chest to the shop owner
	            ChestBlockEntity chest = (ChestBlockEntity) block;
	            BlockPos[] poses = chestshop_surounding_blocks(chest);

	            SignBlockEntity sign = null;
	            for (BlockPos pos1 : poses) {
	                BlockEntity e;
	                if ((e=chest.getWorld().getBlockEntity(pos1)) instanceof SignBlockEntity) {
	                    sign = (SignBlockEntity) e;
	                    if (ChestShopSign.isValid((ISign)(Object)sign)) {
	                        return true;
	                    }
	                }
	            }
	        }

	        if (block instanceof SignBlockEntity) {
	            SignBlockEntity sign = (SignBlockEntity) block;

	            if (ChestShopSign.isValid((ISign)(Object)sign)) {
	                return true;
	            }
	        }
			return false;
		});
	}

    public BlockPos[] chestshop_surounding_blocks(BlockEntity e) {
        BlockPos pos = e.getPos();

        BlockPos left   = new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1);
        BlockPos right  = new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1);
        BlockPos top    = new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ());
        BlockPos bottom = new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ());

        return new BlockPos[] {left, right, top, bottom};
    }
	
}