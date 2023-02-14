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
package test;

import java.io.File;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import ij.ImageJ;
import ij.ImagePlus;
import ij.io.Opener;

public class TestIjCommands {

	/*
	 * Small test program to see how the wrapping of IJ ImagePlus is working 
	 * */
	public void testIjCommands(ImagePlus imp) {
		RandomAccessibleInterval<FloatType> rai = ImageJFunctions.wrap(imp);
		// x y c z t 
		int[] impDim = imp.getDimensions();
		
		long[] dim; // stores x y z dimensions
		if (impDim[3] == 1) { // if there is no z dimension 
			dim = new long[] { impDim[0], impDim[1] };
		} else { // 3D image
			dim = new long[] { impDim[0], impDim[1], impDim[3] };
		}

		RandomAccessibleInterval<FloatType> img;

		for (int c = 1; c <= imp.getNChannels(); c++) { 
			for (int t = 1; t <= imp.getNFrames(); t++) {		
				img = copyImg(rai, c - 1, t - 1, dim);
				// TODO: process img
			}
		}
	}

	public static RandomAccessibleInterval<FloatType> copyImg(RandomAccessibleInterval<FloatType> rai, int channel, int time, long[] dim) {
		// this one will be returned
		RandomAccessibleInterval<FloatType> img = ArrayImgs.floats(dim);
			
		Cursor<FloatType> cursor = Views.iterable(img).localizingCursor();
		RandomAccess<FloatType> ra = rai.randomAccess();

		long[] pos = new long[dim.length];

		while (cursor.hasNext()) {
			// move over output image
			cursor.fwd(); 
			cursor.localize(pos); 
			// set the corresponding position (channel + time)
			ra.setPosition(new long[]{pos[0], pos[1], channel, pos[2], time});
			cursor.get().setReal(ra.get().getRealFloat());
						
		}
		
		return img;
	}

	public static void main(String[] args) {
		new ImageJ();

		File path = new File("/Users/kkolyva/Desktop/test-image-1-c=1.tif");
		// path = path.concat("test_background.tif");

		if (!path.exists())
			throw new RuntimeException("'" + path.getAbsolutePath() + "' doesn't exist.");

		new ImageJ();
		System.out.println("Opening '" + path + "'");

		ImagePlus imp = new Opener().openImage(path.getAbsolutePath());

		if (imp == null)
			throw new RuntimeException("image was not loaded");

		imp.show();

		new TestIjCommands().testIjCommands(imp);

		System.out.println("Doge!");
	}
}
