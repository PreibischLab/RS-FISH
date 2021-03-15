from __future__ import with_statement
import time
import mmap
import random
from collections import defaultdict
import os
import glob
import subprocess

#Thif function counts the number of lines in a file
def bufcount(filename):
    out = subprocess.Popen(['wc', '-l', filename],
                         stdout=subprocess.PIPE,
                         stderr=subprocess.STDOUT
                         ).communicate()[0]
    return float(out.partition(b' ')[0])
write_out_dir = "/home/lepstei/"

#This is a threshold for how far off lines in a detection file can be from the number of known points in its ground truth file, smaller threshold values mean detection files musts have a more similar amount detections to the known number of points
the_thresh = 0.03


# For each setting folder ittterate through rs and fq detection files. Append all to list whos line number is within some number of the known ground truth points. Each detection takes a single. Repeat this across settings directory
# You end up with text files that list the files names of at least plausible detection files, its silly and time consuming to analyze a file with 10,000 detections when oyu know the ground truth has 300.
dir_name = "/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg Density Range Sigxy 2 SigZ 2/"
the_dir = glob.glob("/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg Density Range Sigxy 2 SigZ 2/*txt*")
the_dir1 = glob.glob("/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg Density Range Sigxy 2 SigZ 2/*csv*")
my_file_name = (write_out_dir + (dir_name).split("/")[-2] + "_list_realistic.txt")
the_dir = [x for x in the_dir if 'intensThr' in x]
with open(my_file_name, "w+") as the_file:


    for l in the_dir:
        file_name =  l
        spot_num = float(l.split("_")[9][:-5])
        number_of_lines = bufcount(file_name)
        #print(number_of_lines)
        if (abs(number_of_lines - spot_num - 1)  <=  (spot_num * the_thresh)):
            #prfloat(q)
            #prfloat(number_of_lines)
            the_file.write(file_name + '\n')
            #counter = counter + 1


    for l in the_dir1:
        file_name =  l
        spot_num = float(l.split("_")[9][:-5])
        number_of_lines = bufcount(file_name)
        #print(number_of_lines)
        if (abs(number_of_lines - spot_num - 1)  <=  (spot_num * the_thresh)):
            #prfloat(q)
            #prfloat(number_of_lines)
            the_file.write(file_name + '\n')
            #counter = counter + 1
    the_file.close()
dir_name = "/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg Density Range Sigxy 1 SigZ 2/"
the_dir = glob.glob("/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg Density Range Sigxy 1 SigZ 2/*txt*")
the_dir1 = glob.glob("/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg Density Range Sigxy 1 SigZ 2/*csv*")
my_file_name = (write_out_dir + (dir_name).split("/")[-2] + "_list_realistic.txt")
the_dir = [x for x in the_dir if 'intensThr' in x]
with open(my_file_name, "w+") as the_file:



    for l in the_dir:
        file_name =  l
        spot_num = float(l.split("_")[9][:-5])
        number_of_lines = bufcount(file_name)
        #print(number_of_lines)
        if (abs(number_of_lines - spot_num - 1)  <=  (spot_num * the_thresh)):
            #prfloat(q)
            #prfloat(number_of_lines)
            the_file.write(file_name + '\n')
            #counter = counter + 1


    for l in the_dir1:
        file_name =  l
        spot_num = float(l.split("_")[9][:-5])
        number_of_lines = bufcount(file_name)
        #print(number_of_lines)
        if (abs(number_of_lines - spot_num - 1)  <=  (spot_num * the_thresh)):
            #prfloat(q)
            #prfloat(number_of_lines)
            the_file.write(file_name + '\n')
            #counter = counter + 1
    the_file.close()
dir_name = "/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg Density Range Sigxy 1pt5 SigZ 2/"
the_dir = glob.glob("/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg Density Range Sigxy 1pt5 SigZ 2/*txt*")
the_dir1 = glob.glob("/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg Density Range Sigxy 1pt5 SigZ 2/*csv*")
my_file_name = (write_out_dir + (dir_name).split("/")[-2] + "_list_realistic.txt")
the_dir = [x for x in the_dir if 'intensThr' in x]
with open(my_file_name, "w+") as the_file:


    for l in the_dir:
        file_name =  l
        spot_num = float(l.split("_")[9][:-5])
        number_of_lines = bufcount(file_name)
        #print(number_of_lines)
        if (abs(number_of_lines - spot_num - 1)  <=  (spot_num * the_thresh)):
            #prfloat(q)
            #prfloat(number_of_lines)
            the_file.write(file_name + '\n')
            #counter = counter + 1


    for l in the_dir1:
        file_name =  l
        spot_num = float(l.split("_")[9][:-5])
        number_of_lines = bufcount(file_name)
        #print(number_of_lines)
        if (abs(number_of_lines - spot_num - 1)  <=  (spot_num * the_thresh)):
            #prfloat(q)
            #prfloat(number_of_lines)
            the_file.write(file_name + '\n')
            #counter = counter + 1
    the_file.close()
