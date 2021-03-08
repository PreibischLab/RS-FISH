import sys
import os
import numpy as np
import csv
import copy
from scipy import spatial
import glob
#####Program takes in a list of filenames. For each file name in list opens it and assess the number of accurate detections inthe file

#This function compares two arrays 
#Unmod = ground truth array
#More_than = detections from one of the programs
#Function checks if any points in the non ground truth array are very close / match with points 
#in the gt array it they are under certain distance

#Returns # of undetected ground truth points, spurious detections, and average distance between real detection and associated points
def profile_detections(unmod,more_than):

    min_dist = 2

    distance_arr = []

    removedItems = True
    euc_dist = 0

    while (removedItems and len(more_than) != 0 and len(unmod) != 0 ):
        #print("loop")

        minDist = 10000
        minIndexUnmod = -1
        minIndexMore_Than = -1
        counter = 0
        kd_copy = copy.deepcopy(more_than)
        kdtree = spatial.KDTree(kd_copy)

        for item in unmod:
            distance,index = kdtree.query(item) # a new KD tree is made
            if ( distance < minDist ):
                minDist = distance
                minIndexUnmod = counter
                minIndexMore_Than = index
                #print(minDist, counter, item)
            counter = counter + 1

        if ( minDist < min_dist): # if less than min dist
            more_than = np.delete(more_than, minIndexMore_Than, axis = 0 ) # delete mod ind
            unmod = np.delete(unmod,minIndexUnmod, axis = 0) #delete unmod ind
            #print(len(more_than),distance) # sanity checkd
            removedItems = True
            distance_arr.append(minDist) # if we want to extrat stat ig

        else:
            removedItems = False
    if (len(distance_arr) >0):
        euc_dist = np.mean(np.asarray(distance_arr))
    return(len(unmod),len(more_than),euc_dist)


#Parses ground truth array
def read_in_gt(csv_name):
    x_Crd = []
    y_Crd = []
    z_Crd = []


    with open(csv_name) as csvfile:
        gt_locs= np.asarray(list(csv.reader(csvfile,delimiter="\t")))
    for entry in gt_locs:
        x_coord = (float(str(entry).split("   ")[2]) - 0.5)
        y_coord = (float(str(entry).split("   ")[1]) - 0.5)
        z_coord = (float(str(entry).split("   ")[3]) - 1.0)

        x_Crd = np.append(x_Crd, x_coord)
        y_Crd = np.append(y_Crd, y_coord)
        z_Crd = np.append(z_Crd, z_coord)



    unmod = np.asarray([x_Crd,y_Crd,z_Crd]).T
    return(unmod)
#Parses detections from fishquant and RS
def read_in_detections(g_file):
    x_Crd = []
    y_Crd = []
    z_Crd = []

    type_of = (g_file[-4:])
    if (type_of == ".csv"):
        with open(g_file) as csvfile:
            next(csvfile)
            file_in = np.asarray(list(csv.reader(csvfile,delimiter=",")))
            for entry in file_in:
                x_coord = float((entry)[0])
                y_coord = float((entry)[1])
                z_coord = float((entry)[2])

                x_Crd = np.append(x_Crd, x_coord)
                y_Crd = np.append(y_Crd, y_coord)
                z_Crd = np.append(z_Crd, z_coord)


    if (type_of == ".txt"):
        with open(g_file) as csvfile:
            next(csvfile)
            file_in = np.asarray(list(csv.reader(csvfile,delimiter="\t")))
            for entry in file_in:
                x_coord = float((entry)[0])
                y_coord = float((entry)[1])
                z_coord = float((entry)[2])

                x_Crd = np.append(x_Crd, x_coord)
                y_Crd = np.append(y_Crd, y_coord)
                z_Crd = np.append(z_Crd, z_coord)

    unsimulated = np.asarray([x_Crd,y_Crd,z_Crd]).T
    return(unsimulated)
#Helper function to parse the ground truth file name from the detection/ non ground truth name.
def loc_file_name_help(detected_name,current_dir):
    above_sim_dir = "/home/lepstei/rs_scratch/ImagesForStephan/" # Change to whatever local dir has the loca
    loc_location = detection_name.split("/")
    dir_path = os.path.dirname(os.path.realpath(detection_name))
    detection_dir_name = (os.getcwd())
    sim_dir = above_sim_dir + detection_dir_name.split("/")[-1] + "/"
    file_name = detection_name.split("/")[-1]
    remove_prefix = file_name.split("_results_")
    remove_suffix = remove_prefix[1]
    file_name_clean = remove_suffix.split(".")
    file_name_clean = (file_name_clean[0])
    file_name_clean = file_name_clean + '.loc'
    gt_file = (above_sim_dir + current_dir[24:-19] +"/" + file_name_clean ) #################### Sim dir referenced here
    return(gt_file)

#Loop opens up file to write results to 
#Loops through plausible filenames list
#of all plausible detections
#Doesnt include vey large ground truth /detection files as they take a very long time to run
with open("/home/lepstei/rs_scratch/gridsearchresults.txt", "w") as f:
    f.write("Program_used" +"\t"+ "Image_used" + "\t"+"Parameter_string" + "\t"+ "Simulation_settings" + "\t" + "Missed_detections" + "\t" + "False_detections"+"\t"+"euc_dist" + "\n")
    where_is_base_dir = "/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/Selected_simulation"
    empty_bg_list= glob.glob("/home/lepstei/rs_scratch/*Range*")	
    for dir in empty_bg_list:
        with open(dir) as q:
            lines = q.read().splitlines()
        for file in lines:
            if not("3000spots" or "30000spots") in file:
                detection_name = file
                line_find = detection_name.split("/")[-1]
                detections = read_in_detections(detection_name)
                gt_name = loc_file_name_help(detection_name,(dir.replace('_rs','')))
                program_used = (line_find.split("_")[0])
                program_used = program_used.split("/")[-1]
                detename = (detection_name.split(".")[0])
                image_name = detename.split("results_")[1]
                param_string = (detection_name.split(".tif_")[1][:-4])
                sim_settings = (os.getcwd().split("/")[-1].replace(" ", ""))

                gt = read_in_gt(gt_name)
                new_detections = profile_detections(gt,detections)
   
                f.write(program_used +"\t"+ image_name + "\t" + param_string + "\t" + dir + "\t" + str(new_detections[0]) + "\t" + str(new_detections[1])+ "\t" + str(new_detections[2]) + "\n")
            #else:
                f.flush()	#print(number_of_lines," too small")
f.close()

