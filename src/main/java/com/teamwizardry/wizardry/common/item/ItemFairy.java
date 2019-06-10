package com.teamwizardry.wizardry.common.item;

import com.teamwizardry.librarianlib.features.base.item.ItemMod;
import com.teamwizardry.librarianlib.features.helpers.NBTHelper;
import com.teamwizardry.wizardry.api.entity.FairyObject;
import com.teamwizardry.wizardry.common.entity.EntityFairy;
import com.teamwizardry.wizardry.common.tile.TileJar;
import com.teamwizardry.wizardry.init.ModSounds;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class ItemFairy extends ItemMod {

	public ItemFairy() {
		super("fairy_item");

		setMaxStackSize(1);
	}

	@NotNull
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getHeldItem(hand);
		if (stack.isEmpty()) return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);

		FairyObject object = FairyObject.deserialize(NBTHelper.getCompound(stack, "fairy"));
		if (object == null) return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);

		TileEntity tileEntity = worldIn.getTileEntity(pos);
		if (tileEntity instanceof TileJar) {
			TileJar jar = (TileJar) tileEntity;

			if (jar.fairy != null) return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);

			jar.fairy = object;
			jar.markDirty();
			worldIn.checkLight(pos);

		} else {

			EntityFairy entity = new EntityFairy(worldIn, object);
			entity.setPosition(pos.getX(), pos.getY() + 0.5, pos.getZ());

			worldIn.spawnEntity(entity);
		}

		stack.shrink(1);
		worldIn.playSound(pos.getX(), pos.getY(), pos.getZ(), ModSounds.FAIRY, SoundCategory.BLOCKS, 1, 1, false);

		return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
	}
}
