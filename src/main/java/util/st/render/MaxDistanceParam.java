package util.st.render;

/** 
 * A common parameter class so maxDistance can be changed externally
 */
public class MaxDistanceParam
{
	private double maxSqDistance, maxDistance;

	public MaxDistanceParam( final double maxDistance ) { setMaxDistance( maxDistance ); }

	public void setMaxDistance( final double maxDistance )
	{
		this.maxDistance = maxDistance;
		this.maxSqDistance = maxDistance * maxDistance;
	}

	public double maxDistance() { return maxDistance; }
	public double maxSqDistance() { return maxSqDistance; }
}
