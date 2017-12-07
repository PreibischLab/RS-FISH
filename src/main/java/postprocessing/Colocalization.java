package postprocessing;

import java.util.ArrayList;

import net.imglib2.KDTree;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import fit.Spot;

public class Colocalization {
	// functions in this class check that intronic and exonic probs are
	// co-localized
	// return the pair of corresponding indices
	public static ArrayList<int[]> findColocalization(Img<UnsignedByteType> img, ArrayList<Spot> ex, ArrayList<Spot> in,
			float error) {
		// contains corresponding exonic-intronic pair indices
		ArrayList<int[]> overlapingIndices = new ArrayList<>();
		// construct the KDTree on the intronic spots
		KDTree<Spot> inTree = new KDTree<>(in, in);
		// define the search on the intronic probes
		NearestNeighborSearchOnKDTree<Spot> search = new NearestNeighborSearchOnKDTree<>(inTree);

		int idx = 0; // store the corresponding exonic index
		for (final Spot spot : ex) { // iterate through the exonic spots
			// check that the exon signal is inside of the nuclei
			// and has the corresponding intronic signal
			if (isInNuclei(img, spot) && isNextIntron(search, spot, error)) {
				int[] indices = new int[2]; // ex-in
				indices[0] = idx; // ex index
				indices[1] = in.indexOf(search.getSampler().get()); // in index

				overlapingIndices.add(indices);
			}
			idx++;
		}
		return overlapingIndices;
	}

	// check if the signal is inside of the nuclei
	public static boolean isInNuclei(Img<UnsignedByteType> img, Spot spot) {
		boolean res = false;

		double[] center = spot.getCenter();
		long[] pos = new long[img.numDimensions()];
		// TODO: agree on some solution here
		for (int d = 0; d < img.numDimensions(); d++)
			pos[d] = (long) (center[d] + 0.5); // instead of rounding

		// check if the corresponding image pixel is not empty
		RandomAccess<UnsignedByteType> ra = img.randomAccess();
		ra.setPosition(pos);

		if (ra.get().get() > 0)
			res = true;

		return res;
	}

	// check if the ex signal overlaps with in signal
	public static boolean isNextIntron(NearestNeighborSearchOnKDTree<Spot> search, Spot exSpot, double error) {
		boolean res = false;
		search.search(exSpot);
		double dist = search.getDistance();
		if (dist < error)
			res = true;
		return res;
	}

	public static void testCase() {
		Img<UnsignedByteType> img = new ArrayImgFactory<UnsignedByteType>().create(new long[] { 64, 64, 64 },
				new UnsignedByteType());
		ArrayList<Spot> ex = new ArrayList<>();
		ArrayList<Spot> in = new ArrayList<>();

		int numDimensions = 3;
		int totalNum = 10;
		for (int j = 0; j < totalNum; j++) {

			final Spot spot = new Spot(numDimensions);
			spot.setOriginalLocation(new long[] { j, j, j });
			ex.add(spot);
			final Spot spot2 = new Spot(numDimensions);
			spot2.setOriginalLocation(new long[] { totalNum - j, totalNum - j, totalNum - j });
			in.add(spot2);

			for (int d = 0; d < numDimensions; d++) {
				ex.get(j).center.setSymmetryCenter(j, d);
				in.get(j).center.setSymmetryCenter(totalNum - j + 0.01, d);
			}
		}

		float error = 0.5f;
		ArrayList<int[]> result = findColocalization(img, ex, in, error);
	}

	public static void main(String[] args) {
		testCase();
		System.out.println("DOGE!");
	}

}
