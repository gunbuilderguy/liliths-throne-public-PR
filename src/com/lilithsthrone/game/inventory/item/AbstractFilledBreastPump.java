package com.lilithsthrone.game.inventory.item;

import com.lilithsthrone.controller.xmlParsing.XMLUtil;
import com.lilithsthrone.game.character.FluidStored;
import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.character.body.Fluid;
import com.lilithsthrone.game.character.body.types.FluidType;
import com.lilithsthrone.game.character.body.valueEnums.FluidModifier;
import com.lilithsthrone.game.dialogue.utils.UtilText;
import com.lilithsthrone.game.inventory.AbstractFluidStorage;
import com.lilithsthrone.game.sex.SexAreaOrifice;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.utils.SvgUtil;
import com.lilithsthrone.utils.Util;
import com.lilithsthrone.utils.XMLSaving;
import com.lilithsthrone.utils.colours.Colour;
import com.lilithsthrone.utils.colours.PresetColour;

import javafx.scene.paint.Color;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @since 0.2.1
 * @version 0.4.0
 * @author Innoxia
 */
public class AbstractFilledBreastPump extends AbstractItem implements XMLSaving, FluidContainerInterface {
	protected AbstractFluidStorage storedFluids;

	public AbstractFilledBreastPump(AbstractFilledBreastPump pump){
		super(pump.itemType);
		this.setColours(pump.getColours());
		storedFluids = new AbstractFluidStorage(pump.storedFluids);
		SVGString = getSVGString(itemType.getPathNameInformation().get(0).getPathName(), pump.getColour(0));
	}
	public AbstractFilledBreastPump(AbstractItemType itemType, Colour colour, List<FluidStored> fluids) {
		super(itemType);
		this.setColour(0, colour);
		storedFluids = new AbstractFluidStorage();
		storedFluids.addAll(fluids);
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
							?new Fluid(FluidType.MILK_HUMAN)
							: Fluid.loadFromXML((Element) parentElement.getElementsByTagName("milk").item(0), doc, null, "milk")),
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
			InputStream is = this.getClass().getResourceAsStream("/com/lilithsthrone/res/items/condoms/"
					+ pathName + "Test.svg");
			String s = Util.inputStreamToString(is);

			is.close();

			//setting colors for base and fluid
			float millilitres = getMillilitresStored();
			/*
			double[] colArray = {0,0,0};
			for(FluidStored fluid : storedFluids){
				Color temp = fluid.getColour().getColor();
				System.out.println("millratio : " + fluid.getMillilitres()/millilitres + " for " + fluid.getColour().getName());
				colArray[0] += temp.getRed()*fluid.getMillilitres()/millilitres;
				colArray[1] += temp.getGreen()*fluid.getMillilitres()/millilitres;
				colArray[2] += temp.getBlue()*fluid.getMillilitres()/millilitres;
			}
			Color fluidColor = new Color(colArray[0],colArray[1],colArray[2], 1);
			s = SvgUtil.colourReplacement(String.valueOf(this.hashCode()), colour, new Colour(fluidColor), new Colour(fluidColor), s);
			//setting fluid level
			System.out.println("colour : " + colArray[0] + "," + colArray[1] + "," + colArray[2]);*/

			//double[][] colsArray = {};
			float offset = 0.44f;
			float offsetMult = 0.51f;
			float level = 1 - 0.001f*(Math.min(millilitres, getMaxCapacity()));
			float position = level;
			Colour c = null;
			Colour firstColour = null;
			boolean rainbow = false;
			String hashCode = String.valueOf(Math.abs(this.hashCode()+2)*13);

			//glowin stuff check
			//if any glow, unhide glow layer, already stretched horizontally +10%
			boolean glowing = !storedFluids.isEmpty() && storedFluids.getFluids().stream().anyMatch(fluidStored -> fluidStored.isGlowing());
			//stretch vertically +5% down
			boolean bottomglow = glowing && storedFluids.getFluids().get(0).isGlowing();
			//stretch vertically +5% up
			boolean topglow = glowing && storedFluids.getFluids().get(storedFluids.getFluids().size()-1).isGlowing();
			float alpha = 0;
			float fistAlpha = 0;
			if(glowing) s=s.replace("0.00512", "0.512");

