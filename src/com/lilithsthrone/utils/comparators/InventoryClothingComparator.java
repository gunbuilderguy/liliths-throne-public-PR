package com.lilithsthrone.utils.comparators;

import java.util.Comparator;

import com.lilithsthrone.game.inventory.clothing.AbstractClothing;
<<<<<<< Updated upstream
=======
import com.lilithsthrone.game.inventory.enchanting.ItemEffect;
import jdk.jfr.events.ThrowablesEvent;
>>>>>>> Stashed changes

/**
 * @since 0.1.66
 * @version 0.3.8
 * @author Innoxia
 */
public class InventoryClothingComparator implements Comparator<AbstractClothing> {

	@Override
	public int compare(AbstractClothing first, AbstractClothing second) {
<<<<<<< Updated upstream
		// Sort by enchantment known status above all else:
		if(!first.isEnchantmentKnown() && !second.isEnchantmentKnown()) {
			return 0;
		} else if(first.isEnchantmentKnown() && !second.isEnchantmentKnown()) {
			return -1;
		} else if(!first.isEnchantmentKnown() && second.isEnchantmentKnown()) {
			return 1;
		}
		
		int result = first.getRarity().compareTo(second.getRarity());
		
		if (result != 0) {
			return result;
			
		} else {
			// If rarity is equal, sort by set type:
			int resultSet = first.getClothingType().getClothingSet()==second.getClothingType().getClothingSet()
					?0
					:first.getClothingType().getClothingSet()==null
						?-1
						:second.getClothingType().getClothingSet()==null
							?1
							:first.getClothingType().getClothingSet().getName().compareTo(second.getClothingType().getClothingSet().getName());
			
			if(resultSet!=0) {
				return resultSet;
			}
			
			result = first.getClothingType().toString().compareTo(second.getClothingType().toString());
			
			if(result!=0) {
				return result;
			} else {
				if(first.getColour(0)!=null) {
					if(second.getColour(0)!=null) {
						return first.getColour(0).getName().compareTo(second.getColour(0).getName());
					} else {
						return 1;
					}
				}
				return 0;
			}
=======

		//overhauled comparation:
		//!enchant->rarity->z-layer->alphabetical->type->color->enchants
		//nested IFs so you do an equal check first before doing any comparison, it's straight up faster.
		//nearly the speed of an equals() but with binary search, so for 100 articles of clothing, 7 steps max to conclude.
		if(first.isEnchantmentKnown() == second.isEnchantmentKnown()){
			if(first.getRarity().equals(second.getRarity())){
				if(second.getClothingType().getEquipSlots().get(0).equals(first.getClothingType().getEquipSlots().get(0))){
					//already 95-98% of all the clothes
					if(first.getName().equals(second.getName())){
						if(first.getClothingType().toString().equals(second.getClothingType().toString())){
							if(first.getColour(0) == second.getColour(0)){
								if(first.getEffects().equals(second.getEffects())){
									return 0;
								} else if(first.getEffects().size()>second.getEffects().size()){//check by size
									return -1;
								} else if(second.getEffects().size()>first.getEffects().size()){
									return 1;
								} else {
									//check for each
									int result = 0;
									int n = 0;
									while(first.getEffects().size() < n && second.getEffects().size() < n){
										result += first.getEffects().get(n).getSecondaryModifier().getName()
												.compareTo(second.getEffects().get(n).getSecondaryModifier().getName());
										n++;
									}
									result = Math.min(1,Math.max(-1, result));

									if(result == 0){
										return 0;
									} else {
										return result;
									}
								}

							} else if(first.getColour(0) == null){
								return -1;
							} else if(second.getColour(0) == null){
								return 1;
							} else {
								return first.getColour(0).getName().compareTo(second.getColour(0).getName())
							}
						} else {
							return first.getClothingType().toString().compareTo(second.getClothingType().toString());
						}
					} else {
						return first.getName().compareTo(second.getName());
					}
				} else {
					return (int) Math.signum(getClothingOrder(second.getClothingType().getEquipSlots().get(0)) - getClothingOrder(first.getClothingType().getEquipSlots().get(0)));
				}
			} else {
				return -first.getRarity().compareTo(second.getRarity());
			}

		} else if(first.isEnchantmentKnown() && !second.isEnchantmentKnown()) {
			return 1;
		} else {
			return -1;
>>>>>>> Stashed changes
		}

	}
}
