#!/bin/sh
# Job script with qsub-options
##$ -pe smp 2
##$ -pe orte 4
#$ -V -N "FQ_grid"
########## The long run is needed (especially for the embryo images) due to gaussian fit run time
########## For the embryo images it wasn't enough time
#$ -l h_rt=80:00:00 -l h_vmem=10G -l h_stack=128M -l os=centos7 -cwd

##$ -o FQ_grid-log.txt
##$ -e FQ_grid-log.txt

# export NSLOTS=8
# neccessary to prevent python error
export OPENBLAS_NUM_THREADS=4
# export NUM_THREADS=8
python 2_bigfish_grid_search.py --im_num $IM_NUM  &> /path/to/where/to/save/log/files/log${IM_NUM}.log