package com.lilithsthrone.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lilithsthrone.game.character.FluidStored;
import com.lilithsthrone.game.inventory.AbstractCoreItem;
import com.lilithsthrone.game.inventory.AbstractFluidStorage;
import com.lilithsthrone.game.inventory.item.AbstractFilledBreastPump;
import com.lilithsthrone.game.inventory.item.AbstractFilledCondom;
import com.lilithsthrone.game.inventory.item.FluidContainerInterface;
import com.lilithsthrone.utils.Util;
import org.w3c.dom.events.EventTarget;

import com.lilithsthrone.controller.eventListeners.InventorySelectedItemEventListener;
import com.lilithsthrone.controller.eventListeners.tooltips.TooltipInformationEventListener;
import com.lilithsthrone.controller.eventListeners.tooltips.TooltipInventoryEventListener;
import com.lilithsthrone.game.combat.DamageType;
import com.lilithsthrone.game.dialogue.responses.Response;
import com.lilithsthrone.game.dialogue.utils.InventoryDialogue;
import com.lilithsthrone.game.dialogue.utils.InventoryInteraction;
import com.lilithsthrone.game.dialogue.utils.UtilText;
import com.lilithsthrone.game.inventory.ColourReplacement;
import com.lilithsthrone.game.inventory.ItemTag;
import com.lilithsthrone.game.inventory.clothing.AbstractClothing;
import com.lilithsthrone.game.inventory.clothing.AbstractClothingType;
import com.lilithsthrone.game.inventory.clothing.Sticker;
import com.lilithsthrone.game.inventory.clothing.StickerCategory;
import com.lilithsthrone.game.inventory.item.AbstractItem;
import com.lilithsthrone.game.inventory.weapon.AbstractWeapon;
import com.lilithsthrone.game.inventory.weapon.AbstractWeaponType;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.rendering.Pattern;
import com.lilithsthrone.rendering.RenderingEngine;
import com.lilithsthrone.utils.colours.Colour;

/**
 * @since 0.4.6.4
 * @version 0.4.6.4
 * @author Maxis010, Innoxia
 */
public class InventoryController {
	
