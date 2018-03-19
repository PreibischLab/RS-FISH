package process.radialsymmetry.cluster;

import java.io.File;
import java.util.ArrayList;

import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

import ij.ImagePlus;
import ij.io.FileSaver;

public class IOFunctions {
	public static void saveResult(Img <FloatType> img, String path) {
		// saving part
		FileSaver fs = new FileSaver(ImageJFunctions.wrap(img, ""));
		fs.saveAsTiff(path);
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
