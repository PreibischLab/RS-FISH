// This macro script runs the radial symmetry plug-in on all the images in all the sub-directories of the defined dir
// For each image it iterates over all looped parameters and runs the radialSymmetry detection with those params.

// Get args from the shell command:
// To run in paralell on cluster - sig,thr,suppReg are given args
#@Float sig
#@Float thr
#@Float suppReg

// Path the tif files to be processed, searches all sub-directories.
// If you cloned the RadialSymmetry repo as is, the path should be correct:
dir = "../../../documents/Simulation_of_data/Simulated data/";

// Location of file where all run times will be saved:
timeFile = "/path/to/dir/exeTimeRSgridSearch_sig" + sig + "thr" + thr + "suppReg" + suppReg + ".txt";


setBatchMode(true);

//////// Define grid search parameters: //////////
// start, end, step (inclusive)
// sig_range = newArray(1,2.5,0.25); // 5 steps /// Not Used - Given as arg
// thr_range_xstep = newArray(0.001,0.13,1.50); // 13 steps - multiplication! /// Not Used - Given as arg
// suppReg_range = newArray(2,4,1); // 4 steps /// Not Used - Given as arg
inRat_range = newArray(0.0,0.3,0.1); // 7 steps
maxErr_range = newArray(0.1,2.6,0.5); // 6 steps
intenThr_vals = newArray(0,100,150,200); // 4 steps // thats the actual array

///////////////////////////////////////////////////


walkFiles(dir);

// Find all files in subdirs:
function walkFiles(dir) {
	list = getFileList(dir);
	for (i=0; i<list.length; i++) {
		if (endsWith(list[i], "/"))
		   walkFiles(""+dir+list[i]);

		// If image file
		else  if (endsWith(list[i], ".tif")) 
		   gridProcessImage(dir, list[i]);
	}
}


function gridProcessImage(dirPath, imName) {
	
	open("" + dirPath + imName);


	// Define anisotropy parameter:
	if (matches(dirPath, ".*Sigxy 1 SigZ 2.*")) {
		aniso = 0.5;
	} else if (matches(dirPath, ".*Sigxy 1pt5 SigZ 2.*")) {
		aniso = 0.75;
	} else if (matches(dirPath, ".*Sigxy 2 SigZ 2.*")) {
		aniso = 1;
	} else if (matches(dirPath, ".*Sigxy 1pt35 SigZ 2.*")) {
		aniso = 0.675;
	} else if (matches(dirPath, ".*FISH.*")) {
		aniso = 0.714;
	} else if (matches(dirPath, ".*new_embryo.*")) {
		aniso = 0.714;
	}

	// Loop over parameters:
	for (inRat=inRat_range[0]; inRat<=inRat_range[1]; inRat=inRat+inRat_range[2]) {
		for (maxErr=maxErr_range[0]; maxErr<=maxErr_range[1]; maxErr=maxErr+maxErr_range[2]) {
			for (iit=0; iit<intenThr_vals.length; iit++) {

				intesThr = intenThr_vals[iit];

				results_csv_path = "" + dirPath + "RadialSymmetry_results_" + imName + "_aniso" + aniso + 
				"sig" + sig +
				"thr" + thr + 
				"suppReg" + suppReg + 
				"inRat" + inRat +
				"maxErr" + maxErr + 
				"intensThr" + intesThr + 
				".txt";
				
				RSparams = "image=" + imName + 
				" mode=Advanced anisotropy=" + aniso + " use_anisotropy" +
				" robust_fitting=RANSAC" +
				" sigma=" + sig + 
				" threshold=" + thr + 
				" support=" + suppReg + 
				" min_inlier_ratio=" + inRat +  
				" max_error=" + maxErr +
				" spot_intensity_threshold=" + intesThr +
				" background=[No background subtraction]" +
				" results_file=[" + results_csv_path + "]";

				print(RSparams);

				startTime = getTime();
				run("Radial Symmetry", RSparams);
				exeTime = getTime() - startTime; //in miliseconds
				
				// Save exeTime to file:
				File.append(results_csv_path + "," + exeTime + "\n ", timeFile);
			}
		}
	}


	// Close all windows:
	run("Close All");	
	while (nImages>0) { 
		selectImage(nImages); 
		close(); 
    } 
} 
