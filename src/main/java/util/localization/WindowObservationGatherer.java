/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 Developers.
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
package util.localization;

import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

public class WindowObservationGatherer<T extends RealType<T>, L extends Localizable > implements ObservationGatherer< L >
{
	final RandomAccessibleInterval<T> image;
	final long[] padSize;

	public WindowObservationGatherer( final RandomAccessibleInterval<T> image, final long[] padSize )
	{
		this.image = image;
		this.padSize = padSize;
	}

	@Override
	public Observation gatherObservationData( final L peak )
	{
		return LocalizationUtils.gatherObservationData( image, peak, padSize );
	}

}
