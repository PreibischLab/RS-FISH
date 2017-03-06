package test;

import java.io.File;
import java.net.URISyntaxException;

import fit.OrientedPoint;
import mpicbg.models.Point;

public class JAR_Test
{

	public static void main( String[] args )
	{
		try
		{
			String name = new File( Point.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() ).getName().trim();
			
			System.out.println( name );
			System.out.println( Point.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() );
		}
		catch ( final URISyntaxException e )
		{
		}
		
		new OrientedPoint(new double[ 2 ], new double[ 2 ], 5);

		System.exit(0);

	}
}
