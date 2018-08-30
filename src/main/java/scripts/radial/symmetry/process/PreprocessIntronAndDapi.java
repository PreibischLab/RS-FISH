package scripts.radial.symmetry.process;

import java.io.File;

import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

import cluster.radial.symmetry.process.Preprocess;
import ij.IJ;
import ij.ImagePlus;
import util.ImgLib2Util;

public class PreprocessIntronAndDapi {

	public static void normalizeAndSave(File anotherChannelImagePath, File maskImagePath, File normalizedAnotherChannelImagePath) {
		Img<FloatType> img = ImgLib2Util.openAs32Bit(anotherChannelImagePath);
		Img<FloatType> mask = ImgLib2Util.openAs32Bit(maskImagePath);
		
		
		float [] minmax = Preprocess.getMinmax(img, mask);
		// IMP: decided to take min as 0
		minmax[0] = 0; //
		// last 2 values are irrelevant
		img = Preprocess.normalize(img, minmax[0], minmax[1], 0, 1);

		try {
//			FIXME: some legacy issue; can't use this code to save the images
//			ImgSaver saver = new ImgSaver();
//			saver.saveImg(normalizedAnotherChannelImagePath.getAbsolutePath(), img);
			
			ImagePlus imp = ImageJFunctions.wrap(img, "");
			imp.setDimensions(1, imp.getStackSize(), 1);
			IJ.saveAsTiff(imp, normalizedAnotherChannelImagePath.getAbsolutePath() );
		}
		catch (Exception exc) {
			exc.printStackTrace();
		}
		System.out.println("Saving done!");
	}
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
