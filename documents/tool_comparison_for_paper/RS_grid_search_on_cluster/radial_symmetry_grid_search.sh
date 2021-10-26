#!/bin/sh
# Job script with qsub-options
##$ -pe smp 2
##$ -pe orte 4
#$ -V -N "RS_grid"
#$ -l h_rt=80:00:00 -l h_vmem=26G -l h_stack=128M -l os=centos7 -cwd

##$ -o test-log.txt
##$ -e test-log.txt

# export NSLOTS=8
# neccessary to prevent python error
export OPENBLAS_NUM_THREADS=4
# export NUM_THREADS=8
eval "$fiji_path/ImageJ-linux64 --mem=26G --headless --run /scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/radial_symmetry_grid_search_macro.ijm \"sig=${sig},thr=${thr},suppReg=${suppReg}\" &> /scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Logs_RS/log_sig${sig}thr${thr}suppReg${suppReg}tmpNewEmbryo.txt"
