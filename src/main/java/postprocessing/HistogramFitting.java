package postprocessing;

import java.util.ArrayList;
import java.util.Collections;
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
import net.imglib2.util.RealSum;
import net.imglib2.view.Views;

public class HistogramFitting {

	public static float eps = 1e-7f;
	public static boolean debug = true;

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

		// TODO: adjust these values for smoothing of the data
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

	public static void copyArrayListToImg(long[] from, Img<FloatType> to) {
		RandomAccess<FloatType> ra = to.randomAccess();
		for (int j = 0; j < from.length; j++) {
			float val = from[j];
			ra.setPosition(j, 0); // image is always 1D
			ra.get().set(val);
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

	public static double[] run(Img<FloatType> img, boolean smoothen) {
		if (smoothen)
			smoothenData(img);
		double [] params = runFit(img);
		return params;
	}

	public static float getMean(Img<FloatType> img) {
		double total = 0;
		// long idx = 0;
		
		// Count all values using the RealSum class.
		// It prevents numerical instabilities when adding up millions of pixels
		final RealSum realSum = new RealSum();

		// Cursor<FloatType> cursor = img.localizingCursor();
		RandomAccess<FloatType> ra = img.randomAccess();
		
		// seems fine
		for (int j = 0; j < img.dimension(0); j++) {
			ra.setPosition(j, 0);
			double val = ra.get().getRealDouble(); // non-empty bins
			if(val > 0) { 
				realSum.add(val*j);
				total += val;
			}
		}
		
//		while(cursor.hasNext()) {
//			double val = cursor.next().get();
//			realSum.add(val*idx);
//			idx++; 
//			total += val;
//		}
		
//			if (val.getRealDouble() > 0){
//				realSum.add(val.getRealDouble());
//				total++;
//			}

		return (float) (realSum.getSum() / total);

	}

	// TODO: finish! 
	public static float getMedian(Img<FloatType> img){
		float center = 0;
		
		ArrayList<Float> values = new ArrayList<>((int) img.size());
		Cursor<FloatType> cursor = img.cursor();
		
		System.out.println(img.size() + " : " + values.size());
		
		int idx = 0;
		while (cursor.hasNext()) {
			cursor.fwd();
			values.add(idx, cursor.get().get());
			idx++;
		}
		
		Collections.sort(values);
		center = values.get(values.size() / 2).floatValue();
		
		return center;
	}

	public static float getStd(Img<FloatType> img, float mean) {
		float res = 0;
		long total = 0;
		
		final RealSum realSum = new RealSum();
		RandomAccess<FloatType> ra = img.randomAccess();
		
		// seems fine
		for (int j = 0; j < img.dimension(0); j++) {
			ra.setPosition(j, 0);
			double val = ra.get().getRealDouble(); // non-empty bins
			if(val > 0) { 
				realSum.add(Math.pow(j - mean, 2)*val);
				total += val;
			}
		}
		
//		// std = sqrt(mean(abs(x - x.mean())**2))
//		Cursor<FloatType> cursor = img.cursor();
//		while (cursor.hasNext()) {
//			cursor.fwd();
//			if (cursor.get().get() > 0){
//				res += Math.pow(cursor.get().get() - mean, 2);
//				total++;
//			}
//		}
//
//		res /= total;

		// return (float) Math.sqrt(res);
		return (float) Math.sqrt(realSum.getSum() / total);
	}

	public static double[] runFit(Img<FloatType> img) {
		int numDimensions = 1; // fitting the peak on the 1D data

//		long[] minmax = findMinMax(img);
//		long min = minmax[0];
//		long max = minmax[1];
//		System.out.println(minmax[0] + " : " + minmax[1]);

		float mean = getMean(img);
		float sigma = getStd(img, mean);
		
		if (debug)
			System.out.println("mean: " + mean + " sigma: " + sigma);

		double[] typicalSigmas = new double[numDimensions];
		typicalSigmas[0] = sigma;

		ArrayList<Point> peaks = new ArrayList<>();

		// peaks.add(new Point(new long[] { min + (max - min) / 2 }));
		// peaks.add(new Point(new long[] { (long) median }));
		peaks.add(new Point(new long[] { (long) mean }));

		// TODO: update the code to GenericPeakFitter
		
		PeakFitter<FloatType> pf = new PeakFitter<>(img, (ArrayList) peaks, new LevenbergMarquardtSolver(),
				new EllipticGaussianOrtho(), // use a
				// non-symmetric gauss (sigma_x, sigma_y, sigma_z or sigma_xy &
				// sigma_z)
				new MLEllipticGaussianEstimator(typicalSigmas));
		pf.process();

		// there should be only one element that corresponds to the largest peak
		// that
		// is presented in the histogram
		// System.out.println(pf.getResult().values());

		final Map<Localizable, double[]> fits = pf.getResult();

		if (debug)
			for (Point peak : peaks) {
				double[] params = fits.get(peak);
				for (int j = 0; j < params.length; j++)
					System.out.println(params[j]);
			}

		// there is only one peak that you are looking for
		// float center = (float) fits.get(peaks.get(0))[0];

		return fits.get(peaks.get(0));
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
