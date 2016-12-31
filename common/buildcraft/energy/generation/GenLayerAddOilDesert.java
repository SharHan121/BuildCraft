/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.energy.generation;

import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;

public class GenLayerAddOilDesert extends GenLayerBiomeReplacer {

    protected static final double NOISE_FIELD_SCALE = 0.001;
    protected static final double NOISE_FIELD_THRESHOLD = 0.7;

    public GenLayerAddOilDesert(final long worldSeed, final long seed, final GenLayer parent) {
        super(worldSeed, seed, parent, NOISE_FIELD_SCALE, NOISE_FIELD_THRESHOLD, Biome.getIdForBiome(BiomeOilDesert.INSTANCE));
    }

    @Override
    protected boolean canReplaceBiome(int biomeId) {
        return Biome.getBiomeForId(biomeId) == Biomes.DESERT;
    }
}
