package com.lilithsthrone.game.character.body;

import java.util.ArrayList;
import java.util.List;

import com.lilithsthrone.controller.xmlParsing.XMLUtil;
import com.lilithsthrone.game.character.FluidStored;
import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.character.attributes.Attribute;
import com.lilithsthrone.game.character.body.abstractTypes.AbstractFluidType;
import com.lilithsthrone.game.character.body.coverings.AbstractBodyCoveringType;
import com.lilithsthrone.game.character.body.types.FluidType;
import com.lilithsthrone.game.character.body.valueEnums.FluidFlavour;
import com.lilithsthrone.game.character.body.valueEnums.FluidModifier;
import com.lilithsthrone.game.character.body.valueEnums.FluidTypeBase;
import com.lilithsthrone.game.character.race.AbstractRace;
import com.lilithsthrone.game.dialogue.utils.UtilText;
import com.lilithsthrone.game.inventory.enchanting.ItemEffect;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.utils.Util;
import com.lilithsthrone.utils.XMLSaving;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @since 0.2.6
 * @version 0.3.8.2
 * @author Innoxia
 */
public class AbstractFluid extends AbstractFluidType implements XMLSaving {

	//GENERAL EXPLANATION:
	//AbstractFluid is extended from AbstractFluidType. Its point being to create a customized template from a static one
	//It's essentially a more generic version of FluidCum/Milk/Girlcum
	//one of the bonus is that you don't have to put so much boilerplate code, no getType(), getNameSingular()...

	protected FluidFlavour flavour;
	protected List<FluidModifier> fluidModifiers;
	protected List<ItemEffect> transformativeEffects;

	public AbstractFluid(AbstractFluid fluid) {
		super(fluid);
		this.flavour = fluid.flavour;
		transformativeEffects = fluid.transformativeEffects;
		fluidModifiers = fluid.fluidModifiers;
	}

	public AbstractFluid(AbstractFluidType type) {
		super(type);
		this.flavour = type.getBaseFlavour();
		transformativeEffects = new ArrayList<>();

		fluidModifiers = new ArrayList<>();
		fluidModifiers.addAll(type.getDefaultFluidModifiers());
	}

	public Element saveAsXML(Element parentElement, Document doc) {
		Element element = doc.createElement("fluid");
		parentElement.appendChild(element);

		XMLUtil.addAttribute(doc, element, "type", FluidType.getIdFromFluidType(this));
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

	public static AbstractFluid loadFromXML(Element parentElement, Document doc) {
		return loadFromXML(parentElement, doc, null, "");
	}


	/**
	 * @param parentElement
	 * @param doc
	 * @param baseType If it cannot find the fluidType, takes it from the related bodypart.
	 */
	public static AbstractFluid loadFromXML(Element parentElement, Document doc, AbstractFluidType baseType, String alias) {
		Element parent = (Element)parentElement.getElementsByTagName(alias).item(0);
		Element fluidElement = (Element)parentElement.getElementsByTagName("alias").item(0);
		if(parent != null){
			parentElement = parent;
			fluidElement = (Element)parentElement.getElementsByTagName("fluid").item(0);
		}

		AbstractFluidType fluidType = baseType;
		try {
			fluidType = FluidType.getFluidTypeFromId(fluidElement.getAttribute("type"));
		} catch(Exception ex) {
			if(fluidType == null){
				fluidType = FluidType.CUM_HUMAN;
			}
		}

		AbstractFluid fluid = new AbstractFluid(fluidType);

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
		return (o instanceof AbstractFluid
				&& super.equals(o)
				&& ((AbstractFluid) o).flavour == this.flavour
				&& ((AbstractFluid) o).fluidModifiers.equals(this.fluidModifiers)
				&& ((AbstractFluid) o).transformativeEffects.equals(this.getTransformativeEffects()));
	}
	public FluidFlavour getFlavour() {
		return flavour;
	}

	public void setFlavour(FluidFlavour flavour) {
		this.flavour = flavour;
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

		if(getBaseType().equals(FluidTypeBase.CUM)){
			return UtilText.returnStringAtRandom(
					(gc.getAttributeValue(Attribute.VIRILITY)>=20?"potent":""),
					"hot",
					modifierDescriptor,
					flavour.getRandomFlavourDescriptor(),
					super.getDescriptor(gc));
		}
		return UtilText.returnStringAtRandom(
				modifierDescriptor,
				flavour.getRandomFlavourDescriptor(),
				super.getDescriptor(gc));
	}

	public float getValuePerMl(){
		if(getValuePerMlString() != null){
			try{
				if(this.isFromExternalFile() && Main.game.isStarted()) {
					UtilText.setFluidForParsing("fluid", this);

					return Float.valueOf(UtilText.parse(getValuePerMlString()).trim());
				}
			} catch(Exception ex) {}
		}
		return getBaseType().getBaseValuePerMl();
	}

	public boolean isCum(){
		return getBaseType().equals(FluidTypeBase.CUM);
	}

	public boolean isMilk(){
		return getBaseType().equals(FluidTypeBase.MILK);
	}

	public boolean isGirlcum(){
		return getBaseType().equals(FluidTypeBase.GIRLCUM);
	}

	public boolean isOther(){
		return getBaseType().equals(FluidTypeBase.OTHER);
	}
}
