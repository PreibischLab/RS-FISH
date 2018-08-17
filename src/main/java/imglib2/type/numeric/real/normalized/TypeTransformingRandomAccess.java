package imglib2.type.numeric.real.normalized;

import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.Sampler;
import net.imglib2.type.Type;

public class TypeTransformingRandomAccess< S, T extends Type< T > > implements RandomAccess< T >
{
	final RandomAccess< S > sourceRandomAccess;
	final ValueTransformation< S, T > transform;
	
	final T value;

	public TypeTransformingRandomAccess( final RandomAccess< S > sourceRandomAccess, final ValueTransformation< S, T > transform, final T value )
	{
		this.sourceRandomAccess = sourceRandomAccess;
		this.transform = transform;
		this.value = value.createVariable();
	}

	@Override
	public T get()
	{
		transform.transform( sourceRandomAccess.get(),  value );
		return value;
	}

	@Override
	public void fwd( final int d ) { sourceRandomAccess.fwd( d ); }

	@Override
	public void bck( final int d ) { sourceRandomAccess.bck( d ); }

	@Override
	public void move( final int distance, final int d ) { sourceRandomAccess.move( distance, d ); }

	@Override
	public void move( final long distance, final int d) { sourceRandomAccess.move( distance, d ); }

	@Override
	public void move( final Localizable localizable ) { sourceRandomAccess.move( localizable ); }

	@Override
	public void move( final int[] distance) { sourceRandomAccess.move( distance ); }

	@Override
	public void move( final long[] distance ) { sourceRandomAccess.move( distance ); }

	@Override
	public void setPosition( final Localizable localizable ) { sourceRandomAccess.setPosition( localizable ); }

	@Override
	public void setPosition( final int[] position ) { sourceRandomAccess.setPosition( position ); }

	@Override
	public void setPosition( final long[] position ) { sourceRandomAccess.setPosition( position ); }

	@Override
	public void setPosition( final int position, final int d ) { sourceRandomAccess.setPosition( position, d ); }

	@Override
	public void setPosition( final long position, final int d ) { sourceRandomAccess.setPosition( position, d ); }

	@Override
	public Sampler<T> copy() { return copyRandomAccess(); }

	@Override
	public RandomAccess<T> copyRandomAccess() { return new TypeTransformingRandomAccess<>( sourceRandomAccess.copyRandomAccess(), transform, value ); }

	@Override
	public void localize( final int[] position ) { sourceRandomAccess.localize( position ); }

	@Override
	public void localize( final long[] position ) { sourceRandomAccess.localize( position ); }

	@Override
	public int getIntPosition( final int d ) { return sourceRandomAccess.getIntPosition( d ); }

	@Override
	public long getLongPosition( final int d ) { return sourceRandomAccess.getLongPosition( d ); }

	@Override
	public void localize( final float[] position ) { sourceRandomAccess.localize( position ); }

	@Override
	public void localize( final double[] position ) { sourceRandomAccess.localize( position ); }

	@Override
	public float getFloatPosition( final int d ) { return sourceRandomAccess.getFloatPosition( d ); }

	@Override
	public double getDoublePosition( final int d ) { return sourceRandomAccess.getDoublePosition( d ); }

	@Override
	public int numDimensions() { return sourceRandomAccess.numDimensions(); }
}
