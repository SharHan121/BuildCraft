package net.minecraft.src.buildcraft.core;

import java.util.LinkedList;

import net.minecraft.src.Block;
import net.minecraft.src.BlockCactus;
import net.minecraft.src.BuildCraftCore;
import net.minecraft.src.EntityItem;
import net.minecraft.src.IInventory;
import net.minecraft.src.InventoryLargeChest;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Packet230ModLoader;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import net.minecraft.src.buildcraft.api.APIProxy;
import net.minecraft.src.buildcraft.api.EntityPassiveItem;
import net.minecraft.src.buildcraft.api.IAreaProvider;
import net.minecraft.src.buildcraft.api.ICustomHeightInPipe;
import net.minecraft.src.buildcraft.api.IPipeEntry;
import net.minecraft.src.buildcraft.api.LaserKind;
import net.minecraft.src.buildcraft.api.Orientations;
import net.minecraft.src.buildcraft.api.Position;

public class Utils {	
	
	public static final float pipeMinSize = 0.25F;
	public static final float pipeMaxSize = 0.75F;
	public static float pipeNormalSpeed = 0.01F;
	
	/**
	 * Depending on the kind of item in the pipe, set the floor at a different
	 * level to optimize graphical aspect.
	 */
	public static float getPipeFloorOf (ItemStack item) {		
		if (item.itemID < Block.blocksList.length) {
			Block block = Block.blocksList[item.itemID];
			
			if (block == null) {
				return 0.27F;
			} else if (CoreProxy.isPlainBlock(block) || block instanceof BlockCactus) {
				return 0.4F;
			} else if (block instanceof ICustomHeightInPipe) {
				return ((ICustomHeightInPipe) block).getHeightInPipe();
			} else {
				return 0.27F;
			}
		} else {		
			return 0.27F;
		}
	}
	
	public static Orientations get2dOrientation (Position pos1, Position pos2) {
		double Dx = pos1.x - pos2.x;
    	double Dz = pos1.z - pos2.z;
    	double angle = Math.atan2(Dz, Dx) / Math.PI * 180 + 180;    	
    	
    	if (angle < 45 || angle > 315) {
    		return Orientations.XPos;
    	} else if (angle < 135) {
    		return Orientations.ZPos;
    	} else if (angle < 225) {
    		return Orientations.XNeg;
    	} else {
    		return Orientations.ZNeg;
    	}    	    	    	
	}	
	
	public static Orientations get3dOrientation (Position pos1, Position pos2) {
		double Dx = pos1.x - pos2.x;
    	double Dy = pos1.y - pos2.y;
    	double angle = Math.atan2(Dy, Dx) / Math.PI * 180 + 180;
    	
    	if (angle > 45 && angle < 135) {
    		return Orientations.YPos;
    	} else if (angle > 225 && angle < 315) {
    		return Orientations.YNeg;
    	} else {
    		return get2dOrientation(pos1, pos2);
    	}    	
	}
	
	/**
	 * Look around the tile given in parameter in all 6 position, tries to
	 * add the items to a random pipe entry around. Will make sure that the 
	 * location from which the items are coming from (identified by the from 
	 * parameter) isn't used again so that entities doesn't go backwards. 
	 * Returns true if successful, false otherwise.
	 */
	public static boolean addToRandomPipeEntry (TileEntity tile, Orientations from, ItemStack items) {
		World w = APIProxy.getWorld();
		
		LinkedList <Orientations> possiblePipes = new LinkedList <Orientations> ();
		
		for (int j = 0; j < 6; ++j) {
			if (from.reverse().ordinal() == j) {
				continue;
			}
			
			Position pos = new Position(tile.xCoord, tile.yCoord, tile.zCoord,
					Orientations.values()[j]);
			
			pos.moveForwards(1.0);
			
			TileEntity pipeEntry = w.getBlockTileEntity((int) pos.x,
					(int) pos.y, (int) pos.z);
			
			if (pipeEntry instanceof IPipeEntry) {
				possiblePipes.add(Orientations.values()[j]);
			}
		}
		
		if (possiblePipes.size() > 0) {
			int choice = w.rand.nextInt(possiblePipes.size());
			
			Position entityPos = new Position(tile.xCoord, tile.yCoord, tile.zCoord,
					possiblePipes.get(choice));
			Position pipePos = new Position(tile.xCoord, tile.yCoord, tile.zCoord,
					possiblePipes.get(choice));
			
			entityPos.x += 0.5;
			entityPos.y += getPipeFloorOf(items);
			entityPos.z += 0.5;
			
			entityPos.moveForwards(0.5);
			
			pipePos.moveForwards(1.0);
			
			IPipeEntry pipeEntry = (IPipeEntry) w.getBlockTileEntity(
					(int) pipePos.x, (int) pipePos.y, (int) pipePos.z);
			
			EntityPassiveItem entity = new EntityPassiveItem(w, entityPos.x,
					entityPos.y, entityPos.z, items);
			
			w.entityJoinedWorld(entity);
			pipeEntry.entityEntering(entity, entityPos.orientation);
			items.stackSize = 0;
			return true;
		} else {
			return false;
		}
	}
	
