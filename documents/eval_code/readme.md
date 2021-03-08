Please find a directory of scripts to analyze results of gridsearch of the FQ and RS parameter spaces. These scripts parse the output of these different programs on images with different numbers of points and varying types of image noise.

These scripts are meant to be run in sequence.

1. Count_lines_of_detection_files.py is run to generate a couple of lists of plausible parameters that a. dont produce to many points per points in ground truth image or b. too few ground truth points per image. 
2. With this list you may now run Detection_from_files.py which compares the files in the plausible detection lists to find the best combinationss of parameters for fishquant and radial symmetry across several images.
3. You may now parse fishquant grid search run logs so that runtime performance may be assessed in the next script
4. You may now parse rs grid search runtime logs and the output of detections_from_files to create an excel sheet where rows are parameters used to generate images with different regimes of noise and point organization and their associated images. Columns represent performance statistics for fishquant and Radial symmetry. These statistics include run time # of missed ground truth detection and average euclidean distance from detected point to its associated ground truth points. This is done using gridsearch_results_to_spreadsheet.py


TODO: Reorganize scrupts to emphasize readability

