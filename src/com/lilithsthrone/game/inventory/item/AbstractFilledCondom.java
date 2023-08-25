package com.lilithsthrone.game.inventory.item;

import com.lilithsthrone.controller.xmlParsing.XMLUtil;
import com.lilithsthrone.game.character.FluidStored;
import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.character.body.Fluid;
import com.lilithsthrone.game.character.body.types.FluidType;
import com.lilithsthrone.game.character.body.valueEnums.BodyMaterial;
import com.lilithsthrone.game.character.body.valueEnums.CumProduction;
import com.lilithsthrone.game.character.fetishes.Fetish;
import com.lilithsthrone.game.dialogue.utils.UtilText;
import com.lilithsthrone.game.inventory.AbstractFluidStorage;
import com.lilithsthrone.game.inventory.clothing.AbstractClothing;
import com.lilithsthrone.game.inventory.enchanting.ItemEffect;
import com.lilithsthrone.game.inventory.enchanting.ItemEffectType;
import com.lilithsthrone.game.inventory.enchanting.TFModifier;
import com.lilithsthrone.game.sex.CondomFailure;
import com.lilithsthrone.game.sex.SexAreaOrifice;
import com.lilithsthrone.utils.SvgUtil;
import com.lilithsthrone.utils.Util;
import com.lilithsthrone.utils.XMLSaving;
import com.lilithsthrone.utils.colours.Colour;
import com.lilithsthrone.utils.colours.PresetColour;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @since 0.1.86
 * @version 0.4.0
 * @author Innoxia
 */
public class AbstractFilledCondom extends AbstractItem implements XMLSaving, FluidContainerInterface {
	protected AbstractFluidStorage storedFluids;

	public AbstractFilledCondom(AbstractFilledCondom condom){
		super(condom.itemType);
		this.setColours(condom.getColours());
		storedFluids = new AbstractFluidStorage(condom.storedFluids);
		SVGString = getSVGString(this.itemType.getPathNameInformation().get(0).getPathName(), condom.getColour(0));
	}

	public AbstractFilledCondom(AbstractItemType itemType, Colour colour, List<FluidStored> fluids) {
		super(itemType);
		this.setColour(0, colour);
		storedFluids = new AbstractFluidStorage();
		storedFluids.addAll(fluids);
		SVGString = getSVGString(itemType.getPathNameInformation().get(0).getPathName(), colour);
	}

