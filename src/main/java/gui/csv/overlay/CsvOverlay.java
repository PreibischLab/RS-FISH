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
	SliceObserver sliceObserver;
	ArrayList< RealPoint > peaks;
	boolean isStarted = false;

	public CsvOverlay(ImagePlus imp, File csvPath) {
		this.imp = imp;
		this.csvPath = csvPath;
		// TODO: detect the separator automatically
		this.peaks = readAndSortPositionsFromCsv(csvPath);
		// add listener to the imageplus slice slider
		sliceObserver = new SliceObserver(imp, new ImagePlusListener( this ));
		// to prevent any concurrency bugs 
		this.isStarted = true;
	}

	public static ArrayList< RealPoint >  readAndSortPositionsFromCsv(File csvPath) {
		ArrayList<RealPoint> peaks = IOUtils.readPositionsFromCSV(csvPath);
		Collections.sort(peaks, new CustomComparators().new PosComparator());
		return peaks;
	}

	public boolean isStarted() {
		return isStarted;
	}

	// IMP: we only work with 3D-images {x, y, z} for now
	public void updatePreview() {
		Overlay overlay = setOverlay(imp);
		addSpotsOverlay(imp, overlay, peaks);
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
	
	public static void addSpotsOverlay(ImagePlus imp, Overlay overlay, ArrayList <RealPoint> peaks) {
		int curSlice = imp.getZ();
		int[] indices = new int[2];

		indices[0] = 0;
		indices[1] = peaks.size();

		int numDimensions = peaks.iterator().next().numDimensions();
		final double [] loc = new double[numDimensions];
		
		for (int curPeakIdx = indices[0]; curPeakIdx < indices[1]; curPeakIdx++) {
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
