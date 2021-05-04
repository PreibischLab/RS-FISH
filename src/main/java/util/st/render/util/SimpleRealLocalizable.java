package util.st.render.util;

import net.imglib2.RealLocalizable;

public class SimpleRealLocalizable implements RealLocalizable
{
	final int n;
	final double[] pos;

	public SimpleRealLocalizable( final double[] pos )
	{
		this.n = pos.length;
		this.pos = pos;
	}

	@Override
	public int numDimensions()
	{
		return n;
	}

	@Override
	public void localize( final float[] position )
	{
		for ( int d = 0; d < n; ++d )
			position[ d ] = (float)pos[ d ];
	}

	@Override
	public void localize( final double[] position )
	{
		for ( int d = 0; d < n; ++d )
			position[ d ] = pos[ d ];
	}

	@Override
	public float getFloatPosition( final int d )
	{
		return (float)pos[ d ];
	}

	@Override
	public double getDoublePosition( int d )
	{
		return pos[ d ];
	}
}
