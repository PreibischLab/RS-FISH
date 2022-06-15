import sys
import os
import numpy as np
import csv
import copy
from scipy import spatial
from glob import glob
import pandas as pd
import argparse

##### analysis type: '' (for the original analysis) OR 'adj_spots'
analysis = '_adj_spots'
#analysis = ''
# ##### analysis type: '' (for the original analysis) OR '_indiv_spot' for getting info about each spot
# indiv_spot = '_indiv_spot'
indiv_spot = ''

path_cluster = '/YOUR/PATH/'
path_local = '/YOUR/LOCAL/PATH/'

parser = argparse.ArgumentParser(description='for cluster multiple jobs - tool and im_num is given as arg')
parser.add_argument("--tool")
parser.add_argument("--im_num")
args = parser.parse_args()
if args.tool:
    ## RS=RS-FISH / BF=bigfish / AL=airlocalize / SF=starfish / FQ=fishquant /DB=deepblink / RSMC=RS_multiconsensus
    tool = args.tool
    if tool not in ["RS", "BF", "AL", "DB", "SF", "FQ", "RSMC"]:
        print("Tool must be on of the followng: RS/BF/AL/DB/SF/DB/RSMC")
else:
    print('need to pass a tool')
if args.im_num:
    im_num = int(args.im_num)
else:
    print('need to pass image number')

print(f'tool: {tool} im_num: {im_num}')

def profile_detections(unmod, more_than): 

    min_dist = 2 

    distance_arr = [] 

    removedItems = True 
    euc_dist = 0 
    
    ## for spot identity analysis
    used_spots_GT = []
    used_spots_detected = []

    while (removedItems and len(more_than) != 0 and len(unmod) != 0 ): 

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

            ## for spot identity analysis
            used_spots_GT.append(unmod[minIndexUnmod])
            used_spots_detected.append(more_than[minIndexMore_Than])
            

            more_than = np.delete(more_than, minIndexMore_Than, axis = 0 ) # delete mod ind 
            unmod = np.delete(unmod,minIndexUnmod, axis = 0) #delete unmod ind 
            #print(len(more_than),distance) # sanity checkd 
            removedItems = True 
            distance_arr.append(minDist) # if we want to extrat stat id 

        else: 
            removedItems = False 
    if (len(distance_arr) >0): 
        distance_arr = np.asarray(distance_arr)
        euc_dist = np.mean(distance_arr) 

    ## unmod-FN, more_than-FP, euc_dist-mean_euc_dist
    return(len(unmod), len(more_than), euc_dist,
        np.asarray(used_spots_GT), np.asarray(used_spots_detected), distance_arr)


## for spot identity analysis
def find_idxs(gt, detected, gt_found, detected_found):
    
    gt_idxs = []
    detected_idxs = []
    
    for i in range(len(gt_found)):
        temp = np.where((gt==gt_found[i]).all(axis=1))
        gt_idxs.append(temp)
        
        temp = np.where((detected==detected_found[i]).all(axis=1))
        detected_idxs.append(temp)
        
    gt_idxs = np.append([], gt_idxs)
    detected_idxs = np.append([], detected_idxs)
    
    return gt_idxs, detected_idxs


## On cluster
if (tool in ['RS','BF']) and analysis=='':
    path = os.path.join(path_cluster, 'Selected_simulation')
if (tool in ['RS','BF']) and analysis=='_adj_spots':
    path = os.path.join(path_cluster, 'simulations_adj_spots')
## On workstation
else:
    path = path_local


if tool == 'RS':
    all_paths = glob(os.path.join(path, '*','RadialSymmetry_result*.txt')) 
elif tool == 'BF':
    all_paths = glob(os.path.join(path, '*','FishQuant_result*.csv')) 
elif tool == 'AL':
    all_paths = glob(os.path.join(path, 'airlocalize', f'results_simulated{analysis}', '*', '*', '*.loc4')) 
elif tool == 'DB':
    all_paths = glob(os.path.join(path, 'deepblink', f'results{analysis}', '*.csv')) 
elif tool == 'SF':
    all_paths = glob(os.path.join(path, 'starfish', f'results{analysis}', '*__*.csv')) 
elif tool=='FQ':
    if analysis=='':
        all_paths = glob(os.path.join(path, 'fishquant', f'results{analysis}', '*', '*spots.txt'))
    else:
        all_paths = glob(os.path.join(path, 'fishquant', 'simulated_adj_spots', '*.txt'))
