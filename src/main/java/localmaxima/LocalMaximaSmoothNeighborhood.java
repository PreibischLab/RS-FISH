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
package localmaxima;

import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss.Gauss;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

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
public class LocalMaximaSmoothNeighborhood extends LocalMaximaNeighborhood
{
	public LocalMaximaSmoothNeighborhood( final RandomAccessibleInterval<FloatType> source, final double[] sigma )
	{
		super( smooth( source, sigma ) );	
	}

	public LocalMaximaSmoothNeighborhood( final RandomAccessibleInterval<FloatType> source, final double[] sigma, final double threshold )
	{
		super( smooth( source, sigma ), threshold );	
	}
	
	private static final Img< FloatType > smooth( final RandomAccessibleInterval<FloatType> source, final double[] sigma )
	{
		final Img< FloatType > smoothed = new ArrayImgFactory<FloatType>().create( source, new FloatType() );
		Gauss.inFloat( sigma, Views.extendMirrorSingle( source ), source, smoothed, new Point( source.numDimensions() ), new ArrayImgFactory< FloatType >() );
		return smoothed;
	}
}
