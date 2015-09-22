package gradient;

import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.real.FloatType;

public class ComputeGradient3d implements ComputeGradient
{
	final RandomAccess< FloatType > randomAccess;
	
	public ComputeGradient3d( final RandomAccess< FloatType > randomAccess )
	{
		this.randomAccess = randomAccess;
	}
	
	@Override
	public void gradientAt( final Localizable location, final double[] derivativeVector )
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
		
		derivativeVector[ 0 ] = ( ( (p1+p3+p5+p7) - (p0+p2+p4+p6) ) / 4.0 );
		derivativeVector[ 1 ] = ( ( (p2+p3+p6+p7) - (p0+p1+p4+p5) ) / 4.0 );
		derivativeVector[ 2 ] = ( ( (p4+p5+p6+p7) - (p0+p1+p2+p3) ) / 4.0 );
	}
}
