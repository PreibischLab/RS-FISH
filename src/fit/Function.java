package fit;

import java.util.Collection;

import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;

/**
 * Interface for a {@link Function} that can be fit to {@link Point}s
 * 
 * @author Stephan Preibisch
 *
 * @param <P> - if a special extension of {@link Point} is necessary, otherwise just implement Function<Point>
 */
public interface Function< P extends Point >
{
	/**
	 * @return - how many points are at least necessary to fit the function
	 */
	public int getMinNumPoints();

	/**
	 * Fits this Function to the set of {@link Point}s.

	 * @param points - {@link Collection} of {@link Point}s
	 * @throws NotEnoughDataPointsException - thrown if not enough {@link Point}s are in the {@link Collection}
	 */
	public void fitFunction( final Collection<P> points ) throws NotEnoughDataPointsException;
	
	/**
	 * Computes the minimal distance of a {@link Point} to this function
	 *  
	 * @param point - the {@link Point}
	 * @return - distance to the {@link Function}
	 */
	public double distanceTo( final P point );

}
