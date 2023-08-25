package com.lilithsthrone.game.character;

import com.lilithsthrone.game.character.body.Fluid;
import com.lilithsthrone.game.inventory.clothing.AbstractClothing;
import com.lilithsthrone.game.inventory.enchanting.EffectTarget;
import com.lilithsthrone.game.inventory.enchanting.ItemEffect;
import com.lilithsthrone.game.inventory.weapon.AbstractWeapon;
import com.lilithsthrone.utils.colours.Colour;
import com.lilithsthrone.utils.colours.PresetColour;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.lilithsthrone.controller.xmlParsing.XMLUtil;
import com.lilithsthrone.game.character.attributes.Attribute;
import com.lilithsthrone.game.character.race.AbstractSubspecies;
import com.lilithsthrone.game.character.race.Subspecies;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.utils.XMLSaving;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 0.2.7
 * @version 0.3.1
 * @author Innoxia
 */
public class FluidStored extends Fluid implements XMLSaving {

	private class GeneData {
		private AbstractSubspecies Subspecies; // used for calculating pregnancy.
		private AbstractSubspecies HalfDemonSubspecies; // used for calculating pregnancy.
		private float virility;
		private boolean feral;
		public GeneData(AbstractSubspecies Subspecies, AbstractSubspecies HalfDemonSubspecies, float virility, boolean feral){
			this.Subspecies = Subspecies;
			this.HalfDemonSubspecies = HalfDemonSubspecies;
			this.virility = virility;
			this.feral = feral;
		}
	}
	//GENERAL EXPLANATION:
	//FluidStored is extended from Fluid. In many ways, FluidStored is extruded from it, from a customized production
	//template to a finalized liquid that can be interacted with, having a quantity and geneData which is detached from
	//the person itself to correctly reflect the npc's state then compared to now which may have gone through significant changes.

	private float millilitres;
	private float quantityModifier = 1.0f;
	private GeneData geneData;
	private String charactersFluidID = "";
	private GameCharacter cachedCharacter = null; //for a quicker retrieval of the gamecharacter if it's been accessed before
	private Colour colour;
	private boolean glowing;

	public FluidStored(FluidStored fluid){
		super(fluid);
		this.millilitres = fluid.millilitres;
		this.charactersFluidID = fluid.charactersFluidID;
		this.geneData = fluid.geneData;
		this.colour = fluid.colour;
		this.glowing = fluid.glowing;
	}

	public FluidStored(GameCharacter character, Fluid fluid, float millilitres) {
		super(fluid);

		if(character!=null) {
			this.colour = character.getCovering(this.getBodyCoveringType(character)).getPrimaryColour();
			this.glowing = character.getCovering(this.getBodyCoveringType(character)).isPrimaryGlowing();
			System.out.println("covering color: " + colour.getName());
			this.geneData = new GeneData(character.getSubspecies(),
					character.getHalfDemonSubspecies(),
					character.getAttributeValue(Attribute.VIRILITY),
					character.isFeral());
		} else {
			this.colour = this.getType().getBaseType().getColour();
			this.glowing = false;
			this.geneData = new GeneData(null,
					null,
					25,
					false);
		}

		if(character!=null){
			this.cachedCharacter = character;
			this.charactersFluidID = character.getId();
			this.transformativeEffects = setupTransformationEffects(character);
			quantityModifier = evaluateQuantityModifier(character);
		} else {
			charactersFluidID = "";
		}

		this.millilitres = millilitres;
	}

