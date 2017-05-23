package benchmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import net.imglib2.util.Util;

import fit.Spot;

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
public class LoadSpotFile 
{
	public static ArrayList< Spot > loadSpots2( final File file )
	{
		final ArrayList< double[] > valuesDouble = loadSpotsDouble( file );
		final ArrayList< Spot > values = new ArrayList< Spot >();
		
		final int n = valuesDouble.get( 0 ).length;
		
		for ( final double[] v : valuesDouble )
		{
			final Spot s = new Spot( n );
			
			for ( int d = 0; d < n; ++d )
				s.center.setSymmetryCenter( v[ d ], d );
			
			values.add( s );			
		}

		return values;
	}
	
	public static ArrayList< GroundTruthPoint > loadSpots( final File file )
	{
		final ArrayList< double[] > valuesDouble = loadSpotsDouble( file );
		final ArrayList< GroundTruthPoint > values = new ArrayList< GroundTruthPoint >();
		
		for ( final double[] v : valuesDouble )
			values.add( new GroundTruthPoint( v ) );
		
		return values;
	}
	
	public static ArrayList< double[] > loadSpotsDouble( final File file )
	{
		final BufferedReader in = TextFileAccess.openFileRead( file );
		
		int n = -1;
		
		if ( in == null )
			return null;
		
		final ArrayList< double[] > values = new ArrayList< double[] >();
		
		try 
		{
			while ( in.ready() )
			{
				final String[] l = in.readLine().trim().split( "\\p{javaWhitespace}+" ); // n-times space

				if ( n == -1 )
				{
					// last entry is the intensity
					n = l.length - 1;
					
					// tim's file sometimes have a zero-row
					if ( n > 3 )
						n = 3;
				}
				
				final double[] v = new double[ n ];
				
				for ( int d = 0; d < n; ++d )
					v[ d ] = Double.parseDouble( l[ d ] );
				
				// x and y are mixed in Matlab
				final double tmp = v[ 1 ];
				v[ 1 ] = v[ 0 ];
				v[ 0 ] = tmp;
				
				// adjust for different pixel offsets in tim's code
				v[ 0 ] -= 0.5;
				v[ 1 ] -= 0.5;
				if ( v.length > 2 )
					v[ 2 ] -= 1.0;
				
				//System.out.println( Util.printCoordinates( v ) );
				
				values.add( v );
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return null;
		}
		
		return values;
	}
}
