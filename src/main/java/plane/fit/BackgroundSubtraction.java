package plane.fit;

import java.util.ArrayList;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class BackgroundSubtraction {
	
	public static void applyBackgroundSubtraction(RandomAccessibleInterval <FloatType> src, ArrayList<long[]> peaks, long offset){
		applyBackgroundSubtraction(src, src, peaks, offset);
	}
	
	public static void applyBackgroundSubtraction(RandomAccessibleInterval <FloatType> src, RandomAccessibleInterval<FloatType> dst, ArrayList<long[]> peaks, long offset){
		int numDimensions = src.numDimensions();

		double [] coefficients = new double [numDimensions + 1]; // (!) z y x 1
		long [] position = new long [numDimensions]; // (!) x y z
		long [] spotMin = new long [numDimensions];
		long [] spotMax = new long [numDimensions]; 
		
		long [] imgMin = new long [numDimensions];
		long [] imgMax = new long [numDimensions];
		
		src.min(imgMin);
		src.max(imgMax);
			
		for(int j = 0; j < peaks.size(); ++j){

			backgroundSubtractionCorners(peaks.get(j), src, coefficients, spotMin, spotMax, imgMin, imgMax, offset, numDimensions);

			Cursor <FloatType> cursor = Views.interval(src, spotMin, spotMax).localizingCursor();
			RandomAccess<FloatType> ra = dst.randomAccess();

			while(cursor.hasNext()){
				cursor.fwd();
				cursor.localize(position);				
				double total = coefficients[numDimensions];	
				for (int d = 0; d < numDimensions; ++d){
					total += coefficients[d]*position[numDimensions - d - 1]; 
				}

				// DEBUG: 
				if (j == 0){
					System.out.println("before: " + cursor.get().get());
				}

				// TODO: looks like modifying the initial image is a bad idea 
				ra.setPosition(position);
				ra.get().set(cursor.get().get() - (float)total);

				// DEBUG:
				if (j == 0){
					System.out.println("after:  " + cursor.get().get());
				}

			}		
		}

		// ImageJFunctions.show(source).setTitle("This one is actually modified with background subtraction");
	}

	/**
	 * get the bounding box of the provided peak; the bounding box won't exceed image boundaries
	 * @param peak - coordinate of the peak
	 * @param min - (returned) min coordinates of the bounding box
	 * @param max - (returned) max coordinates of the bounding box
	 * @param imgMin - min coordinates of the underlying image
	 * @param imgMax - max coordinates of the underlying image
	 * @param offset - peak radius
	 * @param numDimensions
	 */
	public static void getBoundaries(long[] peak, long[] min, long [] max, long [] imgMin, long [] imgMax, long offset, int numDimensions){	
		for (int d = 0; d < numDimensions; ++d){
			// check that it does not exceed bounds of the underlying image
			min[d] = Math.max(peak[d] - offset, imgMin[d]);
			max[d] = Math.min(peak[d] + offset, imgMax[d]);
		}
	}
	
	
	/**
	 * calculates the number of boundary points that will be used for plane fitting
	 * @param min - min coordinates of the bounding box
	 * @param max - max coordinates of the bounding box 
	 * @param numDimensions 
	 * @return number of boundary points
	 */
	public static long getNumPoints(long[] min, long [] max, int numDimensions){
		long numPoints = 0;
		// this is some kind of magical number calculation 
		// there is a chance to make it nD but it involves some math 	
		if (numDimensions == 2){	
			// add boundaries (+1 comes from the spot implementation)
			for (int j = 0; j < numDimensions; ++j){
				numPoints += (max[j] - min[j] - 2 + 1)*(1 << (numDimensions - 1));
			}
			// add corners
			numPoints += (1 << numDimensions); //Math.pow(2, numDimensions);
		}
		else{
			if (numDimensions == 3){
				for(int j =0; j < numDimensions; j ++){
					for(int i = 0; i < numDimensions; i++){
						if (i != j){
							// add facets
							numPoints += (max[j] - min[j] - 2 + 1)*(max[i] - min[i] - 2 + 1)*(1 << (numDimensions - 2)); 
						}
					}
					// add edges
					numPoints += (max[j] - min[j] - 2 + 1)*(1 << (numDimensions - 1));
				}			
				// add corners
				numPoints += (1 << numDimensions); //Math.pow(2, numDimensions);
			}
			else
				System.out.println("numDimensions should be 2 or 3, higher dimensionality is not supported.");
		}
		
		return numPoints;
	}
	
	// fill matrices with image intensities
	public static void fillMatrices(RandomAccessibleInterval<FloatType> src, double [][] A, double [] b, long[] min, long [] max, int numDimensions){
		Cursor<FloatType> cursor = Views.interval(src, min, max).localizingCursor();
		
		int rowCount = 0;		
		// TODO: This is not an efficient implementation 
		// because it goes through all image pixels
		while(cursor.hasNext()){
			cursor.fwd();
			double[] pos = new double[numDimensions];
			cursor.localize(pos);
			// check that this is the boundary pixel
			boolean boundary = false;
			for (int d =0; d < numDimensions;++d ){
				if ((long)pos[d] == min[d] || (long)pos[d] == max[d]){
					boundary = true;
					break;
				}
			}
			// process only boundary pixels for plane fitting
			if (boundary){				
				for (int d = 0; d < numDimensions; ++d){			
					A[rowCount][numDimensions - d - 1] = pos[d];
				}
				A[rowCount][numDimensions] = 1;
				// check this one
				b[rowCount] = cursor.get().get();

				rowCount++;
			}
		}
	}
	
	public static void backgroundSubtractionCorners(long [] peak, RandomAccessibleInterval<FloatType> src, double[] coefficients, long[] min, long [] max, long [] imgMin, long [] imgMax, long offset, int numDimensions){	
		
		// fill in min and max boundaries for the peak
		getBoundaries(peak, min, max, imgMin, imgMax, offset, numDimensions);

		// this is some kind of magical number calculation 
		// there is a chance to make it nD but it involves some math 			
		long numPoints = getNumPoints(min, max, numDimensions);

		double [][] A = new double[(int)numPoints][numDimensions + 1];
		double [] b = new double[(int)numPoints];

		fillMatrices(src, A, b, min, max, numDimensions);
		
		// actually fit the plane
		RealMatrix mA = new Array2DRowRealMatrix(A, false);
		RealVector mb = new ArrayRealVector(b, false);
		DecompositionSolver solver = new SingularValueDecomposition(mA).getSolver();
		RealVector mX =  solver.solve(mb);

		// FIXME: This part is done outside of the function for now
		// subtract the values this part 
		// return the result
		// TODO: why proper copying is not working here ?! 
		for (int i  = 0; i < coefficients.length; i++)
			coefficients[i] = mX.toArray()[i];
	}
	
	
	public static void main(String [] args){
		
	}
}
