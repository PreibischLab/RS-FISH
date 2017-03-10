package compute;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import parameters.RadialSymmetryParameters;

public class RadialSymmetry< T extends RealType< T > >
{
	public RadialSymmetry( final RadialSymmetryParameters params, final RandomAccessibleInterval< T > img )
	{
		// actually run it on an image
		
		// something like the TestGauss2d or TestGauss3d (without simulation)
	}
}
