/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport.statements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import buildcraft.api.gates.IGate;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementMouseClick;

import buildcraft.lib.misc.ColourUtil;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.StackUtil;
import buildcraft.transport.BCTransportSprites;

public class ActionParameterSignal implements IStatementParameter {
    @Nullable
    private EnumDyeColor colour = null;

    public ActionParameterSignal() {}

    public ActionParameterSignal(EnumDyeColor colour) {
        this.colour = colour;
    }

    @Nullable
    public EnumDyeColor getColor() {
        return colour;
    }

    @Override
    public TextureAtlasSprite getGuiSprite() {
        EnumDyeColor c = colour;
        if (c == null) {
            return null;
        } else {
            return BCTransportSprites.getPipeSignal(true, c).getSprite();
        }
    }

    @Override
    public boolean onClick(IStatementContainer source, IStatement stmt, ItemStack stack, StatementMouseClick mouse) {
        return false;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        EnumDyeColor c = colour;
        if (c != null) {
            nbt.setByte("color", (byte) c.getMetadata());
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("color")) {
            this.colour = EnumDyeColor.byMetadata(nbt.getByte("color"));
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ActionParameterSignal) {
            ActionParameterSignal param = (ActionParameterSignal) object;

            return param.getColor() == getColor();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getColor());
    }

    @Override
    public String getDescription() {
        EnumDyeColor c = colour;
        if (c == null) {
            return null;
        }
        String format = LocaleUtil.localize("gate.action.pipe.wire");
        Object[] args = { ColourUtil.getTextFullTooltip(c) };
        return String.format(format, args);
    }

    @Override
    public String getUniqueTag() {
        return "buildcraft:pipeWireAction";
    }

    @Override
    public IStatementParameter rotateLeft() {
        return this;
    }

    @Override
    public ItemStack getItemStack() {
        return StackUtil.EMPTY;
    }

    @Override
    public IStatementParameter[] getPossible(IStatementContainer source, IStatement stmt) {
        if (!(source instanceof IGate)) {
            return null;
        }
        IGate gate = (IGate) source;
        List<IStatementParameter> poss = new ArrayList<>(1 + ColourUtil.COLOURS.length);
        poss.add(new ActionParameterSignal());
        for (EnumDyeColor c : ColourUtil.COLOURS) {
            if (TriggerPipeSignal.doesGateHaveColour(gate, c)) {
                poss.add(new ActionParameterSignal(c));
            }
        }
        return poss.toArray(new IStatementParameter[poss.size()]);
    }
}
