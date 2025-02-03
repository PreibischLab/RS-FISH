// This macro script runs the RS (radial symmetry) FIJI plug-in on all the images in all the sub-directories of the defined dir
// After finding the best parameters using the RS plugin GUI interactive mode on one example image,
// You can run this macro script on the entire dataset.
// Just change the directory path, and the values of the parameters in the begining of the script

// You can run this script either in the ImageJ GUI or headless (also from cluster) using this command (linux):
// <FIJI/DIR/PATH>/ImageJ-linux64 --headless --run </PATH/TO/THIS/SCRIPT>/RS_macro.ijm &> </PATH/TO/WHERE/YOU/WANT/YOUR/LOGFILE>.log

// The detection result table will be saved to the same directory as each image it was calculated for.

// Path the tif files to be processed, searches all sub-directories.
dir = "<PATH/TO/YOUR/TIFFILES/PARENT/DIR/>";

// Location of file where all run times will be saved:
timeFile = "</PATH/TO/DIR>/RS_Exe_times.txt";


//////// Define RS parameters: //////////

anisotropyCoefficient = 1.4; 
ransac = "RANSAC";						// options are "RANSAC" (log value "SIMPLE") / "No RANSAC" / "MULTICONSENSU"
imMin = 190; 							// img min intensity
imMax = 255; 							// max intensity
sigmaDoG = 1.4; 
thresholdDoG = 0.007;
supportRadius = 2;
inlierRatio = 0.0;						// meaning: min inlier ratio
maxError = 0.9; 						// meaning: max error range
intensityThreshold = 0;  				// meaning: spot intensity threshold
bsMethod = "No background subtraction";	// Log file 0 / 1 / 2 / 3 / 4 options correspond to "No background subtraction" / "Mean" / "Median" / "RANSAC on Mean" / "RANSAC on Median"
bsMaxError = 0.05;						// background subtraction param
bsInlierRatio = 0.0;					// background subtraction param
useMultithread = "";	// Not from log file (only in advanced mode)! If you wish to use multithreading "use_multithreading", else "" (empty string)
numThreads = 40;						// multithread param
blockSizX = 128;                     	// multithread param
blockSizY = 128;						// multithread param
blockSizZ = 16;							// multithread param


///////////////////////////////////////////////////

ransac_sub = split(ransac, ' ');
ransac_sub = ransac_sub[0];

bsMethod_sub = split(bsMethod, ' ');
bsMethod_sub = bsMethod_sub[0];

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

	results_csv_path = "" + dirPath + "RSresults_" + imName + 
	"_an" + anisotropyCoefficient + 
	"ran" + ransac_sub + 
	"mn" + imMin +
	"mx" + imMax +
	"s" + sigmaDoG +
	"t" + thresholdDoG + 
	"sR" + supportRadius + 
	"iR" + inlierRatio +
	"mE" + maxError + 
	"iT" + intensityThreshold + 
	"bsM" + bsMethod_sub + 
	"bsMxE" + bsMaxError + 
	"bsIR" + bsInlierRatio +
	".csv";


	RSparams =  "image=" + imName + 
	" mode=Advanced anisotropy=" + anisotropyCoefficient + " robust_fitting=[" + ransac + "] use_anisotropy" + 
	" image_min=" + imMin + " image_max=" + imMax + " sigma=" + sigmaDoG + " threshold=" + thresholdDoG + 
	" support=" + supportRadius + " min_inlier_ratio=" + inlierRatio + " max_error=" + maxError + " spot_intensity_threshold=" + intensityThreshold + 
	" background=[" + bsMethod + "] background_subtraction_max_error=" + bsMaxError + " background_subtraction_min_inlier_ratio=" + bsInlierRatio + 
	" results_file=[" + results_csv_path + "]" + 
	" " + useMultithread + " num_threads=" + numThreads + " block_size_x=" + blockSizX + " block_size_y=" + blockSizY + " block_size_z=" + blockSizZ;

	print(RSparams);

	startTime = getTime();
	run("RS-FISH", RSparams);
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
