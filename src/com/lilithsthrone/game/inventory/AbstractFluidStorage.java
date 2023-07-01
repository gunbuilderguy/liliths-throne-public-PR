package com.lilithsthrone.game.inventory;

import com.lilithsthrone.game.character.FluidStored;
import com.lilithsthrone.game.character.body.AbstractFluid;

import java.util.*;

/**
 * A general storage class
 * merging fluids sums their quantity and does a weighed average of their virility
 */
public class AbstractFluidStorage {
	class FluidWrapper extends FluidStored {
		FluidStored fluid;

		public FluidWrapper(FluidStored fluid) {
			super(fluid);
			this.fluid = fluid;
		}
		@Override
		public boolean equals(Object o){
			return (o instanceof FluidStored
					&& super.equals(o)
					&& ((FluidStored) o).isFeral() == this.isFeral()
					&& ((FluidStored) o).getSubspecies() == this.getSubspecies()
					&& ((FluidStored) o).getHalfDemonSubspecies() == this.getHalfDemonSubspecies()
					&& ((FluidStored) o).getCharactersFluidID() == this.getCharactersFluidID());
		}
	}
	private List<FluidWrapper> duplicateCounts;

	public AbstractFluidStorage() {
		duplicateCounts = new ArrayList<>();
	}

	AbstractFluidStorage(AbstractFluidStorage inventoryToCopy) {
		duplicateCounts = new ArrayList<>(inventoryToCopy.duplicateCounts);
	}
	
	public void clear() {
		duplicateCounts.clear();
	}

	public boolean isEmpty() {
		return duplicateCounts.isEmpty();
	}


	/**
	 * @return a Non-modifiable Map
	 */
	public List<FluidStored> getFluids() {
		return Collections.unmodifiableList(duplicateCounts);
	}

	public int getUniqueFluidCount(){
		return duplicateCounts.size();
	}

	public float getTotalFluidQuantity() {
		return (float) duplicateCounts.stream().mapToDouble(e -> e.getMillilitres()).sum();
	}

	public float getFluidQuantity(FluidStored fluid) {
		return hasFluid(fluid)?duplicateCounts.get(duplicateCounts.indexOf(fluid)).getMillilitres():0;
	}

	public void addAll(List<FluidStored> fluids) {
		fluids.forEach(this::addFluid);
	}

	public boolean addFluid(FluidStored fluid) {

		if(fluid.getMillilitres()<0){
			return removeFluid(fluid, -fluid.getMillilitres());
		}

		boolean hasFluid = hasFluid(fluid);
		if (hasFluid) {
			FluidWrapper entry = duplicateCounts.get(duplicateCounts.indexOf(new FluidWrapper(fluid)));
			entry.setMillilitres(entry.getMillilitres() + fluid.getMillilitres());
			entry.setVirility((entry.getVirility() * entry.getMillilitres() + fluid.getVirility() * fluid.getMillilitres())
				/ (entry.getMillilitres() + fluid.getMillilitres()));
		} else {
			duplicateCounts.add(new FluidWrapper(fluid));
		}
		return hasFluid;
	}

	public boolean hasFluid(FluidStored fluid) {
		return duplicateCounts.contains(fluid);
	}

	/**
	 * Removes the FluidStored entry regardless of quantity
	 * @param fluid
	 * @return
	 */
	public boolean removeFluid(FluidStored fluid) {
		return duplicateCounts.remove(fluid);
	}

	public boolean removeFluid(FluidStored fluid, float quantity) {
		boolean hasFluid = hasFluid(fluid);
		if (hasFluid) {
			FluidStored entry = duplicateCounts.get(duplicateCounts.indexOf(fluid));
			entry.setMillilitres(entry.getMillilitres()-quantity);
			if(entry.getMillilitres()<0){
				duplicateCounts.remove(fluid);
			}
		}
		return hasFluid;
	}
}
