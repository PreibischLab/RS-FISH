package cluster.radial.symmetry.process;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;

import radial.symmetry.utils.IOUtils;
import util.NotSoUsefulOutput;

public class RunStepsPreprocess {
	public static void runFirstStepPreprocess(File pathImages, File pathDb, File pathMasks, File pathImagesMedian) {
		// parse the db with smFish labels and good looking images
		ArrayList<ImageData> imageData = IOUtils.readDb(pathDb);
		// to see the feedback
		long currentIdx = 0;
		String ext = ".tif";
		String classname = Preprocess.class.getSimpleName();
		
		for (ImageData imageD : imageData) {
			currentIdx++;
			// unprocessed path
			File inputImageFile = Paths.get(pathImages.getAbsolutePath(), imageD.getFilename() + ext).toFile();
			// processed path 
			File outputImageFile = Paths.get(pathImagesMedian.getAbsolutePath(), imageD.getFilename() + ext).toFile();
			// mask path 
			File maskFile = Paths.get(pathMasks.getAbsolutePath(), imageD.getFilename().substring(3) + ext).toFile();
			System.out.println(NotSoUsefulOutput.toProgressString(currentIdx, imageData.size(), inputImageFile.getAbsolutePath()));
			// check that the corresponding files is not missing
			if (inputImageFile.exists() && maskFile.exists()) {
				Preprocess.firstStepPreprocess(inputImageFile, maskFile, outputImageFile);
			}
			else {
				System.out.println(NotSoUsefulOutput.toComplaintString(classname, inputImageFile.getAbsolutePath()));
			}
		}
	}
	
	public static void runSecondStepPreprocess(File pathImages, File pathDb, File pathMasks, File pathCenters, File pathImagesMedian) {
		// parse the db with smFish labels and good looking images
		ArrayList<ImageData> imageData = IOUtils.readDb(pathDb);
		// grab the values of the centers
		ArrayList<ImageData> centers = IOUtils.readCenters(pathCenters);

		// to see the feedback
		long currentIdx = 0;
		String ext = ".tif";
		String classname = Preprocess.class.getSimpleName();
		
		for (ImageData imageD : imageData) {
			currentIdx++;
			// unprocessed path
			File inputImageFile = Paths.get(pathImages.getAbsolutePath(), imageD.getFilename() + ext).toFile();
			// processed path 
			File outputImageFile = Paths.get(pathImagesMedian.getAbsolutePath(), imageD.getFilename() + ext).toFile();
			// mask path 
			File maskFile = Paths.get(pathMasks.getAbsolutePath(), imageD.getFilename().substring(3) + ext).toFile();
			// peak center value
			float center = Preprocess.getCenter(centers, imageD.getFilename());
			System.out.println(NotSoUsefulOutput.toProgressString(currentIdx, imageData.size(), inputImageFile.getAbsolutePath()));

			// check that the corresponding files is not missing
			if (inputImageFile.exists() && maskFile.exists()) {
				Preprocess.secondStepPreprocess(inputImageFile, maskFile, outputImageFile, center);
			}
			else {
				System.out.println(NotSoUsefulOutput.toComplaintString(classname, inputImageFile.getAbsolutePath()));
			}
		}
	}
}
