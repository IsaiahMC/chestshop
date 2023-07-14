package me.isaiah.mods.chestshop.mixin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import me.isaiah.mods.chestshop.ChestShopSign;
import me.isaiah.mods.chestshop.ChestUtil;
import me.isaiah.mods.chestshop.interfaces.ISign;
import me.isaiah.mods.economy.api.Economy;
import me.isaiah.mods.economy.api.NoLoanPermittedException;
import me.isaiah.mods.economy.api.UserDoesNotExistException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
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
    @Inject(at = @At(value = "INVOKE"), method = "interactBlock", cancellable = true)
    public void chestshop_right_click(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable ci) {
        long current = System.currentTimeMillis();

        BlockEntity block = this.player.getWorld().getBlockEntity(hitResult.getBlockPos());

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
                            LiteralText msg = new LiteralText("Block locked by ChestShop");
                            msg.setStyle(msg.getStyle().withColor(Formatting.RED));
                            this.player.sendMessage(msg, true);
                            ci.cancel();
                            return;
                        }
                    }
                }
            }
        }

        if (block instanceof SignBlockEntity) {
            if (updateTime == 0 || current > updateTime + 500L) {
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
                    
                    if (chest.isEmpty()) {
                    	 message_plr(this.player, null, "&a[ChestShop]&r Shop does not have enough items.");
                         updateTime = current;
                         return;
                    }

                    // Get selling Item
                    int slot = 0;
                    String itemStr = txt[3].toLowerCase(Locale.ROOT);
                    ItemStack item = chest.getStack(slot);
                    System.out.println(item.getClass().getName() + " / " + (item.getItem() instanceof AirBlockItem));
                    while (item.getItem() instanceof AirBlockItem)
                        item = chest.getStack(++slot);

                    String itemName = Registry.ITEM.getId(item.getItem()).toString().split(":")[1];
                    if (!itemName.equalsIgnoreCase(itemStr)) {
                        message_plr(this.player, null, "Shop is misconfigured");
                        updateTime = current;
                        return;
                    }

                    // Shop ran out of items to sell
                    if (item.getCount() < Integer.valueOf(txt[1])) {
                        message_plr(this.player, null, "Shop does not have enough items.");
                        updateTime = current;
                        return;
                    }

                    // Get money
                    BigDecimal money = new BigDecimal(ChestShopSign.getBuyPrice((ISign)(Object)sign));
                    if (money.doubleValue() == -1) {
                        // Sell Sign only
                        updateTime = current;
                        return;
                    }

                    // Move Money from buyer to seller
                    try {
                        String player_name = this.player.getName().asString();
                        String admin_shop = "AdminShop";
                        
                        if (!txt[0].equalsIgnoreCase(admin_shop)) {
                        	Economy.add(txt[0], money);
                        }
                        
                        Economy.substract(player_name, money);
                    } catch (UserDoesNotExistException | NoLoanPermittedException e) {
                        e.printStackTrace();
                        message_plr(this.player, "&cEconomy Exception: " + e.getMessage());
                        updateTime = current;
                        return;
                    }

                    // Give ItemStack
                    ItemStack toGive = chest.removeStack(slot, Integer.valueOf(txt[1]));
                    this.player.giveItemStack(toGive);

                    // Send Bought message
                    String mm = "&a[ChestShop] You bought &f" + txt[1] + "&ax &f" + itemName.toUpperCase() + "&a for &f$" + money.doubleValue();
                    message_plr(this.player, mm);
                    
                    updateTime = current;
                }
            }
        }
    }

    @Inject (method = "tryBreakBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)V"), 
            locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
   private void chestshop_break_block(BlockPos blockpos, CallbackInfoReturnable<Boolean> ci, BlockState state, BlockEntity entity, Block bl) {
        long current = System.currentTimeMillis();

        BlockEntity block = this.player.getWorld().getBlockEntity(blockpos);

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
                            LiteralText msg = new LiteralText("Block locked by ChestShop");
                            msg.setStyle(msg.getStyle().withColor(Formatting.RED));
                            this.player.sendMessage(msg, true);

                            ci.setReturnValue(false);
                            it_still_exists(pos);
                            return;
                        }
                    }
                }
            }
        }

        if (block instanceof SignBlockEntity) {
            SignBlockEntity sign = (SignBlockEntity) block;

            if (ChestShopSign.isValid((ISign)(Object)sign)) {
            	it_still_exists(blockpos);

                LiteralText msg = new LiteralText("Block locked by ChestShop");
                msg.setStyle(msg.getStyle().withColor(Formatting.RED));
                this.player.sendMessage(msg, true);
                updateTime = current;
                ci.setReturnValue(false);
            }
        }
   }

    @Inject(at = @At(value = "INVOKE"), method = "processBlockBreakingAction", cancellable = true)
    public void chestshop_left_click(BlockPos blockpos, PlayerActionC2SPacket.Action action, Direction direction, int integer, CallbackInfo ci) {
        long current = System.currentTimeMillis();
        if (updateTime == 0 || current > updateTime + 700L) {
            BlockEntity block = this.player.getWorld().getBlockEntity(blockpos);

            if (block instanceof SignBlockEntity) {
                SignBlockEntity sign = (SignBlockEntity) block;

                if (ChestShopSign.isValid((ISign)(Object)sign)) {
                	if (chestshop_sign_left_click(current, sign)) {
                		return;
                	}
                }
            }

            updateTime = current;

        }
    }
    
    public boolean chestshop_sign_left_click(long current, SignBlockEntity sign) {
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

        // Get money
        BigDecimal money = new BigDecimal(ChestShopSign.getSellPrice((ISign)(Object)sign));
        if (money.doubleValue() == -1) {
            // Buy Sign only
            updateTime = current;
            return true;
        }

        PlayerInventory inv = this.player.getInventory();
        
        int scount = Integer.parseInt(txt[1]);
        
        boolean found = false;
        boolean not_en = false;
        ItemStack stack = null;
        int slot = -1;
        
        for (int i = 0; i < inv.size(); i++) {
        	ItemStack item = inv.getStack(i);
        	String item_name = Registry.ITEM.getId(item.getItem()).toString().split(":")[1];
        	
        	if (item_name.equalsIgnoreCase(txt[3])) {
        		if (item.getCount() < scount) {
        			not_en = true;
        		} else {
        			found = true;
        			stack = item;
        			slot = i;
        			break;
        		}
        	}
        }
        
        if (!found && not_en) {
        	message_plr(this.player, "You don't have enough items to sell!");
        	return false;
        }
        
        if (!found) {
        	message_plr(this.player, "You don't have enough items to sell!");
        	return false;
        }
        
        // Give ItemStack
        ItemStack toGive = inv.removeStack(slot, scount);
        ChestUtil.add_stack(chest, toGive);

        // Move Money from buyer to seller
        try {
        	String admin_shop = "AdminShop";
        	if (!txt[0].equalsIgnoreCase(admin_shop)) {
        		Economy.substract(txt[0], money);
        	}
            Economy.add(this.player.getName().asString(), money);
        } catch (UserDoesNotExistException | NoLoanPermittedException e) {
            e.printStackTrace();
            message_plr(this.player, null, "Economy Exception: " + e.getMessage());
            updateTime = current;
            return true;
        }

        // Send Bought message
        String text = "&aYou sold &f" + txt[1] + "&ax &f" + txt[3].toUpperCase() + "&a for &f$" + money.doubleValue();
        message_plr(this.player, text);
        
        return false;
    }

    public void message_plr(ServerPlayerEntity cs, String message) {
		try {
			cs.sendMessage(Text.of(translate_alternate_color_codes('&', message)), false);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    public void message_plr(ServerPlayerEntity cs, Formatting color, String message) {
		try {
			if (null == color) {
				cs.sendMessage(Text.of(translate_alternate_color_codes('&', message)), false);
				return;
			}

			List<Text> txts = Text.of(message).getWithStyle(Style.EMPTY.withColor(color));
			for (Text t : txts) {
				cs.sendMessage(t, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
    private static final char COLOR_CHAR = '\u00A7';
    private static String translate_alternate_color_codes(char altColorChar, String textToTranslate) {
        char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i+1]) > -1) {
                b[i] = COLOR_CHAR;
                b[i+1] = Character.toLowerCase(b[i+1]);
            }
        }
        return new String(b);
    }


    // Let the client know that the block isn't broken
    public void it_still_exists(BlockPos pos) {
        this.player.networkHandler.sendPacket(new BlockUpdateS2CPacket(this.player.getWorld(), pos));
        BlockEntity tileentity = player.getWorld().getBlockEntity(pos);
        if (tileentity != null)
            this.player.networkHandler.sendPacket(tileentity.toUpdatePacket());
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