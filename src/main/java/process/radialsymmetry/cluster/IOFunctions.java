package process.radialsymmetry.cluster;

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
}