dir_name = "/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg SNR Range Sigxy 2 SigZ 2/"
the_dir = glob.glob("/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg SNR Range Sigxy 2 SigZ 2/*txt*")
the_dir1 = glob.glob("/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg SNR Range Sigxy 2 SigZ 2/*csv*")
my_file_name = (write_out_dir + (dir_name).split("/")[-2] + "_list_realistic.txt")
the_dir = [x for x in the_dir if 'intensThr' in x]
with open(my_file_name, "w+") as the_file:


    for l in the_dir:
        file_name =  l
        spot_num = float(l.split("_")[9][:-5])
        number_of_lines = bufcount(file_name)
        #print(number_of_lines)
        if (abs(number_of_lines - spot_num - 1)  <=  (spot_num * the_thresh)):
            #prfloat(q)
            #prfloat(number_of_lines)
            the_file.write(file_name + '\n')
            #counter = counter + 1


    for l in the_dir1:
        file_name =  l
        spot_num = float(l.split("_")[9][:-5])
        number_of_lines = bufcount(file_name)
        #print(number_of_lines)
        if (abs(number_of_lines - spot_num - 1)  <=  (spot_num * the_thresh)):
            #prfloat(q)
            #prfloat(number_of_lines)
            the_file.write(file_name + '\n')
            #counter = counter + 1
    the_file.close()
dir_name = "/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg SNR Range Sigxy 1 SigZ 2/"
the_dir = glob.glob("/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg SNR Range Sigxy 1 SigZ 2/*txt*")
the_dir1 = glob.glob("/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg SNR Range Sigxy 1 SigZ 2/*csv*")
my_file_name = (write_out_dir + (dir_name).split("/")[-2] + "_list_realistic.txt")
the_dir = [x for x in the_dir if 'intensThr' in x]
with open(my_file_name, "w+") as the_file:


    for l in the_dir:
        file_name =  l
        spot_num = float(l.split("_")[9][:-5])
        number_of_lines = bufcount(file_name)
        #print(number_of_lines)
        if (abs(number_of_lines - spot_num - 1)  <=  (spot_num * the_thresh)):
            #prfloat(q)
            #prfloat(number_of_lines)
            the_file.write(file_name + '\n')
            #counter = counter + 1


    for l in the_dir1:
        file_name =  l
        spot_num = float(l.split("_")[9][:-5])
        number_of_lines = bufcount(file_name)
        #print(number_of_lines)
        if (abs(number_of_lines - spot_num - 1)  <=  (spot_num * the_thresh)):
            #prfloat(q)
            #prfloat(number_of_lines)
            the_file.write(file_name + '\n')
            #counter = counter + 1
    the_file.close()

dir_name = "/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg SNR Range Sigxy 1pt5 SigZ 2/"
the_dir = glob.glob("/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg SNR Range Sigxy 1pt5 SigZ 2/*txt*")
the_dir1 = glob.glob("/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Empty Bg SNR Range Sigxy 1pt5 SigZ 2/*csv*")
my_file_name = (write_out_dir + (dir_name).split("/")[-2] + "_list_realistic.txt")
the_dir = [x for x in the_dir if 'intensThr' in x]
with open(my_file_name, "w+") as the_file:

    for l in the_dir:
        file_name =  l
        spot_num = float(l.split("_")[9][:-5])
        number_of_lines = bufcount(file_name)
        #print(number_of_lines)
        if (abs(number_of_lines - spot_num - 1)  <=  (spot_num * the_thresh)):
            #prfloat(q)
            #prfloat(number_of_lines)
            the_file.write(file_name + '\n')
            #counter = counter + 1


    for l in the_dir1:
        file_name =  l
        spot_num = float(l.split("_")[9][:-5])
        number_of_lines = bufcount(file_name)
        #print(number_of_lines)
        if (abs(number_of_lines - spot_num - 1)  <=  (spot_num * the_thresh)):
            #prfloat(q)
            #prfloat(number_of_lines)
            the_file.write(file_name + '\n')
            #counter = counter + 1
    the_file.close()

dir_name = "/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Infinite SNR Density Range Sigxy 1pt35 SigZ 2/"
the_dir = glob.glob("/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Infinite SNR Density Range Sigxy 1pt35 SigZ 2/*txt*")
the_dir1 = glob.glob("/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation/Infinite SNR Density Range Sigxy 1pt35 SigZ 2/*csv*")
my_file_name = (write_out_dir + (dir_name).split("/")[-2] + "_list_realistic.txt")
the_dir = [x for x in the_dir if 'intensThr' in x]
with open(my_file_name, "w+") as the_file:

    for l in the_dir:
        file_name =  l
        spot_num = float(l.split("_")[9][:-5])
        number_of_lines = bufcount(file_name)
        #print(number_of_lines)
        if (abs(number_of_lines - spot_num - 1)  <=  (spot_num * the_thresh)):
            #prfloat(q)
            #prfloat(number_of_lines)
            the_file.write(file_name + '\n')
            #counter = counter + 1


    for l in the_dir1:
        file_name =  l
        spot_num = float(l.split("_")[9][:-5])
        number_of_lines = bufcount(file_name)
        #print(number_of_lines)
        if (abs(number_of_lines - spot_num - 1)  <=  (spot_num * the_thresh)):
            #prfloat(q)
            #prfloat(number_of_lines)
            the_file.write(file_name + '\n')
            #counter = counter + 1
    the_file.close()

