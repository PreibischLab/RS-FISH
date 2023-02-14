/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 Developers.
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
package cmd;

import java.util.concurrent.Callable;

import gui.Anisotropy_Plugin;
import ij.ImageJ;
import ij.ImagePlus;
import net.imagej.patcher.LegacyInjector;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class Anisotropy implements Callable<Void> {

	// input file
	@Option(names = {"-i", "--image"}, required = true, description = "input image or N5 container path (requires additional -d for N5), e.g. -i /home/smFish.tif or /home/smFish.n5")
	private String image = null;

	@Option(names = {"-d", "--dataset"}, required = false, description = "if you selected an N5 path, you need to define the dataset within the N5, e.g. -d 'embryo_5_ch0/c0/s0'")
	private String dataset = null;

	@Override
	public Void call() throws Exception
	{
		final ImagePlus imp = RadialSymmetry.open( image, dataset );

		if ( imp == null )
		{
			System.out.println( "Could not open file: " + image  + " (if N5, dataset=" + dataset + ")");
			return null;
		}

		new ImageJ();

		if ( imp.getStackSize() > 1 )
			imp.setSlice( imp.getStackSize() / 2 );

		imp.resetDisplayRange();
		imp.show();

		new Anisotropy_Plugin().run( null );

		return null;
	}

	public static final void main(final String... args) {
		new CommandLine( new Anisotropy() ).execute( args );
	}
}
