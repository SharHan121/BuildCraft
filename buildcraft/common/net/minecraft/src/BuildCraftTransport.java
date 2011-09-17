package net.minecraft.src;

import net.minecraft.src.buildcraft.core.CoreProxy;
import net.minecraft.src.buildcraft.core.DefaultProps;
import net.minecraft.src.buildcraft.core.ItemBuildCraftTexture;
//import net.minecraft.src.buildcraft.transport.BlockCobblestonePipe;
//import net.minecraft.src.buildcraft.transport.BlockDiamondPipe;
import net.minecraft.src.buildcraft.transport.BlockGenericPipe;
import net.minecraft.src.buildcraft.transport.Pipe;
//import net.minecraft.src.buildcraft.transport.BlockGoldenPipe;
//import net.minecraft.src.buildcraft.transport.BlockIronPipe;
//import net.minecraft.src.buildcraft.transport.BlockStonePipe;
//import net.minecraft.src.buildcraft.transport.BlockObsidianPipe;
//import net.minecraft.src.buildcraft.transport.BlockWoodenPipe;
import net.minecraft.src.buildcraft.transport.legacy.LegacyBlockCobblestonePipe;
import net.minecraft.src.buildcraft.transport.legacy.LegacyBlockDiamondPipe;
import net.minecraft.src.buildcraft.transport.legacy.LegacyBlockGoldenPipe;
import net.minecraft.src.buildcraft.transport.legacy.LegacyBlockIronPipe;
import net.minecraft.src.buildcraft.transport.legacy.LegacyBlockObsidianPipe;
import net.minecraft.src.buildcraft.transport.legacy.LegacyBlockStonePipe;
import net.minecraft.src.buildcraft.transport.legacy.LegacyBlockWoodenPipe;
import net.minecraft.src.buildcraft.transport.legacy.LegacyTileCobblestonePipe;
import net.minecraft.src.buildcraft.transport.legacy.LegacyTileDiamondPipe;
import net.minecraft.src.buildcraft.transport.legacy.LegacyTileGoldenPipe;
import net.minecraft.src.buildcraft.transport.legacy.LegacyTileIronPipe;
import net.minecraft.src.buildcraft.transport.legacy.LegacyTileObsidianPipe;
import net.minecraft.src.buildcraft.transport.legacy.LegacyTileStonePipe;
import net.minecraft.src.buildcraft.transport.legacy.LegacyTileWoodenPipe;
import net.minecraft.src.buildcraft.transport.pipes.PipeItemsCobblestone;
import net.minecraft.src.buildcraft.transport.pipes.PipeItemsDiamond;
import net.minecraft.src.buildcraft.transport.pipes.PipeItemsGold;
import net.minecraft.src.buildcraft.transport.pipes.PipeItemsIron;
import net.minecraft.src.buildcraft.transport.pipes.PipeItemsObsidian;
import net.minecraft.src.buildcraft.transport.pipes.PipeItemsStone;
import net.minecraft.src.buildcraft.transport.pipes.PipeItemsWood;
import net.minecraft.src.buildcraft.transport.pipes.PipeLiquidsCobblestone;
import net.minecraft.src.buildcraft.transport.pipes.PipeLiquidsGold;
import net.minecraft.src.buildcraft.transport.pipes.PipeLiquidsIron;
import net.minecraft.src.buildcraft.transport.pipes.PipeLiquidsStone;
import net.minecraft.src.buildcraft.transport.pipes.PipeLiquidsWood;
import net.minecraft.src.buildcraft.transport.pipes.PipePowerGold;
import net.minecraft.src.buildcraft.transport.pipes.PipePowerStone;
import net.minecraft.src.buildcraft.transport.pipes.PipePowerWood;
//import net.minecraft.src.buildcraft.transport.TileCobblestonePipe;
//import net.minecraft.src.buildcraft.transport.TileDiamondPipe;
//import net.minecraft.src.buildcraft.transport.TileGoldenPipe;
//import net.minecraft.src.buildcraft.transport.TileIronPipe;
//import net.minecraft.src.buildcraft.transport.TileObsidianPipe;
//import net.minecraft.src.buildcraft.transport.TileStonePipe;
//import net.minecraft.src.buildcraft.transport.TileWoodenPipe;
import net.minecraft.src.forge.Configuration;
import net.minecraft.src.forge.Property;

public class BuildCraftTransport {
	
	private static boolean initialized = false;
			
	public static BlockGenericPipe genericPipeBlock;
	
	public static int [] diamondTextures = new int [6];
	
	public static boolean alwaysConnectPipes;
		
	public static Item pipeWaterproof;
	
	public static Item pipeItemsWood;
	public static Item pipeItemsStone;
	public static Item pipeItemsCobblestone;
	public static Item pipeItemsIron;
	public static Item pipeItemsGold;
	public static Item pipeItemsDiamond;
	public static Item pipeItemsObsidian;
	
