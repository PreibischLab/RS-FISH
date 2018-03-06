package process.radialsymmetry.cluster;

import java.io.File;
import java.util.ArrayList;

import gui.interactive.HelperFunctions;
import ij.ImageJ;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import parameters.GUIParams;
import util.ImgLib2Util;
import util.MedianFilter;

public class Preprocess {
	// String path - path to the folder with the images
	// String ext - image extension
	public static ArrayList<File> readFolder(File folder, String ext) {
		ArrayList<File> images= new ArrayList<>();
		System.out.println("Grab images from " + folder.getAbsolutePath());		
		for (File file : folder.listFiles())
			// if the file is not hidden and ends with .tif we take it 
			if (file.isFile() && !file.getName().startsWith(".") && file.getName().endsWith(ext))
				images.add(file);
		return images;
	}

	public static void runPreprocess(File iFolder, File oFolder) {
		// set the parameters that we define manually first
		boolean useRANSAC = true;
		final GUIParams params = new GUIParams();
		// apparently the best value for now
		params.setAnisotropyCoefficient(1.09f);
		params.setRANSAC(useRANSAC);
		// pre-detection
		params.setSigmaDog(1.50f);
		params.setThresholdDog(0.0083f);
		// detection
		params.setSupportRadius(3);
		params.setInlierRatio(0.37f);
		params.setMaxError(0.5034f);
		
		// grab all file path to the images in the folder
		ArrayList<File> paths = readFolder(iFolder, ".tif");
		for (File imgPath : paths) {
			System.out.println(imgPath.getName());
			// File outputPath = new File(imgPath.getAbsolutePath().substring(0, imgPath.getAbsolutePath().length() - 4));
			
			File outputPath = new File(oFolder.getAbsolutePath() + "/" + imgPath.getName()); 
			// System.out.println(outputPath.getAbsolutePath());
			preprocessImage(imgPath, params, outputPath);
		}
		new ImageJ();
		// iterate over each image in the folder
	}

	public static void preprocessImage(File imgPath, GUIParams params, File outputPath){
		Img<FloatType> img = ImgLib2Util.openAs32Bit(imgPath.getAbsoluteFile());
		Img<FloatType> bg = new ArrayImgFactory<FloatType>().create(img, new FloatType()); 		

		// 1. get the background
		int [] kernelDim = new int []{21, 21}; 
		MedianFilter.medianFilterSliced(img, bg, kernelDim);

		// 2. subtract the background
		HelperFunctions.subtractImg(img, bg);

		// 3. run the validation step here 
		boolean isValid = true;
		if (!isValid) return;

		// 4. normalize image 
		float min = 0;
		float max = 1;
		Normalize.normalize(img, new FloatType(min), new FloatType(max));
		
		// 4.* just resave the images at the moment
		IOFunctions.saveResult(img, outputPath.getAbsolutePath());

		// perform the processing steps from above
		// 5. run radial symmetry 
		// 6. filter the spot and save them 
		// BatchProcess.runProcess(img, params, outputPath);
	}

	public static void main(String [] args){
		File folder = new File("");
		// runPreprocess(folder);
	}

}
