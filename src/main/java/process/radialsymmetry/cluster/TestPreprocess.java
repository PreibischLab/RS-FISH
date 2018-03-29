package process.radialsymmetry.cluster;

import java.io.File;
import java.io.IOException;

import net.imagej.Dataset;

import gui.radial.symmetry.plugin.Radial_Symmetry;

public class TestPreprocess {
	public static void main(String[] args) {
		Preprocess.runPreprocess(new File("/Volumes/1TB/2018-03-20-laura-radial-symmetry-numbers/SEA-12-channels-correct"), 
														new File("/Volumes/1TB/2018-03-20-laura-radial-symmetry-numbers/SEA-12-channels-correct-csv"), 
														new File("/Volumes/1TB/2018-03-20-laura-radial-symmetry-numbers/smFISH-database/SEA-12-Table 1.csv")); 
		
		System.out.println("Doge!");
	}
}
