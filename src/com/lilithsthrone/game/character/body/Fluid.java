package com.lilithsthrone.game.character.body;

import java.util.ArrayList;
import java.util.List;

import com.lilithsthrone.controller.xmlParsing.XMLUtil;
import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.character.attributes.Attribute;
import com.lilithsthrone.game.character.body.abstractTypes.AbstractFluidType;
import com.lilithsthrone.game.character.body.coverings.AbstractBodyCoveringType;
import com.lilithsthrone.game.character.body.coverings.BodyCoveringType;
import com.lilithsthrone.game.character.body.types.BodyPartType;
import com.lilithsthrone.game.character.body.valueEnums.FluidFlavour;
import com.lilithsthrone.game.character.body.valueEnums.FluidModifier;
import com.lilithsthrone.game.character.body.valueEnums.FluidTypeBase;
import com.lilithsthrone.game.character.race.AbstractRace;
import com.lilithsthrone.game.dialogue.utils.UtilText;
import com.lilithsthrone.game.inventory.enchanting.ItemEffect;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.utils.Util;
import com.lilithsthrone.utils.XMLSaving;
import com.lilithsthrone.utils.colours.Colour;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @since 0.2.6
 * @version 0.3.8.2
 * @author Innoxia
 */
public class Fluid implements XMLSaving, BodyPartInterface {

	//GENERAL EXPLANATION:
	//Fluid is extended from AbstractFluidType. Its point being to create a customized template from a static one
	//It's essentially a more generic version of FluidCum/Milk/Girlcum
	//one of the bonus is that you don't have to put so much boilerplate code, no getType(), getNameSingular()...
	private AbstractFluidType type;
	protected FluidFlavour flavour;
	protected List<FluidModifier> fluidModifiers;
	protected List<ItemEffect> transformativeEffects;
	protected float transformativePower = 1.0f; //<1 gives a fraction of transformativeEffects, >1 gives x times the transformativeEffects
	protected BodyPartType source;

	public Fluid(Fluid fluid) {
		this.type = fluid.type;
		this.flavour = fluid.flavour;
		this.transformativeEffects = fluid.transformativeEffects;
		this.fluidModifiers = fluid.fluidModifiers;
		this.source = fluid.source;
	}

	public Fluid(AbstractFluidType type) {
		this.type = type;
		this.flavour = type.getBaseFlavour();
		this.transformativeEffects = new ArrayList<>();

		this.fluidModifiers = new ArrayList<>();
		this.fluidModifiers.addAll(type.getDefaultFluidModifiers());
	}

	public Element saveAsXML(Element parentElement, Document doc, String alias) {
		Element element = doc.createElement(alias);
		parentElement.appendChild(element);
		return saveAsXML(element, doc);
	}
	public Element saveAsXML(Element parentElement, Document doc) {
		Element element = doc.createElement("fluid");
		parentElement.appendChild(element);

		XMLUtil.addAttribute(doc, element, "type", com.lilithsthrone.game.character.body.types.FluidType.getIdFromFluidType(type));
		XMLUtil.addAttribute(doc, element, "flavour", this.flavour.toString());


		Element modifiers = doc.createElement("modifiers");
		element.appendChild(modifiers);
		for(FluidModifier fm : this.getFluidModifiers()) {
			XMLUtil.addAttribute(doc, modifiers, fm.toString(), "true");
		}

		Element effects = doc.createElement("effects");
		element.appendChild(effects);
		for(ItemEffect ie : this.getTransformativeEffects()) {
			ie.saveAsXML(effects, doc);
		}

		return element;
	}

	public static Fluid loadFromXML(Element parentElement, Document doc) {
		return loadFromXML(parentElement, doc, null, "");
	}


	/**
	 * @param parentElement
	 * @param doc
	 * @param baseType If it cannot find the fluidType, takes it from the related bodypart.
	 */
	public static Fluid loadFromXML(Element parentElement, Document doc, AbstractFluidType baseType, String alias) {
		Element parent = (Element)parentElement.getElementsByTagName(alias).item(0);
		Element fluidElement = (Element)parentElement.getElementsByTagName("fluid").item(0);
		if(parent != null){
			fluidElement = (Element)parent.getElementsByTagName("fluid").item(0);
			if(fluidElement == null){
				fluidElement = parent;
			}
		}

		AbstractFluidType fluidType = baseType;
		try {
			fluidType = com.lilithsthrone.game.character.body.types.FluidType.getFluidTypeFromId(fluidElement.getAttribute("type"));
		} catch(Exception ex) {
			if(fluidType == null){
				fluidType = com.lilithsthrone.game.character.body.types.FluidType.CUM_HUMAN;
			}
		}

		Fluid fluid = new Fluid(fluidType);

		switch (alias){
			case "milkCrotch": fluid.source = BodyPartType.MILK_CROTCH;
				break;
			case "milk": fluid.source = BodyPartType.MILK;
				break;
			case "girlcum": fluid.source = BodyPartType.GIRL_CUM;
				break;
			case "cum": fluid.source = BodyPartType.CUM;
		}

		if(alias == "milkCrotch") alias = "milk";

		String flavourId = fluidElement.getAttribute("flavour");
		if(flavourId.equalsIgnoreCase("SLIME")) {
			fluid.flavour = FluidFlavour.BUBBLEGUM;
		} else {
			fluid.flavour = FluidFlavour.valueOf(flavourId);
		}

		Element modifiers = (Element)fluidElement.getElementsByTagName("modifiers").item(0);
		if(modifiers == null){
			modifiers = (Element)fluidElement.getElementsByTagName(alias+"Modifiers").item(0);
		}
		fluid.fluidModifiers.clear();
		if(modifiers!=null) {
			List<FluidModifier> fluidModifiers = fluid.fluidModifiers;
			Body.handleLoadingOfModifiers(FluidModifier.values(), null, modifiers, fluidModifiers);
		}

		return fluid;
	}