			for(int i = 0; i<this.getStoredFluids().size(); i++){
				FluidStored fluid = this.getStoredFluids().get(i);
				float relativeQPrevious =  i==0?9999:(fluid.getMillilitres()/Math.max(0.01f, this.getStoredFluids().get(i-1).getMillilitres()));
				float relativeQNext = i==this.getStoredFluids().size()-1?9999:(fluid.getMillilitres()/Math.max(0.01f, this.getStoredFluids().get(i+1).getMillilitres()));
				String fluidColor = "";
				String extraColor = "";
				String glowColor = "";
				String glowWhite = "";
				alpha = fluid.isGlowing()?1:0;

				if(fluid.getColour().isRainbow()){
					rainbow = true;

					//position += fluid.getMillilitres()/(millilitres*(fluid.getColour().getRainbowColours().size()+1));
					//String extraColor = "<stop id=\"color\" style=\"stop-color:#ffb380;stop-opacity:1\" offset=\"" + position + "\" />";
					//extraColor = SvgUtil.colourReplacement(String.valueOf(this.hashCode()), colour, c, c, extraColor);
					//s=s.replace("id=\"bottomB\"\n         style=\"stop-color:#ffb380",
					//		SvgUtil.colourReplacement("id=\"bottomB\"\n         style=\"stop-color:#ffb380\"", colour, c, c, s));
//
//					position += fluid.getMillilitres()/(millilitres*(fluid.getColour().getRainbowColours().size()+1));
//					Colour c = new Colour(Color.valueOf(fluid.getColour().getRainbowColours().get(0)));
//					String extraColor = "<stop id=\"color\" style=\"stop-color:#ffb380;stop-opacity:1\" offset=\"" + position + "\" />";
//					extraColor = SvgUtil.colourReplacement(String.valueOf(this.hashCode()), colour, c, c, extraColor);
//					s=s.replace("<stop\n         id=\"start\"", extraColor + "<stop\n         id=\"start\"");

					for (String color : fluid.getColour().getRainbowColours()){

						c = new Colour(Color.valueOf(color));
						position += 0.5*(1-level)*fluid.getMillilitres()/(millilitres*(fluid.getColour().getRainbowColours().size()));
						String col = "<stop\n         "
								+ "id=\"color\"\n         "
								+ "style=\"stop-color:#ffb380;stop-opacity:1; filter: blur(5px)\"\n         "
								+ "offset=\"" + position + "\" />\n      ";
						extraColor += SvgUtil.colourReplacement(hashCode, colour, c, c, col);

						col = "<stop\n         "
								+ "id=\"color\"\n         "
								+ "style=\"stop-color:#ffb380;stop-opacity:1;\"\n         "
								+ "offset=\"" + (position*(1-offsetMult)+offset) + "\" />\n      ";
						fluidColor += SvgUtil.colourReplacement(hashCode, colour, c, c, col);

						col = "<stop\n         "
								+ "id=\"color\"\n         "
								+ "style=\"stop-color:#ffe6d5;stop-opacity:" + alpha + "\"\n         "
								+ "offset=\"" + (position*(1-offsetMult)+offset) + "\" />\n      ";
						glowColor += SvgUtil.colourReplacement(hashCode, colour, c, c, col);

						col = "<stop\n         "
								+ "id=\"color\"\n         "
								+ "style=\"stop-color:#ffffff;stop-opacity:" + alpha + "\"\n         "
								+ "offset=\"" + (position*(1-offsetMult)+offset) + "\" />\n      ";
						glowWhite += SvgUtil.colourReplacement(hashCode, colour, c, c, col);
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
							+ "offset=\"" + (position*(1-offsetMult)+offset) + "\" />\n      ";
					fluidColor += SvgUtil.colourReplacement(hashCode, c, col);

					col = "<stop\n         "
							+ "id=\"color\"\n         "
							+ "style=\"stop-color:#ffd5d5;stop-opacity:" + alpha + "\"\n         "
							+ "offset=\"" + (position*(1-offsetMult)+offset) + "\" />\n      ";
					glowColor += SvgUtil.colourReplacement(hashCode, c, col);

					col = "<stop\n         "
							+ "id=\"color\"\n         "
							+ "style=\"stop-color:#ffffff;stop-opacity:" + alpha + "\"\n         "
							+ "offset=\"" + (position*(1-offsetMult)+offset) + "\" />\n      ";
					glowWhite += SvgUtil.colourReplacement(hashCode, c, col);

					position += mid*(1-level)*fluid.getMillilitres()/millilitres;

					col = "<stop\n         "
							+ "id=\"color\"\n         "
							+ "style=\"stop-color:#ff8080;stop-opacity:1\"\n         "
							+ "offset=\"" + (position*(1-offsetMult)+offset) + "\" />\n      ";
					fluidColor += SvgUtil.colourReplacement(hashCode, c, col);

					col = "<stop\n         "
							+ "id=\"color\"\n         "
							+ "style=\"stop-color:#ffd5d5;stop-opacity:" + alpha + "\"\n         "
							+ "offset=\"" + (position*(1-offsetMult)+offset) + "\" />\n      ";
					glowColor += SvgUtil.colourReplacement(hashCode, c, col);

					col = "<stop\n         "
							+ "id=\"color\"\n         "
							+ "style=\"stop-color:#ffffff;stop-opacity:" + alpha + "\"\n         "
							+ "offset=\"" + (position*(1-offsetMult)+offset) + "\" />\n      ";
					glowWhite += SvgUtil.colourReplacement(hashCode, c, col);
					position += boundL*(1-level)*fluid.getMillilitres()/millilitres;

				}
				s=s.replace("<stop\n         id=\"start\"", extraColor + "<stop\n         id=\"start\"");
				s=s.replace("<stop\n         id=\"fluidStart\"", fluidColor + "<stop\n         id=\"fluidStart\"");
				s=s.replace("<stop\n         id=\"glowStart\"", glowColor + "<stop\n         id=\"glowStart\"");
				s=s.replace("<stop\n         id=\"glowWhiteStart\"", glowWhite + "<stop\n         id=\"glowWhiteStart\"");
				//colsArray[colsArray.length-1] = colorPos;
				if(c != null && firstColour == null){
					firstColour = c;
					fistAlpha = alpha;
				}
			}

			//fluid endcap
			String startColor = "<stop\n         "
					+ "id=\"air\"\n         "
					+ "style=\"stop-color:#ffb380;stop-opacity:0\"\n         "
					+ "offset=\"" + ((level*(1-offsetMult)+offset)-0.01) + "\" />\n      "
					+ "<stop\n         "
					+ "id=\"end\"\n         "
					+ "style=\"stop-color:#ffb380;stop-opacity:1\"\n         "
					+ "offset=\"" + (level*(1-offsetMult)+offset) + "\" />\n      ";
			startColor = SvgUtil.colourReplacement(hashCode, colour, firstColour, firstColour, startColor);
			s=s.replace("linearGradientFluid\">\n      ",
					"linearGradientFluid\">\n      " + startColor);

			//glow endcap
			String startGlow = "<stop\n         "
					+ "id=\"air\"\n         "
					+ "style=\"stop-color:#ffe6d5;stop-opacity:0\"\n         "
					+ "offset=\"" + ((level*(1-offsetMult)+offset)-0.01) + "\" />\n      "
					+ "<stop\n         "
					+ "id=\"end\"\n         "
					+ "style=\"stop-color:#ffe6d5;stop-opacity:" + fistAlpha + "\"\n         "
					+ "offset=\"" + (level*(1-offsetMult)+offset) + "\" />\n      ";
			startGlow = SvgUtil.colourReplacement(hashCode, colour, firstColour, firstColour, startGlow);
			s=s.replace("linearGradientGlow\">\n      ",
					"linearGradientGlow\">\n      " + startGlow);

			s = s.replace("id=\"glowStart\"\n"
							+ "         style=\"stop-color:#ffe6d5;stop-opacity:1\"",
					SvgUtil.colourReplacement(hashCode, colour, firstColour, firstColour,
							"id=\"glowStart\"\n"
							+ "         style=\"stop-color:#ffe6d5;stop-opacity:" + alpha + "\""));

			//glowWhite endcap
			String startGlowWhite = "<stop\n         "
					+ "id=\"air\"\n         "
					+ "style=\"stop-color:#ffffff;stop-opacity:0\"\n         "
					+ "offset=\"" + ((level*(1-offsetMult)+offset)-0.01) + "\" />\n      "
					+ "<stop\n         "
					+ "id=\"end\"\n         "
					+ "style=\"stop-color:#ffffff;stop-opacity:" + fistAlpha + "\"\n         "
					+ "offset=\"" + (level*(1-offsetMult)+offset) + "\" />\n      ";
			startGlowWhite = SvgUtil.colourReplacement(hashCode, colour, firstColour, firstColour, startGlowWhite);
			s=s.replace("linearGradientGlowWhite\">\n      ",
					"linearGradientGlowWhite\">\n      " + startGlowWhite);

			s = s.replace("id=\"glowWhiteStart\"\n"
							+ "         style=\"stop-color:#ffffff;stop-opacity:1\"",
						"id=\"glowWhiteStart\"\n"
								+ "         style=\"stop-color:#ffffff;stop-opacity:" + alpha + "\"");


			//linking IDs to its hashcode
			s=s.replaceAll("(id=\"\\w*)(\")", "$1" + hashCode + "$2");
			s=s.replaceAll("(url\\(#\\w*)(\\))", "$1" + hashCode + "$2");
			s=s.replaceAll("(xlink:href=\"#\\w*)(\")", "$1" + hashCode + "$2");

			s = SvgUtil.colourReplacement(hashCode, colour, c, c, s);

			//float level = 1 - 0.52f * 0.001f*(Math.min(millilitres, 1000));
//			s=s.replace("offset=\"0.0\"", "offset=\"" + (level) + "\"");
//			s=s.replace("offset=\"0.0017\"", "offset=\"" + (level + 0.0017)+ "\"");
			//setting overflow look
			if(getMillilitresStored()<975){
				s=s.replace("opacity:0.996", "opacity:0.000");
			}

			return s;

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return "";
	}

