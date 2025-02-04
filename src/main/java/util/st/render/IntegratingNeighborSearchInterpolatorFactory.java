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

import net.imglib2.RealInterval;
import net.imglib2.interpolation.InterpolatorFactory;

public class IntegratingNeighborSearchInterpolatorFactory< T > implements InterpolatorFactory< T, IntegratingNeighborSearch< T > >
{
	@Override
	public IntegratingNeighborSearchInterpolator< T > create( final IntegratingNeighborSearch< T > search )
	{
		return new IntegratingNeighborSearchInterpolator<>(search.copy());
	}

	@Override
	public IntegratingNeighborSearchInterpolator< T > create( final IntegratingNeighborSearch< T > search, final RealInterval interval )
	{
		return create( search );
	}
}
