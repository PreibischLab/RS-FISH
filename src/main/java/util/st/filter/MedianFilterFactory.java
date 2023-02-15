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

import net.imglib2.KDTree;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.type.numeric.RealType;

public class MedianFilterFactory< T extends RealType< T > > extends RadiusSearchFilterFactory< T, T >
{
	final T outofbounds;
	final double radius;

	public MedianFilterFactory(
			final T outofbounds,
			final double radius )
	{
		this.radius = radius;
		this.outofbounds = outofbounds;
	}

	@Override
	public Filter< T > createFilter( final KDTree< T > tree )
	{
		return new MedianFilter< T >(
				new RadiusNeighborSearchOnKDTree<>( tree ),
				radius,
				outofbounds );
	}

	@Override
	public T create()
	{
		return outofbounds.createVariable();
	}
}
