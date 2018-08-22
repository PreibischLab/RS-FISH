package scripts.radial.symmetry.process;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.RealSum;
import net.imglib2.view.Views;

import radial.symmetry.utils.IOUtils;
import util.ImgLib2Util;

public class ProcessIntrons {

	// process the exon signal to compute the intron signal around the exon one

	// grab intronImagePath and exonPath and put the detections to the intronPath
	public static void processImage(File intronImagePath, File exonPath, File intronPath) {
		// reading 
		Img<FloatType> img = ImgLib2Util.openAs32Bit(intronImagePath);
		ArrayList<RealPoint> exonSpots = IOUtils.readPositionsFromCSV(exonPath, '\t');
		// processing
		ArrayList<Double> intronIntensity = calculateIntronSignals(exonSpots, img);
		// writing 
		IOUtils.writeIntensitiesToCSV(intronPath, intronIntensity, '\t');
	}

	public static ArrayList<Double> calculateIntronSignals(ArrayList<RealPoint> exonSpots, Img<FloatType> img){
		ArrayList<Double> intronIntensities = new ArrayList<>();
		
		int numDimensions = img.numDimensions();
		
		NLinearInterpolatorFactory< FloatType > factory = new NLinearInterpolatorFactory<>();
		RealRandomAccessible< FloatType > interpolant = Views.interpolate(Views.extendMirrorSingle( img ), factory);
		
		int [] kernelSize = new int[] {5, 5, 3};
		long [] min = new long[numDimensions];
		long [] max = new long[numDimensions];
		double [] offset = new double[numDimensions];
		
		long numPixels = getNumberPixels(kernelSize);
	
		for (int i = 0; i < exonSpots.size(); i++) {
			RealPoint spot = exonSpots.get(i);
			offset = getOffset(spot);
			
			for (int d = 0; d < img.numDimensions(); d++) {
				min[d] = (long)spot.getDoublePosition(d) - kernelSize[d]/2;
				max[d] = (long)spot.getDoublePosition(d) + kernelSize[d]/2;
			}
			
			// System.out.println(Util.printCoordinates(min));
			// System.out.println(Util.printCoordinates(max));
			
			FinalInterval interval = new FinalInterval(min, max);
			double intronIntensity = calulateIntronSignal(exonSpots.get(i), img, interpolant, interval, offset, numPixels);
			intronIntensities.add(intronIntensity);
			// break;
		}
		
		return intronIntensities;
	}
	
	public static long getNumberPixels(int[] kernelSize) {
		long res = 1;
		for (int d = 0; d < kernelSize.length; d++)
				res *= kernelSize[d];
		return res;
	}
	
	public static double[] getOffset(RealPoint spot) {
		double [] offset = new double[spot.numDimensions()];
		for (int d = 0; d < spot.numDimensions(); d++)
			offset[d] = spot.getDoublePosition(d) - (long) spot.getDoublePosition(d);  
		return offset;
	}
	
	public static double calulateIntronSignal(RealPoint spot, RandomAccessible<FloatType> img, RealRandomAccessible<FloatType> rImg, FinalInterval interval, double [] offset, long numPixels) {
		Cursor<FloatType> cursor = Views.interval(img, interval).cursor();
		RealRandomAccess<FloatType> rra = rImg.realRandomAccess();
		
		RealSum sum = new RealSum();
		
		while(cursor.hasNext()){
			cursor.fwd();
			
			double [] position = new double[spot.numDimensions()];
			
			cursor.localize(position);
			
			rra.setPosition(position);
			rra.move(offset);
			rra.localize(position);
			
			sum.add(rra.get().get());
		}
		
		double intronIntensity = sum.getSum()/numPixels;
		return intronIntensity;
	}
	
	
	public static void main(String[] args) {
		// TODO: move to the test package
		String root = "/Users/kkolyva/Desktop/2018-07-31-09-53-32-N2-all-results-together/test";
		String intronImageFilename = "C2-N2_395.tif";
		String exonFilename = "C1-N2_395.csv";
		String intronFilename = "C2-N2_395.csv";

		// IOUtils.checkPaths(...)

		File intronImagePath = Paths.get(root, "median", intronImageFilename).toFile();
		File exonPath = Paths.get(root, exonFilename).toFile();
		File intronPath = Paths.get(root, intronFilename).toFile();

		processImage(intronImagePath, exonPath, intronPath);

		System.out.println("DOGE!");
	}

}