	public static void dropItems (World world, ItemStack stack, int i, int j, int k) {
		float f1 = 0.7F;
		double d = (double)(world.rand.nextFloat() * f1) + (double)(1.0F - f1) * 0.5D;
		double d1 = (double)(world.rand.nextFloat() * f1) + (double)(1.0F - f1) * 0.5D;
		double d2 = (double)(world.rand.nextFloat() * f1) + (double)(1.0F - f1) * 0.5D;
		EntityItem entityitem = new EntityItem(world, (double) i + d,
				(double) j + d1, (double) k + d2, stack);
		entityitem.delayBeforeCanPickup = 10;
		
		world.entityJoinedWorld(entityitem);
	}
	
	public static void dropItems (World world, IInventory inventory, int i, int j, int k) {
		for (int l = 0; l < inventory.getSizeInventory(); ++l) {
			ItemStack items = inventory.getStackInSlot(l);
			
			if (items != null && items.stackSize > 0) {
				dropItems (world, inventory.getStackInSlot(l).copy(), i, j, k);
			}
    	}
	}
	
    
    public static TileEntity getTile (World world, Position pos, Orientations step) {
    	Position tmp = new Position (pos);
    	tmp.orientation = step;
    	tmp.moveForwards(1.0);
    	
		return world.getBlockTileEntity((int) tmp.x, (int) tmp.y, (int) tmp.z);    	
    }
	
	public static IInventory getInventory(IInventory inv) {
		if(inv instanceof TileEntityChest)
		{
			TileEntityChest chest = (TileEntityChest)inv;
			Position pos = new Position (chest.xCoord, chest.yCoord, chest.zCoord);
			TileEntity tile;
			IInventory chest2 = null;
			tile = Utils.getTile(chest.worldObj, pos, Orientations.XNeg);
			if (tile instanceof TileEntityChest) {
				chest2 = (IInventory)tile;
			}
			tile = Utils.getTile(chest.worldObj, pos, Orientations.XPos);
			if (tile instanceof TileEntityChest) {
				chest2 = (IInventory)tile;
			}
			tile = Utils.getTile(chest.worldObj, pos, Orientations.ZNeg);
			if (tile instanceof TileEntityChest) {
				chest2 = (IInventory)tile;
			}
			tile = Utils.getTile(chest.worldObj, pos, Orientations.ZPos);
			if (tile instanceof TileEntityChest) {
				chest2 = (IInventory)tile;
			}
			if(chest2 != null) return new InventoryLargeChest("", inv, chest2);
		}
		return inv;
	}
    
