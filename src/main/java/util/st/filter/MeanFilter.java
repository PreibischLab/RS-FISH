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
package util.st.filter;

import net.imglib2.RealLocalizable;
import net.imglib2.neighborsearch.RadiusNeighborSearch;
import net.imglib2.type.numeric.RealType;

public class MeanFilter< S extends RealType< S >, T extends RealType< T > > extends RadiusSearchFilter< S, T >
{
	final T outofbounds;

	public MeanFilter(
			final RadiusNeighborSearch< S > search,
			final double radius,
			final T outofbounds )
	{
		super( search, radius );

		this.outofbounds = outofbounds;
	}

	@Override
	public void filter( final RealLocalizable position, final T output )
	{
		search.search( position, radius, false );

		if ( search.numNeighbors() == 0 )
		{
			output.set( outofbounds );
		}
		else if ( search.numNeighbors() == 1 )
		{
			output.setReal( search.getSampler( 0 ).get().getRealDouble() );
		}
		else
		{
			double value = 0;

			for ( int i = 0; i < search.numNeighbors(); ++i )
				value += search.getSampler( i ).get().getRealDouble();

			output.setReal( value / search.numNeighbors() );
		}
	}
}
