package cluster.process.start;

import scripts.radial.symmetry.process.ProcessIntronsAndDapi;

public class ClusterProcessIntronsAndDapi {
	public static void main(String [] args){
		
		if (args.length != 4) {
			System.out.println("You have to provide exactly 4 arguments!");
		}
		else {
			String root = args[0];
			String exonFilename = args[1];
			String intronFilename = args[2];
			String dapiFilename = args[3];
			
			for (int i = 0; i < args.length; i++)
				System.out.println(args[i]);
			
			ProcessIntronsAndDapi.processOneTriplet(root, exonFilename, intronFilename, dapiFilename);
		}
						
		System.out.println("Doge!");
	}
}
