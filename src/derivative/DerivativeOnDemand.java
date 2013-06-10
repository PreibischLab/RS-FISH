package derivative;

import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Computes the derivative on demand at a certain location, this is useful if it is only a few spots in a big image 
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 */
public class DerivativeOnDemand extends Derivative
{
	final RandomAccess< FloatType > randomAccess;
	
	public DerivativeOnDemand( final RandomAccessibleInterval<FloatType> source )
	{
		super( source.numDimensions() );
		
		this.randomAccess = source.randomAccess();
	}

	@Override
	public void gradientAt( final Localizable location, final float[] derivativeVector )
	{
		randomAccess.setPosition( location );
		
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
