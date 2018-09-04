package cluster.process.start;

import cluster.radial.symmetry.process.RadialSymmetryBothSteps;

public class ClusterProcessRadialSymmetryBothSteps {

	public static void main(String[] args) {	
		if (args.length != 5) {
			System.out.println("You have to provide exactly 5 arguments!");
		}
		else {
			String root = args[0]; // "/Users/kkolyva/Desktop/2018-07-31-09-53-32-N2-all-results-together/test";
			String channelFilename = args[1]; // "C2-N2_395";
			String experimentType = args[2]; // "N2";
			int step = Integer.parseInt(args[3]); // 2;
			int waveLength = Integer.parseInt(args[4]); // 670;
			
			for (int i = 0; i < args.length; i++)
				System.out.println(args[i]);
			
			RadialSymmetryBothSteps.runFullProcess1Step1Image(root, channelFilename, experimentType, step, waveLength);
		}
						
		System.out.println("Doge!");
	}

}
