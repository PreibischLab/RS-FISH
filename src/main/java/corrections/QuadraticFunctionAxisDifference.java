package corrections;

import fit.polynomial.QuadraticFunction;
import mpicbg.models.Point;

public class QuadraticFunctionAxisDifference extends QuadraticFunction
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3873461779139311393L;

	public QuadraticFunctionAxisDifference() { this( 0, 0, 0 ); }
	public QuadraticFunctionAxisDifference( final double a, final double b, final double c )
	{
		super( a, b,c );
	}

	@Override
	public double distanceTo(Point point)
	{
		final double x1 = point.getW()[0];
		final double y1 = point.getW()[1];

		return Math.abs( y1 - evaluateAt( x1 ) );
	}


	@Override
	public QuadraticFunctionAxisDifference copy()
	{
		final QuadraticFunctionAxisDifference c = new QuadraticFunctionAxisDifference( getA(), getB(), getC() );

		c.setCost( getCost() );

		return c;
	}

	public double evaluateAt( final double x )
	{
		return getC() + x*getB() + x*x*getA();
	}
}
