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

To run the grid search as is, you need a SunGrid computing cluster (calls qsub jobs), RadialSymmetry plugin installation in Fiji, and a BigFish installation (details below). First open all the files in the grid search subdirectories and change the paths to your desired paths. Then call the first python script (0_xxx.py), as this script calls the shell script that calls multiple (distributed) jobs.  

The radial symmetry grid search as is calls 234 jobs, one for each combination of sigma, threshold, and supportRadius. The defined relatively big memory used is only needed for edge cases. Notice that each job copies the Fiji folder (you need to delete it after), as without it there are JAVA memory errors.    
The BigFish grid search calls 63 jobs, one for each image in the `selected simulations` subdirectories.


### Compared software installation info:

BigFish installation instructions can be found in their github repo:  
`https://github.com/fish-quant/big-fish`  

Used BigFish version = `0.5.0` you must use the current dev version.  