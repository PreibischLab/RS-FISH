/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2024 RS-FISH developers.
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
package imglib2;

import net.imglib2.type.numeric.RealType;

public class RealTypeNormalization< T extends RealType< T > > implements ValueTransformation< T, T >
{
	final double min, range;

	public RealTypeNormalization( final double min, final double range )
	{
		this.min = min;
		this.range = range;
	}

	@Override
	public void transform( final T a, final T b )
	{
		b.setReal( norm( a.getRealDouble(), min, range ) );
	}

	public static final double norm( final double val, final double min, final double range )
	{
		return (val - min) / range;
	}
}
