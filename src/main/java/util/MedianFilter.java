/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2025 RS-FISH developers.
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
package util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ij.ImageJ;
// import klim.deconvolution.Utils;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class MedianFilter
{
	public static class MedianThread< T extends RealType< T > & Comparable< T > > implements Callable<Void>
	{
		final ImagePortion task;
		final RandomAccessibleInterval<T> src;
		final RandomAccessible< T > infSrc;
		final int[] kernelHalfDim;
		final RandomAccessibleInterval< T > dst;
		final int n;

		public MedianThread( final ImagePortion task, final RandomAccessible< T > infSrc, final RandomAccessibleInterval<T> src, final RandomAccessibleInterval< T > dst, final int[] kernelHalfDim )
		{
			this.task = task;
			this.src = src;
			this.infSrc = infSrc;
			this.dst = dst;
			this.kernelHalfDim = kernelHalfDim;
			this.n = src.numDimensions();
		}
	
		@Override				
		public Void call() throws Exception{
			// @Parallel : these should be local for each Thread
			final Cursor<T> cSrc = Views.iterable(src).localizingCursor();
			final RandomAccess<T> rDst = dst.randomAccess();
			
			cSrc.jumpFwd(task.getStartPosition());
				
			// store kernel boundaries
			final long[] min = new long[n];
			final long[] max = new long[n];

			// store previous/current position of cursor
			final long[] pPos = new long[n]; 
			final long[] cPos = new long[n]; 
			cSrc.localize(cPos);
			
			updateKernelMinMax(min, max, cPos, kernelHalfDim, n);

			// contains all elements of the kernel
			IterableInterval<T> histogram = Views.interval(infSrc, min, max);
			List<T> histogramList = new ArrayList<T>(); 
			addAll(histogram, histogramList);

			final long[] localMin  = new long[n - 1];
			final long[] localMax  = new long[n - 1];

			for(long j = 0; j < task.getLoopSize(); ++j){
				if(cSrc.hasNext()){ // prevents possible problems in the last task
					cSrc.localize(pPos);
					cSrc.fwd();
					cSrc.localize(cPos);

					final long checkDist = checkDist(pPos, cPos, n);	 // check if the cursor moved only by one step						
					if (checkDist == 0){ // moved too far
						// define new boundaries of the new kernel-window
						updateKernelMinMax(min, max, cPos, kernelHalfDim, n);
						// adjust the histogram window
						histogram = Views.interval(infSrc, min, max);
						histogramList.clear();					
						addAll(histogram, histogramList);
					}
					else{ // moved by one
						final int dim = (int)Math.abs(checkDist) - 1; 	 //
						final long step = (long) Math.signum(checkDist); // shows the direction of movement (+1/-1)

						min[ dim ] += step;
						max[ dim ] += step;

						final RandomAccessible<T> removeSlice = Views.hyperSlice(infSrc, dim, step < 0 ? max[dim] + 1: min[dim] - 1);
						final RandomAccessible<T> addSlice  = Views.hyperSlice(infSrc, dim, step < 0 ? min[dim] : max[dim]);		

						// remove one dimension
						updateLocalMinMax( n, dim, localMin, localMax, min, max );

						final RandomAccessibleInterval<T> removeHistogram = Views.interval(removeSlice, localMin, localMax);
						final RandomAccessibleInterval<T> addHistogram  = Views.interval(addSlice, localMin, localMax);

						removeElements(removeHistogram, histogramList);
						addElements(addHistogram, histogramList);
					}

					// get/set the median value 
					rDst.setPosition(cSrc);
					rDst.get().set(histogramList.get(histogramList.size()/2));
				}
			}
			return null; 
		}
	}
	
	private static final void updateLocalMinMax( final int n, final int dim, final long[] localMin, final long[] localMax, final long[] min, final long[] max )
	{
		for (int i = 0; i < n; ++i)
			if (i != dim){
				localMin[i > dim ? i - 1 : i] = min[i];
				localMax[i > dim ? i - 1 : i] = max[i];
			}		
	}
	
	public static class ImagePortion
	{
		public ImagePortion( final long startPosition, long loopSize )
		{
			this.startPosition = startPosition;
			this.loopSize = loopSize;
		}
		
		public long getStartPosition() { return startPosition; }
		public long getLoopSize() { return loopSize; }
		
		protected long startPosition;
		protected long loopSize;
		
		@Override
		public String toString() { return "Portion [" + getStartPosition() + " ... " + ( getStartPosition() + getLoopSize() - 1 ) + " ]"; }
	}
	
	public static final Vector<ImagePortion> divideIntoPortions( final long imageSize, final int numPortions )
	{
		final long threadChunkSize = imageSize / numPortions;
		final long threadChunkMod = imageSize % numPortions;
		
		final Vector<ImagePortion> portions = new Vector<ImagePortion>();
		
		for ( int portionID = 0; portionID < numPortions; ++portionID )
		{
			// move to the starting position of the current thread
			final long startPosition = portionID * threadChunkSize;

			// the last thread may has to run longer if the number of pixels cannot be divided by the number of threads
			final long loopSize;
			if ( portionID == numPortions - 1 )
				loopSize = threadChunkSize + threadChunkMod;
			else
				loopSize = threadChunkSize;
			
			portions.add( new ImagePortion( startPosition, loopSize ) );
		}
		
		return portions;
	}

	/*
	 * call median filter with mirror single extension strategy
	 * */
	public static < T extends RealType<  T > & Comparable<T> > void medianFilter(
			final RandomAccessibleInterval< T > src, final RandomAccessibleInterval< T > dst, final int[] kernelDim, final int numThreads, final int numTasks){
		medianFilter(Views.extendMirrorSingle(src), src, dst, kernelDim, numThreads, numTasks);
	}

	/*
	 * call median filter with the predefined # of threads and # of tasks
	 * */
	public static < T extends RealType<  T > & Comparable<T> > void medianFilter(
			final RandomAccessibleInterval< T > src, final RandomAccessibleInterval< T > dst, final int[] kernelDim )
	{
		// set the parameters automatically 
		final int numThreads = Runtime.getRuntime().availableProcessors(); 
		final int numTasks = numThreads*6;
		medianFilter(Views.extendMirrorSingle(src), src, dst, kernelDim, numThreads, numTasks);
	}
	
	/** 
	 * Apply median filter to the whole image
	 * @param infSrc -- extended image
	 * @param srcInterval -- interval for the input
	 * @param dst -- output image
	 * @param kernelDim -- the size of the kernel in each dimension (should be *odd*)
	 * @param numThreads -- threads to use
	 * @param numTasks -- how many chunks
	 * @param <T> - the type
	 * */
	public static < T extends RealType<  T > & Comparable<T> > void medianFilter(
			final RandomAccessible< T > infSrc, final Interval srcInterval, final RandomAccessibleInterval< T > dst, final int[] kernelDim, final int numThreads, final int numTasks){

		final RandomAccessibleInterval<T> src = Views.interval(infSrc, srcInterval);
		final int n = src.numDimensions();

		final int[] kernelHalfDim = new int[n]; 
		for (int d = 0; d < n; ++d){
			// check that the dimension of the kernel is correct
			if ( kernelDim[d]%2 == 0 || kernelDim[d] < 3)	
				throw new RuntimeException("kernelDim[d] should be odd and (>= 3) for each d. For d = " + d +  " kernelDim[d] = " + kernelDim[d] + ".");
			kernelHalfDim[d] = kernelDim[d]/2; // store dim/2
		}
		
		// @Parallel : 
		final ExecutorService taskExecutor = Executors.newFixedThreadPool( numThreads );
		final ArrayList< Callable<Void> > taskList = new ArrayList< Callable< Void > >(); 
	
		// @Parallel :
		for (final ImagePortion task : divideIntoPortions(Views.iterable(src).size(), numTasks) )
			taskList.add(new MedianThread< T >( task, infSrc, src, dst, kernelHalfDim ));
		
		try
		{
			// invokeAll() returns when all tasks are complete
			// synchronization point
			 taskExecutor.invokeAll(taskList);
		}
		catch(final Exception e){
			System.out.println( "Failed to invoke all tasks" + e );
			e.printStackTrace();
		}
		taskExecutor.shutdown();	
	}

	/*
	 * copy elements from the histogram to list 
	 * */
	public final static <T extends RealType<T> & Comparable<T>> void addAll(final IterableInterval<T> histogram, final List<T> list){
		for (final T h : histogram) 
			list.add(h.copy()); 			
		Collections.sort(list);	
	}

	/*
	 * adjust the min/max values for the kernel
	 * */
	public final static void updateKernelMinMax(final long[] min, final long[] max, final long[] position, final int[] kernelHalfDim, int n){
		for (int d = 0; d < n; ++d){
			min[d] = position[d] - kernelHalfDim[d];
			max[d] = position[d] + kernelHalfDim[d];
		}
	}

	/*
	 *  add every element from histogram to the list
	 */
	public final static <T extends RealType<T> & Comparable<T>> void addElements(final RandomAccessibleInterval<T> histogram, final List<T> list){

		final List<T> histogramArray = new ArrayList<T>(); 
		final List<T> returnArray = new ArrayList<T>(); 

		addAll(Views.iterable(histogram), histogramArray);

		int i = 0; // histogramArray index
		int j = 0; // list index

		final int histogramArraySize = histogramArray.size();
		final int listSize = list.size();

		while((i != histogramArraySize)  && (j != listSize)){
			if (histogramArray.get(i).compareTo(list.get(j)) > 0)
				returnArray.add(list.get(j++));
			else{
				if (histogramArray.get(i).compareTo(list.get(j)) < 0)
					returnArray.add(histogramArray.get(i++));
				else{ // equality case
					returnArray.add(list.get(j++));
					returnArray.add(histogramArray.get(i++));
				}
			}

			// flush the rest
			if (i == histogramArraySize)
				while(j < listSize)
					returnArray.add(list.get(j++));
			if (j == listSize)
				while(i < histogramArraySize)
					returnArray.add(histogramArray.get(i++));		
		}

		list.clear();	
		for(final T h : returnArray) 
			list.add(h.copy());
	}

	/*
	 *  remove elements from the list
	 */
	public final static <T extends RealType<T> & Comparable<T>> void removeElements(final RandomAccessibleInterval<T> histogram, final List<T> list){		
		final List<T> histogramArray = new ArrayList<T>(); 
		addAll(Views.iterable(histogram), histogramArray);

		int i = 0; // histogramArray index
		int j = 0; // list index

		int histogramArraySize = histogramArray.size();
		while(i != histogramArraySize){ // iterate while we do not go through all elements in histogram
			if (histogramArray.get(i).compareTo(list.get(j)) == 0){
				list.remove(j);
				i++; 
			}
			else
				j++; 
		}			
	}	

	/**
	 * check if the cursor moved only by one pixel away
	 * @param pPos - position of the previous element
	 * @param cPos - position of the current element
	 * @param n - # of dimensions
	 * @return d+1 in which it moved, or zero if jumped to far
	 */
	public static long checkDist(final long[] pPos, final long[] cPos, final int n){
		long dim = -1;
		long dir = 0;

		for (int d = 0; d < n; ++d){
			final long dist = cPos[d] - pPos[d]; // dist > 0 if we moved forward, dist < 0 otherwise

			if (dist != 0){ // ?moved
				if((Math.abs(dist) != 1) || (dim != -1)){ //?too far or ?more than once
					return 0;
				}
				else{
					dim = d; 	// set the direction of movement
					dir = dist;  // set the step 
				}
			}
		}

		return (dim + 1)*dir;
	}

	
	/***
	 * apply median filter slice-by-slice to a 3D image
	 * @param src - input image
	 * @param dst - final image
	 * @param kernelDim - size of the kernel for median filter (2D)
	 * @param <T> - the type
	 */
	public static <T extends RealType<T> & Comparable<T>> void medianFilterSliced(final RandomAccessibleInterval< T > src, final RandomAccessibleInterval< T > dst, final int[] kernelDim){
		int zDim = 2;
		for (long z = src.min(zDim); z <= src.max(zDim); ++z){
			// System.out.println("z coordinate processed: " + z);
			medianFilter(Views.hyperSlice(src, zDim, z), Views.hyperSlice(dst, zDim, z), kernelDim);
		}
	}
	
	/**
	 * @return number of slices to process
	 * @param <T> - the type
	 * @param img - image
	 * */
	public static <T extends RealType<T> & Comparable<T>> long getNumSlices(final RandomAccessibleInterval< T > img){
		long numSlices = 1; // by default there is one slice to process
		
		// skip x and y dimensions 
		for(int d = 2; d < img.numDimensions(); d++)
			numSlices *= d;
		
		return numSlices;
	}
	
	public static void main(String [] args){
		new ImageJ(); // to have a menu!

		// File file = new File("src/main/resources/Bikesgray.jpg");
		// File file = new File("src/main/resources/salt-and-pepper.tif");
		//File file = new File("src/main/resources/noisyWoman.png");
		// File file = new File("src/main/resources/test3D.tif");
		// File file = new File("src/main/resources/inputMedian.png");
		// File file = new File("../Documents/Useful/initial_worms_pics/1001-yellow-one.tif");
		File file = new File("/home/milkyklim/Desktop/beads_tifs/cropped/tif-3-c.tif");
		final Img<FloatType> img = ImgLib2Util.openAs32Bit(file);
		final Img<FloatType> dst = img.factory().create(img, img.firstElement());
		

		final int n = img.numDimensions();
		final long[] min = new long[n];
		final long[] max = new long[n];

		// not super important 
//		FloatType minValue = new FloatType();
//		FloatType maxValue = new FloatType();
//		minValue.set(0);
//		maxValue.set(255);		
//		Normalize.normalize(img, minValue, maxValue);

		ImageJFunctions.show(img);

		// while ( min != null )
		// {
		// define the size of the filter
		int zz = 5;
		// run multiple tests
		for (int jj = zz; jj <= zz; jj += 2) {	

//			for (int d = 0; d < n; d++) {
//				min[d] = -jj;
//				max[d] = jj;
//			}
			long inT  = System.nanoTime();
			//final RandomAccessible< T > infSrc, final Interval srcInterval, final RandomAccessibleInterval< T > dst, final int[] kernelDim);
			//medianFilter(img, dst, new FinalInterval(min, max));
			medianFilter(img, dst, new int[]{jj, jj, jj});
			System.out.println("kernel = " + jj + "x" + jj + " : "+ TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inT)/1000.0);
			ImageJFunctions.show(dst);
		// }
		}

		//here comes filtering part 
		// System.out.println("Doge!");
	}	
}
