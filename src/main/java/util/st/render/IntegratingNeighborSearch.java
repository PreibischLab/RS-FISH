/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2025 RS-FISH developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package util.st.render;

import net.imglib2.EuclideanSpace;
import net.imglib2.RealLocalizable;
import net.imglib2.Sampler;

/**
 * Integrating neighbor search in a Euclidean space. The interface describes
 * implementations that perform the search (and potential filtering) around a
 * specific location and provide access to that value.
 * 
 * @author Stephan Preibisch
 */
public interface IntegratingNeighborSearch< T > extends EuclideanSpace
{
	/**
	 * Perform integrating-neighbor search for a reference coordinate.
	 * 
	 * @param reference
	 */
	void search(final RealLocalizable reference);

	/**
	 * Access the data of the nearest neighbor. Data is accessed through a
	 * {@link Sampler} that guarantees write access if the underlying data set
	 * is writable.
	 */
	Sampler< T > getSampler();

	/**
	 * Create a copy.
	 */
	IntegratingNeighborSearch< T > copy();
}
