package mal.lootbags.tileentity;

import mal.lootbags.LootBags;
import mal.lootbags.handler.BagHandler;
import mal.lootbags.loot.LootItem;
import mal.lootbags.network.LootbagsPacketHandler;
import mal.lootbags.network.message.RecyclerMessageServer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
//import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IChatComponent;
//import net.minecraft.util.ITickable;
import net.minecraft.world.World;

public class TileEntityRecycler extends TileEntity implements IInventory, ISidedInventory/*, ITickable*/{

	private ItemStack lootbagSlot;
	private int lootbagCount = 0;
	private int totalValue = 0;
	private ItemStack[] inventory = new ItemStack[27];
	private int lootvalue;
	
	public TileEntityRecycler()
	{
            lootvalue = (int)Math.floor(BagHandler.getBag(LootBags.RECYCLEDID).getBagWeight()*LootBags.TOTALVALUEMULTIPLIER);
	}
	
	@Override
	public void updateEntity()
	{
		if(worldObj != null && !this.worldObj.isRemote)
		{
			//consume inventory to make a lootbag
			for(int i = 0; i < inventory.length; i++)
			{
				if(inventory[i] != null)
					if(LootBags.isItemDroppable(inventory[i]))
					{
						int val = LootBags.getItemValue(inventory[i]);
						if(totalValue <= Integer.MAX_VALUE-val)
						{
							totalValue += val;
							inventory[i].stackSize--;
							if(inventory[i].stackSize <= 0)
								inventory[i] = null;
						}
					}
			}
			
			if(totalValue >= lootvalue && lootbagCount < Integer.MAX_VALUE-1)
			{
				totalValue -= lootvalue;
				lootbagCount += 1;
			}
			
			if(lootbagSlot == null && lootbagCount > 0)
			{
				lootbagSlot = new ItemStack(LootBags.lootbagItem, 1, LootBags.RECYCLEDID);
				lootbagCount--;
			}
			if(lootbagCount <= 0)
				lootbagCount = 0;
			
			LootbagsPacketHandler.instance.sendToAll(new RecyclerMessageServer(this, lootbagCount, totalValue));
		}
	}
	
	public int getTotalBags()
	{
		return (lootbagCount + ((lootbagSlot!=null)?(1):(0)));
	}
	
	public void setData(int count, int value)
	{
		lootbagCount = count;
		totalValue = value;
	}
	