	public static Item pipeLiquidsWood;
	public static Item pipeLiquidsCobblestone;
	public static Item pipeLiquidsStone;
	public static Item pipeLiquidsIron;
	public static Item pipeLiquidsGold;
		
	public static Item pipePowerWood;
	public static Item pipePowerStone;
	public static Item pipePowerGold;
	
	public static void initialize () {
		if (initialized) {
			return;
		}
		
		initialized = true;
		
		mod_BuildCraftCore.initialize();						
			
		Property loadLegacyPipes = BuildCraftCore.mainConfiguration
		.getOrCreateBooleanProperty("loadLegacyPipes", Configuration.GENERAL_PROPERTY, true);
		loadLegacyPipes.comment = "set to true to load pre 2.2.0 worlds pipes";		
		
		Property alwaysConnect = BuildCraftCore.mainConfiguration
				.getOrCreateBooleanProperty("pipes.alwaysConnect",
						Configuration.GENERAL_PROPERTY,
						DefaultProps.PIPES_ALWAYS_CONNECT);
		alwaysConnect.comment = "set to false to deactivate pipe connection rules, true by default";

//		Property exclusionList = BuildCraftCore.mainConfiguration
//		.getOrCreateProperty("woodenPipe.exclusion",
//				Configuration.BLOCK_PROPERTY, "");

//BlockWoodenPipe.excludedBlocks = exclusionList.value.split(",");
//
//for (int j = 0; j < BlockWoodenPipe.excludedBlocks.length; ++j) {
//	BlockWoodenPipe.excludedBlocks[j] = BlockWoodenPipe.excludedBlocks[j]
//			.trim();
//}

		BuildCraftCore.mainConfiguration.save();

		pipeWaterproof = new ItemBuildCraftTexture (DefaultProps.PIPE_WATERPROOF_ID).setIconIndex(2 * 16 + 1);
		pipeWaterproof.setItemName("pipeWaterproof");
		CoreProxy.addName(pipeWaterproof, "Pipe Waterproof");
		ModLoader.AddSmelting(Item.slimeBall.shiftedIndex, new ItemStack(
				pipeWaterproof, 32));
		
		genericPipeBlock = new BlockGenericPipe(166);
		
		pipeItemsWood = createPipe (4050, PipeItemsWood.class, "Wooden Transport Pipe", Block.planks, Block.glass, Block.planks);
		pipeItemsCobblestone = createPipe(4051, PipeItemsCobblestone.class, "Cobblestone Transport Pipe", Block.cobblestone, Block.glass, Block.cobblestone);
		pipeItemsStone = createPipe (4052, PipeItemsStone.class, "Stone Transport Pipe", Block.stone, Block.glass, Block.stone);
		pipeItemsIron = createPipe (4053, PipeItemsIron.class, "Iron Transport Pipe", Item.ingotIron, Block.glass, Item.ingotIron);
		pipeItemsGold = createPipe (4054, PipeItemsGold.class, "Gold Transport Pipe", Item.ingotGold, Block.glass, Item.ingotGold);
		pipeItemsDiamond = createPipe (4055, PipeItemsDiamond.class, "Diamond Transport Pipe", Item.diamond, Block.glass, Item.diamond);
		pipeItemsObsidian = createPipe (4056, PipeItemsObsidian.class, "Obsidian Transport Pipe", Block.obsidian, Block.glass, Block.obsidian);
		
		pipeLiquidsWood = createPipe (4057, PipeLiquidsWood.class, "Wooden Waterproof Pipe", pipeWaterproof, pipeItemsWood, null);
		pipeLiquidsCobblestone = createPipe (4058, PipeLiquidsCobblestone.class, "Cobblestone Waterproof Pipe", pipeWaterproof, pipeItemsCobblestone, null);
		pipeLiquidsStone = createPipe (4059, PipeLiquidsStone.class, "Stone Waterproof Pipe", pipeWaterproof, pipeItemsStone, null);		
		pipeLiquidsIron = createPipe (4060, PipeLiquidsIron.class, "Iron Waterproof Pipe", pipeWaterproof, pipeItemsIron, null);		
		pipeLiquidsGold = createPipe (4061, PipeLiquidsGold.class, "Golden Waterproof Pipe", pipeWaterproof, pipeItemsGold, null);			
		// diamond 4062		
		
		pipePowerWood = createPipe (4063, PipePowerWood.class, "Wooden Conductive Pipe", Item.redstone,  pipeItemsWood, null);		
		// cobblestone 4064
		pipePowerStone = createPipe (4065, PipePowerStone.class, "Stone Conductive Pipe", Item.redstone, pipeItemsStone, null);		
		// iron 4066
		pipePowerGold = createPipe(4067, PipePowerGold.class, "Golden Conductive Pipe", null, null, null);
		
		// diamond 4068
						
//				Integer.parseInt(cobblestonePipeId.value));
//		CoreProxy.addName(cobblestonePipeBlock.setBlockName("cobblestonePipe"),
//				"Cobblestone Pipe");
//		ModLoader.RegisterBlock(cobblestonePipeBlock);		
//		craftingmanager.addRecipe(new ItemStack(cobblestonePipeBlock, 8), new Object[] {
//				"   ", "PGP", "   ", Character.valueOf('P'), Block.cobblestone,
//				Character.valueOf('G'), Block.glass});
		
		for (int j = 0; j < 6; ++j) {
			diamondTextures [j] = 1 * 16 + 6 + j;
		}				
		
		alwaysConnectPipes = Boolean.parseBoolean(alwaysConnect.value);
		
		if (loadLegacyPipes.value.equals("true")) {
			Property woodenPipeId = BuildCraftCore.mainConfiguration
					.getOrCreateBlockIdProperty("woodenPipe.id",
							DefaultProps.WOODEN_PIPE_ID);
			Property stonePipeId = BuildCraftCore.mainConfiguration
					.getOrCreateBlockIdProperty("stonePipe.id",
							DefaultProps.STONE_PIPE_ID);
			Property ironPipeId = BuildCraftCore.mainConfiguration
					.getOrCreateBlockIdProperty("ironPipe.id",
							DefaultProps.IRON_PIPE_ID);
			Property goldenPipeId = BuildCraftCore.mainConfiguration
					.getOrCreateBlockIdProperty("goldenPipe.id",
							DefaultProps.GOLDEN_PIPE_ID);
			Property diamondPipeId = BuildCraftCore.mainConfiguration
					.getOrCreateBlockIdProperty("diamondPipe.id",
							DefaultProps.DIAMOND_PIPE_ID);
			Property obsidianPipeId = BuildCraftCore.mainConfiguration
					.getOrCreateBlockIdProperty("obsidianPipe.id",
							DefaultProps.OBSIDIAN_PIPE_ID);
			Property cobblestonePipeId = BuildCraftCore.mainConfiguration
					.getOrCreateBlockIdProperty("cobblestonePipe.id",
							DefaultProps.COBBLESTONE_PIPE_ID);

			ModLoader.RegisterBlock(new LegacyBlockWoodenPipe(Integer
					.parseInt(woodenPipeId.value)));
			ModLoader.RegisterBlock(new LegacyBlockStonePipe(Integer
					.parseInt(stonePipeId.value)));
			ModLoader.RegisterBlock(new LegacyBlockIronPipe(Integer
					.parseInt(ironPipeId.value)));
			ModLoader.RegisterBlock(new LegacyBlockGoldenPipe(Integer
					.parseInt(goldenPipeId.value)));
			ModLoader.RegisterBlock(new LegacyBlockDiamondPipe(Integer
					.parseInt(diamondPipeId.value)));
			ModLoader.RegisterBlock(new LegacyBlockObsidianPipe(Integer
					.parseInt(obsidianPipeId.value)));
			ModLoader.RegisterBlock(new LegacyBlockCobblestonePipe(Integer
					.parseInt(cobblestonePipeId.value)));

			ModLoader.RegisterTileEntity(LegacyTileWoodenPipe.class,
					"WoodenPipe");
			ModLoader
					.RegisterTileEntity(LegacyTileStonePipe.class, "StonePipe");
			ModLoader.RegisterTileEntity(LegacyTileIronPipe.class, "IronPipe");
			ModLoader.RegisterTileEntity(LegacyTileGoldenPipe.class,
					"GoldenPipe");
			ModLoader.RegisterTileEntity(LegacyTileDiamondPipe.class,
					"DiamondPipe");
			ModLoader.RegisterTileEntity(LegacyTileObsidianPipe.class,
					"ObsidianPipe");
			ModLoader.RegisterTileEntity(LegacyTileCobblestonePipe.class,
					"CobblestonePipe");
		}
		
		BuildCraftCore.mainConfiguration.save();

	}	
	
	private static Item createPipe (int id, Class <? extends Pipe> clas, String descr, Object r1, Object r2, Object r3) {
		Item res =  BlockGenericPipe.registerPipe (id, clas);
		res.setItemName(clas.getSimpleName());
		CoreProxy.addName(res, descr);
		
		CraftingManager craftingmanager = CraftingManager.getInstance();
		
		if (r1 != null && r2 != null && r3 != null) {						
			craftingmanager.addRecipe(new ItemStack(res, 8), new Object[] {
				"   ", "ABC", "   ", 
				Character.valueOf('A'), r1,
				Character.valueOf('B'), r2,
				Character.valueOf('C'), r3});
		} else if (r1 != null && r2 != null) {
			craftingmanager.addRecipe(new ItemStack(res, 1), new Object[] {
				"A ", "B ", 
				Character.valueOf('A'), r1,
				Character.valueOf('B'), r2});
		}
		
		return res;
	}

	public static void ModsLoaded () {
		mod_BuildCraftCore.initialize();
		initialize ();
	}
}
