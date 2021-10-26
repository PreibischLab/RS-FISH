import os
import numpy as np

path = '/scratch/AG_Preibisch/Ella/'

thr = 0.001
multiplier = 1.5
n_steps = 13

# Thr loop
for i in range(0,n_steps):
	# sig loop
	for sig in np.arange(1,2.51, 0.25): 
		for suppReg in range(2,5): #2-4 inclusive

			fiji_path = os.path.join(path,'Fiji.app')
			fiji_path_tmp = f'{fiji_path}_{thr}{sig}{suppReg}'
			cp_command = f'cp -r {fiji_path} {fiji_path_tmp}'
			os.system(cp_command) 

			command = f'qsub -v fiji_path={fiji_path_tmp},sig={sig},thr={thr},suppReg={suppReg} radial_symmetry_grid_search.sh'
			os.system(command)
			#print(command)

	thr = thr * multiplier
