package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import benchmark.TextFileAccess;

public class ParseEASIFISH
{

	public static void main( String[] args )
	{
		String directory = "/Users/spreibi/Documents/BIMSB/Publications/radialsymmetry/EASI-FISH/R7_LHA5/c0/logs";

		final long[] sum = new long[ 1 ];

		Arrays.asList( 
				new File( directory ).list(
						(dir, name) -> name.endsWith( "_c0.o" ) ) ).forEach(
								file -> {
									System.out.println( file );
									
									try {
										BufferedReader in = TextFileAccess.openFileRead( new File( directory, file ) );
										String line;
										while ( ( line = in.readLine() ) != null )
										{
											if ( line.contains( "CPU time" ) )
											{
												while ( line.contains( " ") )
													line = line.replace( " ", "" );
												line = line.substring( line.indexOf( ":" ) + 1, line.indexOf( "." ) );
												System.out.println(line);
												sum[ 0 ] += Long.parseLong( line );
											}
										}
										in.close();
									}
									catch (IOException e) {
										e.printStackTrace();
										System.exit( 0 );
									}
								} );

		System.out.println( "sum = " + sum[ 0 ] );
	}
}
