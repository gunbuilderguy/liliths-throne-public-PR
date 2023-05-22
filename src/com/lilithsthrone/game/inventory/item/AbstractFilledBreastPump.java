package com.lilithsthrone.game.inventory.item;

import com.lilithsthrone.controller.xmlParsing.XMLUtil;
import com.lilithsthrone.game.character.FluidStored;
import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.character.body.FluidMilk;
import com.lilithsthrone.game.character.body.types.FluidType;
import com.lilithsthrone.game.character.body.valueEnums.FluidTypeBase;
import com.lilithsthrone.game.dialogue.utils.UtilText;
import com.lilithsthrone.game.sex.SexAreaOrifice;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.utils.SvgUtil;
import com.lilithsthrone.utils.Units;
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
 * @since 0.2.1
 * @version 0.4.0
 * @author Innoxia
 */
public class AbstractFilledBreastPump extends AbstractFluidContainerItem implements XMLSaving {
	//COMPLETE OVERHAUL, replace everyhting with FluidStored
	private List<FluidStored> storedFluids;

	public AbstractFilledBreastPump(AbstractItemType itemType, Colour colour, List<FluidStored> fluids) {
		super(itemType, colour, fluids);
		this.storedFluids = fluids;
		this.setColour(0, colour);
		SVGString = getSVGString(itemType.getPathNameInformation().get(0).getPathName(), colour);
	}

	@Override
	public boolean equals(Object o) {
		if(super.equals(o)) {
			return (o instanceof AbstractFilledBreastPump)
					&& ((AbstractFilledBreastPump)o).getStoredFluids().equals(storedFluids);
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

	public static AbstractFilledBreastPump loadFromXML(Element parentElement, Document doc) {
		String provider = parentElement.getAttribute("milkProvider");
		if(provider.isEmpty()) {
			provider = parentElement.getAttribute("milkProvidor"); // Support for old versions in which I could not spell
		}
		if(!provider.isEmpty()){ // if old save

			FluidStored fluid = new FluidStored(provider,
					((Element) parentElement.getElementsByTagName("milk").item(0)==null
							?new FluidMilk(FluidType.MILK_HUMAN, false)
							:FluidMilk.loadFromXML("milk", (Element) parentElement.getElementsByTagName("milk").item(0), doc)),
					(parentElement.getAttribute("millilitresStored").isEmpty()
							?25
							:Integer.valueOf(parentElement.getAttribute("millilitresStored"))));

			return new AbstractFilledBreastPump(
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
			return new AbstractFilledBreastPump(
					ItemType.getIdToItemMap().get(parentElement.getAttribute("id")),
					PresetColour.getColourFromId(parentElement.getAttribute("colour")),
					fluids);
		}
	}

	private String getSVGString(String pathName, Colour colour) {
		try {
			InputStream is = this.getClass().getResourceAsStream("/com/lilithsthrone/res/items/" + pathName + ".svg");
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
		if(storedFluids.size()>1){
			float highestChance = 0;
			boolean hasCum = false;
			List<String> ingestString = new ArrayList<>();
			String lastPregText = "";
			for(FluidStored fluid : storedFluids){
				if(fluid.getFluid().getType().getBaseType() == FluidTypeBase.CUM) hasCum = true;
				String str = "";

				if(ingestString.isEmpty()){
					try{
						GameCharacter character = fluid.getFluidCharacter();
						str = UtilText.parse(target, character,
								"<span style='padding:0; margin:0; text-align:center; color:"+fluid.getFluid().getType().getBaseType().getColour().toWebHexString()
								+";'><i>[npc.Name] [npc.verb(swallow)] down "+Units.fluid(fluid.getMillilitres())
								+" of [npc2.namePos] "+fluid.getFluid().getName(character) + "</i></span>");
						if(hasCum)lastPregText = target.ingestFluid(fluid, SexAreaOrifice.MOUTH);
					} catch (Exception ex) {}
				} else {
					try{
						GameCharacter character = fluid.getFluidCharacter();
						str = UtilText.parse(target,
								character,
								"<span style='padding:0; margin:0; text-align:center; color:"+fluid.getFluid().getType().getBaseType().getColour().toWebHexString()
										+";'><i>" + Units.fluid(fluid.getMillilitres())+" of [npc2.namePos] "+fluid.getFluid().getName(character) + "</i></span>");
						if(hasCum)lastPregText = target.ingestFluid(fluid, SexAreaOrifice.MOUTH);
					} catch (Exception ex) {}}
				ingestString.add(str);
			}
			ingestString.set(ingestString.size()-1, ingestString.get(ingestString.size()-1) + "</p>");
			ingestString.set(ingestString.size()-1, ingestString.get(ingestString.size()-1) + "</i>");

			target.addItem(Main.game.getItemGen().generateItem(ItemType.MOO_MILKER_EMPTY), false);
			lastPregText = "<b" + lastPregText.split("<b")[1];
			return Util.stringsToStringList(ingestString, false) + lastPregText;
		}
		return target.ingestFluid(storedFluids.get(0), SexAreaOrifice.MOUTH)
				+ target.addItem(Main.game.getItemGen().generateItem(ItemType.MOO_MILKER_EMPTY), false);
	}

	public List<FluidStored> getStoredFluids() {
		return storedFluids;
	}

	public float getMillilitresStored() {
		return (float) storedFluids.stream().mapToDouble(FluidStored::getMillilitres).sum();
	}

	public void addFluid(FluidStored fluid) {
		Optional<FluidStored> foundFluid = this.storedFluids.stream()
				.filter(findFluid -> findFluid.getCharactersFluidID() == fluid.getCharactersFluidID()
						&& findFluid.getFluid().equals(fluid.getFluid())
						&& findFluid.getCumSubspecies() == fluid.getCumSubspecies()
						&& findFluid.getCumHalfDemonSubspecies() == fluid.getCumHalfDemonSubspecies())
				.findFirst();
		if(foundFluid.isPresent()){
			//averaging both virility values with fluid quantities e.g: F1 = 500ml v:50, F2 = 1200ml v:90 -> F3 = 1700ml v:78.235
			foundFluid.get().setVirility(
					foundFluid.get().getVirility() * (foundFluid.get().getMillilitres()/(foundFluid.get().getMillilitres() + fluid.getMillilitres()))
					+ fluid.getVirility() * (fluid.getMillilitres()/(foundFluid.get().getMillilitres() + fluid.getMillilitres())));
			foundFluid.get().setMillilitres(foundFluid.get().getMillilitres() + fluid.getMillilitres());

		}
		this.storedFluids.add(fluid);
	}
	
}
