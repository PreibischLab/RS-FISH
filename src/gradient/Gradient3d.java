package gradient;

import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.RealType;

public class Gradient3d 
{
	/**
	 * Computes the n-dimensional 1st derivative vector in center of a 2x2x2 environment for a certain location
	 * defined by the position of the RandomAccess
	 * 
	 * @param randomAccess - the top-left-front position for which to compute the derivative
	 * @param derivativeVector - where to put the derivative vector [3]
	 */
	final public static <T extends RealType<T>> void computeDerivativeVector3d( final RandomAccess<T> randomAccess, final float[] derivativeVector )
	{
		// we need 8 points
		final double p0 = randomAccess.get().getRealDouble();
		randomAccess.fwd( 0 );
		final double p1 = randomAccess.get().getRealDouble();
		randomAccess.fwd( 1 );
		final double p3 = randomAccess.get().getRealDouble();
		randomAccess.bck( 0 );
		final double p2 = randomAccess.get().getRealDouble();
		randomAccess.fwd( 2 );
		final double p6 = randomAccess.get().getRealDouble();
		randomAccess.fwd( 0 );
		final double p7 = randomAccess.get().getRealDouble();
		randomAccess.bck( 1 );
		final double p5 = randomAccess.get().getRealDouble();
		randomAccess.bck( 0 );
		final double p4 = randomAccess.get().getRealDouble();
		
		derivativeVector[ 0 ] = (float) ( ( (p1+p3+p5+p7) - (p0+p2+p4+p6) ) / 4.0 );
		derivativeVector[ 1 ] = (float) ( ( (p2+p3+p6+p7) - (p0+p1+p4+p5) ) / 4.0 );
		derivativeVector[ 2 ] = (float) ( ( (p4+p5+p6+p7) - (p0+p1+p2+p3) ) / 4.0 );		
	}

}
