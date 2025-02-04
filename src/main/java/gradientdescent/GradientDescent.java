/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2025 RS-FISH developers.
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
package gradientdescent;

import java.util.ArrayList;

import net.imglib2.util.Util;

import fitting.Spot;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;

/**
 * Radial Symmetry Package
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see http://www.gnu.org/licenses/.
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de) and Timothee Lionnet
 */
public class GradientDescent 
{
	public static void testGradientDescent( final ArrayList< Spot > spots, final boolean[] dims ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final int numDimensions = spots.get( 0 ).candidates.get( 0 ).getP1().getL().length;
		final float[] scale = new float[ numDimensions ];
		for ( int d = 0; d < numDimensions; ++d )
			scale[ d ] = 1;
		
		final float[] scaleUp = scale.clone();
		final float[] scaleDn = scale.clone();
		
		// in 10 steps from scaling of 2 to a scaling of 1.001
		final float[] steps = computeSteps( 0.3, 1.4, 0.01  );
		//final float[] steps = computeSteps( 2, 100, 1.001 );
		
		System.out.println( Util.printCoordinates( steps ) );
		//System.exit( 0 );
		
		// get current cost
		double cost = test( spots, scale );
		System.out.println( "Initial cost: " + cost ); 
		
		for ( final float step : steps )
		{		
			int isBetter = 0;

			// repeat while it gets better in any of the dimensions
			// at this scale change
			do
			{
				// init count of dimensions that get better
				isBetter = 0;
				
				for ( int d = 0; d < numDimensions; ++d )
				{
					// test this dimension?
					if ( !dims[ d ] )
						continue;
					
					for ( int e = 0; e < numDimensions; ++e )
						scaleUp[ e ] = scaleDn[ e ] = scale[ e ];
					
					scaleUp[ d ] = scale[ d ] + step;
					scaleDn[ d ] = scale[ d ] - step;

					final double costUp = test( spots, scaleUp );
					final double costDn = test( spots, scaleDn );
					
					if ( costUp < cost && costUp < costDn )
					{
						//System.out.println( Util.printCoordinates( scaleDn ) + " c=" + costDn + " -> " + Util.printCoordinates( scale ) + " c=" + cost + " Dn" );
						//System.out.println( Util.printCoordinates( scaleUp ) + " c=" + costUp + " -> " + Util.printCoordinates( scale ) + " c=" + cost + " <-------Up" );
						++isBetter;
						cost = costUp;
						scale[ d ] = scaleUp[ d ];
					}
					else if ( costDn < cost )
					{
						//System.out.println( Util.printCoordinates( scaleDn ) + " c=" + costDn + " -> " + Util.printCoordinates( scale ) + " c=" + cost + " <-------Dn" );
						//System.out.println( Util.printCoordinates( scaleUp ) + " c=" + costUp + " -> " + Util.printCoordinates( scale ) + " c=" + cost + " Up" );
						++isBetter;
						cost = costDn;
						scale[ d ] = scaleDn[ d ];					
					}
					else
					{
						//System.out.println( Util.printCoordinates( scaleDn ) + " c=" + costDn + " -> " + Util.printCoordinates( scale ) + " c=" + cost + " Dn" );
						//System.out.println( Util.printCoordinates( scaleUp ) + " c=" + costUp + " -> " + Util.printCoordinates( scale ) + " c=" + cost + " Up" );						
					}
				}
			}
			while ( isBetter > 0 );
		}
		
		cost = test( spots, scale );
		
		//for ( final Spot spot : spots )
		//	System.out.println( spot ); 
		
		System.out.println( "Final cost: " + cost );
		System.out.println( "Final scale: " + Util.printCoordinates( scale ) );
		
		/*
		
		for ( float alpha = 0.5f; alpha < 2f; alpha += 0.01f )
		{
			System.out.print( "scale " + alpha + " ... ");
			
			scale[ 1 ] = alpha;
			
			double cost = 0;
			
			for ( final Spot spot : spots )
			{
				spot.updateScale( scale );
				spot.center.fit( spot.candidates );
				cost += spot.computeAverageCostCandidates();
			}
			
			System.out.println( "cost = " + cost/spots.size() );// + "        " + spots.get( 0 ) );
			
			//for ( final Spot spot : spots )
			//	System.out.println( spot );
		}
		*/
	}
	
	protected static double test( final ArrayList< Spot > spots, final float[] scale ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		double cost = 0;
		
		for ( final Spot spot : spots )
		{
			spot.updateScale( scale );
			spot.center.fit( spot.candidates );
			cost += spot.computeAverageCostCandidates();
		}
				
		return cost / (double)spots.size();
	}
	
	protected static float[] computeSteps( final double size, final double stepSize, final double minPrecision )
	{
		// the step size we will use
		final ArrayList< Float > stepList = new ArrayList<Float>();
		
		float s = (float)size;
		stepList.add( s );
		
		do
		{
			s /= stepSize;
			stepList.add( s );
		}
		while ( s > minPrecision );
				
		final float[] steps = new float[ stepList.size() ];
		
		for ( int i = 0; i < steps.length; ++i )
			steps[ i ] = stepList.get( i );
		
		return steps;
	}

	protected static float[] computeStepsMul( final double maxPrecision, final int numSteps, final double minPrecision )
	{
		// the step size we will use
		final float[] steps = new float[ numSteps ];
		final double stepSize = Math.pow( 10, (Math.log10( maxPrecision ) - Math.log10( minPrecision ))/(numSteps - 1) );
		
		double s = maxPrecision;
		steps[ 0 ] = (float)s;
		
		for ( int i = 1; i < numSteps; ++i )
		{
			s /= stepSize;
			steps[ i ] = (float)s;
		}
				
		return steps;
	}

}
