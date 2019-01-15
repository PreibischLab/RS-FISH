package gui.csv.overlay;

import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import net.imglib2.RealPoint;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.util.Util;

import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.axis.NumberAxis;

import fiji.tool.SliceObserver;
import gui.radial.symmetry.histogram.Histogram;
import gui.radial.symmetry.histogram.MouseListenerValue;
import gui.radial.symmetry.interactive.HelperFunctions;
import gui.radial.symmetry.vizualization.Detections;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import radial.symmetry.utils.CustomComparators;
import radial.symmetry.utils.IOUtils;
import util.NotSoUsefulOutput;

public class CsvOverlay {
	ImagePlus imp;
	File csvPath;

	final Frame frame; // anisotropy frame 
	SliceObserver sliceObserver;
	Detections detection;
	Histogram demo;

	ArrayList< RealPoint > peaks;
	ArrayList< Float > intensities;
	boolean isStarted = false;
	boolean isFinished = false;
	
	float thresholdValue = 0.1111f;
	// public static double histThreshold = 0;

	public void setThresholdValue(float t) {
		this.thresholdValue = t;
	}

	public float getThresholdValue() {
		return this.thresholdValue;
	}

	public CsvOverlay(ImagePlus imp, File csvPath) {
		this.imp = imp;
		this.csvPath = csvPath;
		// TODO: detect the separator automatically
		char separator = '\t';
		this.peaks = readAndSortPositionsFromCsv(csvPath, separator);
		int numDimensions = peaks.iterator().next().numDimensions();
		this.intensities = readAndSortIntensitiesFromCsv(csvPath, separator, numDimensions);
		// TODO: timepoints should contain the number of consecutive points that belong to this timepoint
		float sigma = 5; 
		// TODO: is it actually used?
		float anisotropy = 1.00f;
		// HelperFunctions.localizablePeaks(this.peaks);
		detection = new Detections(imp, peaks, this.intensities, sigma, anisotropy);
		detection.showDetections();
		// do not show histogram if you have zero spots
		if (peaks.size() != 0){
			final List< Double > values = new ArrayList< Double >(this.intensities.size());

			for (final Float i : intensities )
				values.add(i.doubleValue());
			// TODO: make this parameter dynamic ?
			int numBins = 100;

			this.demo = new Histogram( values, numBins, "Intensity distribution", "", this.detection);
			this.demo.getChartPanel().getChart().getXYPlot().getDomainAxis().setRange(0, 5);
			// (NumberAxis) chart.getXYPlot().getDomainAxis()
			this.demo.showHistogram();

			thresholdValue = (float)demo.getHistThreshold();
			// histThreshold = demo.getHistThreshold();
		}
		
		// add listener to the imageplus slice slider
		this.sliceObserver = new SliceObserver(imp, new ImagePlusListener( this ));
		// ...
		this.frame = createFrame(); 
		this.frame.setVisible( true );

		// to prevent any concurrency bugs 
		this.isStarted = true;
		
		do {
			// TODO: change to something that is not deprecated
			SimpleMultiThreading.threadWait(100);
		} while (!demo.isFinished());
		
	}

	public Frame createFrame() {
		Frame frame = new Frame( "Adjust the intensity values" );
		frame.setSize(620, 80);

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();

		int scrollbarSize = 1000;
		int thresholdMin = 0, thresholdMax = 5;
		int scrollbarInitialPosition = (int) (0 + (thresholdValue / thresholdMax) * (0 + scrollbarSize + 1));

		final Scrollbar thresholdBar = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarInitialPosition, 1, 0, scrollbarSize + 1);
		final Label thresholdBarText = new Label("Intensity = " + String.format(java.util.Locale.US, "%.2f", thresholdValue), Label.CENTER);

		final Button button = new Button("Done");
		
		/* Location */
		frame.setLayout(layout);

		// insets constants
		//		int inTop = 0;
		//		int inRight = 5;
		//		int inBottom = 0;
		//		int inLeft = inRight;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		frame.add(thresholdBarText, c);

		++c.gridy;
		frame.add(thresholdBar, c);

		// insets for buttons
		int bInTop = 0;
		int bInRight = 120;
		int bInBottom = 0;
		int bInLeft = bInRight;
		
