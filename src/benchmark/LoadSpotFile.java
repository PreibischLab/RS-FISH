package benchmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class LoadSpotFile 
{
	public static ArrayList< GroundTruthPoint > loadSpots( final File file )
	{
		final BufferedReader in = TextFileAccess.openFileRead( file );
		
		int n = -1;
		
		if ( in == null )
			return null;
		
		final ArrayList< GroundTruthPoint > values = new ArrayList< GroundTruthPoint >();
		
		try 
		{
			while ( in.ready() )
			{
				final String[] l = in.readLine().trim().split( "\\p{javaWhitespace}+" ); // n-times space

				if ( n == -1 )
				{
					// last entry is the intensity
					n = l.length - 1;
				}
				
				final double[] v = new double[ n ];
				
				for ( int d = 0; d < n; ++d )
					v[ d ] = Double.parseDouble( l[ d ] );
				
				// x and y are mixed in Matlab
				final double tmp = v[ 1 ];
				v[ 1 ] = v[ 0 ];
				v[ 0 ] = tmp;
				
				values.add( new GroundTruthPoint( v ) );
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
