#!/bin/sh
# Job script with qsub-options
##$ -pe smp 2
##$ -pe orte 4
#$ -V -N "FQ_grid"
#$ -l h_rt=80:00:00 -l h_vmem=10G -l h_stack=128M -l os=centos7 -cwd

##$ -o test-log.txt
##$ -e test-log.txt

# export NSLOTS=8
# neccessary to prevent python error
export OPENBLAS_NUM_THREADS=4
# export NUM_THREADS=8
python bigfish_grid_search.py --im_num $IM_NUM  &> /scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Logs_FQ/log${IM_NUM}.txt