	public FluidStored(String charactersFluidID, Fluid fluid, float millilitres) {
		super(fluid);

		GameCharacter owner = null;
		try{
			owner = Main.game.getNPCById(charactersFluidID);
		} catch (Exception ex){};

//		[#pc.getCovering(pc.getMilk().getBodyCoveringType(pc)).setPrimaryColour(COLOUR_BASE_BLUE_DARK)]
//		[#pc.getCovering(pc.getMilk().getBodyCoveringType(pc)).getPrimaryColour()]
		if(owner!=null) {
			//owner.getMilk()
			this.colour = owner.getCovering(this.getBodyCoveringType(owner)).getPrimaryColour();
			this.glowing = owner.getCovering(this.getBodyCoveringType(owner)).isPrimaryGlowing();
			this.geneData = new GeneData(owner.getSubspecies(),
					owner.getHalfDemonSubspecies(),
					owner.getAttributeValue(Attribute.VIRILITY),
					owner.isFeral());
		} else {
			this.colour = this.getType().getBaseType().getColour();
			this.glowing = false;
			this.geneData = new GeneData(null,
					null,
					25,
					false);
		}

		if(owner!=null){
			this.cachedCharacter = owner;
			this.charactersFluidID = charactersFluidID;
			this.transformativeEffects = setupTransformationEffects(owner);
			quantityModifier = evaluateQuantityModifier(owner);
		} else {
			this.charactersFluidID = "";
		}

		this.millilitres = millilitres;
	}

	public FluidStored(String charactersFluidID, AbstractSubspecies cumSubspecies, AbstractSubspecies cumHalfDemonSubspecies, Fluid fluid, float millilitres) {
		super(fluid);

		GameCharacter owner = null;
		try{
			owner = Main.game.getNPCById(charactersFluidID);
		} catch (Exception ex){};

		if(owner!=null) {
			this.colour = owner.getCovering(this.getBodyCoveringType(owner)).getPrimaryColour();
			this.glowing = owner.getCovering(this.getBodyCoveringType(owner)).isPrimaryGlowing();
			this.geneData = new GeneData(cumSubspecies,
					cumHalfDemonSubspecies,
					owner.getAttributeValue(Attribute.VIRILITY),
					owner.isFeral());
		} else {
			this.colour = this.getType().getBaseType().getColour();
			this.glowing = false;
			this.geneData = new GeneData(cumSubspecies,
					cumHalfDemonSubspecies,
					25,
					false);
		}

		if(owner!=null){
			this.cachedCharacter = owner;
			this.charactersFluidID = charactersFluidID;
			this.transformativeEffects = setupTransformationEffects(owner);
			quantityModifier = evaluateQuantityModifier(owner);
		} else {
			this.charactersFluidID = "";
		}

		this.millilitres = millilitres;
	}

	public FluidStored(String charactersFluidID,
					   Fluid fluid,
					   AbstractSubspecies Subspecies,
					   AbstractSubspecies HalfDemonSubspecies,
					   boolean feral,
					   float virility,
					   Colour colour,
					   boolean glow,
					   float millilitres) {

		super(fluid);
		this.geneData = new GeneData(Subspecies,
				HalfDemonSubspecies,
				virility,
				feral);
		this.colour = colour;
		this.glowing = glow;
		this.millilitres = millilitres;
		this.charactersFluidID = charactersFluidID;
	}

	@Override
	public boolean equals(Object o) {
		return equals(o, true);
	}

	@Override
	public int hashCode() {
		// Does not take into account quantity on purpose.
		int result = 17;
		result = 31 * result + super.hashCode();
		result = 31 * result + this.getCharactersFluidID().hashCode();
		result = 31 * result + (this.isFeral() ? 1 : 0);
		if(this.getSubspecies()!=null) {
			result = 31 * result + this.getSubspecies().hashCode();
		}
		if(this.getHalfDemonSubspecies()!=null) {
			result = 31 * result + this.getHalfDemonSubspecies().hashCode();
		}
		result = 31 * result + Float.floatToIntBits(this.getVirility());
		result = 31 * result + Float.floatToIntBits(this.getMillilitres());
		result = 31 * result + this.getColour().hashCode();
		result = 31 * result + (this.isGlowing() ? 1 : 0);
		return result;
	}

