package roi.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import fit.Spot;
import ij.ImageJ;
import ij.gui.Roi;
import ij.io.RoiDecoder;

public class RoiProcess {

	// create a list of corresponding roi indices
	public static void processRoi(ArrayList<Spot> allSpots, ArrayList<Roi> roiList, int[] roiIndices) {
		int idx = 0;
		int roiIdx = 0; // global index
		for (Spot spot : allSpots) {
			for (Roi roi : roiList) {
				double[] pos = new double[spot.numDimensions()];
				pos = spot.getCenter();
				if (roi.contains((int) pos[0], (int) pos[1])) {
					roiIndices[roiIdx] = idx;
					break;
				}
				idx++;
			}
			idx = 0;
			roiIdx++;
		}
	}

	// roi format is name as image + (-0, -1, -2, -3 )
	public static ArrayList<Roi> readRoiList(String roiFolderPath) {
		ArrayList<Roi> roiList = new ArrayList<>();
		File roiFolder = new File(roiFolderPath);

		try{
		
		for (final File roiFile : roiFolder.listFiles()) {
			if (roiFile.isFile()) {
				// System.out.println(roiFile.getName());
				final RoiDecoder roiDecoder = new RoiDecoder(roiFile.getAbsolutePath());
				Roi roi = null;
				try {
					roiList.add(roiDecoder.getRoi());
				} catch (IOException e) {
					System.out.println("Check the link to the ROI folder!");
					e.printStackTrace();
				}
			}
		}
		}
		catch(Exception e){
			System.out.println("Exception caught");
			System.out.println("roiFolderPath:" + roiFolderPath);
		}
		return roiList;
	}

	public static void main(String[] args) {
		new ImageJ();

		readRoiList("/home/milkyklim/Desktop/RoiSet");

	}
}