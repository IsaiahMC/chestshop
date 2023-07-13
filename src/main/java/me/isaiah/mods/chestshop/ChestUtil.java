package me.isaiah.mods.chestshop;

import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ChestUtil {

    public static int add_stack(ChestBlockEntity chest, ItemStack stack) {
        int i2 = get_OccupiedSlotWithRoomForStack(chest, stack);
        if (i2 == -1) {
            i2 = get_empty_slot(chest);
        }
        return i2 == -1 ? stack.getCount() : add_stack(chest, i2, stack);
    }
    
    public static int get_OccupiedSlotWithRoomForStack(ChestBlockEntity chest, ItemStack stack) {
        for (int i2 = 0; i2 < chest.size(); ++i2) {
            if (!canStackAddMore(chest, chest.getStack(i2), stack)) continue;
            return i2;
        }
        return -1;
    }
    
    private static boolean canStackAddMore(ChestBlockEntity chest, ItemStack existingStack, ItemStack stack) {
        return !existingStack.isEmpty() && ItemStack.canCombine(existingStack, stack) && existingStack.isStackable() && existingStack.getCount() < existingStack.getMaxCount() && existingStack.getCount() < chest.getMaxCountPerStack();
    }
    
    private static int add_stack(ChestBlockEntity chest, int slot, ItemStack stack) {
        Item item = stack.getItem();
        int j2 = stack.getCount();
        ItemStack itemstack1 = chest.getStack(slot);
        if (itemstack1.isEmpty()) {
            itemstack1 = new ItemStack(item, 0);
            if (stack.hasNbt()) {
                itemstack1.setNbt(stack.getNbt().copy());
            }
            chest.setStack(slot, itemstack1);
        }
        int k2 = j2;
        if (j2 > itemstack1.getMaxCount() - itemstack1.getCount()) {
            k2 = itemstack1.getMaxCount() - itemstack1.getCount();
        }
        if (k2 > chest.getMaxCountPerStack() - itemstack1.getCount()) {
            k2 = chest.getMaxCountPerStack() - itemstack1.getCount();
        }
        if (k2 == 0) {
            return j2;
        }
        itemstack1.increment(k2);
        itemstack1.setBobbingAnimationTime(5);
        return j2 -= k2;
    }
    
    public static int get_empty_slot(ChestBlockEntity chest) {
        for (int i2 = 0; i2 < chest.size(); ++i2) {
            if (!chest.getStack(i2).isEmpty()) continue;
            return i2;
        }
        return -1;
    }
	
}
