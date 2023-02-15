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
package test;

import java.io.File;
import java.net.URISyntaxException;

import fitting.OrientedPoint;
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