	public static void initInventoryListeners() {
		for (int i = 0; i<RenderingEngine.INVENTORY_PAGES; i++) {
			MainController.setInventoryPageLeft(i);
			MainController.setInventoryPageRight(i);
		}
		// Quest inventory:
		MainController.setInventoryPageLeft(5);
		
		// Player:
		String id;
		for (Map.Entry<AbstractWeapon, Integer> entry : Main.game.getPlayer().getAllWeaponsInInventory().entrySet()) {
			id = "PLAYER_WEAPON_"+entry.getKey().hashCode();
			if (MainController.document.getElementById(id) != null) {
				MainController.addTooltipListeners(id,
						new TooltipInventoryEventListener().setWeapon(entry.getKey(), Main.game.getPlayer(), false),
						new InventorySelectedItemEventListener().setWeaponInventory(entry.getKey(), Main.game.getPlayer()), false);
			}
		}
		for (Map.Entry<AbstractItem, Integer> entry : Main.game.getPlayer().getAllItemsInInventory().entrySet()) {
			id = "PLAYER_ITEM_"+entry.getKey().hashCode();
			if (MainController.document.getElementById(id) != null) {
				MainController.addTooltipListeners(id,
						new TooltipInventoryEventListener().setItem(entry.getKey(), Main.game.getPlayer(), null),
						new InventorySelectedItemEventListener().setItemInventory(entry.getKey(), Main.game.getPlayer()), false);
			}
		}
		for (Map.Entry<AbstractClothing, Integer> entry : Main.game.getPlayer().getAllClothingInInventory().entrySet()) {
			id = "PLAYER_CLOTHING_"+entry.getKey().hashCode();
			if (MainController.document.getElementById(id) != null) {
				MainController.addTooltipListeners(id,
						new TooltipInventoryEventListener().setClothing(entry.getKey(), Main.game.getPlayer(), null),
						new InventorySelectedItemEventListener().setClothingInventory(entry.getKey(), Main.game.getPlayer()), false);
			}
		}
		id = "PLAYER_MONEY_TRANSFER_SMALL";
		if (MainController.document.getElementById(id) != null) {
			((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
				if (Main.game.getPlayer().getMoney()>0 && InventoryDialogue.getNPCInventoryInteraction() == InventoryInteraction.FULL_MANAGEMENT) {
					if (InventoryDialogue.getInventoryNPC() == null) {
						Main.game.getPlayerCell().getInventory().incrementMoney((int) Math.max(1, Main.game.getPlayer().getMoney()*0.01f));
					} else {
						InventoryDialogue.getInventoryNPC().incrementMoney((int) Math.max(1, Main.game.getPlayer().getMoney()*0.01f));
					}
					Main.game.getPlayer().incrementMoney((int) -Math.max(1, Main.game.getPlayer().getMoney()*0.01f));
					Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
				}
			}, false);
			MainController.addTooltipListeners(id, new TooltipInformationEventListener().setMoneyTransferTarget(Main.game.getPlayer(), InventoryDialogue.getInventoryNPC(), 1));
		}
		id = "PLAYER_MONEY_TRANSFER_AVERAGE";
		if (MainController.document.getElementById(id) != null) {
			((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
				if (Main.game.getPlayer().getMoney()>0 && InventoryDialogue.getNPCInventoryInteraction() == InventoryInteraction.FULL_MANAGEMENT) {
					if (InventoryDialogue.getInventoryNPC() == null) {
						Main.game.getPlayerCell().getInventory().incrementMoney((int) Math.max(1, Main.game.getPlayer().getMoney()*0.1f));
					} else {
						InventoryDialogue.getInventoryNPC().incrementMoney((int) Math.max(1, Main.game.getPlayer().getMoney()*0.1f));
					}
					Main.game.getPlayer().incrementMoney((int) -Math.max(1, Main.game.getPlayer().getMoney()*0.1f));
					Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
				}
			}, false);
			MainController.addTooltipListeners(id, new TooltipInformationEventListener().setMoneyTransferTarget(Main.game.getPlayer(), InventoryDialogue.getInventoryNPC(), 10));
		}
		id = "PLAYER_MONEY_TRANSFER_BIG";
		if (MainController.document.getElementById(id) != null) {
			((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
				if (Main.game.getPlayer().getMoney()>0 && InventoryDialogue.getNPCInventoryInteraction() == InventoryInteraction.FULL_MANAGEMENT) {
					if (InventoryDialogue.getInventoryNPC() == null) {
						Main.game.getPlayerCell().getInventory().incrementMoney(Math.max(1, Main.game.getPlayer().getMoney()));
					} else {
						InventoryDialogue.getInventoryNPC().incrementMoney(Math.max(1, Main.game.getPlayer().getMoney()));
					}
					Main.game.getPlayer().incrementMoney(-Math.max(1, Main.game.getPlayer().getMoney()));
					Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
				}
			}, false);
			MainController.addTooltipListeners(id, new TooltipInformationEventListener().setMoneyTransferTarget(Main.game.getPlayer(), InventoryDialogue.getInventoryNPC(), 100));
		}
		
		// Partner:
		if (InventoryDialogue.getInventoryNPC() != null) {
			String idModifier = "NPC_"+InventoryDialogue.getInventoryNPC().getId()+"_";
			for (Map.Entry<AbstractWeapon, Integer> entry : InventoryDialogue.getInventoryNPC().getAllWeaponsInInventory().entrySet()) {
				id = idModifier+"WEAPON_"+entry.getKey().hashCode();
				if (MainController.document.getElementById(id) != null) {
					MainController.addTooltipListeners(id,
							new TooltipInventoryEventListener().setWeapon(entry.getKey(), InventoryDialogue.getInventoryNPC(), false),
							new InventorySelectedItemEventListener().setWeaponInventory(entry.getKey(), InventoryDialogue.getInventoryNPC()), false);
				}
			}
			
			for (Map.Entry<AbstractClothing, Integer> entry : InventoryDialogue.getInventoryNPC().getAllClothingInInventory().entrySet()) {
				id = idModifier+"CLOTHING_"+entry.getKey().hashCode();
				if (MainController.document.getElementById(id) != null) {
					MainController.addTooltipListeners(id,
							new TooltipInventoryEventListener().setClothing(entry.getKey(), InventoryDialogue.getInventoryNPC(), null),
							new InventorySelectedItemEventListener().setClothingInventory(entry.getKey(), InventoryDialogue.getInventoryNPC()), false);
				}
			}
			
			for (Map.Entry<AbstractItem, Integer> entry : InventoryDialogue.getInventoryNPC().getAllItemsInInventory().entrySet()) {
				id = idModifier+"ITEM_"+entry.getKey().hashCode();
				if (MainController.document.getElementById(id) != null) {
					MainController.addTooltipListeners(id,
							new TooltipInventoryEventListener().setItem(entry.getKey(), InventoryDialogue.getInventoryNPC(), null),
							new InventorySelectedItemEventListener().setItemInventory(entry.getKey(), InventoryDialogue.getInventoryNPC()), false);
				}
			}
			id = idModifier+"MONEY_TRANSFER_SMALL";
			if (MainController.document.getElementById(id) != null) {
				((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
					if (InventoryDialogue.getInventoryNPC().getMoney()>0 && InventoryDialogue.getNPCInventoryInteraction() == InventoryInteraction.FULL_MANAGEMENT) {
						Main.game.getPlayer().incrementMoney((int) Math.max(1, InventoryDialogue.getInventoryNPC().getMoney()*0.01f));
						InventoryDialogue.getInventoryNPC().incrementMoney((int) -Math.max(1, InventoryDialogue.getInventoryNPC().getMoney()*0.01f));
						Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
					}
				}, false);
				MainController.addTooltipListeners(id, new TooltipInformationEventListener().setMoneyTransferTarget(InventoryDialogue.getInventoryNPC(), Main.game.getPlayer(), 1));
			}
			id = idModifier+"MONEY_TRANSFER_AVERAGE";
			if (MainController.document.getElementById(id) != null) {
				((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
					if (InventoryDialogue.getInventoryNPC().getMoney()>0 && InventoryDialogue.getNPCInventoryInteraction() == InventoryInteraction.FULL_MANAGEMENT) {
						Main.game.getPlayer().incrementMoney((int) Math.max(1, InventoryDialogue.getInventoryNPC().getMoney()*0.1f));
						InventoryDialogue.getInventoryNPC().incrementMoney((int) -Math.max(1, InventoryDialogue.getInventoryNPC().getMoney()*0.1f));
						Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
					}
				}, false);
				MainController.addTooltipListeners(id, new TooltipInformationEventListener().setMoneyTransferTarget(InventoryDialogue.getInventoryNPC(), Main.game.getPlayer(), 10));
			}
			id = idModifier+"MONEY_TRANSFER_BIG";
			if (MainController.document.getElementById(id) != null) {
				((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
					if (InventoryDialogue.getInventoryNPC().getMoney()>0 && InventoryDialogue.getNPCInventoryInteraction() == InventoryInteraction.FULL_MANAGEMENT) {
						Main.game.getPlayer().incrementMoney(Math.max(1, InventoryDialogue.getInventoryNPC().getMoney()));
						InventoryDialogue.getInventoryNPC().incrementMoney(-Math.max(1, InventoryDialogue.getInventoryNPC().getMoney()));
						Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
					}
				}, false);
				MainController.addTooltipListeners(id, new TooltipInformationEventListener().setMoneyTransferTarget(InventoryDialogue.getInventoryNPC(), Main.game.getPlayer(), 100));
			}
		} else {
			// Weapons on floor:
			for (Map.Entry<AbstractWeapon, Integer> entry : Main.game.getPlayerCell().getInventory().getAllWeaponsInInventory().entrySet()) {
				id = "FLOOR_WEAPON_"+entry.getKey().hashCode();
				if (MainController.document.getElementById(id) != null) {
					MainController.addTooltipListeners(id,
							new TooltipInventoryEventListener().setWeapon(entry.getKey(), null, false),
							new InventorySelectedItemEventListener().setWeaponInventory(entry.getKey(), null), false);
				}
			}
			
			// Clothing on floor:
			for (Map.Entry<AbstractClothing, Integer> entry : Main.game.getPlayerCell().getInventory().getAllClothingInInventory().entrySet()) {
				id = "FLOOR_CLOTHING_"+entry.getKey().hashCode();
				if (MainController.document.getElementById(id) != null) {
					MainController.addTooltipListeners(id,
							new TooltipInventoryEventListener().setClothing(entry.getKey(), null, null),
							new InventorySelectedItemEventListener().setClothingInventory(entry.getKey(), null), false);
				}
			}
			
			// Items on floor:
			for (Map.Entry<AbstractItem, Integer> entry : Main.game.getPlayerCell().getInventory().getAllItemsInInventory().entrySet()) {
				id = "FLOOR_ITEM_"+entry.getKey().hashCode();
				if (MainController.document.getElementById(id) != null) {
					MainController.addTooltipListeners(id,
							new TooltipInventoryEventListener().setItem(entry.getKey(), null, null),
							new InventorySelectedItemEventListener().setItemInventory(entry.getKey(), null), false);
				}
			}
			id = "FLOOR_MONEY_TRANSFER_SMALL";
			if (MainController.document.getElementById(id) != null) {
				((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
					if (Main.game.getPlayerCell().getInventory().getMoney()>0 && InventoryDialogue.getNPCInventoryInteraction() == InventoryInteraction.FULL_MANAGEMENT) {
						Main.game.getPlayer().incrementMoney((int) Math.max(1, Main.game.getPlayerCell().getInventory().getMoney()*0.01f));
						Main.game.getPlayerCell().getInventory().incrementMoney((int) -Math.max(1, Main.game.getPlayerCell().getInventory().getMoney()*0.01f));
						Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
					}
				}, false);
				MainController.addTooltipListeners(id, new TooltipInformationEventListener().setMoneyTransferTarget(null, Main.game.getPlayer(), 1));
			}
			id = "FLOOR_MONEY_TRANSFER_AVERAGE";
			if (MainController.document.getElementById(id) != null) {
				((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
					if (Main.game.getPlayerCell().getInventory().getMoney()>0 && InventoryDialogue.getNPCInventoryInteraction() == InventoryInteraction.FULL_MANAGEMENT) {
						Main.game.getPlayer().incrementMoney((int) Math.max(1, Main.game.getPlayerCell().getInventory().getMoney()*0.1f));
						Main.game.getPlayerCell().getInventory().incrementMoney((int) -Math.max(1, Main.game.getPlayerCell().getInventory().getMoney()*0.1f));
						Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
					}
				}, false);
				MainController.addTooltipListeners(id, new TooltipInformationEventListener().setMoneyTransferTarget(null, Main.game.getPlayer(), 10));
			}
			id = "FLOOR_MONEY_TRANSFER_BIG";
			if (MainController.document.getElementById(id) != null) {
				((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
					if (Main.game.getPlayerCell().getInventory().getMoney()>0 && InventoryDialogue.getNPCInventoryInteraction() == InventoryInteraction.FULL_MANAGEMENT) {
						Main.game.getPlayer().incrementMoney(Math.max(1, Main.game.getPlayerCell().getInventory().getMoney()));
						Main.game.getPlayerCell().getInventory().incrementMoney(-Math.max(1, Main.game.getPlayerCell().getInventory().getMoney()));
						Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
					}
				}, false);
				MainController.addTooltipListeners(id, new TooltipInformationEventListener().setMoneyTransferTarget(null, Main.game.getPlayer(), 100));
			}
		}
		
		if (InventoryDialogue.getNPCInventoryInteraction() == InventoryInteraction.TRADING) {
			if (InventoryDialogue.getInventoryNPC() != null) {
				// Buyback panel:
				for (int i = Main.game.getPlayer().getBuybackStack().size()-1; i>=0; i--) {
					if (MainController.document.getElementById("WEAPON_"+i) != null) {
						MainController.addTooltipListeners("WEAPON_"+i,
								new TooltipInventoryEventListener().setWeapon((AbstractWeapon) Main.game.getPlayer().getBuybackStack().get(i).getAbstractItemSold(), InventoryDialogue.getInventoryNPC(), false),
								new InventorySelectedItemEventListener().setWeaponInventory((AbstractWeapon) Main.game.getPlayer().getBuybackStack().get(i).getAbstractItemSold(), InventoryDialogue.getInventoryNPC(), i), false);
					} else if (MainController.document.getElementById("CLOTHING_"+i) != null) {
						MainController.addTooltipListeners("CLOTHING_"+i,
								new TooltipInventoryEventListener().setClothing((AbstractClothing) Main.game.getPlayer().getBuybackStack().get(i).getAbstractItemSold(), InventoryDialogue.getInventoryNPC(), null),
								new InventorySelectedItemEventListener().setClothingInventory((AbstractClothing) Main.game.getPlayer().getBuybackStack().get(i).getAbstractItemSold(), InventoryDialogue.getInventoryNPC(), i), false);
					} else if (MainController.document.getElementById("ITEM_"+i) != null) {
						MainController.addTooltipListeners("ITEM_"+i,
								new TooltipInventoryEventListener().setItem((AbstractItem) Main.game.getPlayer().getBuybackStack().get(i).getAbstractItemSold(), InventoryDialogue.getInventoryNPC(), null),
								new InventorySelectedItemEventListener().setItemInventory((AbstractItem) Main.game.getPlayer().getBuybackStack().get(i).getAbstractItemSold(), InventoryDialogue.getInventoryNPC(), i), false);
					}
				}
			}
		}
	}
	
