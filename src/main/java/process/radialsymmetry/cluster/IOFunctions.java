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
import ij.ImagePlus;
import ij.io.FileSaver;

public class IOFunctions {
	public static void saveResult(Img <FloatType> img, String path) {
		// saving part
		IJ.saveAsTiff(ImageJFunctions.wrap(img, "").duplicate(), path);
	}
	
	public static void saveResultXyz(Img <FloatType> img, String path) {
		// saving part
		ImagePlus imp = ImageJFunctions.wrap(img, "");
		imp.setDimensions(1, imp.getNSlices(), 1);
		FileSaver fs = new FileSaver(imp);
		fs.saveAsTiff(path);
	} 
	
	public static void saveResult(ImagePlus imp, String path) {
		// saving part
		FileSaver fs = new FileSaver(imp);
		fs.saveAsTiff(path);
	}
	
	public static ArrayList<File> readFolder(File folder, String ext) {
		ArrayList<File> images = new ArrayList<>();
		System.out.println("Grab images from " + folder.getAbsolutePath());
		for (File file : folder.listFiles())
			// if the file is not hidden and ends with .tif we take it 
			if (file.isFile() && !file.getName().startsWith(".") && file.getName().endsWith(ext))
				images.add(file);
		return images;
	}
	
}
