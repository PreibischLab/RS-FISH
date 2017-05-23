package localmaxima;

import java.util.ArrayList;

import net.imglib2.EuclideanSpace;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

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
public abstract class LocalMaxima implements EuclideanSpace
{
	final protected RandomAccessibleInterval< FloatType > source;
	final protected int numDimensions;

	public LocalMaxima( final RandomAccessibleInterval< FloatType > source )
	{
		this.source = source;
		this.numDimensions = source.numDimensions();
	}

	public abstract ArrayList< int[] > estimateLocalMaxima();
	
	public RandomAccessibleInterval< FloatType > getSource() { return source; }

	@Override
	public int numDimensions() { return numDimensions; }
}
