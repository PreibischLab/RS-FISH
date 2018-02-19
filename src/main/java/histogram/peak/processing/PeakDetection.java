package histogram.peak.processing;

import java.util.ArrayList;

// TODO: REMOVE NEVER USED!
public class PeakDetection {
	// used to detect the peak on the given histogram 
	
	// spots contains the intensity distributions
	public static void findPeak(ArrayList<Float> intensity){
		
	}
	
	public static void createHistogram(ArrayList<Float> intensity){
		float[] minmax = getMinMax(intensity);
		
		// to have the consistent results we set min to 0
		// and keep the max value as high as the max value 
		// in the image; this might be changed to [0,1] for
		// better comparison
		
		// int numBins = 100;
		int binSize = 100; // also variable
		int numBins = (int)Math.floor(minmax[1]/binSize) + 1;
		
		// bin the intensities 
		long [] bins = binData(intensity, minmax[0], minmax[1], numBins); 
		// now we can fit the gausian on the histogram that we get 
	}
	
	// return a float number that corresponds to the peak center
	//
	public static void fitGaussian(long [] bins){
		
	}
	
	public static long[] binData(ArrayList<Float> intensity, float min, float max, int numBins){
		// avoid the one value that is exactly 100%
		final double size = max - min + 0.000001;

		// bin and count the entries
		final long[] bins = new long[ numBins ];

		for ( final float val : intensity )
			++bins[ (int)Math.floor( ( ( val - min ) / size ) * numBins ) ];

		return bins;
	}
	
	// return the min max of the arraylist 
	public static float[] getMinMax(ArrayList<Float> intensity){
		float[] minmax = new float[2];
		minmax[0] = Float.MAX_VALUE;
		minmax[1] = -Float.MAX_VALUE;
		
		for (float val : intensity){
			if (val < minmax[0])
				minmax[0] = val;
			if (val > minmax[1])
				minmax[1] = val;
		}
		
		return minmax;
	}
}
