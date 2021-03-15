## RS-FISH: Radial Symmetry (RS) FIJI plugin - example scripts:

This readme has info about:  
1. How to run the plugin from a macro script for batch processing.
2. Information about the parameters grid search and the process, and how to rerun the grid search described in the RS-FISH paper. 
3. Installation on cluster details.

### Macro script for batch processing:

A recommended procedure for running the plugin on a dataset will be to first run the plugin in FIJI in interactive mode on one (or a few) example images, to find the best parameters for spot detection. Then use the parameters in a macro script that can be run from FIJI or headless (e.g. on cluster).  

Steps:  
* Find best parameters:
1. Open an example image from the dataset on FIJI.
2. Plugins > Radial Symmetry Localization > Radial Symmetry
3. In the menu that opened - choose "Interactive" Mode, set your image anisotropy coefficient (z resolution / xy resolution), and leave the rest as default values, then click ok.
4. Change the slide bar values of the parameters until you're happy with the detections you see on the image.
5. Write down the parameters used - anisotropy, sigma, threshold, support region, inline ratio, max error, and the intensity threshold.
6. Click done.
* Run on batch:
1. Open the `RS_macro.ijm` (in FIJI, or text editor) 
2. Change the parameters (sigma, threshold..) at the beginning of the file to the values you found.
3. Change the `path` variable value to the parent directory of your images
4. Change the `timeFile` variable value to the path where you wish running times file will be saved.
5. Call the script. Can be done from FIJI GUI, from the terminal, or from a cluster. Example linux command to run the macro script:  
`fiji_dir_path/ImageJ-linux64 --headless --run /path/to/this/script/RS_macro.ijm &> /path/to/where/you/want/yourlogfile.log`  

The macro described runs the same command as the command that is recorded if you record running the RS plugin in advanced mode.  
Command Template:

`parameterString = "image=" + imName + 
	" mode=Advanced anisotropy=" + aniso + " use_anisotropy" +
	" robust_fitting=RANSAC" +
	" sigma=" + sig + 
	" threshold=" + thr + 
	" support=" + suppReg + 
	" min_inlier_ratio=" + inRat +  
	" max_error=" + maxErr +
	" spot_intensity_threshold=" + intesThr +
	" background=[No background subtraction]" +
	" results_file=[" + results_csv_path + "]"`
	
`run("Radial Symmetry", parameterString);`  
	
Example command:  
`run("Radial Symmetry", "image=myimg.tif mode=Advanced anisotropy=0.75 use_anisotropy robust_fitting=RANSAC sigma=1.5 threshold=0.008 support=3 min_inlier_ratio=0.3 max_error=0.5 spot_intensity_threshold=0 background=[No background subtraction]" results_file=["path/to/result/file.txt"]);`  

Warning - running the tool/macro with a combination of parameters where `sigma<1.5`, `threshold<0.002`, and `Support region>=3` will cause both longer running times and requires bigger memory, especially on bigger images.

### Parameter Grid Search

In order to benchmark RS-FISH, we compared its accuracy of spot detection in the simulated data with the FISHquant’s python version, BigFish. For each of the tools, we ran a grid search of the parameter space within a range of sensible values. As BigFish calculates a default value for each parameter automatically, the parameter space was derived from the default value. We also ran a grid search for BigFish’s decompose_dense function parameters, but as the returned detections are not sub-pixel localized, we excluded those from the benchmarking results. The parameter value combinations that were run (inclusive):

RS-FISH grid search parameters:
* dogSigma = 1-2.5, step size += 0.25
* threshold = 0.001-0.13, step size x= 1.5
* supportRadius: 2-4, step size +=1
* inlierRatio: 0.0-0.3, step size += 0.1
* maxError: 0.1-2.6 step size += 0.5
* intensityThreshold: [0,100,150,200]

BigFish grid search parameters:
* sigmaZ = default + [-0.5,-0.25,0,0.25,0.5]
* sigmaYX = default + [-0.5,-0.25,0,0.25,0.5]
* threshold = value of index in threshold array, with indices relative to location of the default. Relative to default indices: [-6,-3,-2,-1,0,1,2,3,6]

To run the grid search as is, you need a SunGrid computing cluster (calls qsub jobs), RadialSymmetry plugin installation in FIJI, and a BigFish installation (details below). First open all the files in the grid search subdirectories and change the paths to your desired paths. Then call the first python script (0_xxx.py), as this script calls the shell script that calls multiple (distributed) jobs.  

The radial symmetry grid search as is calls 234 jobs, one for each combination of sigma, threshold, and supportRadius. The defined relatively big memory used is only needed for edge cases. Notice that each job copies the FIJI folder (you need to delete it after), as without it there are JAVA memory errors.    
The BigFish grid search calls 63 jobs, one for each image in the `selected simulations` subdirectories.


### Installation details

To install the Radial Symmetry plugin in headless mode (for cluster use):  
In the FIJI directory run:  
`./ImageJ-linux64 --update add-update-site RadialSymmetry http://sites.imagej.net/RadialSymmetry/`  

BigFish installation instructions can be found in their github repo:  
`https://github.com/fish-quant/big-fish`

If using BigFish version < `0.5.0` you must use the current dev version.  






