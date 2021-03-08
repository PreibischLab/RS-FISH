import os

# loop over all images in all subdirs:
# Each image is a job - ALL grid search parameters are run on one image in one job
for i in range(63):

	command = f'qsub -v IM_NUM={i} 1_fish_quant_grid_search.sh'
	os.system(command)
