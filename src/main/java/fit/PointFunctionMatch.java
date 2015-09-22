package fit;

import mpicbg.models.CoordinateTransform;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;

public class PointFunctionMatch extends PointMatch
{
	private static final long serialVersionUID = -8070932126418631690L;

	//final protected Function<Point> function;

	double distance = 0;
	
	public PointFunctionMatch( final Point p1 )
	{
		super( p1, null );
	}
	
	//public Function<Point> getFunction() { return function; }

	/**
	 * 	Here one could compute and return the closest point on the function to p1,
	 *  but it is not well defined as there could be more than one...
	 */
	@Deprecated
	@Override
	public Point getP2() { return null; }
	
	public void apply( final CoordinateTransform t )
	{
		distance = (float)((Function<Point>)t).distanceTo( p1 );
	}
	
	public void apply( final CoordinateTransform t, final float amount )
	{
		distance = (float)((Function<Point>)t).distanceTo( p1 );
	}
	
	@Override
	public double getDistance() { return distance; }
}
