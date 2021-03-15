#!/bin/sh
# Job script with qsub-options
##$ -pe smp 2
##$ -pe orte 4
#$ -V -N "RS_grid"
######## The long run time and the lots of memory were only needed for suppReg=4 
#$ -l h_rt=80:00:00 -l h_vmem=26G -l h_stack=128M -l os=centos7 -cwd

#$ -o RS-grid-log.txt
#$ -e RS-grid-log.txt

# export NSLOTS=8
# neccessary to prevent python error
export OPENBLAS_NUM_THREADS=4
# export NUM_THREADS=8
eval "$fiji_path/ImageJ-linux64 --mem=24G --headless --run /path/to/your/2_radial_symmetry_grid_search_macro.ijm \"sig=${sig},thr=${thr},suppReg=${suppReg}\" &> /path/to/where/you/want/your/log/file/sig${sig}thr${thr}suppReg${suppReg}.log"
