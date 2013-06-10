package derivative;

import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

public abstract class Derivative 
{
	final RandomAccessibleInterval< FloatType > source;
	
	public Derivative( final RandomAccessibleInterval< FloatType > source )
	{
		this.source = source;
		
		if ( source.numDimensions() != 3 )
			throw new RuntimeException( "Only 3d is allowed for now" );
	}
	
	/**
	 * Computes the n-dimensional 1st derivative vector in center of a 2x2x2...x2 environment for a certain location
	 * defined by the position of the RandomAccess
	 * 
	 * @param location - the top-left-front position for which to compute the derivative
	 * @param derivativeVector - where to put the derivative vector [3]
	 */
	public abstract void gradientAt( final Localizable location, final float[] derivativeVector );
}
