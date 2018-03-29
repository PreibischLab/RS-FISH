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

	// here process only one image
	public static void reshapeImage(File imgPath, File outputPath) {
		System.out.println(imgPath.getName());
		// open the image
		Img<FloatType> img = ImgLib2Util.openAs32Bit(imgPath.getAbsoluteFile());
		// save the image at the same location but with the roi on it
		ImagePlus imp = ImageJFunctions.wrap(img, "");
		imp.setDimensions(1, imp.getNSlices(), 1);

		// TODO: uncomment when the code is working to have proper saving 
		IOFunctions.saveResult(imp, outputPath.getAbsolutePath());
	}
	
	public static void addRoi(File imgPath, ArrayList<File> roiPaths, File oFolder) {
		String imageName = imgPath.getName().substring(3);
		for (File imageRoi : roiPaths) {
			if (imageRoi.getName().equalsIgnoreCase(imageName)){
				System.out.println("fixed: " + imgPath.getName());
				// open both images
				ImagePlus impNoRoi = IJ.openImage(imgPath.getAbsolutePath());
				ImagePlus impRoi = IJ.openImage(imageRoi.getAbsolutePath());
				// grab and copy roi
				Roi roi = impRoi.getRoi();
				impNoRoi.setRoi(roi);
				
				if (debug) {
					impRoi.show();
					impNoRoi.show();
				}
				
				IJ.save(impNoRoi, oFolder.getAbsolutePath() + "/" + imgPath.getName());
				
				break; // iterate till the first hit 
			}
		}
	}

	public static void runFixImages(File iFolder, File roiFolder, File oFolder) {
		ArrayList<File> paths = IOFunctions.readFolder(iFolder, ".tif");
		ArrayList<File> roiPaths = IOFunctions.readFolder(roiFolder, ".tif");
		for (File imgPath : paths) {
			File outputPath = new File(imgPath.getAbsolutePath().substring(0, imgPath.getAbsolutePath().length() - 4));
			reshapeImage(imgPath, new File(outputPath.getAbsoluteFile() + ".tif"));
			addRoi(imgPath, roiPaths, oFolder);
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