	public int getValue()
	{
		return totalValue;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		
		lootbagCount = nbt.getInteger("lootbagCount");
		totalValue = nbt.getInteger("totalValue");
		
		NBTTagList lootbag = nbt.getTagList("lootbagItem", 10);
		NBTTagCompound var = (NBTTagCompound)lootbag.getCompoundTagAt(0);
		lootbagSlot = ItemStack.loadItemStackFromNBT(var);
		
		
		NBTTagList input = nbt.getTagList("inputItems", 10);
		for (int i = 0; i < input.tagCount(); ++i)
		{
			NBTTagCompound var4 = (NBTTagCompound)input.getCompoundTagAt(i);
			byte var5 = var4.getByte("Slot");

			if (var5 >= 0 && var5 < this.inventory.length)
			{
				this.inventory[var5] = ItemStack.loadItemStackFromNBT(var4);
			}
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		
		nbt.setInteger("lootbagCount", lootbagCount);
		nbt.setInteger("totalValue", totalValue);
		
		NBTTagList lootbag = new NBTTagList();
		if(lootbagSlot != null)
		{
			NBTTagCompound var = new NBTTagCompound();
			var.setByte("Slot", (byte)0);
			lootbagSlot.writeToNBT(var);
			lootbag.appendTag(var);
		}
		nbt.setTag("lootbagItem", lootbag);
		
		NBTTagList input = new NBTTagList();

		for (int i = 0; i < this.inventory.length; ++i)
		{
			if (this.inventory[i] != null)
			{
				NBTTagCompound var4 = new NBTTagCompound();
				this.inventory[i].writeToNBT(var4);
				input.appendTag(var4);
			}
		}
		nbt.setTag("inputItems", input);
	}
	
	@Override
	public int getSizeInventory() {
		return inventory.length+1;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		if(slot == 0)
		{
			return lootbagSlot;
		}
		else if(slot < getSizeInventory())
			return inventory[slot-1];
		else
			return null;
	}

	@Override
	public ItemStack decrStackSize(int slot, int dec) {
		if(slot == 0)
		{
			if(lootbagSlot != null)
			{
				ItemStack is;
				if(lootbagSlot.stackSize <= dec)
				{
					is = lootbagSlot;
					lootbagSlot = null;
					return is;
				}
				else
				{
					is = lootbagSlot.splitStack(dec);
					if(lootbagSlot.stackSize == 0)
						lootbagSlot = null;
					return is;
				}
			}
		}
		else if(slot < getSizeInventory())
		{
			if(inventory[slot-1] != null)
			{
				ItemStack is;
				if(inventory[slot-1].stackSize <= dec)
				{
					is = inventory[slot-1];
					inventory[slot-1] = null;
					return is;
				}
				else
				{
					is = inventory[slot-1].splitStack(dec);
					if(inventory[slot-1].stackSize == 0)
						inventory[slot-1] = null;
					return is;
				}
			}
		}
		return null;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack item) {
		if(slot==0)
		{
			this.lootbagSlot = item;

			if (item != null && item.stackSize > this.getInventoryStackLimit())
			{
				item.stackSize = this.getInventoryStackLimit();
			}
		}
		else if(slot<getSizeInventory())
		{
			inventory[slot-1] = item;

			if (item != null && item.stackSize > this.getInventoryStackLimit())
			{
				item.stackSize = this.getInventoryStackLimit();
			}
		}
		
		this.markDirty();
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer par1EntityPlayer) {
		return this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) != this ? false : par1EntityPlayer.getDistanceSq((double)this.xCoord + 0.5D, (double)this.yCoord + 0.5D, (double)this.zCoord + 0.5D) <= 64.0D;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if(slot == 0)
			return false;
		else if(slot < getSizeInventory())
		{
			if(LootBags.isItemRecyleBlacklisted(stack))
				return false;
			
			if(LootBags.isItemDroppable(stack))
				return true;
		}
		return false;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		int[] i = new int[getSizeInventory()];
		for(int ii = 0; ii<i.length; ii++)
			i[ii]=ii;
		return i;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack,
			int p_102007_3_) {
		if(slot == 0)
			return false;
		else if(slot < getSizeInventory())
		{
			if(LootBags.isItemRecyleBlacklisted(stack))
				return false;
			
			if(LootBags.isItemDroppable(stack))
				return true;
		}
		return false;
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack p_102008_2_,
			int p_102008_3_) {
		if(slot == 0)
			return true;
		return false;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) {
		if(slot==0)
		{
			if(lootbagSlot != null)
			{
				ItemStack var2 = lootbagSlot;
				lootbagSlot = null;
				return var2;
			}
			return null;
		}
		else if(slot>= 0 && slot < this.getSizeInventory())
		{
			if(inventory[slot-1] != null)
			{
				ItemStack var2 = inventory[slot-1];
				inventory[slot-1] = null;
				return var2;	
			}
			return null;
		}
		return null;
	}

	@Override
	public String getInventoryName() {
		return "recycler";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return false;
	}

	@Override
	public void openInventory() {
	}

	@Override
	public void closeInventory() {
	}

	public void activate(World world, int x, int y, int z, EntityPlayer player)
	{
		player.openGui(LootBags.LootBagsInstance, 1, world, x, y, z);
	}
	//TODO: 1.8.9 overrides
	/*public void activate(World world, BlockPos pos, EntityPlayer player) {
		
		player.openGui(LootBags.LootBagsInstance, 1, world, pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public String getName() {
		return "recycler";
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public IChatComponent getDisplayName() {
		return null;
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side) {
		int[] i = new int[getSizeInventory()];
		for(int ii = 0; ii<i.length; ii++)
			i[ii]=ii;
		return i;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack itemStackIn,
			EnumFacing direction) {
		if(slot != 0)
			return true;
		return false;
	}

	@Override
	public boolean canExtractItem(int index, ItemStack stack,
			EnumFacing direction) {
		if(index == 0)
			return true;
		return false;
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		return null;
	}

	@Override
	public void openInventory(EntityPlayer player) {
	}

	@Override
	public void closeInventory(EntityPlayer player) {
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {
	}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {
	}*/

}
/*******************************************************************************
 * Copyright (c) 2016 Malorolam.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the included license.
 * 
 *********************************************************************************/