    public static IAreaProvider getNearbyAreaProvider (World world, int i, int j, int k) {
    	TileEntity a1 = world.getBlockTileEntity(i + 1, j, k);
    	TileEntity a2 = world.getBlockTileEntity(i - 1, j, k);
    	TileEntity a3 = world.getBlockTileEntity(i, j, k + 1);
    	TileEntity a4 = world.getBlockTileEntity(i, j, k - 1);
    	TileEntity a5 = world.getBlockTileEntity(i, j + 1, k);
    	TileEntity a6 = world.getBlockTileEntity(i, j - 1, k);
    	
    	if (a1 instanceof IAreaProvider) {
    		return (IAreaProvider) a1;
    	}
    	
    	if (a2 instanceof IAreaProvider) {
    		return (IAreaProvider) a2;
    	}

    	if (a3 instanceof IAreaProvider) {
    		return (IAreaProvider) a3;
    	}

    	if (a4 instanceof IAreaProvider) {
    		return (IAreaProvider) a4;
    	}
    	
    	if (a5 instanceof IAreaProvider) {
    		return (IAreaProvider) a5;
    	}
    	
    	if (a6 instanceof IAreaProvider) {
    		return (IAreaProvider) a6;
    	}

    	return null;
    }
     
	public static EntityBlock createLaser(World world, Position p1, Position p2,
			LaserKind kind) {
		if (p1.equals(p2)) {
			return null;
		}
		
		double iSize = p2.x - p1.x;
		double jSize = p2.y - p1.y;
		double kSize = p2.z - p1.z;
		
		double i = p1.x;
		double j = p1.y;
		double k = p1.z;
		
		if (iSize != 0) {
			i += 0.5;
			j += 0.45;
			k += 0.45;
			
			jSize = 0.10;
			kSize = 0.10;
		} else if (jSize != 0) {			
			i += 0.45;
			j += 0.5;
			k += 0.45;
			
			iSize = 0.10;
			kSize = 0.10;
		} else if (kSize != 0) {
			i += 0.45;
			j += 0.45;
			k += 0.5;
			
			iSize = 0.10;
			jSize = 0.10;
		}
		
		int texture = BuildCraftCore.redLaserTexture;
		
		switch (kind) {
			case Blue:
				texture = BuildCraftCore.blueLaserTexture;
				break;
			
			case Red:
				texture = BuildCraftCore.redLaserTexture;
				break;
				
			case Stripes:
				texture = BuildCraftCore.stripesLaserTexture;
				break;
		}
		
		EntityBlock block = new EntityBlock(world, i, j, k, iSize, jSize,
				kSize, texture);
		
		world.entityJoinedWorld(block);
		
		return block;
	}
	
	public static EntityBlock[] createLaserBox(World world, double xMin, double yMin,
			double zMin, double xMax, double yMax, double zMax, LaserKind kind) {
		EntityBlock lasers [] = new EntityBlock [12];
		Position [] p = new Position [8];
		
		p [0] = new Position(xMin, yMin, zMin);
		p [1] = new Position(xMax, yMin, zMin);
		p [2] = new Position(xMin, yMax, zMin);
		p [3] = new Position(xMax, yMax, zMin);
		p [4] = new Position(xMin, yMin, zMax);
		p [5] = new Position(xMax, yMin, zMax);
		p [6] = new Position(xMin, yMax, zMax);
		p [7] = new Position(xMax, yMax, zMax);
		
		lasers [0] = Utils.createLaser(world, p [0], p [1], kind);
		lasers [1] = Utils.createLaser(world, p [0], p [2], kind);
		lasers [2] = Utils.createLaser(world, p [2], p [3], kind);
		lasers [3] = Utils.createLaser(world, p [1], p [3], kind);
		lasers [4] = Utils.createLaser(world, p [4], p [5], kind);
		lasers [5] = Utils.createLaser(world, p [4], p [6], kind);
		lasers [6] = Utils.createLaser(world, p [5], p [7], kind);
		lasers [7] = Utils.createLaser(world, p [6], p [7], kind);
		lasers [8] = Utils.createLaser(world, p [0], p [4], kind);
		lasers [9] = Utils.createLaser(world, p [1], p [5], kind);
		lasers [10] = Utils.createLaser(world, p [2], p [6], kind);
		lasers [11] = Utils.createLaser(world, p [3], p [7], kind);
		
		return lasers;
	}

	public static void breakBlock(World world, int x, int y, int z) {
		int blockId = world.getBlockId(x, y, z);
		
		if (blockId != 0) {
			Block.blocksList[blockId].dropBlockAsItem(world, x, y, z,
					world.getBlockMetadata(x, y, z));
		}				
		
		world.setBlockWithNotify(x, y, z, 0);
	}
	
