package com.lilithsthrone.game.dialogue.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.lilithsthrone.controller.eventListeners.tooltips.TooltipInformationEventListener;
import com.lilithsthrone.game.Game;
import com.lilithsthrone.game.character.FluidStored;
import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.character.body.CoverableArea;
import com.lilithsthrone.game.character.body.types.LegType;
import com.lilithsthrone.game.character.body.valueEnums.Femininity;
import com.lilithsthrone.game.character.body.valueEnums.FluidModifier;
import com.lilithsthrone.game.character.effects.StatusEffect;
import com.lilithsthrone.game.character.fetishes.Fetish;
import com.lilithsthrone.game.character.npc.NPC;
import com.lilithsthrone.game.character.quests.QuestLine;
import com.lilithsthrone.game.character.race.AbstractRace;
import com.lilithsthrone.game.character.race.Race;
import com.lilithsthrone.game.combat.DamageType;
import com.lilithsthrone.game.combat.moves.CombatMove;
import com.lilithsthrone.game.combat.spells.SpellSchool;
import com.lilithsthrone.game.dialogue.DialogueFlagValue;
import com.lilithsthrone.game.dialogue.DialogueNode;
import com.lilithsthrone.game.dialogue.DialogueNodeType;
import com.lilithsthrone.game.dialogue.companions.SlaveDialogue;
import com.lilithsthrone.game.dialogue.eventLog.EventLogEntry;
import com.lilithsthrone.game.dialogue.eventLog.EventLogEntryEncyclopediaUnlock;
import com.lilithsthrone.game.dialogue.responses.Response;
import com.lilithsthrone.game.dialogue.responses.ResponseEffectsOnly;
import com.lilithsthrone.game.dialogue.story.CharacterCreation;
import com.lilithsthrone.game.inventory.*;
import com.lilithsthrone.game.inventory.clothing.AbstractClothing;
import com.lilithsthrone.game.inventory.clothing.BlockedParts;
import com.lilithsthrone.game.inventory.clothing.DisplacementType;
import com.lilithsthrone.game.inventory.clothing.Sticker;
import com.lilithsthrone.game.inventory.clothing.StickerCategory;
import com.lilithsthrone.game.inventory.enchanting.ItemEffect;
import com.lilithsthrone.game.inventory.enchanting.ItemEffectType;
import com.lilithsthrone.game.inventory.enchanting.TFModifier;
import com.lilithsthrone.game.inventory.enchanting.TFPotency;
import com.lilithsthrone.game.inventory.item.*;
import com.lilithsthrone.game.inventory.weapon.AbstractWeapon;
import com.lilithsthrone.game.occupantManagement.MilkingRoom;
import com.lilithsthrone.game.sex.sexActions.SexActionUtility;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.rendering.Pattern;
import com.lilithsthrone.rendering.RenderingEngine;
import com.lilithsthrone.rendering.SVGImages;
import com.lilithsthrone.utils.SvgUtil;
import com.lilithsthrone.utils.Units;
import com.lilithsthrone.utils.Util;
import com.lilithsthrone.utils.Util.Value;
import com.lilithsthrone.utils.colours.Colour;
import com.lilithsthrone.utils.colours.PresetColour;
import com.lilithsthrone.utils.comparators.ClothingZLayerComparator;
import javafx.scene.paint.Color;

/**
 * @since 0.1.0
 * @version 0.3.9.5
 * @author Innoxia
 */
public class InventoryDialogue {

	private static final int IDENTIFICATION_PRICE = 400;
	private static final int IDENTIFICATION_ESSENCE_PRICE = 3;

	private static AbstractItem item;
	private static AbstractClothing clothing;
	private static AbstractWeapon weapon;
	private static InventorySlot weaponSlot;
	private static GameCharacter owner;

	private static NPC inventoryNPC;
	private static InventoryInteraction interactionType;

	private static StringBuilder inventorySB = new StringBuilder();

	private static boolean buyback;

	private static int buyBackPrice;
	private static int buyBackIndex;

	public static DamageType damageTypePreview;

	public static List<Colour> dyePreviews;
	public static String dyePreviewPattern;
	public static List<Colour> dyePreviewPatterns;

	public static Map<StickerCategory, Sticker> dyePreviewStickers;

	public static Map<String, String> getDyePreviewStickersAsStrings() {
		Map<String, String> stickerIds = new HashMap<>();
		for(Entry<StickerCategory, Sticker> entry : dyePreviewStickers.entrySet()) {
			stickerIds.put(entry.getKey().getId(), entry.getValue().getId());
		}
		return stickerIds;
	}

	private static void resetClothingDyeColours() {
		dyePreviews = new ArrayList<>();
		dyePreviews.addAll(clothing.getColours());

		dyePreviewPattern = clothing.getPattern();

		dyePreviewPatterns = new ArrayList<>();
		dyePreviewPatterns.addAll(clothing.getPatternColours());

		dyePreviewStickers = new HashMap<>(clothing.getStickersAsObjects());
	}

	private static void resetWeaponDyeColours() {
		dyePreviews = new ArrayList<>();
		dyePreviews.addAll(weapon.getColours());

		damageTypePreview = weapon.getDamageType();
	}

	private static String inventoryView() {
		inventorySB = new StringBuilder();

		inventorySB.append(RenderingEngine.ENGINE.getInventoryPanel(inventoryNPC, buyback));

		return inventorySB.toString();
	}

	private static void equipAll(GameCharacter character) {
		List<AbstractClothing> zlayerClothing = new ArrayList<>(character.getAllClothingInInventory().keySet());
		zlayerClothing.removeIf((c) -> c.isEnchantmentKnown() && c.isSealed());
		zlayerClothing.sort(new ClothingZLayerComparator().reversed());
		Set<InventorySlot> slotsTaken = new HashSet<>();

		for(AbstractClothing c : character.getClothingCurrentlyEquipped()) {
			slotsTaken.add(c.getSlotEquippedTo());
		}

		for(AbstractClothing c : zlayerClothing) {
			if(!slotsTaken.contains(c.getClothingType().getEquipSlots().get(0))) {
				Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"+character.equipClothingFromInventory(c, true, character, character)+"</p>");
				slotsTaken.add(c.getClothingType().getEquipSlots().get(0));
			}
		}
	}

	private static String unequipAll(GameCharacter character) {
		StringBuilder sb = new StringBuilder();

//		for(int i=0; i<character.getArmRows(); i++) {
//			sb.append(character.unequipMainWeapon(i, false, character.isPlayer()));
//			sb.append(character.unequipOffhandWeapon(i, false, character.isPlayer()));
//		}

		List<AbstractClothing> zlayerClothing = new ArrayList<>(character.getClothingCurrentlyEquipped());
		zlayerClothing.sort(new ClothingZLayerComparator());

		for(AbstractClothing c : zlayerClothing) {
			if((!Main.game.isInSex() || (!c.getSlotEquippedTo().isJewellery() && !c.isCondom())) && !c.isMilkingEquipment()) {
				if (c.isDiscardedOnUnequip(null)) {
					character.unequipClothingIntoVoid(c, true, Main.game.getPlayer());
				} else {
					if(Main.game.isInNewWorld()) {
						character.unequipClothingIntoInventory(c, true, Main.game.getPlayer());
					} else {
						character.unequipClothingOntoFloor(c, true, Main.game.getPlayer());
					}
				}
				sb.append("<p style='text-align:center;'>"+character.getUnequipDescription()+"</p>");
			}
		}

		return sb.toString();
	}

	private static String getEnchantmentNotDiscoveredText(String item) {
		StringBuilder sb = new StringBuilder();
		sb.append("You have not discovered how to enchant ");
		sb.append(item);
		sb.append(" yet...");
		sb.append("<br/>");
		if(Main.game.getPlayer().hasQuest(QuestLine.SIDE_ENCHANTMENT_DISCOVERY)) {
			sb.append("[style.italicsArcane(You should go and talk to Lilaya about essences...)]");
		} else {
			sb.append("[style.italicsArcane(You need to absorb an essence after combat or sex, or via purchasing one, to discover more about enchanting...)]");
		}
		return sb.toString();
	}

	private static String getClothingBlockingRemovalText(String equipVerb) {
		StringBuilder sb = new StringBuilder();
		sb.append("You can't ");
		sb.append(equipVerb);
		sb.append(" the ");
		sb.append(clothing.getName());
		sb.append(", as ");
		sb.append(UtilText.parse(owner, "[npc.namePos] "));
		sb.append(owner.getBlockingClothing().getName());
		sb.append((owner.getBlockingClothing().getClothingType().isPlural()?" are":" is"));
		sb.append(" blocking you from doing so!");
		return sb.toString();
	}

	/**
	 * The main DialogueNode. From here, the player can gain access to all parts
	 * of their inventory.
	 */
	public static final DialogueNode INVENTORY_MENU = new DialogueNode("Inventory", "Return to inventory menu.", true) {
		@Override
		public String getLabel() {
			if(!Main.game.isInNewWorld()) {
				return "Evening's Attire";
			}

			if (Main.game.getDialogueFlags().values.contains(DialogueFlagValue.quickTrade) && !Main.game.isInSex() && !Main.game.isInCombat()) {
				return "Inventory (Quick-Manage is <b style='color:" + PresetColour.GENERIC_GOOD.toWebHexString() + ";'>ON</b>)";
			} else {
				return "Inventory";
			}
		}

		@Override
		public String getHeaderContent() {
			return inventoryView();
		}

		@Override
		public String getContent() {
			UtilText.nodeContentSB.setLength(0);

			if(inventoryNPC!=null && interactionType==InventoryInteraction.TRADING) {
				if(!Main.game.getDialogueFlags().hasFlag(DialogueFlagValue.removeTraderDescription)) {
					UtilText.nodeContentSB.append(inventoryNPC.getTraderDescription());
				}

			} else if(interactionType==InventoryInteraction.CHARACTER_CREATION) {
				return CharacterCreation.getCheckingClothingDescription();
			}

			return UtilText.nodeContentSB.toString();
		}

		public String getResponseTabTitle(int index) {
			return getGeneralResponseTabTitle(index);
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if (index == 0) {
				return getCloseInventoryResponse();
			}

			if(responseTab==1) {
				if(item!=null) {
					return ITEM_INVENTORY.getResponse(responseTab, index);

				} else if(clothing!=null) {
					if(Main.game.getPlayer().getClothingCurrentlyEquipped().contains(clothing) || (inventoryNPC!=null && inventoryNPC.getClothingCurrentlyEquipped().contains(clothing))) {
						return CLOTHING_EQUIPPED.getResponse(responseTab, index);
					} else {
						return CLOTHING_INVENTORY.getResponse(responseTab, index);
					}

				} else if(weapon!=null) {
					if(Main.game.getPlayer().hasWeaponEquipped(weapon)
							|| (inventoryNPC!=null && inventoryNPC.hasWeaponEquipped(weapon))) {
						return WEAPON_EQUIPPED.getResponse(responseTab, index);
					} else {
						return WEAPON_INVENTORY.getResponse(responseTab, index);
					}

				} else {
					return null;
				}
			}

			StringBuilder responseSB = new StringBuilder();
			switch(interactionType) {
				case COMBAT:
					if(index == 1) {
						return new Response("Take all", "You can't do this during combat!", null);

					} else if (index == 2) {
						return new Response("Displace all", "You cannot do this while fighting someone!", null);

					} else if (index == 3) {
						return new Response("Replace all", "You cannot do this while fighting someone!", null);

					} else if (index == 4) {
						return new Response("Unequip all", "You cannot do this while fighting someone!", null);

					} else if (index == 5) {
						return new Response("Equip all", "You cannot do this while fighting someone!", null);

					} else if(index == 6) {
						if(Main.game.getPlayer().getLocationPlace().isItemsDisappear()) {
							return new Response("Store all", "You can't do this during combat!", null);
						}
						return new Response("Drop all", "You can't do this during combat!", null);

					} else if(index==11) {
						if(Main.game.getPlayer().getUnlockKeyMap().isEmpty()) {
							return new Response("Keys", "You do not currently own any keys which are used for unlocking certain items of clothing.", null);

						} else if(Main.game.getCurrentDialogueNode()==INVENTORY_MENU_KEYS) {
							return new Response("Keys", "You are already viewing a list of all the keys that you own which are used for unlocking certain items of clothing.", null);

						} else {
							return new Response("Keys", "View a list of all the keys that you currently own which are used for unlocking certain items of clothing.", INVENTORY_MENU_KEYS);
						}

					} else {
						return null;
					}

				case FULL_MANAGEMENT:
					if (index == 1) {
						if(inventoryNPC == null ) {
							if((Main.game.getPlayerCell().getInventory().getInventorySlotsTaken()==0 && !Main.game.getPlayerCell().getInventory().isAnyQuestItemPresent())
									|| Main.game.isInCombat()
									|| Main.game.isInSex()) {
								return new Response("Take all", "Pick up everything on the ground.", null);

							} else {
								return new Response("Take all", "Pick up everything on the ground.", INVENTORY_MENU){
									@Override
									public void effects(){
										for(Entry<AbstractItem, Integer> entry : new HashMap<>(Main.game.getPlayerCell().getInventory().getAllItemsInInventory()).entrySet()) {
											Main.game.getPlayer().addItem(entry.getKey(), entry.getValue(), true, true);
										}
										for(Entry<AbstractWeapon, Integer> entry : new HashMap<>(Main.game.getPlayerCell().getInventory().getAllWeaponsInInventory()).entrySet()) {
											Main.game.getPlayer().addWeapon(entry.getKey(), entry.getValue(), true, true);
										}
										for(Entry<AbstractClothing, Integer> entry : new HashMap<>(Main.game.getPlayerCell().getInventory().getAllClothingInInventory()).entrySet()) {
											Main.game.getPlayer().addClothing(entry.getKey(), entry.getValue(), true, true);
										}
									}
								};
							}

						} else {
							if(inventoryNPC.getInventorySlotsTaken()==0 || Main.game.isInCombat() || Main.game.isInSex()) {
								return new Response("Take all", UtilText.parse(inventoryNPC, "Take everything from [npc.namePos] inventory."), null);

							} else {
								return new Response("Take all", UtilText.parse(inventoryNPC, "Take everything from [npc.namePos] inventory."), INVENTORY_MENU){
									@Override
									public void effects(){
										for(Entry<AbstractItem, Integer> entry : new HashMap<>(inventoryNPC.getAllItemsInInventory()).entrySet()) {
											if(!Main.game.getPlayer().isInventoryFull()
													|| Main.game.getPlayer().hasItem(entry.getKey())
													|| entry.getKey().getRarity()==Rarity.QUEST) {
												inventoryNPC.removeItem(entry.getKey(), entry.getValue());
												Main.game.getPlayer().addItem(entry.getKey(), entry.getValue(), true, true);
											}
										}
										for(Entry<AbstractClothing, Integer> entry : new HashMap<>(inventoryNPC.getAllClothingInInventory()).entrySet()) {
											if(!Main.game.getPlayer().isInventoryFull()
													|| Main.game.getPlayer().hasClothing(entry.getKey())
													|| entry.getKey().getRarity()==Rarity.QUEST) {
												inventoryNPC.removeClothing(entry.getKey(), entry.getValue());
												Main.game.getPlayer().addClothing(entry.getKey(), entry.getValue(), true, true);
											}
										}
										for(Entry<AbstractWeapon, Integer> entry : new HashMap<>(inventoryNPC.getAllWeaponsInInventory()).entrySet()) {
											if(!Main.game.getPlayer().isInventoryFull()
													|| Main.game.getPlayer().hasWeapon(entry.getKey())
													|| entry.getKey().getRarity()==Rarity.QUEST) {
												inventoryNPC.removeWeapon(entry.getKey(), entry.getValue());
												Main.game.getPlayer().addWeapon(entry.getKey(), entry.getValue(), true, true);
											}
										}
									}
								};
							}
						}

					} else if (index == 2) {
						if(Main.game.getPlayer().getClothingCurrentlyEquipped().isEmpty()) {
							return new Response("Displace all", "You aren't wearing any clothing, so there's nothing to displace!", null);

						} else {
							return new Response("Displace all", "Displace as much of your clothing as possible.", INVENTORY_MENU){
								@Override
								public void effects(){
									for(AbstractClothing c : Main.game.getPlayer().getClothingCurrentlyEquipped()) {
										for(BlockedParts bp : c.getBlockedPartsMap(Main.game.getPlayer(), c.getSlotEquippedTo())) {
											if(bp.displacementType != DisplacementType.REMOVE_OR_EQUIP) {
												Main.game.getPlayer().isAbleToBeDisplaced(c, bp.displacementType, true, true, Main.game.getPlayer());
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"+Main.game.getPlayer().getDisplaceDescription()+"</p>");
											}
										}
									}
								}
							};
						}

					} else if (index == 3) {
						if(Main.game.getPlayer().getClothingCurrentlyEquipped().isEmpty()) {
							return new Response("Replace all", "You aren't wearing any clothing, so there's nothing to replace!", null);

						} else {
							return new Response("Replace all", "Replace as much of your clothing as possible.", INVENTORY_MENU){
								@Override
								public void effects(){

									List<AbstractClothing> zlayerClothing = new ArrayList<>(Main.game.getPlayer().getClothingCurrentlyEquipped());
									zlayerClothing.sort(new ClothingZLayerComparator().reversed());

									for(AbstractClothing c : zlayerClothing) {
										for(BlockedParts bp : c.getBlockedPartsMap(Main.game.getPlayer(), c.getSlotEquippedTo())) {
											if(bp.displacementType != DisplacementType.REMOVE_OR_EQUIP) {
												Main.game.getPlayer().isAbleToBeReplaced(c, bp.displacementType, true, true, Main.game.getPlayer());
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"+Main.game.getPlayer().getReplaceDescription()+"</p>");
											}
										}
									}
								}
							};
						}

					} else if (index == 4) {
						if(Main.game.getPlayer().getClothingCurrentlyEquipped().isEmpty()) {
							return new Response("Unequip all", "You aren't wearing any clothing, so there's nothing to remove!", null);

						} else {
							return new Response("Unequip all", "Remove as much of your clothing as possible.", INVENTORY_MENU){
								@Override
								public void effects(){
									Main.game.getTextEndStringBuilder().append(unequipAll(Main.game.getPlayer()));
								}
							};
						}

					} else if (index == 5) {
						if(Main.game.getPlayer().getAllClothingInInventory().isEmpty()) {
							return new Response("Equip all", "You don't have any clothing, so there's nothing to equip!", null);

						} else {
							return new Response("Equip all", "Equip as much of the clothing in your inventory as possible.", INVENTORY_MENU){
								@Override
								public void effects(){
									equipAll(Main.game.getPlayer());
								}
							};
						}

					} else if(index == 6) {
						if(Main.game.getPlayer().getInventorySlotsTaken()==0) {
							return new Response(
									!Main.game.getPlayer().getLocationPlace().isItemsDisappear()
										?"Store all"
										:"Drop all",
									!Main.game.getPlayer().getLocationPlace().isItemsDisappear()
										?"You have nothing in your inventory to store..."
										:"You have nothing in your inventory to drop...",
											null);
						}
						return new Response(
								!Main.game.getPlayer().getLocationPlace().isItemsDisappear()
									?"Store all"
									:"Drop all",
								!Main.game.getPlayer().getLocationPlace().isItemsDisappear()
									?"Store everything from your inventory in this location."
									:"Drop everything from your inventory onto the ground.",
										INVENTORY_MENU) {
							@Override
							public void effects() {
								for(Entry<AbstractItem, Integer> i : new HashSet<>(Main.game.getPlayer().getAllItemsInInventory().entrySet())) {
									if(i.getKey().getItemType().isAbleToBeDropped()) {
										dropItems(Main.game.getPlayer(), i.getKey(), i.getValue());
									}
								}
								for(Entry<AbstractWeapon, Integer> w : new HashSet<>(Main.game.getPlayer().getAllWeaponsInInventory().entrySet())) {
									if(w.getKey().getWeaponType().isAbleToBeDropped()) {
										dropWeapons(Main.game.getPlayer(), w.getKey(), w.getValue());
									}
								}
								for(Entry<AbstractClothing, Integer> c : new HashSet<>(Main.game.getPlayer().getAllClothingInInventory().entrySet())) {
									if(c.getKey().getClothingType().isAbleToBeDropped()) {
										dropClothing(Main.game.getPlayer(), c.getKey(), c.getValue());
									}
								}
							}
						};

					} else if (index == 7 && inventoryNPC != null) {
						if(inventoryNPC.getClothingCurrentlyEquipped().isEmpty()) {
							return new Response(UtilText.parse(inventoryNPC,"Displace all ([npc.HerHim])"),
									UtilText.parse(inventoryNPC, "[npc.Name] isn't wearing any clothing, so there's nothing to displace!"),
									null);

						} else {
							return new Response(UtilText.parse(inventoryNPC,"Displace all ([npc.HerHim])"),
									UtilText.parse(inventoryNPC, "Displace as much of [npc.namePos] clothing as possible."),
									INVENTORY_MENU){
								@Override
								public void effects(){
									for(AbstractClothing c : inventoryNPC.getClothingCurrentlyEquipped()) {
										for(BlockedParts bp : c.getBlockedPartsMap(inventoryNPC, c.getSlotEquippedTo())) {
											if(bp.displacementType != DisplacementType.REMOVE_OR_EQUIP) {
												inventoryNPC.isAbleToBeDisplaced(c, bp.displacementType, true, true, Main.game.getPlayer());
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"+inventoryNPC.getDisplaceDescription()+"</p>");
											}
										}
									}
								}
							};
						}

					} else if (index == 8 && inventoryNPC != null) {
						if(inventoryNPC.getClothingCurrentlyEquipped().isEmpty()) {
							return new Response(UtilText.parse(inventoryNPC,"Replace all ([npc.HerHim])"),
									UtilText.parse(inventoryNPC, "[npc.Name] isn't wearing any clothing, so there's nothing to replace!"),
									null);

						} else {
							return new Response(UtilText.parse(inventoryNPC,"Replace all ([npc.HerHim])"),
									UtilText.parse(inventoryNPC, "Replace as much of [npc.namePos] clothing as possible."),
									INVENTORY_MENU){
								@Override
								public void effects(){

									List<AbstractClothing> zlayerClothing = new ArrayList<>(inventoryNPC.getClothingCurrentlyEquipped());
									zlayerClothing.sort(new ClothingZLayerComparator().reversed());

									for(AbstractClothing c : zlayerClothing) {
										for(BlockedParts bp : c.getBlockedPartsMap(inventoryNPC, c.getSlotEquippedTo())) {
											if(bp.displacementType != DisplacementType.REMOVE_OR_EQUIP) {
												inventoryNPC.isAbleToBeReplaced(c, bp.displacementType, true, true, Main.game.getPlayer());
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"+inventoryNPC.getReplaceDescription()+"</p>");
											}
										}
									}
								}
							};
						}

					} else if (index == 9 && inventoryNPC != null) {
						if(inventoryNPC.getClothingCurrentlyEquipped().isEmpty()) {
							return new Response(UtilText.parse(inventoryNPC, "Unequip all ([npc.HerHim])"),
									UtilText.parse(inventoryNPC, "[npc.Name] isn't wearing any clothing, so there's nothing to remove!"),
									null);

						} else {
							return new Response(UtilText.parse(inventoryNPC, "Unequip all ([npc.HerHim])"),
									UtilText.parse(inventoryNPC, "Remove as much of [npc.namePos] clothing as possible."),
									INVENTORY_MENU){
								@Override
								public void effects(){
									Main.game.getTextEndStringBuilder().append(unequipAll(inventoryNPC));
								}
							};
						}

					} else if (index == 10 && !Main.game.isInSex() && !Main.game.isInCombat()) {
						return getQuickTradeResponse();

					} else if(index==11) {
						if(Main.game.getPlayer().getUnlockKeyMap().isEmpty()) {
							return new Response("Keys", "You do not currently own any keys which are used for unlocking certain items of clothing.", null);

						} else if(Main.game.getCurrentDialogueNode()==INVENTORY_MENU_KEYS) {
							return new Response("Keys", "You are already viewing a list of all the keys that you own which are used for unlocking certain items of clothing.", null);

						} else {
							return new Response("Keys", "View a list of all the keys that you currently own which are used for unlocking certain items of clothing.", INVENTORY_MENU_KEYS);
						}

					} else {
						return null;
					}
				case CHARACTER_CREATION:
					if (index == 1) {
						if(Main.game.getPlayer().isCoverableAreaVisible(CoverableArea.NIPPLES)
								|| Main.game.getPlayer().isCoverableAreaVisible(CoverableArea.ANUS)
								|| Main.game.getPlayer().isCoverableAreaVisible(CoverableArea.PENIS)
								|| Main.game.getPlayer().isCoverableAreaVisible(CoverableArea.VAGINA)
								|| (Main.game.getPlayer().getClothingInSlot(InventorySlot.FOOT)==null && Main.game.getPlayer().getLegType().equals(LegType.HUMAN))) {
							return new Response("To the stage", "You need to be wearing clothing that covers your body, as well as a pair of shoes.", null);

						} else {
							return new Response("To the stage", "You're ready to approach the stage now.", CharacterCreation.CHOOSE_BACKGROUND) {
								@Override
								public int getSecondsPassed() {
									return CharacterCreation.TIME_TO_BACKGROUND;
								}
								@Override
								public void effects() {
									CharacterCreation.moveNPCIntoPlayerTile();
								}
							};
						}

					} else if(index == 2){
						if(Main.game.getPlayer().getClothingCurrentlyEquipped().isEmpty()){
							return new Response("Unequip all", "You're currently naked, there's nothing to be unequipped.", null);
						}
						else{
							return new Response("Unequip all", "Remove as much of your clothing as possible.", INVENTORY_MENU){
								@Override
								public void effects(){
									Main.game.getTextEndStringBuilder().append(unequipAll(Main.game.getPlayer()));
								}
							};
						}

					} else {
						return null;
					}

				case TRADING:
					if (index == 1) {
						if(inventoryNPC != null ||Main.game.getPlayerCell().getInventory().getInventorySlotsTaken()==0 || Main.game.isInCombat() || Main.game.isInSex()) {
							return new Response("Take all", "Pick up everything on the ground.", null);

						} else {
							return new Response("Take all", "Pick up everything on the ground.", INVENTORY_MENU){
								@Override
								public void effects(){
									for(Entry<AbstractItem, Integer> entry : new HashMap<>(Main.game.getPlayerCell().getInventory().getAllItemsInInventory()).entrySet()) {
										Main.game.getPlayer().addItem(entry.getKey(), entry.getValue(), true, true);
									}
									for(Entry<AbstractWeapon, Integer> entry : new HashMap<>(Main.game.getPlayerCell().getInventory().getAllWeaponsInInventory()).entrySet()) {
										Main.game.getPlayer().addWeapon(entry.getKey(), entry.getValue(), true, true);
									}
									for(Entry<AbstractClothing, Integer> entry : new HashMap<>(Main.game.getPlayerCell().getInventory().getAllClothingInInventory()).entrySet()) {
										Main.game.getPlayer().addClothing(entry.getKey(), entry.getValue(), true, true);
									}
								}
							};
						}

					} else if (index == 2) {
						if(Main.game.getPlayer().getClothingCurrentlyEquipped().isEmpty()) {
							return new Response("Displace all", "You aren't wearing any clothing, so there's nothing to displace!", null);

						} else {
							return new Response("Displace all", "Displace as much of your clothing as possible.", INVENTORY_MENU){
								@Override
								public void effects(){
									for(AbstractClothing c : Main.game.getPlayer().getClothingCurrentlyEquipped()) {
										for(BlockedParts bp : c.getBlockedPartsMap(Main.game.getPlayer(), c.getSlotEquippedTo())) {
											if(bp.displacementType != DisplacementType.REMOVE_OR_EQUIP) {
												Main.game.getPlayer().isAbleToBeDisplaced(c, bp.displacementType, true, true, Main.game.getPlayer());
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"+Main.game.getPlayer().getDisplaceDescription()+"</p>");
											}
										}
									}
								}
							};
						}

					} else if (index == 3) {
						if(Main.game.getPlayer().getClothingCurrentlyEquipped().isEmpty()) {
							return new Response("Replace all", "You aren't wearing any clothing, so there's nothing to replace!", null);

						} else {
							return new Response("Replace all", "Replace as much of your clothing as possible.", INVENTORY_MENU){
								@Override
								public void effects(){

									List<AbstractClothing> zlayerClothing = new ArrayList<>(Main.game.getPlayer().getClothingCurrentlyEquipped());
									zlayerClothing.sort(new ClothingZLayerComparator().reversed());

									for(AbstractClothing c : zlayerClothing) {
										for(BlockedParts bp : c.getBlockedPartsMap(Main.game.getPlayer(), c.getSlotEquippedTo())) {
											if(bp.displacementType != DisplacementType.REMOVE_OR_EQUIP) {
												Main.game.getPlayer().isAbleToBeReplaced(c, bp.displacementType, true, true, Main.game.getPlayer());
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"+Main.game.getPlayer().getReplaceDescription()+"</p>");
											}
										}
									}
								}
							};
						}

					} else if (index == 4) {
						if(Main.game.getPlayer().getClothingCurrentlyEquipped().isEmpty()) {
							return new Response("Unequip all", "You aren't wearing any clothing, so there's nothing to remove!", null);

						} else {
							return new Response("Unequip all", "Remove as much of your clothing as possible.", INVENTORY_MENU){
								@Override
								public void effects(){
									Main.game.getTextEndStringBuilder().append(unequipAll(Main.game.getPlayer()));
								}
							};
						}

					} else if (index == 5) {
						if(Main.game.getPlayer().getAllClothingInInventory().isEmpty()) {
							return new Response("Equip all", "You don't have any clothing, so there's nothing to equip!", null);

						} else {
							return new Response("Equip all", "Equip as much of the clothing in your inventory as possible.", INVENTORY_MENU){
								@Override
								public void effects(){
									equipAll(Main.game.getPlayer());
								}
							};
						}

					} else if(index == 6) {
						if(Main.game.getPlayer().getLocationPlace().isItemsDisappear()) {
							return new Response("Store all", "You can't do this while trading with someone...", null);
						}
						return new Response("Drop all", "You can't do this while trading with someone...", null);

					} else if (index == 9 && inventoryNPC!=null) {
						return getBuybackResponse();

					} else if (index == 10 && !Main.game.isInSex() && !Main.game.isInCombat()) {
						return getQuickTradeResponse();

					} else if(index==11) {
						if(Main.game.getPlayer().getUnlockKeyMap().isEmpty()) {
							return new Response("Keys", "You do not currently own any keys which are used for unlocking certain items of clothing.", null);

						} else if(Main.game.getCurrentDialogueNode()==INVENTORY_MENU_KEYS) {
							return new Response("Keys", "You are already viewing a list of all the keys that you own which are used for unlocking certain items of clothing.", null);

						} else {
							return new Response("Keys", "View a list of all the keys that you currently own which are used for unlocking certain items of clothing.", INVENTORY_MENU_KEYS);
						}

					} else {
						return null;
					}
				case SEX:
					if(index == 1) {
						return new Response("Take all", "Pick up everything on the ground.", null);

					} else if (index == 2) {
						if(Main.game.getPlayer().getClothingCurrentlyEquipped().isEmpty()) {
							return new Response("Displace all", "You aren't wearing any clothing, so there's nothing to displace!", null);

						} else {
							return new Response("Displace all", "Displace as much of your clothing as possible.", Main.sex.SEX_DIALOGUE){
								@Override
								public void effects(){
									responseSB.setLength(0);

									for(AbstractClothing c : Main.game.getPlayer().getClothingCurrentlyEquipped()) {
										for(BlockedParts bp : c.getBlockedPartsMap(Main.game.getPlayer(), c.getSlotEquippedTo())) {
											if(bp.displacementType != DisplacementType.REMOVE_OR_EQUIP) {
												Main.game.getPlayer().isAbleToBeDisplaced(c, bp.displacementType, true, true, Main.game.getPlayer());
												responseSB.append("<p style='text-align:center;'>"+Main.game.getPlayer().getDisplaceDescription()+"</p>");
											}
										}
									}

									Main.sex.setUnequipClothingText(null, responseSB.toString());
									Main.mainController.openInventory();
									Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
									Main.sex.setSexStarted(true);
								}
							};
						}

					} else if (index == 3) {
						return new Response("Replace all", "You can't replace clothing in sex!", null);

					} else if (index == 4) {
						if(Main.game.getPlayer().getClothingCurrentlyEquipped().isEmpty()) {
							return new Response("Unequip all", "You aren't wearing any clothing, so there's nothing to remove!", null);

						} else {
							return new Response("Unequip all", "Remove as much of your clothing as possible.", Main.sex.SEX_DIALOGUE){
								@Override
								public void effects(){
									responseSB.setLength(0);

									responseSB.append(unequipAll(Main.game.getPlayer()));

									Main.sex.setUnequipClothingText(null, responseSB.toString());
									Main.mainController.openInventory();
									Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
									Main.sex.setSexStarted(true);
								}
							};
						}

					} else if (index == 5) {
						return new Response("Equip all", "You can't equip clothing in sex!", null);

					} else if(index == 6) {
						if(Main.game.getPlayer().getLocationPlace().isItemsDisappear()) {
							return new Response("Store all", "You can't do this during sex...", null);
						}
						return new Response("Drop all", "You can't do this during sex...", null);

					} else if (index == 7 && inventoryNPC != null) {
						if(!Main.sex.getSexManager().isAbleToRemoveOthersClothing(Main.game.getPlayer(), null)) {
							return new Response(UtilText.parse(inventoryNPC, "Displace all ([npc.HerHim])"), UtilText.parse(inventoryNPC, "You can't displace [npc.namePos] clothing in this sex scene!"), null);

						} else if(inventoryNPC.getClothingCurrentlyEquipped().isEmpty()) {
							return new Response(UtilText.parse(inventoryNPC, "Displace all ([npc.HerHim])"), UtilText.parse(inventoryNPC, "[npc.Name] isn't wearing any clothing, so there's nothing to displace!"), null);

						} else {
							return new Response(UtilText.parse(inventoryNPC, "Displace all ([npc.HerHim])"), UtilText.parse(inventoryNPC, "Displace as much of [npc.namePos] clothing as possible."), Main.sex.SEX_DIALOGUE){
								@Override
								public void effects(){
									responseSB.setLength(0);

									for(AbstractClothing c : inventoryNPC.getClothingCurrentlyEquipped()) {
										for(BlockedParts bp : c.getBlockedPartsMap(inventoryNPC, c.getSlotEquippedTo())) {
											if(bp.displacementType != DisplacementType.REMOVE_OR_EQUIP) {
												inventoryNPC.isAbleToBeDisplaced(c, bp.displacementType, true, true, Main.game.getPlayer());
												responseSB.append("<p style='text-align:center;'>"+inventoryNPC.getDisplaceDescription()+"</p>");
											}
										}
									}

									Main.sex.setUnequipClothingText(null, responseSB.toString());
									Main.mainController.openInventory();
									Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
									Main.sex.setSexStarted(true);
								}
							};
						}

					} else if (index == 8 && inventoryNPC != null) {
						return new Response(UtilText.parse(inventoryNPC, "Replace all ([npc.HerHim])"), "You can't replace clothing in sex!", null);

					} else if (index == 9 && inventoryNPC != null) {
						if(!Main.sex.getSexManager().isAbleToRemoveOthersClothing(Main.game.getPlayer(), null)) {
							return new Response(UtilText.parse(inventoryNPC, "Unequip all ([npc.HerHim])"), UtilText.parse(inventoryNPC, "You can't unequip [npc.namePos] clothing in this sex scene!"), null);

						} else if(inventoryNPC.getClothingCurrentlyEquipped().isEmpty()) {
							return new Response(UtilText.parse(inventoryNPC, "Unequip all ([npc.HerHim])"), UtilText.parse(inventoryNPC, "[npc.Name] isn't wearing any clothing, so there's nothing to remove!"), null);

						} else {
							return new Response(UtilText.parse(inventoryNPC, "Unequip all ([npc.HerHim])"), UtilText.parse(inventoryNPC, "Remove as much of [npc.namePos] clothing as possible."), Main.sex.SEX_DIALOGUE){
								@Override
								public void effects(){
									responseSB.setLength(0);

									responseSB.append(unequipAll(inventoryNPC));

									Main.sex.setUnequipClothingText(null, responseSB.toString());
									Main.mainController.openInventory();
									Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
									Main.sex.setSexStarted(true);
								}
							};
						}

					} else if(index==11) {
						if(Main.game.getPlayer().getUnlockKeyMap().isEmpty()) {
							return new Response("Keys", "You do not currently own any keys which are used for unlocking certain items of clothing.", null);

						} else if(Main.game.getCurrentDialogueNode()==INVENTORY_MENU_KEYS) {
							return new Response("Keys", "You are already viewing a list of all the keys that you own which are used for unlocking certain items of clothing.", null);

						} else {
							return new Response("Keys", "View a list of all the keys that you currently own which are used for unlocking certain items of clothing.", INVENTORY_MENU_KEYS);
						}

					} else {
						return null;
					}
			}

			return null;
		}

		@Override
		public DialogueNodeType getDialogueNodeType() {
			return DialogueNodeType.INVENTORY;
		}
	};

	public static final DialogueNode INVENTORY_MENU_KEYS = new DialogueNode("Inventory", "Return to inventory menu.", true) {
		@Override
		public String getLabel() {
			if(!Main.game.isInNewWorld()) {
				return "Evening's Attire";
			}

			if (Main.game.getDialogueFlags().values.contains(DialogueFlagValue.quickTrade) && !Main.game.isInSex() && !Main.game.isInCombat()) {
				return "Inventory (Quick-Manage is <b style='color:" + PresetColour.GENERIC_GOOD.toWebHexString() + ";'>ON</b>)";
			} else {
				return "Inventory";
			}
		}

		@Override
		public String getHeaderContent() {
			return inventoryView();
		}

		@Override
		public String getContent() {
			UtilText.nodeContentSB.setLength(0);

			UtilText.nodeContentSB.append("<p>");
			UtilText.nodeContentSB.append("[style.boldMinorGood(Keys owned:)]");
			if(Main.game.getPlayer().getUnlockKeyMap().isEmpty()) {
				UtilText.nodeContentSB.append("<br/>[style.italicsDisabled(None...)]");

			} else {
				for(Entry<String, List<InventorySlot>> entry : Main.game.getPlayer().getUnlockKeyMap().entrySet()) {
					try {
						GameCharacter npc = Main.game.getNPCById(entry.getKey());
						List<String> slots = new ArrayList<>();
						for(InventorySlot slot : entry.getValue()) {
							AbstractClothing slotClothing = npc.getClothingInSlot(slot);
							if(slotClothing!=null) {
								slots.add(Util.capitaliseSentence(slotClothing.getName())+" ('"+slot.getName()+"' slot)");
							}
						}
						if(!slots.isEmpty()) {
							UtilText.nodeContentSB.append(UtilText.parse(npc, "<br/><b style='color:"+npc.getFemininity().getColour().toWebHexString()+";'>[npc.Name]</b> ("+slots.size()+"): "));
							int i=0;
							for(String s : slots) {
								UtilText.nodeContentSB.append((i>0?", ":"")+s);
								i++;
							}
						}
					} catch (Exception e) {
					}
				}
			}
			UtilText.nodeContentSB.append("</p>");

			return UtilText.nodeContentSB.toString();
		}

		public String getResponseTabTitle(int index) {
			return getGeneralResponseTabTitle(index);
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			return INVENTORY_MENU.getResponse(responseTab, index);
		}

		@Override
		public DialogueNodeType getDialogueNodeType() {
			return DialogueNodeType.INVENTORY;
		}
	};

	public static final DialogueNode ITEM_INVENTORY = new DialogueNode("Item", "", true) {

		@Override
		public String getLabel() {
			if (Main.game.getDialogueFlags().values.contains(DialogueFlagValue.quickTrade) && !Main.game.isInSex() && !Main.game.isInCombat()) {
				return "Inventory (Quick-Manage is <b style='color:" + PresetColour.GENERIC_GOOD.toWebHexString() + ";'>ON</b>)";
			} else {
				return "Inventory";
			}
		}

		@Override
		public String getHeaderContent() {
			return inventoryView();
		}

		@Override
		public String getContent() {
			return getItemDisplayPanel(item.getSVGString(),
					item.getDisplayName(true),
					item.getDescription()
					+ item.getExtraDescription(owner, owner)
					+ (owner!=null && owner.isPlayer()
							? (inventoryNPC != null && interactionType == InventoryInteraction.TRADING
									? "<p>"
										+(inventoryNPC.willBuy(item) && item.getItemType().isAbleToBeSold()
											?inventoryNPC.getName("The") + " will buy it for " + UtilText.formatAsMoney(item.getPrice(inventoryNPC.getBuyModifier())) + "."
											:inventoryNPC.getName("The") + " doesn't want to buy this.")
										+"</p>"
									: "")
							:(inventoryNPC != null && interactionType == InventoryInteraction.TRADING
								? "<p>"
										+ inventoryNPC.getName("The") + " will sell it for " + UtilText.formatAsMoney(item.getPrice(inventoryNPC.getSellModifier(item))) + "."
									+ "</p>"
								: "")));
		}

		public String getResponseTabTitle(int index) {
			return getGeneralResponseTabTitle(index);
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if (index == 0) {
				return getCloseInventoryResponse();
			}
			if(responseTab==0) {
				return INVENTORY_MENU.getResponse(responseTab, index);
			}
			// ****************************** ITEM BELONGS TO THE PLAYER ******************************
			if(owner != null && owner.isPlayer()) {

				// ****************************** Interacting with the ground ******************************
				if(inventoryNPC == null) {
					boolean areaFull = Main.game.isPlayerTileFull() && !Main.game.getPlayerCell().getInventory().hasItem(item);

					switch(interactionType) {
						case SEX:
							String dropTitle = owner.getLocationPlace().isItemsDisappear()?"Drop ":"Store";
							if(index == 1) {
								return new Response(dropTitle+"(1)", "You can't drop items while masturbating.", null);

							} else if(index == 2) {
								return new Response(dropTitle+"(5)", "You can't drop items while masturbating.", null);

							} else if(index == 3) {
								return new Response(dropTitle+"(All)", "You can't drop items while masturbating.", null);

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant items while masturbating.", null);

							} else if(index == 6) {
								if(!Main.sex.isItemUseAvailable()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", "Items cannot be used during this sex scene!", null);

								} else if (!item.isAbleToBeUsedInSex()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", "You cannot use this during sex!", null);

								} else if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if (!item.isAbleToBeUsed(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)", item.getUnableToBeUsedDescription(Main.game.getPlayer()), null);

								} else {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)",
											item.getItemType().getUseTooltipDescription(owner, owner),
											Main.sex.SEX_DIALOGUE){
										@Override
										public void effects(){
											Main.sex.setUsingItemText(Main.game.getPlayer().useItem(item, Main.game.getPlayer(), false));
											resetPostAction();
											Main.mainController.openInventory();
											Main.sex.endSexTurn(SexActionUtility.PLAYER_USE_ITEM);
											Main.sex.setSexStarted(true);
										}
									};
								}

							} else if(index == 7) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", "You can only use one item at a time during sex!", null);

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else {
								return null;
							}

						default:
							if(index == 1) {
								if(owner.getLocationPlace().isItemsDisappear()) {
									if(!item.getItemType().isAbleToBeDropped()) {
										return new Response("Drop (1)", "You cannot drop the " + item.getName() + "!", null);
									} else if(areaFull) {
										return new Response("Drop (1)", "This area is full, so you can't drop your " + item.getName() + " here!", null);
									} else {
										return new Response("Drop (1)", "Drop your " + item.getName() + ".", INVENTORY_MENU){
											@Override
											public void effects(){
												dropItems(owner, item, 1);
											}
										};
									}
								} else {
									if(!item.getItemType().isAbleToBeDropped()) {
										return new Response("Store (1)", "You cannot drop the " + item.getName() + "!", null);
									} else if(areaFull) {
										return new Response("Store (1)", "This area is full, so you can't store your " + item.getName() + " here!", null);
									} else {
										return new Response("Store (1)", "Store the " + item.getName() + " in this area.", INVENTORY_MENU){
											@Override
											public void effects(){
												dropItems(owner, item, 1);
											}
										};
									}
								}

							} else if(index == 2) {
								if(owner.getLocationPlace().isItemsDisappear()) {
									if(owner.getItemCount(item) < 5) {
										return new Response("Drop (5)", "You don't have five " + item.getNamePlural() + " to give!", null);

									} else if(!item.getItemType().isAbleToBeDropped()) {
										return new Response("Drop (5)", "You cannot drop the " + item.getName() + "!", null);

									} else if(areaFull) {
										return new Response("Drop (5)", "This area is full, so you can't drop your " + item.getNamePlural() + " here!", null);

									} else {
										return new Response("Drop (5)", "Drop five of your " + item.getNamePlural() + ".", INVENTORY_MENU){
											@Override
											public void effects(){
												dropItems(owner, item, 5);
											}
										};
									}
								} else {
									if(owner.getItemCount(item) < 5) {
										return new Response("Store (5)", "You don't have five " + item.getNamePlural() + " to give!", null);

									} else if(!item.getItemType().isAbleToBeDropped()) {
										return new Response("Store (5)", "You cannot drop the " + item.getName() + "!", null);

									} else if(areaFull) {
										return new Response("Store (5)", "This area is full, so you can't store your " + item.getNamePlural() + " here!", null);

									} else {
										return new Response("Store (5)", "Store five of your " + item.getNamePlural() + " in this area.", INVENTORY_MENU){
											@Override
											public void effects(){
												dropItems(owner, item, 5);
											}
										};
									}
								}

							} else if(index == 3) {
								if(owner.getLocationPlace().isItemsDisappear()) {
									if(!item.getItemType().isAbleToBeDropped()) {
										return new Response("Drop (All)", "You cannot drop the " + item.getName() + "!", null);
									} else if(areaFull) {
										return new Response("Drop (All)", "This area is full, so you can't drop your " + item.getNamePlural() + " here!", null);
									} else {
										return new Response("Drop (All)", "Drop all of your " + item.getNamePlural() + ".", INVENTORY_MENU){
											@Override
											public void effects(){
												dropItems(owner, item, owner.getItemCount(item));
											}
										};
									}
								} else {
									if(!item.getItemType().isAbleToBeDropped()) {
										return new Response("Store (All)", "You cannot drop the " + item.getName() + "!", null);
									} else if(areaFull) {
										return new Response("Store (All)", "This area is full, so you can't store your " + item.getNamePlural() + " here!", null);
									} else {
										return new Response("Store (All)", "Store all of your " + item.getNamePlural() + " in this area.", INVENTORY_MENU){
											@Override
											public void effects(){
												dropItems(owner, item, owner.getItemCount(item));
											}
										};
									}
								}

							} else if(index == 5) {
								if(item.getEnchantmentItemType(null)==null || item.getItemTags().contains(ItemTag.UNENCHANTABLE)) {
									return new Response("Enchant", "This item cannot be enchanted!", null);

								} else if(Main.game.isDebugMode()
										|| (Main.game.getPlayer().hasQuest(QuestLine.SIDE_ENCHANTMENT_DISCOVERY) && Main.game.getPlayer().isQuestCompleted(QuestLine.SIDE_ENCHANTMENT_DISCOVERY))) {
									return new Response("Enchant", "Enchant this item.", EnchantmentDialogue.ENCHANTMENT_MENU) {
										@Override
										public DialogueNode getNextDialogue() {
											return EnchantmentDialogue.getEnchantmentMenu(item);
										}
									};

								} else {
									return new Response("Enchant", getEnchantmentNotDiscoveredText("items"), null);
								}

							} else if(index == 6) {
								if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if (!item.isAbleToBeUsed(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)", item.getUnableToBeUsedDescription(Main.game.getPlayer()), null);
								} else {
									if(item.isBreakOutOfInventory()) {
										return new ResponseEffectsOnly(
												Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)",
												item.getItemType().getUseTooltipDescription(owner, owner)){
											@Override
											public void effects(){
												Main.game.getPlayer().useItem(item, Main.game.getPlayer(), false);
												resetPostAction();
											}
										};
									}
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)",
											item.getItemType().getUseTooltipDescription(owner, owner),
											INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + Main.game.getPlayer().useItem(item, Main.game.getPlayer(), false) + "</p>");
											resetPostAction();
										}
									};
								}

							} else if(index == 7) {
								if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if(!item.isAbleToBeUsed(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", item.getUnableToBeUsedDescription(Main.game.getPlayer()), null);

								} else {
									if(item.isBreakOutOfInventory()) {
										return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", "As this item has special effects, you can only use one at a time!", null);
									}
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)",
											item.getItemType().getUseTooltipDescription(owner, owner)
												+"<br/>[style.italicsMinorGood(Repeat this for all of the " + item.getNamePlural() + " which are in your inventory.)]",
											INVENTORY_MENU){
										@Override
										public void effects(){
											int itemCount = Main.game.getPlayer().getItemCount(item);
											for(int i=0;i<itemCount;i++) {
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + Main.game.getPlayer().useItem(item, Main.game.getPlayer(), false) + "</p>");
											}
											resetPostAction();
										}
									};
								}

							} else if (index == 10) {

								//MARKER TEMPORARY
								if(item instanceof FluidContainerInterface){
									return new Response("Manage Fluids", "See what you can do with its contents.", MANAGE_FLUIDS){
										@Override
										public void effects(){
											initFluidManager();
										}
									};
								}
								return getQuickTradeResponse();

							} else {
								return null;
							}
					}

				// ****************************** Interacting with an NPC ******************************
				} else {
					switch(interactionType) {
						case COMBAT:
							if(index == 1) {
								return new Response("Give (1)", "You can't give someone items while fighting them!", null);

							} else if(index == 2) {
								return new Response("Give (5)", "You can't give someone items while fighting them!", null);

							} else if(index == 3) {
								return new Response("Give (All)", "You can't give someone items while fighting them!", null);

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant items while fighting someone!", null);

							} else if(index == 6) {
								if(Main.game.getPlayer().isStunned()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", "You cannot use any items while you're stunned!", null);

								} else if(Main.combat.isCombatantDefeated(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", "You cannot use any items while you're defeated!", null);

								} else if(Main.game.getPlayer().getRemainingAP()<CombatMove.ITEM_USAGE.getAPcost(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", "You need at least "+CombatMove.ITEM_USAGE.getAPcost(Main.game.getPlayer())+" AP to use this actions!", null);

								} else if (!item.isAbleToBeUsedInCombatAllies()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", "You cannot use this during combat!", null);

								} else if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if (!item.isAbleToBeUsed(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)", item.getUnableToBeUsedDescription(Main.game.getPlayer()), null);

								} else {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)",
											item.getItemType().getUseTooltipDescription(owner, owner),
											Main.combat.ENEMY_ATTACK){
										@Override
										public void effects(){
											Main.combat.addItemToBeUsed(owner, owner, item);
											resetPostAction();
											Main.mainController.openInventory();
										}
									};
								}

							} else if(index == 7) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", "You can only use one item at a time during combat!", null);

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {//TODO on ally though???
								if(Main.game.getPlayer().isStunned()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Opponent)", "You cannot use any items while you're stunned!", null);

								} else if(Main.combat.isCombatantDefeated(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Opponent)", "You cannot use any items while you're defeated!", null);

								} else if(Main.game.getPlayer().getRemainingAP()<CombatMove.ITEM_USAGE.getAPcost(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Opponent)", "You need at least "+CombatMove.ITEM_USAGE.getAPcost(Main.game.getPlayer())+" AP to use this actions!", null);

								} else if (!item.isAbleToBeUsedInCombatEnemies()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Opponent)", "You cannot use this during combat!", null);

								} else if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Opponent)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if (!item.isAbleToBeUsed(inventoryNPC)) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Opponent)", item.getUnableToBeUsedDescription(inventoryNPC), null);

								} else if(item.getItemType().isFetishGiving()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Opponent)",
											item.getItemType().getUseTooltipDescription(owner, inventoryNPC),
											Main.combat.ENEMY_ATTACK,
											Util.newArrayListOfValues(Fetish.FETISH_KINK_GIVING),
											Fetish.FETISH_KINK_GIVING.getAssociatedCorruptionLevel(),
											null,
											null,
											null){
										@Override
										public void effects(){
											Main.combat.addItemToBeUsed(owner, inventoryNPC, item);
											resetPostAction();
											Main.mainController.openInventory();
										}
									};
								} else if(item.getItemType().isTransformative()) {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName()) +" (Opponent)",
											item.getItemType().getUseTooltipDescription(owner, inventoryNPC),
											Main.combat.ENEMY_ATTACK,
											Util.newArrayListOfValues(Fetish.FETISH_TRANSFORMATION_GIVING),
											Fetish.FETISH_TRANSFORMATION_GIVING.getAssociatedCorruptionLevel(),
											null,
											null,
											null){
										@Override
										public void effects(){
											Main.combat.addItemToBeUsed(owner, inventoryNPC, item);
											resetPostAction();
											Main.mainController.openInventory();
										}
									};

								} else {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName()) +" (Opponent)",
											item.getItemType().getUseTooltipDescription(owner, inventoryNPC),
											Main.combat.ENEMY_ATTACK){
										@Override
										public void effects(){
											Main.combat.addItemToBeUsed(owner, inventoryNPC, item);
											resetPostAction();
											Main.mainController.openInventory();
										}
									};
								}

							} else if(index == 12) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Opponent)", "You can only use one item at a time during combat!", null);

							} else {
								return null;
							}

						case FULL_MANAGEMENT:  case CHARACTER_CREATION:
							boolean inventoryFull = inventoryNPC.isInventoryFull() && !inventoryNPC.hasItem(item);

							if(index == 1) {
								if(!item.getItemType().isAbleToBeDropped()) {
									return new Response("Give (1)", "You cannot give away the " + item.getName() + "!", null);
								} else if(inventoryFull) {
									return new Response("Give (1)", UtilText.parse(inventoryNPC, "[npc.NamePos] inventory is already full!"), null);
								}
								return new Response("Give (1)", UtilText.parse(inventoryNPC, "Give [npc.name] one " + item.getName() + "."), INVENTORY_MENU){
									@Override
									public void effects(){
										transferItems(Main.game.getPlayer(), inventoryNPC, item, 1);
									}
								};

							} else if(index == 2) {
								if(!item.getItemType().isAbleToBeDropped()) {
									return new Response("Give (5)", "You cannot give away the " + item.getName() + "!", null);
								} else if(inventoryFull) {
									return new Response("Give (5)", UtilText.parse(inventoryNPC, "[npc.NamePos] inventory is already full!"), null);
								}
								if(Main.game.getPlayer().getItemCount(item) >= 5) {
									return new Response("Give (5)", UtilText.parse(inventoryNPC, "Give [npc.name] five of your " + item.getNamePlural() + "."), INVENTORY_MENU){
										@Override
										public void effects(){
											transferItems(Main.game.getPlayer(), inventoryNPC, item, 5);
										}
									};
								} else {
									return new Response("Give (5)", "You don't have five " + item.getNamePlural() + " to give!", null);
								}

							} else if(index == 3) {
								if(!item.getItemType().isAbleToBeDropped()) {
									return new Response("Give (All)", "You cannot give away the " + item.getName() + "!", null);
								} else if(inventoryFull) {
									return new Response("Give (All)", UtilText.parse(inventoryNPC, "[npc.NamePos] inventory is already full!"), null);
								}
								return new Response("Give (All)", UtilText.parse(inventoryNPC, "Give [npc.name] all of your " + item.getNamePlural() + "."), INVENTORY_MENU){
									@Override
									public void effects(){
										transferItems(Main.game.getPlayer(), inventoryNPC, item, Main.game.getPlayer().getItemCount(item));
									}
								};

							} else if(index == 5) {
								if(item.getEnchantmentItemType(null)==null || item.getItemTags().contains(ItemTag.UNENCHANTABLE)) {
									return new Response("Enchant", "This item cannot be enchanted!", null);

								} else if(Main.game.isDebugMode()
											|| (Main.game.getPlayer().hasQuest(QuestLine.SIDE_ENCHANTMENT_DISCOVERY) && Main.game.getPlayer().isQuestCompleted(QuestLine.SIDE_ENCHANTMENT_DISCOVERY))) {
										return new Response("Enchant", "Enchant this item.", EnchantmentDialogue.ENCHANTMENT_MENU) {
											@Override
											public DialogueNode getNextDialogue() {
												return EnchantmentDialogue.getEnchantmentMenu(item);
											}
										};

								} else {
									return new Response("Enchant", getEnchantmentNotDiscoveredText("items"), null);
								}

							} else if(index == 6) {
								if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if (!item.isAbleToBeUsed(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)", item.getUnableToBeUsedDescription(Main.game.getPlayer()), null);

								} else {
									if(item.isBreakOutOfInventory()) {
										return new ResponseEffectsOnly(
												Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)",
												item.getItemType().getUseTooltipDescription(owner, owner)){
											@Override
											public void effects(){
												Main.game.getPlayer().useItem(item, Main.game.getPlayer(), false);
												resetPostAction();
											}
										};
									}
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)",
											item.getItemType().getUseTooltipDescription(owner, owner),
											INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + Main.game.getPlayer().useItem(item, Main.game.getPlayer(), false) + "</p>");
											resetPostAction();
										}
									};
								}

							} else if(index == 7) {
								if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if(!item.isAbleToBeUsed(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", item.getUnableToBeUsedDescription(Main.game.getPlayer()), null);

								} else {
									if(item.isBreakOutOfInventory()) {
										return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", "As this item has special effects, you can only use one at a time!", null);
									}
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)",
											item.getItemType().getUseTooltipDescription(owner, owner)
												+"<br/>[style.italicsMinorGood(Repeat this for all of the " + item.getNamePlural() + " which are in your inventory.)]",
											INVENTORY_MENU){
										@Override
										public void effects(){
											int itemCount = Main.game.getPlayer().getItemCount(item);
											for(int i=0;i<itemCount;i++) {
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + Main.game.getPlayer().useItem(item, Main.game.getPlayer(), false) + "</p>");
											}
											resetPostAction();
										}
									};
								}

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {
								if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " ([npc.HerHim])"), item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if (!item.isAbleToBeUsed(inventoryNPC)) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " ([npc.HerHim])"), item.getUnableToBeUsedDescription(inventoryNPC), null);

								} else if(item.isBreakOutOfInventory()) {
									return new ResponseEffectsOnly(
											Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " ([npc.HerHim])"),
											item.getItemType().getUseTooltipDescription(owner, owner)){
										@Override
										public void effects(){
											Main.game.getPlayer().useItem(item, inventoryNPC, false);
											resetPostAction();
										}
									};

								} else if(item.getItemType().isFetishGiving()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " ([npc.HerHim])"),
											item.getItemType().getUseTooltipDescription(owner, inventoryNPC),
											INVENTORY_MENU,
											Util.newArrayListOfValues(Fetish.FETISH_KINK_GIVING),
											Fetish.FETISH_KINK_GIVING.getAssociatedCorruptionLevel(),
											null,
											null,
											null){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append(inventoryNPC.getItemUseEffects(item, owner, Main.game.getPlayer(), inventoryNPC).getValue());
											resetPostAction();
										}
									};
								} else if(item.getItemType().isTransformative()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " ([npc.HerHim])"),
											item.getItemType().getUseTooltipDescription(owner, inventoryNPC),
											INVENTORY_MENU,
											Util.newArrayListOfValues(Fetish.FETISH_TRANSFORMATION_GIVING),
											Fetish.FETISH_TRANSFORMATION_GIVING.getAssociatedCorruptionLevel(),
											null,
											null,
											null){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append(inventoryNPC.getItemUseEffects(item, owner, Main.game.getPlayer(), inventoryNPC).getValue());
											resetPostAction();
										}
									};

								} else {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " ([npc.HerHim])"),
											item.getItemType().getUseTooltipDescription(owner, inventoryNPC),
											INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append(inventoryNPC.getItemUseEffects(item, owner, Main.game.getPlayer(), inventoryNPC).getValue());
											resetPostAction();
										}
									};
								}
							} else if(index == 12) {
								if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " all ([npc.HerHim])"), item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if(!item.isAbleToBeUsed(inventoryNPC)) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " all ([npc.HerHim])"), item.getUnableToBeUsedDescription(inventoryNPC), null);

								} else if(item.isBreakOutOfInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " all ([npc.HerHim])"), "As this item has special effects, you can only use one at a time!", null);

								} else if(item.getItemType().isFetishGiving()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " all ([npc.HerHim])"),
											item.getItemType().getUseTooltipDescription(owner, inventoryNPC)
												+"<br/>[style.italicsMinorGood(Repeat this for all of the " + item.getNamePlural() + " which are in your inventory.)]",
											INVENTORY_MENU,
											Util.newArrayListOfValues(Fetish.FETISH_KINK_GIVING),
											Fetish.FETISH_KINK_GIVING.getAssociatedCorruptionLevel(),
											null,
											null,
											null){
										@Override
										public void effects(){
											int itemCount = Main.game.getPlayer().getItemCount(item);
											for(int i=0;i<itemCount;i++) {
												Main.game.getTextEndStringBuilder().append(inventoryNPC.getItemUseEffects(item, owner, Main.game.getPlayer(), inventoryNPC).getValue());
											}
											resetPostAction();
										}
									};
								} else if(item.getItemType().isTransformative()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " all ([npc.HerHim])"),
											item.getItemType().getUseTooltipDescription(owner, inventoryNPC)
												+"<br/>[style.italicsMinorGood(Repeat this for all of the " + item.getNamePlural() + " which are in your inventory.)]",
											INVENTORY_MENU,
											Util.newArrayListOfValues(Fetish.FETISH_TRANSFORMATION_GIVING),
											Fetish.FETISH_TRANSFORMATION_GIVING.getAssociatedCorruptionLevel(),
											null,
											null,
											null){
										@Override
										public void effects(){
											int itemCount = Main.game.getPlayer().getItemCount(item);
											for(int i=0;i<itemCount;i++) {
												Main.game.getTextEndStringBuilder().append(inventoryNPC.getItemUseEffects(item, owner, Main.game.getPlayer(), inventoryNPC).getValue());
											}
											resetPostAction();
										}
									};

								} else {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " all ([npc.HerHim])"),
											item.getItemType().getUseTooltipDescription(owner, inventoryNPC)
												+"<br/>[style.italicsMinorGood(Repeat this for all of the " + item.getNamePlural() + " which are in your inventory.)]",
											INVENTORY_MENU){
										@Override
										public void effects(){
											int itemCount = Main.game.getPlayer().getItemCount(item);
											for(int i=0;i<itemCount;i++) {
												Main.game.getTextEndStringBuilder().append(inventoryNPC.getItemUseEffects(item, owner, Main.game.getPlayer(), inventoryNPC).getValue());
											}
											resetPostAction();
										}
									};
								}

							} else {
								return null;
							}

						case SEX:
							if(index == 1) {
								return new Response("Give (1)", "You can't give someone items while having sex with them!", null);

							} else if(index == 2) {
								return new Response("Give (5)", "You can't give someone items while having sex with them!", null);

							} else if(index == 3) {
								return new Response("Give (All)", "You can't give someone items while having sex with them!", null);

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant items while having sex with someone!", null);

							} else if(index == 6) {
								if(!Main.sex.isItemUseAvailable()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", "Items cannot be used during this sex scene!", null);

								} else if (!item.isAbleToBeUsedInSex()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", "You cannot use this during sex!", null);

								} else if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if (!item.isAbleToBeUsed(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)", item.getUnableToBeUsedDescription(Main.game.getPlayer()), null);

								} else {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)",
											item.getItemType().getUseTooltipDescription(owner, owner),
											Main.sex.SEX_DIALOGUE){
										@Override
										public void effects(){
											Main.sex.setUsingItemText(((NPC)Main.sex.getTargetedPartner(Main.game.getPlayer())).getItemUseEffects(item, owner, Main.game.getPlayer(), Main.game.getPlayer()).getValue());
											resetPostAction();
											Main.mainController.openInventory();
											Main.sex.endSexTurn(SexActionUtility.PLAYER_USE_ITEM);
											Main.sex.setSexStarted(true);
										}
									};
								}

							} else if(index == 7) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", "You can only use one item at a time during sex!", null);

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {
								if(!Main.sex.isItemUseAvailable()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (partner)", "Items cannot be used during this sex scene!", null);

								} else if (!item.isAbleToBeUsedInSex()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (partner)", "You cannot use this during sex!", null);

								} else if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (partner)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if (!item.isAbleToBeUsed(inventoryNPC)) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (partner)", item.getUnableToBeUsedDescription(inventoryNPC), null);

								} else if(item.getItemType().isFetishGiving()) {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName()) +" (partner)",
											item.getItemType().getUseTooltipDescription(owner, inventoryNPC),
											Main.sex.SEX_DIALOGUE,
											Util.newArrayListOfValues(Fetish.FETISH_KINK_GIVING),
											Fetish.FETISH_KINK_GIVING.getAssociatedCorruptionLevel(),
											null,
											null,
											null){
										@Override
										public void effects(){
											Main.sex.setUsingItemText(inventoryNPC.getItemUseEffects(item, owner, Main.game.getPlayer(), inventoryNPC).getValue());
											resetPostAction();
											Main.mainController.openInventory();
											Main.sex.endSexTurn(SexActionUtility.PLAYER_USE_ITEM);
											Main.sex.setSexStarted(true);
										}
									};

								} else if(item.getItemType().isTransformative()) {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName()) +" (partner)",
											item.getItemType().getUseTooltipDescription(owner, inventoryNPC),
											Main.sex.SEX_DIALOGUE,
											Util.newArrayListOfValues(Fetish.FETISH_TRANSFORMATION_GIVING),
											Fetish.FETISH_TRANSFORMATION_GIVING.getAssociatedCorruptionLevel(),
											null,
											null,
											null){
										@Override
										public void effects(){
											Main.sex.setUsingItemText(inventoryNPC.getItemUseEffects(item, owner, Main.game.getPlayer(), inventoryNPC).getValue());
											resetPostAction();
											Main.mainController.openInventory();
											Main.sex.endSexTurn(SexActionUtility.PLAYER_USE_ITEM);
											Main.sex.setSexStarted(true);
										}
									};

								} else {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName()) +" (partner)",
											item.getItemType().getUseTooltipDescription(owner, inventoryNPC),
											Main.sex.SEX_DIALOGUE){
										@Override
										public void effects(){
											Main.sex.setUsingItemText(inventoryNPC.getItemUseEffects(item, owner, Main.game.getPlayer(), inventoryNPC).getValue());
											resetPostAction();
											Main.mainController.openInventory();
											Main.sex.endSexTurn(SexActionUtility.PLAYER_USE_ITEM);
											Main.sex.setSexStarted(true);
										}
									};
								}

							} else if(index == 12) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (partner)", "You can only use one item at a time during sex!", null);

							} else {
								return null;
							}

						case TRADING:
							if(index == 1) {
								if(!item.getItemType().isAbleToBeSold()) {
									return new Response("Sell (1)", "You cannot sell the " + item.getName() + "!", null);

								} else if (inventoryNPC.willBuy(item)) {
									int sellPrice = item.getPrice(inventoryNPC.getBuyModifier());
									return new Response("Sell (1) (" + UtilText.formatAsMoney(sellPrice, "span") + ")", "Sell the " + item.getName() + " for " + UtilText.formatAsMoney(sellPrice) + ".", INVENTORY_MENU){
										@Override
										public void effects(){
											sellItems(Main.game.getPlayer(), inventoryNPC, item, 1, sellPrice);
										}
									};
								} else {
									return new Response("Sell (1)", inventoryNPC.getName("The") + " doesn't want to buy this.", null);
								}

							} else if(index == 2) {
								if(Main.game.getPlayer().getItemCount(item) >= 5) {
									if(!item.getItemType().isAbleToBeSold()) {
										return new Response("Sell (5)", "You cannot sell the " + item.getName() + "!", null);

									} else if (inventoryNPC.willBuy(item)) {
										int sellPrice = item.getPrice(inventoryNPC.getBuyModifier());
										return new Response("Sell (5) (" + UtilText.formatAsMoney(sellPrice*5, "span") + ")", "Sell five of your " + item.getNamePlural() + " for " + UtilText.formatAsMoney(sellPrice*5) + ".", INVENTORY_MENU){
											@Override
											public void effects(){
												sellItems(Main.game.getPlayer(), inventoryNPC, item, 5, sellPrice);
											}
										};
									} else {
										return new Response("Sell (5)", inventoryNPC.getName("The") + " doesn't want to buy these.", null);
									}

								} else {
									return new Response("Sell (5)", "You don't have five " + item.getNamePlural() + " to sell!", null);
								}

							} else if(index == 3) {
								if(!item.getItemType().isAbleToBeSold()) {
									return new Response("Sell (All)", "You cannot sell the " + item.getName() + "!", null);

								} else if (inventoryNPC.willBuy(item)) {
									int sellPrice = item.getPrice(inventoryNPC.getBuyModifier());
									return new Response("Sell (All) (" + UtilText.formatAsMoney(sellPrice*Main.game.getPlayer().getItemCount(item), "span") + ")",
											"Sell the " + item.getName() + " for " + UtilText.formatAsMoney(sellPrice*Main.game.getPlayer().getItemCount(item)) + ".", INVENTORY_MENU){
										@Override
										public void effects(){
											sellItems(Main.game.getPlayer(), inventoryNPC, item, Main.game.getPlayer().getItemCount(item), sellPrice);
										}
									};
								} else {
									return new Response("Sell (All)", inventoryNPC.getName("The") + " doesn't want to buy these.", null);
								}

							} else if(index == 5) {
								if(item.getEnchantmentItemType(null)==null || item.getItemTags().contains(ItemTag.UNENCHANTABLE)) {
									return new Response("Enchant", "This item cannot be enchanted!", null);

								} else if(Main.game.isDebugMode()
										|| (Main.game.getPlayer().hasQuest(QuestLine.SIDE_ENCHANTMENT_DISCOVERY) && Main.game.getPlayer().isQuestCompleted(QuestLine.SIDE_ENCHANTMENT_DISCOVERY))) {
									return new Response("Enchant", "Enchant this item.", EnchantmentDialogue.ENCHANTMENT_MENU) {
										@Override
										public DialogueNode getNextDialogue() {
											return EnchantmentDialogue.getEnchantmentMenu(item);
										}
									};

								} else {
									return new Response("Enchant", getEnchantmentNotDiscoveredText("items"), null);
								}

							} else if(index == 6) {
								if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if (!item.isAbleToBeUsed(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)", item.getUnableToBeUsedDescription(Main.game.getPlayer()), null);

								} else {
									if(item.isBreakOutOfInventory()) {
										return new ResponseEffectsOnly(
												Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)",
												item.getItemType().getUseTooltipDescription(owner, owner)){
											@Override
											public void effects(){
												Main.game.getPlayer().useItem(item, Main.game.getPlayer(), false);
												resetPostAction();
											}
										};
									}
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)",
											item.getItemType().getUseTooltipDescription(owner, owner),
											INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + Main.game.getPlayer().useItem(item, Main.game.getPlayer(), false) + "</p>");
											resetPostAction();
										}
									};
								}

							} else if(index == 7) {
								if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if(!item.isAbleToBeUsed(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", item.getUnableToBeUsedDescription(Main.game.getPlayer()), null);

								} else {
									if(item.isBreakOutOfInventory()) {
										return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", "As this item has special effects, you can only use one at a time!", null);
									}
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)",
											item.getItemType().getUseTooltipDescription(owner, owner)
												+"<br/>[style.italicsMinorGood(Repeat this for all of the " + item.getNamePlural() + " which are in your inventory.)]",
											INVENTORY_MENU){
										@Override
										public void effects(){
											int itemCount = Main.game.getPlayer().getItemCount(item);
											for(int i=0;i<itemCount;i++) {
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + Main.game.getPlayer().useItem(item, Main.game.getPlayer(), false) + "</p>");
											}
											resetPostAction();
										}
									};
								}

							} else if (index == 9) {
								return getBuybackResponse();

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " ([npc.HerHim])"), UtilText.parse(inventoryNPC, "[npc.Name] doesn't want to use your items."), null);

							} else if(index == 12) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " all ([npc.HerHim])"), UtilText.parse(inventoryNPC, "[npc.Name] doesn't want to use your items."), null);

							} else {
								return null;
							}
					}
				}

			// ****************************** ITEM DOES NOT BELONG TO PLAYER ******************************

			} else {
				// ****************************** Interacting with the ground ******************************
				if(inventoryNPC == null) {
					boolean inventoryFull = Main.game.getPlayer().isInventoryFull() && !Main.game.getPlayer().hasItem(item) && item.getRarity()!=Rarity.QUEST;

					switch(interactionType) {
						case SEX:
							if(index == 1) {
								return new Response("Take (1)", "You can't pick up items while masturbating.", null);

							} else if(index == 2) {
								return new Response("Take (5)", "You can't pick up items while masturbating.", null);

							} else if(index == 3) {
								return new Response("Take (All)", "You can't pick up items while masturbating.", null);

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant items while masturbating.", null);

							} else if(index == 6) {
								if(!Main.sex.isItemUseAvailable()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", "Items cannot be used during this sex scene!", null);

								} else if (!item.isAbleToBeUsedInSex()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", "You cannot use this during sex!", null);

								} else if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if (!item.isAbleToBeUsed(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)", item.getUnableToBeUsedDescription(Main.game.getPlayer()), null);

								} else {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)",
											item.getItemType().getUseTooltipDescription(Main.game.getPlayer(), Main.game.getPlayer()),
											Main.sex.SEX_DIALOGUE){
										@Override
										public void effects(){
											Main.sex.setUsingItemText(Main.game.getPlayer().useItem(item, Main.game.getPlayer(), true));
											resetPostAction();
											Main.mainController.openInventory();
											Main.sex.endSexTurn(SexActionUtility.PLAYER_USE_ITEM);
											Main.sex.setSexStarted(true);
										}
									};
								}

							} else if(index == 7) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", "You can only use one item at a time during sex!", null);

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else {
								return null;
							}

						default:
							if(index == 1) {
								if(inventoryFull) {
									return new Response("Take (1)", "Your inventory is already full!", null);
								}
								return new Response("Take (1)", "Take one " + item.getName() + " from the ground.", INVENTORY_MENU){
									@Override
									public void effects(){
										pickUpItems(Main.game.getPlayer(), item, 1);
									}
								};

							} else if(index == 2) {
								if(inventoryFull) {
									return new Response("Take (5)", "Your inventory is already full!", null);
								}
								if(Main.game.getPlayerCell().getInventory().getItemCount(item) >= 5) {
									return new Response("Take (5)", "Take five of the " + item.getNamePlural() + " from the ground.", INVENTORY_MENU){
										@Override
										public void effects(){
											pickUpItems(Main.game.getPlayer(), item, 5);
										}
									};
								} else {
									return new Response("Take (5)", "There aren't five " + item.getNamePlural() + " on the ground!", null);
								}

							} else if(index == 3) {
								if(inventoryFull) {
									return new Response("Take (All)", "Your inventory is already full!", null);
								}
								return new Response("Take (All)", "Take all of the " + item.getNamePlural() + " from the ground.", INVENTORY_MENU){
									@Override
									public void effects(){
										pickUpItems(Main.game.getPlayer(), item, Main.game.getPlayerCell().getInventory().getItemCount(item));
									}
								};

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant items on the ground!", null);

							} else if(index == 6) {
								if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if (!item.isAbleToBeUsed(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)", item.getUnableToBeUsedDescription(Main.game.getPlayer()), null);

								} else {
									if(item.isBreakOutOfInventory()) {
										return new ResponseEffectsOnly(
												Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)",
												item.getItemType().getUseTooltipDescription(Main.game.getPlayer(), Main.game.getPlayer())){
											@Override
											public void effects(){
												Main.game.getPlayer().useItem(item, Main.game.getPlayer(), true);
												resetPostAction();
											}
										};
									}
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)",
											item.getItemType().getUseTooltipDescription(Main.game.getPlayer(), Main.game.getPlayer()),
											INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + Main.game.getPlayer().useItem(item, Main.game.getPlayer(), true) + "</p>");
											resetPostAction();
										}
									};
								}

							} else if(index == 7) {
								if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if(!item.isAbleToBeUsed(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", item.getUnableToBeUsedDescription(Main.game.getPlayer()), null);

								} else {
									if(item.isBreakOutOfInventory()) {
										return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" all (Self)", "As this item has special effects, you can only use one at a time!", null);
									}
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)",
											item.getItemType().getUseTooltipDescription(Main.game.getPlayer(), Main.game.getPlayer())
											+"<br/>[style.italicsMinorGood(Repeat this for all of the " + item.getNamePlural() + " which are in this area.)]",
											INVENTORY_MENU){
										@Override
										public void effects(){
											int itemCount = Main.game.getPlayerCell().getInventory().getItemCount(item);
											for(int i=0;i<itemCount;i++) {
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + Main.game.getPlayer().useItem(item, Main.game.getPlayer(), true) + "</p>");
											}
											resetPostAction();
										}
									};
								}

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else {
								return null;
							}
					}

				// ****************************** Interacting with an NPC ******************************
				} else {
					boolean inventoryFull = false;
					switch(interactionType) {
						case COMBAT:
							if(index == 1) {
								return new Response("Take (1)", "You can't take someone items while fighting them!", null);

							} else if(index == 2) {
								return new Response("Take (5)", "You can't take someone items while fighting them!", null);

							} else if(index == 3) {
								return new Response("Take (All)", "You can't take someone items while fighting them!", null);

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant someone else's items, especially not while fighting them!", null);

							} else if(index == 6) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)", "You can't use someone else's items while fighting them!", null);

							} else if(index == 7) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" all (Self)", "You can't use someone else's items while fighting them!", null);

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Opponent)", "You can't use make someone use an item while fighting them!", null);

							} else if(index == 12) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" all (Opponent)", "You can't use make someone use an item while fighting them!", null);

							} else {
								return null;
							}

						case FULL_MANAGEMENT:  case CHARACTER_CREATION:
							inventoryFull = Main.game.getPlayer().isInventoryFull() && !Main.game.getPlayer().hasItem(item) && item.getRarity()!=Rarity.QUEST;

							if(index == 1) {
								if(inventoryFull) {
									return new Response("Take (1)", "Your inventory is already full!", null);
								}
								return new Response("Take (1)", UtilText.parse(inventoryNPC, "Take " + item.getName() + " from [npc.name]."), INVENTORY_MENU){
									@Override
									public void effects(){
										transferItems(inventoryNPC, Main.game.getPlayer(), item, 1);
									}
								};

							} else if(index == 2) {
								if(inventoryFull) {
									return new Response("Take (5)", "Your inventory is already full!", null);
								}
								if(inventoryNPC.getItemCount(item) >= 5) {
									return new Response("Take (5)", UtilText.parse(inventoryNPC, "Take five of  [npc.namePos] " + item.getNamePlural() + "."), INVENTORY_MENU){
										@Override
										public void effects(){
											transferItems(inventoryNPC, Main.game.getPlayer(), item, 5);
										}
									};
								} else {
									return new Response("Take (5)", UtilText.parse(inventoryNPC, "[npc.Name] doesn't have five " + item.getNamePlural() + "!"), null);
								}

							} else if(index == 3) {
								if(inventoryFull) {
									return new Response("Take (All)", "Your inventory is already full!", null);
								}
								return new Response("Take (All)", UtilText.parse(inventoryNPC, "Take all "+Util.intToString(inventoryNPC.getItemCount(item))+" of  [npc.namePos] " + item.getNamePlural() + "."), INVENTORY_MENU){
									@Override
									public void effects(){
										transferItems(inventoryNPC, Main.game.getPlayer(), item, inventoryNPC.getItemCount(item));
									}
								};

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant items owned by someone else!", null);

							} else if(index == 6) {
								if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if (!item.isAbleToBeUsed(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)", item.getUnableToBeUsedDescription(Main.game.getPlayer()), null);

								} else {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)",
											item.getItemType().getUseTooltipDescription(Main.game.getPlayer(), Main.game.getPlayer()),
											INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + Main.game.getPlayer().useItem(item, Main.game.getPlayer(), false, false, false) + "</p>");
											if (item.isConsumedOnUse()) {
												inventoryNPC.getInventory().removeItem(item);
											}
											resetPostAction();
										}
									};
								}

							} else if(index == 7) {
								if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if(!item.isAbleToBeUsed(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", item.getUnableToBeUsedDescription(Main.game.getPlayer()), null);

								} else {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)",
											item.getItemType().getUseTooltipDescription(Main.game.getPlayer(), Main.game.getPlayer())
												+"<br/>[style.italicsMinorGood(Repeat this for all of the " + item.getNamePlural() + " which are in [npc.namePos] inventory.)]",
											INVENTORY_MENU){
										@Override
										public void effects(){
											int itemCount = inventoryNPC.getItemCount(item);
											for(int i=0;i<itemCount;i++) {
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + Main.game.getPlayer().useItem(item, Main.game.getPlayer(), false, false, false) + "</p>");
											}
											if (item.isConsumedOnUse()) {
												inventoryNPC.getInventory().removeItem(item, itemCount);
											}
											resetPostAction();
										}
									};
								}

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {
								if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " ([npc.HerHim])"), item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if (!item.isAbleToBeUsed(inventoryNPC)) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " ([npc.HerHim])"), item.getUnableToBeUsedDescription(inventoryNPC), null);

								} else if(item.isBreakOutOfInventory()) {
									return new ResponseEffectsOnly(
											Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " ([npc.HerHim])"),
											item.getItemType().getUseTooltipDescription(owner, owner)){
										@Override
										public void effects(){
											Main.game.getPlayer().useItem(item, inventoryNPC, false);
											resetPostAction();
										}
									};

								} else if(item.getItemType().isFetishGiving()) {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " ([npc.HerHim])"),
											item.getItemType().getUseTooltipDescription(Main.game.getPlayer(), inventoryNPC),
											INVENTORY_MENU,
											Util.newArrayListOfValues(Fetish.FETISH_KINK_GIVING),
											Fetish.FETISH_KINK_GIVING.getAssociatedCorruptionLevel(),
											null,
											null,
											null){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append(inventoryNPC.getItemUseEffects(item, owner, Main.game.getPlayer(), inventoryNPC).getValue());
											resetPostAction();
										}
									};
								} else if(item.getItemType().isTransformative()) {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " ([npc.HerHim])"),
											item.getItemType().getUseTooltipDescription(Main.game.getPlayer(), inventoryNPC),
											INVENTORY_MENU,
											Util.newArrayListOfValues(Fetish.FETISH_TRANSFORMATION_GIVING),
											Fetish.FETISH_TRANSFORMATION_GIVING.getAssociatedCorruptionLevel(),
											null,
											null,
											null){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append(inventoryNPC.getItemUseEffects(item, owner, Main.game.getPlayer(), inventoryNPC).getValue());
											resetPostAction();
										}
									};

								} else {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " ([npc.HerHim])"),
											item.getItemType().getUseTooltipDescription(Main.game.getPlayer(), inventoryNPC),
											INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append(inventoryNPC.getItemUseEffects(item, owner, Main.game.getPlayer(), inventoryNPC).getValue());
											resetPostAction();
										}
									};
								}

							} else if(index == 12) {
								if (!item.isAbleToBeUsedFromInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " all ([npc.HerHim])"), item.getUnableToBeUsedFromInventoryDescription(), null);

								} else if(!item.isAbleToBeUsed(inventoryNPC)) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " all ([npc.HerHim])"), item.getUnableToBeUsedDescription(inventoryNPC), null);

								} else if(item.isBreakOutOfInventory()) {
									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " all ([npc.HerHim])"), "As this item has special effects, you can only use one at a time!", null);

								} else if(item.getItemType().isFetishGiving()) {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " all ([npc.HerHim])"),
											item.getItemType().getUseTooltipDescription(Main.game.getPlayer(), inventoryNPC)
												+"<br/>[style.italicsMinorGood(Repeat this for all of the " + item.getNamePlural() + " which are in [npc.namePos] inventory.)]",
											INVENTORY_MENU,
											Util.newArrayListOfValues(Fetish.FETISH_KINK_GIVING),
											Fetish.FETISH_KINK_GIVING.getAssociatedCorruptionLevel(),
											null,
											null,
											null){
										@Override
										public void effects(){
											int itemCount = inventoryNPC.getItemCount(item);
											for(int i=0;i<itemCount;i++) {
												Main.game.getTextEndStringBuilder().append(inventoryNPC.getItemUseEffects(item, owner, Main.game.getPlayer(), inventoryNPC).getValue());
											}
											resetPostAction();
										}
									};

								} else if(item.getItemType().isTransformative()) {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " all ([npc.HerHim])"),
											item.getItemType().getUseTooltipDescription(Main.game.getPlayer(), inventoryNPC)
												+"<br/>[style.italicsMinorGood(Repeat this for all of the " + item.getNamePlural() + " which are in [npc.namePos] inventory.)]",
											INVENTORY_MENU,
											Util.newArrayListOfValues(Fetish.FETISH_TRANSFORMATION_GIVING),
											Fetish.FETISH_TRANSFORMATION_GIVING.getAssociatedCorruptionLevel(),
											null,
											null,
											null){
										@Override
										public void effects(){
											int itemCount = inventoryNPC.getItemCount(item);
											for(int i=0;i<itemCount;i++) {
												Main.game.getTextEndStringBuilder().append(inventoryNPC.getItemUseEffects(item, owner, Main.game.getPlayer(), inventoryNPC).getValue());
											}
											resetPostAction();
										}
									};

								} else {
									return new Response(
											Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " all ([npc.HerHim])"),
											item.getItemType().getUseTooltipDescription(Main.game.getPlayer(), inventoryNPC)
												+"<br/>[style.italicsMinorGood(Repeat this for all of the " + item.getNamePlural() + " which are in [npc.namePos] inventory.)]",
											INVENTORY_MENU){
										@Override
										public void effects(){
											int itemCount = inventoryNPC.getItemCount(item);
											for(int i=0;i<itemCount;i++) {
												Main.game.getTextEndStringBuilder().append(inventoryNPC.getItemUseEffects(item, owner, Main.game.getPlayer(), inventoryNPC).getValue());
											}
											resetPostAction();
										}
									};
								}

							} else {
								return null;
							}

						case SEX:
							if(index == 1) {
								return new Response("Take (1)", "You can't take someone's items while having sex with them!", null);

							} else if(index == 2) {
								return new Response("Take (5)", "You can't take someone's items while having sex with them!", null);

							} else if(index == 3) {
								return new Response("Take (All)", "You can't take someone's items while having sex with them!", null);

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant someone else's items, especially not while having sex with them!", null);

							} else if(index == 6) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", "You can't use your partner's items during sex!", null);
								//TODO
//								if (!item.isAbleToBeUsedInSex()) {
//									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", "This cannot be used during sex!", null);
//
//								} else if (!item.isAbleToBeUsedFromInventory()) {
//									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (Self)", item.getUnableToBeUsedFromInventoryDescription(), null);
//
//								} else if (!item.isAbleToBeUsed(Main.game.getPlayer())) {
//									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)", item.getUnableToBeUsedDescription(Main.game.getPlayer()), null);
//
//								} else {
//									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)",
//											Util.capitaliseSentence(item.getItemType().getUseName()) + " the " + item.getName() + ".", Main.sex.SEX_DIALOGUE){
//										@Override
//										public void effects(){
//											Main.sex.setUsingItemText(Main.sex.getPartner().getItemUseEffects(item, owner, inventoryNPC, Main.game.getPlayer()));
//											resetPostAction();
//											Main.mainController.openInventory();
//											Main.sex.endSexTurn(SexActionUtility.PLAYER_USE_ITEM);
//											Main.sex.setSexStarted(true);
//										}
//									};
//								}

							} else if(index == 7) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (Self)", "You can only use one item at a time during sex!", null);

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (partner)", "You can't use your partner's items during sex!", null);
								//TODO
//								if (!item.isAbleToBeUsedInSex()) {
//									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (partner)", "This cannot be used during sex!", null);
//
//								} else if (!item.isAbleToBeUsedFromInventory()) {
//									return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" (partner)", item.getUnableToBeUsedFromInventoryDescription(), null);
//
//								} else if (!item.isAbleToBeUsed(inventoryNPC)) {
//									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (partner)", item.getUnableToBeUsedDescription(inventoryNPC), null);
//
//								} else if(item.getItemType().isFetishGiving()) {
//									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (partner)",
//											"Get "+inventoryNPC.getName("the")+" to "+ item.getItemType().getUseName() + " the " + item.getName() + ".",
//											Main.sex.SEX_DIALOGUE,
//											Util.newArrayListOfValues(Fetish.FETISH_KINK_GIVING),
//											Fetish.v.getAssociatedCorruptionLevel(),
//											null,
//											null,
//											null){
//										@Override
//										public void effects(){
//											Main.sex.setUsingItemText(Main.sex.getPartner().getItemUseEffects(item, owner, inventoryNPC, inventoryNPC));
//											resetPostAction();
//											Main.mainController.openInventory();
//											Main.sex.endSexTurn(SexActionUtility.PLAYER_USE_ITEM);
//											Main.sex.setSexStarted(true);
//										}
//									};
//								} else if(item.getItemType().isTransformative()) {
//									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (partner)",
//											"Get "+inventoryNPC.getName("the")+" to "+ item.getItemType().getUseName() + " the " + item.getName() + ".",
//											Main.sex.SEX_DIALOGUE,
//											Util.newArrayListOfValues(Fetish.FETISH_TRANSFORMATION_GIVING),
//											Fetish.FETISH_TRANSFORMATION_GIVING.getAssociatedCorruptionLevel(),
//											null,
//											null,
//											null){
//										@Override
//										public void effects(){
//											Main.sex.setUsingItemText(Main.sex.getPartner().getItemUseEffects(item, owner, inventoryNPC, inventoryNPC));
//											resetPostAction();
//											Main.mainController.openInventory();
//											Main.sex.endSexTurn(SexActionUtility.PLAYER_USE_ITEM);
//											Main.sex.setSexStarted(true);
//										}
//									};
//
//								} else {
//									return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (partner)",
//											"Get "+inventoryNPC.getName("the")+" to "+ item.getItemType().getUseName() + " the " + item.getName() + ".", Main.sex.SEX_DIALOGUE){
//										@Override
//										public void effects(){
//											Main.sex.setUsingItemText(Main.sex.getPartner().getItemUseEffects(item, owner, inventoryNPC, inventoryNPC));
//											resetPostAction();
//											Main.mainController.openInventory();
//											Main.sex.endSexTurn(SexActionUtility.PLAYER_USE_ITEM);
//											Main.sex.setSexStarted(true);
//										}
//									};
//								}

							} else if(index == 12) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+" all (partner)", "You can only use one item at a time during sex!", null);

							} else {
								return null;
							}

						case TRADING:
							inventoryFull = Main.game.getPlayer().isInventoryFull() && !Main.game.getPlayer().hasItem(item)  && item.getRarity()!=Rarity.QUEST;

							if(index == 1) {
								int sellPrice = buyback?Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getPrice():item.getPrice(inventoryNPC.getSellModifier(item));
								if(inventoryFull) {
									return new Response("Buy (1) ("+UtilText.formatAsMoneyUncoloured(sellPrice, "span")+")", "Your inventory is already full!", null);
								}
								if(Main.game.getPlayer().getMoney() < sellPrice) {
									return new Response("Buy (1) ("+UtilText.formatAsMoneyUncoloured(sellPrice, "span")+")", "You can't afford to buy this!", null);
								}
								return new Response("Buy (1) (" + UtilText.formatAsMoney(sellPrice, "span") + ")", "Buy the " + item.getName() + " for " + UtilText.formatAsMoney(sellPrice) + ".", INVENTORY_MENU){
									@Override
									public void effects(){
										sellItems(inventoryNPC, Main.game.getPlayer(), item, 1, sellPrice);
									}
								};

							} else if(index == 2) {
								int sellPrice = buyback?Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getPrice():item.getPrice(inventoryNPC.getSellModifier(item));
								if((buyback && Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getCount()<5)
										|| (!buyback && inventoryNPC.getItemCount(item) < 5)) {
									return new Response("Buy (5) ("+UtilText.formatAsMoneyUncoloured(sellPrice*5, "span")+")", UtilText.parse(inventoryNPC, "[npc.Name] doesn't have five "+item.getNamePlural()+"."), null);
								}
								if(inventoryFull) {
									return new Response("Buy (5) ("+UtilText.formatAsMoneyUncoloured(sellPrice*5, "span")+")", "Your inventory is already full!", null);
								}
								if(Main.game.getPlayer().getMoney() < sellPrice*5) {
									return new Response("Buy (5) ("+UtilText.formatAsMoneyUncoloured(sellPrice*5, "span")+")", "You can't afford to buy this!", null);
								}
								return new Response("Buy (5) (" + UtilText.formatAsMoney(sellPrice*5, "span") + ")", "Buy the " + item.getName() + " for " + UtilText.formatAsMoney(sellPrice*5) + ".", INVENTORY_MENU){
									@Override
									public void effects(){
										sellItems(inventoryNPC, Main.game.getPlayer(), item, 5, sellPrice);
									}
								};

							} else if(index == 3) {
								int sellPrice = buyback?Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getPrice():item.getPrice(inventoryNPC.getSellModifier(item));
								int count = buyback?Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getCount():inventoryNPC.getItemCount(item);
								if(inventoryFull) {
									return new Response("Buy (All) ("+UtilText.formatAsMoneyUncoloured(sellPrice*count, "span")+")", "Your inventory is already full!", null);
								}
								if(Main.game.getPlayer().getMoney() < sellPrice*count) {
									int affordableCount = Main.game.getPlayer().getMoney() / sellPrice;
									if(affordableCount > 0) {
										return new Response("Buy (Max " + affordableCount + ") (" + UtilText.formatAsMoney(sellPrice * affordableCount, "span") + ")",
												"Buy the " + item.getName() + " for " + UtilText.formatAsMoney(sellPrice * affordableCount) + ".", INVENTORY_MENU) {
											@Override
											public void effects() {
												sellItems(inventoryNPC, Main.game.getPlayer(), item, affordableCount, sellPrice);
											}
										};
									} else {
										return new Response("Buy (All) ("+UtilText.formatAsMoneyUncoloured(sellPrice*count, "span")+")", "You can't afford to buy this!", null);
									}
								}
								return new Response("Buy (All) (" + UtilText.formatAsMoney(sellPrice*count, "span") + ")",
										"Buy the " + item.getName() + " for " + UtilText.formatAsMoney(sellPrice*count) + ".", INVENTORY_MENU){
									@Override
									public void effects(){
										sellItems(inventoryNPC, Main.game.getPlayer(), item, count, sellPrice);
									}
								};

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant someone else's item!", null);

							} else if(index == 6) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" (Self)", UtilText.parse(inventoryNPC, "[npc.Name] isn't going to let you use [npc.her] items without buying them first."), null);

							} else if(index == 7) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName()) +" all (Self)", UtilText.parse(inventoryNPC, "[npc.Name] isn't going to let you use [npc.her] items without buying them first."), null);

							} else if (index == 9) {
								return getBuybackResponse();

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " ([npc.HerHim])"),
										UtilText.parse(inventoryNPC, "[npc.Name] isn't going to use the items that [npc.sheIs] trying to sell!"),
										null);

							} else if(index == 12) {
								return new Response(Util.capitaliseSentence(item.getItemType().getUseName())+UtilText.parse(inventoryNPC, " all ([npc.HerHim])"),
										UtilText.parse(inventoryNPC, "[npc.Name] isn't going to use the items that [npc.sheIs] trying to sell!"),
										null);

							} else {
								return null;
							}
					}
				}
			}
			return null;
		}

		@Override
		public DialogueNodeType getDialogueNodeType() {
			return DialogueNodeType.INVENTORY;
		}
	};

	public static final DialogueNode WEAPON_INVENTORY = new DialogueNode("Weapon", "", true) {

		@Override
		public String getLabel() {
			if (Main.game.getDialogueFlags().values.contains(DialogueFlagValue.quickTrade) && !Main.game.isInSex() && !Main.game.isInCombat()) {
				return "Inventory (Quick-Manage is <b style='color:" + PresetColour.GENERIC_GOOD.toWebHexString() + ";'>ON</b>)";
			} else {
				return "Inventory";
			}
		}

		@Override
		public String getHeaderContent() {
			return inventoryView();
		}

		@Override
		public String getContent() {
			StringBuilder sb = new StringBuilder();
			List<String> extraDescriptions = weapon.getExtraDescriptions(owner);
			if(!extraDescriptions.isEmpty()) {
				sb.append("<p>");
					for(int i=0 ; i<extraDescriptions.size() ; i++) {
						sb.append(extraDescriptions.get(i));
						if(i<extraDescriptions.size()-1) {
							sb.append("<br/>");
						}
					}
				sb.append("</p>");
			}
			return getItemDisplayPanel(weapon.getSVGString(),
					Util.capitaliseSentence(weapon.getDisplayName(true)),
					weapon.getDescription(owner)
					+ sb.toString()
					+ (owner!=null && owner.isPlayer()
							? (inventoryNPC != null && interactionType == InventoryInteraction.TRADING
									? "<p>"
										+(inventoryNPC.willBuy(weapon)
											?inventoryNPC.getName("The") + " will buy it for " + UtilText.formatAsMoney(weapon.getPrice(inventoryNPC.getBuyModifier())) + "."
											:inventoryNPC.getName("The") + " doesn't want to buy this.")
										+"</p>"
									: "")
							:(inventoryNPC != null && interactionType == InventoryInteraction.TRADING
								? "<p>"
										+ inventoryNPC.getName("The") + " will sell it for " + UtilText.formatAsMoney(weapon.getPrice(inventoryNPC.getSellModifier(weapon))) + "."
									+ "</p>"
								: "")));
		}


		public String getResponseTabTitle(int index) {
			return getGeneralResponseTabTitle(index);
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if (index == 0) {
				return getCloseInventoryResponse();
			}
			if(responseTab==0) {
				return INVENTORY_MENU.getResponse(responseTab, index);
			}

			// ****************************** ITEM BELONGS TO THE PLAYER ******************************
			if(owner != null && owner.isPlayer()) {

				// ****************************** Interacting with the ground ******************************
				if(inventoryNPC == null) {
					boolean areaFull = Main.game.isPlayerTileFull() && !Main.game.getPlayerCell().getInventory().hasWeapon(weapon);

					switch(interactionType) {
						case SEX:
							String dropTitle = owner.getLocationPlace().isItemsDisappear()?"Drop ":"Store";
							if(index == 1) {
								return new Response(dropTitle+"(1)", "You can't drop your weapons while masturbating.", null);

							} else if(index == 2) {
								return new Response(dropTitle+"(5)", "You can't drop your weapons while masturbating.", null);

							} else if(index == 3) {
								return new Response(dropTitle+"(All)", "You can't drop your weapons while masturbating.", null);

							} else if(index == 4) {
								return new Response("Dye/Reforge", "You can't dye or reforge your weapons while masturbating.", null);

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant weapons while masturbating.", null);

							} else if(index == 6) {
								return new Response("Equip Main (Self)", "You can't equip weapons while masturbating.", null);

							} else if(index == 7) {
								return new Response("Equip Offhand (Self)", "You can't equip weapons while masturbating.", null);

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else {
								return null;
							}

						default:
							if(index == 1) {
								if(owner.getLocationPlace().isItemsDisappear()) {
									if(!weapon.getWeaponType().isAbleToBeDropped()) {
										return new Response("Drop (1)", "You cannot drop the " + weapon.getName() + "!", null);

									} else if(areaFull) {
										return new Response("Drop (1)", "This area is full, so you can't drop your " + weapon.getName() + " here!", null);

									} else {
										return new Response("Drop (1)", "Drop your " + weapon.getName() + ".", INVENTORY_MENU){
											@Override
											public void effects(){
												dropWeapons(owner, weapon, 1);
											}
										};
									}
								} else {
									if(!weapon.getWeaponType().isAbleToBeDropped()) {
										return new Response("Store (1)", "You cannot drop the " + weapon.getName() + "!", null);

									} else if(areaFull) {
										return new Response("Store (1)", "This area is full, so you can't store your " + weapon.getName() + " here!", null);
									} else {
										return new Response("Store (1)", "Store the " + weapon.getName() + " in this area.", INVENTORY_MENU){
											@Override
											public void effects(){
												dropWeapons(owner, weapon, 1);
											}
										};
									}
								}

							} else if(index == 2) {
								if(owner.getLocationPlace().isItemsDisappear()) {
									if(!weapon.getWeaponType().isAbleToBeDropped()) {
										return new Response("Drop (5)", "You cannot drop the " + weapon.getName() + "!", null);

									} else if(owner.getWeaponCount(weapon) < 5) {
										return new Response("Drop (5)", "You don't have five " + weapon.getNamePlural() + " to drop!", null);

									} else if(areaFull) {
										return new Response("Drop (5)", "This area is full, so you can't drop your " + weapon.getNamePlural() + " here!", null);

									} else {
										return new Response("Drop (5)", "Drop five of your " + weapon.getNamePlural() + ".", INVENTORY_MENU){
											@Override
											public void effects(){
												dropWeapons(owner, weapon, 5);
											}
										};
									}
								} else {
									if(!weapon.getWeaponType().isAbleToBeDropped()) {
										return new Response("Store (5)", "You cannot drop the " + weapon.getName() + "!", null);

									} else if(owner.getWeaponCount(weapon) < 5) {
										return new Response("Store (5)", "You don't have five " + weapon.getNamePlural() + " to drop!", null);

									} else if(areaFull) {
										return new Response("Store (5)", "This area is full, so you can't store your " + weapon.getNamePlural() + " here!", null);

									} else {
										return new Response("Store (5)", "Store five of your " + weapon.getNamePlural() + " in this area.", INVENTORY_MENU){
											@Override
											public void effects(){
												dropWeapons(owner, weapon, 5);
											}
										};
									}
								}

							} else if(index == 3) {
								if(!weapon.getWeaponType().isAbleToBeDropped()) {
									return new Response("Drop (All)", "You cannot drop the " + weapon.getName() + "!", null);

								} else if(owner.getLocationPlace().isItemsDisappear()) {
									if(areaFull) {
										return new Response("Drop (All)", "This area is full, so you can't drop your " + weapon.getNamePlural() + " here!", null);
									} else {
										return new Response("Drop (All)", "Drop all of your " + weapon.getNamePlural() + ".", INVENTORY_MENU){
											@Override
											public void effects(){
												dropWeapons(owner, weapon, owner.getWeaponCount(weapon));
											}
										};
									}
								} else {
									if(!weapon.getWeaponType().isAbleToBeDropped()) {
										return new Response("Store (All)", "You cannot drop the " + weapon.getName() + "!", null);

									} else if(areaFull) {
										return new Response("Store (All)", "This area is full, so you can't store your " + weapon.getNamePlural() + " here!", null);
									} else {
										return new Response("Store (All)", "Store all of your " + weapon.getNamePlural() + " in this area.", INVENTORY_MENU){
											@Override
											public void effects(){
												dropWeapons(owner, weapon, owner.getWeaponCount(weapon));
											}
										};
									}
								}

							} else if (index==4) {
								if (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH)
										|| Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER)
										|| Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
									boolean hasFullInventory = Main.game.getPlayer().isInventoryFull() && weapon.getRarity()!=Rarity.QUEST;
									boolean isDyeingStackItem = Main.game.getPlayer().getAllWeaponsInInventory().get(weapon) > 1;
									boolean canDye = !(isDyeingStackItem && hasFullInventory);
									if (canDye) {
										return new Response("Dye/Reforge",
												Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
													?"Use your proficiency with [style.colourEarth(Earth spells)] to dye or reforge this item."
													:"Use a dye-brush or reforge hammer to alter this weapon's properties.",
												DYE_WEAPON) {
											@Override
											public void effects() {
												resetWeaponDyeColours();
											}
										};
									} else {
										return new Response("Dye/Reforge", "Your inventory is full, so you can't alter this weapon's properties.", null);
									}
								} else {
									return new Response("Dye/Reforge", "You'll need to find a dye-brush or reforge hammer if you want to alter this weapon's properties.", null);
								}

							} else if(index == 5) {
								if(weapon.getEnchantmentItemType(null)==null || weapon.getItemTags().contains(ItemTag.UNENCHANTABLE)) {
									return new Response("Enchant", "This weapon cannot be enchanted!", null);

								} else if(Main.game.isDebugMode()
										|| (Main.game.getPlayer().hasQuest(QuestLine.SIDE_ENCHANTMENT_DISCOVERY) && Main.game.getPlayer().isQuestCompleted(QuestLine.SIDE_ENCHANTMENT_DISCOVERY))) {
									return new Response("Enchant", "Enchant this weapon.", EnchantmentDialogue.ENCHANTMENT_MENU) {
										@Override
										public DialogueNode getNextDialogue() {
											return EnchantmentDialogue.getEnchantmentMenu(weapon);
										}
									};

								} else {
									return new Response("Enchant", getEnchantmentNotDiscoveredText("weapons"), null);
								}

							} else if(index == 6) {
								InventorySlot slot = InventorySlot.mainWeaponSlots[Main.game.getPlayer().getMainWeaponIndexToEquipTo(weapon)];
								if(weapon.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									return new Response("Equip Main (Self)", "Equip the " + weapon.getName() + ".", INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append(
												"<p style='text-align:center;'>"
													+ Main.game.getPlayer().equipMainWeaponFromInventory(weapon, Main.game.getPlayer())
												+ "</p>");
											resetPostAction();
										}
									};
								} else {
									return new Response("Equip Main (Self)", weapon.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}

							} else if(index == 7) {
								if(weapon.getWeaponType().isTwoHanded()) {
									return new Response("Equip Offhand (Self)",
											(weapon.getWeaponType().isPlural()
												?"As the " + weapon.getName() + " require two hands to wield, they can only be equipped in the main slot!"
												:"As the " + weapon.getName() + " is a two-handed weapon, it can only be equipped in the main slot!"),
											null);
								}

								InventorySlot slot = InventorySlot.offhandWeaponSlots[Main.game.getPlayer().getOffhandWeaponIndexToEquipTo(weapon)];
								if(weapon.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									return new Response("Equip Offhand (Self)",
											"Equip the " + weapon.getName() + "."
												+(weapon.getWeaponType().isTwoHanded()
														?"<br/>[style.italicsGood(Although "+(weapon.getWeaponType().isPlural()?"":"")+")]"
														:""),
											INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append(
												"<p style='text-align:center;'>"
													+ Main.game.getPlayer().equipOffhandWeaponFromInventory(weapon, Main.game.getPlayer())
												+ "</p>");
											resetPostAction();
										}
									};
								} else {
									return new Response("Equip Offhand (Self)", weapon.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else {
								return null;
							}
					}

				// ****************************** Interacting with an NPC ******************************
				} else {
					switch(interactionType) {
						case COMBAT:
							if(index == 1) {
								return new Response("Give (1)", "You can't give someone weapons while fighting them!", null);

							} else if(index == 2) {
								return new Response("Give (5)", "You can't give someone weapons while fighting them!", null);

							} else if(index == 3) {
								return new Response("Give (All)", "You can't give someone weapons while fighting them!", null);

							} else if(index == 4) {
								return new Response("Dye", "You can't dye your weapons while fighting someone!", null);

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant weapons while fighting someone!", null);

							} else if(index == 6) {
								return new Response("Equip Main (Self)", "You can't change weapons while fighting someone!", null);

							} else if(index == 7) {
								return new Response("Equip Offhand (Self)", "You can't change weapons while fighting someone!", null);

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {
								return new Response("Equip (Opponent)", "You can't make your opponent equip a weapon!", null);

							} else {
								return null;
							}

						case FULL_MANAGEMENT:  case CHARACTER_CREATION:
							boolean inventoryFull = inventoryNPC.isInventoryFull() && !inventoryNPC.hasWeapon(weapon);

							if(index == 1) {
								if(!weapon.getWeaponType().isAbleToBeDropped()) {
									return new Response("Give (1)", "You cannot give away the " + weapon.getName() + "!", null);

								} else if(inventoryFull) {
									return new Response("Give (1)", UtilText.parse(inventoryNPC, "[npc.NamePos] inventory is already full!"), null);
								}
								return new Response("Give (1)", UtilText.parse(inventoryNPC, "Give [npc.name] one " + weapon.getName() + "."), INVENTORY_MENU){
									@Override
									public void effects(){
										transferWeapons(Main.game.getPlayer(), inventoryNPC, weapon, 1);
									}
								};

							} else if(index == 2) {
								if(!weapon.getWeaponType().isAbleToBeDropped()) {
									return new Response("Give (5)", "You cannot give away the " + weapon.getName() + "!", null);

								} else if(inventoryFull) {
									return new Response("Give (5)", UtilText.parse(inventoryNPC, "[npc.NamePos] inventory is already full!"), null);
								}
								if(Main.game.getPlayer().getWeaponCount(weapon) >= 5) {
									return new Response("Give (5)", UtilText.parse(inventoryNPC, "Give [npc.name] five of your " + weapon.getNamePlural() + "."), INVENTORY_MENU){
										@Override
										public void effects(){
											transferWeapons(Main.game.getPlayer(), inventoryNPC, weapon, 5);
										}
									};
								} else {
									return new Response("Give (5)", "You don't have five " + weapon.getNamePlural() + " to give!", null);
								}

							} else if(index == 3) {
								if(!weapon.getWeaponType().isAbleToBeDropped()) {
									return new Response("Give (All)", "You cannot give away the " + weapon.getName() + "!", null);

								} else if(inventoryFull) {
									return new Response("Give (All)", UtilText.parse(inventoryNPC, "[npc.NamePos] inventory is already full!"), null);
								}
								return new Response("Give (All)", UtilText.parse(inventoryNPC, "Give [npc.name] all of your " + weapon.getNamePlural() + "."), INVENTORY_MENU){
									@Override
									public void effects(){
										transferWeapons(Main.game.getPlayer(), inventoryNPC, weapon, Main.game.getPlayer().getWeaponCount(weapon));
									}
								};

							} else if (index==4) {
								if (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH)
										|| Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER)
										|| Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
									boolean hasFullInventory = Main.game.getPlayer().isInventoryFull() && weapon.getRarity()!=Rarity.QUEST;
									boolean isDyeingStackItem = Main.game.getPlayer().getAllWeaponsInInventory().get(weapon) > 1;
									boolean canDye = !(isDyeingStackItem && hasFullInventory);
									if (canDye) {
										return new Response("Dye/Reforge",
												Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
													?"Use your proficiency with [style.colourEarth(Earth spells)] to dye or reforge this item."
													:"Use a dye-brush or reforge hammer to alter this weapon's properties.",
												DYE_WEAPON) {
											@Override
											public void effects() {
												resetWeaponDyeColours();
											}
										};
									} else {
										return new Response("Dye/Reforge", "Your inventory is full, so you can't alter this weapon's properties.", null);
									}
								} else {
									return new Response("Dye/Reforge", "You'll need to find a dye-brush or reforge hammer if you want to alter this weapon's properties.", null);
								}

							} else if(index == 5) {
								if(weapon.getEnchantmentItemType(null)==null || weapon.getItemTags().contains(ItemTag.UNENCHANTABLE)) {
									return new Response("Enchant", "This weapon cannot be enchanted!", null);

								} else if(Main.game.isDebugMode()
										|| (Main.game.getPlayer().hasQuest(QuestLine.SIDE_ENCHANTMENT_DISCOVERY) && Main.game.getPlayer().isQuestCompleted(QuestLine.SIDE_ENCHANTMENT_DISCOVERY))) {
									return new Response("Enchant", "Enchant this weapon.", EnchantmentDialogue.ENCHANTMENT_MENU) {
										@Override
										public DialogueNode getNextDialogue() {
											return EnchantmentDialogue.getEnchantmentMenu(weapon);
										}
									};

								} else {
									return new Response("Enchant", getEnchantmentNotDiscoveredText("weapons"), null);
								}

							} else if(index == 6) {
								InventorySlot slot = InventorySlot.mainWeaponSlots[Main.game.getPlayer().getMainWeaponIndexToEquipTo(weapon)];
								if(weapon.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									return new Response("Equip Main (Self)", "Equip the " + weapon.getName() + " as your main weapon.", INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"
													+ Main.game.getPlayer().equipMainWeaponFromInventory(weapon, Main.game.getPlayer())
												+ "</p>");
											resetPostAction();
										}
									};
								} else {
									return new Response("Equip Main (Self)", weapon.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}

							} else if(index == 7) {
								if(weapon.getWeaponType().isTwoHanded()) {
									return new Response("Equip Offhand (Self)",
											(weapon.getWeaponType().isPlural()
												?"As the " + weapon.getName() + " require two hands to wield, they can only be equipped in the main slot!"
												:"As the " + weapon.getName() + " is a two-handed weapon, it can only be equipped in the main slot!"),
											null);
								}

								InventorySlot slot = InventorySlot.offhandWeaponSlots[Main.game.getPlayer().getOffhandWeaponIndexToEquipTo(weapon)];
								if(weapon.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									return new Response("Equip Offhand (Self)", "Equip the " + weapon.getName() + " as your offhand weapon.", INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"
													+ Main.game.getPlayer().equipOffhandWeaponFromInventory(weapon, Main.game.getPlayer())
												+ "</p>");
											resetPostAction();
										}
									};
								} else {
									return new Response("Equip Offhand (Self)", weapon.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {
//								if(!weapon.getWeaponType().isAbleToBeDropped()) {
//									return new Response(UtilText.parse(inventoryNPC, "Equip Main ([npc.HerHim])"), "You cannot give away the " + weapon.getName() + "!", null);
//								}

								InventorySlot slot = InventorySlot.mainWeaponSlots[inventoryNPC.getMainWeaponIndexToEquipTo(weapon)];
								if(weapon.isCanBeEquipped(inventoryNPC, slot)) {
									return new Response(UtilText.parse(inventoryNPC, "Equip Main ([npc.HerHim])"), UtilText.parse(inventoryNPC, "Make [npc.name] equip the "+weapon.getName()+" as [npc.her] main weapon."), INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"
												+ inventoryNPC.equipMainWeaponFromInventory(weapon, Main.game.getPlayer())
												+ "</p>");
											resetPostAction();
										}
									};
								} else {
									return new Response(UtilText.parse(inventoryNPC, "Equip Main ([npc.HerHim])"), weapon.getCannotBeEquippedText(inventoryNPC, slot), null);
								}


							} else if(index == 12) {
//								if(!weapon.getWeaponType().isAbleToBeDropped()) {
//									return new Response(UtilText.parse(inventoryNPC, "Equip Main ([npc.HerHim])"), "You cannot give away the " + weapon.getName() + "!", null);
//								}
								if(weapon.getWeaponType().isTwoHanded()) {
									return new Response(UtilText.parse(inventoryNPC, "Equip Offhand ([npc.HerHim])"),
											(weapon.getWeaponType().isPlural()
												?"As the " + weapon.getName() + " require two hands to wield, they can only be equipped in the main slot!"
												:"As the " + weapon.getName() + " is a two-handed weapon, it can only be equipped in the main slot!"),
											null);
								}
								InventorySlot slot = InventorySlot.offhandWeaponSlots[inventoryNPC.getOffhandWeaponIndexToEquipTo(weapon)];
								if(weapon.isCanBeEquipped(inventoryNPC, slot)) {
									return new Response(UtilText.parse(inventoryNPC, "Equip Offhand ([npc.HerHim])"), UtilText.parse(inventoryNPC, "Make [npc.name] equip the "+weapon.getName()+" as [npc.her] offhand weapon."), INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"
												+ inventoryNPC.equipOffhandWeaponFromInventory(weapon, Main.game.getPlayer())
												+ "</p>");
											resetPostAction();
										}
									};
								} else {
									return new Response(UtilText.parse(inventoryNPC, "Equip Offhand ([npc.HerHim])"), weapon.getCannotBeEquippedText(inventoryNPC, slot), null);
								}

							} else {
								return null;
							}

						case SEX:
							if(index == 1) {
								return new Response("Give (1)", "You can't give someone weapons while having sex with them!", null);

							} else if(index == 2) {
								return new Response("Give (5)", "You can't give someone weapons while having sex with them!", null);

							} else if(index == 3) {
								return new Response("Give (All)", "You can't give someone weapons while having sex with them!", null);

							} else if(index == 4) {
								return new Response("Dye", "You can't dye your weapons while having sex with someone!", null);

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant weapons while having sex with someone!", null);

							} else if(index == 6) {
								return new Response("Equip Main (Self)", "You can't equip weapons while having sex with someone!", null);

							} else if(index == 7) {
								return new Response("Equip Offhand (Self)", "You can't equip weapons while having sex with someone!", null);

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {
								return new Response(UtilText.parse(inventoryNPC, "Equip ([npc.HerHim])"), "You can't equip weapons while having sex with someone!", null);

							} else {
								return null;
							}

						case TRADING:
							if(index == 1) {
								if(!weapon.getWeaponType().isAbleToBeSold()) {
									return new Response("Sell (1)", "You cannot sell the " + weapon.getName() + "!", null);

								} else if (inventoryNPC.willBuy(weapon)) {
									int sellPrice = weapon.getPrice(inventoryNPC.getBuyModifier());
									return new Response("Sell (1) (" + UtilText.formatAsMoney(sellPrice, "span") + ")", "Sell the " + weapon.getName() + " for " + UtilText.formatAsMoney(sellPrice) + ".", INVENTORY_MENU){
										@Override
										public void effects(){
											sellWeapons(Main.game.getPlayer(), inventoryNPC, weapon, 1, sellPrice);
										}
									};
								} else {
									return new Response("Sell (1)", inventoryNPC.getName("The") + " doesn't want to buy this.", null);
								}

							} else if(index == 2) {
								if(Main.game.getPlayer().getWeaponCount(weapon) >= 5) {
									if(!weapon.getWeaponType().isAbleToBeSold()) {
										return new Response("Sell (5)", "You cannot sell the " + weapon.getName() + "!", null);

									} else if (inventoryNPC.willBuy(weapon)) {
										int sellPrice = weapon.getPrice(inventoryNPC.getBuyModifier());
										return new Response("Sell (5) (" + UtilText.formatAsMoney(sellPrice*5, "span") + ")", "Sell five of your " + weapon.getNamePlural() + " for " + UtilText.formatAsMoney(sellPrice*5) + ".", INVENTORY_MENU){
											@Override
											public void effects(){
												sellWeapons(Main.game.getPlayer(), inventoryNPC, weapon, 5, sellPrice);
											}
										};
									} else {
										return new Response("Sell (5)", inventoryNPC.getName("The") + " doesn't want to buy these.", null);
									}

								} else {
									return new Response("Sell (5)", "You don't have five " + weapon.getNamePlural() + " to sell!", null);
								}

							} else if(index == 3) {
								if(!weapon.getWeaponType().isAbleToBeSold()) {
									return new Response("Sell (All)", "You cannot sell the " + weapon.getName() + "!", null);

								} else if (inventoryNPC.willBuy(weapon)) {
									int sellPrice = weapon.getPrice(inventoryNPC.getBuyModifier());
									return new Response("Sell (All) (" + UtilText.formatAsMoney(sellPrice*Main.game.getPlayer().getWeaponCount(weapon), "span") + ")",
											"Sell the " + weapon.getName() + " for " + UtilText.formatAsMoney(sellPrice*Main.game.getPlayer().getWeaponCount(weapon)) + ".", INVENTORY_MENU){
										@Override
										public void effects(){
											sellWeapons(Main.game.getPlayer(), inventoryNPC, weapon, Main.game.getPlayer().getWeaponCount(weapon), sellPrice);
										}
									};
								} else {
									return new Response("Sell (All)", inventoryNPC.getName("The") + " doesn't want to buy these.", null);
								}

							} else if (index==4) {
								if (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH)
										|| Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER)
										|| Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
									boolean hasFullInventory = Main.game.getPlayer().isInventoryFull() && weapon.getRarity()!=Rarity.QUEST;
									boolean isDyeingStackItem = Main.game.getPlayer().getAllWeaponsInInventory().get(weapon) > 1;
									boolean canDye = !(isDyeingStackItem && hasFullInventory);
									if (canDye) {
										return new Response("Dye/Reforge",
												Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
													?"Use your proficiency with [style.colourEarth(Earth spells)] to dye or reforge this item."
													:"Use a dye-brush or reforge hammer to alter this weapon's properties.",
												DYE_WEAPON) {
											@Override
											public void effects() {
												resetWeaponDyeColours();
											}
										};
									} else {
										return new Response("Dye/Reforge", "Your inventory is full, so you can't alter this weapon's properties.", null);
									}
								} else {
									return new Response("Dye/Reforge", "You'll need to find a dye-brush or reforge hammer if you want to alter this weapon's properties.", null);
								}

							} else if(index == 5) {
								if(weapon.getEnchantmentItemType(null)==null || weapon.getItemTags().contains(ItemTag.UNENCHANTABLE)) {
									return new Response("Enchant", "This weapon cannot be enchanted!", null);

								} else if(Main.game.isDebugMode()
										|| (Main.game.getPlayer().hasQuest(QuestLine.SIDE_ENCHANTMENT_DISCOVERY) && Main.game.getPlayer().isQuestCompleted(QuestLine.SIDE_ENCHANTMENT_DISCOVERY))) {
									return new Response("Enchant", "Enchant this weapon.", EnchantmentDialogue.ENCHANTMENT_MENU) {
										@Override
										public DialogueNode getNextDialogue() {
											return EnchantmentDialogue.getEnchantmentMenu(weapon);
										}
									};

								} else {
									return new Response("Enchant", getEnchantmentNotDiscoveredText("weapons"), null);
								}

							} else if(index == 6) {
								InventorySlot slot = InventorySlot.mainWeaponSlots[Main.game.getPlayer().getMainWeaponIndexToEquipTo(weapon)];
								if(weapon.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									return new Response("Equip Main (Self)", "Equip the " + weapon.getName() + " as your main weapon.", INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append(
												"<p style='text-align:center;'>"
													+ Main.game.getPlayer().equipMainWeaponFromInventory(weapon, Main.game.getPlayer())
												+ "</p>");
											resetPostAction();
										}
									};
								} else {
									return new Response("Equip Main (Self)", weapon.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}

							} else if(index == 7) {
								if(weapon.getWeaponType().isTwoHanded()) {
									return new Response("Equip Offhand (Self)",
											(weapon.getWeaponType().isPlural()
												?"As the " + weapon.getName() + " require two hands to wield, they can only be equipped in the main slot!"
												:"As the " + weapon.getName() + " is a two-handed weapon, it can only be equipped in the main slot!"),
											null);
								}

								InventorySlot slot = InventorySlot.offhandWeaponSlots[Main.game.getPlayer().getOffhandWeaponIndexToEquipTo(weapon)];
								if(weapon.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									return new Response("Equip Offhand (Self)", "Equip the " + weapon.getName() + " as your offhand weapon.", INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append(
												"<p style='text-align:center;'>"
													+ Main.game.getPlayer().equipOffhandWeaponFromInventory(weapon, Main.game.getPlayer())
												+ "</p>");
											resetPostAction();
										}
									};
								} else {
									return new Response("Equip Offhand (Self)", weapon.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}

							} else if (index == 9) {
								return getBuybackResponse();

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {
								return new Response(UtilText.parse(inventoryNPC, "Equip Main ([npc.HerHim])"), UtilText.parse(inventoryNPC, "[npc.Name] doesn't want to use your weapons."), null);

							} else if(index == 12) {
								return new Response(UtilText.parse(inventoryNPC, "Equip Offhand ([npc.HerHim])"), UtilText.parse(inventoryNPC, "[npc.Name] doesn't want to use your weapons."), null);

							} else {
								return null;
							}
					}
				}

			// ****************************** ITEM DOES NOT BELONG TO PLAYER ******************************

			} else {
				// ****************************** Interacting with the ground ******************************
				if(inventoryNPC == null) {
					boolean inventoryFull = Main.game.getPlayer().isInventoryFull() && !Main.game.getPlayer().hasWeapon(weapon) && weapon.getRarity()!=Rarity.QUEST;

					switch(interactionType) {
						case SEX:
							if(index == 1) {
								return new Response("Take (1)", "You can't pick up weapons while masturbating.", null);

							} else if(index == 2) {
								return new Response("Take (5)", "You can't pick up weapons while masturbating.", null);

							} else if(index == 3) {
								return new Response("Take (All)", "You can't pick up weapons while masturbating.", null);

							} else if(index == 4) {
								return new Response("Dye", "You can't dye weapons while masturbating.", null);

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant weapons while masturbating.", null);

							} else if(index == 6) {
								return new Response("Equip Main (Self)", "You can't equip weapons while masturbating.", null);

							} else if(index == 7) {
								return new Response("Equip Offhand (Self)", "You can't equip weapons while masturbating.", null);

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else {
								return null;
							}

						default:
							if(index == 1) {
								if(inventoryFull) {
									return new Response("Take (1)", "Your inventory is already full!", null);
								}
								return new Response("Take (1)", "Take one " + weapon.getName() + " from the ground.", INVENTORY_MENU){
									@Override
									public void effects(){
										pickUpWeapons(Main.game.getPlayer(), weapon, 1);
									}
								};

							} else if(index == 2) {
								if(inventoryFull) {
									return new Response("Take (5)", "Your inventory is already full!", null);
								}
								if(Main.game.getPlayerCell().getInventory().getWeaponCount(weapon) >= 5) {
									return new Response("Take (5)", "Take five of the " + weapon.getNamePlural() + " from the ground.", INVENTORY_MENU){
										@Override
										public void effects(){
											pickUpWeapons(Main.game.getPlayer(), weapon, 5);
										}
									};
								} else {
									return new Response("Take (5)", "There aren't five " + weapon.getNamePlural() + " on the ground!", null);
								}

							} else if(index == 3) {
								if(inventoryFull) {
									return new Response("Take (All)", "Your inventory is already full!", null);
								}
								return new Response("Take (All)", "Take all of the " + weapon.getNamePlural() + " from the ground.", INVENTORY_MENU){
									@Override
									public void effects(){
										pickUpWeapons(Main.game.getPlayer(), weapon, Main.game.getPlayerCell().getInventory().getWeaponCount(weapon));
									}
								};

							} else if (index==4) {
								if (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH)
										|| Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER)
										|| Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
									boolean hasFullInventory = Main.game.getPlayerCell().getInventory().isInventoryFull() && weapon.getRarity()!=Rarity.QUEST;
									boolean isDyeingStackItem = Main.game.getPlayerCell().getInventory().getAllWeaponsInInventory().get(weapon) > 1;
									boolean canDye = !(isDyeingStackItem && hasFullInventory);
									if (canDye) {
										return new Response("Dye/Reforge",
												Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
													?"Use your proficiency with [style.colourEarth(Earth spells)] to dye or reforge this item."
													:"Use a dye-brush or reforge hammer to alter this weapon's properties.",
												DYE_WEAPON) {
											@Override
											public void effects() {
												resetWeaponDyeColours();
											}
										};
									} else {
										return new Response("Dye/Reforge", "Your inventory is full, so you can't alter this weapon's properties.", null);
									}
								} else {
									return new Response("Dye/Reforge", "You'll need to find a dye-brush or reforge hammer if you want to alter this weapon's properties.", null);
								}

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant weapons on the ground!", null);

							} else if(index == 6) {
								InventorySlot slot = InventorySlot.mainWeaponSlots[Main.game.getPlayer().getMainWeaponIndexToEquipTo(weapon)];
								if(weapon.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									return new Response("Equip Main (Self)", "Equip the " + weapon.getName() + " as your main weapon.", INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"
												+ Main.game.getPlayer().equipMainWeaponFromFloor(weapon)
												+ "</p>");
											resetPostAction();
										}
									};
								} else {
									return new Response("Equip Main (Self)", weapon.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}

							} else if(index == 7) {
								InventorySlot slot = InventorySlot.mainWeaponSlots[Main.game.getPlayer().getMainWeaponIndexToEquipTo(weapon)];
								if(!weapon.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									return new Response("Equip Offhand (Self)", weapon.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}

								if(weapon.getWeaponType().isTwoHanded()) {
									return new Response("Equip Offhand (Self)",
											(weapon.getWeaponType().isPlural()
												?"As the " + weapon.getName() + " require two hands to wield, they can only be equipped in the main slot!"
												:"As the " + weapon.getName() + " is a two-handed weapon, it can only be equipped in the main slot!"),
											null);
								}
								return new Response("Equip Offhand (Self)", "Equip the " + weapon.getName() + " as your offhand weapon.", INVENTORY_MENU){
									@Override
									public void effects(){
										Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"
											+ Main.game.getPlayer().equipOffhandWeaponFromFloor(weapon)
											+ "</p>");
										resetPostAction();
									}
								};

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else {
								return null;
							}
					}

				// ****************************** Interacting with an NPC ******************************
				} else {
					boolean inventoryFull = false;
					switch(interactionType) {
						case COMBAT:
							if(index == 1) {
								return new Response("Take (1)", "You can't take someone weapons while fighting them!", null);

							} else if(index == 2) {
								return new Response("Take (5)", "You can't take someone weapons while fighting them!", null);

							} else if(index == 3) {
								return new Response("Take (All)", "You can't take someone weapons while fighting them!", null);

							} else if(index == 4) {
								return new Response("Dye", "You can't dye someone's weapons while fighting them!", null);

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant someone else's weapons, especially not while fighting them!", null);

							} else if(index == 6) {
								return new Response("Equip Main (Self)", "You can't use someone else's weapons while fighting them!", null);

							} else if(index == 7) {
								return new Response("Equip Offhand (Self)", "You can't use someone else's weapons while fighting them!", null);

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {
								return new Response("Equip Main (Opponent)", "You can't use make someone use a weapon while fighting them!", null);

							} else if(index == 12) {
								return new Response("Equip Offhand (Opponent)", "You can't use make someone use a weapon while fighting them!", null);

							} else {
								return null;
							}

						case FULL_MANAGEMENT:  case CHARACTER_CREATION:
							inventoryFull = Main.game.getPlayer().isInventoryFull() && !Main.game.getPlayer().hasWeapon(weapon) && weapon.getRarity()!=Rarity.QUEST;

							if(index == 1) {
								if(inventoryFull) {
									return new Response("Take (1)", "Your inventory is already full!", null);
								}
								return new Response("Take (1)", UtilText.parse(inventoryNPC, "Take one " + weapon.getName() + " from [npc.name]."), INVENTORY_MENU){
									@Override
									public void effects(){
										transferWeapons(inventoryNPC, Main.game.getPlayer(), weapon, 1);
									}
								};

							} else if(index == 2) {
								if(inventoryFull) {
									return new Response("Take (5)", "Your inventory is already full!", null);
								}
								if(inventoryNPC.getWeaponCount(weapon) >= 5) {
									return new Response("Take (5)", UtilText.parse(inventoryNPC, "Take five of  [npc.namePos] " + weapon.getNamePlural() + "."), INVENTORY_MENU){
										@Override
										public void effects(){
											transferWeapons(inventoryNPC, Main.game.getPlayer(), weapon, 5);
										}
									};
								} else {
									return new Response("Take (5)", UtilText.parse(inventoryNPC, "[npc.Name] doesn't have five " + weapon.getNamePlural() + "!"), null);
								}

							} else if(index == 3) {
								if(inventoryFull) {
									return new Response("Take (All)", "Your inventory is already full!", null);
								}
								return new Response("Take (All)", UtilText.parse(inventoryNPC, "Take all "+Util.intToString(inventoryNPC.getWeaponCount(weapon))+" of  [npc.namePos] " + weapon.getNamePlural() + "."), INVENTORY_MENU){
									@Override
									public void effects(){
										transferWeapons(inventoryNPC, Main.game.getPlayer(), weapon, inventoryNPC.getWeaponCount(weapon));
									}
								};

							} else if (index==4) {
								if (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH)
										|| Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER)
										|| Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
									boolean hasFullInventory = inventoryNPC.isInventoryFull() && weapon.getRarity()!=Rarity.QUEST;
									boolean isDyeingStackItem = inventoryNPC.getAllWeaponsInInventory().get(weapon) > 1;
									boolean canDye = !(isDyeingStackItem && hasFullInventory);
									if (canDye) {
										return new Response("Dye/Reforge",
												Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
													?"Use your proficiency with [style.colourEarth(Earth spells)] to dye or reforge this item."
													:"Use a dye-brush or reforge hammer to alter this weapon's properties.",
												DYE_WEAPON) {
											@Override
											public void effects() {
												resetWeaponDyeColours();
											}
										};
									} else {
										return new Response("Dye/Reforge", UtilText.parse(inventoryNPC, "[npc.NamePos] inventory is full, so you can't alter this weapon's properties."), null);
									}
								} else {
									return new Response("Dye/Reforge", UtilText.parse(inventoryNPC, "You'll need to find a dye-brush or reforge hammer if you want to alter the properties of [npc.namePos] weapons."), null);
								}

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant weapons owned by someone else!", null);

							} else if(index == 6) {
								InventorySlot slot = InventorySlot.mainWeaponSlots[Main.game.getPlayer().getMainWeaponIndexToEquipTo(weapon)];
								if(weapon.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									return new Response("Equip Main (Self)", "Equip the " + weapon.getName() + " as your main weapon.", INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"
												+ Main.game.getPlayer().equipMainWeaponFromInventory(weapon, inventoryNPC)
												+ "</p>");
											resetPostAction();
										}
									};
								} else {
									return new Response("Equip Main (Self)", weapon.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}

							} else if(index == 7) {
								if(weapon.getWeaponType().isTwoHanded()) {
									return new Response("Equip Offhand (Self)",
											(weapon.getWeaponType().isPlural()
												?"As the " + weapon.getName() + " require two hands to wield, they can only be equipped in the main slot!"
												:"As the " + weapon.getName() + " is a two-handed weapon, it can only be equipped in the main slot!"),
											null);
								}

								InventorySlot slot = InventorySlot.offhandWeaponSlots[Main.game.getPlayer().getOffhandWeaponIndexToEquipTo(weapon)];
								if(weapon.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									return new Response("Equip Offhand (Self)", "Equip the " + weapon.getName() + " as your offhand weapon.", INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"
												+ Main.game.getPlayer().equipOffhandWeaponFromInventory(weapon, inventoryNPC)
												+ "</p>");
											resetPostAction();
										}
									};
								} else {
									return new Response("Equip Offhand (Self)", weapon.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {
								InventorySlot slot = InventorySlot.mainWeaponSlots[inventoryNPC.getMainWeaponIndexToEquipTo(weapon)];
								if(weapon.isCanBeEquipped(inventoryNPC, slot)) {
									return new Response(UtilText.parse(inventoryNPC, "Equip Main ([npc.HerHim])"), UtilText.parse(inventoryNPC, "Get [npc.name] to equip the " + weapon.getName() + " as [npc.her] main weapon."), INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"
												+ inventoryNPC.equipMainWeaponFromInventory(weapon, inventoryNPC)
												+ "</p>");
											resetPostAction();
										}
									};
								} else {
									return new Response(UtilText.parse(inventoryNPC, "Equip Main ([npc.HerHim])"), weapon.getCannotBeEquippedText(inventoryNPC, slot), null);
								}

							} else if(index == 12) {
								if(weapon.getWeaponType().isTwoHanded()) {
									return new Response(UtilText.parse(inventoryNPC, "Equip Offhand ([npc.HerHim])"),
											(weapon.getWeaponType().isPlural()
												?"As the " + weapon.getName() + " require two hands to wield, they can only be equipped in the main slot!"
												:"As the " + weapon.getName() + " is a two-handed weapon, it can only be equipped in the main slot!"),
											null);
								}

								InventorySlot slot = InventorySlot.offhandWeaponSlots[Main.game.getPlayer().getOffhandWeaponIndexToEquipTo(weapon)];
								if(weapon.isCanBeEquipped(inventoryNPC, slot)) {
									return new Response(UtilText.parse(inventoryNPC, "Equip Offhand ([npc.HerHim])"), UtilText.parse(inventoryNPC, "Get [npc.name] to equip the " + weapon.getName() + " as [npc.her] offhand weapon."), INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>"
												+ inventoryNPC.equipOffhandWeaponFromInventory(weapon, inventoryNPC)
												+ "</p>");
											resetPostAction();
										}
									};
								} else {
									return new Response(UtilText.parse(inventoryNPC, "Equip Offhand ([npc.HerHim])"), weapon.getCannotBeEquippedText(inventoryNPC, slot), null);
								}

							} else {
								return null;
							}

						case SEX:
							if(index == 1) {
								return new Response("Take (1)", "You can't take someone's weapons while having sex with them!", null);

							} else if(index == 2) {
								return new Response("Take (5)", "You can't take someone's weapons while having sex with them!", null);

							} else if(index == 3) {
								return new Response("Take (All)", "You can't take someone's weapons while having sex with them!", null);

							} else if(index == 4) {
								return new Response("Dye", "You can't dye someone's weapons while having sex with them!", null);

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant someone else's weapons, especially not while having sex with them!", null);

							} else if(index == 6) {
								return new Response("Equip Main (Self)", "You can't use someone else's weapons while having sex with them!", null);

							} else if(index == 7) {
								return new Response("Equip Offhand (Self)", "You can't use someone else's weapons while having sex with them!", null);

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {
								return new Response("Equip Main (Opponent)", "You can't use make someone use a weapon while having sex with them!", null);

							} else if(index == 12) {
								return new Response("Equip Offhand (Opponent)", "You can't use make someone use a weapon while having sex with them!", null);

							} else {
								return null;
							}

						case TRADING:
							inventoryFull = Main.game.getPlayer().isInventoryFull() && !Main.game.getPlayer().hasWeapon(weapon) && weapon.getRarity()!=Rarity.QUEST;

							if(index == 1) {
								int sellPrice = buyback?Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getPrice():weapon.getPrice(inventoryNPC.getSellModifier(weapon));
								if(inventoryFull) {
									return new Response("Buy (1) ("+UtilText.formatAsMoneyUncoloured(sellPrice, "span")+")", "Your inventory is already full!", null);
								}
								if(Main.game.getPlayer().getMoney() < sellPrice) {
									return new Response("Buy (1) ("+UtilText.formatAsMoneyUncoloured(sellPrice, "span")+")", "You can't afford to buy this!", null);
								}
								return new Response("Buy (1) (" + UtilText.formatAsMoney(sellPrice, "span") + ")", "Buy the " + weapon.getName() + " for " + UtilText.formatAsMoney(sellPrice) + ".", INVENTORY_MENU){
									@Override
									public void effects(){
										sellWeapons(inventoryNPC, Main.game.getPlayer(), weapon, 1, sellPrice);
									}
								};

							} else if(index == 2) {
								int sellPrice = buyback?Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getPrice():weapon.getPrice(inventoryNPC.getSellModifier(weapon));
								if((buyback && Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getCount()<5)
										|| (!buyback && inventoryNPC.getWeaponCount(weapon) < 5)) {
									return new Response("Buy (5) ("+UtilText.formatAsMoneyUncoloured(sellPrice*5, "span")+")", UtilText.parse(inventoryNPC, "[npc.Name] doesn't have five "+weapon.getNamePlural()+"."), null);
								}
								if(inventoryFull) {
									return new Response("Buy (5) ("+UtilText.formatAsMoneyUncoloured(sellPrice*5, "span")+")", "Your inventory is already full!", null);
								}
								if(Main.game.getPlayer().getMoney() < sellPrice*5) {
									return new Response("Buy (5) ("+UtilText.formatAsMoneyUncoloured(sellPrice*5, "span")+")", "You can't afford to buy this!", null);
								}
								return new Response("Buy (5) (" + UtilText.formatAsMoney(sellPrice*5, "span") + ")", "Buy the " + weapon.getName() + " for " + UtilText.formatAsMoney(sellPrice*5) + ".", INVENTORY_MENU){
									@Override
									public void effects(){
										sellWeapons(inventoryNPC, Main.game.getPlayer(), weapon, 5, sellPrice);
									}
								};

							} else if(index == 3) {
								int sellPrice = buyback?Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getPrice():weapon.getPrice(inventoryNPC.getSellModifier(weapon));
								int count = buyback?Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getCount():inventoryNPC.getWeaponCount(weapon);
								if(inventoryFull) {
									return new Response("Buy (All) ("+UtilText.formatAsMoneyUncoloured(sellPrice*count, "span")+")", "Your inventory is already full!", null);
								}
								if(Main.game.getPlayer().getMoney() < sellPrice*count) {
									int affordableCount = Main.game.getPlayer().getMoney() / sellPrice;
									if(affordableCount > 0) {
										return new Response("Buy (Max " + affordableCount + ") (" + UtilText.formatAsMoney(sellPrice * affordableCount, "span") + ")",
												"Buy the " + weapon.getName() + " for " + UtilText.formatAsMoney(sellPrice * affordableCount) + ".", INVENTORY_MENU) {
											@Override
											public void effects() {
												sellWeapons(inventoryNPC, Main.game.getPlayer(), weapon, affordableCount, sellPrice);
											}
										};
									} else {
										return new Response("Buy (All) ("+UtilText.formatAsMoneyUncoloured(sellPrice*count, "span")+")", "You can't afford to buy this!", null);
									}
								}
								return new Response("Buy (All) (" + UtilText.formatAsMoney(sellPrice*count, "span") + ")",
										"Buy the " + weapon.getName() + " for " + UtilText.formatAsMoney(sellPrice*count) + ".", INVENTORY_MENU){
									@Override
									public void effects(){
										sellWeapons(inventoryNPC, Main.game.getPlayer(), weapon, count, sellPrice);
									}
								};

							} else if(index == 4) {
								return new Response("Dye", UtilText.parse(inventoryNPC, "[npc.Name] isn't going to let you dye the weapon that [npc.sheIs] trying to sell!"), null);

							} else if(index == 5) {
								return new Response("Enchant", "You can't enchant someone else's weapon!", null);

							} else if(index == 6) {
								return new Response("Equip Main (Self)", UtilText.parse(inventoryNPC, "[npc.Name] isn't going to let you equip [npc.her] weapons without buying them first."), null);

							} else if(index == 7) {
								return new Response("Equip Offhand (Self)", UtilText.parse(inventoryNPC, "[npc.Name] isn't going to let you equip [npc.her] weapons without buying them first."), null);

							} else if (index == 9) {
								return getBuybackResponse();

							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index == 11) {
								return new Response(UtilText.parse(inventoryNPC, "Equip Main ([npc.HerHim])"), UtilText.parse(inventoryNPC, "[npc.Name] isn't going to equip the weapons that [npc.sheIs] trying to sell!"), null);

							} else if(index == 12) {
								return new Response(UtilText.parse(inventoryNPC, "Equip Offhand ([npc.HerHim])"), UtilText.parse(inventoryNPC, "[npc.Name] isn't going to equip the weapons that [npc.sheIs] trying to sell!"), null);

							} else {
								return null;
							}
					}
				}
			}
			return null;
		}

		@Override
		public DialogueNodeType getDialogueNodeType() {
			return DialogueNodeType.INVENTORY;
		}
	};

	public static final DialogueNode CLOTHING_INVENTORY = new DialogueNode("Clothing", "", true) {

		@Override
		public String getLabel() {
			if(!Main.game.isInNewWorld()) {
				return "Evening's Attire";
			}

			if (Main.game.getDialogueFlags().values.contains(DialogueFlagValue.quickTrade) && !Main.game.isInSex() && !Main.game.isInCombat()) {
				return "Inventory (Quick-Manage is <b style='color:" + PresetColour.GENERIC_GOOD.toWebHexString() + ";'>ON</b>)";
			} else {
				return "Inventory";
			}
		}

		@Override
		public String getHeaderContent() {
			return inventoryView();
		}

		@Override
		public String getContent() {
			StringBuilder sb = new StringBuilder();
			sb.append(clothing.getDescription());
			sb.append("<p>");
				for(String s : clothing.getExtraDescriptions(null, null, true)) {
					sb.append(s+"<br/>");
				}
				for(InventorySlot is : clothing.getClothingType().getEquipSlots()) {
					List<String> descriptions = clothing.getExtraDescriptions(null, is, true);
					if(!descriptions.isEmpty()) {
						sb.append("<i>When equipped into the '"+is.getName()+"' slot:</i><br/>");
						for(String s : clothing.getExtraDescriptions(null, is, true)) {
							sb.append(s+"<br/>");
						}
					}
				}
			sb.append("</p>");
			sb.append((owner!=null && owner.isPlayer()
							? (inventoryNPC != null && interactionType == InventoryInteraction.TRADING
							? "<p>"
								+(inventoryNPC.willBuy(clothing)
									? inventoryNPC.getName("The") + " will buy it for " + UtilText.formatAsMoney(clothing.getPrice(inventoryNPC.getBuyModifier())) + "."
									: inventoryNPC.getName("The") + " doesn't want to buy this.")
								+"</p>"
							: "")
					:(inventoryNPC != null && interactionType == InventoryInteraction.TRADING
						? "<p>"
								+ inventoryNPC.getName("The") + " will sell it for " + UtilText.formatAsMoney(clothing.getPrice(inventoryNPC.getSellModifier(clothing))) + "."
							+ "</p>"
						: "")));


			return getItemDisplayPanel(clothing.getSVGString(), clothing.getDisplayName(true), sb.toString())
					+(interactionType==InventoryInteraction.CHARACTER_CREATION?CharacterCreation.getCheckingClothingDescription():"");
		}

		public String getResponseTabTitle(int index) {
			return getGeneralResponseTabTitle(index);
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if (index == 0) {
				return getCloseInventoryResponse();
			}
			if(responseTab==0) {
				return INVENTORY_MENU.getResponse(responseTab, index);
			}
			
			// ****************************** ITEM BELONGS TO THE PLAYER ******************************
			if(owner != null && owner.isPlayer()) {
				
				// ****************************** Interacting with the ground ******************************
				if(inventoryNPC == null) {
					boolean areaFull = Main.game.isPlayerTileFull() && !Main.game.getPlayerCell().getInventory().hasClothing(clothing);

					switch(interactionType) {
						case SEX:
							String dropTitle = owner.getLocationPlace().isItemsDisappear()?"Drop ":"Store";
							if(index == 1) {
								return new Response(dropTitle+"(1)", "You can't drop clothing while masturbating.", null);
								
							} else if(index == 2) {
								return new Response(dropTitle+"(5)", "You can't drop clothing while masturbating.", null);
								
							} else if(index == 3) {
								return new Response(dropTitle+"(All)", "You can't drop clothing while masturbating.", null);
								
							} else if(index == 4) {
								return new Response("Dye", "You can't dye your clothing while masturbating.", null);
								
							} else if(index == 5) {
								if(clothing.isCondom()) {
									if(clothing.getCondomEffect().getPotency().isNegative()) {
										return new Response("Repair (<i>1 Essence</i>)", "You can't repair the condom while masturbating.", null);
									}
									return new Response("Sabotage", "You can't sabotage the condom while masturbating.", null);
								}
								return new Response("Enchant", "You can't enchant clothing while masturbating.", null);

							} else if(index >= 6 && index <= 9 && index-6<clothing.getClothingType().getEquipSlots().size()) {
								InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-6);
								if(clothing.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									if(clothing.isAbleToBeEquippedDuringSex(slot).getKey()) {
										if(!Main.sex.getInitialSexManager().isAbleToEquipSexClothing(Main.game.getPlayer(), Main.game.getPlayer(), clothing)) {
											return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "As this is a special sex scene, you cannot equip clothing during it!", null);
										}
										if(!Main.sex.isClothingEquipAvailable(Main.game.getPlayer(), slot, clothing)) {
											return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "This slot is involved with an ongoing sex action, so you cannot equip clothing into it!", null);
										}
										if(Main.game.getPlayer().isAbleToEquip(clothing, slot, false, Main.game.getPlayer())) {
											return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "Equip the " + clothing.getName() + ".", Main.sex.SEX_DIALOGUE){
												@Override
												public void effects(){
													AbstractClothing c = clothing;
													equipClothingFromInventory(Main.game.getPlayer(), slot, Main.game.getPlayer(), clothing);
													Main.sex.setEquipClothingText(c, Main.game.getPlayer().getUnequipDescription());
													Main.mainController.openInventory();
													Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
													Main.sex.setSexStarted(true);
												}
											};
										} else {
											return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), getClothingBlockingRemovalText("equip"), null);
										}
										
									} else {
										return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), clothing.isAbleToBeEquippedDuringSex(slot).getValue(), null);
									}
									
								} else {
									return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), clothing.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}
								
							} else if (index == 10) {
								return getQuickTradeResponse();

							} else {
								return null;
							}
					default:
							if(index == 1) {
								if(owner.getLocationPlace().isItemsDisappear()) {
									if(!clothing.getClothingType().isAbleToBeDropped()) {
										return new Response("Drop (1)", "You cannot drop the " + clothing.getName() + "!", null);
										
									} else if(areaFull) {
										return new Response("Drop (1)", "This area is full, so you can't drop your " + clothing.getName() + " here!", null);
									} else {
										return new Response("Drop (1)", "Drop your " + clothing.getName() + ".", INVENTORY_MENU){
											@Override
											public void effects(){
												dropClothing(owner, clothing, 1);
											}
										};
									}
								} else {
									if(!clothing.getClothingType().isAbleToBeDropped()) {
										return new Response("Store (1)", "You cannot drop the " + clothing.getName() + "!", null);
										
									} else if(areaFull) {
										return new Response("Store (1)", "This area is full, so you can't store your " + clothing.getName() + " here!", null);
									} else {
										return new Response("Store (1)", "Store the " + clothing.getName() + " in this area.", INVENTORY_MENU){
											@Override
											public void effects(){
												dropClothing(owner, clothing, 1);
											}
										};
									}
								}
								
							} else if(index == 2) {
								if(owner.getLocationPlace().isItemsDisappear()) {
									if(!clothing.getClothingType().isAbleToBeDropped()) {
										return new Response("Drop (5)", "You cannot drop the " + clothing.getName() + "!", null);
										
									} else if(owner.getClothingCount(clothing) < 5) {
										return new Response("Drop (5)", "You don't have five " + clothing.getNamePlural() + " to give!", null);
										
									} else if(areaFull) {
										return new Response("Drop (5)", "This area is full, so you can't drop your " + clothing.getNamePlural() + " here!", null);
										
									} else {
										return new Response("Drop (5)", "Drop five of your " + clothing.getNamePlural() + ".", INVENTORY_MENU){
											@Override
											public void effects(){
												dropClothing(owner, clothing, 5);
											}
										};
									}
								} else {
									if(!clothing.getClothingType().isAbleToBeDropped()) {
										return new Response("Store (5)", "You cannot drop the " + clothing.getName() + "!", null);
										
									} else if(owner.getClothingCount(clothing) < 5) {
										return new Response("Store (5)", "You don't have five " + clothing.getNamePlural() + " to give!", null);
										
									} else if(areaFull) {
										return new Response("Store (5)", "This area is full, so you can't store your " + clothing.getNamePlural() + " here!", null);
										
									} else {
										return new Response("Store (5)", "Store five of your " + clothing.getNamePlural() + " in this area.", INVENTORY_MENU){
											@Override
											public void effects(){
												dropClothing(owner, clothing, 5);
											}
										};
									}
								}
								
							} else if(index == 3) {
								if(owner.getLocationPlace().isItemsDisappear()) {
									if(!clothing.getClothingType().isAbleToBeDropped()) {
										return new Response("Drop (All)", "You cannot drop the " + clothing.getName() + "!", null);
										
									} else if(areaFull) {
										return new Response("Drop (All)", "This area is full, so you can't drop your " + clothing.getNamePlural() + " here!", null);
									} else {
										return new Response("Drop (All)", "Drop all of your " + clothing.getNamePlural() + ".", INVENTORY_MENU){
											@Override
											public void effects(){
												dropClothing(owner, clothing, owner.getClothingCount(clothing));
											}
										};
									}
								} else {
									if(!clothing.getClothingType().isAbleToBeDropped()) {
										return new Response("Store (All)", "You cannot drop the " + clothing.getName() + "!", null);
										
									} else if(areaFull) {
										return new Response("Store (All)", "This area is full, so you can't store your " + clothing.getNamePlural() + " here!", null);
									} else {
										return new Response("Store (All)", "Store all of your " + clothing.getNamePlural() + " in this area.", INVENTORY_MENU){
											@Override
											public void effects(){
												dropClothing(owner, clothing, owner.getClothingCount(clothing));
											}
										};
									}
								}
								
							} else if (index==4) {
								if (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
									boolean hasFullInventory = Main.game.getPlayer().isInventoryFull() && clothing.getRarity()!=Rarity.QUEST;
									boolean isDyeingStackItem = Main.game.getPlayer().getAllClothingInInventory().get(clothing) > 1;
									boolean canDye = !(isDyeingStackItem && hasFullInventory);
									if (canDye) {
										return new Response("Dye",
												Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
													?"Use your proficiency with [style.colourEarth(Earth spells)] to dye this item of clothing."
													:"Use a dye-brush to dye this item of clothing.",
												DYE_CLOTHING) {
											@Override
											public void effects() {
												resetClothingDyeColours();
											}
										};
									} else {
										return new Response("Dye", "Your inventory is full, so you can't dye this item of clothing.", null);
									}
								} else {
									return new Response("Dye", "You'll need to find a dye-brush if you want to dye your clothes.", null);
								}
								
							} else if(index == 5) {
								if(clothing.isCondom()) {
									return getCondomSabotageResponse(clothing);
								}
								if(Main.game.isDebugMode()
										|| (Main.game.getPlayer().hasQuest(QuestLine.SIDE_ENCHANTMENT_DISCOVERY) && Main.game.getPlayer().isQuestCompleted(QuestLine.SIDE_ENCHANTMENT_DISCOVERY))) {
									if(clothing.getEnchantmentItemType(null)==null || clothing.getItemTags().contains(ItemTag.UNENCHANTABLE)) {
										return new Response("Enchant", "This clothing cannot be enchanted!", null);
										
									} else if(!clothing.isEnchantmentKnown()) {
										if(Main.game.getPlayer().getEssenceCount() >= IDENTIFICATION_ESSENCE_PRICE) {
											return new Response("Identify ([style.italicsArcane("+IDENTIFICATION_ESSENCE_PRICE+" Essences)])",
													"To identify the "+clothing.getName()+", you can either spend "+IDENTIFICATION_ESSENCE_PRICE+" arcane essences to do it yourself,"
															+ " or go to a vendor and pay "+IDENTIFICATION_PRICE+" flames to have them do it for you.",
													CLOTHING_INVENTORY) {
												@Override
												public void effects() {
													Main.game.getPlayer().incrementEssenceCount(-IDENTIFICATION_ESSENCE_PRICE, false);
													
													String enchantmentRemovedString = clothing.setEnchantmentKnown(owner, true);
													
													clothing = AbstractClothing.enchantmentRemovedClothing;
													
													Main.game.getTextEndStringBuilder().append(
															"<p>"
																+ "You channel the power of "+Util.intToString(IDENTIFICATION_ESSENCE_PRICE)+" of your arcane essences into the "+clothing.getName()
																	+", and as it emits a faint purple glow, you find yourself able to detect what sort of enchantment it has!"
															+ "</p>"
															+ enchantmentRemovedString
															+ "<p style='text-align:center;'>"
																+ "Identifying the "+clothing.getName()+" has cost you [style.boldBad("+Util.intToString(IDENTIFICATION_ESSENCE_PRICE)+")] [style.boldArcane(Arcane Essences)]!"
															+ "</p>");
													RenderingEngine.setPage(Main.game.getPlayer(), clothing);
												}
											};
										} else {
											return new Response("Identify (<i>"+IDENTIFICATION_ESSENCE_PRICE+" Essences</i>)",
													"To identify the "+clothing.getName()+", you can either spend "+IDENTIFICATION_ESSENCE_PRICE+" arcane essences to do it yourself ([style.italicsBad(which you don't have)]),"
															+ " or go to a vendor and pay "+IDENTIFICATION_PRICE+" flames to have them do it for you.", null);
										}
									}
									return new Response("Enchant", "Enchant this clothing.", EnchantmentDialogue.ENCHANTMENT_MENU) {
										@Override
										public DialogueNode getNextDialogue() {
											return EnchantmentDialogue.getEnchantmentMenu(clothing);
										}
									};
									
								} else {
									return new Response("Enchant", getEnchantmentNotDiscoveredText("clothing"), null);
								}
								
							} else if(index >= 6 && index <= 9 && index-6<clothing.getClothingType().getEquipSlots().size()) {
								InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-6);
								if(clothing.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "Equip the " + clothing.getName() + ".", INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + equipClothingFromInventory(Main.game.getPlayer(), slot, Main.game.getPlayer(), clothing) + "</p>");
										}
									};
								} else {
									return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), clothing.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}
								
							} else if (index == 10) {
								return getQuickTradeResponse();
								
							} else {
								return null;
							}
					}
					
				// ****************************** Interacting with an NPC ******************************
				} else {
					switch(interactionType) {
						case COMBAT:
							if(index == 1) {
								return new Response("Give (1)", "You can't give someone clothing while fighting them!", null);
								
							} else if(index == 2) {
								return new Response("Give (5)", "You can't give someone clothing while fighting them!", null);
								
							} else if(index == 3) {
								return new Response("Give (All)", "You can't give someone clothing while fighting them!", null);
								
							} else if(index == 4) {
								return new Response("Dye", "You can't dye your clothing while fighting someone!", null);
								
							} else if(index == 5) {
								if(clothing.isCondom()) {
									if(clothing.getCondomEffect().getPotency().isNegative()) {
										return new Response("Repair (<i>1 Essence</i>)", "You can't repair the condom while fighting someone!", null);
									}
									return new Response("Sabotage", "You can't sabotage the condom while fighting someone!", null);
								}
								return new Response("Enchant", "You can't enchant clothing while fighting someone!", null);
								
							} else if(index >= 6 && index <= 9 && index-6<clothing.getClothingType().getEquipSlots().size()) {
								InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-6);
								return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "You cannot change your clothes while fighting someone!", null);
								
							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index >= 11 && index <= 14 && index-11<clothing.getClothingType().getEquipSlots().size()) {
								InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-11);
								return new Response("Equip: "+Util.capitaliseSentence(slot.getName())+" (Opponent)", "You can't make your opponent equip clothing while fighting them!", null);
								
							} else {
								return null;
							}
							
						case FULL_MANAGEMENT: case CHARACTER_CREATION:
							boolean inventoryFull = inventoryNPC.isInventoryFull() && !inventoryNPC.hasClothing(clothing);
							
							if(index == 1) {
								if(!clothing.getClothingType().isAbleToBeDropped()) {
									return new Response("Give (1)", "You cannot give away the " + clothing.getName() + "!", null);
									
								} else if(inventoryFull) {
									return new Response("Give (1)", UtilText.parse(inventoryNPC, "[npc.NamePos] inventory is already full!"), null);
								}
								return new Response("Give (1)", UtilText.parse(inventoryNPC, "Give [npc.name] the " + clothing.getName() + "."), INVENTORY_MENU){
									@Override
									public void effects(){
										transferClothing(Main.game.getPlayer(), inventoryNPC, clothing, 1);
									}
								};
								
							} else if(index == 2) {
								if(!clothing.getClothingType().isAbleToBeDropped()) {
									return new Response("Give (5)", "You cannot give away the " + clothing.getName() + "!", null);
									
								} else if(inventoryFull) {
									return new Response("Give (5)", UtilText.parse(inventoryNPC, "[npc.NamePos] inventory is already full!"), null);
								}
								if(Main.game.getPlayer().getClothingCount(clothing) >= 5) {
									return new Response("Give (5)", UtilText.parse(inventoryNPC, "Give [npc.name] five of your " + clothing.getNamePlural() + "."), INVENTORY_MENU){
										@Override
										public void effects(){
											transferClothing(Main.game.getPlayer(), inventoryNPC, clothing, 5);
										}
									};
								} else {
									return new Response("Give (5)", "You don't have five " + clothing.getNamePlural() + " to give!", null);
								}
								
							} else if(index == 3) {
								if(!clothing.getClothingType().isAbleToBeDropped()) {
									return new Response("Give (All)", "You cannot give away the " + clothing.getName() + "!", null);
									
								} else if(inventoryFull) {
									return new Response("Give (All)", UtilText.parse(inventoryNPC, "[npc.NamePos] inventory is already full!"), null);
								}
								return new Response("Give (All)", UtilText.parse(inventoryNPC, "Give [npc.name] all of your " + clothing.getNamePlural() + "."), INVENTORY_MENU){
									@Override
									public void effects(){
										transferClothing(Main.game.getPlayer(), inventoryNPC, clothing, Main.game.getPlayer().getClothingCount(clothing));
									}
								};
								
							} else if (index==4) {
								if (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
									boolean hasFullInventory = Main.game.getPlayer().isInventoryFull();
									boolean isDyeingStackItem = Main.game.getPlayer().getAllClothingInInventory().get(clothing) > 1;
									boolean canDye = !(isDyeingStackItem && hasFullInventory);
									if (canDye) {
										return new Response("Dye",
												Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
													?"Use your proficiency with [style.colourEarth(Earth spells)] to dye this item."
													:"Use a dye-brush to dye this item of clothing.",
												DYE_CLOTHING) {
											@Override
											public void effects() {
												resetClothingDyeColours();
											}
										};
									} else {
										return new Response("Dye", "Your inventory is full, so you can't dye this item of clothing.", null);
									}
								} else {
									return new Response("Dye", "You'll need to find a dye-brush if you want to dye your clothes.", null);
								}
								
							} else if(index == 5) {
								if(clothing.isCondom()) {
									return getCondomSabotageResponse(clothing);
								}
								if(Main.game.isDebugMode()
										|| (Main.game.getPlayer().hasQuest(QuestLine.SIDE_ENCHANTMENT_DISCOVERY) && Main.game.getPlayer().isQuestCompleted(QuestLine.SIDE_ENCHANTMENT_DISCOVERY))) {
									if(clothing.getEnchantmentItemType(null)==null || clothing.getItemTags().contains(ItemTag.UNENCHANTABLE)) {
										return new Response("Enchant", "This clothing cannot be enchanted!", null);
										
									} else if(!clothing.isEnchantmentKnown()) {
										if(Main.game.getPlayer().getEssenceCount() >= IDENTIFICATION_ESSENCE_PRICE) {
											return new Response("Identify ([style.italicsArcane("+IDENTIFICATION_ESSENCE_PRICE+" Essences)])",
													"To identify the "+clothing.getName()+", you can either spend "+IDENTIFICATION_ESSENCE_PRICE+" arcane essences to do it yourself,"
															+ " or go to a vendor and pay "+IDENTIFICATION_PRICE+" flames to have them do it for you.",
													CLOTHING_INVENTORY) {
												@Override
												public void effects() {
													Main.game.getPlayer().incrementEssenceCount(-IDENTIFICATION_ESSENCE_PRICE, false);

													String enchantmentRemovedString = clothing.setEnchantmentKnown(owner, true);
													
													clothing = AbstractClothing.enchantmentRemovedClothing;
													
													Main.game.getTextEndStringBuilder().append(
															"<p>"
																+ "You channel the power of "+Util.intToString(IDENTIFICATION_ESSENCE_PRICE)+" of your arcane essences into the "+clothing.getName()
																	+", and as it emits a faint purple glow, you find yourself able to detect what sort of enchantment it has!"
															+ "</p>"
															+ enchantmentRemovedString
															+ "<p style='text-align:center;'>"
																+ "Identifying the "+clothing.getName()+" has cost you [style.boldBad("+Util.intToString(IDENTIFICATION_ESSENCE_PRICE)+")] [style.boldArcane(Arcane Essences)]!"
															+ "</p>");
													RenderingEngine.setPage(Main.game.getPlayer(), clothing);
												}
											};
										} else {
											return new Response("Identify (<i>"+IDENTIFICATION_ESSENCE_PRICE+" Essences</i>)",
													"To identify the "+clothing.getName()+", you can either spend "+IDENTIFICATION_ESSENCE_PRICE+" arcane essences to do it yourself ([style.italicsBad(which you don't have)]),"
															+ " or go to a vendor and pay "+IDENTIFICATION_PRICE+" flames to have them do it for you.", null);
										}
									}
									return new Response("Enchant", "Enchant this clothing.", EnchantmentDialogue.ENCHANTMENT_MENU) {
										@Override
										public DialogueNode getNextDialogue() {
											return EnchantmentDialogue.getEnchantmentMenu(clothing);
										}
									};
									
								} else {
									return new Response("Enchant", getEnchantmentNotDiscoveredText("clothing"), null);
								}
								
							} else if(index >= 6 && index <= 9 && index-6<clothing.getClothingType().getEquipSlots().size()) {
								InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-6);
								if(clothing.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "Equip the " + clothing.getName() + ".", INVENTORY_MENU) {
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + equipClothingFromInventory(Main.game.getPlayer(), slot, Main.game.getPlayer(), clothing) + "</p>");
										}
									};
								} else {
									return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), clothing.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}
									
							} else if (index == 10) {
								return getQuickTradeResponse();
								
							} else if(index >= 11 && index <= 14 && index-11<clothing.getClothingType().getEquipSlots().size()) {
								InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-11);
//								if(!clothing.getClothingType().isAbleToBeDropped()) {
//									return new Response(
//											UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
//											"You cannot give away the " + clothing.getName() + "!",
//											null);
//								}
								Value<Boolean, String> equipAllowed = inventoryNPC.isInventoryEquipAllowed(clothing, slot);
								if(!equipAllowed.getKey()) {
									return new Response(
											UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
											UtilText.parse(inventoryNPC, equipAllowed.getValue()),
											null);
								}
								if(clothing.isCanBeEquipped(inventoryNPC, slot)) {
									if(inventoryNPC.isAbleToEquip(clothing, slot, true, Main.game.getPlayer()) && clothing.isEnslavementClothing() && (!inventoryNPC.isSlave() || !inventoryNPC.getOwner().isPlayer())) {
										boolean willEnslave = !inventoryNPC.isSlave() && inventoryNPC.isAbleToBeEnslaved() && Main.game.getPlayer().isHasSlaverLicense();
										return new Response(
												UtilText.parse(inventoryNPC,
														!willEnslave
															?"[style.colourMinorBad(Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim]))]"
															:"[style.colourArcane(Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim]))]"),
												UtilText.parse(inventoryNPC,
														"Make [npc.name] equip the "+clothing.getName()+"."
														+(!willEnslave
															?"<br/><i>Despite the fact that the "+clothing.getName()+" "
																	+(clothing.getClothingType().isPlural()?"have an enslavement enchantment, they":"has an enslavement enchantment, it")
																	+" [style.colourMinorBad(will not enslave [npc.name])], as"
																	+ (Main.game.getPlayer().isHasSlaverLicense()
																			?" [npc.sheIsFull] not a suitable target for enslavement"
																			:" you do not possess a slaver license")
																	+"!</i>"
															:"<br/><i>Thanks to the fact that the "+clothing.getName()+" "
																	+(clothing.getClothingType().isPlural()?"have an enslavement enchantment, they":"has an enslavement enchantment, it")
																	+" [style.colourArcane(will enslave [npc.name])], making you [npc.her] new owner!</i>")),
												INVENTORY_MENU){
											@Override
											public DialogueNode getNextDialogue() {
												if(inventoryNPC.getEnslavementDialogue(clothing)!=null) {
													return inventoryNPC.getEnslavementDialogue(clothing);
													
												} else {
													return INVENTORY_MENU;
												}
											}
											@Override
											public void effects() {
												List<NPC> enslavementTargets = new ArrayList<>(Main.game.getNonCompanionCharactersPresent());
//												enslavementTargets.removeIf((npc) -> Main.game.getPlayer().getFriendlyOccupants().contains(npc.getId()));
												enslavementTargets.removeIf((npc) -> !Main.combat.getEnemies(Main.game.getPlayer()).contains(npc));
												if(enslavementTargets.size()<=1) {
													SlaveDialogue.setFollowupEnslavementDialogue(Main.game.getDefaultDialogue(false));
												} else {
													SlaveDialogue.setFollowupEnslavementDialogue(Main.game.getSavedDialogueNode());
												}
												if(inventoryNPC.getEnslavementDialogue(clothing)==null) {
													Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + equipClothingFromInventory(inventoryNPC, slot, Main.game.getPlayer(), clothing) + "</p>");
													
												} else {
													equipClothingFromInventory(inventoryNPC, slot, Main.game.getPlayer(), clothing);
												}
											}
										};
										
									} else {
										return new Response(
												UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
												UtilText.parse(inventoryNPC, "Make [npc.name] equip the "+clothing.getName()+"!"),
												INVENTORY_MENU){
											@Override
											public void effects(){
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + equipClothingFromInventory(inventoryNPC, slot, Main.game.getPlayer(), clothing) + "</p>");
											}
										};
									}
									
								} else {
									return new Response(
											UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
											clothing.getCannotBeEquippedText(inventoryNPC, slot),
											null);
								}
							
							} else {
								return null;
							}
							
						case SEX:
							if(index == 1) {
								return new Response("Give (1)", "You can't give someone clothing while having sex with them!", null);
								
							} else if(index == 2) {
								return new Response("Give (5)", "You can't give someone clothing while having sex with them!", null);
								
							} else if(index == 3) {
								return new Response("Give (All)", "You can't give someone clothing while having sex with them!", null);
								
							} else if(index == 4) {
								return new Response("Dye", "You can't dye your clothing while having sex with someone!", null);
								
							} else if(index == 5) {
								if(clothing.isCondom()) {
									if(clothing.getCondomEffect().getPotency().isNegative()) {
										return new Response("Repair (<i>1 Essence</i>)", "You can't repair the condom while having sex with someone!", null);
									}
									return new Response("Sabotage", "You can't sabotage the condom while having sex with someone!", null);
								}
								return new Response("Enchant", "You can't enchant clothing while having sex with someone!", null);

							} else if(index >= 6 && index <= 9 && index-6<clothing.getClothingType().getEquipSlots().size()) {
								InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-6);
								if(clothing.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									if(clothing.isAbleToBeEquippedDuringSex(slot).getKey()) {
										if(!Main.sex.getInitialSexManager().isAbleToEquipSexClothing(Main.game.getPlayer(), Main.game.getPlayer(), clothing)) {
											return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "As this is a special sex scene, you cannot equip clothing during it!", null);
										}
										if(!Main.sex.isClothingEquipAvailable(Main.game.getPlayer(), slot, clothing)) {
											return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "This slot is involved with an ongoing sex action, so you cannot equip clothing into it!", null);
										}
										if (Main.game.getPlayer().isAbleToEquip(clothing, slot, false, Main.game.getPlayer())) {
											return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "Equip the " + clothing.getName() + ".", Main.sex.SEX_DIALOGUE){
												@Override
												public void effects(){
													AbstractClothing c = clothing;
													equipClothingFromInventory(Main.game.getPlayer(), slot, Main.game.getPlayer(), clothing);
													Main.sex.setEquipClothingText(c, Main.game.getPlayer().getUnequipDescription());
													Main.mainController.openInventory();
													Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
													Main.sex.setSexStarted(true);
												}
											};
										} else {
											return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), getClothingBlockingRemovalText("equip"), null);
										}
										
									} else {
										return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), clothing.isAbleToBeEquippedDuringSex(slot).getValue(), null);
									}
									
								} else {
									return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), clothing.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}
								
							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index >= 11 && index <= 14 && index-11<clothing.getClothingType().getEquipSlots().size()) {
								InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-11);
//								if(!clothing.getClothingType().isAbleToBeDropped()) {
//									return new Response(
//											UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
//											"You cannot give away the " + clothing.getName() + "!",
//											null);
//								}
								Value<Boolean, String> equipAllowed = inventoryNPC.isInventoryEquipAllowed(clothing, slot);
								if(!equipAllowed.getKey()) {
									return new Response(
											UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
											UtilText.parse(inventoryNPC, equipAllowed.getValue()),
											null);
								}
								if(clothing.isCanBeEquipped(inventoryNPC, slot)) {
									if(clothing.isAbleToBeEquippedDuringSex(slot).getKey()) {
										if(!Main.sex.getInitialSexManager().isAbleToEquipSexClothing(Main.game.getPlayer(), inventoryNPC, clothing)) {
											return new Response(
													UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
													"As this is a special sex scene, you cannot equip clothing during it!",
													null);
										}
										if(!Main.sex.isClothingEquipAvailable(inventoryNPC, slot, clothing)) {
											return new Response(
													UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
													"This slot is involved with an ongoing sex action, so you cannot equip clothing into it!",
													null);
										}
										if(inventoryNPC.isAbleToEquip(clothing, slot, false, Main.game.getPlayer())) {
											return new Response(
													UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
													UtilText.parse(inventoryNPC, "Get [npc.name] to equip the " + clothing.getName() + "."),
													Main.sex.SEX_DIALOGUE){
												@Override
												public void effects(){
													AbstractClothing c = clothing;
													equipClothingFromInventory(inventoryNPC, slot, Main.game.getPlayer(), clothing);
													Main.sex.setEquipClothingText(c, inventoryNPC.getUnequipDescription());
													Main.mainController.openInventory();
													Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
													Main.sex.setSexStarted(true);
												}
											};
										} else {
											return new Response(UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
													UtilText.parse(inventoryNPC, "[npc.Name] can't equip the " + clothing.getName() + ", as other clothing is blocking [npc.herHim] from doing so!"), null);
										}
										
									} else {
										return new Response(UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"), clothing.isAbleToBeEquippedDuringSex(slot).getValue(), null);
									}
									
								} else {
									return new Response(UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"), clothing.getCannotBeEquippedText(inventoryNPC, slot), null);
								}
								
							} else {
								return null;
							}
							
						case TRADING:
							if(index == 1) {
								if(!clothing.getClothingType().isAbleToBeSold()) {
									return new Response("Sell (1)", "You cannot sell the " + clothing.getName() + "!", null);
									
								} else if (inventoryNPC.willBuy(clothing)) {
									int sellPrice = clothing.getPrice(inventoryNPC.getBuyModifier());
									return new Response("Sell (1) (" + UtilText.formatAsMoney(sellPrice, "span") + ")", "Sell the " + clothing.getName() + " for " + UtilText.formatAsMoney(sellPrice) + ".", INVENTORY_MENU){
										@Override
										public void effects(){
											sellClothing(Main.game.getPlayer(), inventoryNPC, clothing, 1, sellPrice);
										}
									};
								} else {
									return new Response("Sell (1)", inventoryNPC.getName("The") + " doesn't want to buy this.", null);
								}
								
							} else if(index == 2) {
								if(!clothing.getClothingType().isAbleToBeSold()) {
									return new Response("Sell (5)", "You cannot sell the " + clothing.getName() + "!", null);
									
								} else if(Main.game.getPlayer().getClothingCount(clothing) >= 5) {
									if (inventoryNPC.willBuy(clothing)) {
										int sellPrice = clothing.getPrice(inventoryNPC.getBuyModifier());
										return new Response("Sell (5) (" + UtilText.formatAsMoney(sellPrice*5, "span") + ")", "Sell five of your " + clothing.getNamePlural() + " for " + UtilText.formatAsMoney(sellPrice*5) + ".", INVENTORY_MENU){
											@Override
											public void effects(){
												sellClothing(Main.game.getPlayer(), inventoryNPC, clothing, 5, sellPrice);
											}
										};
									} else {
										return new Response("Sell (5)", inventoryNPC.getName("The") + " doesn't want to buy these.", null);
									}
									
								} else {
									return new Response("Sell (5)", "You don't have five " + clothing.getNamePlural() + " to sell!", null);
								}
								
							} else if(index == 3) {
								if(!clothing.getClothingType().isAbleToBeSold()) {
									return new Response("Sell (All)", "You cannot sell the " + clothing.getName() + "!", null);
									
								} else if (inventoryNPC.willBuy(clothing)) {
									int sellPrice = clothing.getPrice(inventoryNPC.getBuyModifier());
									return new Response("Sell (All) (" + UtilText.formatAsMoney(sellPrice*Main.game.getPlayer().getClothingCount(clothing), "span") + ")",
											"Sell the " + clothing.getName() + " for " + UtilText.formatAsMoney(sellPrice*Main.game.getPlayer().getClothingCount(clothing)) + ".", INVENTORY_MENU){
										@Override
										public void effects(){
											sellClothing(Main.game.getPlayer(), inventoryNPC, clothing, Main.game.getPlayer().getClothingCount(clothing), sellPrice);
										}
									};
								} else {
									return new Response("Sell (All)", inventoryNPC.getName("The") + " doesn't want to buy these.", null);
								}
								
							} else if (index==4) {
								if (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
									boolean hasFullInventory = Main.game.getPlayer().isInventoryFull() && clothing.getRarity()!=Rarity.QUEST;
									boolean isDyeingStackItem = Main.game.getPlayer().getAllClothingInInventory().get(clothing) > 1;
									boolean canDye = !(isDyeingStackItem && hasFullInventory);
									if (canDye) {
										return new Response("Dye", 
												Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
													?"Use your proficiency with [style.colourEarth(Earth spells)] to dye this item."
													:"Use a dye-brush to dye this item of clothing.",
												DYE_CLOTHING) {
											@Override
											public void effects() {
												resetClothingDyeColours();
											}
										};
									} else {
										return new Response("Dye", "Your inventory is full, so you can't dye this item of clothing.", null);
									}
								} else {
									return new Response("Dye", "You'll need to find a dye-brush if you want to dye your clothes.", null);
								}
								
							} else if(index == 5) {
								if(clothing.isCondom()) {
									return getCondomSabotageResponse(clothing);
								}
								if(Main.game.isDebugMode()
										|| (Main.game.getPlayer().hasQuest(QuestLine.SIDE_ENCHANTMENT_DISCOVERY) && Main.game.getPlayer().isQuestCompleted(QuestLine.SIDE_ENCHANTMENT_DISCOVERY))) {
									if(clothing.getEnchantmentItemType(null)==null || clothing.getItemTags().contains(ItemTag.UNENCHANTABLE)) {
										return new Response("Enchant", "This clothing cannot be enchanted!", null);
										
									} else if(!clothing.isEnchantmentKnown()) {
										if(Main.game.getPlayer().getEssenceCount() >= IDENTIFICATION_ESSENCE_PRICE) {
											return new Response("Identify ([style.italicsArcane("+IDENTIFICATION_ESSENCE_PRICE+" Essences)])",
													"To identify the "+clothing.getName()+", you can either spend "+IDENTIFICATION_ESSENCE_PRICE+" arcane essences to do it yourself,"
															+ " or go to a vendor and pay "+IDENTIFICATION_PRICE+" flames to have them do it for you.",
													CLOTHING_INVENTORY) {
												@Override
												public void effects() {
													Main.game.getPlayer().incrementEssenceCount(-IDENTIFICATION_ESSENCE_PRICE, false);

													String enchantmentRemovedString = clothing.setEnchantmentKnown(owner, true);
													
													clothing = AbstractClothing.enchantmentRemovedClothing;
													
													Main.game.getTextEndStringBuilder().append(
															"<p>"
																+ "You channel the power of "+Util.intToString(IDENTIFICATION_ESSENCE_PRICE)+" of your arcane essences into the "+clothing.getName()
																	+", and as it emits a faint purple glow, you find yourself able to detect what sort of enchantment it has!"
															+ "</p>"
															+ enchantmentRemovedString
															+ "<p style='text-align:center;'>"
																+ "Identifying the "+clothing.getName()+" has cost you [style.boldBad("+Util.intToString(IDENTIFICATION_ESSENCE_PRICE)+")] [style.boldArcane(Arcane Essences)]!"
															+ "</p>");
													RenderingEngine.setPage(Main.game.getPlayer(), clothing);
												}
											};
										} else {
											return new Response("Identify (<i>"+IDENTIFICATION_ESSENCE_PRICE+" Essences</i>)",
													"To identify the "+clothing.getName()+", you can either spend "+IDENTIFICATION_ESSENCE_PRICE+" arcane essences to do it yourself ([style.italicsBad(which you don't have)]),"
															+ " or go to a vendor and pay "+IDENTIFICATION_PRICE+" flames to have them do it for you.", null);
										}
									}
									return new Response("Enchant", "Enchant this clothing.", EnchantmentDialogue.ENCHANTMENT_MENU) {
										@Override
										public DialogueNode getNextDialogue() {
											return EnchantmentDialogue.getEnchantmentMenu(clothing);
										}
									};
									
								} else {
									return new Response("Enchant", getEnchantmentNotDiscoveredText("clothing"), null);
								}

							} else if(index >= 6 && index <= 9 && index-6<clothing.getClothingType().getEquipSlots().size()) {
								InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-6);
								if(clothing.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "Equip the " + clothing.getName() + ".", INVENTORY_MENU){
											@Override
											public void effects(){
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + equipClothingFromInventory(Main.game.getPlayer(), slot, Main.game.getPlayer(), clothing) + "</p>");
											}
										};
								} else {
									return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), clothing.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}
								
							} else if (index == 9) {
								return getBuybackResponse();
								
							}
//							else if (index == 10) {
//								return getQuickTradeResponse();
//
//							}
							else if(index >= 11 && index <= 14 && index-11<clothing.getClothingType().getEquipSlots().size()) {
								InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-11);
								return new Response(UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"), UtilText.parse(inventoryNPC, "[npc.Name] doesn't want to wear your clothing."), null);
								
							} else if (index == 10 && !clothing.isEnchantmentKnown()) {
								if(!inventoryNPC.willBuy(clothing)) {
									return new Response("Identify", inventoryNPC.getName("The") + " can't identify clothing!", null);
									
								} else if(Main.game.getPlayer().getMoney() < IDENTIFICATION_PRICE){
									// don't format as money because we don't want to highlight non-selectable choices
									return new Response("Identify (" + UtilText.formatAsMoneyUncoloured(IDENTIFICATION_PRICE, "span") + ")", "You don't have enough money!", null);
									
								}else {
									return new Response("Identify (" + UtilText.formatAsMoney(IDENTIFICATION_PRICE, "span") + ")",
												"Have the " + clothing.getName() + " identified for " + UtilText.formatAsMoney(IDENTIFICATION_PRICE, "span") + ".", CLOTHING_INVENTORY){
										@Override
										public void effects(){
											Main.game.getPlayer().incrementMoney(-IDENTIFICATION_PRICE);

											String enchantmentRemovedString = clothing.setEnchantmentKnown(owner, true);
											
											clothing = AbstractClothing.enchantmentRemovedClothing;
											
											Main.game.getTextEndStringBuilder().append(
													"<p style='text-align:center;'>"
														+ UtilText.parse(inventoryNPC,
																"You hand over " + UtilText.formatAsMoney(IDENTIFICATION_PRICE) + " to [npc.name],"
																		+ " who promptly feeds several bottles of arcane essence into a specialist identification device, before using it to reveal the enchantment on your "+clothing.getName()+".")
													+ "</p>"
													+enchantmentRemovedString);
											
											RenderingEngine.setPage(Main.game.getPlayer(), clothing);
										}
									};
								}
							} else {
								return null;
							}
					}
				}
				
			// ****************************** ITEM DOES NOT BELONG TO PLAYER ******************************
				
			} else {
				// ****************************** Interacting with the ground ******************************
				if(inventoryNPC == null) {

					boolean inventoryFull = Main.game.getPlayer().isInventoryFull() && !Main.game.getPlayer().hasClothing(clothing) && clothing.getRarity()!=Rarity.QUEST;
					switch(interactionType) {
						case CHARACTER_CREATION:
							if (index == 1) {
								if(Main.game.getPlayer().isCoverableAreaVisible(CoverableArea.NIPPLES)
										|| Main.game.getPlayer().isCoverableAreaVisible(CoverableArea.ANUS)
										|| Main.game.getPlayer().isCoverableAreaVisible(CoverableArea.PENIS)
										|| Main.game.getPlayer().isCoverableAreaVisible(CoverableArea.VAGINA)
										|| (Main.game.getPlayer().getClothingInSlot(InventorySlot.FOOT)==null && Main.game.getPlayer().getLegType().equals(LegType.HUMAN))) {
									return new Response("To the stage", "You need to be wearing clothing that covers your body, as well as a pair of shoes.", null);
									
								} else {
									return new Response("To the stage", "You're ready to approach the stage now.", CharacterCreation.CHOOSE_BACKGROUND) {
										@Override
										public void effects() {
											CharacterCreation.moveNPCIntoPlayerTile();
										}
									};
								}
								
							} else if(index == 4) {
								if(Main.game.getPlayer().getClothingCurrentlyEquipped().isEmpty()){
									return new Response("Unequip all", "You're currently naked, there's nothing to be unequipped.", null);
								}
								else{
									return new Response("Unequip all", "Remove as much of your clothing as possible.", INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append(unequipAll(Main.game.getPlayer()));
										}
									};
								}
								
							} else if(index == 5) {
								return new Response("Change colour", "Change the colour of this item of clothing.", DYE_CLOTHING_CHARACTER_CREATION) {
									@Override
									public void effects() {
										resetClothingDyeColours();
									}
								};
							} else if(index >= 6 && index <= 9 && index-6<clothing.getClothingType().getEquipSlots().size()) {
								InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-6);
								if(clothing.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "Equip the " + clothing.getName() + ".", INVENTORY_MENU){
										@Override
										public void effects(){
											equipClothingFromGround(Main.game.getPlayer(), slot, Main.game.getPlayer(), clothing);
										}
									};
									
								} else {
									return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), clothing.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}
							
							} else {
								return null;
							}
					
					case SEX:
						if(index == 1) {
							return new Response("Take (1)", "You can't pick up clothing while masturbating.", null);
							
						} else if(index == 2) {
							return new Response("Take (5)", "You can't pick up clothing while masturbating.", null);
							
						} else if(index == 3) {
							return new Response("Take (All)", "You can't pick up clothing while masturbating.", null);
							
						} else if(index == 4) {
							return new Response("Dye", "You can't dye your clothing while masturbating.", null);
							
						} else if(index == 5) {
							if(clothing.isCondom()) {
								if(clothing.getCondomEffect().getPotency().isNegative()) {
									return new Response("Repair (<i>1 Essence</i>)", "You can't repair condoms on the ground!", null);
								}
								return new Response("Sabotage", "You can't sabotage condoms on the ground!", null);
							}
							return new Response("Enchant", "You can't enchant clothing on the ground!", null);

						} else if(index >= 6 && index <= 9 && index-6<clothing.getClothingType().getEquipSlots().size()) {
							InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-6);
							if(clothing.isCanBeEquipped(Main.game.getPlayer(), slot)) {
								if(clothing.isAbleToBeEquippedDuringSex(slot).getKey()) {
									if(!Main.sex.getInitialSexManager().isAbleToEquipSexClothing(Main.game.getPlayer(), Main.game.getPlayer(), clothing)) {
										return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "As this is a special sex scene, you cannot equip clothing during it!", null);
									}
									if(!Main.sex.isClothingEquipAvailable(Main.game.getPlayer(), slot, clothing)) {
										return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "This slot is involved with an ongoing sex action, so you cannot equip clothing into it!", null);
									}
									if (Main.game.getPlayer().isAbleToEquip(clothing, slot, false, Main.game.getPlayer())) {
										return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "Equip the " + clothing.getName() + ".", Main.sex.SEX_DIALOGUE){
											@Override
											public void effects(){
												AbstractClothing c = clothing;
												equipClothingFromGround(Main.game.getPlayer(), slot, Main.game.getPlayer(), clothing);
												Main.sex.setEquipClothingText(c, Main.game.getPlayer().getUnequipDescription());
												Main.mainController.openInventory();
												Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
												Main.sex.setSexStarted(true);
											}
										};
									} else {
										return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), getClothingBlockingRemovalText("equip"), null);
									}
								} else {
									return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), clothing.isAbleToBeEquippedDuringSex(slot).getValue(), null);
								}
							} else {
								return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), clothing.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
							}
							
						} else if (index == 10) {
							return getQuickTradeResponse();

						} else {
							return null;
						}
					
					default:
						if(index == 1) {
							if(inventoryFull) {
								return new Response("Take (1)", "Your inventory is already full!", null);
							}
							return new Response("Take (1)", "Take one " + clothing.getName() + " from the ground.", INVENTORY_MENU){
								@Override
								public void effects(){
									pickUpClothing(Main.game.getPlayer(), clothing, 1);
								}
							};
							
						} else if(index == 2) {
							if(inventoryFull) {
								return new Response("Take (5)", "Your inventory is already full!", null);
							}
							if(Main.game.getPlayerCell().getInventory().getClothingCount(clothing) >= 5) {
								return new Response("Take (5)", "Take five of the " + clothing.getNamePlural() + " from the ground.", INVENTORY_MENU){
									@Override
									public void effects(){
										pickUpClothing(Main.game.getPlayer(), clothing, 5);
									}
								};
							} else {
								return new Response("Take (5)", "There aren't five " + clothing.getNamePlural() + " on the ground!", null);
							}
							
						} else if(index == 3) {
							if(inventoryFull) {
								return new Response("Take (All)", "Your inventory is already full!", null);
							}
							return new Response("Take (All)", "Take all of the " + clothing.getNamePlural() + " from the ground.", INVENTORY_MENU){
								@Override
								public void effects(){
									pickUpClothing(Main.game.getPlayer(), clothing, Main.game.getPlayerCell().getInventory().getClothingCount(clothing));
								}
							};
							
						} else if (index==4) {
							if (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
								boolean hasFullInventory = Main.game.getPlayerCell().getInventory().isInventoryFull();
								boolean isDyeingStackItem = Main.game.getPlayerCell().getInventory().getAllClothingInInventory().get(clothing) > 1;
								boolean canDye = !(isDyeingStackItem && hasFullInventory);
								if (canDye) {
									return new Response("Dye", 
											Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"Use your proficiency with [style.colourEarth(Earth spells)] to dye this item."
												:"Use a dye-brush to dye this item of clothing.",
											DYE_CLOTHING) {
										@Override
										public void effects() {
											resetClothingDyeColours();
										}
									};
								} else {
									return new Response("Dye", "Your inventory is full, so you can't dye this item of clothing.", null);
								}
							} else {
								return new Response("Dye", "You'll need to find a dye-brush if you want to dye your clothes.", null);
							}
							
						} else if(index == 5) {
							if(clothing.isCondom()) {
								if(clothing.getCondomEffect().getPotency().isNegative()) {
									return new Response("Repair (<i>1 Essence</i>)", "You can't repair condoms on the ground!", null);
								}
								return new Response("Sabotage", "You can't sabotage condoms on the ground!", null);
							}
							return new Response("Enchant", "You can't enchant clothing on the ground!", null);
	
						} else if(index >= 6 && index <= 9 && index-6<clothing.getClothingType().getEquipSlots().size()) {
							InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-6);
							if(clothing.isCanBeEquipped(Main.game.getPlayer(), slot)) {
								return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "Equip the " + clothing.getName() + ".", INVENTORY_MENU){
									@Override
									public void effects(){
										Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + equipClothingFromGround(Main.game.getPlayer(), slot, Main.game.getPlayer(), clothing) + "</p>");
									}
								};
							} else {
								return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), clothing.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
							}
							
						} else if (index == 10) {
							return getQuickTradeResponse();
							
						} else {
							return null;
						}
					}
					
				// ****************************** Interacting with an NPC ******************************
				} else {
					boolean inventoryFull = false;
					switch(interactionType) {
						case COMBAT:
							if(index == 1) {
								return new Response("Take (1)", "You can't take someone clothing while fighting them!", null);
								
							} else if(index == 2) {
								return new Response("Take (5)", "You can't take someone clothing while fighting them!", null);
								
							} else if(index == 3) {
								return new Response("Take (All)", "You can't take someone clothing while fighting them!", null);
								
							} else if(index == 4) {
								return new Response("Dye", "You can't dye someone's clothing while fighting them!", null);
								
							} else if(index == 5) {
								if(clothing.isCondom()) {
									if(clothing.getCondomEffect().getPotency().isNegative()) {
										return new Response("Repair (<i>1 Essence</i>)", "You can't repair someone else's condom, especially not when fighting them!", null);
									}
									return new Response("Sabotage", "You can't sabotage someone else's condom, especially not while fighting them!", null);
								}
								return new Response("Enchant", "You can't enchant someone else's clothing, especially not while fighting them!", null);
								
							} else if(index == 6) {
								return new Response("Equip (Self)", "You can't use someone else's clothing while fighting them!", null);
								
							} else if (index == 10) {
								return getQuickTradeResponse();
								
							} else if(index == 11) {
								return new Response(UtilText.parse(inventoryNPC, "Equip: ([npc.HerHim])"), "You can't make someone wear clothing while fighting them!", null);
								
							} else {
								return null;
							}
							
						case FULL_MANAGEMENT: case CHARACTER_CREATION:
							inventoryFull = Main.game.getPlayer().isInventoryFull() && !Main.game.getPlayer().hasClothing(clothing) && clothing.getRarity()!=Rarity.QUEST;
						
							if(index == 1) {
								if(inventoryFull) {
									return new Response("Take (1)", "Your inventory is already full!", null);
								}
								return new Response("Take (1)", UtilText.parse(inventoryNPC, "Take the " + clothing.getName() + " from [npc.name]."), INVENTORY_MENU){
									@Override
									public void effects(){
										transferClothing(inventoryNPC, Main.game.getPlayer(), clothing, 1);
									}
								};
								
							} else if(index == 2) {
								if(inventoryFull) {
									return new Response("Take (5)", "Your inventory is already full!", null);
								}
								if(inventoryNPC.getClothingCount(clothing) >= 5) {
									return new Response("Take (5)", UtilText.parse(inventoryNPC, "Take five of  [npc.namePos] " + clothing.getNamePlural() + "."), INVENTORY_MENU){
										@Override
										public void effects(){
											transferClothing(inventoryNPC, Main.game.getPlayer(), clothing, 5);
										}
									};
								} else {
									return new Response("Take (5)", UtilText.parse(inventoryNPC, "[npc.Name] doesn't have five " + clothing.getNamePlural() + "!"), null);
								}
								
							} else if(index == 3) {
								if(inventoryFull) {
									return new Response("Take (All)", "Your inventory is already full!", null);
								}
								return new Response("Take (All)", UtilText.parse(inventoryNPC, "Take all "+Util.intToString(inventoryNPC.getClothingCount(clothing))+" of  [npc.namePos] " + clothing.getNamePlural() + "."), INVENTORY_MENU){
									@Override
									public void effects(){
										transferClothing(inventoryNPC, Main.game.getPlayer(), clothing, inventoryNPC.getClothingCount(clothing));
									}
								};
								
							} else if (index==4) {
								if (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
									boolean hasFullInventory = inventoryNPC.isInventoryFull();
									boolean isDyeingStackItem = clothing!=null && inventoryNPC.getAllClothingInInventory().get(clothing) > 1;
									boolean canDye = !(isDyeingStackItem && hasFullInventory);
									if (canDye) {
										return new Response("Dye", 
												Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
													?"Use your proficiency with [style.colourEarth(Earth spells)] to dye this item."
													:"Use a dye-brush to dye this item of clothing.",
												DYE_CLOTHING) {
											@Override
											public void effects() {
												resetClothingDyeColours();
											}
										};
									} else {
										return new Response("Dye", UtilText.parse(inventoryNPC, "[npc.NamePos] inventory is full, so you can't dye this item of clothing."), null);
									}
								} else {
									return new Response("Dye", UtilText.parse(inventoryNPC, "You'll need to find another dye-brush if you want to dye [npc.namePos] clothes."), null);
								}
								
							} else if(index == 5) {
								if(clothing.isCondom()) {
									if(clothing.getCondomEffect().getPotency().isNegative()) {
										return new Response("Repair (<i>1 Essence</i>)", "You can't repair condoms owned by someone else!", null);
									}
									return new Response("Sabotage", "You can't sabotage condoms owned by someone else!", null);
								}
								return new Response("Enchant", "You can't enchant clothing owned by someone else!", null);

							} else if(index >= 6 && index <= 9 && index-6<clothing.getClothingType().getEquipSlots().size()) {
								InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-6);
								if(clothing.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "Equip the " + clothing.getName() + ".", INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + equipClothingFromInventory(Main.game.getPlayer(), slot, Main.game.getPlayer(), clothing) + "</p>");
										}
									};
								} else {
									return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), clothing.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}
								
							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index >= 11 && index <= 14 && index-11<clothing.getClothingType().getEquipSlots().size()) {
								InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-11);
								Value<Boolean, String> equipAllowed = inventoryNPC.isInventoryEquipAllowed(clothing, slot);
								if(!equipAllowed.getKey()) {
									return new Response(
											UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
											UtilText.parse(inventoryNPC, equipAllowed.getValue()),
											null);
								}
								if(clothing.isCanBeEquipped(inventoryNPC, slot)) {
									if(inventoryNPC.isAbleToEquip(clothing, slot, true, Main.game.getPlayer()) && clothing.isEnslavementClothing() && (!inventoryNPC.isSlave() || !inventoryNPC.getOwner().isPlayer())) {
										boolean willEnslave = !inventoryNPC.isSlave() && inventoryNPC.isAbleToBeEnslaved() && Main.game.getPlayer().isHasSlaverLicense();
										return new Response(
												UtilText.parse(inventoryNPC,
														!willEnslave
															?"[style.colourMinorBad(Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim]))]"
															:"[style.colourArcane(Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim]))]"),
												UtilText.parse(inventoryNPC,
														"Make [npc.name] equip the "+clothing.getName()+"."
														+(!willEnslave
															?"<br/><i>Despite the fact that the "+clothing.getName()+" "
																	+(clothing.getClothingType().isPlural()?"have an enslavement enchantment, they":"has an enslavement enchantment, it")
																	+" [style.colourMinorBad(will not enslave [npc.name])], as"
																	+ (Main.game.getPlayer().isHasSlaverLicense()
																			?" [npc.sheIsFull] not a suitable target for enslavement"
																			:" you do not possess a slaver license")
																	+"!</i>"
															:"<br/><i>Thanks to the fact that the "+clothing.getName()+" "
																	+(clothing.getClothingType().isPlural()?"have an enslavement enchantment, they":"has an enslavement enchantment, it")
																	+" [style.colourArcane(will enslave [npc.name])], making you [npc.her] new owner!</i>")),
												INVENTORY_MENU){
											@Override
											public DialogueNode getNextDialogue() {
												if(inventoryNPC.getEnslavementDialogue(clothing)!=null) {//inventoryNPC.isAbleToBeEnslaved() && !inventoryNPC.isSlave()) {
													return inventoryNPC.getEnslavementDialogue(clothing);
													
												} else {
													return INVENTORY_MENU;
												}
											}
											@Override
											public void effects() {
												List<NPC> enslavementTargets = new ArrayList<>(Main.game.getNonCompanionCharactersPresent());
//												enslavementTargets.removeIf((npc) -> Main.game.getPlayer().getFriendlyOccupants().contains(npc.getId()));
												enslavementTargets.removeIf((npc) -> !Main.combat.getEnemies(Main.game.getPlayer()).contains(npc));
												if(enslavementTargets.size()<=1) {
													SlaveDialogue.setFollowupEnslavementDialogue(Main.game.getDefaultDialogue(false));
												} else {
													SlaveDialogue.setFollowupEnslavementDialogue(Main.game.getSavedDialogueNode());
												}
												if(inventoryNPC.getEnslavementDialogue(clothing)==null) {
													Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + equipClothingFromInventory(inventoryNPC, slot, Main.game.getPlayer(), clothing) + "</p>");
													
												} else {
													equipClothingFromInventory(inventoryNPC, slot, Main.game.getPlayer(), clothing);
												}
											}
										};
										
									} else {
										return new Response(
												UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
												UtilText.parse(inventoryNPC, "Get [npc.name] to equip the " + clothing.getName() + "."),
												INVENTORY_MENU){
											@Override
											public void effects(){
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + equipClothingFromInventory(inventoryNPC, slot, Main.game.getPlayer(), clothing) + "</p>");
											}
										};
									}
								} else {
									return new Response(
											UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
											clothing.getCannotBeEquippedText(inventoryNPC, slot),
											null);
								}
								
							} else {
								return null;
							}
							
						case SEX:
							if(index == 1) {
								return new Response("Take (1)", "You can't take someone's clothing while having sex with them!", null);
								
							} else if(index == 2) {
								return new Response("Take (5)", "You can't take someone's clothing while having sex with them!", null);
								
							} else if(index == 3) {
								return new Response("Take (All)", "You can't take someone's clothing while having sex with them!", null);
								
							} else if(index == 4) {
								return new Response("Dye", "You can't dye someone's clothing while having sex with them!", null);
								
							} else if(index == 5) {
								if(clothing.isCondom()) {
									if(clothing.getCondomEffect().getPotency().isNegative()) {
										return new Response("Repair (<i>1 Essence</i>)", "You can't repair someone else's condom, especially not while having sex with them!", null);
									}
									return new Response("Sabotage", "You can't sabotage someone else's condom, especially not while having sex with them!", null);
								}
								return new Response("Enchant", "You can't enchant someone else's clothing, especially not while having sex with them!", null);

							} else if(index >= 6 && index <= 9 && index-6<clothing.getClothingType().getEquipSlots().size()) { //TODO ???
								InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-6);
								if(clothing.isCanBeEquipped(Main.game.getPlayer(), slot)) {
									if(clothing.isAbleToBeEquippedDuringSex(slot).getKey() && !inventoryNPC.isTrader()) {
										if(!Main.sex.getInitialSexManager().isAbleToEquipSexClothing(Main.game.getPlayer(), Main.game.getPlayer(), clothing)) {
											return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "As this is a special sex scene, you cannot equip clothing during it!", null);
										}
										if(!Main.sex.isClothingEquipAvailable(Main.game.getPlayer(), slot, clothing)) {
											return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "This slot is involved with an ongoing sex action, so you cannot equip clothing into it!", null);
										}
										if (Main.game.getPlayer().isAbleToEquip(clothing, slot, false, Main.game.getPlayer())) {
											return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), "Equip the " + clothing.getName() + ".", Main.sex.SEX_DIALOGUE){
												@Override
												public void effects(){
													AbstractClothing c = clothing;
													equipClothingFromInventory(Main.game.getPlayer(), slot, Main.game.getPlayer(), clothing);
													Main.sex.setEquipClothingText(c, Main.game.getPlayer().getUnequipDescription());
													Main.mainController.openInventory();
													Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
													Main.sex.setSexStarted(true);
												}
											};
										} else {
											return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), getClothingBlockingRemovalText("equip"), null);
										}
										
									} else {
										return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), clothing.isAbleToBeEquippedDuringSex(slot).getValue(), null);
									}
									
								} else {
									return new Response("Equip: "+Util.capitaliseSentence(slot.getName()), clothing.getCannotBeEquippedText(Main.game.getPlayer(), slot), null);
								}
								
							} else if (index == 10) {
								return getQuickTradeResponse();

							} else if(index >= 11 && index <= 14 && index-11<clothing.getClothingType().getEquipSlots().size()) {
								InventorySlot slot = clothing.getClothingType().getEquipSlots().get(index-11);
								Value<Boolean, String> equipAllowed = inventoryNPC.isInventoryEquipAllowed(clothing, slot);
								if(!equipAllowed.getKey()) {
									return new Response(
											UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
											UtilText.parse(inventoryNPC, equipAllowed.getValue()),
											null);
								}
								if(clothing.isCanBeEquipped(inventoryNPC, slot)) {
									if(clothing.isAbleToBeEquippedDuringSex(slot).getKey() && !inventoryNPC.isTrader()) {
										if(!Main.sex.getInitialSexManager().isAbleToEquipSexClothing(Main.game.getPlayer(), inventoryNPC, clothing)) {
											return new Response(
													UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
													"As this is a special sex scene, you cannot equip clothing during it!",
													null);
										}
										if(!Main.sex.isClothingEquipAvailable(inventoryNPC, slot, clothing)) {
											return new Response(
													UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"), 
													"This slot is involved with an ongoing sex action, so you cannot equip clothing into it!",
													null);
										}
										if (inventoryNPC.isAbleToEquip(clothing, slot, false, Main.game.getPlayer())) {
											return new Response(
													UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
													UtilText.parse(inventoryNPC, "Get [npc.name] to equip the " + clothing.getName() + "."),
													Main.sex.SEX_DIALOGUE){
												@Override
												public void effects(){
													AbstractClothing c = clothing;
													equipClothingFromInventory(inventoryNPC, slot, Main.game.getPlayer(), clothing);
													Main.sex.setEquipClothingText(c, inventoryNPC.getUnequipDescription());
													Main.mainController.openInventory();
													Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
													Main.sex.setSexStarted(true);
												}
											};
										} else {
											return new Response(
													UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
													UtilText.parse(inventoryNPC, "[npc.Name] can't equip the " + clothing.getName() + ", as other clothing is blocking [npc.herHim] from doing so!"),
													null);
										}
										
									} else {
										return new Response(
												UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
												clothing.isAbleToBeEquippedDuringSex(slot).getValue(),
												null);
									}
									
								} else {
									return new Response(
											UtilText.parse(inventoryNPC, "Equip: "+Util.capitaliseSentence(slot.getName())+" ([npc.HerHim])"),
											clothing.getCannotBeEquippedText(inventoryNPC, slot),
											null);
								}
								
							} else {
								return null;
							}
							
						case TRADING:
							inventoryFull = Main.game.getPlayer().isInventoryFull() && !Main.game.getPlayer().hasClothing(clothing) && clothing.getRarity()!=Rarity.QUEST;
							
							if(index == 1) {
								int sellPrice = buyback?Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getPrice():clothing.getPrice(inventoryNPC.getSellModifier(clothing));
								if(inventoryFull) {
									return new Response("Buy (1) ("+UtilText.formatAsMoneyUncoloured(sellPrice, "span")+")", "Your inventory is already full!", null);
								}
								if(Main.game.getPlayer().getMoney() < sellPrice) {
									return new Response("Buy (1) ("+UtilText.formatAsMoneyUncoloured(sellPrice, "span")+")", "You can't afford to buy this!", null);
								}
								return new Response("Buy (1) (" + UtilText.formatAsMoney(sellPrice, "span") + ")", "Buy the " + clothing.getName() + " for " + UtilText.formatAsMoney(sellPrice) + ".", INVENTORY_MENU){
									@Override
									public void effects(){
										sellClothing(inventoryNPC, Main.game.getPlayer(), clothing, 1, sellPrice);
									}
								};
								
							} else if(index == 2) {
								int sellPrice = buyback?Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getPrice():clothing.getPrice(inventoryNPC.getSellModifier(clothing));
								if((buyback && Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getCount()<5)
										|| (!buyback && inventoryNPC.getClothingCount(clothing) < 5)) {
									return new Response("Buy (5) ("+UtilText.formatAsMoneyUncoloured(sellPrice*5, "span")+")", UtilText.parse(inventoryNPC, "[npc.Name] doesn't have five "+clothing.getNamePlural()+"."), null);
								}
								if(inventoryFull) {
									return new Response("Buy (5) ("+UtilText.formatAsMoneyUncoloured(sellPrice*5, "span")+")", "Your inventory is already full!", null);
								}
								if(Main.game.getPlayer().getMoney() < sellPrice*5) {
									return new Response("Buy (5) ("+UtilText.formatAsMoneyUncoloured(sellPrice*5, "span")+")", "You can't afford to buy this!", null);
								}
								return new Response("Buy (5) (" + UtilText.formatAsMoney(sellPrice*5, "span") + ")", "Buy the " + clothing.getName() + " for " + UtilText.formatAsMoney(sellPrice*5) + ".", INVENTORY_MENU){
									@Override
									public void effects(){
										sellClothing(inventoryNPC, Main.game.getPlayer(), clothing, 5, sellPrice);
									}
								};
								
							} else if(index == 3) {
								int sellPrice = buyback?Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getPrice():clothing.getPrice(inventoryNPC.getSellModifier(clothing));
								int count = buyback?Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getCount():inventoryNPC.getClothingCount(clothing);
								if(inventoryFull) {
									return new Response("Buy (All) ("+UtilText.formatAsMoneyUncoloured(sellPrice*count, "span")+")", "Your inventory is already full!", null);
								}
								if(Main.game.getPlayer().getMoney() < sellPrice*count) {
									int affordableCount = Main.game.getPlayer().getMoney() / sellPrice;
									if(affordableCount > 0) {
										return new Response("Buy (Max " + affordableCount + ") (" + UtilText.formatAsMoney(sellPrice * affordableCount, "span") + ")",
												"Buy the " + clothing.getName() + " for " + UtilText.formatAsMoney(sellPrice * affordableCount) + ".", INVENTORY_MENU) {
											@Override
											public void effects() {
												sellClothing(inventoryNPC, Main.game.getPlayer(), clothing, affordableCount, sellPrice);
											}
										};
									} else {
										return new Response("Buy (All) ("+UtilText.formatAsMoneyUncoloured(sellPrice*count, "span")+")", "You can't afford to buy this!", null);
									}
								}
								return new Response("Buy (All) (" + UtilText.formatAsMoney(sellPrice*count, "span") + ")",
										"Buy the " + clothing.getName() + " for " + UtilText.formatAsMoney(sellPrice*count) + ".", INVENTORY_MENU){
									@Override
									public void effects(){
										sellClothing(inventoryNPC, Main.game.getPlayer(), clothing, count, sellPrice);
									}
								};
								
							} else if(index == 4) {
								return new Response("Dye", UtilText.parse(inventoryNPC, "[npc.Name] isn't going to let you dye the clothing that [npc.sheIs] trying to sell!"), null);
								
							} else if(index == 5) {
								if(clothing.isCondom()) {
									if(clothing.getCondomEffect().getPotency().isNegative()) {
										return new Response("Repair (<i>1 Essence</i>)", "You can't repair someone else's condom!", null);
									}
									return new Response("Sabotage", "You can't sabotage someone else's condom!", null);
								}
								return new Response("Enchant", "You can't enchant someone else's clothing!", null);
								
							} else if(index == 6) {
								return new Response("Equip (Self)", UtilText.parse(inventoryNPC, "[npc.Name] isn't going to let you use [npc.her] clothing without buying them first."), null);
								
							} else if (index == 9) {
								return getBuybackResponse();
								
							} else if (index == 10) {
								return getQuickTradeResponse();
								
							} else if(index == 11) {
								return new Response(UtilText.parse(inventoryNPC, "Equip ([npc.HerHim])"), UtilText.parse(inventoryNPC, "[npc.Name] isn't going to use the clothing that [npc.sheIs] trying to sell!"), null);
								
							} else {
								return null;
							}
					}
				}
			}
			return null;
			
		}

		@Override
		public DialogueNodeType getDialogueNodeType() {
			return DialogueNodeType.INVENTORY;
		}
	};

	public static final DialogueNode WEAPON_EQUIPPED = new DialogueNode("Weapon equipped", "", true) {

		@Override
		public String getLabel() {
			if (Main.game.getDialogueFlags().values.contains(DialogueFlagValue.quickTrade) && !Main.game.isInSex() && !Main.game.isInCombat()) {
				return "Inventory (Quick-Manage is <b style='color:" + PresetColour.GENERIC_GOOD.toWebHexString() + ";'>ON</b>)";
			} else {
				return "Inventory";
			}
		}

		@Override
		public String getHeaderContent() {
			return inventoryView();
		}

		@Override
		public String getContent() {
			StringBuilder sb = new StringBuilder();
			List<String> extraDescriptions = weapon.getExtraDescriptions(owner);
			if(!extraDescriptions.isEmpty()) {
				sb.append("<p>");
					for(int i=0 ; i<extraDescriptions.size() ; i++) {
						sb.append(extraDescriptions.get(i));
						if(i<extraDescriptions.size()-1) {
							sb.append("<br/>");
						}
					}
				sb.append("</p>");
			}
			return getItemDisplayPanel(weapon.getSVGEquippedString(owner),
					Util.capitaliseSentence(weapon.getDisplayName(true)),
					weapon.getDescription(owner)
					 	+sb.toString());
		}

		public String getResponseTabTitle(int index) {
			return getGeneralResponseTabTitle(index);
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if (index == 0) {
				return getCloseInventoryResponse();
			}
			if(responseTab==0) {
				return INVENTORY_MENU.getResponse(responseTab, index);
			}

			// ****************************** ITEM BELONGS TO THE PLAYER ******************************
			if(owner != null && owner.isPlayer()) {
				switch(interactionType) {
					case COMBAT:
						if (index == 1) {
							if(Main.game.getPlayer().getLocationPlace().isItemsDisappear()) {
								return new Response("Drop", "You can't change weapons while fighting someone!", null);

							} else {
								return new Response("Store", "You can't change weapons while fighting someone!", null);
							}

						} else if (index==4) {
							return new Response("Dye/Reforge", "You can't alter the properties of "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" weapons in combat!", null);

						} else if(index == 5) {
							return new Response("Enchant", "You can't enchant equipped weapons!", null);

						} if(index == 6) {
							return new Response("Unequip", "You can't change weapons while fighting someone!", null);

						} else if (index == 10) {
							return getQuickTradeResponse();

						} else {
							return null;
						}

					case FULL_MANAGEMENT: case TRADING: case CHARACTER_CREATION:
						if (index == 1) {
							boolean areaFull = Main.game.isPlayerTileFull() && !Main.game.getPlayerCell().getInventory().hasWeapon(weapon);
							if(Main.game.getPlayer().getLocationPlace().isItemsDisappear()) {
								if(!weapon.getWeaponType().isAbleToBeDropped()) {
									return new Response("Drop", "You cannot drop the " + weapon.getName() + "!", null);

								} else if(areaFull) {
									return new Response("Drop", "This area is full, so you can't drop your " + weapon.getName() + " here!", null);
								} else {
									return new Response("Drop", "Drop your " + weapon.getName() + ".", INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append(
													"<p style='text-align:center;'>"
														+ (Main.game.getPlayer().unequipWeapon(weaponSlot, weapon, true, true))
													+ "</p>");
											resetPostAction();
										}
									};
								}

							} else {
								if(!weapon.getWeaponType().isAbleToBeDropped()) {
									return new Response("Store", "You cannot drop the " + weapon.getName() + "!", null);

								} else if(areaFull) {
									return new Response("Store", "This area is full, so you can't store your " + weapon.getName() + " here!", null);
								} else {
									return new Response("Store", "Store your " + weapon.getName() + " in this area.", INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append(
													"<p style='text-align:center;'>"
														+ (Main.game.getPlayer().unequipWeapon(weaponSlot, weapon, true, true))
													+ "</p>");
											resetPostAction();
										}
									};
								}
							}

						} else if (index==4) {
							if (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH)
									|| Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER)
									|| Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
								return new Response("Dye/Reforge",
										Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
											?"Use your proficiency with [style.colourEarth(Earth spells)] to alter this weapon's properties."
											:"Use a dye-brush or reforge hammer to alter this weapon's properties.",
										DYE_EQUIPPED_WEAPON) {
									@Override
									public void effects() {
										resetWeaponDyeColours();
									}
								};
							} else {
								return new Response("Dye", "You need a dye-brush or reforge hammer in order to alter this weapon's properties.", null);
							}

						} else if(index == 5) {
							return new Response("Enchant", "You can't enchant equipped weapons!", null);

						} else if(index == 6) {
							return new Response("Unequip", "Unequip the " + weapon.getName() + ".", INVENTORY_MENU){
								@Override
								public void effects(){
									Main.game.getTextEndStringBuilder().append(
											"<p style='text-align:center;'>"
												+ (Main.game.getPlayer().unequipWeapon(weaponSlot, weapon, false, true))
											+ "</p>");
									resetPostAction();
								}
							};

						} else if (index == 10) {
							return getQuickTradeResponse();

						} else {
							return null;
						}

					case SEX:
						if (index == 1) {
							boolean areaFull = Main.game.isPlayerTileFull() && !Main.game.getPlayerCell().getInventory().hasWeapon(weapon);
							if(Main.game.getPlayer().getLocationPlace().isItemsDisappear()) {
								if(!weapon.getWeaponType().isAbleToBeDropped()) {
									return new Response("Drop", "You cannot drop the " + weapon.getName() + "!", null);

								} else if(areaFull) {
									return new Response("Drop", "This area is full, so you can't drop your " + weapon.getName() + " here!", null);
								} else {
									return new Response("Drop", "Drop your " + weapon.getName() + ".", Main.sex.SEX_DIALOGUE){
										@Override
										public void effects(){
											Main.sex.setUnequipWeaponText(weapon,
													"<p style='text-align:center;'>"
														+ (Main.game.getPlayer().unequipWeapon(weaponSlot, weapon, true, true))
													+ "</p>");
											resetPostAction();
											Main.mainController.openInventory();
											Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
											Main.sex.setSexStarted(true);
										}
									};
								}

							} else {
								if(!weapon.getWeaponType().isAbleToBeDropped()) {
									return new Response("Store", "You cannot drop the " + weapon.getName() + "!", null);

								} else if(areaFull) {
									return new Response("Store", "This area is full, so you can't store your " + weapon.getName() + " here!", null);
								} else {
									return new Response("Store", "Store your " + weapon.getName() + " in this area.", Main.sex.SEX_DIALOGUE){
										@Override
										public void effects(){
											Main.sex.setUnequipWeaponText(weapon,
													"<p style='text-align:center;'>"
														+ (Main.game.getPlayer().unequipWeapon(weaponSlot, weapon, true, true))
													+ "</p>");
											resetPostAction();
											Main.mainController.openInventory();
											Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
											Main.sex.setSexStarted(true);
										}
									};
								}
							}

						} else if (index==4) {
							return new Response("Dye/Reforge", "You can't alter the properties of "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" weapons in sex!", null);

						} else if(index == 5) {
							return new Response("Enchant", "You can't enchant equipped weapons!", null);

						} else if(index == 6) {
							return new Response("Unequip", "Unequip the " + weapon.getName() + ".", Main.sex.SEX_DIALOGUE){
								@Override
								public void effects(){
									Main.sex.setUnequipWeaponText(weapon,
											"<p style='text-align:center;'>"
												+ (Main.game.getPlayer().unequipWeapon(weaponSlot, weapon, false, true))
											+ "</p>");
									resetPostAction();
									Main.mainController.openInventory();
									Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
									Main.sex.setSexStarted(true);
								}
							};

						} else if (index == 10) {
							return getQuickTradeResponse();

						} else {
							return null;
						}
				}

			// ****************************** ITEM DOES NOT BELONG TO PLAYER ******************************

			} else {
				switch(interactionType) {
					case COMBAT:
						if(index == 1) {
							return new Response("Drop", "You can't make someone drop their weapon while fighting them!", null);

						} else if(index == 2) {
							return new Response("Take", "You can't take someone's weapon while fighting them!", null);

						} else if (index==4) {
							return new Response("Dye/Reforge", "You can't alter the properties of someone else's equipped weapons while you're fighting them!", null);

						} else if(index == 5) {
							return new Response("Enchant", "You can't enchant someone else's equipped weapon, especially not while fighting them!", null);

						} else if (index == 6) {
							return new Response("Unequip", "You can't unequip someone's weapon while fighting them!", null);

						} else if (index == 10) {
							return getQuickTradeResponse();

						} else {
							return null;
						}

					case FULL_MANAGEMENT:  case CHARACTER_CREATION:
						boolean inventoryFull = Main.game.getPlayer().isInventoryFull() && !Main.game.getPlayer().hasWeapon(weapon) && weapon.getRarity()!=Rarity.QUEST;

						if (index == 1) {
							boolean areaFull = Main.game.isPlayerTileFull() && !Main.game.getPlayerCell().getInventory().hasWeapon(weapon);
							if(Main.game.getPlayer().getLocationPlace().isItemsDisappear()) {
								if(!weapon.getWeaponType().isAbleToBeDropped()) {
									return new Response("Drop", UtilText.parse(inventoryNPC, "[npc.Name] cannot drop [npc.her] " + weapon.getName() + "!"), null);

								} else if(areaFull) {
									return new Response("Drop", UtilText.parse(inventoryNPC, "This area is full, so [npc.name] can't drop [npc.her] " + weapon.getName() + " here!"), null);
								} else {
									return new Response("Drop", UtilText.parse(inventoryNPC, "Get [npc.name] to drop [npc.her] " + weapon.getName() + "."), INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append(
													"<p style='text-align:center;'>"
														+ (inventoryNPC.unequipWeapon(weaponSlot, weapon, true, false))
													+ "</p>");
											resetPostAction();
										}
									};
								}

							} else {
								if(!weapon.getWeaponType().isAbleToBeDropped()) {
									return new Response("Drop", UtilText.parse(inventoryNPC, "[npc.Name] cannot drop [npc.her] " + weapon.getName() + "!"), null);

								} else if(areaFull) {
									return new Response("Store", UtilText.parse(inventoryNPC, "This area is full, so [npc.name] can't store [npc.her] " + weapon.getName() + " here!"), null);
								} else {
									return new Response("Store", UtilText.parse(inventoryNPC, "Get [npc.name] to store [npc.her] " + weapon.getName() + " in this area."), INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append(
													"<p style='text-align:center;'>"
														+ (inventoryNPC.unequipWeapon(weaponSlot, weapon, true, false))
													+ "</p>");
											resetPostAction();
										}
									};
								}
							}

						} else if (index == 2) {
							if(inventoryFull) {
								return new Response("Take", "Your inventory is full, so you can't take this!", null);

							} else {
								return new Response("Take",
										UtilText.parse(inventoryNPC, "Take [npc.namePos] " + weapon.getName() + " and add it to your inventory."),
										INVENTORY_MENU){
									@Override
									public void effects(){
										inventoryNPC.unequipWeaponIntoVoid(weaponSlot, weapon, true);
										Main.game.getTextEndStringBuilder().append(
												"<p style='text-align:center;'>"
													+ (Main.game.getPlayer().addWeapon(weapon, false))
												+ "</p>");
										resetPostAction();
									}
								};
							}

						} else if (index==4) {
							if (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH)
									|| Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER)
									|| Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
								return new Response("Dye/Reforge",
										Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
											?"Use your proficiency with [style.colourEarth(Earth spells)] to alter this weapon's properties."
											:"Use a dye-brush or reforge hammer to alter this weapon's properties.",
										DYE_EQUIPPED_WEAPON) {
									@Override
									public void effects() {
										resetWeaponDyeColours();
									}
								};
							} else {
								return new Response("Dye", UtilText.parse(inventoryNPC, "You'll need to find a dye-brush or reforge hammer if you want to alter the properties of [npc.namePos] weapons."), null);
							}

						} else if(index == 5) {
							return new Response("Enchant", "You can't enchant equipped weapons!", null);

						} else if(index == 6) {
							if(!weapon.getWeaponType().isAbleToBeDropped()) {
								return new Response("Unequip", UtilText.parse(inventoryNPC, "As it's a unique weapon, the " + weapon.getName() + " cannot be unequipped into [npc.namePos] inventory."), null);
							}
							return new Response("Unequip", UtilText.parse(inventoryNPC, "Get [npc.name] to unequip [npc.her] " + weapon.getName() + "."), INVENTORY_MENU){
								@Override
								public void effects(){
									Main.game.getTextEndStringBuilder().append(
											"<p style='text-align:center;'>"
												+ (inventoryNPC.unequipWeapon(weaponSlot, weapon, false, false))
											+ "</p>");
									resetPostAction();
								}
							};

						} else if (index == 10) {
							return getQuickTradeResponse();

						} else {
							return null;
						}

					case SEX:
						if(index == 1) {
							return new Response("Drop", "You can't unequip someone's weapon while having sex with them!", null);

						} else if (index == 2) {
							return new Response("Take", "You can't take someone's weapon while having sex with them!", null);

						} else if (index==4) {
							return new Response("Dye/Reforge", UtilText.parse(inventoryNPC, "You can't alter the properties of [npc.namePos] weapons in sex!"), null);

						} else if(index == 5) {
							return new Response("Enchant", "You can't enchant equipped weapons!", null);

						} else if(index == 6) {
							return new Response("Unequip", "You can't unequip someone's weapon while having sex with them!", null);

						} else if (index == 10) {
							return getQuickTradeResponse();

						} else {
							return null;
						}

					case TRADING:
						if(index == 1) {
							return new Response("Drop", "You can't make someone drop their weapon!", null);

						} else if (index == 2) {
							return new Response("Take", "You can't take someone else's weapon!", null);

						} else if (index==4) {
							return new Response("Dye/Reforge", UtilText.parse(inventoryNPC, "You can't alter the properties of [npc.namePos] weapons!"), null);

						} else if(index == 5) {
							return new Response("Enchant", "You can't enchant someone else's equipped weapon!", null);

						} else if(index == 6) {
							return new Response("Unequip", "You can't unequip someone else's weapon!", null);

						} else if (index == 10) {
							return getQuickTradeResponse();

						} else {
							return null;
						}
					}

				}
				return null;
		}

		@Override
		public DialogueNodeType getDialogueNodeType() {
			return DialogueNodeType.INVENTORY;
		}
	};
	public static final DialogueNode CLOTHING_EQUIPPED = new DialogueNode("Clothing equipped", "", true) {

		@Override
		public String getLabel() {
			if(!Main.game.isInNewWorld()) {
				return "Evening's Attire";
			}

			if (Main.game.getDialogueFlags().values.contains(DialogueFlagValue.quickTrade) && !Main.game.isInSex() && !Main.game.isInCombat()) {
				return "Inventory (Quick-Manage is <b style='color:" + PresetColour.GENERIC_GOOD.toWebHexString() + ";'>ON</b>)";
			} else {
				return "Inventory";
			}
		}

		@Override
		public String getHeaderContent() {
			return inventoryView();
		}

		@Override
		public String getContent() {
			StringBuilder sb = new StringBuilder();
			sb.append(clothing.getDescription());
			sb.append("<p>");
				GameCharacter descriptionTarget = owner; //Main.game.isInSex()?owner:Main.game.getPlayer()
				for(String s : clothing.getExtraDescriptions(descriptionTarget, null, true)) {
					sb.append(s+"<br/>");
				}
				for(String s : clothing.getExtraDescriptions(descriptionTarget, clothing.getSlotEquippedTo(), true)) {
					sb.append(s+"<br/>");
				}
			sb.append("</p>");
			sb.append(Main.game.isInSex()||Main.game.isInCombat()?clothing.getDisplacementBlockingDescriptions(owner):"");

			return getItemDisplayPanel(clothing.getSVGEquippedString(owner), clothing.getDisplayName(true), sb.toString())
						+(interactionType==InventoryInteraction.CHARACTER_CREATION?CharacterCreation.getCheckingClothingDescription():"");
		}

		public String getResponseTabTitle(int index) {
			return getGeneralResponseTabTitle(index);
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if (index == 0) {
				return getCloseInventoryResponse();
			}
			if(responseTab==0) {
				return INVENTORY_MENU.getResponse(responseTab, index);
			}
			
			// ****************************** ITEM BELONGS TO THE PLAYER ******************************
			if(owner != null && owner.isPlayer()) {
				InventorySlot slotEquippedTo = clothing.getSlotEquippedTo();
				switch(interactionType) {
					case COMBAT:
						if (index == 1) {
							if(Main.game.getPlayer().getLocationPlace().isItemsDisappear()) {
								return new Response("Drop", "You cannot drop the " + clothing.getName() + " while fighting someone!", null);
								
							} else {
								return new Response("Store", "You cannot drop the " + clothing.getName() + " while fighting someone!", null);
							}
							
						} else if (index==4) {
							return new Response("Dye", "You can't dye "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" clothes in combat!", null);
							
						} else if(index == 5) {
							if(clothing.isSealed()) {
								return getJinxRemovalResponse(true);
									
							} else {
								if(clothing.isCondom()) {
									if(clothing.getCondomEffect().getPotency().isNegative()) {
										return new Response("Repair (<i>1 Essence</i>)", "You can't repair equipped condoms!", null);
									}
									return new Response("Sabotage", "You can't sabotage equipped condoms!", null);
								}
								return new Response("Enchant", "You can't enchant equipped clothing!", null);
							}
							
						} else if(index == 6 && !clothing.isDiscardedOnUnequip(slotEquippedTo)) {
							return new Response("Unequip", "You can't unequip the " + clothing.getName() + " during combat!", null);
							
						} else if (index == 10) {
							return getQuickTradeResponse();
							
						} else if (index > 10 && index - 11 < clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).size()){
								return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index -11).getDescription()),
										"You can't "+clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index -11).getDescription()
										+ " the " + clothing.getName() + " during combat!", null);
								
							
						} else {
							
							return null;
						}
						
					case FULL_MANAGEMENT: case TRADING:
						if (index == 1) {
							boolean areaFull = Main.game.isPlayerTileFull() && !Main.game.getPlayerCell().getInventory().hasClothing(clothing);
							if(Main.game.getPlayer().getLocationPlace().isItemsDisappear()) {
								if(!clothing.getClothingType().isAbleToBeDropped()) {
									return new Response("Drop", "You cannot drop the " + clothing.getName() + "!", null);
									
								} else if(areaFull && !clothing.isDiscardedOnUnequip(slotEquippedTo)) {
									return new Response("Drop", "This area is full, so you can't drop "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" " + clothing.getName() + " here!", null);
								} else {
									return new Response((clothing.isDiscardedOnUnequip(slotEquippedTo)?"Discard":"Drop"),
											(clothing.isDiscardedOnUnequip(slotEquippedTo)
													?"Take off "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" " + clothing.getName() + " and throw it away."
													:"Drop "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" " + clothing.getName() + "."),
											INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + unequipClothingToFloor(Main.game.getPlayer(), clothing) + "</p>");
										}
									};
								}
								
							} else {
								if(!clothing.getClothingType().isAbleToBeDropped()) {
									return new Response("Store", "You cannot drop the " + clothing.getName() + "!", null);
									
								} else if(areaFull && !clothing.isDiscardedOnUnequip(slotEquippedTo)) {
									return new Response("Store", "This area is full, so you can't store "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" " + clothing.getName() + " here!", null);
								} else {
									return new Response((clothing.isDiscardedOnUnequip(slotEquippedTo)?"Discard":"Store"),
											(clothing.isDiscardedOnUnequip(slotEquippedTo)
													?"Take off "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" " + clothing.getName() + " and throw it away."
													:"Store "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" " + clothing.getName() + " in this area."),
											INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + unequipClothingToFloor(Main.game.getPlayer(), clothing) + "</p>");
										}
									};
								}
							}
							
						} else if (index==4) {
							if (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
								return new Response("Dye", 
										Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
											?"Use your proficiency with [style.colourEarth(Earth spells)] to dye this item."
											:"Use a dye-brush to dye this item of clothing.",
										DYE_EQUIPPED_CLOTHING) {
									@Override
									public void effects() {
										resetClothingDyeColours();
									}
								};
							} else {
								return new Response("Dye", "You need a dye-brush in order to dye this item of clothing.", null);
							}
							
						} else if(index == 5) {
							if(clothing.isSealed()) {
								return getJinxRemovalResponse(true);
								
							} else {
								if(clothing.isCondom()) {
									if(clothing.getCondomEffect().getPotency().isNegative()) {
										return new Response("Repair (<i>1 Essence</i>)", "You can't repair equipped condoms!", null);
									}
									return new Response("Sabotage", "You can't sabotage equipped condoms!", null);
								}
								return new Response("Enchant", "You can't enchant equipped clothing!", null);
							}
							
						} else if(index == 6 && !clothing.isDiscardedOnUnequip(slotEquippedTo)) {
							if (owner.isAbleToUnequip(clothing, true, Main.game.getPlayer())) {
								return new Response("Unequip", "Unequip the " + clothing.getName() + ".", INVENTORY_MENU){
									@Override
									public void effects(){
										Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + unequipClothingToInventory(Main.game.getPlayer(), clothing) + "</p>");
									}
								};
								
							} else {
								return new Response("Unequip", getClothingBlockingRemovalText("unequip"), null);
							}
							
						} else if (index == 10) {
							return getQuickTradeResponse();
							
						} else if (index > 10 && index - 11 < clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).size()){
							
							if (clothing.getDisplacedList().contains(clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index - 11))) {
								return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index - 11).getOppositeDescription()),
										Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index - 11).getOppositeDescription()) + " the " + clothing.getName() + ". "
												+ clothing.getClothingBlockingDescription(
														clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index - 11),
														Main.game.getPlayer(),
														clothing.getSlotEquippedTo(),
														" <span style='color:" + PresetColour.GENERIC_GOOD.toWebHexString() + ";'>This will cover "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" ",
														".</span>"),
												CLOTHING_EQUIPPED){
									@Override
									public void effects(){
										Main.game.getPlayer().isAbleToBeReplaced(clothing, clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index - 11), true, true, Main.game.getPlayer());
									}
								};
							} else {
								return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index - 11).getDescription()),
										Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index - 11).getDescription()) + " the " + clothing.getName() + ". "
												+ clothing.getClothingBlockingDescription(
														clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index - 11),
														Main.game.getPlayer(),
														clothing.getSlotEquippedTo(),
														" <span style='color:" + PresetColour.GENERIC_SEX.toWebHexString() + ";'>This will expose "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" ",
														".</span>"),
												CLOTHING_EQUIPPED){
									@Override
									public void effects(){
										Main.game.getPlayer().isAbleToBeDisplaced(clothing, clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index - 11), true, true, Main.game.getPlayer());
									}
								};
							}
							
						} else {
							return null;
						}
						
					case CHARACTER_CREATION:
						if (index == 1) {
							if(Main.game.getPlayer().isCoverableAreaVisible(CoverableArea.NIPPLES)
									|| Main.game.getPlayer().isCoverableAreaVisible(CoverableArea.ANUS)
									|| Main.game.getPlayer().isCoverableAreaVisible(CoverableArea.PENIS)
									|| Main.game.getPlayer().isCoverableAreaVisible(CoverableArea.VAGINA)
									|| (Main.game.getPlayer().getClothingInSlot(InventorySlot.FOOT)==null && Main.game.getPlayer().getLegType().equals(LegType.HUMAN))) {
								return new Response("To the stage", "You need to be wearing clothing that covers your body, as well as a pair of shoes.", null);
								
							} else {
								return new Response("To the stage", "You're ready to approach the stage now.", CharacterCreation.CHOOSE_BACKGROUND) {
									@Override
									public void effects() {
										CharacterCreation.moveNPCIntoPlayerTile();
									}
								};
							}
							
						} else if(index == 4){
							if(Main.game.getPlayer().getClothingCurrentlyEquipped().isEmpty()){
								return new Response("Unequip all", "You're currently naked, there's nothing to be unequipped.", null);
							}
							else{
								return new Response("Unequip all", "Remove as much of your clothing as possible.", INVENTORY_MENU){
									@Override
									public void effects(){
										Main.game.getTextEndStringBuilder().append(unequipAll(Main.game.getPlayer()));
									}
								};
							}
							
						} else if(index == 5) {
							return new Response("Change colour", "Change the colour of this item of clothing.", DYE_EQUIPPED_CLOTHING_CHARACTER_CREATION) {
								@Override
								public void effects() {
									resetClothingDyeColours();
								}
							};
							
						} else if(index == 6) {
							return new Response("Unequip", "Unequip the " + clothing.getName() + ".", INVENTORY_MENU){
								@Override
								public void effects(){
									unequipClothingToFloor(Main.game.getPlayer(), clothing);
								}
							};
								
						} else {
							return null;
						}
						
					case SEX:
						if (index == 1) {
							if(clothing.isDiscardedOnUnequip(slotEquippedTo) && !Main.sex.getSexManager().isAbleToRemoveSelfClothing(Main.game.getPlayer())) {
								return new Response("Discard", "You can't unequip the " + clothing.getName() + " in this sex scene!", null);
							}
							boolean areaFull = Main.game.isPlayerTileFull() && !Main.game.getPlayerCell().getInventory().hasClothing(clothing);
							if(Main.game.getPlayer().getLocationPlace().isItemsDisappear()) {
								if(!clothing.getClothingType().isAbleToBeDropped()) {
									return new Response("Drop", "You cannot drop the " + clothing.getName() + "!", null);
									
								} else if(areaFull && !clothing.isDiscardedOnUnequip(slotEquippedTo)) {
									return new Response("Drop", "This area is full, so you can't drop "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" " + clothing.getName() + " here!", null);
									
								} else {
									if (owner.isAbleToUnequip(clothing, false, Main.game.getPlayer())) {
										return new Response((clothing.isDiscardedOnUnequip(slotEquippedTo)?"Discard":"Drop"),
												(clothing.isDiscardedOnUnequip(slotEquippedTo)
														?"Take off "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" " + clothing.getName() + " and throw it away."
														:"Drop "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" " + clothing.getName() + "."),
												Main.sex.SEX_DIALOGUE){
											@Override
											public void effects(){
												GameCharacter unequipOwner = owner;
												AbstractClothing c = clothing;
												unequipClothingToFloor(Main.game.getPlayer(), clothing);
												Main.sex.setUnequipClothingText(c, unequipOwner.getUnequipDescription());
												Main.mainController.openInventory();
												Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
												Main.sex.setSexStarted(true);
											}
										};
									} else {
										return new Response("Drop", getClothingBlockingRemovalText("unequip"), null);
									}
								}
								
							} else {
								if(!clothing.getClothingType().isAbleToBeDropped()) {
									return new Response("Store", "You cannot drop the " + clothing.getName() + "!", null);
									
								} else if(areaFull && !clothing.isDiscardedOnUnequip(slotEquippedTo)) {
									return new Response("Store", "This area is full, so you can't store "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" " + clothing.getName() + " here!", null);
									
								} else {
									if (owner.isAbleToUnequip(clothing, false, Main.game.getPlayer())) {
										return new Response((clothing.isDiscardedOnUnequip(slotEquippedTo)?"Discard":"Store"),
												(clothing.isDiscardedOnUnequip(slotEquippedTo)
														?"Take off "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" " + clothing.getName() + " and throw it away."
														:"Drop "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" " + clothing.getName() + "."),
												Main.sex.SEX_DIALOGUE){
											@Override
											public void effects(){
												GameCharacter unequipOwner = owner;
												AbstractClothing c = clothing;
												unequipClothingToFloor(Main.game.getPlayer(), clothing);
												Main.sex.setUnequipClothingText(c, unequipOwner.getUnequipDescription());
												Main.mainController.openInventory();
												Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
												Main.sex.setSexStarted(true);
											}
										};
									} else {
										return new Response("Store", getClothingBlockingRemovalText("unequip"), null);
									}
								}
							}
							
						} else if (index==4) {
							return new Response("Dye", "You can't dye "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" clothes in sex!", null);
							
						} else if(index == 5) {
							if(clothing.isSealed()) {
								return getJinxRemovalResponse(true);
								
							} else {
								if(clothing.isCondom()) {
									if(clothing.getCondomEffect().getPotency().isNegative()) {
										return new Response("Repair (<i>1 Essence</i>)", "You can't repair equipped condoms!", null);
									}
									return new Response("Sabotage", "You can't sabotage equipped condoms!", null);
								}
								return new Response("Enchant", "You can't enchant equipped clothing!", null);
							}
							
						} else if(index == 6 && !clothing.isDiscardedOnUnequip(slotEquippedTo)) {
							if(!Main.sex.getSexManager().isAbleToRemoveSelfClothing(Main.game.getPlayer())) {
								return new Response("Unequip", "You can't unequip the " + clothing.getName() + " in this sex scene!", null);
							}
							
							if (owner.isAbleToUnequip(clothing, false, Main.game.getPlayer())) {
								return new Response("Unequip", "Unequip the " + clothing.getName() + ".", Main.sex.SEX_DIALOGUE){
									@Override
									public void effects(){
										AbstractClothing c = clothing;
										unequipClothingToInventory(Main.game.getPlayer(), clothing);
										Main.sex.setUnequipClothingText(c, owner.getUnequipDescription());
										Main.mainController.openInventory();
										Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
										Main.sex.setSexStarted(true);
									}
								};
							} else {
								return new Response("Unequip", getClothingBlockingRemovalText("unequip"), null);
							}
							
						} else if (index == 10) {
							return getQuickTradeResponse();
							
						} else if (index > 10 && index - 11 < clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).size()) {
							if (clothing.getDisplacedList().contains(clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index - 11))) {
								return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index -11).getDescription()),
										"The "+ clothing.getName()+ " "
										+(clothing.getClothingType().isPlural()?"have":"has")+" already been "
												+ clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index -11).getDescriptionPast() + "!", null);
								
							} else {
								if(!Main.sex.getSexManager().isAbleToRemoveSelfClothing(Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index - 11).getDescription()),
											"You can't can't "+clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index -11).getDescription()
											+ " "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" " + clothing.getName() + " in this sex scene!", null);
								}
								
								if(owner.isAbleToBeDisplaced(clothing, clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index -11), false, false, Main.game.getPlayer())) {
									return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index - 11).getDescription()),
											Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index - 11).getDescription()) + " the " + clothing.getName() + ". "
													+ clothing.getClothingBlockingDescription(
															clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index - 11),
															Main.game.getPlayer(),
															clothing.getSlotEquippedTo(),
															" <span style='color:" + PresetColour.GENERIC_SEX.toWebHexString() + ";'>This will expose "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" ",
															".</span>"),
													Main.sex.SEX_DIALOGUE){
										@Override
										public void effects(){
											Main.game.getPlayer().isAbleToBeDisplaced(clothing, clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index - 11), true, true, Main.game.getPlayer());
											Main.sex.setDisplaceClothingText(clothing, owner.getDisplaceDescription());
											Main.mainController.openInventory();
											Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
											Main.sex.setSexStarted(true);
										}
									};
								
								} else {
									return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index -11).getDescription()),
											"You can't "+clothing.getBlockedPartsKeysAsListWithoutNONE(owner, clothing.getSlotEquippedTo()).get(index -11).getDescription()
											+ " the " + clothing.getName() + ", as "+UtilText.parse(owner, "[npc.namePos] ")
											+owner.getBlockingClothing().getName()+" "+(owner.getBlockingClothing().getClothingType().isPlural()?"are":"is")+" in the way!", null);
								}
							}
							
						} else {
							return null;
						}
				}
				
			// ****************************** ITEM DOES NOT BELONG TO PLAYER ******************************
				
			} else {
				InventorySlot slotEquippedTo = clothing.getSlotEquippedTo();
				switch(interactionType) {
					case COMBAT:
						if (index == 1) {
							return new Response("Drop", "You can't make someone drop their clothing while fighting them!", null);
							
						} else if (index==4) {
							return new Response("Dye", "You can't dye someone else's equipped clothing while you're fighting them!", null);
							
						} else if(index == 5) {
							if(clothing.isSealed()) {
								return getJinxRemovalResponse(false);
								
							} else {
								if(clothing.isCondom()) {
									if(clothing.getCondomEffect().getPotency().isNegative()) {
										return new Response("Repair (<i>1 Essence</i>)", "You can't repair equipped condoms!", null);
									}
									return new Response("Sabotage", "You can't sabotage equipped condoms!", null);
								}
								return new Response("Enchant", "You can't enchant equipped clothing!", null);
							}
							
						} else if(index == 6) {
							return new Response("Unequip", "You can't unequip someone's clothing while fighting them!", null);
							
						} else if (index == 10) {
							return getQuickTradeResponse();
							
						} else if (index > 10 && index - 11 < clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).size()){
							if (clothing.getDisplacedList().contains(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11))) {
								return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index -11).getDescription()),
										"The "+ clothing.getName()+ " "
										+(clothing.getClothingType().isPlural()?"have":"has")+" already been "
												+ clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index -11).getDescriptionPast() + "!", null);
								
							} else {
								return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index -11).getDescription()),
										"You can't "+clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index -11).getDescription() + " the " + clothing.getName() + " while in a fight!", null);
							}
							
						} else {
							return null;
						}
						
					case FULL_MANAGEMENT: case CHARACTER_CREATION:
						boolean inventoryFull = Main.game.getPlayer().isInventoryFull() && !Main.game.getPlayer().hasClothing(clothing) && clothing.getRarity()!=Rarity.QUEST;
						
						if (index == 1) {
							boolean areaFull = Main.game.isPlayerTileFull() && !Main.game.getPlayerCell().getInventory().hasClothing(clothing);
							if(Main.game.getPlayer().getLocationPlace().isItemsDisappear()) {
								if(!clothing.getClothingType().isAbleToBeDropped()) {
									return new Response("Drop", "You cannot drop the " + clothing.getName() + "!", null);
									
								} else if(areaFull && !clothing.isDiscardedOnUnequip(slotEquippedTo)) {
									return new Response("Drop", UtilText.parse(inventoryNPC, "This area is full, so you can't drop [npc.namePos] " + clothing.getName() + " here!"), null);
								} else {
									return new Response((clothing.isDiscardedOnUnequip(slotEquippedTo)?"Discard":"Drop"),
											(clothing.isDiscardedOnUnequip(slotEquippedTo)
													?UtilText.parse(inventoryNPC, "Take off [npc.namePos] " + clothing.getName() + " and throw it away.")
													:UtilText.parse(inventoryNPC, "Drop [npc.namePos] " + clothing.getName() + ".")),
											INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + unequipClothingToFloor(Main.game.getPlayer(), clothing) + "</p>");
										}
									};
								}
								
							} else {
								if(!clothing.getClothingType().isAbleToBeDropped()) {
									return new Response("Store", "You cannot drop the " + clothing.getName() + "!", null);
									
								} else if(areaFull && !clothing.isDiscardedOnUnequip(slotEquippedTo)) {
									return new Response("Store", UtilText.parse(inventoryNPC, "This area is full, so you can't store [npc.namePos] " + clothing.getName() + " here!"), null);
								} else {
									return new Response((clothing.isDiscardedOnUnequip(slotEquippedTo)?"Discard":"Store"),
											(clothing.isDiscardedOnUnequip(slotEquippedTo)
													?UtilText.parse(inventoryNPC, "Take off [npc.namePos] " + clothing.getName() + " and throw it away.")
													:UtilText.parse(inventoryNPC, "Store [npc.namePos] " + clothing.getName() + " in this area.")),
											INVENTORY_MENU){
										@Override
										public void effects(){
											Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + unequipClothingToFloor(Main.game.getPlayer(), clothing) + "</p>");
										}
									};
								}
							}
							
						} else if(index==2) {
							if(inventoryFull) {
								return new Response("Take", "Your inventory is full, so you can't take this!", null);
								
							} else if(clothing.isDiscardedOnUnequip(slotEquippedTo)) {
								return new Response("Take", "This piece of clothing is automatically discarded when unequipped, so you can't take it!", null);
								
							} else {
								return new Response("Take",
										UtilText.parse(inventoryNPC, "Take [npc.namePos] " + clothing.getName() + " and add it to your inventory."),
										INVENTORY_MENU){
									@Override
									public void effects(){
										Main.game.getTextEndStringBuilder().append(
												"<p style='text-align:center;'>"
													+ unequipClothingToUnequippersInventory(Main.game.getPlayer(), clothing)
												+ "</p>");
									}
								};
							}
							
						} else if (index==4) {
							if (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
								return new Response("Dye", 
										Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
											?"Use your proficiency with [style.colourEarth(Earth spells)] to dye this item."
											:"Use a dye-brush to dye this item of clothing.",
										DYE_EQUIPPED_CLOTHING) {
									@Override
									public void effects() {
										resetClothingDyeColours();
									}
								};
								
							} else {
								return new Response("Dye", UtilText.parse(inventoryNPC, "You'll need to find a dye-brush if you want to dye [npc.namePos] clothes."), null);
							}
							
						} else if(index == 5) {
							if(clothing.isSealed()) {
								return getJinxRemovalResponse(false);
								
							} else {
								if(clothing.isCondom()) {
									if(clothing.getCondomEffect().getPotency().isNegative()) {
										return new Response("Repair (<i>1 Essence</i>)", "You can't repair equipped condoms!", null);
									}
									return new Response("Sabotage", "You can't sabotage equipped condoms!", null);
								}
								return new Response("Enchant", "You can't enchant equipped clothing!", null);
							}
							
						} else if(index == 6 && !clothing.isDiscardedOnUnequip(slotEquippedTo)) {
							if(!clothing.getClothingType().isAbleToBeDropped()) {
								return new Response("Unequip", UtilText.parse(inventoryNPC, "As it's a unique item, the " + clothing.getName() + " cannot be unequipped into [npc.namePos] inventory."), null);
							}
							return new Response("Unequip", "Unequip the " + clothing.getName() + ".", INVENTORY_MENU){
								@Override
								public void effects(){
									Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + unequipClothingToInventory(Main.game.getPlayer(), clothing) + "</p>");
								}
							};
							
						} else if (index == 10) {
							return getQuickTradeResponse();
							
						} else if (index > 10 && index - 11 < clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).size()){
							
							if (clothing.getDisplacedList().contains(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11))) {
								return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11).getOppositeDescription()),
										Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11).getOppositeDescription()) + " the " + clothing.getName() + ". "
												+ clothing.getClothingBlockingDescription(
														clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11),
														owner,
														clothing.getSlotEquippedTo(),
														" <span style='color:" + PresetColour.GENERIC_GOOD.toWebHexString() + ";'>This will cover "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" ",
														".</span>"),
												CLOTHING_EQUIPPED){
									@Override
									public void effects(){
										owner.isAbleToBeReplaced(clothing, clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11), true, true, Main.game.getPlayer());
									}
								};
							} else {
								return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11).getDescription()),
										Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11).getDescription()) + " the " + clothing.getName() + ". "
												+ clothing.getClothingBlockingDescription(
														clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11),
														owner,
														clothing.getSlotEquippedTo(),
														" <span style='color:" + PresetColour.GENERIC_SEX.toWebHexString() + ";'>This will expose "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" ",
														".</span>"),
												CLOTHING_EQUIPPED){
									@Override
									public void effects(){
										owner.isAbleToBeDisplaced(clothing, clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11), true, true, Main.game.getPlayer());
										Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + owner.getDisplaceDescription() + "</p>");
									}
								};
							}
							
						} else {
							return null;
						}
						
					case SEX:
						if (index == 1) {
							if(clothing.isDiscardedOnUnequip(slotEquippedTo) && !Main.sex.getSexManager().isAbleToRemoveOthersClothing(Main.game.getPlayer(), clothing)) {
								return new Response("Discard", "You can't unequip the " + clothing.getName() + " in this sex scene!", null);
							}
							boolean areaFull = Main.game.isPlayerTileFull() && !Main.game.getPlayerCell().getInventory().hasClothing(clothing);
							if(Main.game.getPlayer().getLocationPlace().isItemsDisappear()) {
								if(!clothing.getClothingType().isAbleToBeDropped()) {
									return new Response("Drop", "You cannot drop the " + clothing.getName() + "!", null);
									
								} else if(!Main.sex.getSexManager().isAbleToRemoveOthersClothing(Main.game.getPlayer(), clothing)) {
									return new Response("Drop", UtilText.parse(inventoryNPC, "You can't unequip the " + clothing.getName() + " in this sex scene!"), null);
									
								} else if(areaFull && !clothing.isDiscardedOnUnequip(slotEquippedTo)) {
									return new Response("Drop", UtilText.parse(inventoryNPC, "This area is full, so you can't drop [npc.namePos] " + clothing.getName() + " here!"), null);
									
								} else {
									if (owner.isAbleToUnequip(clothing, false, Main.game.getPlayer())) {
										return new Response((clothing.isDiscardedOnUnequip(slotEquippedTo)?"Discard":"Drop"),
											(clothing.isDiscardedOnUnequip(slotEquippedTo)
													?UtilText.parse(inventoryNPC, "Take off [npc.namePos] " + clothing.getName() + " and throw it away.")
													:UtilText.parse(inventoryNPC, "Drop [npc.namePos] " + clothing.getName() + ".")),
												Main.sex.SEX_DIALOGUE){
											@Override
											public void effects(){
												AbstractClothing c = clothing;
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + unequipClothingToFloor(Main.game.getPlayer(), clothing) + "</p>");
												Main.sex.setUnequipClothingText(c, inventoryNPC.getUnequipDescription());
												Main.mainController.openInventory();
												Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
												Main.sex.setSexStarted(true);
											}
										};
									} else {
										return new Response("Drop", getClothingBlockingRemovalText("unequip"), null);
									}
								}
								
							} else {
								if(!clothing.getClothingType().isAbleToBeDropped()) {
									return new Response("Store", "You cannot drop the " + clothing.getName() + "!", null);
									
								} else if(!Main.sex.getSexManager().isAbleToRemoveOthersClothing(Main.game.getPlayer(), clothing)) {
									return new Response("Store", UtilText.parse(inventoryNPC, "You can't unequip the " + clothing.getName() + " in this sex scene!"), null);
									
								} else if(areaFull && !clothing.isDiscardedOnUnequip(slotEquippedTo)) {
									return new Response("Store", UtilText.parse(inventoryNPC, "This area is full, so you can't store [npc.namePos] " + clothing.getName() + " here!"), null);
								} else {
									if (owner.isAbleToUnequip(clothing, false, Main.game.getPlayer())) {
										return new Response((clothing.isDiscardedOnUnequip(slotEquippedTo)?"Discard":"Store"),
												(clothing.isDiscardedOnUnequip(slotEquippedTo)
														?UtilText.parse(inventoryNPC, "Take off [npc.namePos] " + clothing.getName() + " and throw it away.")
														:UtilText.parse(inventoryNPC, "Store [npc.namePos] " + clothing.getName() + " in this area.")),
												Main.sex.SEX_DIALOGUE){
											@Override
											public void effects(){
												AbstractClothing c = clothing;
												Main.game.getTextEndStringBuilder().append("<p style='text-align:center;'>" + unequipClothingToFloor(Main.game.getPlayer(), clothing) + "</p>");
												Main.sex.setUnequipClothingText(c, inventoryNPC.getUnequipDescription());
												Main.mainController.openInventory();
												Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
												Main.sex.setSexStarted(true);
											}
										};
									} else {
										return new Response("Store", getClothingBlockingRemovalText("unequip"), null);
									}
								}
							}
							
						} else if (index==4) {
							return new Response("Dye", UtilText.parse(inventoryNPC, "You can't dye [npc.namePos] clothes in sex!"), null);
							
						} else if(index == 5) {
							if(clothing.isSealed()) {
								return getJinxRemovalResponse(false);
								
							} else {
								if(clothing.isCondom()) {
									if(clothing.getCondomEffect().getPotency().isNegative()) {
										return new Response("Repair (<i>1 Essence</i>)", "You can't repair equipped condoms!", null);
									}
									return new Response("Sabotage", "You can't sabotage equipped condoms!", null);
								}
								return new Response("Enchant", "You can't enchant equipped clothing!", null);
							}
							
						} else if(index == 6 && !clothing.isDiscardedOnUnequip(slotEquippedTo)) {
							if(!Main.sex.getSexManager().isAbleToRemoveOthersClothing(Main.game.getPlayer(), clothing)) {
								return new Response("Unequip", "You can't unequip the " + clothing.getName() + " in this sex scene!", null);
							}
							
							if (owner.isAbleToUnequip(clothing, false, Main.game.getPlayer())) {
								return new Response("Unequip", "Unequip the " + clothing.getName() + ".", Main.sex.SEX_DIALOGUE){
									@Override
									public void effects(){
										AbstractClothing c = clothing;
										unequipClothingToInventory(Main.game.getPlayer(), clothing);
										Main.sex.setUnequipClothingText(c, inventoryNPC.getUnequipDescription());
										Main.mainController.openInventory();
										Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
										Main.sex.setSexStarted(true);
									}
								};
							} else {
								return new Response("Unequip", getClothingBlockingRemovalText("unequip"), null);
							}
							
						} else if (index == 10) {
							return getQuickTradeResponse();
							
						} else if (index > 10 && index - 11 < clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).size()){
							if (clothing.getDisplacedList().contains(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11))) {
								return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index -11).getDescription()),
										"The "+ clothing.getName()+ " "
										+(clothing.getClothingType().isPlural()?"have":"has")+" already been "
												+ clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index -11).getDescriptionPast() + "!", null);
								
							} else {
								if(owner.isAbleToBeDisplaced(clothing, clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index -11), false, false, Main.game.getPlayer())){
									
									if(!Main.sex.getSexManager().isAbleToRemoveOthersClothing(Main.game.getPlayer(), clothing)) {
										return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11).getDescription()),
												"You "+clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index -11).getDescription() + " the " + clothing.getName() + " in this sex scene!", null);
									}
									
									return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11).getDescription()),
											Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11).getDescription()) + " the " + clothing.getName() + ". "
													+ clothing.getClothingBlockingDescription(
															clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11),
															owner,
															clothing.getSlotEquippedTo(),
															" <span style='color:" + PresetColour.GENERIC_SEX.toWebHexString() + ";'>This will expose "+(owner.isPlayer()?"your":owner.getName("")+"'s")+" ",
															".</span>"),
													Main.sex.SEX_DIALOGUE){
										@Override
										public void effects(){
											owner.isAbleToBeDisplaced(clothing, clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11), true, true, Main.game.getPlayer());
											Main.sex.setDisplaceClothingText(clothing, owner.getDisplaceDescription());
											Main.mainController.openInventory();
											Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
											Main.sex.setSexStarted(true);
										}
									};
								
								} else {
									return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index -11).getDescription()),
											"You can't "+clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index -11).getDescription()
											+ " the " + clothing.getName() + ", as "+UtilText.parse(owner, "[npc.namePos] ")
											+owner.getBlockingClothing().getName()+" "+(owner.getBlockingClothing().getClothingType().isPlural()?"are":"is")+" in the way!", null);
								}
							}
							
						} else {
							return null;
						}
						
						case TRADING:
							if (index == 1) {
								return new Response("Drop", UtilText.parse(inventoryNPC, "You can't make [npc.name] drop [npc.her] clothing!"), null);
								
							} else if (index==4) {
								return new Response("Dye", UtilText.parse(inventoryNPC, "You can't dye [npc.namePos] clothes!"), null);
								
							}  else if(index == 5) {
								if(clothing.isCondom()) {
									if(clothing.getCondomEffect().getPotency().isNegative()) {
										return new Response("Repair (<i>1 Essence</i>)", "You can't repair [npc.namePos] condom!", null);
									}
									return new Response("Sabotage", "You can't sabotage [npc.namePos] condom!", null);
								}
								return new Response("Enchant", UtilText.parse(inventoryNPC, "You can't enchant [npc.namePos] clothing!"), null);
								
							} else if(index == 6) {
								return new Response("Unequip", UtilText.parse(inventoryNPC, "You can't unequip [npc.namePos] clothing!"), null);
								
							} else if (index == 10) {
								return getQuickTradeResponse();
								
							} else if (index > 10 && index - 11 < clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).size()){
								if (clothing.getDisplacedList().contains(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index - 11))) {
									return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index -11).getDescription()),
											"The "+ clothing.getName()+ " "
													+(clothing.getClothingType().isPlural()?"have":"has")+" already been "
													+ clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index -11).getDescriptionPast() + "!", null);
									
								} else {
									return new Response(Util.capitaliseSentence(clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index -11).getDescription()),
											"You can't "+clothing.getBlockedPartsKeysAsListWithoutNONE(inventoryNPC, clothing.getSlotEquippedTo()).get(index -11).getDescription() + " the " + clothing.getName() + "!", null);
								}
								
							} else {
								return null;
							}
					}
				
				}
				return null;
		}

		@Override
		public DialogueNodeType getDialogueNodeType() {
			return DialogueNodeType.INVENTORY;
		}
	};

	private static String getClothingDyeUI() {
		InventorySlot slotEquippedTo = clothing.getSlotEquippedTo();
		if(slotEquippedTo==null) {
			slotEquippedTo = clothing.getClothingType().getEquipSlots().get(0);
		}

		inventorySB = new StringBuilder(
				"<div class='container-full-width'>"
					+ "<div class='inventoryImage'>"
						+ "<div class='inventoryImage-content'>"
							+ clothing.getSVGString()
						+ "</div>"
					+ "</div>"
					+ "<h3 style='text-align:center;'><b>"+clothing.getDisplayName(true)+"</b></h3>"
					+ "<p>"
						+ "Select the desired colours from the coloured buttons below, and after using the preview to see how the new clothing will look, press the 'Dye' option at the bottom of the screen to apply your changes."
					+ "</p>"
				+ "</div>"

				+ "<br/>"

				+ "<div class='container-full-width'>"
					+ "<div class='inventoryImage'>"
						+ "<div class='inventoryImage-content'>"
							+ clothing.getClothingType().getSVGImage(slotEquippedTo,
									dyePreviews,
									dyePreviewPattern,
									dyePreviewPatterns,
									getDyePreviewStickersAsStrings())
						+ "</div>"
					+ "</div>");

		inventorySB.append("<h3 style='text-align:center;'><b>Dye & Preview</b></h3>");

		if(!clothing.getClothingType().getStickers().isEmpty()) { //TODO test stickers
			StringBuilder stickerSB = new StringBuilder();
			boolean stickerFound = false;
			List<StickerCategory> orderedCategories = new ArrayList<>(clothing.getClothingType().getStickers().keySet());
			Collections.sort(orderedCategories, (s1, s2)->s1.getPriority()-s2.getPriority());

			for(StickerCategory cat : orderedCategories) {
				stickerSB.append("<div class='container-quarter-width' style='width:calc(75% - 16px); margin:0 8px; padding:0;'>");
					stickerSB.append("<div class='container-quarter-width' style='margin:0; padding-top:6px; width:20%;'>");
						stickerSB.append(Util.capitaliseSentence(cat.getName())+":"); // Category name
					stickerSB.append("</div>");

					stickerSB.append("<div class='container-quarter-width' style='margin:0; padding:0; width:80%;'>");
						List<Sticker> orderedStickers = new ArrayList<>(clothing.getClothingType().getStickers().get(cat));
						Collections.sort(orderedStickers, (s1, s2)->s1.getPriority()-s2.getPriority());
						for(Sticker sticker : orderedStickers) {
							String requirements = UtilText.parse(sticker.getUnavailabilityText()).trim();
							if(requirements.isEmpty() || sticker.isShowDisabledButton()) {
								boolean specialSticker = !sticker.getAvailabilityText().isEmpty() || !sticker.getTagsApplied().isEmpty() || !sticker.getTagsRemoved().isEmpty();
								stickerFound = true;
								String id = "ITEM_STICKER_"+cat.getId()+sticker.getId();
								if(!requirements.isEmpty()) {
									stickerSB.append(
											"<div id='"+id+"' class='cosmetics-button disabled'>"
													+ "<b style='color:" + PresetColour.TEXT_GREY.toWebHexString() + ";'>" + Util.capitaliseSentence(sticker.getName()) + (specialSticker?"*":"") + "</b>"
											+ "</div>");

								} else if(dyePreviewStickers.get(cat)==sticker) {
									stickerSB.append(
											"<div id='"+id+"' class='cosmetics-button active'>"
													+ "<b style='color:" + sticker.getColourSelected().toWebHexString() + ";'>" + Util.capitaliseSentence(sticker.getName()) + (specialSticker?"*":"") + "</b>"
											+ "</div>");
								} else {
									stickerSB.append(
											"<div id='"+id+"' class='cosmetics-button'>"
													+ "<span style='color:"+sticker.getColourDisabled().toWebHexString()+";'>" + Util.capitaliseSentence(sticker.getName()) + (specialSticker?"*":"") + "</span>"
											+ "</div>");
								}
							}
						}
					stickerSB.append("</div>");
				stickerSB.append("</div>");

				if(stickerFound) {
					stickerFound = false;
					inventorySB.append(stickerSB.toString());
					stickerSB = new StringBuilder();
				}
			}
		}

		for(int i=0; i<clothing.getClothingType().getColourReplacements().size(); i++) {
			ColourReplacement cr = clothing.getClothingType().getColourReplacement(i);
			if(!cr.getAllColours().isEmpty()) {
				inventorySB.append("<div class='container-quarter-width' style='width:calc(75% - 16px);'>"
							+ Util.capitaliseSentence(Util.intToPrimarySequence(i+1))+" Colour"+(cr.isRecolouringAllowed()?"":" ([style.italicsBad(cannot be changed)])")+":<br/>");

				for(Colour c : cr.getAllColours()) {
					inventorySB.append("<div class='normal-button"+(dyePreviews.size()>i && dyePreviews.get(i)==c?" selected":"")+"' id='DYE_CLOTHING_"+i+"_"+c.getId()+"'"
										+ " style='width:auto; margin-right:4px; border-width:1px;"
											+(cr.getDefaultColours().contains(c)
												?"border-color:"+PresetColour.TEXT_GREY.toWebHexString()+";"
												:"")
											+(dyePreviews.size()>i && dyePreviews.get(i)==c
												?" background-color:"+PresetColour.BASE_GREEN.getShades()[4]+";"
												:"")+"'>"
									+ "<div class='phone-item-colour' style='"
										+ (c.isMetallic()
												?"background: repeating-linear-gradient(135deg, " + c.toWebHexString() + ", " + c.getShades()[4] + " 10px);"
												:"background-color:" + c.toWebHexString() + ";")
										+ "'></div>"
						+ "</div>");
				}
				inventorySB.append("</div>");
			}
		}

		if(clothing.getClothingType().isPatternAvailable()){
			inventorySB.append(
					"<br/>"
					+ "<div class='container-full-width'>"
					+ "Pattern:<br/>");

			for (Pattern pattern : Pattern.getAllPatterns()) {
				if (dyePreviewPattern.equals(pattern.getId())) {
					inventorySB.append(
							"<div class='cosmetics-button active'>"
								+ "<b style='color:" + PresetColour.GENERIC_GOOD.toWebHexString() + ";'>" + Util.capitaliseSentence(pattern.getNiceName()) + "</b>"
							+ "</div>");
				} else {
					inventorySB.append(
							"<div id='ITEM_PATTERN_"+pattern.getId()+"' class='cosmetics-button'>"
							+ "<span style='color:"+PresetColour.TRANSFORMATION_GENERIC.getShades()[0]+";'>" + Util.capitaliseSentence(pattern.getNiceName()) + "</span>"
							+ "</div>");
				}
			}
			inventorySB.append("</div>");

			for(int i=0; i<clothing.getClothingType().getPatternColourReplacements().size(); i++) {
				ColourReplacement cr = clothing.getClothingType().getPatternColourReplacement(i);
				if(!cr.getAllColours().isEmpty() && Pattern.getPattern(dyePreviewPattern).isRecolourAvailable(cr)) {
					inventorySB.append("<div class='container-quarter-width' style='width:calc(75% - 16px);'>"
								+ "Pattern "+Util.capitaliseSentence(Util.intToPrimarySequence(i+1))+" Colour:<br/>");

					for (Colour c : cr.getAllColours()) {
						inventorySB.append("<div class='normal-button"+(dyePreviewPatterns.size()>i && dyePreviewPatterns.get(i)==c?" selected":"")+"' id='DYE_CLOTHING_PATTERN_"+i+"_"+c.getId()+"'"
											+ " style='width:auto; margin-right:4px;"+(dyePreviewPatterns.size()>i && dyePreviewPatterns.get(i)==c?" background-color:"+PresetColour.BASE_GREEN.getShades()[4]+";":"")+"'>"
										+ "<div class='phone-item-colour' style='"
											+ (c.isMetallic()
													?"background: repeating-linear-gradient(135deg, " + c.toWebHexString() + ", " + c.getShades()[4] + " 10px);"
													:"background-color:" + c.toWebHexString() + ";")
											+ "'></div>"
							+ "</div>");
					}
					inventorySB.append("</div>");
				}
			}
		}
		inventorySB.append("</div>");

		return inventorySB.toString();
	}

	private static String getWeaponDyeUI() {
		inventorySB = new StringBuilder(
				"<div class='container-full-width'>"
					+ "<div class='inventoryImage'>"
						+ "<div class='inventoryImage-content'>"
							+ weapon.getSVGString()
						+ "</div>"
					+ "</div>"
					+ "<h3 style='text-align:center;'><b>"+weapon.getDisplayName(true)+"</b></h3>"
					+ "<p>"
						+ "Select the desired colours from the coloured buttons below, and after using the preview to see how the new weapon will look, press the 'Dye' option at the bottom of the screen to apply your changes."
					+ "</p>"
				+ "</div>"
				+ "<br/>"
				+ "<div class='container-full-width'>"
					+ "<div class='container-full-width' style='text-align:center; width:calc(25% - 16px); float:right;'>"
						+ "<div class='inventoryImage' style='width:100%;'>"
							+ (weapon.getWeaponType().isEquippedSVGImageDifferent()
								?"Unequipped"
								:"")
							+ "<div class='inventoryImage-content'>"
								+ weapon.getWeaponType().getSVGImage(damageTypePreview, dyePreviews)
							+ "</div>"
						+ "</div>"
						+(weapon.getWeaponType().isEquippedSVGImageDifferent()
							?"<div class='inventoryImage' style='width:100%;'>"
								+ "Equipped"
									+ "<div class='inventoryImage-content'>"
										+ weapon.getWeaponType().getSVGEquippedImage(damageTypePreview, dyePreviews)
									+ "</div>"
								+ "</div>"
							:"")
					+ "</div>"
					+ "<h3 style='text-align:center;'><b>Dye & Preview</b></h3>");


		inventorySB.append("<div class='container-quarter-width' style='text-align:center;width:calc(75% - 16px);'>"
				+ "<b>Damage type:</b><br/>");
		for(DamageType dt : weapon.getWeaponType().getAvailableDamageTypes()) {
			inventorySB.append("<div class='normal-button"+(damageTypePreview==dt?" selected":"")+"' id='DAMAGE_TYPE_"+dt.toString()+"'"
							+ "style='width:20%; margin:0 2.5%; color:"+(damageTypePreview==dt?dt.getColour().toWebHexString():dt.getColour().getShades(8)[0])+";'>"
						+ Util.capitaliseSentence(dt.getName())
					+ "</div>");
		}
		inventorySB.append("</div>");

		boolean colourOptions = false;

		for(int i=0; i<weapon.getWeaponType().getColourReplacements(false).size(); i++) {
			colourOptions = true;
			ColourReplacement cr = weapon.getWeaponType().getColourReplacement(false, i);
			if(!cr.getAllColours().isEmpty()) {
				inventorySB.append("<div class='container-quarter-width' style='width:calc(75% - 16px);'>"
						+ Util.capitaliseSentence(Util.intToPrimarySequence(i+1))+" Colour"+(cr.isRecolouringAllowed()?"":" ([style.italicsBad(cannot be changed)])")+":<br/>");

				for(Colour c : cr.getAllColours()) {
					inventorySB.append("<div class='normal-button"+(dyePreviews.size()>i && dyePreviews.get(i)==c?" selected":"")+"' id='DYE_WEAPON_"+i+"_"+c.getId()+"'"
										+ " style='width:auto; margin-right:4px; border-width:1px;"
											+(cr.getDefaultColours().contains(c)
												?"border-color:"+PresetColour.TEXT_GREY.toWebHexString()+";"
												:"")
											+(dyePreviews.size()>i && dyePreviews.get(i)==c
												?" background-color:"+PresetColour.BASE_GREEN.getShades()[4]+";"
												:"")
										+"'>"
									+ "<div class='phone-item-colour' style='"
										+ (c.isMetallic()
												?"background: repeating-linear-gradient(135deg, " + c.toWebHexString() + ", " + c.getShades()[4] + " 10px);"
												:"background-color:" + c.toWebHexString() + ";")
										+ "'></div>"
						+ "</div>");
				}
				inventorySB.append("</div>");
			}
		}

		if(!colourOptions) {
			inventorySB.append("<div class='container-half-width' style='text-align:center;'>"
					+ "[style.colourDisabled(No dye options available...)]"
					+ "</div>");
		}

		inventorySB.append("</div>");

		return inventorySB.toString();
	}

	public static final DialogueNode DYE_CLOTHING = new DialogueNode("Dye clothing", "", true) {

		@Override
		public String getContent() {
			return getClothingDyeUI();
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if (index == 0) {
				return new Response("Back", "Return to the previous menu.", INVENTORY_MENU);

			} else if (index == 1) {
				if(dyePreviews.equals(clothing.getColours())
						&& dyePreviewPattern.equals(clothing.getPattern())
						&& dyePreviewPatterns.equals(clothing.getPatternColours())
						&& dyePreviewStickers.equals(clothing.getStickersAsObjects())) {
					return new Response("Dye",
							"You need to choose different colours before being able to dye the " + clothing.getName() + "!",
							null);
				}

				return new Response("Dye",
						"Dye the " + clothing.getName() + " in the colours you have chosen."
								+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
										?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can dye it a different colour at any time."
										:" This action is permanent, and you'll need another dye-brush if you want to change its colour again."),
						INVENTORY_MENU) {
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().useItem(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH), owner, false);
							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getDyeBrushEffects(clothing, dyePreviews.get(0))
									+ "</p>"
									+ "<p>"
										+ "<b>The " + clothing.getName() + " " + (clothing.getClothingType().isPlural() ? "have been" : "has been") + " dyed</b>!"
									+ "</p>"
									+ "<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH))
														+ "</b> dye-brush" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH)) == 1 ? "" : "es") + " left!"
												:"You have <b>0</b> dye-brushes left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
											+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to dye the " + clothing.getName() + " without needing to use a dye-brush!"
										+ "</p>");
						}

						if(owner!=null) {
							owner.removeClothing(clothing);
							AbstractClothing dyedClothing = new AbstractClothing(clothing) {};
							dyedClothing.setColours(dyePreviews);
							dyedClothing.setPattern(dyePreviewPattern);
							dyedClothing.setPatternColours(dyePreviewPatterns);
							dyedClothing.setStickersAsObjects(dyePreviewStickers);
							owner.addClothing(dyedClothing, false);
							Main.game.addEvent(new EventLogEntry("Dyed", dyedClothing.getDisplayName(true)), false);

						} else {
							Main.game.getPlayerCell().getInventory().removeClothing(clothing);
							AbstractClothing dyedClothing = new AbstractClothing(clothing) {};
							dyedClothing.setColours(dyePreviews);
							dyedClothing.setPattern(dyePreviewPattern);
							dyedClothing.setPatternColours(dyePreviewPatterns);
							dyedClothing.setStickersAsObjects(dyePreviewStickers);
							Main.game.getPlayerCell().getInventory().addClothing(dyedClothing);
							Main.game.addEvent(new EventLogEntry("Dyed", dyedClothing.getDisplayName(true)), false);
						}

					}
				};

			} else if (index == 6) {
				if(dyePreviews.equals(clothing.getColours())
						&& dyePreviewPattern.equals(clothing.getPattern())
						&& dyePreviewPatterns.equals(clothing.getPatternColours())
						&& dyePreviewStickers.equals(clothing.getStickersAsObjects())) {
					return new Response("Dye all (stack)",
							"You need to choose different colours before being able to dye the " + clothing.getName() + "!",
							null);
				}

				int stackCount = 0;
				if(owner!=null) {
					stackCount = owner.getClothingCount(clothing);
				} else {
					stackCount = Main.game.getPlayerCell().getInventory().getClothingCount(clothing);
				}

				int finalCount = stackCount;

				if(stackCount==1) {
					return new Response("Dye all (stack)",
							"You only have one "+clothing.getName()+", so you should dye it using the individual action...",
							null);
				}

				if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
					int dyeBrushCount = Main.game.getPlayer().getItemCount(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH));

					if(dyeBrushCount<stackCount) {
						return new Response("Dye all (stack)",
								"You do not have enough dye brushes to dye all the " + clothing.getNamePlural() + "! You have "+dyeBrushCount+" dye brushes, but there are "+stackCount+" "+clothing.getNamePlural()+" in this stack...",
								null);
					}
				}

				return new Response("Dye all (stack)",
						"Dye all " + clothing.getNamePlural() + " which are in this clothing stack ("+stackCount+" in total) in the colours you have chosen."
								+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
										?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can dye them a different colour at any time."
										:" This action is permanent, and you'll need another dye-brush if you want to change their colour again."),
						INVENTORY_MENU) {
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().removeItem(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH), finalCount);
							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getDyeBrushEffects(clothing, dyePreviews.get(0))
									+ "</p>"
									+ "<p>"
										+ "<b>The "+clothing.getName()+(clothing.getClothingType().isPlural()?"have":"has")+" been dyed</b>!"
									+ "</p>");

							Main.game.getTextEndStringBuilder().append("<p>You then repeat this for the other "+Util.intToString(finalCount-1)+" "+clothing.getNamePlural()+"...</p>");

							Main.game.getTextEndStringBuilder().append("<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH))
														+ "</b> dye-brush" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH)) == 1 ? "" : "es") + " left!"
												:"You have <b>0</b> dye-brushes left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
											+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to dye "
												+ (finalCount==2
													?"both"
													:"all "+Util.intToString(finalCount))
												+" of the " + clothing.getNamePlural() + " without needing to use a single dye-brush!"
										+ "</p>");
						}

						if(owner!=null) {
							owner.removeClothing(clothing, finalCount);
							AbstractClothing dyedClothing = new AbstractClothing(clothing) {};
							dyedClothing.setColours(dyePreviews);
							dyedClothing.setPattern(dyePreviewPattern);
							dyedClothing.setPatternColours(dyePreviewPatterns);
							dyedClothing.setStickersAsObjects(dyePreviewStickers);
							owner.addClothing(dyedClothing, finalCount, false, false);
							Main.game.addEvent(new EventLogEntry("Dyed", dyedClothing.getDisplayName(true)), false);

						} else {
							Main.game.getPlayerCell().getInventory().removeClothing(clothing, finalCount);
							AbstractClothing dyedClothing = new AbstractClothing(clothing) {};
							dyedClothing.setColours(dyePreviews);
							dyedClothing.setPattern(dyePreviewPattern);
							dyedClothing.setPatternColours(dyePreviewPatterns);
							dyedClothing.setStickersAsObjects(dyePreviewStickers);
							Main.game.getPlayerCell().getInventory().addClothing(dyedClothing, finalCount);
							Main.game.addEvent(new EventLogEntry("Dyed", dyedClothing.getDisplayName(true)), false);
						}
					}
				};

			} else if (index == 11) {
				if(dyePreviews.equals(clothing.getColours())
						&& dyePreviewPattern.equals(clothing.getPattern())
						&& dyePreviewPatterns.equals(clothing.getPatternColours())
						&& dyePreviewStickers.equals(clothing.getStickersAsObjects())) {
					return new Response("Dye all",
							"You need to choose different colours before being able to dye the " + clothing.getName() + "!",
							null);
				}

				List<AbstractClothing> clothingMatches = new ArrayList<>();
				int stackCount = 0;
				if(owner!=null) {
					for(Entry<AbstractClothing, Integer> entry : owner.getAllClothingInInventory().entrySet()) {
						if(entry.getKey().getClothingType().equals(clothing.getClothingType())) {
							clothingMatches.add(entry.getKey());
							stackCount += entry.getValue();
						}
					}
				} else {
					for(Entry<AbstractClothing, Integer> entry : Main.game.getPlayerCell().getInventory().getAllClothingInInventory().entrySet()) {
						if(entry.getKey().getClothingType().equals(clothing.getClothingType())) {
							clothingMatches.add(entry.getKey());
							stackCount += entry.getValue();
						}
					}
				}

				int finalCount = stackCount;

				if(stackCount==1) {
					return new Response("Dye all",
							"You only have one "+clothing.getName()+", so you should dye it using the individual action...",
							null);
				}

				if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
					int dyeBrushCount = Main.game.getPlayer().getItemCount(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH));

					if(dyeBrushCount<stackCount) {
						return new Response("Dye all",
								"You do not have enough dye brushes to dye all the " + clothing.getNamePlural() + "! You have "+dyeBrushCount+" dye brushes, but there are "+stackCount+" "+clothing.getNamePlural()+" in total...",
								null);
					}
				}

				return new Response("Dye all",
						"Dye all " + clothing.getNamePlural() + " which are in this clothing stack ("+stackCount+" in total) in the colours you have chosen."
								+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
										?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can dye them a different colour at any time."
										:" This action is permanent, and you'll need another dye-brush if you want to change their colour again."),
						INVENTORY_MENU) {
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().removeItem(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH), finalCount);
							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getDyeBrushEffects(clothing, dyePreviews.get(0))
									+ "</p>"
									+ "<p>"
									+ "<b>The "+clothing.getName()+(clothing.getClothingType().isPlural()?"have":"has")+" been dyed</b>!"
									+ "</p>");

							Main.game.getTextEndStringBuilder().append("<p>You then repeat this for the other "+Util.intToString(finalCount-1)+" "+clothing.getNamePlural()+"...</p>");

							Main.game.getTextEndStringBuilder().append("<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH))
														+ "</b> dye-brush" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH)) == 1 ? "" : "es") + " left!"
												:"You have <b>0</b> dye-brushes left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
											+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to dye "
												+ (finalCount==2
													?"both"
													:"all "+Util.intToString(finalCount))
												+" of the " + clothing.getNamePlural() + " without needing to use a single dye-brush!"
										+ "</p>");
						}

						if(owner!=null) {
							for(AbstractClothing c : clothingMatches) {
								int clothingCount = owner.getAllClothingInInventory().get(c);
								owner.removeClothing(c, clothingCount);
								AbstractClothing dyedClothing = new AbstractClothing(c) {};
								dyedClothing.setColours(dyePreviews);
								dyedClothing.setPattern(dyePreviewPattern);
								dyedClothing.setPatternColours(dyePreviewPatterns);
								dyedClothing.setStickersAsObjects(dyePreviewStickers);
								owner.addClothing(dyedClothing, clothingCount, false, false);
								Main.game.addEvent(new EventLogEntry("Dyed", dyedClothing.getDisplayName(true)), false);
							}

						} else {
							for(AbstractClothing c : clothingMatches) {
								int clothingCount = Main.game.getPlayerCell().getInventory().getAllClothingInInventory().get(c);
								Main.game.getPlayerCell().getInventory().removeClothing(c, clothingCount);
								AbstractClothing dyedClothing = new AbstractClothing(c) {};
								dyedClothing.setColours(dyePreviews);
								dyedClothing.setPattern(dyePreviewPattern);
								dyedClothing.setPatternColours(dyePreviewPatterns);
								dyedClothing.setStickersAsObjects(dyePreviewStickers);
								Main.game.getPlayerCell().getInventory().addClothing(dyedClothing, clothingCount);
								Main.game.addEvent(new EventLogEntry("Dyed", dyedClothing.getDisplayName(true)), false);
							}
						}
					}
				};

			} else {
				return null;
			}
		}

		@Override
		public DialogueNodeType getDialogueNodeType() {
			return DialogueNodeType.INVENTORY;
		}
	};

	public static final DialogueNode DYE_EQUIPPED_CLOTHING = new DialogueNode("Dye clothing", "", true) {

		@Override
		public String getContent() {
			return getClothingDyeUI();
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if (index == 0) {
				return new Response("Back", "Return to the previous menu.", INVENTORY_MENU);

			} else if (index == 1) {
				if(dyePreviews.equals(clothing.getColours())
						&& dyePreviewPattern.equals(clothing.getPattern())
						&& dyePreviewPatterns.equals(clothing.getPatternColours())
						&& dyePreviewStickers.equals(clothing.getStickersAsObjects())) {
					return new Response("Dye",
							"You need to choose different colours before being able to dye the " + clothing.getName() + "!",
							null);
				}

				return new Response("Dye",
								"Dye the " + clothing.getName() + " in the colours you have chosen."
										+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can dye it a different colour at any time."
												:" This action is permanent, and you'll need another dye-brush if you want to change its colour again."),
								INVENTORY_MENU){
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().useItem(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH), owner, false);
							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getDyeBrushEffects(clothing, dyePreviews.get(0))
									+ "</p>"
									+ "<p>"
										+ "<b>The " + clothing.getName() + " " + (clothing.getClothingType().isPlural() ? "have been" : "has been") + " dyed</b>!"
									+ "</p>"
									+ "<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH))
														+ "</b> dye-brush" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH)) == 1 ? "" : "es") + " left!"
												:"You have <b>0</b> dye-brushes left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
											+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to dye the " + clothing.getName() + " without needing to use a dye-brush!"
										+ "</p>");
						}

						clothing.setColours(dyePreviews);
						clothing.setPattern(dyePreviewPattern);
						clothing.setPatternColours(dyePreviewPatterns);
						clothing.setStickersAsObjects(dyePreviewStickers);

						Main.game.addEvent(new EventLogEntry("Dyed", clothing.getDisplayName(true)), false);
					}
				};

			} else
				return null;
		}

		@Override
		public DialogueNodeType getDialogueNodeType() {
			return DialogueNodeType.INVENTORY;
		}
	};


	public static final DialogueNode DYE_CLOTHING_CHARACTER_CREATION = new DialogueNode("Choose Colour", "", true) {

		@Override
		public String getContent() {
			return getClothingDyeUI();
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if (index == 0) {
				return new Response("Back", "Return to the previous menu.", CLOTHING_INVENTORY);

			} else if (index == 1) {
				if(dyePreviews.equals(clothing.getColours())
						&& dyePreviewPattern.equals(clothing.getPattern())
						&& dyePreviewPatterns.equals(clothing.getPatternColours())
						&& dyePreviewStickers.equals(clothing.getStickersAsObjects())) {
					return new Response("Dye",
							"You need to choose different colours before being able to dye the " + clothing.getName() + "!",
							null);
				}

				return new Response("Dye",
						"Change the colour of the " + clothing.getName() + " to the colours you have chosen.",
						INVENTORY_MENU){
					@Override
					public void effects(){
						Main.game.getPlayerCell().getInventory().removeClothing(clothing);
						AbstractClothing dyedClothing = new AbstractClothing(clothing) {};
						dyedClothing.setColours(dyePreviews);
						dyedClothing.setPattern(dyePreviewPattern);
						dyedClothing.setPatternColours(dyePreviewPatterns);
						dyedClothing.setStickersAsObjects(dyePreviewStickers);
						clothing = dyedClothing;
						Main.game.getPlayerCell().getInventory().addClothing(dyedClothing);
					}
				};

			} else
				return null;
		}

		@Override
		public DialogueNodeType getDialogueNodeType() {
			return DialogueNodeType.INVENTORY;
		}
	};

	public static final DialogueNode DYE_EQUIPPED_CLOTHING_CHARACTER_CREATION = new DialogueNode("Choose Colour", "", true) {

		@Override
		public String getContent() {
			return getClothingDyeUI();
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if (index == 0) {
				return new Response("Back", "Return to the previous menu.", CLOTHING_EQUIPPED);

			} else if (index  == 1) {
				if(dyePreviews.equals(clothing.getColours())
						&& dyePreviewPattern.equals(clothing.getPattern())
						&& dyePreviewPatterns.equals(clothing.getPatternColours())
						&& dyePreviewStickers.equals(clothing.getStickersAsObjects())) {
					return new Response("Dye",
							"You need to choose different colours before being able to dye the " + clothing.getName() + "!",
							null);
				}

				return new Response("Dye",
						"Change the colour of the " + clothing.getName() + " to the colours you have chosen.",
						CLOTHING_EQUIPPED){
					@Override
					public void effects(){
						clothing.setColours(dyePreviews);
						clothing.setPattern(dyePreviewPattern);
						clothing.setPatternColours(dyePreviewPatterns);
						clothing.setStickersAsObjects(dyePreviewStickers);
					}
				};

			} else
				return null;
		}

		@Override
		public DialogueNodeType getDialogueNodeType() {
			return DialogueNodeType.INVENTORY;
		}
	};

	public static final DialogueNode DYE_WEAPON = new DialogueNode("Dye Weapon", "", true) {

		@Override
		public String getContent() {
			return getWeaponDyeUI();
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if (index == 0) {
				return new Response("Back", "Return to the previous menu.", INVENTORY_MENU);

			} else if (index == 1) {
				if (!Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH)
						&& !Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
					return new Response("Dye",
							"You do not have a dye-brush, so cannot change the colours of the " + weapon.getName() + "...",
							null);
				}

				if(dyePreviews.equals(weapon.getColours())) {
					return new Response("Dye",
							"You need to choose different colours before being able to dye the " + weapon.getName() + "!",
							null);
				}

				return new Response("Dye",
						"Dye the " + weapon.getName() + " in the colours you have chosen."
								+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
										?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can dye it a different colour at any time."
										:" This action is permanent, and you'll need another dye-brush if you want to change its colour again."),
						INVENTORY_MENU){
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().useItem(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH), owner, false);
							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getDyeBrushEffects(weapon, dyePreviews.get(0))
									+ "</p>"
									+ "<p>"
										+ "<b>The " + weapon.getName() + " " + (weapon.getWeaponType().isPlural() ? "have been" : "has been") + " dyed</b>!"
									+ "</p>"
									+ "<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH))
														+ "</b> dye-brush" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH)) == 1 ? "" : "es") + " left!"
												:"You have <b>0</b> dye-brushes left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
										+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to dye the " + weapon.getName() + " without needing to use a dye-brush!"
									+ "</p>");
						}

						if(owner!=null) {
							owner.removeWeapon(weapon);
							AbstractWeapon dyedWeapon = Main.game.getItemGen().generateWeapon(weapon);
							dyedWeapon.setColours(dyePreviews);
							owner.addWeapon(dyedWeapon, false);
							Main.game.addEvent(new EventLogEntry("Dyed", dyedWeapon.getDisplayName(true)), false);
							weapon = dyedWeapon;

						} else {
							Main.game.getPlayerCell().getInventory().removeWeapon(weapon);
							AbstractWeapon dyedWeapon = Main.game.getItemGen().generateWeapon(weapon);
							dyedWeapon.setColours(dyePreviews);
							Main.game.getPlayerCell().getInventory().addWeapon(dyedWeapon);
							Main.game.addEvent(new EventLogEntry("Dyed", dyedWeapon.getDisplayName(true)), false);
							weapon = dyedWeapon;
						}
					}
				};

			} else if (index == 2) {
				if (!Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER)
						&& !Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
					return new Response("Reforge",
							"You do not have a reforging hammer, so cannot change the damage type of the " + weapon.getName() + "...",
							null);
				}

				if(damageTypePreview == weapon.getDamageType()) {
					return new Response("Reforge",
							"You need to choose a different damage type before being able to reforge the " + weapon.getName() + "!",
							null);
				}

				return new Response("Reforge",
						"Reforge the " + weapon.getName() + " into the damage type you have chosen."
								+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
										?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can reforge it at any time."
										:" This action is permanent, and you'll need another reforging hammer if you want to change its damage type again."),
						INVENTORY_MENU){
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().useItem(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER), owner, false);

							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getReforgeHammerEffects(weapon, damageTypePreview)
									+ "</p>"
									+ "<p>"
										+ "<b>The " + weapon.getName() + " " + (weapon.getWeaponType().isPlural() ? "have been" : "has been") + " reforged</b>!"
									+ "</p>"
									+ "<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER))
														+ "</b> reforging " + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER)) == 1 ? "hammer" : "hammers") + " left!"
												:"You have <b>0</b> reforging hammers left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
											+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to reforge the " + weapon.getName() + " without needing to use a reforging hammer!"
										+ "</p>");
						}

						if(owner!=null) {
							owner.removeWeapon(weapon);
							AbstractWeapon modifiedWeapon = Main.game.getItemGen().generateWeapon(weapon);
							modifiedWeapon.setDamageType(damageTypePreview);
							// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
							owner.addWeapon(Main.game.getItemGen().generateWeapon(modifiedWeapon), false);
							Main.game.addEvent(new EventLogEntry("Reforged", modifiedWeapon.getDisplayName(true)), false);

						} else {
							Main.game.getPlayerCell().getInventory().removeWeapon(weapon);
							AbstractWeapon modifiedWeapon = Main.game.getItemGen().generateWeapon(weapon);
							modifiedWeapon.setDamageType(damageTypePreview);
							// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
							Main.game.getPlayerCell().getInventory().addWeapon(Main.game.getItemGen().generateWeapon(modifiedWeapon));
							Main.game.addEvent(new EventLogEntry("Reforged", modifiedWeapon.getDisplayName(true)), false);
						}
					}
				};

			} else if (index == 3) {
				if ((!Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER) || !Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH))
						&& !Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
					return new Response("Dye & reforge",
							"You do not have both a dye brush and a reforging hammer, so cannot dye and reforge the " + weapon.getName() + "...",
							null);
				}

				if(damageTypePreview == weapon.getDamageType()) {
					return new Response("Dye & reforge",
							"You need to choose a different damage type before being able to reforge the " + weapon.getName() + "!",
							null);
				}

				if(dyePreviews.equals(weapon.getColours())) {
					return new Response("Dye & reforge",
							"You need to choose different colours before being able to dye the " + weapon.getName() + "!",
							null);
				}

				return new Response("Dye & reforge",
						"Dye and reforge the " + weapon.getName() + " into the colours and damage type you have chosen."
								+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
										?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can dye and reforge it at any time."
										:" This action is permanent, and you'll need another dye-brush and another reforging hammer if you want to change its colours and damage type again."),
						INVENTORY_MENU){
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().useItem(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH), owner, false);
							Main.game.getPlayer().useItem(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER), owner, false);
							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getDyeBrushEffects(weapon, dyePreviews.get(0))
									+ "</p>"
									+ "<p style='text-align:center;'>"
										+ getReforgeHammerEffects(weapon, damageTypePreview)
									+ "</p>"
									+ "<p>"
										+ "<b>The " + weapon.getName() + " " + (weapon.getWeaponType().isPlural() ? "have been" : "has been") + " dyed and reforged</b>!"
									+ "</p>"
									+ "<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH))
														+ "</b> dye-brush" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH)) == 1 ? "" : "es") + " left!"
												:"You have <b>0</b> dye-brushes left!")
										+"<br/>"
										+ (Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER))
														+ "</b> reforging " + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER)) == 1 ? "hammer" : "hammers") + " left!"
												:"You have <b>0</b> reforging hammers left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
											+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to dye and reforge the " + weapon.getName() + " without needing to use a dye-brush or reforging hammer!"
										+ "</p>");
						}

						if(owner!=null) {
							owner.removeWeapon(weapon);
							AbstractWeapon modifiedWeapon = Main.game.getItemGen().generateWeapon(weapon);
							modifiedWeapon.setDamageType(damageTypePreview);
							modifiedWeapon.setColours(dyePreviews);
							// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
							owner.addWeapon(Main.game.getItemGen().generateWeapon(modifiedWeapon), false);
							Main.game.addEvent(new EventLogEntry("Dyed & Reforged", modifiedWeapon.getDisplayName(true)), false);

						} else {
							Main.game.getPlayerCell().getInventory().removeWeapon(weapon);
							AbstractWeapon modifiedWeapon = Main.game.getItemGen().generateWeapon(weapon);
							modifiedWeapon.setDamageType(damageTypePreview);
							modifiedWeapon.setColours(dyePreviews);
							// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
							Main.game.getPlayerCell().getInventory().addWeapon(Main.game.getItemGen().generateWeapon(modifiedWeapon));
							Main.game.addEvent(new EventLogEntry("Dyed & Reforged", modifiedWeapon.getDisplayName(true)), false);
						}
					}
				};

			} else if (index == 6) {
				if(dyePreviews.equals(weapon.getColours())) {
					return new Response("Dye all (stack)",
							"You need to choose different colours before being able to dye the " + weapon.getName() + "!",
							null);
				}

				int stackCount = 0;
				if(owner!=null) {
					stackCount = owner.getWeaponCount(weapon);
				} else {
					stackCount = Main.game.getPlayerCell().getInventory().getWeaponCount(weapon);
				}

				int finalCount = stackCount;

				if(stackCount==1) {
					return new Response("Dye all (stack)",
							"You only have one "+weapon.getName()+", so you should dye it using the individual action...",
							null);
				}

				if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
					int dyeBrushCount = Main.game.getPlayer().getItemCount(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH));

					if(dyeBrushCount<stackCount) {
						return new Response("Dye all (stack)",
								"You do not have enough dye brushes to dye all the " + weapon.getNamePlural() + "! You have "+dyeBrushCount+" dye brushes, but there are "+stackCount+" "+weapon.getNamePlural()+" in this stack...",
								null);
					}
				}

				return new Response("Dye all (stack)",
						"Dye all " + weapon.getNamePlural() + " which are in this weapon stack ("+stackCount+" in total) in the colours you have chosen."
								+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
										?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can dye them a different colour at any time."
										:" This action is permanent, and you'll need another dye-brush if you want to change their colour again."),
						INVENTORY_MENU) {
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().removeItem(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH), finalCount);
							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getDyeBrushEffects(weapon, dyePreviews.get(0))
									+ "</p>"
									+ "<p>"
									+ "<b>The "+weapon.getName()+(weapon.getWeaponType().isPlural()?"have":"has")+" been dyed</b>!"
									+ "</p>");

							Main.game.getTextEndStringBuilder().append("<p>You then repeat this for the other "+Util.intToString(finalCount-1)+" "+weapon.getNamePlural()+"...</p>");

							Main.game.getTextEndStringBuilder().append("<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH))
														+ "</b> dye-brush" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH)) == 1 ? "" : "es") + " left!"
												:"You have <b>0</b> dye-brushes left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
											+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to dye "
												+ (finalCount==2
													?"both"
													:"all "+Util.intToString(finalCount))
												+" of the " + weapon.getNamePlural() + " without needing to use a single dye-brush!"
										+ "</p>");
						}

						if(owner!=null) {
							owner.removeWeapon(weapon, finalCount);
							AbstractWeapon dyedWeapon = Main.game.getItemGen().generateWeapon(weapon);
							dyedWeapon.setColours(dyePreviews);
							owner.addWeapon(dyedWeapon, finalCount, false, false);
							Main.game.addEvent(new EventLogEntry("Dyed", dyedWeapon.getDisplayName(true)), false);

						} else {
							Main.game.getPlayerCell().getInventory().removeWeapon(weapon, finalCount);
							AbstractWeapon dyedWeapon = Main.game.getItemGen().generateWeapon(weapon);
							dyedWeapon.setColours(dyePreviews);
							Main.game.getPlayerCell().getInventory().addWeapon(dyedWeapon, finalCount);
							Main.game.addEvent(new EventLogEntry("Dyed", dyedWeapon.getDisplayName(true)), false);
						}
					}
				};

			} else if (index == 7) {
				if(damageTypePreview == weapon.getDamageType()) {
					return new Response("Reforge all (stack)",
							"You need to choose a different damage type before being able to reforge the " + weapon.getName() + "!",
							null);
				}

				int stackCount = 0;
				if(owner!=null) {
					stackCount = owner.getWeaponCount(weapon);
				} else {
					stackCount = Main.game.getPlayerCell().getInventory().getWeaponCount(weapon);
				}

				int finalCount = stackCount;

				if(stackCount==1) {
					return new Response("Reforge all (stack)",
							"You only have one "+weapon.getName()+", so you should reforge it using the individual action...",
							null);
				}

				if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
					int reforgeHammerCount = Main.game.getPlayer().getItemCount(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER));

					if(reforgeHammerCount<stackCount) {
						return new Response("Reforge all (stack)",
								"You do not have enough reforging hammers to dye all the " + weapon.getNamePlural() + "! You have "+reforgeHammerCount+" reforging hammers, but there are "+stackCount+" "+weapon.getNamePlural()+" in this stack...",
								null);
					}
				}

				return new Response("Reforge all (stack)",
						"Reforge all " + weapon.getNamePlural() + " which are in this weapon stack ("+stackCount+" in total) into the damage type you have chosen."
								+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
										?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can reforge them at any time."
										:" This action is permanent, and you'll need another reforging hammer if you want to change their damage type again."),
						INVENTORY_MENU) {
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().removeItem(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER), finalCount);
							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getReforgeHammerEffects(weapon, damageTypePreview)
									+ "</p>"
									+ "<p>"
									+ "<b>The "+weapon.getName()+(weapon.getWeaponType().isPlural()?"have":"has")+" been reforged</b>!"
									+ "</p>");

							Main.game.getTextEndStringBuilder().append("<p>You then repeat this for the other "+Util.intToString(finalCount-1)+" "+weapon.getNamePlural()+"...</p>");

							Main.game.getTextEndStringBuilder().append("<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER))
														+ "</b> reforging hammer" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER)) == 1 ? "" : "s") + " left!"
												:"You have <b>0</b> reforging hammers left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
											+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to reforge "
												+ (finalCount==2
													?"both"
													:"all "+Util.intToString(finalCount))
												+" of the " + weapon.getNamePlural() + " without needing to use a single reforging hammer!"
										+ "</p>");
						}

						if(owner!=null) {
							owner.removeWeapon(weapon, finalCount);
							AbstractWeapon modifiedWeapon = Main.game.getItemGen().generateWeapon(weapon);
							modifiedWeapon.setDamageType(damageTypePreview);
							// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
							owner.addWeapon(Main.game.getItemGen().generateWeapon(modifiedWeapon), finalCount, false, false);
							Main.game.addEvent(new EventLogEntry("Reforged", modifiedWeapon.getDisplayName(true)), false);

						} else {
							Main.game.getPlayerCell().getInventory().removeWeapon(weapon, finalCount);
							AbstractWeapon modifiedWeapon = Main.game.getItemGen().generateWeapon(weapon);
							modifiedWeapon.setDamageType(damageTypePreview);
							// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
							Main.game.getPlayerCell().getInventory().addWeapon(Main.game.getItemGen().generateWeapon(modifiedWeapon), finalCount);
							Main.game.addEvent(new EventLogEntry("Reforged", modifiedWeapon.getDisplayName(true)), false);
						}
					}
				};

			} else if (index == 8) {
				if(damageTypePreview == weapon.getDamageType()) {
					return new Response("Dye & reforge all (stack)",
							"You need to choose a different damage type before being able to reforge the " + weapon.getName() + "!",
							null);
				}

				if(dyePreviews.equals(weapon.getColours())) {
					return new Response("Dye & reforge all (stack)",
							"You need to choose different colours before being able to dye the " + weapon.getName() + "!",
							null);
				}


				int stackCount = 0;
				if(owner!=null) {
					stackCount = owner.getWeaponCount(weapon);
				} else {
					stackCount = Main.game.getPlayerCell().getInventory().getWeaponCount(weapon);
				}

				int finalCount = stackCount;

				if(stackCount==1) {
					return new Response("Dye & reforge all (stack)",
							"You only have one "+weapon.getName()+", so you should dye & reforge it using the individual action...",
							null);
				}

				if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
					int dyeBrushCount = Main.game.getPlayer().getItemCount(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH));
					int reforgeHammerCount = Main.game.getPlayer().getItemCount(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER));

					if(dyeBrushCount<stackCount || reforgeHammerCount<stackCount) {
						return new Response("Dye & reforge all (stack)",
								"You do not have enough dye brushes or reforge hammers to dye & reforge all "+stackCount+" "+weapon.getNamePlural()+" in this stack...",
								null);
					}
				}

				return new Response("Dye & reforge all (stack)",
						"Dye & reforge all " + weapon.getNamePlural() + " which are in this weapon stack ("+stackCount+" in total) in the colours and damage type you have chosen."
								+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
										?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can do this at any time."
										:" This action is permanent, and you'll need another dye-brush and reforging hammer if you want to do this again."),
						INVENTORY_MENU) {
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().removeItem(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH), finalCount);
							Main.game.getPlayer().removeItem(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER), finalCount);
							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getDyeBrushEffects(weapon, dyePreviews.get(0))
									+ "</p>"
									+ "<p style='text-align:center;'>"
										+ getReforgeHammerEffects(weapon, damageTypePreview)
									+ "</p>"
									+ "<p>"
									+ "<b>The "+weapon.getName()+(weapon.getWeaponType().isPlural()?"have":"has")+" been dyed and reforged</b>!"
									+ "</p>");

							Main.game.getTextEndStringBuilder().append("<p>You then repeat this for the other "+Util.intToString(finalCount-1)+" "+weapon.getNamePlural()+"...</p>");

							Main.game.getTextEndStringBuilder().append("<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH))
														+ "</b> dye-brush" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH)) == 1 ? "" : "es") + " left!"
												:"You have <b>0</b> dye-brushes left!")
									+ "</p>");

							Main.game.getTextEndStringBuilder().append("<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER))
														+ "</b> reforging hammer" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER)) == 1 ? "" : "s") + " left!"
												:"You have <b>0</b> reforging hammers left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
											+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to dye "
												+ (finalCount==2
													?"both"
													:"all "+Util.intToString(finalCount))
												+" of the " + weapon.getNamePlural() + " without needing to use a single dye-brush or reforging hammer!"
										+ "</p>");
						}

						if(owner!=null) {
							owner.removeWeapon(weapon, finalCount);
							AbstractWeapon modifiedWeapon = Main.game.getItemGen().generateWeapon(weapon);
							modifiedWeapon.setDamageType(damageTypePreview);
							modifiedWeapon.setColours(dyePreviews);
							// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
							owner.addWeapon(Main.game.getItemGen().generateWeapon(modifiedWeapon), finalCount, false, false);
							Main.game.addEvent(new EventLogEntry("Dyed & Reforged", modifiedWeapon.getDisplayName(true)), false);

						} else {
							Main.game.getPlayerCell().getInventory().removeWeapon(weapon, finalCount);
							AbstractWeapon modifiedWeapon = Main.game.getItemGen().generateWeapon(weapon);
							modifiedWeapon.setDamageType(damageTypePreview);
							modifiedWeapon.setColours(dyePreviews);
							// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
							Main.game.getPlayerCell().getInventory().addWeapon(Main.game.getItemGen().generateWeapon(modifiedWeapon), finalCount);
							Main.game.addEvent(new EventLogEntry("Dyed & Reforged", modifiedWeapon.getDisplayName(true)), false);
						}
					}
				};

			} else if (index == 11) {
				if(dyePreviews.equals(weapon.getColours())) {
					return new Response("Dye all",
							"You need to choose different colours before being able to dye the " + weapon.getName() + "!",
							null);
				}

				List<AbstractWeapon> weaponMatches = new ArrayList<>();
				int stackCount = 0;
				if(owner!=null) {
					for(Entry<AbstractWeapon, Integer> entry : owner.getAllWeaponsInInventory().entrySet()) {
						if(entry.getKey().getWeaponType().equals(weapon.getWeaponType())) {
							weaponMatches.add(entry.getKey());
							stackCount += entry.getValue();
						}
					}
				} else {
					for(Entry<AbstractWeapon, Integer> entry : Main.game.getPlayerCell().getInventory().getAllWeaponsInInventory().entrySet()) {
						if(entry.getKey().getWeaponType().equals(weapon.getWeaponType())) {
							weaponMatches.add(entry.getKey());
							stackCount += entry.getValue();
						}
					}
				}

				int finalCount = stackCount;

				if(stackCount==1) {
					return new Response("Dye all",
							"You only have one "+weapon.getName()+", so you should dye it using the individual action...",
							null);
				}

				if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
					int dyeBrushCount = Main.game.getPlayer().getItemCount(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH));

					if(dyeBrushCount<stackCount) {
						return new Response("Dye all",
								"You do not have enough dye brushes to dye all the " + weapon.getNamePlural() + "! You have "+dyeBrushCount+" dye brushes, but there are "+stackCount+" "+weapon.getNamePlural()+" in total...",
								null);
					}
				}

				return new Response("Dye all",
						"Dye all " + weapon.getNamePlural() + " which are in this clothing stack ("+stackCount+" in total) in the colours you have chosen."
								+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
										?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can dye them a different colour at any time."
										:" This action is permanent, and you'll need another dye-brush if you want to change their colour again."),
						INVENTORY_MENU) {
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().removeItem(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH), finalCount);
							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getDyeBrushEffects(weapon, dyePreviews.get(0))
									+ "</p>"
									+ "<p>"
									+ "<b>The "+weapon.getName()+(weapon.getWeaponType().isPlural()?"have":"has")+" been dyed</b>!"
									+ "</p>");

							Main.game.getTextEndStringBuilder().append("<p>You then repeat this for the other "+Util.intToString(finalCount-1)+" "+weapon.getNamePlural()+"...</p>");

							Main.game.getTextEndStringBuilder().append("<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH))
														+ "</b> dye-brush" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH)) == 1 ? "" : "es") + " left!"
												:"You have <b>0</b> dye-brushes left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
											+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to dye "
												+ (finalCount==2
													?"both"
													:"all "+Util.intToString(finalCount))
												+" of the " + weapon.getNamePlural() + " without needing to use a single dye-brush!"
										+ "</p>");
						}


						if(owner!=null) {
							for(AbstractWeapon w : weaponMatches) {
								int weaponCount = owner.getAllWeaponsInInventory().get(w);
								owner.removeWeapon(w, weaponCount);
								AbstractWeapon dyedWeapon = Main.game.getItemGen().generateWeapon(w);
								dyedWeapon.setColours(dyePreviews);
								owner.addWeapon(dyedWeapon, weaponCount, false, false);
								Main.game.addEvent(new EventLogEntry("Dyed", dyedWeapon.getDisplayName(true)), false);
							}

						} else {
							for(AbstractWeapon w : weaponMatches) {
								int weaponCount = Main.game.getPlayerCell().getInventory().getAllWeaponsInInventory().get(w);
								Main.game.getPlayerCell().getInventory().removeWeapon(w, weaponCount);
								AbstractWeapon dyedWeapon = Main.game.getItemGen().generateWeapon(w);
								dyedWeapon.setColours(dyePreviews);
								Main.game.getPlayerCell().getInventory().addWeapon(dyedWeapon, weaponCount);
								Main.game.addEvent(new EventLogEntry("Dyed", dyedWeapon.getDisplayName(true)), false);
							}
						}
					}
				};

			} else if (index == 12) {
				if(damageTypePreview == weapon.getDamageType()) {
					return new Response("Reforge all",
							"You need to choose a different damage type before being able to reforge the " + weapon.getName() + "!",
							null);
				}

				List<AbstractWeapon> weaponMatches = new ArrayList<>();
				int stackCount = 0;
				if(owner!=null) {
					for(Entry<AbstractWeapon, Integer> entry : owner.getAllWeaponsInInventory().entrySet()) {
						if(entry.getKey().getWeaponType().equals(weapon.getWeaponType())) {
							weaponMatches.add(entry.getKey());
							stackCount += entry.getValue();
						}
					}
				} else {
					for(Entry<AbstractWeapon, Integer> entry : Main.game.getPlayerCell().getInventory().getAllWeaponsInInventory().entrySet()) {
						if(entry.getKey().getWeaponType().equals(weapon.getWeaponType())) {
							weaponMatches.add(entry.getKey());
							stackCount += entry.getValue();
						}
					}
				}

				int finalCount = stackCount;

				if(stackCount==1) {
					return new Response("Reforge all",
							"You only have one "+weapon.getName()+", so you should reforge it using the individual action...",
							null);
				}

				if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
					int reforgeHammerCount = Main.game.getPlayer().getItemCount(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER));

					if(reforgeHammerCount<stackCount) {
						return new Response("Reforge all",
								"You do not have enough reforging hammers to dye all the " + weapon.getNamePlural() + "! You have "+reforgeHammerCount+" reforging hammers, but there are "+stackCount+" "+weapon.getNamePlural()+" in total...",
								null);
					}
				}

				return new Response("Reforge all",
						"Reforge all " + weapon.getNamePlural() + " which are in this weapon stack ("+stackCount+" in total) into the damage type you have chosen."
								+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
										?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can reforge them at any time."
										:" This action is permanent, and you'll need another reforging hammer if you want to change their damage type again."),
						INVENTORY_MENU) {
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().removeItem(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER), finalCount);
							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getReforgeHammerEffects(weapon, damageTypePreview)
									+ "</p>"
									+ "<p>"
									+ "<b>The "+weapon.getName()+(weapon.getWeaponType().isPlural()?"have":"has")+" been reforged</b>!"
									+ "</p>");

							Main.game.getTextEndStringBuilder().append("<p>You then repeat this for the other "+Util.intToString(finalCount-1)+" "+weapon.getNamePlural()+"...</p>");

							Main.game.getTextEndStringBuilder().append("<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER))
														+ "</b> reforging hammer" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER)) == 1 ? "" : "s") + " left!"
												:"You have <b>0</b> reforging hammers left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
											+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to reforge "
												+ (finalCount==2
													?"both"
													:"all "+Util.intToString(finalCount))
												+" of the " + weapon.getNamePlural() + " without needing to use a single reforging hammer!"
										+ "</p>");
						}

						if(owner!=null) {
							for(AbstractWeapon w : weaponMatches) {
								int weaponCount = owner.getAllWeaponsInInventory().get(w);
								owner.removeWeapon(w, weaponCount);
								AbstractWeapon modifiedWeapon = Main.game.getItemGen().generateWeapon(w);
								modifiedWeapon.setDamageType(damageTypePreview);
								// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
								owner.addWeapon(Main.game.getItemGen().generateWeapon(modifiedWeapon), weaponCount, false, false);
								Main.game.addEvent(new EventLogEntry("Reforged", modifiedWeapon.getDisplayName(true)), false);
							}

						} else {
							for(AbstractWeapon w : weaponMatches) {
								int weaponCount = Main.game.getPlayerCell().getInventory().getAllWeaponsInInventory().get(w);
								Main.game.getPlayerCell().getInventory().removeWeapon(w, weaponCount);
								AbstractWeapon modifiedWeapon = Main.game.getItemGen().generateWeapon(w);
								modifiedWeapon.setDamageType(damageTypePreview);
								// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
								Main.game.getPlayerCell().getInventory().addWeapon(Main.game.getItemGen().generateWeapon(modifiedWeapon), weaponCount);
								Main.game.addEvent(new EventLogEntry("Reforged", modifiedWeapon.getDisplayName(true)), false);
							}
						}
					}
				};

			} else if (index == 13) {
				if(damageTypePreview == weapon.getDamageType()) {
					return new Response("Dye & reforge all",
							"You need to choose a different damage type before being able to reforge the " + weapon.getName() + "!",
							null);
				}

				if(dyePreviews.equals(weapon.getColours())) {
					return new Response("Dye & reforge all",
							"You need to choose different colours before being able to dye the " + weapon.getName() + "!",
							null);
				}

				List<AbstractWeapon> weaponMatches = new ArrayList<>();
				int stackCount = 0;
				if(owner!=null) {
					for(Entry<AbstractWeapon, Integer> entry : owner.getAllWeaponsInInventory().entrySet()) {
						if(entry.getKey().getWeaponType().equals(weapon.getWeaponType())) {
							weaponMatches.add(entry.getKey());
							stackCount += entry.getValue();
						}
					}
				} else {
					for(Entry<AbstractWeapon, Integer> entry : Main.game.getPlayerCell().getInventory().getAllWeaponsInInventory().entrySet()) {
						if(entry.getKey().getWeaponType().equals(weapon.getWeaponType())) {
							weaponMatches.add(entry.getKey());
							stackCount += entry.getValue();
						}
					}
				}

				int finalCount = stackCount;

				if(stackCount==1) {
					return new Response("Dye & reforge all",
							"You only have one "+weapon.getName()+", so you should dye & reforge it using the individual action...",
							null);
				}

				if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
					int dyeBrushCount = Main.game.getPlayer().getItemCount(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH));
					int reforgeHammerCount = Main.game.getPlayer().getItemCount(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER));

					if(dyeBrushCount<stackCount || reforgeHammerCount<stackCount) {
						return new Response("Dye & reforge all",
								"You do not have enough dye brushes or reforge hammers to dye & reforge all "+stackCount+" "+weapon.getNamePlural()+" in this stack...",
								null);
					}
				}

				return new Response("Dye & reforge all",
						"Dye & reforge all " + weapon.getNamePlural() + " which are in this weapon stack ("+stackCount+" in total) in the colours and damage type you have chosen."
								+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
										?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can do this at any time."
										:" This action is permanent, and you'll need another dye-brush and reforging hammer if you want to do this again."),
						INVENTORY_MENU) {
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().removeItem(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH), finalCount);
							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getDyeBrushEffects(weapon, dyePreviews.get(0))
									+ "</p>"
									+ "<p style='text-align:center;'>"
										+ getReforgeHammerEffects(weapon, damageTypePreview)
									+ "</p>"
									+ "<p>"
									+ "<b>The "+weapon.getName()+(weapon.getWeaponType().isPlural()?"have":"has")+" been dyed and reforged</b>!"
									+ "</p>");

							Main.game.getTextEndStringBuilder().append("<p>You then repeat this for the other "+Util.intToString(finalCount-1)+" "+weapon.getNamePlural()+"...</p>");

							Main.game.getTextEndStringBuilder().append("<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH))
														+ "</b> dye-brush" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH)) == 1 ? "" : "es") + " left!"
												:"You have <b>0</b> dye-brushes left!")
									+ "</p>");

							Main.game.getTextEndStringBuilder().append("<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER))
														+ "</b> reforging hammer" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER)) == 1 ? "" : "s") + " left!"
												:"You have <b>0</b> reforging hammers left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
											+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to dye and reforge "
												+ (finalCount==2
													?"both"
													:"all "+Util.intToString(finalCount))
												+" of the " + weapon.getNamePlural() + " without needing to use a single dye-brush or reforging hammer!"
										+ "</p>");
						}

						if(owner!=null) {
							for(AbstractWeapon w : weaponMatches) {
								int weaponCount = owner.getAllWeaponsInInventory().get(w);
								owner.removeWeapon(w, weaponCount);
								AbstractWeapon modifiedWeapon = Main.game.getItemGen().generateWeapon(w);
								modifiedWeapon.setDamageType(damageTypePreview);
								modifiedWeapon.setColours(dyePreviews);
								// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
								owner.addWeapon(Main.game.getItemGen().generateWeapon(modifiedWeapon), weaponCount, false, false);
								Main.game.addEvent(new EventLogEntry("Dyed & Reforged", modifiedWeapon.getDisplayName(true)), false);
							}

						} else {
							for(AbstractWeapon w : weaponMatches) {
								int weaponCount = Main.game.getPlayerCell().getInventory().getAllWeaponsInInventory().get(w);
								Main.game.getPlayerCell().getInventory().removeWeapon(w, weaponCount);
								AbstractWeapon modifiedWeapon = Main.game.getItemGen().generateWeapon(w);
								modifiedWeapon.setDamageType(damageTypePreview);
								modifiedWeapon.setColours(dyePreviews);
								// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
								Main.game.getPlayerCell().getInventory().addWeapon(Main.game.getItemGen().generateWeapon(modifiedWeapon), weaponCount);
								Main.game.addEvent(new EventLogEntry("Dyed & Reforged", modifiedWeapon.getDisplayName(true)), false);
							}
						}
					}
				};

			} else {
				return null;
			}
		}

		@Override
		public DialogueNodeType getDialogueNodeType() {
			return DialogueNodeType.INVENTORY;
		}
	};

	public static final DialogueNode DYE_EQUIPPED_WEAPON = new DialogueNode("Dye Weapon", "", true) {

		@Override
		public String getContent() {
			return getWeaponDyeUI();
		}

		@Override
		public Response getResponse(int responseTab, int index) {
			if (index == 0) {
				return new Response("Back", "Return to the previous menu.", INVENTORY_MENU);

			} else if (index == 1) {
				if (!Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH)
						&& !Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
					return new Response("Dye",
							"You do not have a dye-brush, so cannot change the colours of the " + weapon.getName() + "...",
							null);
				}

				if(dyePreviews.equals(weapon.getColours())) {
					return new Response("Dye",
							"You need to choose different colours before being able to dye the " + weapon.getName() + "!",
							null);
				}

				return new Response("Dye",
						"Dye the " + weapon.getName() + " in the colours you have chosen."
								+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
										?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can dye it a different colour at any time."
										:" This action is permanent, and you'll need another dye-brush if you want to change its colour again."),
						INVENTORY_MENU){
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().useItem(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH), owner, false);
							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getDyeBrushEffects(weapon, dyePreviews.get(0))
									+ "</p>"
									+ "<p>"
										+ "<b>The " + weapon.getName() + " " + (weapon.getWeaponType().isPlural() ? "have been" : "has been") + " dyed</b>!"
									+ "</p>"
									+ "<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH))
														+ "</b> dye-brush" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH)) == 1 ? "" : "es") + " left!"
												:"You have <b>0</b> dye-brushes left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
											+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to dye the " + weapon.getName() + " without needing to use a dye-brush!"
										+ "</p>");
						}



						owner.unequipWeaponIntoVoid(weaponSlot, weapon, true);
						AbstractWeapon modifiedWeapon = Main.game.getItemGen().generateWeapon(weapon);
						modifiedWeapon.setColours(dyePreviews);

						if(weaponSlot==InventorySlot.WEAPON_MAIN_1
								|| weaponSlot==InventorySlot.WEAPON_MAIN_2
								|| weaponSlot==InventorySlot.WEAPON_MAIN_3) {
							// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
							owner.equipMainWeaponFromNowhere(Main.game.getItemGen().generateWeapon(modifiedWeapon));

						} else {
							// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
							owner.equipOffhandWeaponFromNowhere(Main.game.getItemGen().generateWeapon(modifiedWeapon));
						}

//						weapon.setPrimaryColour(dyePreviewPrimary);
//						weapon.setSecondaryColour(dyePreviewSecondary);
						Main.game.addEvent(new EventLogEntry("Dyed", weapon.getDisplayName(true)), false);
					}
				};

			} else if (index == 2) {
				if (!Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER)
						&& !Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
					return new Response("Reforge",
							"You do not have a reforging hammer, so cannot change the damage type of the " + weapon.getName() + "...",
							null);
				}

				if(damageTypePreview == weapon.getDamageType()) {
					return new Response("Reforge",
							"You need to choose a different damage type before being able to reforge the " + weapon.getName() + "!",
							null);
				}

				return new Response("Reforge",
						"Reforge the " + weapon.getName() + " into the damage type you have chosen."
								+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
										?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can reforge it at any time."
										:" This action is permanent, and you'll need another reforging hammer if you want to change its damage type again."),
						INVENTORY_MENU){
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().useItem(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER), owner, false);
							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getReforgeHammerEffects(weapon, damageTypePreview)
									+ "</p>"
									+ "<p>"
										+ "<b>The " + weapon.getName() + " " + (weapon.getWeaponType().isPlural() ? "have been" : "has been") + " reforged</b>!"
									+ "</p>"
									+ "<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER))
														+ "</b> reforging " + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER)) == 1 ? "hammer" : "hammers") + " left!"
												:"You have <b>0</b> reforging hammers left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
											+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to reforge the " + weapon.getName() + " without needing to use a reforging hammer!"
										+ "</p>");
						}

						owner.unequipWeaponIntoVoid(weaponSlot, weapon, true);
						AbstractWeapon modifiedWeapon = Main.game.getItemGen().generateWeapon(weapon);
						modifiedWeapon.setDamageType(damageTypePreview);

						if(weaponSlot==InventorySlot.WEAPON_MAIN_1
								|| weaponSlot==InventorySlot.WEAPON_MAIN_2
								|| weaponSlot==InventorySlot.WEAPON_MAIN_3) {
							// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
							owner.equipMainWeaponFromNowhere(Main.game.getItemGen().generateWeapon(modifiedWeapon));

						} else {
							// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
							owner.equipOffhandWeaponFromNowhere(Main.game.getItemGen().generateWeapon(modifiedWeapon));
						}

						Main.game.addEvent(new EventLogEntry("Reforged", weapon.getDisplayName(true)), false);
					}
				};

			} else if (index == 3) {
				if ((!Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER) || !Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH))
						&& !Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
					return new Response("Dye & reforge",
							"You do not have both a dye brush and a reforging hammer, so cannot dye and reforge the " + weapon.getName() + "...",
							null);
				}

				if(damageTypePreview == weapon.getDamageType()) {
					return new Response("Dye & reforge",
							"You need to choose a different damage type before being able to reforge the " + weapon.getName() + "!",
							null);
				}

				if(dyePreviews.equals(weapon.getColours())) {
					return new Response("Dye & reforge",
							"You need to choose different colours before being able to dye the " + weapon.getName() + "!",
							null);
				}

				return new Response("Dye & reforge",
						"Dye and reforge the " + weapon.getName() + " into the colours and damage type you have chosen."
								+ (Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
										?" This action is permanent, but thanks to your proficiency with [style.boldEarth(Earth spells)], you can dye and reforge it at any time."
										:" This action is permanent, and you'll need another dye-brush and another reforging hammer if you want to change its colours and damage type again."),
						INVENTORY_MENU){
					@Override
					public void effects(){
						if(!Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)) {
							Main.game.getPlayer().useItem(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH), owner, false);
							Main.game.getPlayer().useItem(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER), owner, false);
							Main.game.getTextEndStringBuilder().append(
									"<p style='text-align:center;'>"
										+ getDyeBrushEffects(weapon, dyePreviews.get(0))
									+ "</p>"
									+ "<p style='text-align:center;'>"
										+ getReforgeHammerEffects(weapon, damageTypePreview)
									+ "</p>"
									+ "<p>"
										+ "<b>The " + weapon.getName() + " " + (weapon.getWeaponType().isPlural() ? "have been" : "has been") + " reforged</b>!"
									+ "</p>"
									+ "<p>"
										+ (Main.game.getPlayer().hasItemType(ItemType.DYE_BRUSH) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH))
														+ "</b> dye-brush" + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.DYE_BRUSH)) == 1 ? "" : "es") + " left!"
												:"You have <b>0</b> dye-brushes left!")
										+"<br/>"
										+ (Main.game.getPlayer().hasItemType(ItemType.REFORGE_HAMMER) || Main.game.getPlayer().isSpellSchoolSpecialAbilityUnlocked(SpellSchool.EARTH)
												?"You have <b>" + Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER))
														+ "</b> reforging " + (Main.game.getPlayer().getAllItemsInInventory().get(Main.game.getItemGen().generateItem(ItemType.REFORGE_HAMMER)) == 1 ? "hammer" : "hammers") + " left!"
												:"You have <b>0</b> reforging hammers left!")
									+ "</p>");

						} else {
							Main.game.getTextEndStringBuilder().append(
									"<p>"
											+ "Thanks to your proficiency with [style.boldEarth(Earth spells)], you are able to dye and reforge the " + weapon.getName() + " without needing to use a dye-brush or reforging hammer!"
										+ "</p>");
						}

						owner.unequipWeaponIntoVoid(weaponSlot, weapon, true);
						AbstractWeapon modifiedWeapon = Main.game.getItemGen().generateWeapon(weapon);
						modifiedWeapon.setColours(dyePreviews);
						modifiedWeapon.setDamageType(damageTypePreview);

						if(weaponSlot==InventorySlot.WEAPON_MAIN_1
								|| weaponSlot==InventorySlot.WEAPON_MAIN_2
								|| weaponSlot==InventorySlot.WEAPON_MAIN_3) {
							// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
							owner.equipMainWeaponFromNowhere(Main.game.getItemGen().generateWeapon(modifiedWeapon));

						} else {
							// For some reason, if you add the modifiedWeapon directly, it won't stack with other identical weapons... Have to generateWeapon(modifiedWeapon) again to get it to start stacking properly:
							owner.equipOffhandWeaponFromNowhere(Main.game.getItemGen().generateWeapon(modifiedWeapon));
						}

//						weapon.setDamageType(damageTypePreview);
//						weapon.setPrimaryColour(dyePreviewPrimary);
//						weapon.setSecondaryColour(dyePreviewSecondary);
						Main.game.addEvent(new EventLogEntry("Dyed & Reforged", weapon.getDisplayName(true)), false);
					}
				};

			} else {
				return null;
			}
		}

		@Override
		public DialogueNodeType getDialogueNodeType() {
			return DialogueNodeType.INVENTORY;
		}
	};

	public static final DialogueNode MANAGE_FLUIDS = new DialogueNode("Manage Fluids", "", true) {

		@Override
		public String getContent() {
			return getFluidManagerUi();
		}


		@SuppressWarnings("unused")
		@Override
		public Response getResponse(int responseTab, int index) {
			if (index == 0) {
				return new Response("Back", "Return to the previous menu.", INVENTORY_MENU);

			} else if (index == 1) {
				if(!transferLog.isEmpty()) {
					return new Response("Apply changes",
							"Finalize the transfers",
							INVENTORY_MENU) {
						@Override
						public void effects(){
							applyTransfers();
						}
					};
				}
				return new Response("Apply changes",
						"You haven't done anything yet. ",
						INVENTORY_MENU) {};

			}
			return null;
		}

		@Override
		public DialogueNodeType getDialogueNodeType() {
			return DialogueNodeType.INVENTORY;
		}
	};



	// Utility methods:

	private static String getDyeBrushEffects(AbstractClothing clothing, Colour colour) {
		return "<p>"
					+ "As you take hold of the Dye-brush, you see the purple glow around the tip growing in strength."
					+ " The closer you move it to your " + clothing.getName() + ", the brighter the glow becomes, until suddenly, images of different colours start flashing through your mind."
					+ " As you touch the bristles to the " + clothing.getName() + "'s surface, the Dye-brush instantly evaporates!"
					+ " You see that the arcane enchantment has dyed the " + clothing.getName() + " " + colour.getName() + "."
				+ "</p>";
	}

	private static String getDyeBrushEffects(AbstractWeapon weapon, Colour colour) {
		return "<p>"
					+ "As you take hold of the Dye-brush, you see the purple glow around the tip growing in strength."
					+ " The closer you move it to your " + weapon.getName() + ", the brighter the glow becomes, until suddenly, images of different colours start flashing through your mind."
					+ " As you touch the bristles to the " + weapon.getName() + "'s surface, the Dye-brush instantly evaporates!"
					+ " You see that the arcane enchantment has dyed the " + weapon.getName() + " " + colour.getName() + "."
				+ "</p>";
	}

	private static String getReforgeHammerEffects(AbstractWeapon weapon, DamageType damageType) {
		return "<p>"
					+ "As you take hold of the reforging hammer, you see the metal head start to emit a deep purple glow."
					+ " The closer you move it to your " + weapon.getName() + ", the brighter this glow becomes, until suddenly, images of different damage types start flashing through your mind."
					+ " As you touch the metal head  to the " + weapon.getName() + ", the reforge hammer instantly evaporates!"
					+ " You see that the arcane enchantment has reforged the " + weapon.getName() + " so that it now deals " + damageType.getName() + " damage."
				+ "</p>";
	}

	private static String getItemDisplayPanel(String SVGString, String title, String description) {
		return "<div class='inventoryImage'>" // style='width: calc(50% - 4px);'
					+ "<div class='inventoryImage-content'>"
						+ SVGString
					+ "</div>"
				+ "</div>"
				+ "<h5 style='margin-bottom:0; padding-bottom:0;'><b>"+title+"</b></h5>"
				+ "<p style='margin-top:0; padding-top:0;'>"
					+ description
				+ "</p>";
	}

	private static String getGeneralResponseTabTitle(int index) {
		if(index==0) {
			return "Overview";
		} else if(index==1) {
			return "Selected item";
		} else {
			return null;
		}
	}
	
	private static Response getCloseInventoryResponse() {
		if(interactionType == InventoryInteraction.CHARACTER_CREATION) {
			return new Response("Back", "Return to looking in the mirror at your appearance.", CharacterCreation.CHOOSE_ADVANCED_APPEARANCE){
				@Override
				public int getSecondsPassed() {
					return -CharacterCreation.TIME_TO_CLOTHING;
				}
				@Override
				public void effects(){
					item = null;
					clothing = null;
					weapon = null;
				}
			};

		} else {
			return new ResponseEffectsOnly("Close Inventory", "Close the Inventory menu."){
				@Override
				public void effects(){
					item = null;
					clothing = null;
					weapon = null;
					Main.mainController.openInventory();
				}
			};
		}
	}
	
	private static Response getBuybackResponse() {
		if (buyback) {
			return new Response("Normal trade", "Switch back to the normal trade menu.", INVENTORY_MENU){
				@Override
				public void effects(){
					buyback = !buyback;
				}
			};
		} else {
			if(Main.game.getPlayer().getBuybackStack()==null || Main.game.getPlayer().getBuybackStack().isEmpty()) {
				return new Response("Buyback", "You have not sold any items, so cannot buy anything back...", null);
			} else {
				return new Response("Buyback", "Switch to viewing the buyback menu.", INVENTORY_MENU){
					@Override
					public void effects(){
						buyback = !buyback;
					}
				};
			}
		}
	}
	
	private static Response getQuickTradeResponse() { //TODO move this into options
		
		return null;

//		if (Main.game.getDialogueFlags().quickTrade) {
//			return new Response("Quick-Manage: <b style='color:" + PresetColour.GENERIC_GOOD.toWebHexString() + ";'>ON</b>",
//					"Quick-Manage is turned <b style='color:" + PresetColour.GENERIC_GOOD.toWebHexString() + ";'>ON</b>!<br/>"
//							+ "That means you can buy and sell items with a single click when trading, and pick-up and drop items with a single click when in normal inventory mode.", INVENTORY_MENU){
//
//				@Override
//				public DialogueNodeOld getNextDialogue() {
//					return Main.game.getCurrentDialogueNode();
//				}
//
//				@Override
//				public void effects(){
//					Main.game.getDialogueFlags().quickTrade = !Main.game.getDialogueFlags().quickTrade;
//				}
//			};
//
//		} else {
//			return new Response("Quick-Manage: <b style='color:" + PresetColour.TEXT_GREY.toWebHexString() + ";'>OFF</b>",
//					"Quick-Manage is turned <b style='color:" + PresetColour.TEXT_GREY.toWebHexString() + ";'>OFF</b>.<br/>"
//							+ "That means when you click on an item, you get a detailed view of the item before deciding whether to buy/sell or pick-up/drop it.", INVENTORY_MENU){
//
//				@Override
//				public DialogueNodeOld getNextDialogue() {
//					return Main.game.getCurrentDialogueNode();
//				}
//
//				@Override
//				public void effects(){
//					Main.game.getDialogueFlags().quickTrade = !Main.game.getDialogueFlags().quickTrade;
//				}
//			};
//		}
	}
	
	private static Response getJinxRemovalResponse(boolean selfUnseal) {
		boolean ownsKey = Main.game.getPlayer().getUnlockKeyMap().containsKey(owner.getId()) && Main.game.getPlayer().getUnlockKeyMap().get(owner.getId()).contains(clothing.getSlotEquippedTo());
		int removalCost = clothing.getJinxRemovalCost(Main.game.getPlayer(), selfUnseal);

		if(interactionType==InventoryInteraction.COMBAT) {
			return new Response("Unseal"+(ownsKey?"(Use key)":"(<i>"+removalCost+" Essences</i>)"),
					"You can't unseal clothing in combat!",
					null);
		}

		if(interactionType==InventoryInteraction.SEX && !Main.sex.getInitialSexManager().isAbleToRemoveClothingSeals(Main.game.getPlayer())) {
			return new Response("Unseal"+(ownsKey?"(Use key)":"(<i>"+removalCost+" Essences</i>)"),
					"You can't unseal clothing in this sex scene!",
					null);
		}


		if(!ownsKey) {
			if(!Main.game.getPlayer().isQuestCompleted(QuestLine.SIDE_ENCHANTMENT_DISCOVERY)) {
				return new Response("Unseal", "You don't know how to unseal clothing! Perhaps you should pay Lilaya a visit and ask her about it...", null);
			}
			if(Main.game.getPlayer().getClothingCurrentlyEquipped().stream().anyMatch(c -> c.isSelfTransformationInhibiting())) {
				return new Response("Unseal",
						"Although you are normally able to unseal clothing, you cannot do so due to an enchantment on one or more pieces of your equipped clothing!"
						+ "<br/>[style.italicsArcane(Visit Lilaya to get your sealed clothing removed!)]",
						null);
			}
			if(Main.game.getPlayer().getTattoos().values().stream().anyMatch(c -> c.isSelfTransformationInhibiting())) {
				return new Response("Unseal",
						"Although you are normally able to unseal clothing, you cannot do so due to an enchantment on one or more of your tattoos!"
								+ "<br/>[style.italicsArcane(Visit Kate to get the tattoo removed!)]",
						null);
			}
		}

		if(ownsKey || Main.game.getPlayer().getEssenceCount()>=removalCost) {
			return new Response("Unseal "+(ownsKey?"([style.italicsGood(Use key)])":"([style.italicsArcane("+removalCost+" Essences)])"),
						ownsKey
							?"As you own the key which unlocks this piece of clothing, you can remove it without having to spend any arcane essences!"
							:"Spend "+removalCost+" arcane essences on unsealing from this piece of clothing.",
						interactionType==InventoryInteraction.SEX
							?Main.sex.SEX_DIALOGUE
							:INVENTORY_MENU) {
				@Override
				public void effects() {
					String s = "";
					if(ownsKey) {
						if(!Main.game.isInSex()) {
							Main.game.getPlayer().removeFromUnlockKeyMap(owner.getId(), clothing.getSlotEquippedTo());
						}
						s = "<p>"
								+ "Using the key which is in your possession, you unlock the "+clothing.getName()+"!"
							+ "</p>";

					} else {
						Main.game.getPlayer().incrementEssenceCount(-removalCost, false);
						s = UtilText.parse(owner,
								"<p>"
									+ "You channel the power of your arcane essences into [npc.namePos] "+clothing.getName()+", and with a bright purple flash, you manage to remove the seal!"
								+ "</p>"
								+ "<p style='text-align:center;'>"
									+ "Removing the seal has cost you [style.boldBad("+removalCost+")] [style.boldArcane(Arcane Essences)]!"
								+ "</p>");
					}

					// Have to remove and then re-add the clothing as setting the sealed status affects the clothing's hashCode
					List<DisplacementType> clothingDisplacementTypes = new ArrayList<>();
					if(Main.game.isInSex() && Main.sex.getAllParticipants().contains(owner)) {
						clothingDisplacementTypes.addAll(Main.sex.getClothingPreSexMap().get(owner).get(clothing.getSlotEquippedTo()).get(clothing));
						Main.sex.getClothingPreSexMap().get(owner).get(clothing.getSlotEquippedTo()).remove(clothing);
					}
					clothing.setSealed(false);
					if(Main.game.isInSex() && Main.sex.getAllParticipants().contains(owner)) {
						Main.sex.getClothingPreSexMap().get(owner).get(clothing.getSlotEquippedTo()).put(clothing, clothingDisplacementTypes);
					}

					if(interactionType==InventoryInteraction.SEX) {
						Main.sex.setUnequipClothingText(clothing, s);
						Main.mainController.openInventory();
						Main.sex.endSexTurn(SexActionUtility.CLOTHING_REMOVAL);
						Main.sex.setSexStarted(true);

					} else {
						Main.game.getTextEndStringBuilder().append(s);
					}
				}
			};

		} else {
			return new Response("Unseal (<i>"+removalCost+" Essences</i>)",
					"You need at least "+removalCost+" arcane essences in order to unseal this piece of clothing!",
					null);
		}
	}



	// Items:

	private static void transferItems(GameCharacter from, GameCharacter to, AbstractItem item, int count) {
		if (!to.isInventoryFull() || to.hasItem(item) || item.getRarity()==Rarity.QUEST) {
			from.removeItem(item, count);
			to.addItem(item, count, false, to.isPlayer());
		}
		resetPostAction();
	}

	private static void dropItems(GameCharacter from, AbstractItem item, int count) {
		if (!Main.game.getPlayerCell().getInventory().isInventoryFull() || Main.game.getPlayerCell().getInventory().hasItem(item)) {
			from.dropItem(item, count, from.isPlayer());
		}
		resetPostAction();
	}

	private static void pickUpItems(GameCharacter to, AbstractItem item, int count) {
		if (!to.isInventoryFull() || to.hasItem(item) || item.getRarity()==Rarity.QUEST) {
			to.addItem(item, count, true, to.isPlayer());
		}
		resetPostAction();
	}

	private static void sellItems(GameCharacter from, GameCharacter to, AbstractItem item, int count, int itemPrice) {
		Main.game.getDialogueFlags().setFlag(DialogueFlagValue.removeTraderDescription, false);

		if (!to.isPlayer() || !to.isInventoryFull() || to.hasItem(item) || item.getRarity()==Rarity.QUEST) {
			from.incrementMoney(itemPrice*count);
			to.incrementMoney(-itemPrice*count);

			if(buyback && to.isPlayer()) {
				Main.game.getPlayer().addItem(item, count, false, true);
				Main.game.getPlayer().getBuybackStack().get(buyBackIndex).incrementCount(-count);
				if(Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getCount()<=0) {
					Main.game.getPlayer().getBuybackStack().remove(buyBackIndex);
				}

			} else {
				if(from.isPlayer()) {
					Main.game.getPlayer().getBuybackStack().push(new ShopTransaction(item, itemPrice, count));
				} else {
					to.addItem(item, count, false, true);
				}
				from.removeItem(item, count);
			}

			if(from.isPlayer()) {
				Main.game.addEvent(new EventLogEntry("Sold",
								count+"x <span style='color:"+item.getRarity().getColour().toWebHexString()+";'>"+(count==1?item.getName():item.getNamePlural())+"</span> for "+UtilText.formatAsMoney(itemPrice*count)),
						false);
			}

			if(to.isPlayer()) {
				//((NPC) from).handleSellingEffects(item, count, itemPrice); // Delete: This was replaced by applyItemTransactionEffects
				Main.game.addEvent(new EventLogEntry("Bought",
								count+"x <span style='color:"+item.getRarity().getColour().toWebHexString()+";'>"+(count==1?item.getName():item.getNamePlural())+"</span> for "+UtilText.formatAsMoney(itemPrice*count)),
						false);
			}

			if(!from.isPlayer()) {
				((NPC)from).applyItemTransactionEffects(item, count, itemPrice, true);

			} else {
				((NPC)to).applyItemTransactionEffects(item, count, itemPrice, false);
			}
		}

		resetPostAction();
	}


	// Weapons:

	private static void transferWeapons(GameCharacter from, GameCharacter to, AbstractWeapon weapon, int count) {
		if (!to.isInventoryFull() || to.hasWeapon(weapon) || weapon.getRarity()==Rarity.QUEST) {
			from.removeWeapon(weapon, count);
			to.addWeapon(weapon, count, false, to.isPlayer());
		}
		resetPostAction();
	}

	private static void dropWeapons(GameCharacter from, AbstractWeapon weapon, int count) {
		if (!Main.game.getPlayerCell().getInventory().isInventoryFull() || Main.game.getPlayerCell().getInventory().hasWeapon(weapon)) {
			from.dropWeapon(weapon, count, from.isPlayer());
		}
		resetPostAction();
	}

	private static void pickUpWeapons(GameCharacter to, AbstractWeapon weapon, int count) {
		if (!to.isInventoryFull() || to.hasWeapon(weapon) || weapon.getRarity()==Rarity.QUEST) {
			to.addWeapon(weapon, count, true, to.isPlayer());
		}
		resetPostAction();
	}

	private static void sellWeapons(GameCharacter from, GameCharacter to, AbstractWeapon weapon, int count, int itemPrice) {
		Main.game.getDialogueFlags().setFlag(DialogueFlagValue.removeTraderDescription, false);
		if (!to.isPlayer() || !to.isInventoryFull() || to.hasWeapon(weapon) || weapon.getRarity()==Rarity.QUEST) {

			from.incrementMoney(itemPrice*count);
			to.incrementMoney(-itemPrice*count);

			if(buyback && to.isPlayer()) {
				Main.game.getPlayer().addWeapon(Main.game.getItemGen().generateWeapon(weapon), count, false, true);
				Main.game.getPlayer().getBuybackStack().get(buyBackIndex).incrementCount(-count);
				if(Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getCount()<=0) {
					Main.game.getPlayer().getBuybackStack().remove(buyBackIndex);
				}

			} else {
				if(from.isPlayer()) {
					Main.game.getPlayer().getBuybackStack().push(new ShopTransaction(weapon, itemPrice, count));
				} else {
					to.addWeapon(Main.game.getItemGen().generateWeapon(weapon), count, false, true);
				}
				from.removeWeapon(weapon, count);
			}

			if(from.isPlayer()) {
				Main.game.addEvent(new EventLogEntry("Sold",
								count+"x <span style='color:"+weapon.getRarity().getColour().toWebHexString()+";'>"+(count==1?weapon.getName():weapon.getNamePlural())+"</span> for "+UtilText.formatAsMoney(itemPrice*count)),
						false);
			}

			if(to.isPlayer()) {
				//((NPC) from).handleSellingEffects(weapon, count, itemPrice); // Delete: This was replaced by applyItemTransactionEffects
				Main.game.addEvent(new EventLogEntry("Bought",
								count+"x <span style='color:"+weapon.getRarity().getColour().toWebHexString()+";'>"+(count==1?weapon.getName():weapon.getNamePlural())+"</span> for "+UtilText.formatAsMoney(itemPrice*count)),
						false);
			}

			if(!from.isPlayer()) {
				((NPC)from).applyItemTransactionEffects(weapon, count, itemPrice, true);

			} else {
				((NPC)to).applyItemTransactionEffects(weapon, count, itemPrice, false);
			}
		}


		resetPostAction();
	}


	// Clothing:

	private static void transferClothing(GameCharacter from, GameCharacter to, AbstractClothing clothing, int count) {
		if (!to.isInventoryFull() || to.hasClothing(clothing) || clothing.getRarity()==Rarity.QUEST) {
			from.removeClothing(clothing, count);
			to.addClothing(clothing, count, false, to.isPlayer());
			owner = to;
		}
		resetPostAction();
	}
	
	
	private static void dropClothing(GameCharacter from, AbstractClothing clothing, int count) {
		if (!Main.game.getPlayerCell().getInventory().isInventoryFull() || Main.game.getPlayerCell().getInventory().hasClothing(clothing)) {
			from.dropClothing(clothing, count, from.isPlayer());

			if(from.getClothingCount(clothing) == 0) {
				owner = null;
			}
		}
		resetPostAction();
	}

	private static void pickUpClothing(GameCharacter to, AbstractClothing clothing, int count) {
		if (!to.isInventoryFull() || to.hasClothing(clothing) || clothing.getRarity()==Rarity.QUEST) {
			to.addClothing(clothing, count, true, to.isPlayer());

			owner = to;
		}
		resetPostAction();
	}

	private static void sellClothing(GameCharacter from, GameCharacter to, AbstractClothing clothing, int count, int itemPrice) {
		Main.game.getDialogueFlags().setFlag(DialogueFlagValue.removeTraderDescription, false);
		if (!to.isPlayer() || !to.isInventoryFull() || to.hasClothing(clothing) || clothing.getRarity()==Rarity.QUEST) {
			from.incrementMoney(itemPrice*count);
			to.incrementMoney(-itemPrice*count);

			if(buyback && to.isPlayer()) {
				Main.game.getPlayer().addClothing(new AbstractClothing(clothing) {}, count, false, true);
				Main.game.getPlayer().getBuybackStack().get(buyBackIndex).incrementCount(-count);
				if(Main.game.getPlayer().getBuybackStack().get(buyBackIndex).getCount()<=0) {
					Main.game.getPlayer().getBuybackStack().remove(buyBackIndex);
				}

			} else {
				if(from.isPlayer()) {
					Main.game.getPlayer().getBuybackStack().push(new ShopTransaction(clothing, itemPrice, count));
				} else {
					to.addClothing(new AbstractClothing(clothing) {}, count, false, true);
				}
				from.removeClothing(clothing, count);
			}

			if(from.isPlayer()) {
				Main.game.addEvent(new EventLogEntry("Sold",
								count+"x <span style='color:"+clothing.getRarity().getColour().toWebHexString()+";'>"+(count==1?clothing.getName():clothing.getNamePlural())+"</span> for "+UtilText.formatAsMoney(itemPrice*count)),
						false);
			}

			if(to.isPlayer()) {
				//((NPC) from).handleSellingEffects(clothing, count, itemPrice); // Delete: This was replaced by applyItemTransactionEffects
				Main.game.addEvent(new EventLogEntry("Bought",
								count+"x <span style='color:"+clothing.getRarity().getColour().toWebHexString()+";'>"+(count==1?clothing.getName():clothing.getNamePlural())+"</span> for "+UtilText.formatAsMoney(itemPrice*count)),
						false);
			}

			if(!from.isPlayer()) {
				((NPC)from).applyItemTransactionEffects(clothing, count, itemPrice, true);

			} else {
				((NPC)to).applyItemTransactionEffects(clothing, count, itemPrice, false);
			}
		}

		resetPostAction();
	}
	
	private static String unequipClothingToFloor(GameCharacter unequipper, AbstractClothing clothing) {
		String unequipDescription = "";
		if(clothing.isDiscardedOnUnequip(clothing.getSlotEquippedTo())) {
			unequipDescription = owner.unequipClothingIntoVoid(clothing, true, unequipper);
		} else {
			unequipDescription = owner.unequipClothingOntoFloor(clothing, true, unequipper);
		}
		owner = null;
		resetPostAction();

		return unequipDescription;
	}
	
	private static String unequipClothingToUnequippersInventory(GameCharacter unequipper, AbstractClothing clothing) {
		String unequipDescription = "";
		if(clothing.isDiscardedOnUnequip(clothing.getSlotEquippedTo())) {
			unequipDescription = owner.unequipClothingIntoVoid(clothing, true, unequipper);
		} else {
			unequipDescription = owner.unequipClothingIntoUnequippersInventory(clothing, true, unequipper);
		}
		resetPostAction();

		return unequipDescription;
	}
	
	private static String unequipClothingToInventory(GameCharacter unequipper, AbstractClothing clothing) {
		String unequipDescription = "";
		if(clothing.isDiscardedOnUnequip(clothing.getSlotEquippedTo())) {
			unequipDescription = owner.unequipClothingIntoVoid(clothing, true, unequipper);
		} else {
			unequipDescription = owner.unequipClothingIntoInventory(clothing, true, unequipper);
		}
		resetPostAction();

		return unequipDescription;
	}

	private static String equipClothingFromInventory(GameCharacter to, InventorySlot slot, GameCharacter equipper, AbstractClothing clothing) {
		String equipDescription = to.equipClothingFromInventory(clothing, slot, true, equipper, owner);
		owner = to;
		resetPostAction();
		return equipDescription;
	}

	private static String equipClothingFromGround(GameCharacter to, InventorySlot slot, GameCharacter equipper, AbstractClothing clothing) {
		owner = to;
		resetPostAction();
		return to.equipClothingFromGround(clothing, slot, true, equipper);
	}
	
	private static Response getCondomSabotageResponse(AbstractClothing clothing) {
		if(clothing.getCondomEffect().getPotency().isNegative()) {
			if(Main.game.getPlayer().getEssenceCount() >= 1) {
				return new Response("Repair ([style.italicsArcane(1 Essence)])",
						"Spend 1 arcane essence to repair the condom.", CLOTHING_INVENTORY) {
					@Override
					public void effects() {
						Main.game.getPlayer().incrementEssenceCount(-1, false);
						Main.game.getTextEndStringBuilder().append(
								"<p>"
									+ "You channel the power of an arcane essence into the condom, and, after emitting a faint purple glow, it is repaired!"
								+ "</p>"
								+ "<p style='text-align:center;'>"
									+ "Repairing the condom has cost you [style.boldBad(1)] [style.boldArcane(Arcane Essence)]!"
								+ "</p>");
						AbstractClothing c = (AbstractClothing) EnchantmentDialogue.craftAndApplyFullInventoryEffects(clothing, clothing.getClothingType().getEffects());

						Main.game.getPlayer().removeClothing(c);
						c.setName(c.getClothingType().getName());
						setClothing(c);
						Main.game.getPlayer().addClothing(c, false);

						RenderingEngine.setPage(Main.game.getPlayer(), c);
					}
				};
			} else {
				return new Response("Repair (<i>1 Essence</i>)", "You need at least 1 arcane essence in order to repair the condom!", null);
			}

		} else {
			return new Response("Sabotage", "By making a small tear in the end of this condom, you can ensure that it will break at the moment of orgasm!", CLOTHING_INVENTORY) {
				@Override
				public void effects(){
					EnchantmentDialogue.setOutputName(clothing.getClothingType().getName());
					AbstractClothing c = (AbstractClothing) EnchantmentDialogue.craftAndApplyFullInventoryEffects(clothing,
							Util.newArrayListOfValues(new ItemEffect(ItemEffectType.CLOTHING, TFModifier.CLOTHING_CONDOM, TFModifier.ARCANE_BOOST, TFPotency.MAJOR_DRAIN, 0)),
							false);

					Main.game.getPlayer().removeClothing(c);
					setClothing(c);
					Main.game.getPlayer().addClothing(c, false);

					RenderingEngine.setPage(Main.game.getPlayer(), c);
					Main.game.getTextEndStringBuilder().append(
							"<p>"
								+ "By making a tiny, near-invisible tear in the end of the condom, you ensure that it will split when filled with cum..."
							+ "</p>"
							+ "<p style='text-align:center;'>"
								+ "[style.italicsBad(The condom is now guaranteed to break upon orgasm)]!"
							+ "</p>");
				}
			};
		}
	}


	public static Map<AbstractCoreItem, AbstractCoreItem> usableItems = new HashMap<>();
	public static CharacterInventory playerInventory;
	public static CharacterInventory groundInventory;
	public static MilkingRoom milkingRoom;
	public static AbstractCoreItem chosenItem; //base item
	public static AbstractCoreItem selectedItem; //item to use with
	public static AbstractCoreItem chosenItemPreview; //target item
	public static AbstractCoreItem selectedItemPreview; //item to use with
	public static float fluidAmount = 0;
	public static Set<FluidStored> selectedFluid = new HashSet<>();
	public static float selectedMillilitres = 0;
	public static boolean preciseTransfer; //true: down to ml select fluids, false: in 1/8 increments, all the fluids at the same time
	//stores all transfers, [0] -> giver item, [1] -> storedFluid (incl quantity), [2] -> reciever item
	//has a parallel where the fluid is negative for transferring at confirmation.
	public static List<Object[]> transferLog;

	private static void initFluidManager(){
		transferLog = new ArrayList<>();
		chosenItem = item;
		if(chosenItem == null){
			chosenItem = clothing;
			if(chosenItem == null){
				chosenItem = weapon;
			}
		}

		playerInventory = Main.game.getPlayer().getInventory();
		groundInventory = Main.game.getPlayer().getCell().getInventory();
		updateUsableItems();
	}

	private static void updateUsableItems(){
		AbstractCoreItem finalItem = chosenItem;
		usableItems.clear();
		List<AbstractCoreItem> items = new ArrayList<>();
		items.addAll(playerInventory.getAllItemsInInventory().keySet().stream()
				.filter(i -> i instanceof FluidContainerInterface && !i.equals(finalItem)).collect(Collectors.toList()));
		items.addAll(playerInventory.getAllClothingInInventory().keySet().stream()
				.filter(i -> i instanceof FluidContainerInterface && !i.equals(finalItem)).collect(Collectors.toList()));
		items.addAll(playerInventory.getAllWeaponsInInventory().keySet().stream()
				.filter(i -> i instanceof FluidContainerInterface && !i.equals(finalItem)).collect(Collectors.toList()));

		items.addAll(groundInventory.getAllItemsInInventory().keySet().stream()
				.filter(i -> i instanceof FluidContainerInterface && !i.equals(finalItem)).collect(Collectors.toList()));
		items.addAll(groundInventory.getAllClothingInInventory().keySet().stream()
				.filter(i -> i instanceof FluidContainerInterface && !i.equals(finalItem)).collect(Collectors.toList()));
		items.addAll(groundInventory.getAllWeaponsInInventory().keySet().stream()
				.filter(i -> i instanceof FluidContainerInterface && !i.equals(finalItem)).collect(Collectors.toList()));

		for(AbstractCoreItem item : items){
			usableItems.put(item, getClone(item));
		}
	}

	private static void applyTransfers(){
		for(Object[] entry :  transferLog){
			AbstractCoreItem item = ((AbstractCoreItem) entry[0]);
			if(item != null){
				FluidStored fluid = (FluidStored) entry[1];
				((FluidContainerInterface)item).removeFluid(fluid, fluid.getMillilitres());
			}
			item = ((AbstractCoreItem)entry[2]);
			if(item != null){
				FluidStored fluid = (FluidStored) entry[1];
				((FluidContainerInterface)item).addFluid(fluid);
			}
		}
		transferLog.clear();
	}

	private static String renderGauge(FluidContainerInterface container, boolean left){

		try {
			InputStream is = InventoryDialogue.class.getResourceAsStream("/com/lilithsthrone/res/UIElements/fluidGauge.svg");
			String s = Util.inputStreamToString(is);

			is.close();

			if(container == null){

				//s = SvgUtil.colourReplacement(String.valueOf(5318008), new Colour(Color.color(0.1f, 0.1f, 0.1f)), s);
				s = s.replaceAll("fill-opacity:0.999", "fill-opacity:0.001");//making all fluid-based gradients invisible

				return s;
			}
			//setting colors for base and fluid
			float millilitres = container.getMillilitresStored();
			float level = 1 - (1/container.getMaxCapacity())*(Math.min(millilitres, container.getMaxCapacity()));
			float position = level;
			Colour c = null;
			Colour firstColour = null;
			boolean rainbow = false;
			String hashCode = String.valueOf(Math.abs(container.hashCode()+2));
			//glowin stuff check
			//if any glow, unhide glow layer, already stretched horizontally +10%
			boolean glowing = !container.getStoredFluids().isEmpty() && container.getStoredFluids().stream().anyMatch(fluidStored -> fluidStored.isGlowing());
			//stretch vertically +5% down
			boolean bottomglow = glowing && container.getStoredFluids().get(0).isGlowing();
			//stretch vertically +5% up
			boolean topglow = glowing && container.getStoredFluids().get(container.getStoredFluids().size()-1).isGlowing();
			float alpha = 0;
			float fistAlpha = 0;
			if(glowing) s=s.replace("0.001337", "1");

			for(int i = 0; i<container.getStoredFluids().size(); i++){
				FluidStored fluid = container.getStoredFluids().get(i);
				if(fluid.getMillilitres()<0.01) continue;
				float relativeQPrevious =  i==0?9999:(fluid.getMillilitres()/Math.max(0.01f, container.getStoredFluids().get(i-1).getMillilitres()));
				float relativeQNext = i==container.getStoredFluids().size()-1?9999:(fluid.getMillilitres()/Math.max(0.01f, container.getStoredFluids().get(i+1).getMillilitres()));
				String fluidColor = "";
				String glowColor = "";
				String glowWhite = "";
				alpha = fluid.isGlowing()?1:0;

				if(fluid.getColour().isRainbow()){
					rainbow = true;

					for (String color : fluid.getColour().getRainbowColours()){

						c = new Colour(Color.valueOf(color));
						position += 0.5*(1-level)*fluid.getMillilitres()/(millilitres*(fluid.getColour().getRainbowColours().size()));
						String col = "<stop\n         "
								+ "id=\"color\"\n         "
								+ "style=\"stop-color:#ff8080;stop-opacity:1;\"\n         "
								+ "offset=\"" + position + "\" />\n      ";
						fluidColor += SvgUtil.colourReplacement(hashCode, c, col);

						col = "<stop\n         "
								+ "id=\"color\"\n         "
								+ "style=\"stop-color:#ffd5d5;stop-opacity:" + alpha + "\"\n         "
								+ "offset=\"" + position + "\" />\n      ";
						glowColor += SvgUtil.colourReplacement(hashCode, c, col);

						col = "<stop\n         "
								+ "id=\"color\"\n         "
								+ "style=\"stop-color:#ffffff;stop-opacity:" + alpha + "\"\n         "
								+ "offset=\"" + position + "\" />\n      ";
						glowWhite += SvgUtil.colourReplacement(hashCode, c, col);
						position += 0.5*(1-level)*fluid.getMillilitres()/(millilitres*(fluid.getColour().getRainbowColours().size()));
					}

				} else {
					float modifier = 1;
					if(fluid.hasFluidModifier(FluidModifier.VISCOUS)){
						modifier = 0.4f;
					}
					//the larger the fluid relatively the smaller the bounds, from 40% to 0% (edge of fluid)
					float boundF = (Math.min(0.4f,0.4f/relativeQPrevious))*modifier;
					float boundL = (Math.min(0.4f,0.4f/relativeQNext))*modifier;
					float mid = 1-boundF-boundL;

					c = fluid.getColour();
					position += boundF*(1-level)*fluid.getMillilitres()/millilitres;
					String col = "<stop\n         "
							+ "id=\"color\"\n         "
							+ "style=\"stop-color:#ff8080;stop-opacity:1\"\n         "
							+ "offset=\"" + position + "\" />\n      ";
					fluidColor += SvgUtil.colourReplacement(hashCode, c, col);

					col = "<stop\n         "
							+ "id=\"color\"\n         "
							+ "style=\"stop-color:#ffd5d5;stop-opacity:" + alpha + "\"\n         "
							+ "offset=\"" + position + "\" />\n      ";
					glowColor += SvgUtil.colourReplacement(hashCode, c, col);

					col = "<stop\n         "
							+ "id=\"color\"\n         "
							+ "style=\"stop-color:#ffffff;stop-opacity:" + alpha + "\"\n         "
							+ "offset=\"" + position + "\" />\n      ";
					glowWhite += SvgUtil.colourReplacement(hashCode, c, col);

					position += mid*(1-level)*fluid.getMillilitres()/millilitres;

					col = "<stop\n         "
							+ "id=\"color\"\n         "
							+ "style=\"stop-color:#ff8080;stop-opacity:1\"\n         "
							+ "offset=\"" + position + "\" />\n      ";
					fluidColor += SvgUtil.colourReplacement(hashCode, c, col);

					col = "<stop\n         "
							+ "id=\"color\"\n         "
							+ "style=\"stop-color:#ffd5d5;stop-opacity:" + alpha + "\"\n         "
							+ "offset=\"" + position + "\" />\n      ";
					glowColor += SvgUtil.colourReplacement(hashCode, c, col);

					col = "<stop\n         "
							+ "id=\"color\"\n         "
							+ "style=\"stop-color:#ffffff;stop-opacity:" + alpha + "\"\n         "
							+ "offset=\"" + position + "\" />\n      ";
					glowWhite += SvgUtil.colourReplacement(hashCode, c, col);
					position += boundL*(1-level)*fluid.getMillilitres()/millilitres;

				}
				s=s.replace("<stop\n         id=\"fluidStart\"", fluidColor + "<stop\n         id=\"fluidStart\"");
				s=s.replace("<stop\n         id=\"glowStart\"", glowColor + "<stop\n         id=\"glowStart\"");
				s=s.replace("<stop\n         id=\"glowWhiteStart\"", glowWhite + "<stop\n         id=\"glowWhiteStart\"");
				//overlay
				String overlay = "  <rect\n"
						+ "    id =\"CHOOSE_FLUID_" + fluid.hashCode() + "_" + container.hashCode() + "\"\n"
						+ "    style=\"fill:#ffffff;fill-opacity:0.001;stroke-width:1.5; "
						+ (selectedFluid.contains(fluid) && container == chosenItemPreview
							?"stroke:url(#linearGradientSelectRef);"
							:"")
						+ "\"\n"
						+ "    width=\"80\"\n"
						+ "    height=\"" + 319.18 * (1-level)*fluid.getMillilitres()/millilitres + "\"\n"
						+ "    x=\"8.65\"\n"
						+ "    y=\"" + (30.66 + 319.18 * (position-(1-level)*fluid.getMillilitres()/millilitres)) + "\" >\n"
						+ (left? "    <style>\n"
						+ "      #CHOOSE_FLUID_" + fluid.hashCode() + "_" + container.hashCode() + ":hover { stroke:url(#linearGradientSelectRef); stroke-opacity:1; }\n"
						+ "    </style>\n":"")
						//+ " <set attributeName=\"fill\" to=\"rgba(255, 255, 255, 0.3)\" begin=\"CHOOSE_FLUID_" + hashCode + ".mouseover\""
						//+ " end=\"CHOOSE_FLUID" + hashCode + ".mouseout\"/>"
						+ "  </rect>\n";
				s=s.replaceAll("<overlayStart/>\n", "<overlayStart/>\n" + overlay);

				if(c != null && firstColour == null){
					firstColour = c;
					fistAlpha = alpha;
				}
			}

			//fluid endcap
			String startColor = "<stop\n         "
					+ "id=\"air\"\n         "
					+ "style=\"stop-color:#ff8080;stop-opacity:0\"\n         "
					+ "offset=\"" + (level-0.01) + "\" />\n      "
					+ "<stop\n         "
					+ "id=\"end\"\n         "
					+ "style=\"stop-color:#ff8080;stop-opacity:1\"\n         "
					+ "offset=\"" + level + "\" />\n      ";
			startColor = SvgUtil.colourReplacement(hashCode, firstColour, startColor);
			s=s.replace("linearGradientFluid\">\n      ",
					"linearGradientFluid\">\n      " + startColor);

			//glow endcap
			String startGlow = "<stop\n         "
					+ "id=\"air\"\n         "
					+ "style=\"stop-color:#ffd5d5;stop-opacity:0\"\n         "
					+ "offset=\"" + (level-0.01) + "\" />\n      "
					+ "<stop\n         "
					+ "id=\"end\"\n         "
					+ "style=\"stop-color:#ffd5d5;stop-opacity:" + fistAlpha + "\"\n         "
					+ "offset=\"" + level + "\" />\n      ";
			startGlow = SvgUtil.colourReplacement(hashCode, firstColour, startGlow);
			s=s.replace("linearGradientGlow\">\n      ",
					"linearGradientGlow\">\n      " + startGlow);

			s = s.replace("id=\"glowStart\"\n"
							+ "         style=\"stop-color:#ffd5d5;stop-opacity:1\"",
					SvgUtil.colourReplacement(hashCode, c,
							"id=\"glowStart\"\n"
									+ "         style=\"stop-color:#ffd5d5;stop-opacity:" + alpha + "\""));

			//glowWhite endcap
			String startGlowWhite = "<stop\n         "
					+ "id=\"air\"\n         "
					+ "style=\"stop-color:#ffffff;stop-opacity:0\"\n         "
					+ "offset=\"" + (level-0.01) + "\" />\n      "
					+ "<stop\n         "
					+ "id=\"end\"\n         "
					+ "style=\"stop-color:#ffffff;stop-opacity:" + fistAlpha + "\"\n         "
					+ "offset=\"" + level + "\" />\n      ";
			startGlowWhite = SvgUtil.colourReplacement(hashCode, firstColour, startGlowWhite);
			s=s.replace("linearGradientGlowWhite\">\n      ",
					"linearGradientGlowWhite\">\n      " + startGlowWhite);

			s = s.replace("id=\"glowWhiteStart\"\n"
							+ "         style=\"stop-color:#ffffff;stop-opacity:1\"",
					"id=\"glowWhiteStart\"\n"
							+ "         style=\"stop-color:#ffffff;stop-opacity:" + alpha + "\"");

			//milliliters
			s = s.replaceAll("</text>", Units.fluid(millilitres, Units.UnitType.SHORT) + "</text>");

			//linking IDs to its hashcode
			s=s.replaceAll("(id=\"(\\w|-)*)(\")", "$1" + hashCode + "$3");
			s=s.replaceAll("(url\\(#\\w*)(\\))", "$1" + hashCode + "$2");
			s=s.replaceAll("(xlink:href=\"#\\w*)(\")", "$1" + hashCode + "$2");

			s = SvgUtil.colourReplacement(hashCode, c, s);

			return s;

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return "";
	}

	public static AbstractCoreItem getClone(AbstractCoreItem item){
		//man i wish AbstractCoreItem had Cloneable
		if(item instanceof FluidContainerInterface){
			if(item instanceof AbstractFilledCondom){
				return new AbstractFilledCondom((AbstractFilledCondom) item){};
			} else if(item instanceof AbstractFilledBreastPump){
				return new AbstractFilledBreastPump((AbstractFilledBreastPump) item){};
			}
		} else if(item instanceof AbstractItem){
			return Main.game.getItemGen().generateItem(((AbstractItem) item).getItemType());
		} else if(item instanceof AbstractWeapon){
			return Main.game.getItemGen().generateWeapon((AbstractWeapon) item);
		} else if(item instanceof AbstractClothing){
			return new AbstractClothing((AbstractClothing) item){};
		}
		return null;
	}

	private static String getFluidManagerUi() {
		if(playerInventory == null || chosenItem == null) initFluidManager();

		inventorySB = new StringBuilder();
		preciseTransfer = true;
/*
		System.out.print("log: ");
		for(Object[] log : transferLog){
			System.out.println("giver: " + log[0] + " fluid: " + log[1] + " reciever: " + log[2]);
		}
		System.out.println("");*/

		if(chosenItem != null) chosenItemPreview = usableItems.get(chosenItem);
		if(selectedItem !=null) selectedItemPreview = usableItems.get(selectedItem);

		if(selectedItemPreview == null){
			selectedMillilitres = 0;
		} else {

			selectedMillilitres = selectedFluid.isEmpty()
					?((FluidContainerInterface)chosenItemPreview).getMillilitresStored()
					:(float)selectedFluid.stream().mapToDouble(FluidStored::getMillilitres).sum();
		}
		//left side: chosen item box, millilitre selector box
		inventorySB.append("<div class='container-full-width' style='width:35%; text-align:center; float:left; position:relative; padding:0; margin: 2.5%;'>");

		inventorySB.append("<div class='transfer log' style='height:40%; overflow:scroll;'>"
						+ "<p style='text-align:center;padding:0;margin:0;'><b>→   Event Log   →</b></p>"
						+ "<div class='event-log-inner' id='event-log-inner-id'>");

		if(transferLog.isEmpty()) {
			inventorySB.append("<p style='text-align:center;padding:0;margin:0;'><span style='color:"+PresetColour.TEXT_GREY.toWebHexString()+";'>No transfers yet...</span></p>");
		}

		int count = 0;
		for(Object[] info : transferLog) {
			AbstractCoreItem giver = (AbstractCoreItem) info[0];
			AbstractCoreItem receiver = (AbstractCoreItem) info[2];
			FluidStored fluid = (FluidStored) info[1];
			if(fluid.getMillilitres()<0) continue;
			GameCharacter charactersFluid = fluid.getFluidCharacter();


			if(count%2==0) {
			inventorySB.append("<div class='event-log-entry' style='background:#222222; text-align:center;'>");
			} else {
				inventorySB.append("<div class='event-log-entry' style='background:#1f1f1f; text-align:center;'>");
			}

			inventorySB.append("<div class='inventory-item-slot' style='margin: 0; width:100%; display: flex; flex-wrap: nowrap; height:2em; '>");

			inventorySB.append("<div class='inventory-item-slot' style='margin: 0; width:2em;'>"
					+ "<div class='inventory-icon-content'>"+giver.getSVGString()+"</div>"
					+"</div>");

			String spanStart = (fluid.isGlowing()
					?"[style.glow"
					:"[style.colour")
					+ (fluid.getColour().getFormattingNames()==null
					?fluid.getColour().getName().replaceAll("\\s", "")
					:fluid.getColour().getFormattingNames().get(0).replaceAll("\\s", "")) + "(";

			inventorySB.append("<div class='inventory-item-slot' style=' display: flex; flex-grow: 1; margin: 0;'>");
			//inventorySB.append(" → ");

			if(fluid.getCharactersFluidID() == ""){
					inventorySB.append("<p style ='font-size: 2vw; text-align: center; width: 100%; color:"+fluid.getBaseType().getColour().toWebHexString() + ";'>"
							+ Units.fluid(fluid.getMillilitres(), Units.UnitType.SHORT)+ " of "
							+ (fluid.getType().getRace() == Race.HUMAN?"": fluid.getType().getRace().getDefaultTransformName())
							+ " " + spanStart + Util.randomItemFrom(fluid.getBaseType().getNames()) + ")] </p>");
			} else {
				inventorySB.append("<p style ='font-size: 2vw; text-align: center; width: 100%; color:"+fluid.getBaseType().getColour().toWebHexString() + ";'>"
								+ Units.fluid(fluid.getMillilitres(), Units.UnitType.SHORT) + " of "
								+ "<span style ='color:"+charactersFluid.getFemininity().getColour().toWebHexString() + ";'>"
								+ (charactersFluid.isPlayer()
								?"your"
								:charactersFluid.getName() + "'s") + " </span>"
								+ spanStart + fluid.getName(charactersFluid) + ")]"
								+ "</p>");
			}
			//inventorySB.append(" → ");
			inventorySB.append("</div>");

			inventorySB.append("<div class='inventory-item-slot' style='margin: 0; width:2em;'>"
					+ "<div class='inventory-icon-content'>"+receiver.getSVGString()+"</div>"
					+"</div>");

			inventorySB.append("</div>");
			inventorySB.append("</div>");
			count++;
			//System.out.println(inventorySB.toString());
		}
		inventorySB.append("</div>");
		inventorySB.append("</div>");


		inventorySB.append("<div class='container-full-width' style='float: left;'>");
		int i = 0;
		for(AbstractCoreItem currentItem : usableItems.keySet()){
			i++;
			if(i>30)break;
			inventorySB.append("<div class='inventory-item-slot' style='background-color:" + PresetColour.RARITY_COMMON_BACKGROUND.toWebHexString() + ";'>"
					+ "<div class='inventory-icon-content'>"+currentItem.getSVGString()+"</div>"
					+ "<div class='overlay' id='CHOOSE_" + currentItem.hashCode() + "'></div>"
					+"</div>");

		}
		//empty slots
		for (; i < 30;i++) {
			inventorySB.append("<div class='inventory-item-slot'></div>");
		}

		inventorySB.append("</div>");
		inventorySB.append("</div>");

		//middle
		inventorySB.append("<div class='container-full-width' style='width:15%; text-align:center; float:left; position:relative; padding:0; margin: 5% 0 0;'>");

		inventorySB.append("<div class='container-full-width' style='width:100%; text-align:center; float:left; position:relative; padding:0; margin:0;'>");

		boolean increaseDisabled = fluidAmount>=selectedMillilitres;
		boolean decreaseDisabled = fluidAmount<=0;
		if(preciseTransfer){

			inventorySB.append(
					"<div class='container-full-width' style='width:100%; text-align:center; float:left; position:relative; padding:1%; margin: 0%;'>"

						+ "<div class='container-full-width' style='width:100%; text-align:center; float:right; position:relative; padding:0; margin:0;'>"
							+ "<div id='FLUID_TRANSFER_MAX' class='normal-button"+(increaseDisabled?" disabled":"")+"' style='width:98%; margin:5% 0%; padding:0 1px;'>"
								+ (increaseDisabled?"[style.boldDisabled(max)]":"[style.boldGood(max)]")
							+ "</div>"
							+ "<div id='FLUID_TRANSFER_INCREASE_GIANT' class='normal-button"+(increaseDisabled?" disabled":"")+"' style='width:78%; margin:2% 10%; padding:0;'>"
								+ (increaseDisabled?"[style.boldDisabled(+":"[style.boldGood(+")+Units.fluid(1000)+")]"
							+ "</div>"
							+ "<div id='FLUID_TRANSFER_INCREASE_HUGE' class='normal-button"+(increaseDisabled?" disabled":"")+"' style='width:78%; margin:2% 10%; padding:0 1px;'>"
								+ (increaseDisabled?"[style.boldDisabled(+":"[style.boldGood(+")+Units.fluid(100)+")]"
							+ "</div>"
							+ "<div id='FLUID_TRANSFER_INCREASE_LARGE' class='normal-button"+(increaseDisabled?" disabled":"")+"' style='width:78%; margin:2% 10%; padding:0 1px;'>"
								+ (increaseDisabled?"[style.boldDisabled(+":"[style.boldGood(+")+Units.fluid(10)+")]"
							+ "</div>"
							+ "<div id='FLUID_TRANSFER_INCREASE' class='normal-button"+(increaseDisabled?" disabled":"")+"' style='width:78%; margin:2% 10%; padding:0 1px;'>"
								+ (increaseDisabled?"[style.boldDisabled(":"[style.boldGoodMinor(")+Units.fluid(1)+")]"
							+ "</div>"
						+ "</div>"

						+ "<div class='container-full-width' style='width:100%; margin:1%; padding:20% 0% 20%; text-align:center; float:left; position:relative;'>"
							+ Units.fluid(fluidAmount)
						+ "</div>"

						+ "<div class='container-full-width' style='width:100%; text-align:center; float:left; position:relative; padding:0; margin:0;'>"
							+ "<div id='FLUID_TRANSFER_DECREASE' class='normal-button"+(decreaseDisabled?" disabled":"")+"' style='width:78%; margin:2% 10%; padding:0 1px;'>"
								+ (decreaseDisabled?"[style.boldDisabled(":"[style.boldBadMinor(")+Units.fluid(-1)+")]"
							+ "</div>"
							+ "<div id='FLUID_TRANSFER_DECREASE_LARGE' class='normal-button"+(decreaseDisabled?" disabled":"")+"' style='width:78%; margin:2% 10%; padding:0 1px;'>"
								+ (decreaseDisabled?"[style.boldDisabled(":"[style.boldBad(")+Units.fluid(-10)+")]"
							+ "</div>"
							+ "<div id='FLUID_TRANSFER_DECREASE_HUGE' class='normal-button"+(decreaseDisabled?" disabled":"")+"' style='width:78%; margin:2% 10%; padding:0 1px;'>"
								+ (decreaseDisabled?"[style.boldDisabled(":"[style.boldBad(")+Units.fluid(-100)+")]"
							+ "</div>"
							+ "<div id='FLUID_TRANSFER_DECREASE_GIANT' class='normal-button"+(decreaseDisabled?" disabled":"")+"' style='width:78%; margin:2% 10%; padding:0 1px;'>"
								+ (decreaseDisabled?"[style.boldDisabled(":"[style.boldBad(")+Units.fluid(-1000)+")]"
							+ "</div>"
							+ "<div id='FLUID_TRANSFER_MIN' class='normal-button"+(decreaseDisabled?" disabled":"")+"' style='width:98%; margin:5% 0%; padding:0 1px;'>"
								+ (decreaseDisabled?"[style.boldDisabled(zero)]":"[style.boldBad(zero)]")
							+ "</div>"
						+ "</div>"
					+ "</div>");
		} else {

			inventorySB.append(
				"<div class='container-full-width' style='width:100%; text-align:center; float:left; position:relative; padding:0; margin: 2.5%%;'>"

					+ "<div class='container-full-width' style='width:39%; text-align:center; float:right; position:relative; padding:0; margin:0;'>"
						+ "<div id='FLUID_TRANSFER_MAX' class='normal-button"+(increaseDisabled?" disabled":"")+"' style='width:98%; margin:5% 0%; padding:0 1px;'>"
						+ (increaseDisabled?"[style.boldDisabled(max)]":"[style.boldGood(max)]")
						+ "</div>"
						+ "<div id='FLUID_TRANSFER_INCREASE' class='normal-button"+(increaseDisabled?" disabled":"")+"' style='width:78%; margin:2% 10%; padding:0;'>"
							+ (increaseDisabled?"[style.boldDisabled(+1/8)]":"[style.boldGoodMinor(+1/8)]")
						+ "</div>"
						+ "<div id='FLUID_TRANSFER_INCREASE_LARGE' class='normal-button"+(increaseDisabled?" disabled":"")+"' style='width:78%; margin:2% 10%; padding:0 1px;'>"
							+ (increaseDisabled?"[style.boldDisabled(+1/2)]":"[style.boldGood(+1/2)]")
						+ "</div>"
					+ "</div>"

					+ "<div class='container-full-width' style='width:20%; margin:1%; padding:20% 0% 20%; text-align:center; float:left; position:relative;'>"
						+ Units.fluid(fluidAmount)
					+ "</div>"

					+ "<div class='container-full-width' style='width:39%; text-align:center; float:left; position:relative; padding:0; margin:0;'>"
						+ "<div id='FLUID_TRANSFER_DECREASE' class='normal-button"+(decreaseDisabled?" disabled":"")+"' style='width:78%; margin:2% 10%; padding:0 1px;'>"
							+ (decreaseDisabled?"[style.boldDisabled(-1/8)]":"[style.boldBadMinor(-1/8)]")
						+ "</div>"
						+ "<div id='FLUID_TRANSFER_DECREASE_LARGE' class='normal-button"+(decreaseDisabled?" disabled":"")+"' style='width:78%; margin:2% 10%; padding:0 1px;'>"
							+ (decreaseDisabled?"[style.boldDisabled(-1/2)]":"[style.boldBad(-1/2)]")
						+ "</div>"
						+ "<div id='FLUID_TRANSFER_MIN' class='normal-button"+(increaseDisabled?" disabled":"")+"' style='width:98%; margin:5% 0%; padding:0 1px;'>"
							+ (decreaseDisabled?"[style.boldDisabled(zero)]":"[style.boldBad(zero)]")
						+ "</div>"
					+ "</div>"
				+ "</div>");
		}
		inventorySB.append("</div>");

		inventorySB.append("<div class='container-full-width' style='width:100%; text-align:center; float:left; position:relative; padding:0; margin:10% 0;'>"
				+ "<div id='FLUID_TRANSFER' class='normal-button"+(fluidAmount==0?" disabled":"")+"' style='width:90%; margin:5% 0%; padding:0 1px;'>"
					+ (fluidAmount==0?"[style.boldDisabled(Transfer)]":"[style.boldGood(Transfer)]")
				+ "</div>");

		inventorySB.append("</div>");

		inventorySB.append("</div>");

		//right side
		inventorySB.append("<div class='container-full-width' style='width:40%; text-align:center; float:right; position:relative; padding:0; margin:2.5%;'>");

		//left box: chosen

		inventorySB.append("<div class='container-full-width' style='width:50%; text-align:center; float:left; position:relative; padding:0; margin:0;'>");

		//The gauge:
		//driven by SVG file + code for fluids, fluids are shown with one gradient shared both by transparent (glow) shapes and opaque ones.
		inventorySB.append("<div class='inventory-item-slot' style='margin: 2.5%; width:95%; min-height:19%; height:19%; float:right; background-color:" + PresetColour.RARITY_COMMON_BACKGROUND.toWebHexString() + ";'>"
				+ "<div class='inventory-icon-content'>"+chosenItemPreview.getSVGString()+"</div>"
				+"</div>");

		inventorySB.append(renderGauge((FluidContainerInterface) chosenItemPreview, true));

		inventorySB.append("</div>");
		//right box: selected

		inventorySB.append("<div class='container-full-width' style='width:50%; text-align:center; float:right; position:relative; padding:0; margin:0;'>");

		//The gauge:
		//driven by SVG file + code for fluids, fluids are shown with one gradient shared both by transparent (glow) shapes and opaque ones.
		inventorySB.append("<div class='inventory-item-slot' style='margin: 2.5%; width:95%; min-height:19%; height:19%; float:right; background-color:" + PresetColour.RARITY_COMMON_BACKGROUND.toWebHexString() + ";'>"
				+(selectedItemPreview!=null
					?"<div class='inventory-icon-content'>"+selectedItemPreview.getSVGString()+"</div>"
					:"")
				+"</div>");

		inventorySB.append(renderGauge(selectedItemPreview != null && selectedItemPreview instanceof FluidContainerInterface
				?(FluidContainerInterface) selectedItemPreview
				:null, false));

		inventorySB.append("</div>");

		inventorySB.append("</div>");


/*
		selectedSources = temp;
		//System.out.println(clothing.getCumSources());

		//System.out.println(toyInfo);
		boolean decreaseDisabled = Integer.valueOf(toyInfo.get("fluidAmount").get(0)) < 1;
		boolean increaseDisabled = Integer.valueOf(toyInfo.get("fluidAmount").get(0)) > 100000;
		//List<String> tankSources = clothing.getTankSources();
		boolean fakeCum = Boolean.valueOf(toyInfo.get("fakeFluidInfo").get(0));

		StringBuilder content = new StringBuilder();
		content.setLength(0);

		for(FluidModifier modifier : FluidModifier.values()) {
			if(fakeCum) {
				if(toyInfo.get("fakeFluidInfo").contains(modifier.toString())) {
					content.append(
							"<div id='CUM_MODIFIER_"+modifier+"' class='cosmetics-button active' style='margin: 1px; padding: 1px;'>"
									+ "<span style=' min-width: 60px; margin: 1px; padding: 1px; color:"+modifier.getColour().toWebHexString()+";'>"+Util.capitaliseSentence(modifier.getName())+(modifier.isSpecialEffects()?"*":"")+"</span>"
									+ "</div>");

				} else {
					content.append(
							"<div id='CUM_MODIFIER_"+modifier+"' class='cosmetics-button' style='margin: 1px; padding: 1px;'>"
									+ "<span style=' min-width: 60px; margin: 1px; padding: 1px; color:"+modifier.getColour().getShades()[0]+";'>"+Util.capitaliseSentence(modifier.getName())+(modifier.isSpecialEffects()?"*":"")+"</span>"
									+ "</div>");
				}
			} else {
				content.append(
						"<div id='CUM_MODIFIER_"+modifier+"' class='cosmetics-button disabled' style='margin: 1px; padding: 1px;'>"
								+ "<span style=' min-width: 60px; margin: 1px; padding: 1px; color:"+modifier.getColour().getShades()[0]+";'>"+Util.capitaliseSentence(modifier.getName())+(modifier.isSpecialEffects()?"*":"")+"</span>"
								+ "</div>");
			}
		}


		inventorySB = new StringBuilder(
				"<div class='container-full-width'>"
						+ "<div class='inventoryImage'>"
						+ "<div class='inventoryImage-content'>"
						+ clothing.getSVGString()
						+ "</div>"
						+ "</div>"
						+ "<h3 style='text-align:center;'><b>"+clothing.getDisplayName(true)+"</b></h3>"
						+ "<p>"
						+ (clothing.isOrganic()
						?"Instruct the " + clothing.getName() + " to change its behavior to what you want, from the type fluid it produces, how much of it, or if it lays eggs instead"
						: "Reshape the arcane symbols of your " + clothing.getName() + " to determine what type of semen you want from it, or to take it from a known individual")
						+ (!Main.game.getOccupancyUtil().getMilkingRooms().isEmpty()
						?" or from your milking room"
						:(Main.game.getOccupancyUtil().getMilkingRooms().size()>1
						?"s."
						:"."))
						+ "</p>"
						+ "<div style='width: 74%;'>"
						+"<div class='cosmetics-inner-container' style='margin:0.5% 0.5%; width:40%; padding:0.5%; box-sizing:border-box; position:relative;'>"
						+ "<p style='margin:0; padding:0;'>"
						+ getInformationDiv("TOY_CUM_PRODUCTION", new TooltipInformationEventListener().setInformation(
						"", "How much fluid will be ejaculated upon climax by the " + clothing.getBaseName()
								+ ".<br/>Helpful Reminder:<br/>"
								+"-Targeted persons cannot provide more than their regular ejaculation/hourly milk production volume, nor more than they have stored<br/>"
								+"-Milking room fluid tanks can provide any fluid quantity you want, but no more than what they have stored<br/>"
								+"<i>if the total ejaculation quantity of multiple sources are larger than the set volume, only a wheiged portion will be taken from each</i>"))
						+ "<b>Ejaculate Volume</b>"
						+"</p>"
						+ "<div class='container-full-width' style='width:30%; text-align:center; float:left; position:relative; padding:0; margin:0;'>"
						+ "<div id='TOY_CUM_PRODUCTION_DECREASE' class='normal-button"+(decreaseDisabled?" disabled":"")+"' style='width:98%; margin:1%; padding:0;'>"
						+ (decreaseDisabled?"[style.boldDisabled("+ Units.fluid(-5)+")]":"[style.boldBadMinor("+Units.fluid(-5)+")]")
						+ "</div>"
						+ "<div id='TOY_CUM_PRODUCTION_DECREASE_LARGE' class='normal-button"+(decreaseDisabled?" disabled":"")+"' style='width:98%; margin:1%; padding:0;'>"
						+ (decreaseDisabled?"[style.boldDisabled("+Units.fluid(-50)+")]":"[style.boldBad("+Units.fluid(-50)+")]")
						+ "</div>"
						+ "<div id='TOY_CUM_PRODUCTION_DECREASE_HUGE' class='normal-button"+(decreaseDisabled?" disabled":"")+"' style='width:98%; margin:1%; padding:0;'>"
						+ (decreaseDisabled?"[style.boldDisabled("+Units.fluid(-500)+")]":"[style.boldBad("+Units.fluid(-500)+")]")
						+ "</div>"
						+ "</div>"
						+ "<div class='container-full-width' style='width:38%; margin:1%; padding:0; text-align:center; float:left; position:relative;'>"
						+ Units.fluid(Integer.valueOf(toyInfo.get("fluidAmount").get(0)))
						+ "</div>"
						+ "<div class='container-full-width' style='width:30%; text-align:center; float:right; position:relative; padding:0; margin:0;'>"
						+ "<div id='TOY_CUM_PRODUCTION_INCREASE' class='normal-button"+(increaseDisabled?" disabled":"")+"' style='width:98%; margin:1%; padding:0;'>"
						+ (increaseDisabled?"[style.boldDisabled(+"+Units.fluid(5)+")]":"[style.boldGoodMinor(+"+Units.fluid(5)+")]")
						+ "</div>"
						+ "<div id='TOY_CUM_PRODUCTION_INCREASE_LARGE' class='normal-button"+(increaseDisabled?" disabled":"")+"' style='width:98%; margin:1%; padding:0;'>"
						+ (increaseDisabled?"[style.boldDisabled(+"+Units.fluid(50)+")]":"[style.boldGood(+"+Units.fluid(50)+")]")
						+ "</div>"
						+ "<div id='TOY_CUM_PRODUCTION_INCREASE_HUGE' class='normal-button"+(increaseDisabled?" disabled":"")+"' style='width:98%; margin:1%; padding:0;'>"
						+ (increaseDisabled?"[style.boldDisabled(+"+Units.fluid(500)+")]":"[style.boldGood(+"+Units.fluid(500)+")]")
						+ "</div>"
						+ "</div>"
						+ "</div>"
						+"<div class='cosmetics-inner-container' style='margin:0.5% 0.5%; width:58%; padding:0.5%; box-sizing:border-box; position:relative;'>"
						//+ "<p style='margin:0; padding:0;'>"
						+ getInformationDiv("TOY_CUM_PRODUCTION_FAKE", new TooltipInformationEventListener().setInformation(
						"Fake cum", "Makes the " + clothing.getBaseName() + " produce fake cum mimicking normal jizz, with related fluid effects, but completely unable to impregnate the user"
								+ ".<br/>"
								+"<i>overrides any selected fluid sources</i>"))
						//+"</p>"
						+ "<div id='TOY_SET_FAKE' class='cosmetics-button"+(fakeCum?" active":"")+"'>"
						+ "<span style=' min-width: 60px; margin: 1px; padding: 1px;'>Fake Cum</span>"
						+ "</div>"
						+ "<br/>"
						+content
						+ "</div>"
						+ "</div>"
						//+ "<input type='range' min='1' max='100' value='50' style='width: 73%; background-fill: #713c7b;' id='myRange'>"

						+ "</div>"

						+ "<br/>"
		);

		inventorySB.append(
				"<div style='position:relative; display:inline-block; padding-bottom:0; margin 0 auto; vertical-align:middle; width:100%; text-align:center;'>"
						+ "<i>"
						+ "Find the name of your target in a list of sources."
						+ "</i>"
						+ "<br/>"
						//+ "<p style='display:inline-block; padding:0; margin:0; height:32px; line-height:32px; width:100px;'>First name: </p>"
						+ "<div style=' width:59%; text-align:center; float: left;'>"
						//+ "</form style='display:inline-block; padding:0; margin:0; text-align:center;'>"
						+ "<input type='text' id='nameInput' placeholder='Name' value='"+ name+ "' style='width: 20%; float: left; margin: 2vw 2vw;'>"//</form>"
						//+ "<p style='display:inline-block; padding:0; margin:0; height:32px; line-height:32px; width:100px;'>Surname: </p>"
						//+ "<form style='display:inline-block; padding:0; margin:0; text-align:center;'>"
						+ "<input type='text' id='surnameInput' placeholder='Surname' value='"+ surname + "' style='width: 20%; float: left; margin: 2vw 2vw;'>"//</form>"
						+ "</div>"
						+ "</div>"
						+ "<p id='hiddenFieldName' style='display:none;'></p>"
						+ "<p id='hiddenFieldSurname' style='display:none;'></p>"
						+ "<br/>"

						+ "<div class='container-full-width' style=' width:28%; height: 80vw; margin: 0 1vw; left: 0; overflow: overlay; display: block; text-align: center;'>");

		if(characterFluids.isEmpty()) {
			inventorySB.append("no match found");
		}
		int i = 1;
		for(FluidStored fluid : characterFluids) {
			if(i>50) {
				break;
			}
			GameCharacter character = null;
			try {
				character = fluid.getFluidCharacter();
			} catch(Exception ex) {
				System.err.println("somehow, " + fluid.getCharactersFluidID() + " dissapeared");
			}
			//System.out.println("N size: " + character.getName().length());
			//System.out.println("S size: " + character.getSurname().length());
			//System.out.println("F size: " + (0.9f/(float)Math.max(1, (character.getName().length() + character.getSurname().length() + fluid.getFluid().getType().getName(character).toString().length())/20f)));

			if(character != null) {
				String ListName = character.getName();
				String ListSurname = character.getSurname();
				int femininity = character.getFemininityValue();
				AbstractRace race = fluid.getFluid().getType().getRace();
				inventorySB.append(
						//tab colors
						"<div style='border-radius: 5px; width: calc(100% - 17px); background: "+ (i % 2 == 0
								? (selectedSources.contains(fluid)? "#2d1133": PresetColour.BACKGROUND.toWebHexString())
								: (selectedSources.contains(fluid)? "#37143e": PresetColour.BACKGROUND_ALT.toWebHexString()))
								+ "; display: flex; flex-direction: column; float: left; padding: 5px;'>"
								//icon - fuck that
								//name and femininity
								+ "<div style='float: left; margin:2px 2px; position: relative; left: 50%; transform: translate(-50%, 0); max-height: 50%;'>"
								+ "<div class='overlay' id='FLUID_ADD_" + character.hashCode() + "_" + (fluid.isCum()?"CUM":(fluid.isGirlCum()?"GIRLCUM":"MILK")) + "'></div>"
								+ "<b style ='line-height: 0.85; font-size: " + (0.9f/(float)Math.max(1, (character.getName().length() + character.getSurname().length() + fluid.getFluid().getType().getName(character).length())/21f)) + "em; text-align: center;'>"
								+ "<b style='color:"+ Femininity.valueOf(character.getFemininityValue()).getColour().toWebHexString() + "';>"
								+ ListName + " " + ListSurname+ "'s </b> "
								//fluid race, colour
								+ (fluid.isGirlCum()
								? "<b style='color: " + PresetColour.GIRLCUM.toWebHexString() +  ";'>Girlcum </b></b><br/>" + (character.isRaceConcealed()
								? "Human Girlcum"
								: "<b style='color: " + race.getColour().toWebHexString() + ";'>" + race.getName(fluid.isFeral()) + "</b>")
								: (fluid.isMilk()
								? "<b style='color: " + PresetColour.MILK.toWebHexString() +  ";'>Milk </b></b><br/>" + (character.isRaceConcealed()
								? "Human Milk"
								: "<b style='color: " + race.getColour().toWebHexString() + ";'>" + race.getName(fluid.isFeral()) + "</b>")
								: "<b style='color: " + PresetColour.CUM.toWebHexString() +  ";'>Cum </b></b><br/>" + (character.isRaceConcealed()
								? "Human Cum"
								: "<b style='color: " + race.getColour().toWebHexString() + ";'>" + race.getName(fluid.isFeral()) + "</b>")))

								//+"</p>"
								//fluid type, virility and quantity
								//+ "<p>"
								+ "</div>");
				inventorySB.append("<div style='display: flex; align-items: center; justify-content: center;'>");

				inventorySB.append("<div style='max-width: 180px; position: relative; float: left; width: 100%; display: block; overflow: visible; white-space: nowrap; vertical-align: top;'>");

				inventorySB.append((fluid.getFluid().getFluidModifiers().contains(FluidModifier.ADDICTIVE)
						?"<div style='width: 18%; position: relative; margin: 0 auto; max width: 37.5px; display: inline-block; vertical-align: top;'>"
						+ StatusEffect.ADDICTIONS.getSVGString(character)
						+ "<div class='overlay' id='CUM_EFFECT_ADDICTIVE'></div>"
						+ "</div>"
						:""));

				inventorySB.append((fluid.getFluid().getFluidModifiers().contains(FluidModifier.MINERAL_OIL)
						?"<div style='width: 22.5%; position: relative; margin: 0 auto; transform: translate(-7%, -9%); max width: 47.5px; display: inline-block; vertical-align: top;'>"
						+TFModifier.TF_MOD_FLUID_MINERAL_OIL.getSVGString()
						+ "<div class='overlay' id='CUM_EFFECT_MINERAL_OIL'></div>"
						+ "</div>"
						:""));

				inventorySB.append((fluid.getFluid().getFluidModifiers().contains(FluidModifier.HALLUCINOGENIC)
						?"<div style='width: 18%; position: relative; margin: 0 auto; transform: translate(-10%, 2%); max width: 37.5x; display: inline-block; vertical-align: top;'>"
						+StatusEffect.PSYCHOACTIVE.getSVGString(character)
						+ "<div class='overlay' id='CUM_EFFECT_HALLUCINOGENIC'></div>"
						+ "</div>"
						:""));

				InputStream is = TFModifier.TF_MOD_FLAVOUR_BEER.getClass().getResourceAsStream("/com/lilithsthrone/res/crafting/flavours/beer.svg");
				String s = Util.inputStreamToString(is);
				try {
					is.close();
				} catch(Exception ex) {}
				s = SvgUtil.colourReplacement(TFModifier.TF_MOD_FLAVOUR_BEER.toString(), TFModifier.TF_MOD_FLAVOUR_BEER.getColour(), s);

				inventorySB.append((fluid.getFluid().getFluidModifiers().contains(FluidModifier.ALCOHOLIC)
						?"<div style='width: 18%; position: relative; margin: 0 auto; max width: 37.5px; display: inline-block; vertical-align: top;'>"
						+ s
						+ "<div class='overlay' id='CUM_EFFECT_ALCOHOLIC'></div>"
						+ "</div>"
						:"")
						+ "</div>"
						+ "</div>");

				inventorySB.append(
						"<b style='line-height: 0.85; font-size: 0.9em; text-align: left; color:" + fluid.getFluid().getType().getBaseType().getColour().toWebHexString() + "'>"
								+ "<div>"
								+ (fluid.isCum() ? "<div style='float: right;'>Virility: " + Math.round(fluid.getVirility()) + "</div>" : "" )
								+ "<div style='"+(fluid.isCum()?"float: left;":"text-align: center;")+"'>"
								+ Units.fluid(fluid.getMillilitres())+"/"
								+ Units.fluid((fluid.isGirlCum()
								? (character.getVaginaWetness().getValue()*(character.isVaginaSquirter()?5:1))
								: (fluid.isMilk()
								? (character.getBreastRawMilkStorageValue()+character.getBreastCrotchRawMilkStorageValue())
								: character.getPenisRawCumStorageValue())))
								+ "</div>"
								+ "</div>"
								+ "</b>"
								+ "</b>"
								+ "</div>");
			}
			i++;
		}
		inventorySB.append("</div>");

		inventorySB.append("<div class='container-full-width' style=' width:28%; height: 80vw; margin: 0 1vw; left: 0; overflow: overlay; display: block; text-align: center;'>");

		if(milkingRoomFluids.isEmpty()) {
			inventorySB.append("no match found");
		}

		List<String> tankSources = clothing.getTankSources();
		i = 1;
		for(FluidStored fluid : milkingRoomFluids) {
			if(i>50) {
				break;
			}
			GameCharacter character = null;
			try {
				character = fluid.getFluidCharacter();
			} catch(Exception ex) {
				System.err.println("somehow, " + fluid.getCharactersFluidID() + " dissapeared");
			}
			//System.out.println("N size: " + character.getName().length());
			//System.out.println("S size: " + character.getSurname().length());
			//System.out.println("F size: " + (0.9f/(float)Math.max(1, (character.getName().length() + character.getSurname().length() + fluid.getFluid().getType().getName(character).toString().length())/20f)));

			if(character != null) {
				String ListName = character.getName();
				String ListSurname = character.getSurname();
				int femininity = character.getFemininityValue();
				AbstractRace race = fluid.getFluid().getType().getRace();
				inventorySB.append(
						//tab colors
						"<div style='border-radius: 5px; width: calc(100% - 17px); background: "+ (i % 2 == 0
								? (selectedSources.contains(fluid)? "#2d1133": PresetColour.BACKGROUND.toWebHexString())
								: (selectedSources.contains(fluid)? "#37143e": PresetColour.BACKGROUND_ALT.toWebHexString()))
								+ "; display: flex; flex-direction: column; float: left; padding: 5px;'>"
								//icon - fuck that
								//name and femininity
								+ "<div style='float: left; margin:2px 2px; position: relative; left: 50%; transform: translate(-50%, 0); max-height: 50%;'>"
								+ "<div class='overlay' id='FLUIDTANK_ADD_" + fluid.hashCode() + "'></div>"
								+ "<b style ='line-height: 0.85; font-size: " + (0.9f/(float)Math.max(1, (character.getName().length() + character.getSurname().length() + fluid.getFluid().getType().getName(character).length())/21f)) + "em; text-align: center;'>"
								+ "<b style='color:"+ Femininity.valueOf(character.getFemininityValue()).getColour().toWebHexString() + "';>"
								+ ListName + " " + ListSurname+ "'s </b> "
								//fluid race, colour
								+ (fluid.isGirlCum()
								? "<b style='color: " + PresetColour.GIRLCUM.toWebHexString() +  ";'>Girlcum </b></b><br/>" + (character.isRaceConcealed()
								? "Human Girlcum"
								: "<b style='color: " + race.getColour().toWebHexString() + ";'>" + race.getName(fluid.isFeral()) + "</b>")
								: (fluid.isMilk()
								? "<b style='color: " + PresetColour.MILK.toWebHexString() +  ";'>Milk </b></b><br/>" + (character.isRaceConcealed()
								? "Human Milk"
								: "<b style='color: " + race.getColour().toWebHexString() + ";'>" + race.getName(fluid.isFeral()) + "</b>")
								: "<b style='color: " + PresetColour.CUM.toWebHexString() +  ";'>Cum </b></b><br/>" + (character.isRaceConcealed()
								? "Human Cum"
								: "<b style='color: " + race.getColour().toWebHexString() + ";'>" + race.getName(fluid.isFeral()) + "</b>")))

								//+"</p>"
								//fluid type, virility and quantity
								//+ "<p>"
								+ "</div>");
				inventorySB.append("<div style='position: relative; float: left; width: 100%; display: block; overflow: visible; white-space: nowrap; vertical-align: top;'>");

				inventorySB.append((fluid.getFluid().getFluidModifiers().contains(FluidModifier.ADDICTIVE)
						?"<div style='width: 18%; position: relative; margin: 0 auto; max width: 37.5px; display: inline-block; vertical-align: top;'>"
						+StatusEffect.ADDICTIONS.getSVGString(character)
						+ "<div class='overlay' id='CUM_EFFECT_ADDICTIVE'></div>"
						+ "</div>"
						:""));

				inventorySB.append((fluid.getFluid().getFluidModifiers().contains(FluidModifier.MINERAL_OIL)
						?"<div style='width: 22.5%; position: relative; margin: 0 auto; transform: translate(-7%, -9%); max width: 47.5px; display: inline-block; vertical-align: top;'>"
						+TFModifier.TF_MOD_FLUID_MINERAL_OIL.getSVGString()
						+ "<div class='overlay' id='CUM_EFFECT_MINERAL_OIL'></div>"
						+ "</div>"
						:""));

				inventorySB.append((fluid.getFluid().getFluidModifiers().contains(FluidModifier.HALLUCINOGENIC)
						?"<div style='width: 18%; position: relative; margin: 0 auto; transform: translate(-10%, 2%); max width: 37.5x; display: inline-block; vertical-align: top;'>"
						+StatusEffect.PSYCHOACTIVE.getSVGString(character)
						+ "<div class='overlay' id='CUM_EFFECT_HALLUCINOGENIC'></div>"
						+ "</div>"
						:""));

				InputStream is = TFModifier.TF_MOD_FLAVOUR_BEER.getClass().getResourceAsStream("/com/lilithsthrone/res/crafting/flavours/beer.svg");
				String s = Util.inputStreamToString(is);
				try {
					is.close();
				} catch(Exception ex) {}
				s = SvgUtil.colourReplacement(TFModifier.TF_MOD_FLAVOUR_BEER.toString(), TFModifier.TF_MOD_FLAVOUR_BEER.getColour(), s);

				inventorySB.append((fluid.getFluid().getFluidModifiers().contains(FluidModifier.ALCOHOLIC)
						?"<div style='width: 18%; position: relative; margin: 0 auto; max width: 37.5px; display: inline-block; vertical-align: top;'>"
						+ s
						+ "<div class='overlay' id='CUM_EFFECT_ALCOHOLIC'></div>"
						+ "</div>"
						:"")
						+ "</div>");

				inventorySB.append(
						"<b style='line-height: 0.85; font-size: 0.9em; text-align: left; color:" + fluid.getFluid().getType().getBaseType().getColour().toWebHexString() + "'>"
								+ "<div>"
								+ (fluid.isCum() ? "<div style='float: right;'>Virility: " + Math.round(fluid.getVirility()) + "</div>" : "" )
								+ "<div style='"+(fluid.isCum()?"float: left;":"text-align: center;")+"'>"
								+ Units.fluid(fluid.getMillilitres())
								+ "</div>"
								+ "</div>"
								+ "</b>"
								+ "</b>"
								+ "</div>");
			}
			i++;
		}

		inventorySB.append("</div>");

		inventorySB.append("<div class='container-full-width' style=' width:29%; height: 80vw; margin: 0 1vw; overflow: overlay; display: block; text-align: center; float: right;'>");

		if(selectedSources.isEmpty()) {
			inventorySB.append("nothing selected");
		}

		i = 1;
		for(FluidStored fluid : selectedSources) {
			if(i>50) {
				break;
			}
			GameCharacter character = null;
			try {
				character = fluid.getFluidCharacter();
			} catch(Exception ex) {
				System.err.println("somehow, " + fluid.getCharactersFluidID() + " dissapeared");
			}
			if(character != null) {
				String ListName = character.getName();
				String ListSurname = character.getSurname();
				int femininity = character.getFemininityValue();
				AbstractRace race = fluid.getFluid().getType().getRace();
				inventorySB.append(
						//tab colors
						"<div style='border-radius: 5px; width: calc(100% - 17px); background: "+ (i % 2 == 0
								? PresetColour.BACKGROUND.toWebHexString()
								: PresetColour.BACKGROUND_ALT.toWebHexString())
								+ "; display: flex; flex-direction: column; float: left; padding: 5px;'>"
								//icon - fuck that
								//name and femininity
								+ "<div style='float: left; margin:2px 2px; position: relative; left: 50%; transform: translate(-50%, 0); max-height: 50%;'>"
								+ "<div class='overlay' id='FLUID_REMOVE_" + (toyInfo.get("fluidTankSources").contains(String.valueOf(fluid.hashCode()))? fluid.hashCode() + "_TANK":fluid.getCharactersFluidID() + (fluid.isCum()?"_CUM":(fluid.isMilk()?"_MILK":"_GIRLCUM"))) + "'></div>"
								+ "<b style ='line-height: 0.85; font-size: " + (0.9f/(float)Math.max(1, (character.getName().length() + character.getSurname().length() + fluid.getFluid().getType().getName(character).length())/21f)) + "em; text-align: center;'>"
								+ "<b style='color:"+ Femininity.valueOf(character.getFemininityValue()).getColour().toWebHexString() + "';>"
								+ ListName + " " + ListSurname+ "'s </b> "
								//fluid race, colour
								+ (fluid.isGirlCum()
								? "<b style='color: " + PresetColour.GIRLCUM.toWebHexString() +  ";'>Girlcum </b></b><br/>" + (character.isRaceConcealed()
								? "Human Girlcum"
								: "<b style='color: " + race.getColour().toWebHexString() + ";'>" + race.getName(fluid.isFeral()) + "</b>")
								: (fluid.isMilk()
								? "<b style='color: " + PresetColour.MILK.toWebHexString() +  ";'>Milk </b></b><br/>" + (character.isRaceConcealed()
								? "Human Milk"
								: "<b style='color: " + race.getColour().toWebHexString() + ";'>" + race.getName(fluid.isFeral()) + "</b>")
								: "<b style='color: " + PresetColour.CUM.toWebHexString() +  ";'>Cum </b></b><br/>" + (character.isRaceConcealed()
								? "Human Cum"
								: "<b style='color: " + race.getColour().toWebHexString() + ";'>" + race.getName(fluid.isFeral()) + "</b>")))

								//+"</p>"
								//fluid type, virility and quantity
								//+ "<p>"
								+ "</div>");
				inventorySB.append("<div style='position: relative; float: left; width: 100%; display: block; overflow: visible; white-space: nowrap; vertical-align: top;'>");

				inventorySB.append((fluid.getFluid().getFluidModifiers().contains(FluidModifier.ADDICTIVE)
						?"<div style='width: 18%; position: relative; margin: 0 auto; max width: 37.5px; display: inline-block; vertical-align: top;'>"
						+StatusEffect.ADDICTIONS.getSVGString(character)
						+ "<div class='overlay' id='CUM_EFFECT_ADDICTIVE'></div>"
						+ "</div>"
						:""));

				inventorySB.append((fluid.getFluid().getFluidModifiers().contains(FluidModifier.MINERAL_OIL)
						?"<div style='width: 22.5%; position: relative; margin: 0 auto; transform: translate(-7%, -9%); max width: 47.5px; display: inline-block; vertical-align: top;'>"
						+TFModifier.TF_MOD_FLUID_MINERAL_OIL.getSVGString()
						+ "<div class='overlay' id='CUM_EFFECT_MINERAL_OIL'></div>"
						+ "</div>"
						:""));

				inventorySB.append((fluid.getFluid().getFluidModifiers().contains(FluidModifier.HALLUCINOGENIC)
						?"<div style='width: 18%; position: relative; margin: 0 auto; transform: translate(-10%, 2%); max width: 37.5x; display: inline-block; vertical-align: top;'>"
						+StatusEffect.PSYCHOACTIVE.getSVGString(character)
						+ "<div class='overlay' id='CUM_EFFECT_HALLUCINOGENIC'></div>"
						+ "</div>"
						:""));

				InputStream is = TFModifier.TF_MOD_FLAVOUR_BEER.getClass().getResourceAsStream("/com/lilithsthrone/res/crafting/flavours/beer.svg");
				String s = Util.inputStreamToString(is);
				try {
					is.close();
				} catch(Exception ex) {}
				s = SvgUtil.colourReplacement(TFModifier.TF_MOD_FLAVOUR_BEER.toString(), TFModifier.TF_MOD_FLAVOUR_BEER.getColour(), s);

				inventorySB.append((fluid.getFluid().getFluidModifiers().contains(FluidModifier.ALCOHOLIC)
						?"<div style='width: 18%; position: relative; margin: 0 auto; max width: 37.5px; display: inline-block; vertical-align: top;'>"
						+ s
						+ "<div class='overlay' id='CUM_EFFECT_ALCOHOLIC'></div>"
						+ "</div>"
						:"")
						+ "</div>");

				inventorySB.append(
						"<b style='line-height: 0.85; font-size: 0.9em; text-align: left; color:" + fluid.getFluid().getType().getBaseType().getColour().toWebHexString() + "'>"

								+ "<div>"
								+ (fluid.isCum() ? "<div style='float: right;'>Virility: " + Math.round(fluid.getVirility()) + "</div>" : "" )
								+ "<div style='"+(fluid.isCum()?"float: left;":"text-align: center;")+"'>"
								+ Units.fluid(fluid.getMillilitres())+"/"
								+ Units.fluid((fluid.isGirlCum()
								? (character.getVaginaWetness().getValue()*(character.isVaginaSquirter()?5:1))
								: (fluid.isMilk()
								? (character.getBreastRawMilkStorageValue()+character.getBreastCrotchRawMilkStorageValue())
								: character.getPenisRawCumStorageValue())))
								+ "</div>"
								+ "</div>"
								+ "</b>"
								+ "</b>"
								+ "</div>");
			}
			i++;
		}
		inventorySB.append(
				"</div>");*/

		return inventorySB.toString();
	}
	
	private static void resetPostAction() {
		Main.game.setResponseTab(0);
		resetItems();
	}
	
	public static void resetItems() {
		item = null;
		clothing = null;
		weapon = null;
	}

	public static AbstractItem getItem() {
		return item;
	}

	public static void setItem(AbstractItem item) {
		resetItems();
		InventoryDialogue.item = item;
		if(Main.getProperties().addItemDiscovered(item.getItemType())) {
			Main.game.addEvent(new EventLogEntryEncyclopediaUnlock(item.getItemType().getName(false), item.getRarity().getColour()), true);
		}
	}

	public static AbstractWeapon getWeapon() {
		return weapon;
	}

	public static void setWeapon(InventorySlot slot, AbstractWeapon weapon) {
		resetItems();
		InventoryDialogue.weaponSlot = slot;
		InventoryDialogue.weapon = weapon;
		if (Main.getProperties().addWeaponDiscovered(weapon.getWeaponType())) {
			Main.game.addEvent(new EventLogEntryEncyclopediaUnlock(weapon.getWeaponType().getName(), weapon.getWeaponType().getRarity().getColour()), true);
		}
	}

	public static AbstractClothing getClothing() {
		return clothing;
	}

	public static void setClothing(AbstractClothing clothing) {
		resetItems();
		InventoryDialogue.clothing = clothing;
		if(Main.getProperties().addClothingDiscovered(clothing.getClothingType())) {
			Main.game.addEvent(new EventLogEntryEncyclopediaUnlock(clothing.getClothingType().getName(), clothing.getClothingType().getRarity().getColour()), true);
		}
	}

	public static boolean isBuyback() {
		return buyback;
	}

	public static void setBuyback(boolean buyback) {
		InventoryDialogue.buyback = buyback;
	}

	public static int getBuyBackPrice() {
		return buyBackPrice;
	}

	public static void setBuyBackPrice(int buyBackPrice) {
		InventoryDialogue.buyBackPrice = buyBackPrice;
	}

	public static int getBuyBackIndex() {
		return buyBackIndex;
	}

	public static void setBuyBackIndex(int buyBackIndex) {
		InventoryDialogue.buyBackIndex = buyBackIndex;
	}

	public static GameCharacter getOwner() {
		return owner;
	}

	public static void setOwner(GameCharacter owner) {
		InventoryDialogue.owner = owner;
	}

	public static NPC getInventoryNPC() {
		return inventoryNPC;
	}

	public static void setInventoryNPC(NPC inventoryNPC) {
		InventoryDialogue.inventoryNPC = inventoryNPC;
	}

	public static InventoryInteraction getNPCInventoryInteraction() {
		return interactionType;
	}

	public static void setNPCInventoryInteraction(InventoryInteraction npcInventoryInteraction) {
		interactionType = npcInventoryInteraction;
	}

}