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
package benchmark;

import net.imglib2.RealLocalizable;
import net.imglib2.util.Util;

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
public class GroundTruthPoint implements RealLocalizable
{
	final double[] loc;
	final int n;
	
	// how often it has been assigned to another point
	int numAssigned;
	
	public GroundTruthPoint( final double[] loc )
	{
		this.loc = loc;
		this.n = loc.length;
		this.numAssigned = 0;
	}
	
	public int getNumAssigned() { return numAssigned; }
	public void incNumAssigned() { ++numAssigned; }
	public void setNumAssigned( final int num ) { this.numAssigned = num; }
	
	@Override
	public int numDimensions() { return n; }

	@Override
	public void localize( final float[] position ) 
	{
		for ( int d = 0; d < n; ++d )
			position[ d ] = (float)loc[ d ];
	}

	@Override
	public void localize( final double[] position ) 
	{
		for ( int d = 0; d < n; ++d )
			position[ d ] = loc[ d ];
	}

	@Override
	public float getFloatPosition( final int d ) { return (float)loc[ d ]; }

	@Override
	public double getDoublePosition( final int d ) { return loc[ d ]; }

	@Override
	public String toString() { return Util.printCoordinates( loc ); }
}
