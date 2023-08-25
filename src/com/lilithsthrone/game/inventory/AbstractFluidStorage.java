package com.lilithsthrone.game.inventory;

import com.lilithsthrone.game.character.FluidStored;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A general storage class
 * merging fluids sums their quantity and does a weighed average of their virility
 */
public class AbstractFluidStorage {
	class FluidWrapper {
		FluidStored fluid;

		public FluidWrapper(FluidStored fluid) {
			this.fluid = new FluidStored(fluid);
		}
		@Override
		public boolean equals(Object o){
			if(o instanceof FluidWrapper){
				return fluid.equals(((FluidWrapper) o).fluid, false);
			}
			return false;
		}

		@Override
		public int hashCode(){
			return 13 + 31*fluid.hashCode();
		}
	}
	private List<FluidWrapper> duplicateCounts;
	private float talliedQuantity = 0f; //stored just to make getTotalFluidQuantity's total sum O(1) instead of O(n)
	float capacity = Float.POSITIVE_INFINITY;

	public AbstractFluidStorage() {
		duplicateCounts = new ArrayList<>();
	}

	public AbstractFluidStorage(AbstractFluidStorage inventoryToCopy) {
		duplicateCounts = new ArrayList<>(inventoryToCopy.duplicateCounts.stream().map(fw -> new FluidWrapper(fw.fluid)).collect(Collectors.toList()));
		talliedQuantity = inventoryToCopy.getTotalFluidQuantity();
	}
	
	public void clear() {
		duplicateCounts.clear();
		talliedQuantity = 0f;
	}

	public boolean isEmpty() {
		return duplicateCounts.isEmpty();
	}


	/**
	 * @return a Non-modifiable Map
	 */
	public List<FluidStored> getFluids() {
		return Collections.unmodifiableList(duplicateCounts.stream().map(wr -> wr.fluid).collect(Collectors.toList()));
	}

	public int getUniqueFluidCount(){
		return duplicateCounts.size();
	}

	public float getTotalFluidQuantity() {
		return talliedQuantity;
	}

	public float getFluidQuantity(FluidStored fluid) {
		return hasFluid(fluid)?duplicateCounts.get(duplicateCounts.indexOf(fluid)).fluid.getMillilitres():0;
	}

	public float getFluidCapacity() {
		return capacity;
	}

	public void addAll(List<FluidStored> fluids) {
		fluids.forEach(this::addFluid);
	}

	/**
	 * @param fluid the StoredFluid added
	 * @param quantity
	 * @return the quantity that was actually added, if the capacity has been reached
	 */
	public float addFluid(FluidStored fluid, float quantity) {

		quantity = Math.min(capacity-talliedQuantity, quantity);

		fluid = new FluidStored(fluid);
		fluid.setMillilitres(quantity);
		FluidWrapper wrapper = new FluidWrapper(fluid);
		if(fluid.getMillilitres()<0){
			return removeFluid(fluid, -fluid.getMillilitres());
		}

		boolean hasFluid = hasFluid(wrapper);
		if (hasFluid) {
			FluidWrapper entry = duplicateCounts.get(duplicateCounts.indexOf(wrapper));
			entry.fluid.setMillilitres(entry.fluid.getMillilitres() + quantity);
			entry.fluid.setVirility((entry.fluid.getVirility() * entry.fluid.getMillilitres() + wrapper.fluid.getVirility() * quantity)
					/ (entry.fluid.getMillilitres() + quantity));
		} else {
			duplicateCounts.add(wrapper);
		}
		talliedQuantity += quantity;
		return quantity;
	}
	public float addFluid(FluidStored fluid) {
		return addFluid(fluid, fluid.getMillilitres());
	}

	public boolean hasFluid(FluidStored fluid) {
		return duplicateCounts.contains(new FluidWrapper(fluid));
	}
	public boolean hasFluid(FluidWrapper fluid) {
		return duplicateCounts.contains(fluid);
	}

	public FluidStored getFluid(FluidStored fluid){
		FluidWrapper tempWrap = new FluidWrapper(fluid);
//		int index = duplicateCounts.indexOf(tempWrap);
//		if(index != -1){
//			duplicateCounts.get(index);
//		}
		for(FluidWrapper wrapper : duplicateCounts){
			if(wrapper.equals(tempWrap)) return wrapper.fluid;
		}
		return null;
	}

	/**
	 * Removes the FluidStored entry regardless of quantity
	 * @param fluid
	 * @return
	 */
	public boolean removeFluid(FluidStored fluid) {
		return duplicateCounts.remove(fluid);
	}

	/**
	 * @param fluid the StoredFluid removed
	 * @param quantity
	 * @return the quantity that was actually removed, if there wasn't enough fluid
	 */
	public float removeFluid(FluidStored fluid, float quantity) {
		float removedQuantity=0;
		if (hasFluid(fluid)) {
			FluidWrapper entry = duplicateCounts.get(duplicateCounts.indexOf(new FluidWrapper(fluid)));
			System.out.println("removed ml start:" + entry.fluid.getMillilitres() + " removin: " + quantity);
			removedQuantity = Math.min(entry.fluid.getMillilitres(), quantity);
			talliedQuantity -= removedQuantity;
			entry.fluid.setMillilitres(entry.fluid.getMillilitres()-quantity);
			System.out.println("removed ml:" + entry.fluid.getMillilitres());
			if(entry.fluid.getMillilitres()<0.001f){
				System.out.println("removed! ml:" + entry.fluid.getMillilitres());
				duplicateCounts.remove(new FluidWrapper(fluid));
			}
		}
		return removedQuantity;
	}
}
