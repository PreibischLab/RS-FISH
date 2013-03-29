package gradient;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.DoubleType;

public class Gradient3d 
{
	/**
	 * Computes the n-dimensional 1st derivative vector in 3x3x3...x3 environment for a certain {@link Image} location
	 * defined by the position of the {@link LocalizableByDimCursor}.
	 * 
	 * @param cursor - the position for which to compute the Hessian Matrix
	 * @param Image<DoubleType> - the derivative, which is essentially a one-dimensional {@link DoubleType} {@link Image} of size [numDimensions]
	 */
	final public static <T extends RealType<T>> void computeDerivativeVector1( final LocalizableByDimCursor<T> cursor, final Image<DoubleType> derivativeVector )
	{
		// instantiate a cursor to traverse over the derivative vector we want to compute, the position defines the current dimension
		final LocalizableCursor<DoubleType> derivativeCursor = derivativeVector.createLocalizableCursor();
		
		while ( derivativeCursor.hasNext() )
		{
			derivativeCursor.fwd();
			
			final int dim = derivativeCursor.getPosition( 0 );
			
			// we compute the derivative for dimension A like this
			//
			// | a0 | a1 | a2 | 
			//        ^
			//        |
			//  Original position of image cursor
			//
			// d(a) = (a2 - a0)/2
			// we divide by 2 because it is a jump over two pixels
			
			cursor.fwd( dim );
			
			final double a2 = cursor.getType().getRealDouble();
			
			cursor.bck( dim );
			cursor.bck( dim );
			
			final double a0 = cursor.getType().getRealDouble();
			
			// back to the original position
			cursor.fwd( dim );
						
			derivativeCursor.getType().setReal( (a2 - a0)/2 );
		}
		
		derivativeCursor.close();
	}

	/**
	 * Computes the n-dimensional 1st derivative vector in 3x3x3...x3 environment for a certain {@link Image} location
	 * defined by the position of the {@link LocalizableByDimCursor}.
	 * 
	 * @param cursor - the position for which to compute the Hessian Matrix
	 * @param Image<DoubleType> - the derivative, which is essentially a one-dimensional {@link DoubleType} {@link Image} of size [numDimensions]
	 */
	final public static <T extends RealType<T>> void computeDerivativeVector3d( final LocalizableByDimCursor<T> cursor, final float[] derivativeVector )
	{
		// we need all 8 points
		final double p0 = cursor.getType().getRealDouble();
		cursor.fwd( 0 );
		final double p1 = cursor.getType().getRealDouble();
		cursor.fwd( 1 );
		final double p3 = cursor.getType().getRealDouble();
		cursor.bck( 0 );
		final double p2 = cursor.getType().getRealDouble();
		cursor.fwd( 2 );
		final double p6 = cursor.getType().getRealDouble();
		cursor.fwd( 0 );
		final double p7 = cursor.getType().getRealDouble();
		cursor.bck( 1 );
		final double p5 = cursor.getType().getRealDouble();
		cursor.bck( 0 );
		final double p4 = cursor.getType().getRealDouble();
		
		// dim 0 
		derivativeVector[ 0 ] = (float) ( ( (p1+p3+p5+p7) - (p0+p2+p4+p6) ) / 4.0 );
		derivativeVector[ 1 ] = (float) ( ( (p2+p3+p6+p7) - (p0+p1+p4+p5) ) / 4.0 );
		derivativeVector[ 2 ] = (float) ( ( (p4+p5+p6+p7) - (p0+p1+p2+p3) ) / 4.0 );		
	}

}
