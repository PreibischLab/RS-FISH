package gradient;

import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.real.FloatType;

public class ComputeGradient2d implements ComputeGradient
{
	final RandomAccess< FloatType > randomAccess;
	
	public ComputeGradient2d( final RandomAccess< FloatType > randomAccess )
	{
		this.randomAccess = randomAccess;
	}
	
	@Override
	public void gradientAt( final Localizable location, final float[] derivativeVector )
	{
		randomAccess.setPosition( location );
		
		// we need 4 points
		final double p0 = randomAccess.get().getRealDouble();
		randomAccess.fwd( 0 );
		final double p1 = randomAccess.get().getRealDouble();
		randomAccess.fwd( 1 );
		final double p3 = randomAccess.get().getRealDouble();
		randomAccess.bck( 0 );
		final double p2 = randomAccess.get().getRealDouble();
		
		derivativeVector[ 0 ] = (float) ( ( (p1+p3) - (p0+p2) ) / 2.0 );
		derivativeVector[ 1 ] = (float) ( ( (p2+p3) - (p0+p1) ) / 2.0 );
	}
}
