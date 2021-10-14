## RS-FISH: Radial Symmetry (RS) Fiji plugin - example scripts:

This readme has info about:  
1. How to run the plugin from a macro script for batch processing.
2. Installation on cluster details.

### Macro script for batch processing:

A recommended procedure for running the plugin on a dataset will be to first run the plugin in Fiji in interactive mode on one (or a few) example images, to find the best parameters for spot detection. Then use the parameters in a macro script that can be run from Fiji or headless (e.g. on cluster).  

Steps:   
* Find the best parameters for your dataset:
1. Open an example image from the dataset in FIJI.
2. Go to: Plugins > RS-FISH > RS-FISH
3. In the menu that opened - choose "Interactive" Mode, set your image anisotropy coefficient (z resolution / XY resolution), leave the rest as default values, then click ok.
4. Change the slider values of the parameters until you are happy with the detections you see on the image. Then click “Done”.
5. You can also change the intensity threshold in the “Intensity distribution” window according to the detections seen on the image. Once the correct threshold has been set, write it down and click “OK - press to proceed to final results”.
6. Save the FIJI log, as it details the parameters used. For this, in the “Log” window, go to: File > Save As, and save the Log.txt file at your chosen location.
* Run in batch mode:
1. Open the Log.txt file you just saved, so you could copy the parameters you found in interactive mode to the macro script.
2. Open the `RS_macro.ijm` (in FIJI or a text editor) from the GitHub link above.
3. In the macro file, change the parameters (e.g., `anisotropyCoefficient`, `sigmaDoG`) at the beginning of the macro file (under the line `Define RS parameter`) to the values from the Log.txt file. Unless you’re sure otherwise, only change the values of existing variables in the macro file.
4. In the macro file, change the `intensityThreshold` variable to the threshold value you wrote down (in step 5 in the previous section).
5. In the macro file, it’s recommended to keep the `useMultithread` variable as “use_multithreading” for a speedier run. This option is not available when RS-FISH is run in “Interactive” mode. If multithreading is used, `numThreads` should be set according to the number of threads on your machine. `blockSizX`,  `blockSizY`, and `blockSizZ` should be set for chunking each image in the analysis to blocks. Note, different multithreading runs may result in ever so slightly inconsistent results.
6. In the macro file, change the `path` variable value to the parent directory of your .tif images (all tifs in all sub-directories will be processed).
7. In the macro file, change the `timeFile` variable value to the path you wish to save the running times file.
8. Call the script. It can be done from the FIJI GUI, from the terminal, or from a computing cluster. Example Linux command to run the macro script:  
`<fiji_dir_path>/ImageJ-linux64 --headless --run </path/to/this/script>/RS_macro.ijm &> </path/to/where/you/want/yourlogfile>.log`  


The macro described runs the same command as the command that is recorded if you record running the RS plugin in advanced mode.  
Command Template:   

`parameterString = "image=" + imName + " mode=Advanced anisotropy=" + anisotropyCoefficient + " robust_fitting=[" + ransacStr + "] use_anisotropy" +  " image_min=" + imMin + " image_max=" + imMax + " sigma=" + sigmaDoG + " threshold=" + thresholdDoG + " support=" + supportRadius + " min_inlier_ratio=" + inlierRatio + " max_error=" + maxError + " spot_intensity_threshold=" + intensityThreshold +  " background=[" + bsMethodStr + "] background_subtraction_max_error=" + bsMaxError + " background_subtraction_min_inlier_ratio=" + bsInlierRatio + " results_file=[" + results_csv_path + "]" +  " " + useMultithreadStr + " num_threads=" + numThreads + " block_size_x=" + blockSizX + " block_size_y=" + blockSizY + " block_size_z=" + blockSizZ;`    

`run("RS-FISH", parameterString);`   
	
Example command:  
`run("RS-FISH", "image=im.tif mode=Advanced anisotropy=0.6500 robust_fitting=[RANSAC] use_anisotropy  image_min=0 image_max=65535 sigma=1.203 threshold=0.0025 support=3 min_inlier_ratio=0.30 max_error=1.12237 spot_intensity_threshold=0  background=[No background subtraction] background_subtraction_max_error=0.05 background_subtraction_min_inlier_ratio=0.10 results_file=[/home/bob/Desktop/im_spots.csv] [use_multithreading] num_threads=40 block_size_x=128 block_size_y=128 block_size_z=16");`  

Notably, running the tool/macro with a combination of parameters where `sigma<1.5`, `threshold<0.002`, and `Support region>=3` will cause both longer running times and requires bigger memory, especially on bigger images.  


### Cluster installation details

To install the RS-FISH plugin in headless mode (for cluster use):   
In the FIJI directory run:   
`./ImageJ-linux64 --update add-update-site RadialSymmetry http://sites.imagej.net/RadialSymmetry/`   








