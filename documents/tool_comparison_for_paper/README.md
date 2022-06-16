This directory includes the scripts, files, and documentation that was run in the benchmarking analysis of RS-FISH against FISH-Quant, BigFish, and AIRLOCALIZE, Starfish, deepBlink.

The accuracy analysis - done with 50 simulated images (size 256x256x32), each image with either 30 or 300 spots and varying SNR.  

Images can be found here:  
`https://github.com/PreibischLab/RS-FISH/tree/master/documents/Simulation_of_data/Simulated%20data`  

For each tool, one main analysis pipeline was chosen and a grid search was run over the pipeline parameters and their combinations within a range of sensible values.  
That resulted in many optional result files per image per tool. For each tool the best results per image was selected by comparing the results to ground truth data.  

Other datasets used in the analysis:   
`https://github.com/timotheelionnet/simulated_spots_rsFISH/tree/main/out`    
`https://bimsbstatic.mdc-berlin.de/akalin/preibisch/RSFISH_embryos.zip`  
`https://bimsbstatic.mdc-berlin.de/akalin/preibisch/FISH_real_w_noise_levels.zip`   

For this analysis, parameter settings were chosen for each tool's pipeline for each image so that the detected number of spots per image will be similar across tools (as number of detected spots is a main contributer to execution time).

### How to run analysis:

Below are instructions for how each tool was run and how the resulted detections were analyzed. 

#### RS-FISH 

Version used: RS-FISH-8e53110.jar   
Pipeline: RANSAC   

To rerun the analysis:    

Importantly, the scripts in `RS_grid_search_on_cluster` were run on a Sungrid computing  cluster, thus will need to be adjusted if running on a different environment.  
The radial symmetry grid search as is calls 234 jobs, one for each combination of sigma, threshold, and supportRadius. The defined relatively big memory used is only needed for edge cases. Notice that each job copies the Fiji folder (you need to delete it after), as without it there are JAVA memory errors.   

1. First, change the path in all the scripts in `RS_grid_search_on_cluster` and `RSFISH_speed_test_embryos,ijm` to match your path.
2. To run the accuracy test (on the simulated data), run `RS_grid_search_on_cluster/radial_symmetry_grid_call_qsub.py`.
3. To run the speed test (the the embryo data), run `RSFISH_speed_test_embryos,ijm`. 

#### FISH-Quant

`https://bitbucket.org/muellerflorian/fish_quant/src/master/`  

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
The BigFish grid search calls 63 jobs, one for each image in the `selected simulations` subdirectories. The images with 3000spots were not used in the final analysis in the paper.  

1. open all scripts in the `BF_grid_search_on_cluster` directory and the scripts that start with `Bigfish` and change all paths within the scripts to your paths.  
2. To run the accuracy analysis (on the simulated data), first run `BF_grid_search_on_cluster/fish_quant_grid_call_qsub.py` then, on its results, run `Bigfish_compare_results.ipynb`, followed by `Bigfish_get_best_results.ipynb`.  
3. To run the execution time analysis (on the embryo data), run `Bigfish on embryos.py`.  

#### AIRLOCALIZE  

`https://github.com/timotheelionnet/AIRLOCALIZE`    

Version used: 1.6   
Pipeline: 3DMaskFull (recommended)  

Please follow the steps in `airlocalize.ipynb`  

#### Starfish  

`https://github.com/spacetx/starfish`    

Version used: 0.2.2   
Pipeline: BlobDetector 

Sratfish is a tool for building image analysis pipelines, thus it offers more than one pipeline for spot detection. We chose the BlobDetector as our simulated images are fairly simple.  

Please follow the steps in `starfish.ipynb`   

#### deepBlink  

`https://github.com/BBQuercus/deepBlink`    

Version used: 0.1.1   
Pipeline: "Pretrained network "particle.h5" 

deepBlink is a deep learning based application that is currently meant for 2D images, however they offer a 3D solution on top of the network. We used "Particle" as it gave superior detection results compared to other pretrained networks offered as well as our attempts to train a network with our small dataset.

Please follow the steps in `deepBlink.ipynb`  

## Comparison to GT:

The comparison of all tool results to ground truth was done with `compare_GT.py`
