/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 RS-FISH developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package fitting;

import java.util.ArrayList;
import java.util.Collection;

import compute.RadialSymmetry;
import fit.AbstractFunction;
import fit.PointFunctionMatch;
import net.imglib2.util.Util;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;

public class Center extends AbstractFunction< Center >
{
	private static final long serialVersionUID = 1L;

	public enum CenterMethod { MEAN, MEDIAN };

	final int minNumPoints = 1;
	double p = 0;

	final boolean mean;

	public Center( final CenterMethod method )
	{
		if ( method == CenterMethod.MEAN )
			mean = true;
		else
			mean = false;
	}

	public double getP() { return p; }

	@Override
	public int getMinNumPoints() { return minNumPoints; }

	@Override
	public void fitFunction( final Collection<Point> points ) throws NotEnoughDataPointsException
	{
		final int numPoints = points.size();
		
		if ( numPoints < minNumPoints )
			throw new NotEnoughDataPointsException( "Not enough points, at least " + minNumPoints + " are necessary." );

		// could be an interface instead of an if
		if ( mean )
		{
			double sum = 0;

			for ( final Point p : points )
				sum += p.getW()[ 0 ];

			this.p = sum / (double)points.size();
		}
		else
		{
			final double[] values = new double[ points.size() ];

			int i = 0;

			for ( final Point p : points )
				values[ i++ ] = p.getW()[ 0 ];

			this.p = Util.median( values );
		}
	}

	@Override
	public double distanceTo( final Point point )
	{
		return Math.abs( point.getW()[ 0 ] - p );
	}
	
	public static int i = 0;
	
	@Override
	public void set( final Center m )
	{
		this.p = m.getP();
		this.setCost( m.getCost() );
	}

	@Override
	public Center copy()
	{
		Center c;
		if ( mean )
			c = new Center( CenterMethod.MEAN );
		else
			c = new Center( CenterMethod.MEDIAN );

		c.p = getP();
		c.setCost( getCost() );
		
		return c;
	}
	
	public static void main( String[] args ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final ArrayList< Point > points = new ArrayList<Point>();

		points.add( new Point( new double[]{ 1f } ) );
		points.add( new Point( new double[]{ 3f } ) );
		points.add( new Point( new double[]{ 1.5f } ) );
		points.add( new Point( new double[]{ 0.8f } ) );
		
		final ArrayList< PointFunctionMatch > candidates = new ArrayList<PointFunctionMatch>();
		final ArrayList< PointFunctionMatch > inliers = new ArrayList<PointFunctionMatch>();
		
		for ( final Point p : points )
			candidates.add( new PointFunctionMatch( p ) );
		
		final Center l = new Center( CenterMethod.MEAN );
		
		l.ransac( candidates, inliers, RadialSymmetry.bsNumIterations, 1, 0.1 );
		
		System.out.println( inliers.size() );
		
		l.fit( inliers );
		
		System.out.println( "p = " + l.p );
		for ( final PointFunctionMatch p : inliers )
			System.out.println( p.getP1().getL()[ 0 ] + " " + l.distanceTo( p.getP1() ) );
		
		//System.out.println( l.distanceTo( new Point( new float[]{ 1f, 0f } ) ) );
	}}
