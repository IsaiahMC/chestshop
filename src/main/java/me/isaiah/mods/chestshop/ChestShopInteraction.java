package me.isaiah.mods.chestshop;

import java.util.List;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class ChestShopInteraction {

    // Let the client know that the block isn't broken
    public void itStillExists(ServerPlayerEntity player, BlockPos pos) {
        player.networkHandler.sendPacket(new BlockUpdateS2CPacket(player.getWorld(), pos));
        BlockEntity tileentity = player.getWorld().getBlockEntity(pos);
        if (tileentity != null)
            player.networkHandler.sendPacket(tileentity.toUpdatePacket());
    }

    public BlockPos[] getSuroundingBlocks(BlockEntity e) {
        BlockPos pos = e.getPos();

        BlockPos left   = new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1);
        BlockPos right  = new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1);
        BlockPos top    = new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ());
        BlockPos bottom = new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ());

        return new BlockPos[] {left, right, top, bottom};
    }
    
    

}