	@Override
	public boolean equals(Object o) {
		if(super.equals(o)) {
			return (o instanceof AbstractFilledCondom)
					&& ((AbstractFilledCondom)o).getStoredFluids().equals(storedFluids);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (storedFluids == null ?0:storedFluids.hashCode());
		return result;
	}

	@Override
	public Element saveAsXML(Element parentElement, Document doc) {
		Element element = doc.createElement("item");
		parentElement.appendChild(element);

		XMLUtil.addAttribute(doc, element, "id", this.getItemType().getId());
		XMLUtil.addAttribute(doc, element, "colour", this.getColour(0).getId());

		Element innerElement = doc.createElement("storedFluids");
		element.appendChild(innerElement);
		for(FluidStored fluid : this.getStoredFluids()) {
			fluid.saveAsXML(innerElement, doc);
		}

		return element;
	}

	public static AbstractFilledCondom loadFromXML(Element parentElement, Document doc) {
		String provider = parentElement.getAttribute("cumProvider");
		if(provider.isEmpty()) {
			provider = parentElement.getAttribute("cumProvidor"); // Support for old versions in which I could not spell
		}
		if(!provider.isEmpty()){ // if old save

			FluidStored fluid = new FluidStored(provider,
					((Element) parentElement.getElementsByTagName("cum").item(0)==null
							?new Fluid(FluidType.CUM_HUMAN)
							: Fluid.loadFromXML((Element) parentElement.getElementsByTagName("cum").item(0), doc, null, "cum")),
					(parentElement.getAttribute("millilitresStored").isEmpty()
							?25
							:Integer.valueOf(parentElement.getAttribute("millilitresStored"))));

			return new AbstractFilledCondom(
					ItemType.getIdToItemMap().get(parentElement.getAttribute("id")),
					PresetColour.getColourFromId(parentElement.getAttribute("colour")),
					Util.newArrayListOfValues(fluid));

		} else { // if new save
			List<FluidStored> fluids = new ArrayList<>();

			NodeList element = ((Element) parentElement.getElementsByTagName("storedFluids").item(0)).getElementsByTagName("fluidStored");
			for(int i = 0; i < element.getLength(); i++){
				Element e = ((Element)element.item(i));
				FluidStored fluid = FluidStored.loadFromXML(null, e, doc);
				if(fluid != null) {
					fluids.add(fluid);
				}
			}
			return new AbstractFilledCondom(
					ItemType.getIdToItemMap().get(parentElement.getAttribute("id")),
					PresetColour.getColourFromId(parentElement.getAttribute("colour")),
					fluids);
		}
	}

	private String getSVGString(String pathName, Colour colour) {
		if(itemType.equals(ItemType.CONDOM_USED_WEBBING)){
			try {
				InputStream is = this.getClass().getResourceAsStream("/com/lilithsthrone/res/items/"
						+ pathName + ".svg");
				String s = Util.inputStreamToString(is);

				s = SvgUtil.colourReplacement(String.valueOf(this.hashCode()), colour, s);

				is.close();

				return s;

			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		float volume = getMillilitresStored();
		int[] values = {25,50,100,250,500,1000,2500,5000};
		String variant = "New_" + (values.length + 1);
		for(int i = 0; i<values.length; i++){
			if (volume < values[i]) {
				variant = "New_" + (i + 1);
				break;
			}
		}
//
//		return Util.newArrayListOfValues(new SvgInformation(1,
//				ItemType.CONDOM_USED.SVGString + variant,
//				100, 0, new HashMap<>()));
		try {
			InputStream is = this.getClass().getResourceAsStream("/com/lilithsthrone/res/items/condoms/"
					+ pathName + variant + ".svg");
			String s = Util.inputStreamToString(is);

			s = SvgUtil.colourReplacement(String.valueOf(this.hashCode()), colour, s);

			is.close();

			return s;

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return "";
	}

	@Override
	public String applyEffect(GameCharacter user, GameCharacter target) {

		if(target.hasFetish(Fetish.FETISH_CUM_ADDICT)) {
			return UtilText.parse(target, user,
					"<p>"
						+ "[npc.Name] can't help but let out a delighted [npc.moan] as [npc.she] greedily [npc.verb(gulp)] down the slimy fluid."
						+ " Darting [npc.her] [npc.tongue] out, [npc.she] desperately [npc.verb(lick)] up every last drop of cum; only discarding the condom once [npc.sheIs] sure that's it's completely empty."
					+ "</p>"
					+ target.ingestFluid(SexAreaOrifice.MOUTH, storedFluids.getFluids()));

		} else {
			return UtilText.parse(target, user,
					"<p>"
						+ "[npc.Name] [npc.verb(scrunch)] [npc.her] [npc.eyes] shut as [npc.she] [npc.verb(gulp)] down the slimy fluid,"
						+ " trying [npc.her] best not to think about what [npc.sheHas] just done as "+(user.equals(target)?"[npc.she] [npc.verb(throw)]":"[npc2.name] [npc2.verb(throw)]")+" the now-empty condom to the floor..."
					+ "</p>"
					+ target.ingestFluid(SexAreaOrifice.MOUTH, storedFluids.getFluids()));
		}

	}

	public List<FluidStored> getStoredFluids() {
		return storedFluids.getFluids();
	}

	public float getMillilitresStored() {
		return storedFluids.getTotalFluidQuantity();
	}

	public float getMaxCapacity() {
		for(ItemEffect effect : getEffects()) {
			if(effect.getPrimaryModifier()== TFModifier.CLOTHING_CONDOM) {
				switch(effect.getPotency()) {
					case MINOR_BOOST:
						return CumProduction.FIVE_HUGE.getMaximumValue();//100
					case BOOST:
						return CumProduction.SIX_EXTREME.getMaximumValue();//1000
				}
			}
		}
		return Float.MAX_VALUE;
	}

	public float addFluid(FluidStored fluid) {
		return addFluid(fluid, fluid.getMillilitres());
	}

	public float addFluid(FluidStored fluid, float amount) {
		float added = storedFluids.addFluid(fluid, amount);
		SVGString = getSVGString(itemType.getPathNameInformation().get(0).getPathName(), getColour(0));
		return added;
	}

	public float removeFluid(FluidStored fluid, float amount) {
		float rest = storedFluids.removeFluid(fluid, amount);
		SVGString = getSVGString(itemType.getPathNameInformation().get(0).getPathName(), getColour(0));
		return rest;
	}

	public FluidStored getFluid(FluidStored fluid){
		return storedFluids.getFluid(fluid);
	}
}
