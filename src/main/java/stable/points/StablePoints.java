package stable.points;

import java.io.File;
import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import background.NormalizedGradient;
import background.NormalizedGradientAverage;
import background.NormalizedGradientMedian;
import background.NormalizedGradientRANSAC;
import fit.Spot;
import fit.Center.CenterMethod;
import gradient.Gradient;
import gradient.GradientPreCompute;
import gui.Radial_Symmetry;
import gui.interactive.HelperFunctions;
import ij.IJ;
import ij.ImageJ;
import parameters.RadialSymmetryParameters;
import util.ImgLib2Util;

public class StablePoints {


	public static void startRS(RandomAccessibleInterval<FloatType> img, int supportRadius, int thresholdValue){		
		Cursor <FloatType> cursor = Views.iterable(img).cursor();
		
		final ArrayList<long[]> simplifiedPeaks = new ArrayList<>(0);
		int numDimensions = img.numDimensions();
		
		// set the initital points
		while (cursor.hasNext()){
			cursor.fwd();
			long [] position = new long[numDimensions]; // should be created inside of the loop, stupid java thing			
			cursor.localize(position);
			simplifiedPeaks.add(position);
		}
		
		//simplifiedPeaks.add(new long[]{50,50,50});
		
		System.out.println("# of peaks: " + simplifiedPeaks.size());
		Gradient derivative = new GradientPreCompute(img);	
		
		// ImageJFunctions.show(new GradientPreCompute(img).preCompute(img));
		
		final NormalizedGradient ng = null;
		
		final long[] range = new long[numDimensions];
		for (int d = 0; d < numDimensions; ++d)
			range[d] = 2*supportRadius;
		
		ArrayList<Spot> spots = Spot.extractSpots(img, simplifiedPeaks, derivative, ng, range);
		System.out.println("# of spots: " + spots.size());
		
		for (final Spot spot : spots){
			try{
				// 50 50 50
				// System.out.println("GOOD!");
				Spot.fitCandidates(spot);
				
//				for (int d = 0; d < numDimensions; d++)
//					System.out.print(spot.getLongPosition(d) + " ");
//				System.out.println();
//								
//				if (spot.getLongPosition(0) == 50 &&
//					spot.getLongPosition(1) == 50 &&
//					spot.getLongPosition(2) == 50)
//						System.out.println("PING!");
				
				System.out.println("GOOD!");
			}
			catch(Exception e){
				// System.out.println("EXCEPTION CAUGHT");
			}
		}	
		
		// for 
	}

	
	public static void main(String[] args) {

		new ImageJ();
		File file = new File("/Users/kkolyva/Desktop/untitled folder/gauss3d-1,2,3.tif"); 
		Img<FloatType> img = ImgLib2Util.openAs32Bit(file);

		ImageJFunctions.show(img);
		
		int supportRadius = 3; 
		int thresholdValue = 3;

		startRS(img, supportRadius, thresholdValue);

		System.out.println("DOGE!");
	}

}