	public static void handleDescriptionPacket (Packet230ModLoader packet) {
		int x = packet.dataInt[0];
		int y = packet.dataInt[1];
		int z = packet.dataInt[2];

		if (APIProxy.getWorld().blockExists(x, y, z)) {
			TileEntity tile = APIProxy.getWorld().getBlockTileEntity(x, y, z);

			if (tile instanceof ISynchronizedTile) {
				((ISynchronizedTile) tile).handleDescriptionPacket(packet);

				return;
			}
		}

		BlockIndex index = new BlockIndex(x, y, z);

		if (BuildCraftCore.bufferedDescriptions.containsKey(index)) {
			BuildCraftCore.bufferedDescriptions.remove(index);
		}
		
		BuildCraftCore.bufferedDescriptions.put(index, packet);
	}
	
	public static void handleUpdatePacket (Packet230ModLoader packet) {
		int x = packet.dataInt[0];
		int y = packet.dataInt[1];
		int z = packet.dataInt[2];
		
		if (APIProxy.getWorld().blockExists(x, y, z)) {
			TileEntity tile = APIProxy.getWorld().getBlockTileEntity(x, y, z);
			
			if (tile instanceof ISynchronizedTile) {
				((ISynchronizedTile) tile).handleUpdatePacket(packet);	
				
				return;
			}
		}
	}
	
	public static void handleBufferedDescription (ISynchronizedTile tileSynch) {
		TileEntity tile = (TileEntity) tileSynch;
		BlockIndex index = new BlockIndex(tile.xCoord, tile.yCoord, tile.zCoord);		
		
		if (BuildCraftCore.bufferedDescriptions.containsKey(index)) {
			
			Packet230ModLoader packet = BuildCraftCore.bufferedDescriptions.get(index);
			BuildCraftCore.bufferedDescriptions.remove(index);
			
			tileSynch.handleDescriptionPacket(packet);
		}
	}
	
	/**
	 * Return true if the block given in parameter is pass through (e.g. air,
	 * water...)
	 */
	public static boolean softBlock (int blockId) {
		return blockId == 0 
				|| blockId == Block.waterStill.blockID
				|| blockId == Block.waterMoving.blockID
				|| Block.blocksList [blockId] == null;
	}
	
	/**
	 * Return true if the block cannot be broken, typically bedrock and lava
	 */
	public static boolean unbreakableBlock (int blockId) {
		return blockId == Block.bedrock.blockID
			|| blockId == Block.lavaStill.blockID
			|| blockId == Block.lavaMoving.blockID;
	}
	
	public static int packetIdToInt (PacketIds id) {
		switch (id) {
		case DiamondPipeGUI:
			return 70;
		case AutoCraftingGUI:
			return 71;
		case FillerGUI:
			return 72;
		case TemplateGUI:
			return 73;
		case BuilderGUI:
			return 74;
		case EngineSteamGUI:
			return 75;
		case EngineCombustionGUI:
			return 76;
		default:
			throw new RuntimeException("Invalid GUI id: " + id);
		}
	}
	
	public static PacketIds intToPacketId (int id) {
		switch (id) {
		case 70:
			return PacketIds.DiamondPipeGUI;
		case 71:
			return PacketIds.AutoCraftingGUI;
		case 72:
			return PacketIds.FillerGUI;
		case 73:
			return PacketIds.TemplateGUI;
		case 74:
			return PacketIds.BuilderGUI;
		case 75:
			return PacketIds.EngineSteamGUI;
		case 76:
			return PacketIds.EngineCombustionGUI;
		default:
			throw new RuntimeException("Invalid GUI number: " + id);
		}
	}
	
	public static void preDestroyBlock (World world, int i, int j, int k) {
		TileEntity tile = world.getBlockTileEntity(i, j, k);
		
		if (tile instanceof IInventory && !APIProxy.isClient(world)) {
			dropItems(world, (IInventory) tile, i, j, k);
		}
		
		if (tile instanceof TileBuildCraft) {
			((TileBuildCraft) tile).destroy();
		}
	}
}