	public static void initClothingDyeListeners() {
		AbstractClothingType clothing = InventoryDialogue.getClothing().getClothingType();
		for (int i = 0; i<clothing.getColourReplacements().size(); i++) {
			int index = i;
			ColourReplacement cr = clothing.getColourReplacement(i);
			for (Colour c : cr.getAllColours()) {
				String id = "DYE_CLOTHING_"+i+"_"+c.getId();
				if (MainController.document.getElementById(id) != null) {
					((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
						if (cr.isRecolouringAllowed()) {
							InventoryDialogue.dyePreviews.remove(index);
							InventoryDialogue.dyePreviews.add(index, c);
						}
						Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
					}, false);
					MainController.addTooltipListeners(id, new TooltipInventoryEventListener().setDyeClothing(InventoryDialogue.getClothing(), i, c));
				}
			}
		}
	}
	
	public static void initPatternListeners() {
		for (Pattern pattern : Pattern.getAllPatterns()) {
			String id = "ITEM_PATTERN_"+pattern.getId();
			if (MainController.document.getElementById(id) != null) {
				((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
					if (!InventoryDialogue.dyePreviewPattern.equals(pattern.getId())) {
						InventoryDialogue.dyePreviewPattern = pattern.getId();
						Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
					}
				}, false);
			}
		}
	}
	
