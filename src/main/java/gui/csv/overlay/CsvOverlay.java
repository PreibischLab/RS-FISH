package gui.csv.overlay;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Scrollbar;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.IntStream;

import net.imglib2.RealPoint;
import net.imglib2.util.Util;

import fiji.tool.SliceObserver;
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
	ArrayList< RealPoint > peaks;
	ArrayList< Double > intensities;
	boolean isStarted = false;
	
	float thresholdValue = 0.1111f;
	
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
		// add listener to the imageplus slice slider
		this.sliceObserver = new SliceObserver(imp, new ImagePlusListener( this ));
		// ...
		this.frame = createFrame(); 
		this.frame.setVisible( true );
		// to prevent any concurrency bugs 
		this.isStarted = true;
	}
	
	public Frame createFrame() {
		Frame frame = new Frame( "Adjust the intensity values" );
		frame.setSize(360, 80);

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		
		int scrollbarInitialPosition = 0;
		int scrollbarSize = 1000;
		int thresholdMin = 0, thresholdMax = 5;
				
		final Scrollbar thresholdBar = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarInitialPosition, 1, 0, scrollbarSize + 1);
		final Label thresholdBarText = new Label("Intensity = " + String.format(java.util.Locale.US, "%.2f", 0.0), Label.CENTER);
		
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
//		int bInTop = 0;
//		int bInRight = 120;
//		int bInBottom = 0;
//		int bInLeft = bInRight;
		
		/* On screen positioning */
		/* Screen positioning */
		int xOffset = 20; 
		int yOffset = 20;
		frame.setLocation(xOffset, yOffset);
		
		thresholdBar.addAdjustmentListener(new ThresholdListener(this, thresholdBarText, thresholdMin, thresholdMax, scrollbarSize));
		
		return frame;
	}


	public static ArrayList< RealPoint > readAndSortPositionsFromCsv(File csvPath, char separator) {
		ArrayList<RealPoint> peaks = IOUtils.readPositionsFromCSV(csvPath, separator);
		Collections.sort(peaks, new CustomComparators().new PosComparator());
		return peaks;
	}

	public static ArrayList< Double > readAndSortIntensitiesFromCsv(File csvPath, char separator, int n) {
		ArrayList<RealPoint> peaks = IOUtils.readPositionsFromCSV(csvPath, separator);
		ArrayList<Double> intensities = IOUtils.readIntensitiesFromCSV(csvPath, separator, n);
		
		int [] sortedIndices = IntStream.range(0, peaks.size()).boxed()
				.sorted((i, j) -> new Double(peaks.get(i).getDoublePosition(n - 1)).compareTo(peaks.get(j).getDoublePosition(n - 1)))
				.mapToInt(ele -> ele).toArray();
		
		ArrayList<Double> sortedIntensities = new ArrayList<>(intensities.size());
		for (int j = 0; j < sortedIndices.length; j++) {
			sortedIntensities.add(intensities.get(sortedIndices[j]));
			// sortedIntensities.set(j, intensities.get(sortedIndices[j]));
		}
		
		return sortedIntensities;
	}
	
	public boolean isStarted() {
		return isStarted;
	}

	// IMP: we only work with 3D-images {x, y, z} for now
	public void updatePreview() {
		Overlay overlay = setOverlay(imp);
		addSpotsOverlay(imp, overlay, peaks, intensities, thresholdValue);
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
	
	public static void addSpotsOverlay(ImagePlus imp, Overlay overlay, ArrayList <RealPoint> peaks, ArrayList <Double>intensities, float thresholdValue) {
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
