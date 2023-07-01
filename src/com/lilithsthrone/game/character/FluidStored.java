package com.lilithsthrone.game.character;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.lilithsthrone.controller.xmlParsing.XMLUtil;
import com.lilithsthrone.game.character.attributes.Attribute;
import com.lilithsthrone.game.character.body.AbstractFluid;
import com.lilithsthrone.game.character.race.AbstractSubspecies;
import com.lilithsthrone.game.character.race.Subspecies;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.utils.XMLSaving;

/**
 * @since 0.2.7
 * @version 0.3.1
 * @author Innoxia
 */
public class FluidStored extends AbstractFluid implements XMLSaving {

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
	//FluidStored is extended from AbstractFluid. In many ways, FluidStored is extruded from it, from a customized production
	//template to a finalized liquid that can be interacted with, having a quantity and geneData which is detached from
	//the person itself to correctly reflect the npc's state then compared to now which may have gone through significant changes.

	private float millilitres;
	private GeneData geneData;
	private String charactersFluidID;

	public FluidStored(FluidStored fluid){
		super(fluid);
		this.millilitres = fluid.millilitres;
		this.charactersFluidID = fluid.charactersFluidID;
		this.geneData = fluid.geneData;
	}

	public FluidStored(GameCharacter character, AbstractFluid fluid, float millilitres) {
		super(fluid);

		if(character!=null) {
			geneData = new GeneData(character.getSubspecies(),
					character.getHalfDemonSubspecies(),
					character.getAttributeValue(Attribute.VIRILITY),
					character.isFeral());
		} else {
			geneData = new GeneData(null,
					null,
					25,
					false);
		}

		if(character!=null){
			charactersFluidID = character.getId();
		} else {
			charactersFluidID = "";
		}

		this.millilitres = millilitres;
	}

	public FluidStored(String charactersFluidID, AbstractFluid fluid, float millilitres) {
		super(fluid);

		GameCharacter owner = null;
		try{
			owner = Main.game.getNPCById(charactersFluidID);
		} catch (Exception ex){};

		if(owner!=null) {
			geneData = new GeneData(owner.getSubspecies(),
					owner.getHalfDemonSubspecies(),
					owner.getAttributeValue(Attribute.VIRILITY),
					owner.isFeral());
		} else {
			geneData = new GeneData(null,
					null,
					25,
					false);
		}

		if(owner!=null){
			this.charactersFluidID = charactersFluidID;
		} else {
			this.charactersFluidID = "";
		}

		this.millilitres = millilitres;
	}

	public FluidStored(String charactersFluidID, AbstractSubspecies cumSubspecies, AbstractSubspecies cumHalfDemonSubspecies, AbstractFluid fluid, float millilitres) {
		super(fluid);

		GameCharacter owner = null;
		try{
			owner = Main.game.getNPCById(charactersFluidID);
		} catch (Exception ex){};

		if(owner!=null) {
			geneData = new GeneData(owner.getSubspecies(),
					owner.getHalfDemonSubspecies(),
					owner.getAttributeValue(Attribute.VIRILITY),
					owner.isFeral());
		} else {
			geneData = new GeneData(null,
					null,
					25,
					false);
		}

		if(owner!=null){
			this.charactersFluidID = charactersFluidID;
		} else {
			this.charactersFluidID = "";
		}

		this.millilitres = millilitres;
	}

	public FluidStored(String charactersFluidID,
					   AbstractFluid fluid,
					   AbstractSubspecies Subspecies,
					   AbstractSubspecies HalfDemonSubspecies,
					   boolean feral,
					   float virility,
					   float millilitres) {

		super(fluid);
		this.geneData = new GeneData(Subspecies,
				HalfDemonSubspecies,
				virility,
				feral);
		this.millilitres = millilitres;
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof FluidStored){
			if(super.equals(o)
					&& ((FluidStored)o).getCharactersFluidID().equals(this.getCharactersFluidID())
					&& ((FluidStored)o).isFeral() == this.isFeral()
					&& ((FluidStored)o).getSubspecies()==this.getSubspecies()
					&& ((FluidStored)o).getHalfDemonSubspecies()==this.getHalfDemonSubspecies()
					&& ((FluidStored)o).getVirility() == this.getVirility()
					&& ((FluidStored)o).getMillilitres() == this.getMillilitres()) {
				return true;
			}
		}
		return false;
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
		XMLUtil.addAttribute(doc, fluidStoredElement, "Subspecies", String.valueOf(geneData.Subspecies));
		XMLUtil.addAttribute(doc, fluidStoredElement, "HalfDemonSubspecies", String.valueOf(geneData.HalfDemonSubspecies));
		XMLUtil.addAttribute(doc, fluidStoredElement, "millilitres", String.valueOf(millilitres));


		super.saveAsXML(fluidStoredElement, doc);

		return fluidStoredElement;
	}

	public static FluidStored loadFromXML(StringBuilder log, Element parentElement, Document doc) {
		String ID = parentElement.getAttribute("charactersFluidID");

		float millilitres = Float.parseFloat(parentElement.getAttribute("millilitres"));

		boolean feral = false;
		float virility = 25;
		virility = Float.parseFloat(parentElement.getAttribute("virility"));

		if(parentElement.hasAttribute("bestial")){ //legacy code
			String typeName = parentElement.getChildNodes().item(0).getNodeName();

			return new FluidStored(ID,
					AbstractFluid.loadFromXML(parentElement, doc, null, typeName),
					Subspecies.getSubspeciesFromId(parentElement.getAttribute("cumSubspecies")),
					Subspecies.getSubspeciesFromId(parentElement.getAttribute("cumHalfDemonSubspecies")),
					Boolean.parseBoolean(parentElement.getAttribute("bestial")),
					virility,
					millilitres);
		}

		//new code:
		//-bestial hammered off
		//-removed cum from saving structure, since it doesn't have to be unique to it anymore
		//+loads the fluidTemplate it's based on instead of whatever the old thing was
		return new FluidStored(ID,
				AbstractFluid.loadFromXML(parentElement, doc, null, ""),
				Subspecies.getSubspeciesFromId(parentElement.getAttribute("Subspecies")),
				Subspecies.getSubspeciesFromId(parentElement.getAttribute("HalfDemonSubspecies")),
				Boolean.parseBoolean(parentElement.getAttribute("feral")),
				virility,
				millilitres);
	}


	public String getCharactersFluidID() {
		return charactersFluidID;
	}

	/**
	 * @return The character whose fluid this is.
	 * @throws Exception A NullPointerException if the character does not exist.
	 */
	public GameCharacter getFluidCharacter() throws Exception {
		if(charactersFluidID.equals(Main.game.getPlayer().getId())) {
			return Main.game.getPlayer();
		}
		return Main.game.getNPCById(charactersFluidID);
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