	@Override
	public String applyEffect(GameCharacter user, GameCharacter target) {
		return UtilText.parse(target, user,
				target.ingestFluid(SexAreaOrifice.MOUTH, storedFluids.getFluids())
				+ target.addItem(Main.game.getItemGen().generateItem(ItemType.MOO_MILKER_EMPTY), false));
	}


	public List<FluidStored> getStoredFluids() {
		return storedFluids.getFluids();
	}

	public float getMillilitresStored() {
		return storedFluids.getTotalFluidQuantity();
	}

	public float getMaxCapacity() {
		return 1000.0f;
	}

	public float addFluid(FluidStored fluid) {
		return addFluid(fluid, fluid.getMillilitres());
	}

	/**
	 * @return the quantity of fluid that was unsuccessfully inserted, 0 if enough space was left
	 */
	public float addFluid(FluidStored fluid, float amount) {
		amount = storedFluids.addFluid(fluid, amount);
		SVGString = getSVGString(itemType.getPathNameInformation().get(0).getPathName(), getColour(0));
		return amount;
	}

	/**
	 * @return the quantity of fluid that was unsuccessfully removed, 0 if enough fluid was present
	 */
	public float removeFluid(FluidStored fluid, float amount) {
		float rest = storedFluids.removeFluid(fluid, amount);
		SVGString = getSVGString(itemType.getPathNameInformation().get(0).getPathName(), getColour(0));
		return rest;
	}

	public FluidStored getFluid(FluidStored fluid){
		return storedFluids.getFluid(fluid);
	}
	
}
