// This macro script runs the radial symmetry (RS) FIJI plug-in on all the images in all the sub-directories of the defined dir
// After finding the best parameters using the RS plugin GUI interactive mode on one example image,
// You can run this macro script on the entire dataset.
// Just change the directory path, and the values of the parameters in the begining of the script

// You can run this script either in the ImageJ GUI or headless (also from cluster) using this command (linux):
// fiji_dir_path/ImageJ-linux64 --headless --run /path/to/this/script/RS_macro.ijm &> /path/to/where/you/want/yourlogfile.log

// The detection result table will be saved to the same directory as each image it was calculated for.

// Path the tif files to be processed, searches all sub-directories.
dir = "path/to/your/tiffiles/parent/dir/";

// Location of file where all run times will be saved:
timeFile = "/path/to/dir/RS_Exe_times.txt";


//////// Define RS parameters: //////////

aniso = 0.714; 		// anisotropy
sig = 1.5; 			// sigma
thr = 0.5; 			// threshold
suppReg = 2; 		// support
inRat = 0.2			// min_inlier_ratio
maxErr_range = 0.6; // max_error
intesThr = 0;  		// spot_intensity_threshold

setBatchMode(true);

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
		   processImage(dir, list[i]);
	}
}


function processImage(dirPath, imName) {
	
	open("" + dirPath + imName);

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

	// Close all windows:
	run("Close All");	
	while (nImages>0) { 
		selectImage(nImages); 
		close(); 
    } 
} 