		++c.gridy;
		c.insets = new Insets(bInTop, bInLeft, bInBottom, bInRight);
		frame.add(button, c);

		/* On screen positioning */
		/* Screen positioning */
		int xOffset = 20; 
		int yOffset = 20;
		frame.setLocation(xOffset, yOffset);

		thresholdBar.addAdjustmentListener(new ThresholdListener(this, thresholdBarText, thresholdMin, thresholdMax, scrollbarSize));
		button.addActionListener(new FinishedButtonListener(this, isFinished));
		
		return frame;
	}

	public static ArrayList< RealPoint > readAndSortPositionsFromCsv(File csvPath, char separator) {
		ArrayList<RealPoint> peaks = IOUtils.readPositionsFromCSV(csvPath, separator);
		Collections.sort(peaks, new CustomComparators().new PosComparator());
		return peaks;
	}

	public static ArrayList< Float > readAndSortIntensitiesFromCsv(File csvPath, char separator, int n) {
		ArrayList<RealPoint> peaks = IOUtils.readPositionsFromCSV(csvPath, separator);
		ArrayList<Double> intensities = IOUtils.readIntensitiesFromCSV(csvPath, separator, n);

		int [] sortedIndices = IntStream.range(0, peaks.size()).boxed()
				.sorted((i, j) -> new Double(peaks.get(i).getDoublePosition(n - 1)).compareTo(peaks.get(j).getDoublePosition(n - 1)))
				.mapToInt(ele -> ele).toArray();

		ArrayList<Float> sortedIntensities = new ArrayList<>(intensities.size());
		for (int j = 0; j < sortedIndices.length; j++) {
			sortedIntensities.add(intensities.get(sortedIndices[j]).floatValue());
			// sortedIntensities.set(j, intensities.get(sortedIndices[j]));
		}

		return sortedIntensities;
	}

	public boolean isStarted() {
		return isStarted;
	}
	
	protected final void dispose()
	{
		// clear the image
		if ( imp != null) {
			imp.getOverlay().clear();
			imp.updateAndDraw();
		}
		
		// close the scroll bar 
		if (sliceObserver != null)
			sliceObserver.unregister();
		
		// close the histogram here
		if (demo != null)
			demo.dispose();
		
		if (frame != null)
			frame.dispose();
			
		isFinished = true;
	}

	// IMP: we only work with 3D-images {x, y, z} for now
	public void updatePreview() {
		Overlay overlay = setOverlay(imp);
		addSpotsOverlay(imp, overlay, peaks, intensities, thresholdValue);
		// move the threshold line on the histogram
		((MouseListenerValue) demo.getChartPanel().getListeners(ChartMouseListener.class)[0]).scrollbarChanged(thresholdValue);
		imp.updateAndDraw();
	}

	public static Overlay setOverlay(ImagePlus imp) {
		Overlay overlay = imp.getOverlay(); // contains marked spots
		if (overlay == null) {
			overlay = new Overlay();
			imp.setOverlay(overlay);
		}
		overlay.clear();
		return overlay;
	}

	public static void addSpotsOverlay(ImagePlus imp, Overlay overlay, ArrayList <RealPoint> peaks, ArrayList <Float>intensities, float thresholdValue) {
		int curSlice = imp.getZ();
		int[] indices = new int[2];

		indices[0] = 0;
		indices[1] = peaks.size();

		int numDimensions = peaks.iterator().next().numDimensions();
		final double [] loc = new double[numDimensions];

		for (int curPeakIdx = indices[0]; curPeakIdx < indices[1]; curPeakIdx++) {
			if (intensities.get(curPeakIdx) > thresholdValue) {
				RealPoint peak = peaks.get(curPeakIdx);
				peak.localize(loc);
				// TODO: make radius adaptive
				double initRadius = 5; 
				double radius = initRadius - Math.abs(loc[2] - (curSlice - 1));
				final OvalRoi or = new OvalRoi(loc[0] - radius + 0.5, loc[1] - radius + 0.5, radius * 2, radius * 2);
				or.setStrokeColor(new Color(255, 0, 0));
				overlay.add(or);
			}
		}
	}

}
