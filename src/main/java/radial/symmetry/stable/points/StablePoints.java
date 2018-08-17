package radial.symmetry.stable.points;

import java.io.File;
import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.KDTree;
import net.imglib2.KDTree.KDTreeCursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.neighborsearch.KNearestNeighborSearchOnKDTree;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import fitting.Spot;
import gradient.computation.Gradient;
import gradient.computation.GradientPreCompute;
import gradient.normalized.computation.NormalizedGradient;
import ij.ImageJ;
import util.ImgLib2Util;

public class StablePoints {


	public static void startRS(RandomAccessibleInterval<FloatType> img, int supportRadius, int thresholdValue){		
		Cursor <FloatType> cursor = Views.iterable(img).cursor();

		// known ground truth
		double [] realPosition = new double[]{50, 50, 50};
		// error in pixels 
		double pixelError = 0.5;
		
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
			range[d] = supportRadius;

		ArrayList<Spot> spots = Spot.extractSpots(img, simplifiedPeaks, derivative, ng, range);
		System.out.println("# of spots: " + spots.size());
		// stores the coordinates of the spots that didn't throw the exception 
		ArrayList<Spot> goodSpots = new ArrayList<>(); 

		for (final Spot spot : spots){
			try{
				// real coordinate: 50 50 50
				Spot.fitCandidates(spot);			
				double [] position = spot.getCenter();
				if (Spot.length(new double[]{position[0] - realPosition[0], position[1] - realPosition[1], position[2] - realPosition[2]}) < 0.5){
					goodSpots.add(spot);
				}				
			}
			catch(Exception e){
				// System.out.println("EXCEPTION CAUGHT");
			}
		}	
		
		
		KDTree<Spot> tree = new KDTree<>(goodSpots, goodSpots);
		KDTreeCursor kdtCursor = tree.cursor(); // FIXME: is this an error here 
		
		while(kdtCursor.hasNext()){
			kdtCursor.fwd();
				
			KNearestNeighborSearchOnKDTree<Spot> knnSearch = new KNearestNeighborSearchOnKDTree<>(tree, 2);
			// knnSearch.
			//K
			System.out.println(knnSearch.getDistance(0));
			if (knnSearch.getDistance(0) < pixelError){ // this one should be smaller than some given threshold, half of the pixel?
				
			}

			
//			double [] position = ((Spot)kdtCursor.get()).getCenter(); // FIXME: is this cast necessary
//			System.out.print(Spot.length(new double[]{position[0] - realPosition[0], position[1] - realPosition[1], position[2] - realPosition[2]}) + ": ");
//			for (int d = 0; d < numDimensions; d++)
//				System.out.print(position[d] + " ");
//			System.out.println();
		}
		
		
		// KNearestNeighborSearch<Spot> search=new KNearestNeighborSearchOnKDTree<Spot>(new KDTree<Spot>(goodSpots, goodSpots),Math.min(20,(int)goodSpots.size()));
		

		// find which goodSpots belong to which actual spot (N:M mapping)
		// M spots, with N regions(i.e. goodSpots) supporting it 
		// where are all the M spots? (given the allowed error of e.g. 0.5 and min # of e.g. 5)
		for (final Spot goodSpot : goodSpots){
			double [] position = goodSpot.getCenter();
//			System.out.print(Spot.length(new double[]{position[0] - realPosition[0], position[1] - realPosition[1], position[2] - realPosition[2]}) + ": ");
//			for (int d = 0; d < numDimensions; d++)
//				System.out.print(position[d] + " ");
//			System.out.println();

		}
		
		// KD TREE for all good spots 
		// for each good spot 
		// - NN search
		// - gives the cluster 
		// - (clusters == # of good spits )
		
		// sort clusters in dec order
		// iterate big to small
		// remove the repetitions from the smaller clusters
		// resort (don't sort just take the largest one )
		
		
		// new Detections(img, goodSpots).showDetections();

	}


	public static void main(String[] args) {

		new ImageJ();
		File file = new File("src/main/resources/one-spot.tif"); 
		Img<FloatType> img = ImgLib2Util.openAs32Bit(file);

		ImageJFunctions.show(img);

		int supportRadius = 3; 
		int thresholdValue = 3;

		startRS(img, supportRadius, thresholdValue);

		System.out.println("DOGE!");
	}

}
