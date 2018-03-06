package process.radialsymmetry.cluster;

import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

import ij.io.FileSaver;

public class IOFunctions {
	public static void saveResult(Img <FloatType> img, String path) {
		// saving part
		FileSaver fs = new FileSaver(ImageJFunctions.wrap(img, ""));
		fs.saveAsTiff(path);
	} 
}
