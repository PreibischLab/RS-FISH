package cluster.radial.symmetry.process;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;

import cluster.radial.symmetry.process.parameters.ParametersFirstRun;
import radial.symmetry.parameters.GUIParams;
import radial.symmetry.utils.IOUtils;
import util.NotSoUsefulOutput;

public class RunBatchProcess {
	// to support old code
	public static void runProcess(File pathImagesMedian, File pathImagesRoi, File pathDatabase, File pathZcorrected,
			File pathResultCsv, boolean doZcorrection) {
		runProcess(pathImagesMedian, pathImagesRoi, pathDatabase, pathZcorrected, null, null, pathResultCsv,
				doZcorrection);
	}

	public static void runProcessImage(File pathMedian, File pathMask, File pathZcorrected,
			File pathResultCsvBeforeCorrection, File pathParameters, File pathResultCsv, boolean doZcorrection,
			int waveLength) {
		String classname = Preprocess.class.getSimpleName();

		if (pathMedian.exists()) {
			// set the params according to the way length
			// TODO: Arethe parameters different for SEA12 and N2?
			GUIParams params = ParametersFirstRun.getSEA12ParametersFirstRun(waveLength);
			BatchProcess.process(pathMedian, pathMask, params, pathResultCsvBeforeCorrection, pathParameters,
					pathZcorrected, pathResultCsv, doZcorrection);
		} else {
			System.out.println(NotSoUsefulOutput.toComplaintString(classname, pathMedian.getAbsolutePath()));
		}
	}

	public static void runProcess(File pathImagesMedian, File pathImagesRoi, File pathDatabase, File pathZcorrected,
			File pathResultCsvBeforeCorrection, File pathParameters, File pathResultCsv, boolean doZcorrection) {
		// parse the db with smFish labels and good looking images
		ArrayList<ImageData> imageData = IOUtils.readDb(pathDatabase);

		long currentIndex = 0;
		String extImg = ".tif";
		String extOut = ".csv";
		String classname = Preprocess.class.getSimpleName();

		for (ImageData imageD : imageData) {
			currentIndex++;
			// path to the processed image
			File inputImagePath = Paths.get(pathImagesMedian.getAbsolutePath(), imageD.getFilename() + extImg).toFile();
			File inputRoiPath = Paths.get(pathImagesRoi.getAbsolutePath(), imageD.getFilename().substring(3) + extImg)
					.toFile();

			System.out.println(NotSoUsefulOutput.toProgressString(currentIndex, imageData.size(),
					inputImagePath.getAbsolutePath()));

			if (inputImagePath.exists()) {
				// path to the image
				File outputPathZCorrected = new File("");
				if (pathZcorrected != null)
					outputPathZCorrected = Paths.get(pathZcorrected.getAbsolutePath(), imageD.getFilename() + extImg)
							.toFile();
				// table with all processing parameters
				File outputPathParameters = new File("");
				if (pathParameters != null)
					outputPathParameters = Paths.get(pathParameters.getAbsolutePath(), imageD.getFilename() + extOut)
							.toFile();
				// table to store the results before we perform the z-correction
				File outputPathResultCsvBeforeCorrection = new File("");
				if (pathResultCsvBeforeCorrection != null)
					outputPathResultCsvBeforeCorrection = Paths
							.get(pathResultCsvBeforeCorrection.getAbsolutePath(), imageD.getFilename() + extOut)
							.toFile();
				// table to store the results for each channel
				File outputPathCsv = Paths.get(pathResultCsv.getAbsolutePath(), imageD.getFilename() + extOut).toFile();
				// set the params according to the way length
				// FIXME: SET THE PARAMETERS PROPERLY? ARGUMENT MAYBE?
				GUIParams params = ParametersFirstRun.getSEA12ParametersFirstRun(imageD.getLambda());

				BatchProcess.process(inputImagePath, inputRoiPath, params, outputPathResultCsvBeforeCorrection,
						outputPathParameters, outputPathZCorrected, outputPathCsv, doZcorrection);
			} else {
				System.out.println(NotSoUsefulOutput.toComplaintString(classname, inputImagePath.getAbsolutePath()));
			}
		}
	}
}
