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

import java.io.File;

import ij.ImageJ;
import ij.ImagePlus;
import ij.io.Opener;


public class RadialSymmetryTests {
	public static void testRSGaussianFit(){
		File path = new File("/media/milkyklim/Samsung_T3/2017-06-26-radial-symmetry-test/Simulated_3D.tif");

		if (!path.exists())
			throw new RuntimeException("'" + path.getAbsolutePath() + "' doesn't exist.");

		new ImageJ();
		System.out.println("Opening '" + path + "'");

		ImagePlus imp = new Opener().openImage(path.getAbsolutePath());

		if (imp == null)
			throw new RuntimeException("image was not loaded");

		imp.show();
		imp.setSlice(121);

		// new Radial_Symmetry().run(new String());
	}
	
	
	public static void main(String[] args){
		testRSGaussianFit();
		System.out.println("Doge!");
	}
	
}