	@Override
	public Element saveAsXML(Element parentElement, Document doc) {
		// Core:
		Element fluidStoredElement = doc.createElement("fluidStored");
		parentElement.appendChild(fluidStoredElement);

		XMLUtil.addAttribute(doc, fluidStoredElement, "charactersFluidID", charactersFluidID);
		XMLUtil.addAttribute(doc, fluidStoredElement, "feral", String.valueOf(geneData.feral));
		XMLUtil.addAttribute(doc, fluidStoredElement, "virility", String.valueOf(geneData.virility));
		XMLUtil.addAttribute(doc, fluidStoredElement, "Subspecies", Subspecies.getIdFromSubspecies(geneData.Subspecies));
		XMLUtil.addAttribute(doc, fluidStoredElement, "HalfDemonSubspecies", Subspecies.getIdFromSubspecies(geneData.HalfDemonSubspecies));
		XMLUtil.addAttribute(doc, fluidStoredElement, "colour", String.valueOf(colour));
		XMLUtil.addAttribute(doc, fluidStoredElement, "millilitres", String.valueOf(millilitres));

		super.saveAsXML(fluidStoredElement, doc);

		return fluidStoredElement;
	}

	public static FluidStored loadFromXML(StringBuilder log, Element parentElement, Document doc) {
		Fluid fluid = null;
		String ID = parentElement.getAttribute("charactersFluidID");

		float millilitres = Float.parseFloat(parentElement.getAttribute("millilitres"));

		boolean feral = false;
		float virility = 25;
		virility = Float.parseFloat(parentElement.getAttribute("virility"));

		if(parentElement.hasAttribute("bestial")){ //legacy code
			String typeName = "";
			for(int i = 0; i<parentElement.getChildNodes().getLength(); i++){

				if (parentElement.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element) parentElement.getChildNodes().item(i);
					typeName = element.getNodeName();
					break;
				}
			}
			fluid = Fluid.loadFromXML(parentElement, doc, null, typeName);
			return new FluidStored(ID,
					fluid,
					Subspecies.getSubspeciesFromId(parentElement.getAttribute("cumSubspecies")),
					Subspecies.getSubspeciesFromId(parentElement.getAttribute("cumHalfDemonSubspecies")),
					Boolean.parseBoolean(parentElement.getAttribute("bestial")),
					virility,
					fluid.getColour(),
					false,
					millilitres);
		}

