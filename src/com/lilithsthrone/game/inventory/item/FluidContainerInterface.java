package com.lilithsthrone.game.inventory.item;

import com.lilithsthrone.game.character.FluidStored;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 0.1.86
 * @version 0.4.0
 * @author Innoxia
 */
public interface FluidContainerInterface {

	public List<FluidStored> getStoredFluids();

	public float getMillilitresStored();

	public float addFluid(FluidStored fluid);

	public float addFluid(FluidStored fluid, float amount);

	public float removeFluid(FluidStored fluid, float quantity);

	public float getMaxCapacity();

	public FluidStored getFluid(FluidStored fluid);
	
}
