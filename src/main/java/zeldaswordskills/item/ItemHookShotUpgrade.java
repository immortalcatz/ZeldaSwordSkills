/**
    Copyright (C) <2015> <coolAlias>

    This file is part of coolAlias' Zelda Sword Skills Minecraft Mod; as such,
    you can redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software Foundation,
    either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package zeldaswordskills.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import zeldaswordskills.api.item.IUnenchantable;
import zeldaswordskills.creativetab.ZSSCreativeTabs;
import zeldaswordskills.handler.TradeHandler;
import zeldaswordskills.ref.Config;
import zeldaswordskills.ref.ModInfo;
import zeldaswordskills.util.MerchantRecipeHelper;
import zeldaswordskills.util.PlayerUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 
 * All HookShot add-ons belong to this class. Handles adding tooltip and custom trades.
 *
 */
public class ItemHookShotUpgrade extends Item implements IUnenchantable
{
	/** Current types of add-ons available */
	public static enum AddonType { EXTENSION, STONECLAW, MULTI };

	protected static final String[] addonNames = {"Extender","Claw","Multi"};

	@SideOnly(Side.CLIENT)
	private IIcon[] iconArray;

	public ItemHookShotUpgrade() {
		super();
		setMaxStackSize(1);
		setHasSubtypes(true);
		setCreativeTab(ZSSCreativeTabs.tabTools);
	}

	/** Returns this addon's enum Type from stack damage value */
	public AddonType getType(int damage) {
		return (damage > -1 ? AddonType.values()[damage % AddonType.values().length] : AddonType.EXTENSION);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if (!world.isRemote) {
			PlayerUtils.sendTranslatedChat(player, "chat.zss.use.fail.0");
		}
		return stack;
	}

	@Override
	public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
		if (entity.getClass() == EntityVillager.class) {
			if (stack.getItem() instanceof ItemHookShotUpgrade) {
				addSpecialTrade(stack, player, (EntityVillager) entity);
			}
		}
		return true;
	}

	@Override
	public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase entity) {
		return entity instanceof EntityVillager;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int type) {
		return iconArray[getType(type).ordinal()];
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return getUnlocalizedName() + "." + stack.getItemDamage();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		for (int i = 0; i < AddonType.values().length; ++i) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister register) {
		iconArray = new IIcon[AddonType.values().length];
		for (int i = 0; i < AddonType.values().length; ++i) {
			iconArray[i] = register.registerIcon(ModInfo.ID + ":" + addonNames[getType(i).ordinal()].toLowerCase());
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack,	EntityPlayer player, List list, boolean par4) {
		list.add(EnumChatFormatting.ITALIC + StatCollector.translateToLocal("tooltip.zss.hookshot.upgrade.desc.0"));
		list.add(EnumChatFormatting.ITALIC + StatCollector.translateToLocal("tooltip.zss.hookshot.upgrade.desc.1"));
		list.add(EnumChatFormatting.ITALIC + StatCollector.translateToLocal("tooltip.zss.hookshot.upgrade.desc.2"));
	}

	/**
	 * Adds first appropriate trade to blacksmith's trade list, if any
	 */
	public void addSpecialTrade(ItemStack stack, EntityPlayer player, EntityVillager villager) {
		if (!player.worldObj.isRemote) {
			MerchantRecipeList trades = villager.getRecipes(player);
			if (villager.getProfession() == 3 && trades != null) {
				MerchantRecipe trade = getHookShotTradeFromInventory(stack, player, trades);
				if (trade != null && trades.size() >= Config.getFriendTradesRequired()) {
					MerchantRecipeHelper.addUniqueTrade(trades, trade);
					PlayerUtils.sendTranslatedChat(player, "chat.zss.trade.generic.new.0");
				} else {
					trade = new MerchantRecipe(stack.copy(), new ItemStack(Items.emerald, 16));
					if (MerchantRecipeHelper.addToListWithCheck(trades, trade) || player.worldObj.rand.nextFloat() < 0.5F) {
						PlayerUtils.sendTranslatedChat(player, "chat.zss.trade.generic.sell.0");
					} else {
						PlayerUtils.sendTranslatedChat(player, "chat.zss.trade.generic.sorry.1");
					}
				}
			} else {
				PlayerUtils.sendTranslatedChat(player, "chat.zss.trade.generic.sorry.0");
			}
		}
	}

	/**
	 * Returns the first extendable HookShot trade for which the type of base hookshot found 
	 * that also doesn't have a trade recipe already in the merchant's list, or null
	 */
	private MerchantRecipe getHookShotTradeFromInventory(ItemStack stack, EntityPlayer player, MerchantRecipeList list) {
		AddonType type = ((ItemHookShotUpgrade) stack.getItem()).getType(stack.getItemDamage());
		for (ItemStack invStack : player.inventory.mainInventory) {
			if (invStack != null && invStack.getItem() instanceof ItemHookShot) {
				if (!MerchantRecipeHelper.doesListContain(list, TradeHandler.getTrade(type, invStack))) {
					return TradeHandler.getTrade(type, invStack);
				}
			}
		}
		return null;
	}
}
