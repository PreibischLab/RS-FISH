package postprocessing;

import java.util.ArrayList;
import java.util.Map;

import fit.Spot;
import milkyklim.algorithm.localization.EllipticGaussianOrtho;
import milkyklim.algorithm.localization.LevenbergMarquardtSolver;
import milkyklim.algorithm.localization.MLEllipticGaussianEstimator;
import milkyklim.algorithm.localization.PeakFitter;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class HistogramFitting {

	public static float eps = 1e-7f;

	public static void testRun() {
		int totalBins = 20;
		double sigma = 3;
		double amplitude = 5;
		double shift = 5;

		Img<FloatType> img = new ArrayImgFactory<FloatType>().create(new int[] { totalBins }, new FloatType());
		Cursor<FloatType> cursor = Views.iterable(img).cursor();
		int x = 0;
		while (cursor.hasNext()) {
			cursor.fwd();
			// generate normal distibution
			double val = amplitude * Math.exp(-(x - shift) * (x - shift) / (2 * sigma * sigma));
			// another noisy peak
			val += 0.5 * Math.exp(-(x - 15) * (x - 15) / (2 * 0.5 * sigma * 0.5 * sigma));

			// test:
			// if (x == 0 || x == 2 || x == 1 || x == 19)
			// val = 0;
			cursor.get().set((float) val);
			// check the values
			System.out.println(x + " : " + cursor.get().get());
			x++;
		}

		// smoothen the data if necessary
		if (true)
			smoothenData(img);

		x = 0;
		cursor.reset();
		while (cursor.hasNext()) {
			cursor.fwd();
			System.out.println(x + " : " + cursor.get().get());
			x++;
		}

		runFit(img);
	}

	// smoothen the initial data to fit the gaussian later on
	public static void smoothenData(Img<FloatType> img) {

		Img<FloatType> duplicate = img.factory().create(img, img.firstElement());
		copy(img, duplicate);

		int numDimensions = img.numDimensions();
		int filterSize = 5;
		float[] kernel = new float[filterSize];

		kernel[0] = kernel[filterSize - 1] = 0.05f;
		kernel[1] = kernel[filterSize - 2] = 0.15f;
		kernel[2] = 0.6f;

		Cursor<FloatType> cursor = Views.iterable(duplicate).localizingCursor();
		RandomAccess<FloatType> ra = Views.extendZero(duplicate).randomAccess();
		RandomAccess<FloatType> raImg = img.randomAccess();

		while (cursor.hasNext()) {
			cursor.fwd();
			long[] position = new long[numDimensions];

			cursor.localize(position);
			float totalVal = 0;
			for (int j = -filterSize / 2; j <= filterSize / 2; j++) {
				ra.setPosition(position[0] + j, 0);
				totalVal += kernel[j + filterSize / 2] * ra.get().get();
			}

			// not necessary if the values are weighted
			// totalVal /= filterSize;

			raImg.setPosition(cursor);
			raImg.get().set(totalVal);
		}

	}

	public static <T extends Type<T>> void copy(final RandomAccessible<T> source, final IterableInterval<T> target) {
		// create a cursor that automatically localizes itself on every move
		Cursor<T> targetCursor = target.localizingCursor();
		RandomAccess<T> sourceRandomAccess = source.randomAccess();

		// iterate over the input cursor
		while (targetCursor.hasNext()) {
			// move input cursor forward
			targetCursor.fwd();
			// set the output cursor to the position of the input cursor
			sourceRandomAccess.setPosition(targetCursor);
			// set the value of this pixel of the output image, every Type
			// supports T.set( T type )
			targetCursor.get().set(sourceRandomAccess.get());
		}
	}

	public static void run(Img<FloatType> img) {
		if (true)
			smoothenData(img);
		run(img);
	}

	public static float[] runFit(Img<FloatType> img) {
		int numDimensions = 1; // fitting the peak on the 1D data

		long[] minmax = findMinMax(img);
		long min = minmax[0];
		long max = minmax[1];

		System.out.println(minmax[0] + " : " + minmax[1]);

		double[] typicalSigmas = new double[numDimensions];
		typicalSigmas[0] = (max - min) / 2;

		// TODO: implement the background subtraction here, otherwise peakfitter
		// will
		// give the wrong result

		ArrayList<Point> peaks = new ArrayList<>();

		peaks.add(new Point(new long[] { min + (max - min) / 2 }));

		PeakFitter<FloatType> pf = new PeakFitter<FloatType>(img, (ArrayList) peaks, new LevenbergMarquardtSolver(),
				new EllipticGaussianOrtho(), // use a
				// non-symmetric gauss (sigma_x, sigma_y, sigma_z or sigma_xy &
				// sigma_z)
				new MLEllipticGaussianEstimator(typicalSigmas));
		pf.process();

		// there should be only one element that corresponds to the largest peak
		// that
		// is presented in the histogram
		// System.out.println(pf.getResult().values());

		// TODO: make spot implement Localizable - then this is already a
		// HashMap that maps Spot > double[]
		// this is actually a Map< Spot, double[] >
		final Map<Localizable, double[]> fits = pf.getResult();

		for (Point peak : peaks) {
			double[] params = fits.get(peak);
			for (int j = 0; j < params.length; j++)
				System.out.println(j != 2 ? params[j] : Math.sqrt(1 / (2 * params[j])));
		}

		// there is only one peak that you are looking for
		float center = (float) fits.get(peaks.get(0))[0];

		float border = 0.35f;
		return new float[] { (1 - border) * center, (1 + border) * center };
	}

	// works only on the 1D images
	public static long[] findMinMax(Img<FloatType> arr) {
		int numDimensions = arr.numDimensions();

		long[] minmax = new long[2];
		minmax[0] = arr.dimension(0);
		Cursor<FloatType> cursor = Views.iterable(arr).localizingCursor();

		int[] pos = new int[numDimensions]; // always working on the 1D images

		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);

			float val = cursor.get().get();

			if (pos[0] < minmax[0] && val > eps)
				minmax[0] = pos[0];
			if (pos[0] > minmax[1] && val > eps)
				minmax[1] = pos[0];
		}

		return minmax;
	}

	public static void main(String[] args) {
		testRun();
		System.out.println("Doge!");
	}
}
