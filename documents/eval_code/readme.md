Please find in this directory scripts to analyze the grid-search results of the FISHquant and RS-FISH parameter spaces. These scripts parse the outputs of the two different spot detection programs from images with different numbers of points and varying types of image noise.

These scripts are meant to be run in this sequence: 

* 1. Count_lines_of_detection_files.py is run to generate a couple of lists of plausible parameters that a. do not produce too many points per points in ground-truth image or b. too few ground truth points per image. 
* 2. With this list, you may now run Detection_from_files.py, which compares the files in the plausible detection lists to find the best combinations of parameters for FISHquant and radial symmetry across several images.
* 3. You may now parse FISHquant grid search run logs so that run-time performance may be assessed in the next script
* 4. You may now parse RS-FISH grid search run-time logs and the output of detections_from_files to create an excel sheet. In this sheet,  rows are parameters used to generate images with different noise and point organization regimes and their associated images. Columns represent performance statistics for FISHquant and Radial symmetry. These statistics include run-time # of missed ground truth detection and average euclidean distance from the detected point to its associated ground truth points. This is done using gridsearch_results_to_spreadsheet.py


TODO: Reorganize scripts to emphasize readability
