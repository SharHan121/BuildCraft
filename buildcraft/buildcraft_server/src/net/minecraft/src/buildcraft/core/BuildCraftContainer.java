package net.minecraft.src.buildcraft.core;

import net.minecraft.src.Container;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Slot;

public class BuildCraftContainer extends Container {

	private int inventorySize;
	
	public BuildCraftContainer (int inventorySize) {
		this.inventorySize = inventorySize;
	}
	
	@Override	
	public final ItemStack func_27086_a(int i)
	{
		ItemStack itemstack = null;
		Slot slot = (Slot)inventorySlots.get(i);
		if(slot != null && slot.func_27006_b())
		{
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();
			if (i < inventorySize) {
				func_28126_a(itemstack1, inventorySize, inventorySlots.size(), true);
			} else {
				func_28126_a(itemstack1, 0, inventorySize, false);
			}

			if(itemstack1.stackSize == 0) {
				slot.putStack(null);
			} else {
				slot.onSlotChanged();
			}
		}
		return itemstack;
	}

	@Override
	public final boolean canInteractWith(EntityPlayer entityplayer) {
		return true;
	}

}
