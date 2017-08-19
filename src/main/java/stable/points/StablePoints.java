package stable.points;

import java.io.File;
import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import background.NormalizedGradient;
import fit.Spot;
import gradient.Gradient;
import gradient.GradientPreCompute;
import ij.ImageJ;
import util.ImgLib2Util;

public class StablePoints {


	public static void startRS(RandomAccessibleInterval<FloatType> img, int supportRadius, int thresholdValue){		
		Cursor <FloatType> cursor = Views.iterable(img).cursor();
		
		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(0);
		int numDimensions = img.numDimensions();
		
		// set the initital points
		while (cursor.hasNext()){
			cursor.fwd();
			// should be created inside of the loop, stupid java thing	
			long [] position = new long[numDimensions]; 		
			cursor.localize(position);
			simplifiedPeaks.add(position);
		}
		
		System.out.println("# of peaks: " + simplifiedPeaks.size());
		Gradient derivative = new GradientPreCompute(img);	
		// do not normalize the gradient 
		final NormalizedGradient ng = null;
		
		final long[] range = new long[numDimensions];
		for (int d = 0; d < numDimensions; ++d)
			range[d] = 2*supportRadius;
		
		ArrayList<Spot> spots = Spot.extractSpots(img, simplifiedPeaks, derivative, ng, range);
		System.out.println("# of spots: " + spots.size());
		// stores the coordinates of the spots that didn't throw the exception 
		ArrayList<double[]> goodSpots = new ArrayList<>(); 
		
		for (final Spot spot : spots){
			try{
				// real coordinate: 50 50 50
				Spot.fitCandidates(spot);				
				goodSpots.add(spot.getCenter());
			}
			catch(Exception e){
				// System.out.println("EXCEPTION CAUGHT");
			}
		}	
		
		for (final double[] goodSpot : goodSpots){
			if (Spot.length(new double[]{goodSpot[0] - 50, goodSpot[1] - 50, goodSpot[2] - 50}) < 0.5){				
				System.out.print(Spot.length(new double[]{goodSpot[0] - 50, goodSpot[1] - 50, goodSpot[2] - 50}) + ": ");
				for (int d = 0; d < numDimensions; d++)
					System.out.print(goodSpot[d] + " ");
				System.out.println();
			}
		}
		
		
	}

	
	public static void main(String[] args) {

		new ImageJ();
		File file = new File("/Users/kkolyva/Desktop/untitled folder/one-spot.tif"); 
		Img<FloatType> img = ImgLib2Util.openAs32Bit(file);

		ImageJFunctions.show(img);
		
		int supportRadius = 3; 
		int thresholdValue = 3;

		startRS(img, supportRadius, thresholdValue);

		System.out.println("DOGE!");
	}

}
