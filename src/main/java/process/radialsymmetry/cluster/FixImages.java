/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2025 RS-FISH developers.
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
package process.radialsymmetry.cluster;

import java.io.File;
import java.util.ArrayList;

import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import util.ImgLib2Util;

public class FixImages {
	// class to fix the number of slices in the stacks 
	// and add the roi that are gone because of channel
	// splitting 
	
	private static boolean debug = false;

	public static void reshapeAndAddRoi(File pathImage, ArrayList<File> pathImagesRoi, File pathImageFixed) {
		
		if (debug)
			System.out.println(pathImage.getName());
		
		String imageName = pathImage.getName().substring(3);
		for (File imageRoi : pathImagesRoi) {
			if (imageRoi.getName().equalsIgnoreCase(imageName)){
				System.out.println("fixed: " + pathImage.getName());
				// open both images
				ImagePlus impNoRoi = IJ.openImage(pathImage.getAbsolutePath());
				ImagePlus impRoi = IJ.openImage(imageRoi.getAbsolutePath());
				
				// reslice!
				impNoRoi.setDimensions(1, impNoRoi.getNSlices(), 1);
				
				// grab and copy roi
				Roi roi = impRoi.getRoi();
				impNoRoi.setRoi(roi);
				
				if (debug) {
					impRoi.show();
					impNoRoi.show();
				}
				IJ.save(impNoRoi, pathImageFixed.getAbsolutePath() + "/" + pathImage.getName());
				break; // iterate till the first hit 
			}
		}
	}
	
	public static void runFixImages(File iFolder, File roiFolder, File oFolder) {
		ArrayList<File> paths = IOFunctions.readFolder(iFolder, ".tif");
		ArrayList<File> roiPaths = IOFunctions.readFolder(roiFolder, ".tif");
		for (File imgPath : paths) {
			// File outputPath = new File(imgPath.getAbsolutePath().substring(0, imgPath.getAbsolutePath().length() - 4));
			// reshapeImage(imgPath, new File(outputPath.getAbsoluteFile() + ".tif"));
			// addRoi(imgPath, roiPaths, oFolder);
			
			reshapeAndAddRoi(imgPath, roiPaths, oFolder);
		}
	}

	public static void main(String [] args) {
		// TODO: move this part from here 
		new ImageJ();
		// no roi folder:
		File iFolder = new File("/Volumes/1TB/2018-03-20-laura-radial-symmetry-numbers/SEA-12-channels-correct");
		// roi folder:
		File roiFolder = new File("/Volumes/1TB/2018-03-20-laura-radial-symmetry-numbers/SEA-12");
		// roi folder:
		File oFolder = new File("/Volumes/1TB/2018-03-20-laura-radial-symmetry-numbers/SEA-12-channels-correct");
		
		// ArrayList<File> list = new ArrayList<>();
		// list.add(folderRoi);
		
		
		runFixImages(iFolder, roiFolder, oFolder);
		// addRoi(folderNoRoi, list, new File (""));
		
		System.out.println("DONE!");
	}
}