	public static void initPatternRecolourListeners() {
		AbstractClothingType clothing = InventoryDialogue.getClothing().getClothingType();
		for (int i = 0; i<clothing.getPatternColourReplacements().size(); i++) {
			ColourReplacement cr = clothing.getPatternColourReplacement(i);
			for (Colour c : cr.getAllColours()) {
				String id = "DYE_CLOTHING_PATTERN_"+i+"_"+c.getId();
				if (MainController.document.getElementById(id) != null) {
					int finalI = i;
					((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
						InventoryDialogue.dyePreviewPatterns.remove(finalI);
						InventoryDialogue.dyePreviewPatterns.add(finalI, c);
						Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
					}, false);
					MainController.addTooltipListeners(id, new TooltipInventoryEventListener().setDyeClothingPattern(InventoryDialogue.getClothing(), i, c));
				}
			}
		}
	}
	
	public static void initStickerListeners() {
		AbstractClothingType clothing = InventoryDialogue.getClothing().getClothingType();
		for (Map.Entry<StickerCategory, List<Sticker>> stickerEntry : clothing.getStickers().entrySet()) {
			for (Sticker s : stickerEntry.getValue()) {
				String id = "ITEM_STICKER_"+stickerEntry.getKey().getId()+s.getId();
				String requirements = UtilText.parse(s.getUnavailabilityText()).trim();
				if (MainController.document.getElementById(id) != null) {
					((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
						if (InventoryDialogue.dyePreviewStickers.get(stickerEntry.getKey()) != s && requirements.isEmpty()) {
							InventoryDialogue.dyePreviewStickers.put(stickerEntry.getKey(), s);
							Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
						}
					}, false);
					
					int lineHeight = 0;
					StringBuilder tooltipDescriptionSB = new StringBuilder();
					if (!requirements.isEmpty()) {
						tooltipDescriptionSB.append("[style.boldBad(Locked:)] <i>"+requirements+"</i><br/>");
						lineHeight += 2;
					} else {
						if (!s.getAvailabilityText().isEmpty()) {
							tooltipDescriptionSB.append("[style.boldGood(Unlocked:)] <i>"+s.getAvailabilityText()+"</i><br/>");
							lineHeight += 2;
						}
						
						boolean tagApplicationFound = false;
						for (ItemTag tag : s.getTagsApplied()) {
							for (String tagTooltip : tag.getClothingTooltipAdditions()) {
								if (!tagApplicationFound) {
									tooltipDescriptionSB.append("[style.boldMinorGood(Effects added:)]<br/>");
									tagApplicationFound = true;
									lineHeight++;
								}
								tooltipDescriptionSB.append(tagTooltip+"<br/>");
								lineHeight++;
							}
						}
						
						tagApplicationFound = false;
						for (ItemTag tag : s.getTagsRemoved()) {
							for (String tagTooltip : tag.getClothingTooltipAdditions()) {
								if (!tagApplicationFound) {
									tooltipDescriptionSB.append("[style.boldMinorBad(Effects removed:)]<br/>");
									tagApplicationFound = true;
									lineHeight++;
								}
								tooltipDescriptionSB.append(tagTooltip+"<br/>");
								lineHeight++;
							}
						}
					}
					TooltipInformationEventListener el = new TooltipInformationEventListener();
					if (lineHeight>0) {
						el.setInformation("Special Effects",
								tooltipDescriptionSB.toString(),
								(lineHeight*16));
					} else {
						el.setInformation("No Special Effects", "");
					}
					MainController.addTooltipListeners(id, el);
				}
			}
		}
	}
	