	@Override
	public boolean equals(Object o){
		return (o instanceof Fluid
				&& type == ((Fluid) o).type
				&& ((Fluid) o).flavour == this.flavour
				&& ((Fluid) o).fluidModifiers.equals(this.fluidModifiers)
				&& ((Fluid) o).transformativeEffects.equals(this.getTransformativeEffects()));
	}

	@Override
	public int hashCode() {
		// Does not take into account quantity on purpose.
		int result = 17;
		result = 31 * result + flavour.hashCode();
		result = 31 * result + fluidModifiers.hashCode();
		result = 31 * result + transformativeEffects.hashCode();
		return result;
	}
	public FluidFlavour getFlavour() {
		return flavour;
	}

	public void setFlavour(FluidFlavour flavour) {
		this.flavour = flavour;
	}

	public AbstractBodyCoveringType getBaseFluidCoveringType() {
		return getBaseType().getCoveringType();
	}

	@Override
	public AbstractBodyCoveringType getBodyCoveringType(GameCharacter gc) {
		if(source != null){
			switch (source){
				case PENIS: return BodyCoveringType.CUM;
				case VAGINA: return BodyCoveringType.GIRL_CUM;
				case BREAST: return BodyCoveringType.MILK;
				case BREAST_CROTCH: return BodyCoveringType.MILK_CROTCH;
			}
		}
		return getBaseType().getCoveringType();
	}

	public Colour getColour() {
		return getBaseType().getColour();
	}

	public void setColour(GameCharacter gc, Colour colour) {
		gc.getCovering(this.getBodyCoveringType(gc)).setPrimaryColour(colour);
	}

	public boolean hasFluidModifier(FluidModifier fluidModifier) {
		return fluidModifiers.contains(fluidModifier);
	}

	public List<FluidModifier> getFluidModifiers() {
		return fluidModifiers;
	}

	public void setFluidModifiers(List<FluidModifier> fluidModifiers) {
		this.fluidModifiers = fluidModifiers;
	}

	public boolean addFluidModifier(FluidModifier modifier){
		return fluidModifiers.add(modifier);
	}

	public void clearFluidModifiers(){
		fluidModifiers.clear();
	}

	public List<ItemEffect> getTransformativeEffects() {
		return transformativeEffects;
	}

	public void setTransformativeEffects(List<ItemEffect> transformativeEffects) {
		this.transformativeEffects = transformativeEffects;
	}

	@Override
	public String getDescriptor(GameCharacter gc) {
		String modifierDescriptor = "";
		if(!fluidModifiers.isEmpty()) {
			modifierDescriptor = fluidModifiers.get(Util.random.nextInt(fluidModifiers.size())).getName();
		}

		if(type.getBaseType().equals(FluidTypeBase.CUM)){
			return UtilText.returnStringAtRandom(
					(gc.getAttributeValue(Attribute.VIRILITY)>=20?"potent":""),
					"hot",
					modifierDescriptor,
					flavour.getRandomFlavourDescriptor(),
					type.getDescriptor(gc));
		}
		return UtilText.returnStringAtRandom(
				modifierDescriptor,
				flavour.getRandomFlavourDescriptor(),
				type.getDescriptor(gc));
	}

	@Override
	public String getName(GameCharacter gc) {
		if(type.getName(gc) == "spooge" || type.getName(gc) == "jism" && Math.random()>0.4 || type.getName(gc) == "cream" && Math.random()>0.4){
			return getName(gc);
		}
		return type.getName(gc);
	}

	@Override
	public String getNameSingular(GameCharacter gc) {
		return type.getNameSingular(gc);
	}

	@Override
	public String getNamePlural(GameCharacter gc) {
		return type.getNamePlural(gc);
	}

	@Override
	public String getDeterminer(GameCharacter gc) {
		return type.getDeterminer(gc);
	}

	@Override
	public boolean isFeral(GameCharacter owner) {
		if(owner==null) {
			return false;
		}
		return owner.isFeral();
	}

	@Override
	public AbstractFluidType getType() {
		return type;
	}

	public void setType(AbstractFluidType type){
		this.type = type;
	}

	public FluidTypeBase getBaseType(){
		return type.getBaseType();
	}

    public AbstractRace getRace(){
        return type.getRace();
    }

    public float getValuePerMl(){
        if(type.getValuePerMlString() != null){
            try{
                if(type.isFromExternalFile() && Main.game.isStarted()) {
                    UtilText.setFluidForParsing("fluid", this);

                	return Float.valueOf(UtilText.parse(new ArrayList<>(),
							type.getValuePerMlString(),
							true,
							new ArrayList<>()).trim());
				}
            } catch(Exception ex) {}
		}
        return type.getBaseType().getBaseValuePerMl();
    }

    public boolean isCum(){
		return type.getBaseType().equals(FluidTypeBase.CUM);
	}

	public boolean isMilk(){
		return type.getBaseType().equals(FluidTypeBase.MILK);
	}

	public boolean isGirlcum(){
		return type.getBaseType().equals(FluidTypeBase.GIRLCUM);
	}

	public boolean isOther(){
		return type.getBaseType().equals(FluidTypeBase.OTHER);
	}
}
