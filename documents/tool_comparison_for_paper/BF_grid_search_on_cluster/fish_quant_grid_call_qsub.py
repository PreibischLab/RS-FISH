import os

# Thr loop
for i in range(80):

	command = f'qsub -v IM_NUM={i} fish_quant_grid_search.sh'
	os.system(command)
