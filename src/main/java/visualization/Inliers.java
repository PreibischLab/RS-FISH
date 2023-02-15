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
package visualization;

import java.util.ArrayList;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import fitting.Spot;
import gui.interactive.HelperFunctions;
import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;

public class Inliers {

	public Inliers() {

	}

	public static void showInliers(ImagePlus imagePlus, ArrayList<Spot> spots, ArrayList<Long> timePoint) {
		if (imagePlus.getNChannels() != 1)
			System.out.println("only 1 channel images are supported");

		final boolean encodeErrors = false;
		// ugly but this is the way at the moment
		int tIdx = HelperFunctions.getTidx(imagePlus);
		long[] fullDimensions = HelperFunctions.getAllDimensions(imagePlus);
		Img<FloatType> imgInliers = new ArrayImgFactory<FloatType>().create(fullDimensions, new FloatType());

		int fromSpot = 0;
		int toSpot = -1;

		for (int t = 0; t < imagePlus.getNFrames(); ++t) {
			fromSpot = (toSpot + 1);
			toSpot += timePoint.get(t);
			// draw current slice
			Spot.drawRANSACArea(spots.subList(fromSpot, toSpot + 1), 
					tIdx != -1 ? Views.dropSingletonDimensions(Views.hyperSlice(imgInliers, tIdx, t)) : imgInliers, encodeErrors);
		}

		final ImagePlus impInliers = ImageJFunctions.wrap(imgInliers, "inliers").duplicate();
		final ImageStack stack = new ImageStack(imagePlus.getWidth(), imagePlus.getHeight());
		
		for (int i = 0; i < imagePlus.getStackSize(); ++i) {
			stack.addSlice(imagePlus.getStack().getProcessor(i + 1));
			stack.addSlice(impInliers.getStack().getProcessor(i + 1));
		}
		ImagePlus merged = new ImagePlus("Initial spots and RANSAC inliers", stack);
		merged.setDimensions(2, imagePlus.getNSlices(), imagePlus.getNFrames());

		CompositeImage ci = new CompositeImage(merged);
		ci.setDisplayMode(CompositeImage.COMPOSITE);
		ci.show();
	}

	public static void main(String[] args) {

	}

}
