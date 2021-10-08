This directory includes the scripts, files, and documentation that was run in the benchmarking analysis of RS-FISH against FISH-Quant, BigFish, and AIRLOCALIZE, Starfish, deepBlink.

The analysis consisted of two main parts:
* Accuracy analysis - done with 50 simulated images (size 256x256x32), each image with either 30 or 300 spots and varying SNR.  

Images can be found here:
`https://github.com/PreibischLab/RS-FISH/tree/master/documents/Simulation_of_data/Simulated%20data`  

For each tool, one main analysis pipeline was chosen and a grid search was run over the pipeline parameters and their combinations within a range of sensible values..  
That resulted in many optional result files per image per tool. For each tool the best results per image was selected by comparing the results to ground truth data.  


* Computation time analysis - done with 13 3D images of C. elegans embryos. Image size varied between embryo images, with XY size 334-539 and Z size 81-101.  

Images can be downloaded with this link:  
`https://bimsbstatic.mdc-berlin.de/akalin/preibisch/RSFISH_embryos.zip`  

For this analysis, parameter settings were chosen for each tool's pipeline for each image so that the detected number of spots per image will be similar across tools (as number of detected spots is a main contributer to execution time).

### How to run analysis:

Below are instructions for how each tool was run and how the resulted detections were analyzed. 

#### FISH-Quant

Version used: v3  

FISH-Quant is a GUI MATLAB application, as such, the analysis was run manually and execution times were calculated by hand.
Optimal settings found for each simulated image can be found in the `FQ` directory.

#### BigFish

BigFish installation instructions can be found in their github repo:  
`https://github.com/fish-quant/big-fish`  

Version used: 0.5.0
Pipeline: Dense region decomposition 

As BigFish calculates a default value for each parameter automatically, the parameter space was derived from the default value. We ran a grid search for BigFish’s dense region decomposition as it seems to be the main pipeline (e.g. in BigFish's preprint paper on Biorxiv).  

To rerun the analysis:  

Importantly, the scripts in `BF_grid_search_on_cluster` (running Bigfish on the simulated images) were run on a Sungrid computing  cluster, thus will need to be adjusted if running on a different environment. 

1. open all scripts in the `BF_grid_search_on_cluster` directory and the scripts that start with `Bigfish` and change all paths within the scripts to your paths.  
2. To run the accuracy analysis (on the simulated data), first run `BF_grid_search_on_cluster/fish_quant_grid_call_qsub.py` then, on its results, run `Bigfish_compare_results.ipynb`, followed by `Bigfish_get_best_results.ipynb`.  
3. To run the execution time analysis (on the embryo data), run `Bigfish on embryos.py`.




The radial symmetry grid search as is calls 234 jobs, one for each combination of sigma, threshold, and supportRadius. The defined relatively big memory used is only needed for edge cases. Notice that each job copies the Fiji folder (you need to delete it after), as without it there are JAVA memory errors.    
The BigFish grid search calls 63 jobs, one for each image in the `selected simulations` subdirectories.

Link to download embryo images:  
bimsbstatic.mdc-berlin.de/akalin/preibisch/RSFISH_embryos.zip  

### Compared software installation info:



Used BigFish version = `0.5.0` you must use the current dev version.  

Some of the current setup was run on a SunGrid computing cluster (it calls qsub jobs) and requires installing the RS-FISH plugin in FIJI and the installation of Big-FISH. To run the grid search scripts for both RS-FISH and Big-FISH, only the first file (0_xxx.py) in each subdirectory should be called, as this script submits all of the required jobs. Before calling the python script, open each script in the grid search subdirectory and ensure that all the paths are defined correctly.