package me.isaiah.mods.chestshop.mixin;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.isaiah.mods.chestshop.ChestShopSign;
import me.isaiah.mods.chestshop.interfaces.ISign;
import me.isaiah.mods.economy.api.Economy;
import me.isaiah.mods.economy.api.NoLoanPermittedException;
import me.isaiah.mods.economy.api.UserDoesNotExistException;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

@Mixin(ServerPlayerInteractionManager.class)
public class PlayerInteractionMixin {

    @Shadow
    public ServerPlayerEntity player;

    long updateTime = 0;

    @SuppressWarnings("rawtypes")
    @Inject(at = { @At(value = "INVOKE") }, method = { "interactBlock" }, cancellable = true)
    public void rightClick(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable ci) {
        long current = System.currentTimeMillis();

        BlockEntity block = this.player.getServerWorld().getBlockEntity(hitResult.getBlockPos());

        if (block instanceof ChestBlockEntity) {
            // Lock the Chest to the shop owner
            ChestBlockEntity chest = (ChestBlockEntity) block;
            BlockPos[] poses = getSuroundingBlocks(chest);

            SignBlockEntity sign = null;
            for (BlockPos pos : poses) {
                BlockEntity e;
                if ((e=chest.getWorld().getBlockEntity(pos)) instanceof SignBlockEntity) {
                    sign = (SignBlockEntity) e;
                    if (ChestShopSign.isValid((ISign)(Object)sign)) {
                        String[] txt = ChestShopSign.readText((ISign)(Object)sign);
                        if (!txt[0].equalsIgnoreCase(this.player.getName().asString())) {
                            this.player.sendSystemMessage(new LiteralText("Chest locked by ChestShop"), UUID.randomUUID());
                            ci.cancel();
                            return;
                        }
                    }
                }
            }
        }

        if (block instanceof SignBlockEntity) {
            if (updateTime == 0 || current > updateTime + 1500L) {
                SignBlockEntity sign = (SignBlockEntity) block;

                if (ChestShopSign.isValid((ISign)(Object)sign)) {
                    String[] txt = ChestShopSign.readText((ISign)(Object)sign);

                    // Find the Chest that this shop is attached to
                    BlockPos[] poses = getSuroundingBlocks(sign);
                    ChestBlockEntity chest = null;
                    for (BlockPos pos : poses) {
                        BlockEntity e;
                        if ((e=sign.getWorld().getBlockEntity(pos)) instanceof ChestBlockEntity) {
                            chest = (ChestBlockEntity) e;
                            break;
                        }
                    }

                    // Get selling Item
                    String itemStr = txt[3].toLowerCase(Locale.ROOT);
                    ItemStack item = chest.getStack(0);
                    String itemName = Registry.ITEM.getId(item.getItem()).toString().split(":")[1];
                    if (!itemName.equalsIgnoreCase(itemStr)) {
                        this.player.sendSystemMessage(new LiteralText("Shop is misconfigured"), UUID.randomUUID());
                        updateTime = current;
                        return;
                    }

                    // Shop ran out of items to sell
                    if (item.getCount() < Integer.valueOf(txt[1])) {
                        this.player.sendSystemMessage(new LiteralText("Shop does not have enough items"), UUID.randomUUID());
                        updateTime = current;
                        return;
                    }

                    // Get money
                    BigDecimal money = new BigDecimal(ChestShopSign.getBuyPrice((ISign)(Object)sign));
                    if (money.doubleValue() == -1) {
                        System.out.println("SELL SIGN");
                        // Sell Sign
                    }

                    // Move Money from buyer to seller
                    try {
                        Economy.add(txt[0], money);
                        Economy.substract(this.player.getName().asString(), money);
                    } catch (UserDoesNotExistException | NoLoanPermittedException e) {
                        e.printStackTrace();
                        this.player.sendSystemMessage(new LiteralText("Economy Exception: " + e.getMessage()), UUID.randomUUID());
                        updateTime = current;
                        return;
                    }

                    // Give ItemStack
                    ItemStack toGive = chest.removeStack(0, Integer.valueOf(txt[1]));
                    this.player.giveItemStack(toGive);

                    // Send Bought message
                    LiteralText msg = new LiteralText(String.format("You bought %sx %s for %s", txt[1], itemName.toUpperCase(), money.doubleValue()));
                    msg.setStyle(msg.getStyle().withColor(Formatting.GREEN));
                    this.player.sendSystemMessage(msg, UUID.randomUUID());
                    updateTime = current;
                }
            }
        }
    }

    @Inject(at = { @At(value = "INVOKE") }, method = { "processBlockBreakingAction" })
    public void leftClick(BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int integer, CallbackInfo ci) {
        System.out.println("blockBreak!!!");
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