		//new code:
		//-bestial hammered off
		//-removed cum from saving structure, since it doesn't have to be unique to it anymore
		//+loads the fluidTemplate it's based on instead of whatever the old thing was
		fluid = Fluid.loadFromXML(parentElement, doc, null, "");
		Colour colour = PresetColour.getColourFromId(parentElement.getAttribute("colour"));
		if(colour == null) colour = fluid.getColour();
		return new FluidStored(ID,
				fluid,
				Subspecies.getSubspeciesFromId(parentElement.getAttribute("Subspecies")),
				Subspecies.getSubspeciesFromId(parentElement.getAttribute("HalfDemonSubspecies")),
				Boolean.parseBoolean(parentElement.getAttribute("feral")),
				virility,
				colour,
				Boolean.valueOf(parentElement.getAttribute("glowing")),
				millilitres);
	}

	public float evaluateQuantityModifier(GameCharacter character){
		float modifier = 1.0f;
		//the smaller of the two: generation per hour or max capacity)
		switch (getType().getSource()){
			case PENIS:
				modifier *= getMillilitres()/Math.min(character.getPenisRawCumProductionRegenerationValue()/24, character.getPenisRawCumStorageValue());
			case BREAST:
				modifier *= getMillilitres()/Math.min(character.getBreastRawLactationRegenerationValue()/24, character.getBreastRawMilkStorageValue());
			case VAGINA:
				modifier *= getMillilitres()/character.getVaginaWetness().getValue()*(character.isVaginaSquirter()?5:1);
			case BREAST_CROTCH:
				modifier *= getMillilitres()/Math.min(character.getBreastCrotchRawLactationRegenerationValue()/24, character.getBreastCrotchRawMilkStorageValue());
		}
		return modifier;
	}


	public boolean equals(Object o, boolean fullCheck) {
		if(o instanceof FluidStored){
			if(super.equals(o)
					&& ((FluidStored)o).getCharactersFluidID().equals(this.getCharactersFluidID())
					&& ((FluidStored)o).isFeral() == this.isFeral()
					&& ((FluidStored)o).getSubspecies().equals(this.getSubspecies())
					&& ((FluidStored)o).getHalfDemonSubspecies().equals(this.getHalfDemonSubspecies())

					&& (!fullCheck
						|| (((FluidStored)o).getVirility() == this.getVirility()
						&& ((FluidStored)o).getMillilitres() == this.getMillilitres()))

					&& ((FluidStored)o).getColour().equals(this.getColour())
					&& ((FluidStored)o).isGlowing() == this.isGlowing()) {
				return true;
			}
		}
		return false;
	}

	private List<ItemEffect> setupTransformationEffects(GameCharacter giver){

		if(giver == null) return selectRandom(getTransformativeEffects(), transformativePower * quantityModifier);

		List<ItemEffect> effects = getTransformativeEffects();
		List<ItemEffect> addedEffects = new ArrayList<>();
		for(AbstractClothing clothing : giver.getClothingCurrentlyEquipped()){
			addedEffects.addAll(clothing.getEffects());
		}
		for(AbstractWeapon wep : giver.getOffhandWeaponArray()){
			addedEffects.addAll(wep.getEffects());
		}
		for(AbstractWeapon wep : giver.getMainWeaponArray()){
			addedEffects.addAll(wep.getEffects());
		}

		effects.addAll(addedEffects.stream().filter(ie -> ie.getTarget().isCorrectFluid(this)).map(ie -> {
			ItemEffect eff = new ItemEffect(ie.getItemEffectType());
			if(ie.isRecursive()){
				eff.setRecursive(true);
				eff.setTarget(ie.getTarget());
			} else {
				eff.setTarget(EffectTarget.SELF);
				eff.setRecursive(false);
			}
			return eff;
		}).collect(Collectors.toList()));

		return selectRandom(effects, transformativePower * quantityModifier);
	}

	private static List<ItemEffect> selectRandom(List<ItemEffect> list, float fraction) {
		List<ItemEffect> result = new ArrayList<>();

		if (fraction <= 0.0) {
			return result; // empty list
		} else if (fraction >= 1.0) {
			int wholePart = (int) fraction;
			float fractionalPart = fraction - wholePart;

			for (int i = 0; i < wholePart; i++) {
				result.addAll(list);
			}

			result.addAll(selectRandom(list, fractionalPart));
		} else {
			List<ItemEffect> shuffledList = new ArrayList<>(list);
			Collections.shuffle(shuffledList);
			int count = (int) (fraction * list.size());
			for (int i = 0; i < count; i++) {
				result.add(shuffledList.get(i));
			}
		}

		return result;
	}

	public String getCharactersFluidID() {
		return charactersFluidID;
	}

	public GameCharacter getFluidCharacter() {
		if(cachedCharacter != null) return cachedCharacter;

		System.err.println("searchid: " + getCharactersFluidID());
		if(charactersFluidID.equals(Main.game.getPlayer().getId())) {
			return Main.game.getPlayer();
		}
		try{
			return Main.game.getNPCById(charactersFluidID);
		} catch (Exception ex){
			return null;
		}
	}

	public AbstractSubspecies getSubspecies() {
		return geneData.Subspecies;
	}

	public AbstractSubspecies getHalfDemonSubspecies() {
		return geneData.HalfDemonSubspecies;
	}

	public boolean isFeral() {
		return geneData.feral;
	}

	public float getVirility() {
		return geneData.virility;
	}

	public void setVirility(float virility) {
		this.geneData.virility = virility;
	}

	@Override
	public Colour getColour() {
		if(colour == null){
			return getType().getBaseType().getColour();
		}
		return colour;
	}

	public void setColour(Colour colour) {
		this.colour = colour;
	}

	public Boolean isGlowing() {
		return glowing;
	}

	public void setGlowing(boolean glow) {
		glowing = glow;
	}

	public float getMillilitres() {
		return millilitres;
	}

	public void setMillilitres(float millilitres) {
		this.millilitres = Math.max(0, millilitres);
	}

	public void incrementMillilitres(float increment) {
		setMillilitres(this.millilitres + increment);
	}


}
