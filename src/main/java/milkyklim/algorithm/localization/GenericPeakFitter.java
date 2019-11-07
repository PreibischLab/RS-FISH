/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
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

package milkyklim.algorithm.localization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.algorithm.localization.FitFunction;
import net.imglib2.algorithm.localization.FunctionFitter;
import net.imglib2.algorithm.localization.Observation;
import net.imglib2.algorithm.localization.StartPointEstimator;
import net.imglib2.type.numeric.RealType;

/**
 * @author Jean-Yves Tinevez - 2013
 */
public class GenericPeakFitter <T extends RealType<T>, L extends Localizable > implements MultiThreaded, OutputAlgorithm<Map<L, double[]>>, Benchmark {

	private static final String BASE_ERROR_MESSAGE = "PeakFitter: ";

	private final ObservationGatherer< L > gatherer;
	private final ArrayList<L> peaks;
	private final FunctionFitter fitter;
	private final FitFunction peakFunction;
	private final StartPointEstimator estimator;
	private ConcurrentHashMap<L, double[]> results;
	private int numThreads;
	private final StringBuffer errorHolder = new StringBuffer();

	private long processingTime;




	/*
	 * CONSTRUCTOR
	 */

	/**
	 * @param image the image to operate on.
	 */
	public GenericPeakFitter(final RandomAccessibleInterval<T> image, Collection< L > peaks, FunctionFitter fitter, FitFunction peakFunction, StartPointEstimator estimator) {

		this(
				new WindowObservationGatherer< T, L >( image, estimator.getDomainSpan() ),
				peaks,
				fitter,
				peakFunction, estimator );
	}

	public GenericPeakFitter(final ObservationGatherer< L > observationGatherer, Collection< L > peaks, FunctionFitter fitter, FitFunction peakFunction, StartPointEstimator estimator) {
		this.gatherer = observationGatherer;
		this.fitter = fitter;
		this.peakFunction = peakFunction;
		this.estimator = estimator;
		this.peaks = new ArrayList<>();
		this.peaks.addAll( peaks );
		setNumThreads();
	}

	/*
	 * METHODS
	 */

	@Override
	public String toString() {
		return "PeakFitter configured to:\n" +
				" - fit a " + peakFunction.toString() + "\n" +
				" - on " + peaks.size() + " peaks\n" +
				" - using " + estimator.toString() + "\n" +
				" - and " + fitter.toString() + "\n" +
				" - allocating " + numThreads + " threads.";
	}



	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public String getErrorMessage() {
		return errorHolder.toString();
	}

	@Override
	public boolean process() {
		
		long start = System.currentTimeMillis();
		results = new ConcurrentHashMap<>(peaks.size());

		ExecutorService workers = Executors.newFixedThreadPool(numThreads);
		for (final L peak : peaks) {
			Runnable task = new Runnable() {

				@Override
				public void run() {
					Observation data = gatherer.gatherObservationData( peak ); // LocalizationUtils.gatherObservationData(image, peak, padSize);
					double[] params = estimator.initializeFit(peak, data);
					try {
						double[][] X = data.X;
						double[] I = data.I;
						fitter.fit(X, I, params, peakFunction);
					} catch (Exception e) {
						errorHolder.append(BASE_ERROR_MESSAGE + 
								"Problem fitting around " + peak +
								": " + e.getMessage() + ".\n");
					}
					results.put(peak, params);
				}

			};
			workers.execute(task);
		}

		workers.shutdown();
		boolean ok = true;
		try {
			ok = workers.awaitTermination(1, TimeUnit.HOURS);
			if (!ok) {
				System.err.println("Timeout reached while processing.");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
		
		long end = System.currentTimeMillis();
		processingTime = end - start;
		
		return ok;
	}

	@Override
	public Map<L, double[]> getResult() {
		return results;
	}

	@Override
	public void setNumThreads() {
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads() {
		return numThreads;
	}

	@Override
	public long getProcessingTime() {
		return processingTime;
	} 
}
