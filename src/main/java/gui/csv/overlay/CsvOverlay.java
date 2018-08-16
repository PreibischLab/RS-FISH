package gui.csv.overlay;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import net.imglib2.RealPoint;

import ij.ImagePlus;
import radial.symmetry.utils.CustomComparators;
import radial.symmetry.utils.IOUtils;

public class CsvOverlay {

		ImagePlus imp;
		File csvPath;
		
		ArrayList< RealPoint > peaks;
		
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
		
		// TODO: USE THE CODE FROM DETECTIONS
		// FIXME: adjust this one to this case; copied from Detections.java 
		public void updatePreview(int currentSlice) {
			this.currentSlice = currentSlice;
			
			System.out.println("Slice updated!");

//			Overlay overlay = imp.getOverlay(); // contains marked spots
//
//			if (overlay == null) {
//				overlay = new Overlay();
//				imp.setOverlay(overlay);
//			}
//
//			overlay.clear();
//			
//			
//			// what do we do here? 
//			// - grab the index of the current slice 
//			// - create the overlay to fill in 
//			// - iterate over the +/- slice_id and put the dots in the overlay
//			
//
//			int zSlice = imp.getZ();
//			int tSlice = imp.getT();
//			int[] indices = new int[2];
//
//			// image has z ?
//			if (imp.getDimensions()[3] != 1) {
//				// FIXME: should work in 2D + time, too
//				indices = findIndices(zSlice, sigma, anisotropy); // contains indices of the lower and upper slices
//			} else {
//				indices[0] = 0;
//				indices[1] = peaks.size();
//			}
//
//			if (indices[0] >= 0 && indices[1] >= 0) {
//				for (int curPeakIdx = indices[0]; curPeakIdx < indices[1]; curPeakIdx++) {
//					if (timePointIndices.get(curPeakIdx) + 1 == tSlice) { // filter corresponding time points
//						if (intensity.get(curPeakIdx) > threshold) { // this one should be the comparison with the current
//																		// peak intensity
//							double[] peak = peaks.get(curPeakIdx);
//
//							final double x = peak[0];
//							final double y = peak[1];
//
//							float initRadius = sigma + 1; // make radius a bit larger than DoG sigma
//							float radius = initRadius;
//
//							if (numDimensions == 3) {
//								final long z = (long) peak[2];
//								radius -= Math.abs(z - (zSlice - 1));
//							}
//
//							final OvalRoi or = new OvalRoi(x - radius + 0.5, y - radius + 0.5, radius * 2, radius * 2);
//							or.setStrokeColor(new Color(255, 0, 0));
//							overlay.add(or);
//						}
//					}
//				}
//			}
//
//			imp.updateAndDraw();
//			isComputing = false;
		}
}
