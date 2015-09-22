package benchmark;

import net.imglib2.RealLocalizable;
import net.imglib2.util.Util;

public class GroundTruthPoint implements RealLocalizable
{
	final double[] loc;
	final int n;
	
	// how often it has been assigned to another point
	int numAssigned;
	
	public GroundTruthPoint( final double[] loc )
	{
		this.loc = loc;
		this.n = loc.length;
		this.numAssigned = 0;
	}
	
	public int getNumAssigned() { return numAssigned; }
	public void incNumAssigned() { ++numAssigned; }
	public void setNumAssigned( final int num ) { this.numAssigned = num; }
	
	@Override
	public int numDimensions() { return n; }

	@Override
	public void localize( final float[] position ) 
	{
		for ( int d = 0; d < n; ++d )
			position[ d ] = (float)loc[ d ];
	}

	@Override
	public void localize( final double[] position ) 
	{
		for ( int d = 0; d < n; ++d )
			position[ d ] = loc[ d ];
	}

	@Override
	public float getFloatPosition( final int d ) { return (float)loc[ d ]; }

	@Override
	public double getDoublePosition( final int d ) { return loc[ d ]; }

	@Override
	public String toString() { return Util.printCoordinates( loc ); }
}
