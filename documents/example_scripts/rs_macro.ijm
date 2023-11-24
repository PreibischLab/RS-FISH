// This macro script runs the RS (radial symmetry) FIJI plug-in on all the images of the selected dir.
// After finding the best parameters using the RS plugin GUI interactive mode on one example image, you can run this macro script on the entire dataset.
// Just give the script the log of your interactive run (for parameter setup).
// The detection result table will be saved to the same directory as each image it was calculated for.
// If image is not a tif please run this macro in the imagej GUI and not headless.

// Headless (also from cluster) using this command (linux):
// <FIJI/DIR/PATH>/ImageJ-linux64 --headless --run </PATH/TO/THIS/SCRIPT>/RS_macro.ijm "dir='<PATH/TO/YOUR/IMAGE/DIRECTORY>',paramFile='<PATH/TO/RS/LOG/FILE>',channel=<0/NUM>,sliceNumber=<0/NUM>,fileExtension='<.EXT>'" &> </PATH/TO/WHERE/YOU/WANT/YOUR/LOGFILE>.log

// Path the tif files to be processed, searches all sub-directories.
#@File dir(label="Select an input directory", style="directory")
#@File paramFile(label="Select a RSFISH logfile for  parameter extraction", style="file")
#@int channel(label="Channel number (0 if not relevant)", value=0)
#@int sliceNumber(label="slice number (0 if not relevant)", value=0)
#@String fileExtension(label="Image file extension (e.g. .tif)", value=".tif")

//////// Define RS parameters: //////////

fileContents = File.openAsString(paramFile);
lines = split(fileContents, "\n"); 

for (i = 0; i < lines.length; i++) {
    if (matches(lines[i], ".*:.*")) {
    	
    	parts = split(lines[i], ":");
    	label = parts[0].trim();
    	value = parts[1].trim();
    	
    	if (label=="SigmaDoG") {
    		sigmaDoG = parseFloat(value);
    	} else if (label=="ThresholdDoG") {
    		thresholdDoG = parseFloat(value);
    	} else if (label=="anisotropyCoefficient") {
    		anisotropyCoefficient = parseFloat(value);
    	} else if (label=="RANSAC") {
    		if (value=="SIMPLE") {
    			ransac = "RANSAC";
    		} else if (label=="No RANSAC") { //need to verify that that's the output
    			ransac = "No RANSAC";
    		} else {
    			ransac = "MULTICONSENSU"
    		}
    	} else if (label=="MaxError") {
    		maxError = parseFloat(value);
    	} else if (label=="InlierRatio") {
    		inlierRatio = parseFloat(value);
    	} else if (label=="supportRadius") {
    		supportRadius = parseFloat(value);	
       	} else if (label=="min intensity") {
    		imMin = parseFloat(value);
       	} else if (label=="max intensity") {
    		imMax = parseFloat(value);	
       	} else if (label=="bsMethod") {
    		bsMethod = value;
    	} else if (label=="bsMaxError") {
    		bsMaxError = parseFloat(value);
    	} else if (label=="bsInlierRatio") {
    		bsInlierRatio = parseFloat(value);
    	} else if (label=="intensityThreshold") {
    		intensityThreshold = parseFloat(value);
    	} 
		useMultithread = "";	// Not from log file (only in advanced mode)! use multithreading "use_multithreading", else ""
		numThreads = 40;						// multithread param
		blockSizX = 128;                     	// multithread param
		blockSizY = 128;						// multithread param
		blockSizZ = 16;						// multithread param
    }
}

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
		else  if (endsWith(list[i], fileExtension)) 
		   processImage(dir, list[i]);
	}
}

function processImage(dirPath, imName) {
	
	filePath = "" + dirPath + File.separator + imName;
	
	if (fileExtension==".tif") {
		open(filePath);
	} else {
		run("Bio-Formats Importer", "open=[" + filePath + "]");
	}
  	
	if (nImages == 0) {
	    print("Failed to open image: " + filePath);
	    exit();
	}
	
	Stack.getDimensions(width, height, channels, slices, frames);
	//print(nSlices);
	
	if (channels>1) {
		if (channel==0) {
			print("Error: Multi-channel image, please specify channel number");
		}
		run("Split Channels");
		imName = "C" + channel + "-" + imName;
		selectWindow(imName);
	}

	imNameResultFile = imName;
	if (sliceNumber!=0) {
		setSlice(sliceNumber);
		run("Duplicate...", "use");
		imName = getInfo("image.title");
	}

	print("Number of slices: " + nSlices + " (if 1 then RS is run on 2D)");
		
	results_csv_path = "" + dirPath + File.separator + "RS_" + imNameResultFile + 
	"_ani" + anisotropyCoefficient + 
	"rnsc" + ransac_sub + 
	"iMn" + imMin +
	"iMx" + imMax +
	"s" + sigmaDoG +
	"t" + thresholdDoG + 
	"sR" + supportRadius + 
	"iR" + inlierRatio +
	"mE" + maxError + 
	"iT" + intensityThreshold + 
	"bsM" + bsMethod_sub + 
	"bsME" + bsMaxError + 
	"bsIR" + bsInlierRatio +
	".csv";

	RSparams =  "image='" + imName + 
	"' mode=Advanced anisotropy=" + anisotropyCoefficient + " robust_fitting=[" + ransac + "] use_anisotropy spot_intensity=[Linear Interpolation]" +  
	" image_min=" + imMin + " image_max=" + imMax + " sigma=" + sigmaDoG + " threshold=" + thresholdDoG + 
	" support=" + supportRadius + " min_inlier_ratio=" + inlierRatio + " max_error=" + maxError + " spot_intensity_threshold=" + intensityThreshold + 
	" background=[" + bsMethod + "] background_subtraction_max_error=" + bsMaxError + " background_subtraction_min_inlier_ratio=" + bsInlierRatio + 
	" results_file=[" + results_csv_path + "]" + 
	" " + useMultithread + " num_threads=" + numThreads + " block_size_x=" + blockSizX + " block_size_y=" + blockSizY + " block_size_z=" + blockSizZ;;

	print(RSparams);

	run("RS-FISH", RSparams);

	// Close all windows:
	run("Close All");	
	while (nImages>0) { 
		selectImage(nImages); 
		close(); 
    } 
	run("Close");
} 