if tool == 'RSMC':
    all_paths = glob(os.path.join(path, 'rsfish','multiconsensus_analysis','results','RadialSymmetry_results*.csv')) 


all_paths = [p for p in all_paths if ("mbryo" not in p) and ("000spo" not in p)]

if tool in ["RS", "BF"]: # [""]:#just for cluster no access time
    loc_files = glob(os.path.join(path, '*','*.loc'))
else:
    loc_files = glob(os.path.join(path, f'simulations{analysis}', '*','*.loc'))

loc_files = [l for l in loc_files if "000spo" not in l] 
loc_file = loc_files[im_num]

all_paths = [f for f in all_paths if loc_file.split('/')[-2] in f and loc_file.split('/')[-1][:-4] in f]

print(f'num of files in analysis: {len(all_paths)}')

df = pd.read_csv(loc_file, sep = "\s+", header=None) 
gt_spots = df.to_numpy()[:,:-1] - np.array([0.5,0.5,1])

os.makedirs('indiv_spot', exist_ok=True)

loc_diffs = []
loc_all_files = []
counter = 0

for i,p in enumerate(all_paths): 

    num_lines = sum(1 for line in open(p))
    print(p,num_lines)

    #if ("_30spots" in p and num_lines<60) or ("_300spots" in p and num_lines<500):
    if (num_lines<1000) or (analysis=='_adj_spots' and num_lines<60):

        if os.stat(p).st_size == 0:
            continue
        if tool=='RS':
            detected_spots = pd.read_csv(p, sep="\t")[["y","x","z"]].to_numpy()
        elif tool=='RSMC':
            detected_spots = pd.read_csv(p)[["y","x","z"]].to_numpy()
        elif tool=='BF':
            detected_spots = pd.read_csv(p)[["y","x","z"]].to_numpy() + np.array([0.25,0.25,0.25])
        elif tool=='DB':
            detected_spots = pd.read_csv(p)
            detected_spots = detected_spots[detected_spots.particle.notna()][["y","x","z"]].to_numpy()
        elif tool=='SF':
            detected_spots = pd.read_csv(p)[["y","x","z"]].to_numpy()
        elif tool=='AL':
            detected_spots = pd.read_csv(p, sep="\t")[["x_in_pix","y_in_pix","z_in_pix"]].to_numpy() - np.array([0.5,0.5,1])
        elif tool=='FQ':
            # FQ file has lines before the start of the table.
            # The table x,y,z locations is in nm not pixels, so needs to be converted before used.
            with open(p,'r') as f:
                lines = f.read().split('\n')

            # Get the pixel size value:
            idx = [idx for idx, s in enumerate(lines) if 'Pix-XY' in s][0] + 1
            xy_z = [int(x) for x in lines[idx].split('\t')[:2]]
            xy_z.insert(0,xy_z[0])

            # Get the table:
            idx_start_table = lines.index('SPOTS_START')+1
            detected_spots = pd.read_csv(p, sep='\t', skiprows=idx_start_table)
            
            detected_spots = detected_spots.iloc[:-1,:][["Pos_Y","Pos_X","Pos_Z"]].astype(float).to_numpy() 

            detected_spots = (detected_spots / xy_z)
        
        diff_results = list(profile_detections(gt_spots, detected_spots))

        if indiv_spot == '_indiv_spot' and (not isinstance(diff_results[5], list)) and (9<diff_results[5].shape[0]<400):
            ## diff_results[3] - holds the xyz of the spots that were detected in GT
            ## diff_results[4] - holds the xyz of the spots that were matched in detected
            ## diff_results[5] - holds the euc distance between each pair
            gt_idxs, detected_idxs = find_idxs(gt_spots, detected_spots, 
                diff_results[3],  diff_results[4])

            df_spots = pd.DataFrame({"idx_gt":gt_idxs,"idx_detected":detected_idxs,
                         "val_gt":[str(x) for x in diff_results[3]],
                         "val_detected":[str(x) for x in diff_results[4]],
                         "euc_dist":diff_results[5]})
            
            pp = os.path.join(os.getcwd(), 'indiv_spot', f'{tool}{im_num}_{counter}.csv')
            df_spots.to_csv(pp, index=False)


        loc_diffs.append([str(d) for d in diff_results[:3]])
        loc_all_files.append(p)

        counter = counter + 1
        

with open(f'{tool}{analysis}{indiv_spot}_diff_results_{im_num}.txt', 'w') as f:
    for i,d in enumerate(loc_diffs):
        f.write(loc_all_files[i])    
        f.write(', ')
        f.write(",".join(d))
        f.write('\n')
    