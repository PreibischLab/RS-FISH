package fit;

import mpicbg.models.Point;

public class OrientedPoint extends Point
{
	private static final long serialVersionUID = -5947160364235033692L;

	/**
	 * Orientation in local coordinates
	 */
	protected final float[] ol;

	/**
	 * Orientation in world coordinates
	 */
	protected final float[] ow;

	/**
	 * gradient magnitude
	 */
	protected final float magnitude;

	// TODO: Multithreading-save
	protected final float[] tmp;
	
	public OrientedPoint( final float[] position, final float[] vector, final float magnitude )
	{
		super( position );

		ol = vector;
		ow = ol.clone();
		this.magnitude = magnitude;
		
		tmp = new float[ ol.length ];
	}
	
	/**
	 * @return The magnitude of the orientation/gradient
	 */
	public float getMagnitude(){ return magnitude; }	

	/**
	 * @return The orientation/gradient in local coordinates
	 */
	public float[] getOrientationL(){ return ol; }
	
	/**
	 * @return The orientation/gradient in world coordinates
	 */
	public float[] getOrientationW(){ return ow; }
	
	/**
	 * angle between this points orientation
	 * the direction from this point to p (in world coordinates).
	 * 
	 * @param p
	 * @return angle in radians [0, pi]
	 */
	public float angleTo( Point p )
	{
		double len = 0;
		for ( int d = 0; d < ow.length; ++d )
		{
			tmp[ d ] = p.getW()[ d ] - w[ d ];
			len += tmp[ d ] * tmp[ d ];
		}
		len = Math.sqrt( len );

		double dot = 0;
		for ( int d = 0; d < ow.length; ++d )
		{
			dot += ow[ d ] * tmp[ d ] / len;
		}
		return (float) Math.acos( dot );
	}
}
