package com.lilithsthrone.game.inventory.item;

import com.lilithsthrone.controller.xmlParsing.XMLUtil;
import com.lilithsthrone.game.character.FluidStored;
import com.lilithsthrone.game.character.body.AbstractFluid;
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
public class AbstractFluidContainerItem extends AbstractItem implements XMLSaving {
	protected List<FluidStored> storedFluids;

	public AbstractFluidContainerItem(AbstractItemType itemType, Colour colour, List<FluidStored> fluids) {
		super(itemType);
		this.storedFluids = fluids;
		this.setColour(0, colour);
		SVGString = getSVGString(itemType.getPathNameInformation().get(0).getPathName(), colour);
	}
	
	@Override
	public boolean equals(Object o) {
		if(super.equals(o)) {
			return (o instanceof AbstractFluidContainerItem)
					&& ((AbstractFluidContainerItem)o).getStoredFluids().equals(storedFluids);
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

	public static AbstractFluidContainerItem loadFromXML(Element parentElement, Document doc) {

		List<FluidStored> fluids = new ArrayList<>();

		NodeList element = ((Element) parentElement.getElementsByTagName("storedFluids").item(0)).getElementsByTagName("fluidStored");
		for(int i = 0; i < element.getLength(); i++){
			Element e = ((Element)element.item(i));
			FluidStored fluid = FluidStored.loadFromXML(null, e, doc);
			if(fluid != null) {
				fluids.add(fluid);
			}
		}
		return new AbstractFluidContainerItem(
				ItemType.getIdToItemMap().get(parentElement.getAttribute("id")),
				PresetColour.getColourFromId(parentElement.getAttribute("colour")),
				fluids);
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

	public List<FluidStored> getStoredFluids() {
		return storedFluids;
	}

	public float getMillilitresStored() {
		return (float) storedFluids.stream().mapToDouble(FluidStored::getMillilitres).sum();
	}

	public void addFluid(FluidStored fluid) {
		Optional<FluidStored> foundFluid = this.storedFluids.stream()
				.filter(findFluid -> findFluid.getCharactersFluidID() == fluid.getCharactersFluidID()
						&& ((AbstractFluid)fluid).equals(findFluid)
						&& findFluid.getSubspecies() == fluid.getSubspecies()
						&& findFluid.getHalfDemonSubspecies() == fluid.getHalfDemonSubspecies())
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
