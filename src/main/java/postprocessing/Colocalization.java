package postprocessing;

import java.util.ArrayList;

import net.imglib2.KDTree;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import fit.Spot;

public class Colocalization {
	// functions in this class check that intronic and exonic probs are co-localized 
	private static boolean debug = true;

	// error defines how far the corresponding points can be from each other!
	public static <T extends RealType<T>> void findColocalization(RandomAccessibleInterval<T> img, ArrayList<Spot> ex, ArrayList<Spot> in, float error){
		// construct the KDTree on the intronic spots
		KDTree<Spot> inTree = new KDTree<>(in, in);
		// define the search on the intronic probes 
		NearestNeighborSearchOnKDTree<Spot> search = new NearestNeighborSearchOnKDTree<>(inTree);

		for (Spot spot : ex){ // iterate through the exonic spots

			if (debug)
				System.out.println("spot: " + spot.getIntPosition(0) + " " + spot.getIntPosition(1));

			// check that the exon signal is inside of the nuclei 
			// and has the corresponding intron signal
			if (isInNuclei(img, spot) && isNextIntron(search, spot, error)){
				// TODO: some type of processing here 
			}
			else{
				// TODO: guess do nothing here 
				System.out.println("NOT IN!");
			}
		}
	}


	// check if the signal is inside of the nuclei
	public static <T extends RealType<T>> boolean isInNuclei(RandomAccessibleInterval<T> img, Spot spot){
		boolean res = false;
		
		double [] center = spot.getCenter();
		long [] pos = new long [img.numDimensions()];
		// TODO: agree on some solution here 
		for (int d = 0; d < img.numDimensions(); d++)
			pos[d] = (long) (center[d] + 0.5 ); // instead of rounding 
		
		// check if the corresponding image pixel is not empty
		RandomAccess<T> ra = img.randomAccess();
		ra.setPosition(pos);
		
		if (ra.get().getRealDouble() > 0)
			res = true;
		
		return res;
	}

	// check if the ex signal overlaps with in signal
	public static boolean isNextIntron(NearestNeighborSearchOnKDTree<Spot> search, Spot exSpot, double error){
		boolean res = false;

		search.search(exSpot);
		double dist = search.getDistance();

		if (dist < error){
			res = true;
			if (debug){
				double [] position = new double[3]; 
				search.getPosition().localize(position);
				System.out.println("Closest point: " + position[0] + " " + position[1]);
			}
		}
		return res;
	}

	public static void main(String [] args){
		Img<FloatType> img = new ArrayImgFactory<FloatType>().create(new long[]{64, 64, 64}, new FloatType());
		ArrayList<Spot> ex = new ArrayList<>();
		ArrayList<Spot> in = new ArrayList<>();

		int numDimensions = 3;
		int totalNum = 10;
		for (int j = 0; j < totalNum; j++){

			final Spot spot = new Spot(numDimensions);
			spot.setOriginalLocation( new long []{j, j, j} );
			ex.add(spot);
			final Spot spot2 = new Spot(numDimensions);
			spot2.setOriginalLocation( new long []{totalNum - j, totalNum - j, totalNum - j} );
			in.add(spot2);

			for (int d = 0; d < numDimensions; d++){
				ex.get(j).center.setSymmetryCenter(j, d);
				in.get(j).center.setSymmetryCenter(totalNum - j + 0.01, d);
			}

		}

		float error = 0.5f;
		findColocalization(img, ex, in, error);

		System.out.println("DOGE!");

	}

}
