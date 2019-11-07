package imglib2;

import net.imglib2.Interval;
import net.imglib2.Positionable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPositionable;
import net.imglib2.type.Type;

public class TypeTransformingRandomAccessibleInterval< S, T extends Type< T > > implements RandomAccessibleInterval< T >
{
	final RandomAccessibleInterval< S > source;
	final ValueTransformation< S, T > transform;
	final T value;

	public TypeTransformingRandomAccessibleInterval( final RandomAccessibleInterval< S > source, final ValueTransformation< S, T > transform, final T value )
	{
		this.source = source;
		this.transform = transform;
		this.value = value.createVariable();
	}

	@Override
	public RandomAccess<T> randomAccess() { return new TypeTransformingRandomAccess<>( source.randomAccess(), transform, value ); }

	@Override
	public RandomAccess<T> randomAccess( final Interval interval ) { return new TypeTransformingRandomAccess<>( source.randomAccess( interval ), transform, value ); }

	@Override
	public int numDimensions() { return source.numDimensions(); }

	@Override
	public long min( final int d ) { return source.min( d ); }

	@Override
	public void min( final long[] min ) { source.min( min ); }

	@Override
	public void min( final Positionable min ) { source.min( min ); }

	@Override
	public long max( final int d ) { return source.max( d ); }

	@Override
	public void max( final long[] max ) { source.max( max ); }

	@Override
	public void max( final Positionable max ) { source.max( max ); }

	@Override
	public double realMin( final int d ) { return source.realMin( d ); }

	@Override
	public void realMin( final double[] min) { source.realMin( min ); }

	@Override
	public void realMin( final RealPositionable min ) { source.realMin( min ); }

	@Override
	public double realMax( final int d ) { return source.realMax( d ); }

	@Override
	public void realMax( final double[] max ) { source.realMax( max ); }

	@Override
	public void realMax( final RealPositionable max ) { source.realMax( max ); }

	@Override
	public void dimensions( final long[] dimensions) { source.dimensions( dimensions ); } 

	@Override
	public long dimension( final int d ) { return source.dimension( d ); }
}
