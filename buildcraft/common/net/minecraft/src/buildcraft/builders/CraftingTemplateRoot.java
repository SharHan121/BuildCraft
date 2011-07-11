package net.minecraft.src.buildcraft.builders;

import net.minecraft.src.IInventory;
import net.minecraft.src.Slot;
import net.minecraft.src.buildcraft.core.BuildCraftContainer;

class CraftingTemplateRoot extends BuildCraftContainer {
	
	IInventory playerIInventory;
	TileTemplate template;
	int computingTime = 0;
	
	public CraftingTemplateRoot (IInventory playerInventory, TileTemplate template) {
		super (2);
		this.playerIInventory = playerInventory;
		this.template = template;
		
		addSlot(new Slot(template, 0, 55, 35));
		addSlot(new Slot(template, 1, 114, 35));

        for(int l = 0; l < 3; l++) {
            for(int k1 = 0; k1 < 9; k1++) {
                addSlot(new Slot(playerInventory, k1 + l * 9 + 9, 8 + k1 * 18, 84 + l * 18));
            }

        }

        for(int i1 = 0; i1 < 9; i1++) {
            addSlot(new Slot(playerInventory, i1, 8 + i1 * 18, 142));
        }
	}

}