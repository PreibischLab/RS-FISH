package gradient;

import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Computes the derivative on demand at a certain location, this is useful if it is only a few spots in a big image 
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 */
public class GradientOnDemand extends Gradient
{
	final ComputeGradient computeGradient;	
	
	public GradientOnDemand( final RandomAccessibleInterval<FloatType> source )
	{
		super( source.numDimensions() );
		
		if ( numDimensions == 2 )
			computeGradient = new ComputeGradient2d( source.randomAccess() );
		else if ( numDimensions == 3 )
			computeGradient = new ComputeGradient3d( source.randomAccess() );
		else
			throw new RuntimeException( "GradientOnDemand: Only 2d/3d is allowed for now" );
	}

	@Override
	public void gradientAt( final Localizable location, final float[] derivativeVector )
	{
		computeGradient.gradientAt( location, derivativeVector );
	}
}
