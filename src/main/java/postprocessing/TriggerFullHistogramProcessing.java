package postprocessing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;

import fit.Spot;
import gui.interactive.HelperFunctions;


public class TriggerFullHistogramProcessing {
	/*
	 * full pipeline inculdes 1. read the intensities from the csv file 2. bin
	 * the data 2.* smoothen if necessary 3. gauss fit on the histogram 4. save
	 * the params to the file
	 */
	// file with the values to process
	File file;
	// read data from the file
	ArrayList<Float> intensity;
	// won't use this values
	ArrayList<Spot> spots;
	ArrayList<Long> timePoint;
	ArrayList<Long> channelPoint;
	
	private static boolean debug = false;

	public TriggerFullHistogramProcessing() {
		// file is not set by default
		file = null;
		// read data from the file
		intensity = new ArrayList<>();
		// won't use this values
		spots = null;
		timePoint = null;
		channelPoint = null;
	}

	public TriggerFullHistogramProcessing(File file, ArrayList<Float> intensity, ArrayList<Spot> spots, ArrayList<Long> timePoint,
			ArrayList<Long> channelPoint) {
		// set the file 
		this.file = file;
		// read data from the file
		this.intensity = intensity;
		this.spots = spots;
		this.timePoint = timePoint;
		this.channelPoint = channelPoint;
	}

	public double[] processHistogram(File file, ArrayList<Float> intensity, ArrayList<Spot> spots, ArrayList<Long> timePoint,
			ArrayList<Long> channelPoint) {
		readData(file, intensity, spots, timePoint, channelPoint);
		long[] bins = createHistogram(intensity);
		
		if (bins != null){
			Img<FloatType> img = new ArrayImgFactory<FloatType>().create(new int[] { bins.length }, new FloatType());
			HistogramFitting.copyArrayListToImg(bins, img);
			double[] params = HistogramFitting.run(img, false);
			return params;
		}
		return null;
	}

	// extract intensities from the csv file
	public void readData(File file, ArrayList<Float> intensity, ArrayList<Spot> spots, ArrayList<Long> timePoint,
			ArrayList<Long> channelPoint) {
		// TODO: move the file path
		File path = file;
		// File path = new File("/Users/kkolyva/Desktop/05.nd2 - C=2-1.csv");
		HelperFunctions.readCSV(path.getAbsolutePath(), spots, timePoint, channelPoint, intensity);
	}

	// dump the parameters to the txt file, keeping the same tree structure as before 
	public static void writeTxt(double[] params, File file){
		// create all of the missing folders here
		file.getParentFile().mkdirs();
		try(FileWriter fw = new FileWriter(file.getAbsolutePath())){
			// FileWriter fw = new FileWriter(file.getAbsolutePath());
			for (int i = 0; i < params.length; i++)
				fw.write(Double.toString(params[i]) + "\n");
			fw.close();
		} 
		catch(IOException e){
			System.out.println("Problem writting to " + file.getAbsolutePath());
		}
	}

	public static long[] createHistogram(ArrayList<Float> intensity) {
		float[] minmax = getMinMax(intensity);

		if (debug)
			System.out.println(minmax[0] + " " + minmax[1]);
		
		// FIXME: add the check for the case when there is no data in the file
		if (minmax[0] == Float.MAX_VALUE || minmax[1] == -Float.MAX_VALUE)
			return null;
			
		// to have the consistent results we set min to 0
		// and keep the max value as high as the max value
		// in the image; this might be changed to [0,1] for
		// better comparison

		// int numBins = 100;
		int binSize = 100; // also variable
		int numBins = (int) Math.floor(minmax[1] / binSize) + 1;

		// bin the intensities
		long[] bins = binData(intensity, minmax[0], minmax[1], numBins);
		// now we can fit the gausian on the histogram that we get
		return bins;
	}

	public static long[] binData(ArrayList<Float> intensity, float min, float max, int numBins) {
		// avoid the one value that is exactly 100%
		final double size = max - min + 0.000001;

		// bin and count the entries
		final long[] bins = new long[numBins];

		for (final float val : intensity)
			++bins[(int) Math.floor(((val - min) / size) * numBins)];

		return bins;
	}

	// return the min max of the arraylist
	public static float[] getMinMax(ArrayList<Float> intensity) {
		float[] minmax = new float[2];
		minmax[0] = Float.MAX_VALUE;
		minmax[1] = -Float.MAX_VALUE;

		for (float val : intensity) {
			if (val < minmax[0])
				minmax[0] = val;
			if (val > minmax[1])
				minmax[1] = val;
		}

		return minmax;
	}

	// triggers the computations
	public static void runMultiple(){
		String inPath = "/Users/kkolyva/Desktop/results/processed";
		String outPath = "/Users/kkolyva/Desktop/results/gauss";

		String[] folders = new String[]{"MK4_DPY23-ex-int_mdh1_003"}; /*, 
										"N2_DPY23-ex-int_mdh1_003","N2_DPY23-ex-int_mdh1_004",
										"N2_DPY23-ex-int_mdh1_005","N2_DPY23-ex-int_mdh1_006"};*/
		
		for (String folder : folders){
			for (int j = 1; j <=3; j++){
				File inFullPath = new File(inPath + "/" + folder + "/" + "c" + j + "-result");

				for (final File file : inFullPath.listFiles()) {
					if (file.isFile()) {
						if (debug)
							System.out.println("Processing: " + file.getName());
						File outFullPath = new File(outPath + "/" + folder + "/" + "c" + j + "-result" + "/" + createFileName(file.getName()));

						TriggerFullHistogramProcessing tfhp = new TriggerFullHistogramProcessing();

						double[] params = tfhp.processHistogram(file.getAbsoluteFile(), tfhp.intensity, tfhp.spots, tfhp.timePoint, tfhp.channelPoint);
						if (params != null)
							writeTxt(params, outFullPath);
					}
				}
			}
		}
	}

	// create the 
	public static String createFileName(String fileName){
		return fileName.substring(0, fileName.length() - 3) + "txt";
	}

	public static void main(String[] args) {
		TriggerFullHistogramProcessing tfhp = new TriggerFullHistogramProcessing();
		// tfhp.processHistogram(tfhp.file, tfhp.intensity, tfhp.spots, tfhp.timePoint, tfhp.channelPoint);
		runMultiple();

		System.out.println("Doge!");
	}

}
