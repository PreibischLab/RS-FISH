/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2024 RS-FISH developers.
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
