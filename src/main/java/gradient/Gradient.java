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
package gradient;

import net.imglib2.EuclideanSpace;
import net.imglib2.Localizable;

/**
 * Radial Symmetry Package
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see http://www.gnu.org/licenses/.
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de) and Timothee Lionnet
 */
public abstract class Gradient implements EuclideanSpace
{
	final int numDimensions;
	
	public Gradient( final int numDimensions )
	{
		this.numDimensions = numDimensions;
	}
	
	/**
	 * Computes the n-dimensional 1st derivative vector in center of a 2x2x2...x2 environment for a certain location
	 * defined by the position of the RandomAccess
	 * 
	 * @param location - the top-left-front position for which to compute the derivative
	 * @param derivativeVector - where to put the derivative vector [3]
	 */
	public abstract void gradientAt( final Localizable location, final double[] derivativeVector );
	
	@Override
	public int numDimensions() { return numDimensions; }
}
