## This python script defines **some** of the grid-search parmeters 
## and calls a .sh script multiple times with specific parameters for qsub jobs.

## WARNING!!! As running those multiple jobs on the same Fiji crashed
## This script creates a new Fiji software folder for each run!
## You can delete those after the run.

import os
import numpy as np

dir_path = '/Path/To/The/Parent/Directory/Of/Your/Fiji/Directory'

## Thr is the threshold value for the DoG
thr = 0.001
multiplier = 1.5
n_steps = 13

## Sig is one of the DoG sigmas
sig_range = np.arange(1,2.51, 0.25)

## Support region radius range:
suppReg_range  = range(2,5)

# Thr loop
for i in range(0,n_steps):
	# sig loop
	for sig in sig_range: 
		# SuppReg loop:
		for suppReg in suppReg_range: 

			# Make a copy of the entire Fiji directory:
			fiji_path = os.path.join(dir_path,'Fiji.app')
			fiji_path_tmp = f'{fiji_path}_{thr}{sig}{suppReg}'
			cp_command = f'cp -r {fiji_path} {fiji_path_tmp}'
			os.system(cp_command) 

			# Call the qsub shell script that calls the ImageJ macro with the parameters:
			command = f'qsub -v fiji_path={fiji_path_tmp},sig={sig},thr={thr},suppReg={suppReg} 1_radial_symmetry_grid_search_qsub.sh'
			os.system(command)

	thr = thr * multiplier
