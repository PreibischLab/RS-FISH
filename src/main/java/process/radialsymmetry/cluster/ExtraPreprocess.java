/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 Developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package process.radialsymmetry.cluster;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.opencsv.CSVReader;

import fit.PointFunctionMatch;
import fit.polynomial.QuadraticFunction;
import fitting.Spot;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ImageProcessor;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class ExtraPreprocess {
	// this class performs extra preprocessing of the images: 
	// 1. calculate the median intensity per plane (consider only roi pixels) = S_i
	// 2. subtract min (or median) of S_i from each pixel including outer roi
	// 3. multiple the whole image by a factor p so that 1 values are 1's again
	// 4. perform the plane dependent multiplication (z-plane fix of the intensity drop) 

	public static void runExtraPreprocess(File pathImages, File pathDb, File pathImagesMedian) {
		// parse the db with smFish labels and good looking images
		ArrayList<ImageData> imageData = Preprocess.readDb(pathDb);

		// to see the feedback
		long currentIdx = 0;
		for (ImageData imageD : imageData) {
			currentIdx++;
			// unprocessed path
			String inputImagePath = pathImages.getAbsolutePath() + "/" + imageD.getFilename() + ".tif";
			// processed path 
			String outputImagePath = pathImagesMedian.getAbsolutePath() + "/" + imageD.getFilename() + ".tif";

			System.out.println( currentIdx + "/" + imageData.size() + ": " + inputImagePath);
			// System.out.println(outputImagePath);
			// System.out.println(roiImagePath);

			// check that the corresponding files is not missing
			if (new File(inputImagePath).exists()) {
				medianOfMedian(new File(inputImagePath), new File(outputImagePath));
			}
			else {
				System.out.println("Preprocess.java: " + inputImagePath + " file is missing");
			}
		}
	}


	// read the z and I values from the csbv file 
	public static ArrayList<float []> readCsv(File path) {
		final int nColumns = 5;
		final int zIndex = 3;
		final int iIndex = 4;

		CSVReader reader = null;
		String[] nextLine = new String [nColumns];

		ArrayList<float []> zI = new ArrayList<>();

		try {
			int toSkip = 1; 
			reader = new CSVReader(new FileReader(path));
			for ( int i = 0; i < toSkip; ++i )
				reader.readNext();

			//reader.skip( toSkip );
			// while there are rows in the file
			while ((nextLine = reader.readNext()) != null) {

				float z = Float.parseFloat(nextLine[zIndex]);
				float I = Float.parseFloat(nextLine[iIndex]);

				zI.add(new float[] {z, I});

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return zI;
	}



	public static ArrayList<float[]> sortValues(ArrayList<float[]> zI) {
		Collections.sort(zI, new zComparator());
		return zI;
	}

	public static ArrayList<float []> getMedianPerSlice(ArrayList<float[]> zI, long zSlices){

		ArrayList<float[]> medianZI = new ArrayList<>();

		for (int z = 0; z < zSlices; z++) {
			ArrayList<float[]> list = new ArrayList<>();
			for (float [] item : zI) {
				if(Math.round(item[0]) == z) {
					list.add(item);
				}
			}
			if (!list.isEmpty()) {
				Collections.sort(list, new iComparator());
				medianZI.add(list.get(list.size()/2));
				System.out.println(list.get(list.size()/2)[0] + ", "+ list.get(list.size()/2)[1]);
			}
		}
		return medianZI;
	}


	// FIXME: CHECK THAT THIS ONE IS ACTUALLY WORKING
	// calculates the median intensity per-slice and returns the set of z-points and intensities
	public static ArrayList<float []> getMedianPerSlice(ArrayList<Spot> spots, ArrayList<Float> intensity, long zSlices){
		ArrayList<float[]> medianZI = new ArrayList<>();

		for (int z = 0; z < zSlices; z++) {
			ArrayList<Integer> indices = new ArrayList<>();
			for (int j = 0; j < spots.size(); j++) {
				// not the exact slice but in between
				// if(spots.get(j).getIntPosition(2) == z) {
				int spread = 3;
				if (Math.abs(spots.get(j).getOriginalLocation()[2] - z) <= spread) {
					indices.add(j);
				}
			}

			// there are points for this specific zSlice
			if (!indices.isEmpty()) {
				// grabs only necessary intensity values
				List<Float> fIntensity = indices.stream().map(intensity::get).collect(Collectors.toList());
				List<Spot> fSpots = indices.stream().map(spots::get).collect(Collectors.toList());
				// sort according to the intensities
				IndexComparator comparator = new IndexComparator(fIntensity);
				Integer[] intensityIndices = comparator.createIndexArray();
				Arrays.sort(intensityIndices, comparator);
				// now indices will give me the result I am looking for 
				int id = intensityIndices[intensityIndices.length/2];

				medianZI.add(new float[]{fSpots.get(id).getFloatPosition(2), fIntensity.get(id)});
			} 
		}
		return medianZI;
	}

	public static class zComparator implements Comparator<float[]>{
		@Override
		public int compare(float[] e1, float[] e2) {
			int result = 0; // same 
			if (e1[0] < e2[0]) {
				result = -1;
			} else if (e1[0] > e2[0]) {
				result = 1;
			}
			return result;
		}
	}

	public static class iComparator implements Comparator<float[]>{
		@Override
		public int compare(float[] e1, float[] e2) {
			int result = 0; // same 
			if (e1[1] < e2[1]) {
				result = -1;
			} else if (e1[1] > e2[1]) {
				result = 1;
			}
			return result;
		}
	}

	public static class IndexComparator implements Comparator<Integer>
	{
		private List<Float> intensity;

		public IndexComparator(List<Float> intensity)
		{
			this.intensity = intensity;
		}

		public Integer[] createIndexArray()
		{
			int n = intensity.size();
			Integer[] indexes = new Integer[n];
			for (int i = 0; i < n; i++)
				indexes[i] = i; // Autoboxing
			return indexes;
		}

		@Override
		public int compare(Integer index1, Integer index2)
		{
			// Autounbox from Integer to int to use as array indexes
			return intensity.get(index1).compareTo(intensity.get(index2));
		}
	}

	//	public static ImagePlus fixIntensitiesOnlySpots(ImagePlus imp, File spotsFirstRunFilename) {
	//		Img<FloatType> img = ImageJFunctions.wrap(imp);
	//
	//		// System.out.println(ip.getWidth() + " : " + ip.getHeight());
	//		// System.out.println(imp.getWidth() + " : " + imp.getHeight());
	//
	//		// we expect this values to be in the ROI
	//		ArrayList<float []> zI = readCsv(spotsFirstRunFilename);
	//
	//		// fitting part
	//		boolean includeIntercept = true; // use constant term
	//		SimpleRegression sr = new SimpleRegression(includeIntercept);
	//		// perform correction in 3D only
	//		int numDimensions = 3;
	//
	//		double zMin = Double.MAX_VALUE;
	//
	//		//		for (float [] item : zI) {
	//		//			float z = item[0];
	//		//			float I = item[1];
	//		//			sr.addData(z, I);
	//		//			if (z < zMin)
	//		//				zMin = z;
	//		//		}
	//
	//		ArrayList<float[]> medianZI =  getMedianPerSlice(zI,img.dimension(numDimensions - 1));
	//
	//		for (float [] item : medianZI) {
	//			float z = item[0];
	//			float I = item[1];
	//			sr.addData(z, I);
	//			if (z < zMin)
	//				zMin = z;
	//		}
	//
	//
	//		double slope = sr.getSlope();
	//		double intercept = sr.getIntercept();
	//		// at this point the fitting i already done
	//
	//		// double zMin = getZMin(pixi, numDimensions);
	//
	//		System.out.println("zMin:" + zMin);
	//		System.out.println("params are:" + slope + " : " + intercept);
	//
	//
	//		Cursor<FloatType> c = img.cursor();
	//
	//		while(c.hasNext()) {
	//			c.fwd();
	//
	//			int z = c.getIntPosition(2);
	//			float I = c.get().get();
	//			double dI = linearFunc(zMin, slope, intercept) - linearFunc(z, slope, intercept);
	//
	//			NumberFormat formatter = new DecimalFormat("0.#####E0");
	//			// System.out.println(formatter.format((float)(I)) +" => " + formatter.format((float)(I + dI)));
	//
	//			c.get().set((float)(I + dI));
	//		}
	//
	//		return imp;
	//	}

	public static ArrayList<Point> copySpots(ArrayList<Spot> spots, ArrayList<Float> intensity){
		final ArrayList<Point> points = new ArrayList<>();
		
		for (Spot spot : spots) {
			// lazy
			int numDimensions = 3;
			
			double [] loc = new double [numDimensions];
			spot.localize(loc);
			points.add(new Point(new double[] {loc[2], intensity.get(spots.indexOf(spot))}));
		}
		return points;
	}

	public static ImagePlus fixIntensitiesOnlySpotsRansac(Img<FloatType> img, ArrayList<Spot> spots, ArrayList<Float> intensity, double [] gCoeff, boolean doZcorrection) {
	
		int numDimensions = 3;
		
		// fitting part 
		int degree = gCoeff.length - 1;
		
		int nIterations = 1000;
		double epsilon = 0.1;
		double minInlierRatio = 0.5;
		QuadraticFunction qf = new QuadraticFunction();
		
		ArrayList<Point> points = copySpots(spots, intensity);
		final ArrayList<PointFunctionMatch> candidates = new ArrayList<>();
		final ArrayList<PointFunctionMatch> inliers = new ArrayList<>();
		
		for (final Point p : points)
			candidates.add(new PointFunctionMatch(p));
		
		try {
			qf.ransac(candidates, inliers, nIterations, epsilon, minInlierRatio);
			qf.fit(candidates);
		}
		catch (NotEnoughDataPointsException | IllDefinedDataPointsException exc) {
			// TODO Auto-generated catch block
			exc.printStackTrace();
		}
		
		for (int j = 0; j <= degree; j++)
			gCoeff[j] = qf.getCoefficient(j);
		
		float zMin = Float.MAX_VALUE;
		for (Spot spot : spots)
			if (spot.getFloatPosition(numDimensions - 1) < zMin)
				zMin = spot.getFloatPosition(numDimensions - 1);
		
		// at this point the fitting i already done
		System.out.println("zMin:" + zMin);
		System.out.println("params are:" + qf.getCoefficient(0) + " : " + qf.getCoefficient(1) + " : " + qf.getCoefficient(2));
		
		int currentSlice = 0;

		// we z correct the whole image, not only the embryo
		Cursor<FloatType> c = img.cursor();
		while(c.hasNext()) {
			c.fwd();
			float z = c.getFloatPosition(numDimensions - 1);
			float I = c.get().get();

			//			// old way to fix the intensities
			//			double dI = linearFunc(zMin, slope, intercept) - linearFunc(z, slope, intercept);
			//			NumberFormat formatter = new DecimalFormat("0.#####E0");
			//			// System.out.println(formatter.format((float)(I)) +" => " + formatter.format((float)(I + dI)));
			//			c.get().set((float)(I + dI));

			double fixFactor = polyFunc(zMin, gCoeff) / polyFunc(z, gCoeff);
			// DEBUG: 
//			if ((int) z != currentSlice)
//				System.out.println("z: " + (currentSlice++) + ", factor=: " + fixFactor);
			c.get().set((float)(I*fixFactor));
		}

		// TODO: REMOVE hardcode! 
		// BatchProcess.saveResult(new File("/Users/kkolyva/Desktop/2018-04-18-08-29-25-test/test/2018-05-02-13-33-11-median-median-first-test/csv/FIRST_RUN.csv"), spots, intensity);
		
		// TODO: z-correct the intensities and return them here, too
		if (doZcorrection) {
			for(int j = 0; j < spots.size(); j++) {
				float z = spots.get(j).getFloatPosition(numDimensions - 1);
				float I = intensity.get(j);

				double fixFactor = polyFunc(zMin, gCoeff) / polyFunc(z, gCoeff);
				intensity.set(j, (float)(I*fixFactor));
			} 
		}

		return ImageJFunctions.wrap(img, "");
	}
	
	
	// FIXME: Check that this function is actually working
	public static ImagePlus fixIntensitiesOnlySpots(Img<FloatType> img, ArrayList<Spot> spots, ArrayList<Float> intensity, double [] gCoeff, boolean doZcorrection) {
		// Img<FloatType> img = ImageJFunctions.wrap(imp);

		// fitting part
		int degree = gCoeff.length - 1;
		WeightedObservedPoints obs = new WeightedObservedPoints();
		PolynomialCurveFitter pcf = PolynomialCurveFitter.create(degree);
		
		// perform correction in 3D only
		int numDimensions = 3;

		double zMin = Double.MAX_VALUE;
		// double zMin = getZMin(spots, numDimensions);

		ArrayList<float[]> medianZI = getMedianPerSlice(spots, intensity, img.dimension(numDimensions - 1));

		for (int j = 0; j < medianZI.size(); ++j) {
			float z = medianZI.get(j)[0];
			float I = medianZI.get(j)[1];
			obs.add(z, I);
			if (z < zMin)
				zMin = z;
		}

		final double[] coeff = pcf.fit(obs.toList());
		for (int j = 0; j < coeff.length; j++)
			gCoeff[j] = coeff[j];

		// at this point the fitting i already done
		System.out.println("zMin:" + zMin);
		System.out.println("params are:" + coeff[0] + " : " + coeff[1] + " : " + coeff[2]);

		int currentSlice = 0;

		// we z correct the whole image, not only the embryo
		Cursor<FloatType> c = img.cursor();
		while(c.hasNext()) {
			c.fwd();
			float z = c.getFloatPosition(numDimensions - 1);
			float I = c.get().get();

			//			// old way to fix the intensities
			//			double dI = linearFunc(zMin, slope, intercept) - linearFunc(z, slope, intercept);
			//			NumberFormat formatter = new DecimalFormat("0.#####E0");
			//			// System.out.println(formatter.format((float)(I)) +" => " + formatter.format((float)(I + dI)));
			//			c.get().set((float)(I + dI));

			double fixFactor = polyFunc(zMin, coeff) / polyFunc(z, coeff);
			// DEBUG: 
			if ((int) z != currentSlice)
				System.out.println("z: " + (currentSlice++) + ", factor=: " + fixFactor);
			c.get().set((float)(I*fixFactor));
		}

		// TODO: REMOVE hardcode! 
		// BatchProcess.saveResult(new File("/Users/kkolyva/Desktop/2018-04-18-08-29-25-test/test/2018-05-02-13-33-11-median-median-first-test/csv/FIRST_RUN.csv"), spots, intensity);
		
		// TODO: z-correct the intensities and return them here, too
		if (doZcorrection) {
			for(int j = 0; j < spots.size(); j++) {
				float z = spots.get(j).getFloatPosition(numDimensions - 1);
				float I = intensity.get(j);

				double fixFactor = polyFunc(zMin, coeff) / polyFunc(z, coeff);
				intensity.set(j, (float)(I*fixFactor));
			} 
		}

		return ImageJFunctions.wrap(img, "");
	}
	
	
	// FIXME: Check that this function is actually working
	public static ImagePlus fixIntensitiesOnlySpots1(Img<FloatType> img, ArrayList<Spot> spots, ArrayList<Float> intensity, boolean doZcorrection) {
		// Img<FloatType> img = ImageJFunctions.wrap(imp);

		// fitting part
		boolean includeIntercept = true;
		SimpleRegression sr = new SimpleRegression(includeIntercept);
		// perform correction in 3D only
		int numDimensions = 3;

		double zMin = Double.MAX_VALUE;
		// double zMin = getZMin(spots, numDimensions);

		ArrayList<float[]> medianZI = getMedianPerSlice(spots, intensity, img.dimension(numDimensions - 1));

		for (int j = 0; j < medianZI.size(); ++j) {
			float z = medianZI.get(j)[0];
			float I = medianZI.get(j)[1];
			sr.addData(z, I);
			if (z < zMin)
				zMin = z;
		}

		double slope = sr.getSlope();
		double intercept = sr.getIntercept();

		// at this point the fitting i already done
		System.out.println("zMin:" + zMin);
		System.out.println("params are:" + slope + " : " + intercept);

		int currentSlice = 0;

		// we z correct the whole image, not only the embryo
		Cursor<FloatType> c = img.cursor();
		while(c.hasNext()) {
			c.fwd();
			float z = c.getFloatPosition(numDimensions - 1);
			float I = c.get().get();

			//			// old way to fix the intensities
			//			double dI = linearFunc(zMin, slope, intercept) - linearFunc(z, slope, intercept);
			//			NumberFormat formatter = new DecimalFormat("0.#####E0");
			//			// System.out.println(formatter.format((float)(I)) +" => " + formatter.format((float)(I + dI)));
			//			c.get().set((float)(I + dI));

			double fixFactor = linearFunc(zMin, slope, intercept) / linearFunc(z, slope, intercept);
			// DEBUG: 
			if ((int) z != currentSlice)
				System.out.println("z: " + (currentSlice++) + ", factor=: " + fixFactor);
			c.get().set((float)(I*fixFactor));
		}

		// TODO: REMOVE hardcode! 
		// BatchProcess.saveResult(new File("/Users/kkolyva/Desktop/2018-04-18-08-29-25-test/test/2018-04-18-14-46-52-median-median-first-test/csv/FIRST_RUN.csv"), spots, intensity);
		
		
		// TODO: z-correct the intensities and return them here, too
		if (doZcorrection) {
			for(int j = 0; j < spots.size(); j++) {
				float z = spots.get(j).getFloatPosition(numDimensions - 1);
				float I = intensity.get(j);

				double fixFactor = linearFunc(zMin, slope, intercept) / linearFunc(z, slope, intercept);
				intensity.set(j, (float)(I*fixFactor));
			} 
		}

		return ImageJFunctions.wrap(img, "");
	}

	public static ImagePlus fixIntensitiesOnlySpots(ImagePlus imp, File spotsFirstRunFilename) {
		Img<FloatType> img = ImageJFunctions.wrap(imp);

		// System.out.println(ip.getWidth() + " : " + ip.getHeight());
		// System.out.println(imp.getWidth() + " : " + imp.getHeight());

		// we expect this values to be in the ROI
		ArrayList<float []> zI = readCsv(spotsFirstRunFilename);

		// fitting part
		boolean includeIntercept = true; // use constant term
		SimpleRegression sr = new SimpleRegression(includeIntercept);
		// perform correction in 3D only
		int numDimensions = 3;

		double zMin = Double.MAX_VALUE;

		//		for (float [] item : zI) {
		//			float z = item[0];
		//			float I = item[1];
		//			sr.addData(z, I);
		//			if (z < zMin)
		//				zMin = z;
		//		}

		ArrayList<float[]> medianZI =  getMedianPerSlice(zI,img.dimension(numDimensions - 1));

		for (float [] item : medianZI) {
			float z = item[0];
			float I = item[1];
			sr.addData(z, I);
			if (z < zMin)
				zMin = z;
		}


		double slope = sr.getSlope();
		double intercept = sr.getIntercept();
		// at this point the fitting i already done

		// double zMin = getZMin(pixi, numDimensions);

		System.out.println("zMin:" + zMin);
		System.out.println("params are:" + slope + " : " + intercept);


		Cursor<FloatType> c = img.cursor();

		while(c.hasNext()) {
			c.fwd();

			int z = c.getIntPosition(2);
			float I = c.get().get();
			double dI = linearFunc(zMin, slope, intercept) - linearFunc(z, slope, intercept);

			NumberFormat formatter = new DecimalFormat("0.#####E0");
			// System.out.println(formatter.format((float)(I)) +" => " + formatter.format((float)(I + dI)));

			c.get().set((float)(I + dI));
		}

		return imp;
	}


	public static float calculateMedianIntensity(ImagePlus imp) {
		// used for iterating
		Img<FloatType> img = ImageJFunctions.wrap(imp);

		ImageProcessor ip = imp.getMask();
		Rectangle bounds = imp.getRoi().getBounds();
		// System.out.println(ip.getWidth() + " : " + ip.getHeight());
		// System.out.println(imp.getWidth() + " : " + imp.getHeight());

		float [] medianPerPlane = new float[imp.getNSlices()];

		// System.out.println(bounds.x + " : " + bounds.y);
		// will be used for median filtering
		// float [] pixels = new float [(int)(img.dimension(0)*img.dimension(1))];

		for(int z = 0; z < img.dimension(2); z++) {
			// iterator over the slice
			final Cursor< FloatType > cursor = Views.hyperSlice(img, 2, z).cursor();
			ArrayList<Float> pixels = new ArrayList<>();

			while(cursor.hasNext()) {
				cursor.fwd();

				int x = cursor.getIntPosition(0) - bounds.x;
				int y = cursor.getIntPosition(1) - bounds.y;

				if (ip != null && ip.getPixel(x, y) != 0) {
					pixels.add(cursor.get().get());
				}
			}

			Collections.sort(pixels);
			medianPerPlane[z] = pixels.get(pixels.size() / 2);
		}


		Arrays.sort(medianPerPlane);
		float medianMedianPerPlane = medianPerPlane[medianPerPlane.length / 2];

		System.out.println("medianMedianPerPlane: " + medianMedianPerPlane);

		return medianMedianPerPlane;
	}

	public static ImagePlus subtractValue(ImagePlus imp, float value) {
		Img<FloatType> img = ImageJFunctions.wrap(imp);
		Cursor<FloatType> cursor = img.cursor();

		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.get().sub(new FloatType(value));
		}

		return imp;
	}

	public static Img<FloatType> subtractValue(Img<FloatType> img, float value) {
		Cursor<FloatType> cursor = img.cursor();

		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.get().sub(new FloatType(value));
		}

		return img;
	}


	public static ImagePlus multiplyByValue(ImagePlus imp, float value) {
		Img<FloatType> img = ImageJFunctions.wrap(imp);
		Cursor<FloatType> cursor = img.cursor();

		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.get().mul(new FloatType(value));
		}

		return imp;
	}

	// consider all pixels in the roi image
	public static ImagePlus fixIntensitiesAllPixels(ImagePlus imp) {
		boolean includeIntercept = true; // use constant term
		SimpleRegression sr = new SimpleRegression(includeIntercept);
		// perform correction in 3D only
		int numDimensions = 3;

		Img<FloatType> img = ImageJFunctions.wrap(imp);

		ImageProcessor ip = imp.getMask();
		Rectangle bounds = imp.getRoi().getBounds();
		// System.out.println(ip.getWidth() + " : " + ip.getHeight());
		// System.out.println(imp.getWidth() + " : " + imp.getHeight());

		float [] medianPerPlane = new float[imp.getNSlices()];

		// System.out.println(bounds.x + " : " + bounds.y);
		// will be used for median filtering
		// float [] pixels = new float [(int)(img.dimension(0)*img.dimension(1))];

		double zMin = Double.MAX_VALUE;

		final Cursor<FloatType> cursor = img.cursor();
		while(cursor.hasNext()) {
			cursor.fwd();

			int x = cursor.getIntPosition(0) - bounds.x;
			int y = cursor.getIntPosition(1) - bounds.y; 
			// check that the pixil is inside of the roi
			if (ip != null && ip.getPixel(x, y) != 0) {
				int z = cursor.getIntPosition(2);
				float I = cursor.get().get();
				sr.addData(z, I);

				// looking for min z
				if (z < zMin)
					zMin = z;
			}
		}

		double slope = sr.getSlope();
		double intercept = sr.getIntercept();
		// at this point the fitting i already done

		// double zMin = getZMin(pixi, numDimensions);

		System.out.println("zMin:" + zMin);
		System.out.println("params are:" + slope + " : " + intercept);

		cursor.reset();

		while(cursor.hasNext()) {
			cursor.fwd();

			int x = cursor.getIntPosition(0) - bounds.x;
			int y = cursor.getIntPosition(1) - bounds.y;
			// check that the pixil is inside of the roi
			if (ip != null && ip.getPixel(x, y) != 0) {

				int z = cursor.getIntPosition(2);
				float I = cursor.get().get();
				double dI = linearFunc(zMin, slope, intercept) - linearFunc(z, slope, intercept);

				NumberFormat formatter = new DecimalFormat("0.#####E0");
				// System.out.println(formatter.format((float)(dI)));

				cursor.get().set((float)(I + dI));
			}
		}

		return imp;
	}

	// return y for y = kx + b
	public static double linearFunc(double x, double k, double b) {
		return k * x + b;
	}

	// return y for y = a*x*x + b*x + c
	public static double polyFunc(double x, double [] a) {
		double y = 0;
		for (int j = 0; j < a.length; j++)
			y += Math.pow(x, j)*a[j];
		return y;
	}
	
	public static void medianOfMedian(File filepath, File outpath) {
		ImagePlus imp = IJ.openImage(filepath.getAbsolutePath());
		float medianMedianPerPlane = calculateMedianIntensity(imp);
		ImagePlus pImp = subtractValue(imp, medianMedianPerPlane);

		// float scalingFactor = 1/(1 - medianMedianPerPlane);
		// pImp = multiplyByValue(pImp, scalingFactor);

		FileSaver fs = new FileSaver(pImp);
		fs.saveAsTiff(outpath.getAbsolutePath());
	}


	public static void main(String[] args) {
		new ImageJ();

		String folder = "/Volumes/1TB/test/";
		String imgName = "C1-N2_96.tif";

		String spotsFirstRunFilename = "C1-N2_96-first-run.csv";

		File filepath = new File(folder + imgName);
		ImagePlus imp = IJ.openImage(filepath.getAbsolutePath());

		float medianMedianPerPlane = calculateMedianIntensity(imp);
		ImagePlus pImp = subtractValue(imp, medianMedianPerPlane);

		float scalingFactor = 1/(1 - medianMedianPerPlane);
		pImp = multiplyByValue(pImp, scalingFactor);

		// run the z-dependent fix only on the pixels inside of the roi
		File spotsFilepath = new File(folder + spotsFirstRunFilename);
		System.out.println(spotsFilepath);
		pImp = fixIntensitiesOnlySpots(pImp, spotsFilepath);

		// imp.show();
		pImp.show();

		FileSaver fs = new FileSaver(pImp);
		fs.saveAsTiff(folder + "/C1-N2_96-test-not-imp.tif");

		System.out.println("DOGE!");
	}

}