	public static void initWeaponDyeListeners() {
		AbstractWeaponType weapon = InventoryDialogue.getWeapon().getWeaponType();
		for (int i = 0; i<weapon.getColourReplacements(false).size(); i++) {
			int index = i;
			ColourReplacement cr = weapon.getColourReplacement(false, i);
			for (Colour c : cr.getAllColours()) {
				String id = "DYE_WEAPON_"+i+"_"+c.getId();
				if (MainController.document.getElementById(id) != null) {
					((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
						if (cr.isRecolouringAllowed()) {
							InventoryDialogue.dyePreviews.remove(index);
							InventoryDialogue.dyePreviews.add(index, c);
						}
						Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
					}, false);
					MainController.addTooltipListeners(id, new TooltipInventoryEventListener().setDyeWeapon(InventoryDialogue.getWeapon(), i, c));
				}
			}
		}
	}

	public static void initFluidManagementListeners() {
		for (AbstractCoreItem item : InventoryDialogue.usableItems.keySet()) {
			String id = "CHOOSE_"+item.hashCode();
			if (MainController.document.getElementById(id) != null) {

				((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
					if(InventoryDialogue.selectedItem == null && InventoryDialogue.chosenItem == item){
						//nothing
					} else if(InventoryDialogue.chosenItem == item
						|| InventoryDialogue.selectedItem == item){ //if already in the selected/chosen, swap the two around
						AbstractCoreItem it = InventoryDialogue.chosenItem;
						InventoryDialogue.chosenItem = InventoryDialogue.selectedItem;
						InventoryDialogue.selectedItem = it;
					} else {
						InventoryDialogue.chosenItem = InventoryDialogue.selectedItem;
						InventoryDialogue.selectedItem = item;
					}
					InventoryDialogue.selectedFluid.clear();
					InventoryDialogue.selectedItemPreview = InventoryDialogue.usableItems.get(InventoryDialogue.selectedItem);
					InventoryDialogue.chosenItemPreview = InventoryDialogue.usableItems.get(InventoryDialogue.chosenItem);
					Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
				}, false);

				if(item instanceof FluidContainerInterface){
					if(item instanceof AbstractFilledCondom){
						MainController.addTooltipListeners(id, new TooltipInventoryEventListener().setItem((AbstractFilledCondom) item, Main.game.getPlayer(), null));
					} else if(item instanceof AbstractFilledBreastPump){
						MainController.addTooltipListeners(id, new TooltipInventoryEventListener().setItem((AbstractFilledBreastPump) item, Main.game.getPlayer(), null));
					}
				} else if(item instanceof AbstractItem){
					MainController.addTooltipListeners(id, new TooltipInventoryEventListener().setItem((AbstractItem) item, Main.game.getPlayer(), null));
				} else if(item instanceof AbstractWeapon){
					MainController.addTooltipListeners(id, new TooltipInventoryEventListener().setWeapon((AbstractWeapon) item, Main.game.getPlayer(), false));
				} else if(item instanceof AbstractClothing){
					MainController.addTooltipListeners(id, new TooltipInventoryEventListener().setClothing((AbstractClothing) item, Main.game.getPlayer(), null));
				}
			}
		}
		Map<String, Integer> mappings = Util.newHashMapOfValues(
				new Util.Value<>("MIN", -1000000000),
				new Util.Value<>("DECREASE_GIANT", -1000),
				new Util.Value<>("DECREASE_HUGE", -100),
				new Util.Value<>("DECREASE_LARGE", -10),
				new Util.Value<>("DECREASE", -1),
				new Util.Value<>("INCREASE", 1),
				new Util.Value<>("INCREASE_LARGE", 10),
				new Util.Value<>("INCREASE_HUGE", 100),
				new Util.Value<>("INCREASE_GIANT", 1000),
				new Util.Value<>("MAX", 1000000000));
		if(!InventoryDialogue.preciseTransfer){
			mappings = Util.newHashMapOfValues(
					new Util.Value<>("MIN", -1000000000),
					new Util.Value<>("DECREASE_LARGE", -(int)InventoryDialogue.selectedMillilitres/2),
					new Util.Value<>("DECREASE", -(int)InventoryDialogue.selectedMillilitres/8),
					new Util.Value<>("INCREASE", (int)InventoryDialogue.selectedMillilitres/8),
					new Util.Value<>("INCREASE_LARGE", (int)InventoryDialogue.selectedMillilitres/2),
					new Util.Value<>("MAX", 1000000000));
		}
		for (Map.Entry<String, Integer> entry : mappings.entrySet()) {
			String i = entry.getKey();
			int j = entry.getValue();
			String id = "FLUID_TRANSFER_" + i;

			if (MainController.document.getElementById(id) != null) {
				((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
					InventoryDialogue.fluidAmount = Math.max(0,Math.min(InventoryDialogue.selectedMillilitres, InventoryDialogue.fluidAmount+j));
					Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
				}, false);
			}
		}

		String id = "FLUID_TRANSFER";

		if (MainController.document.getElementById(id) != null) {
			((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
				float total = (float) InventoryDialogue.selectedFluid.stream().mapToDouble(FluidStored::getMillilitres).sum();
				Float quantity = Math.min(total, InventoryDialogue.fluidAmount);
				FluidContainerInterface realGiver = (FluidContainerInterface)InventoryDialogue.chosenItem;
				FluidContainerInterface realReceiver = (FluidContainerInterface)InventoryDialogue.selectedItem;

				FluidContainerInterface giver = (FluidContainerInterface) InventoryDialogue.usableItems.get(realGiver);
				FluidContainerInterface receiver = (FluidContainerInterface) InventoryDialogue.usableItems.get(realReceiver);

				//System.out.println("uhh?" + (giver == realGiver) + " equals?: " + giver.equals(realGiver));
				System.out.println("preview is orig?: " + (giver == realGiver) + " equals?: " + giver.equals(realGiver) + ". " + giver + ", " + realGiver);
				if(!InventoryDialogue.preciseTransfer || InventoryDialogue.selectedFluid.isEmpty()){
					List<FluidStored> fluids = new ArrayList<>(giver.getStoredFluids());
					float totalVolume = giver.getMillilitresStored();
					for(FluidStored fluid : fluids){
						float drained = quantity*fluid.getMillilitres()/totalVolume;
						receiver.addFluid(fluid, drained);
						giver.removeFluid(fluid, drained);

						FluidStored infoFluid = new FluidStored(fluid);
						infoFluid.setMillilitres(drained);
						Object[] log = {realGiver, infoFluid, realReceiver};
						InventoryDialogue.transferLog.add(log);

					}
				} else {
					List<FluidStored> newFluids = new ArrayList<>();

//					System.out.println("giver liquids: qt Tr =" + quantity);
//					for(FluidStored fluid : ((FluidContainerInterface) InventoryDialogue.chosenItemPreview).getStoredFluids()){
//
//						System.out.println("{" + fluid.hashCode() + ", q=" + fluid.getMillilitres() + "}");
//					}
					for(FluidStored fluid : InventoryDialogue.selectedFluid) {
						float amount = quantity*fluid.getMillilitres()/total;
						receiver.addFluid(fluid, amount);
						giver.removeFluid(fluid, amount);
						FluidStored updatedFluid = giver.getFluid(fluid);
						if(updatedFluid!= null){
							newFluids.add(updatedFluid);
//							System.out.println("is not null");
						}
//						System.out.println("is null");

						FluidStored infoFluid = new FluidStored(fluid);
						infoFluid.setMillilitres(amount);
						Object[] log = {realGiver, infoFluid, realReceiver};
						Object[] prevlog = {null,null,null};
						if(InventoryDialogue.transferLog.size()>0){
							prevlog = InventoryDialogue.transferLog.get(InventoryDialogue.transferLog.size()-1);
						}

//						System.out.println("LOG giver: " + log[0] + " fluid: " + log[1] + " reciever: " + log[2]);
//						System.out.println("PREVLOG giver: " + prevlog[0] + " fluid: " + prevlog[1] + " reciever: " + prevlog[2]);
//						System.out.println("isfluid same? " + log[1].equals(log[1]));
						//some simple logcrushing
						if(log[0] == prevlog[0]
							&& log[2] == prevlog[2]
							&& ((FluidStored)log[1]).equals(prevlog[1], false)){
							FluidStored prevFLuid = ((FluidStored)prevlog[1]);
							prevFLuid.setMillilitres(prevFLuid.getMillilitres() + ((FluidStored)log[1]).getMillilitres());
//							System.out.println("crushSame");
						} else if(log[0] == prevlog[2]
							&& log[2] == prevlog[0]
							&& ((FluidStored)log[1]).equals(prevlog[1], false)){
							FluidStored prevFLuid = ((FluidStored)prevlog[1]);
							if(prevFLuid.getMillilitres()>((FluidStored)log[1]).getMillilitres()){
//								System.out.println("crushOtherSub");
								prevFLuid.setMillilitres(prevFLuid.getMillilitres() - ((FluidStored)log[1]).getMillilitres());
							} else {
								//swapping logs around ex:
								// log1 = B1 -> F1 (46ml) -> B3
								// then log2 = B3 -> F1(48ml) -> B1
								// crush is log2 deleted, log1 = B3 -> F1(2ml) -> B1
								prevFLuid.setMillilitres(((FluidStored)log[1]).getMillilitres() - prevFLuid.getMillilitres());
								prevlog[0] = log[2];
								prevlog[2] = log[0];
//								System.out.println("crushOtherSwap");
							}
						} else {
							InventoryDialogue.transferLog.add(log);
//							System.out.println("addLog");
						}
					}
//					System.out.println("uh");
					InventoryDialogue.selectedFluid.clear();
					InventoryDialogue.selectedFluid.addAll(newFluids);
//					System.out.println("new giver liquids: qt Tr =" + quantity);
//					for(FluidStored fluid : ((FluidContainerInterface) InventoryDialogue.chosenItemPreview).getStoredFluids()){
//
//						System.out.println("{" + fluid.hashCode() + ", q=" + fluid.getMillilitres() + "}");
//					}
				}

				Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
			}, false);
		}

		if(InventoryDialogue.chosenItemPreview instanceof FluidContainerInterface){
			for(FluidStored fluid : ((FluidContainerInterface)InventoryDialogue.chosenItemPreview).getStoredFluids()){
				id = "CHOOSE_FLUID_" + fluid.hashCode() + "_" + InventoryDialogue.chosenItemPreview.hashCode();
				if (MainController.document.getElementById(id) != null) {
					((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
						if(InventoryDialogue.selectedFluid.contains(fluid)){
							InventoryDialogue.selectedFluid.remove(fluid);
						} else {
							InventoryDialogue.selectedFluid.add(fluid);
						}
						Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
					}, false);
				}
			}
		}

	}

	public static void initDamageTypeListeners() {
		AbstractWeaponType weapon = InventoryDialogue.getWeapon().getWeaponType();
		for (DamageType dt : weapon.getAvailableDamageTypes()) {
			String id = "DAMAGE_TYPE_"+dt.toString();
			if (MainController.document.getElementById(id) != null) {
				((EventTarget) MainController.document.getElementById(id)).addEventListener("click", e->{
					InventoryDialogue.damageTypePreview = dt;
					Main.game.setContent(new Response("", "", Main.game.getCurrentDialogueNode()));
				}, false);
				MainController.addTooltipListeners(id, new TooltipInventoryEventListener().setDamageTypeWeapon(InventoryDialogue.getWeapon(), dt));
			}
		}
	}
}
