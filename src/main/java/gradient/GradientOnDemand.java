/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 RS-FISH developers.
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

import net.imglib2.Localizable;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Computes the derivative on demand at a certain location, this is useful if it is only a few spots in a big image 
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 */
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
public class GradientOnDemand extends Gradient
{
	final ComputeGradient computeGradient;	
	
	public GradientOnDemand( final RandomAccessible<FloatType> source )
	{
		super( source.numDimensions() );
		
		if ( numDimensions == 2 )
			computeGradient = new ComputeGradient2d( source.randomAccess() );
		else if ( numDimensions == 3 )
			computeGradient = new ComputeGradient3d( source.randomAccess() );
		else
			throw new RuntimeException( "GradientOnDemand: Only 2d/3d is allowed for now" );
	}

	@Override
	public void gradientAt( final Localizable location, final double[] derivativeVector )
	{
		computeGradient.gradientAt( location, derivativeVector );
	}
}
