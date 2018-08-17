package imglib2.type.numeric.real.normalized;

import net.imglib2.type.numeric.RealType;

public class RealTypeNormalization< T extends RealType< T > > implements ValueTransformation< T, T >
{
	final double min, range;

	public RealTypeNormalization( final double min, final double range )
	{
		this.min = min;
		this.range = range;
	}

	@Override
	public void transform( final T a, final T b )
	{
		b.setReal( norm( a.getRealDouble(), min, range ) );
	}

	public static final double norm( final double val, final double min, final double range )
	{
		return (val - min) / range;
	}
}
