package gui.csv.overlay;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import net.imglib2.RealPoint;

import fiji.tool.SliceObserver;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import radial.symmetry.utils.CustomComparators;
import radial.symmetry.utils.IOUtils;

public class CsvOverlay {

	ImagePlus imp;
	File csvPath;

	ArrayList< RealPoint > peaks;

	SliceObserver sliceObserver;
	// TODO: is it necessary to have this one as a field
	int currentSlice;

	// TODO: these 2 might be redundant
	boolean isStarted;
	boolean isComputing;

	public CsvOverlay(ImagePlus imp, File csvPath) {
		this.imp = imp;
		this.csvPath = csvPath;

		// TODO: detect the separator automatically
		// this.peaks = IOUtils.readPositionsFromCSV(csvPath, '\t');
		this.peaks = readAndSortPositionsFromCsv(csvPath, '\t');

		this.isStarted = false;
		this.isComputing = false;

		// TODO: this might be wrong
		this.currentSlice = imp.getSlice(); 
		// TODO: do we need a global overlay, too? 

		// add listener to the imageplus slice slider
		sliceObserver = new SliceObserver(imp, new ImagePlusListener( this ));

		// to prevent any concurrency bugs 
		isStarted = true;
	}

	// DEBUG: remove once done
	public void test() {
		for (RealPoint p : peaks) {
			for (int d = 0; d < peaks.get(0).numDimensions(); d++)
				System.out.print(p.getDoublePosition(d) + " ");
			System.out.println();
		}
	}

	public ArrayList< RealPoint >  readAndSortPositionsFromCsv(File csvPath, char separator) {
		ArrayList<RealPoint> peaks = IOUtils.readPositionsFromCSV(csvPath, separator);
		Collections.sort(peaks, new CustomComparators().new PosComparator());
		return peaks;
	}

	public void showCurrentSliceOverlay() {

	}

	// TODO: these 2 might be redundant
	public boolean isStarted() {
		return isStarted;
	}
	public boolean isComputing() {
		return isComputing;
	}

	// IMP: we only work with 3D-images {x, y, z} for now
	public void updatePreview() {

		Overlay overlay = imp.getOverlay(); // contains marked spots

		if (overlay == null) {
			overlay = new Overlay();
			imp.setOverlay(overlay);
		}

		overlay.clear();

		// what do we do here? 
		// - grab the index of the current slice 
		// - create the overlay to fill in 
		// - iterate over the +/- slice_id and put the dots in the overlay

		int curSlice = imp.getZ();
		int[] indices = new int[2];

		indices[0] = 0;
		indices[1] = peaks.size();

		// TODO: why do I need this check 
		if (indices[0] >= 0 && indices[1] >= 0) {
			for (int curPeakIdx = indices[0]; curPeakIdx < indices[1]; curPeakIdx++) {

				// peak intensity
				RealPoint peak = peaks.get(curPeakIdx);

				final double x = peak.getDoublePosition(0);
				final double y = peak.getDoublePosition(1);

				// TODO: make it adaptive?
				float initRadius = 4 + 1; // make radius a bit larger than DoG sigma
				float radius = initRadius;

				final long z = (long) peak.getDoublePosition(2);
				radius -= Math.abs(z - (curSlice - 1));

				final OvalRoi or = new OvalRoi(x - radius + 0.5, y - radius + 0.5, radius * 2, radius * 2);
				or.setStrokeColor(new Color(255, 0, 0));
				overlay.add(or);

			}
		}

		imp.updateAndDraw();
		isComputing = false;
	}
}
