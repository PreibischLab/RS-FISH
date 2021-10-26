#!/usr/bin/env python
# coding: utf-8

import numpy as np
import os
from glob import glob
import re
import time
import pandas as pd
import argparse
import bigfish.stack as stack
import bigfish.detection as detection
import bigfish
print("Big-FISH version: {0}".format(bigfish.__version__), flush=True)


parser = argparse.ArgumentParser(description='for cluster multiple jobs - im_num is given as arg')
parser.add_argument("--im_num")
args = parser.parse_args()
if args.im_num:
    im_num = int(args.im_num)
else:
    print('need to pass image number')
### We have 63 ims - give each job an image (as a number in a sorted list below) as argument



## I've copied their code and changed it a bit to get all possible thresholds
def _get_breaking_point(x, y):
    """Select the x-axis value where a L-curve has a kink.
    Assuming a L-curve from A to B, the 'breaking_point' is the more distant
    point to the segment [A, B].
    Parameters
    ----------
    x : np.array, np.float64
        X-axis values.
    y : np.array, np.float64
        Y-axis values.
    Returns
    -------
    breaking_point : float
        X-axis value at the kink location.
    x : np.array, np.float64
        X-axis values.
    y : np.array, np.float64
        Y-axis values.
    """
    # select threshold where curve break
    slope = (y[-1] - y[0]) / len(y)
    y_grad = np.gradient(y)
    m = list(y_grad >= slope)
    j = m.index(False)
    m = m[j:]
    x = x[j:]
    y = y[j:]
    if True in m:
        i = m.index(True)
    else:
        i = -1
    breaking_point = float(x[i])

    return breaking_point, x, y

def _get_spot_counts(thresholds, value_spots):
    """Compute and format the spots count function for different thresholds.
    Parameters
    ----------
    thresholds : np.ndarray, np.float64
        Candidate threshold values.
    value_spots : np.ndarray
        Pixel intensity values of all spots.
    Returns
    -------
    count_spots : np.ndarray, np.float64
        Spots count function.
    """
    # count spots for each threshold
    count_spots = np.log([np.count_nonzero(value_spots > t)
                          for t in thresholds])
    count_spots = stack.centered_moving_average(count_spots, n=5)

    # the tail of the curve unnecessarily flatten the slop
    count_spots = count_spots[count_spots > 2]
    thresholds = thresholds[:count_spots.size]

    return thresholds, count_spots


def spots_thresholding(image, mask_local_max, threshold,
                       remove_duplicate=True):
    """Filter detected spots and get coordinates of the remaining spots.
    In order to make the thresholding robust, it should be applied to a
    filtered image (bigfish.stack.log_filter for example). If the local
    maximum is not unique (it can happen with connected pixels with the same
    value), connected component algorithm is applied to keep only one
    coordinate per spot.
    Parameters
    ----------
    image : np.ndarray
        Image with shape (z, y, x) or (y, x).
    mask_local_max : np.ndarray, bool
        Mask with shape (z, y, x) or (y, x) indicating the local peaks.
    threshold : float or int
        A threshold to discriminate relevant spots from noisy blobs.
    remove_duplicate : bool
        Remove potential duplicate coordinates for the same spots. Slow the
        running.
    Returns
    -------
    spots : np.ndarray, np.int64
        Coordinate of the local peaks with shape (nb_peaks, 3) or
        (nb_peaks, 2) for 3-d or 2-d images respectively.
    mask : np.ndarray, bool
        Mask with shape (z, y, x) or (y, x) indicating the spots.
    """
    # check parameters
    stack.check_array(image,
                      ndim=[2, 3],
                      dtype=[np.uint8, np.uint16, np.float32, np.float64])
    stack.check_array(mask_local_max,
                      ndim=[2, 3],
                      dtype=[bool])
    stack.check_parameter(threshold=(float, int),
                          remove_duplicate=bool)

    # remove peak with a low intensity
    mask = (mask_local_max & (image > threshold))
    if mask.sum() == 0:
        spots = np.array([], dtype=np.int64).reshape((0, image.ndim))
        return spots, mask

    # make sure we detect only one coordinate per spot
    if remove_duplicate:
        # when several pixels are assigned to the same spot, keep the centroid
        cc = label(mask)
        local_max_regions = regionprops(cc)
        spots = []
        for local_max_region in local_max_regions:
            spot = np.array(local_max_region.centroid)
            spots.append(spot)
        spots = np.stack(spots).astype(np.int64)

        # built mask again
        mask = np.zeros_like(mask)
        mask[spots[:, 0], spots[:, 1]] = True

    else:
        # get peak coordinates
        spots = np.nonzero(mask)
        spots = np.column_stack(spots)

    return spots, mask

def _get_candidate_thresholds(pixel_values):
    """Choose the candidate thresholds to test for the spot detection.
    Parameters
    ----------
    pixel_values : np.ndarray
        Pixel intensity values of the image.
    Returns
    -------
    thresholds : np.ndarray, np.float64
        Candidate threshold values.
    """
    # choose appropriate thresholds candidate
    start_range = 0
    end_range = int(np.percentile(pixel_values, 99.9999))
    if end_range < 100:
        thresholds = np.linspace(start_range, end_range, num=100)
    else:
        thresholds = [i for i in range(start_range, end_range + 1)]
    thresholds = np.array(thresholds)

    return thresholds
def automated_threshold_setting(image, mask_local_max):
    """Automatically set the optimal threshold to detect spots.
    In order to make the thresholding robust, it should be applied to a
    filtered image (bigfish.stack.log_filter for example). The optimal
    threshold is selected based on the spots distribution. The latter should
    have a kink discriminating a fast decreasing stage from a more stable one
    (a plateau).
    Parameters
    ----------
    image : np.ndarray
        Image with shape (z, y, x) or (y, x).
    mask_local_max : np.ndarray, bool
        Mask with shape (z, y, x) or (y, x) indicating the local peaks.
    Returns
    -------
    optimal_threshold : int
        Optimal threshold to discriminate spots from noisy blobs.
    """
    # check parameters
    stack.check_array(image,
                      ndim=[2, 3],
                      dtype=[np.uint8, np.uint16, np.float32, np.float64])
    stack.check_array(mask_local_max,
                      ndim=[2, 3],
                      dtype=[bool])

    # get threshold values we want to test
    thresholds = _get_candidate_thresholds(image.ravel())

    # get spots count and its logarithm
    first_threshold = float(thresholds[0])
    spots, mask_spots = spots_thresholding(
        image, mask_local_max, first_threshold, remove_duplicate=False)
    value_spots = image[mask_spots]
    thresholds, count_spots = _get_spot_counts(thresholds, value_spots)

    # select threshold where the kink of the distribution is located
    optimal_threshold, _, _ = _get_breaking_point(thresholds, count_spots)

    return thresholds.astype(float), optimal_threshold


#path = '/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/'
path = '/scratch/AG_Preibisch/Ella/RS_and_FQ_gridsearch_compare/'
ims_path = 'Selected_simulation/'

## Set parameters:

## As sigma depands on the dir name we set what to add to sigma:
sigma_yx_range = np.arange(-0.5,0.51,0.25)
sigma_z_range = np.arange(-0.5,0.51,0.25)

## as threshold depands on the default threshold, 
# we just choose locations in the threshold array in referance to the default
# The default is the first, so it will be ran for sure:
thr_range = [0,-6,-3,-2,-1,1,2,3,6]

##### Dense region decomposition parameters:

## Gamma - used for denoising: large_sigma = tuple([sigma_ * gamma for sigma_ in sigma])
## Five is default
gamma_range = [4, 4.5, 5, 5.5, 6.1]

alpha_range = np.arange(0.5,0.8,0.1)

beta_range = np.arange(0.8,1.3,0.1)

# set voxel size:
voxel_size_z = 1
voxel_size_yx = 1

#all_ims_dirs = sorted(glob(os.path.join(path,ims_path,"*")))

paths_ims_to_process = sorted(glob(os.path.join(path,ims_path,'*','*.tif')))

im_path = paths_ims_to_process[im_num]

# Get default sigma value from dir name:

if "embryos_FISH" in im_path:
    default_sig_z = 2
    default_sig_yx = 1.5

else:
    sigs_str = os.path.basename(os.path.dirname(im_path))

    default_sig_z = int(sigs_str.split(" ")[-1])

    sigma_yx_str = re.search('Sigxy (.*) SigZ', sigs_str).group(1)
    default_sig_yx = float(sigma_yx_str) if 'pt' not in sigma_yx_str else float(sigma_yx_str.replace("pt","."))

def process_im(im, sigma, voxel_size_z, voxel_size_yx, psf_z, psf_yx, im_path, gamma_range, alpha_range, beta_range):
    
    ## Filter image LoG:
    time_tmp = time.time()
    rna_log = stack.log_filter(im, sigma)
    time1 = int(round((time.time() - time_tmp)*1000))
    print('ran log', flush=True)
    
    ## Detect local maxima:
    time_tmp = time.time()
    mask = detection.local_maximum_detection(rna_log, min_distance=sigma)
    time2 = int(round((time.time() - time_tmp)*1000))
    print('ran local maxima', flush=True)
    
    ## Find defualt threshold + all thresholds:
    time_tmp = time.time()
    all_thrs, default_thr = automated_threshold_setting(rna_log, mask)
    time3 = int(round((time.time() - time_tmp)*1000))
    print(f'ran find threshold, default thr found: {default_thr}', flush=True)
    
    # Find thresholds to test around the default
    idx_default_thr = np.where(all_thrs==default_thr)[0][0]
    thrs_to_use_idxs = [it+idx_default_thr for it in thr_range if 0<(it+idx_default_thr)<all_thrs.size]
    thrs_to_use = all_thrs[thrs_to_use_idxs]
    
    ## Iterate thresholds
    
    # So we won't run the same spots found - if two or more thresholds have same # of spots
    # Only one is ran.
    n_spots = []
    ## GRID PARAMETER
    for threshold in thrs_to_use:
        
        ## Detect spots
        time_tmp = time.time()
        spots, _ = detection.spots_thresholding(rna_log, mask, threshold)
        time4 = int(round((time.time() - time_tmp)*1000))
        print('ran detect spots', flush=True)
        
        thr_n_spots = spots.shape[0]
        ## Compare #spots to GT #spots. If too far off, don't waste run time on more acurate calcs.
        GT_n_spots = int(re.search('.*_(\d+)spots', im_path).group(1) if "spots" in im_path else thr_n_spots)
        
        if (thr_n_spots not in n_spots):
            
            n_spots.append(thr_n_spots)
            print(f'sigmas={sigma} threshold={threshold:.3f}, detected #spots={spots.shape[0]}', flush=True)
            
            conditions_str = (f'FishQuant_results_{os.path.basename(im_path)}'
                              f'_sigz{sigma[0]}sigyx{sigma[1]}thr{threshold:.3f}')
            
            ## save the spots (int)
            df = pd.DataFrame(data=spots, columns=["z", "y", "x"])
            df = df[["x","y","z"]]
            df_path = os.path.join(os.path.dirname(im_path), f'{conditions_str}_noSubLocalization.csv')
            df.to_csv(df_path, index=False)
            # just in case we want to run it further:
            np.save(f'{df_path[:-4]}.npy', spots)
            print(f'{df_path} saved', flush=True)
            
            ## Conditions to see if the number of spots detected is too far off
            ## Then no need to run the next part.
            if ((thr_n_spots/GT_n_spots>1.25) and (thr_n_spots-GT_n_spots>300)):                
                print(f'Times:log{time1}_localmax{time2}_findthr{time3}_findspots{time4}', flush=True)   
            else:

                time_tmp = time.time()
                spots_subpixel = detection.fit_subpixel(im, spots, voxel_size_z, voxel_size_yx, psf_z, psf_yx)
                time5 = int(round((time.time() - time_tmp)*1000))
                print('ran fit gaussian for subpixel localization', flush=True)

                print(f'Times:log{time1}_localmax{time2}_findthr{time3}_findspots{time4}_gaussfit{time5}', flush=True)

                df = pd.DataFrame(data=spots_subpixel, columns=["z", "y", "x"])
                df = df[["x","y","z"]].round(3)
                df_path = os.path.join(os.path.dirname(im_path), f'{conditions_str}_withSubLocalization.csv')
                df.to_csv(df_path, index=False)
                print(f'{df_path} saved', flush=True)

                ### Dense region decomposition:

                ## GRID PARAMETER
                for gamma in gamma_range:
                    ## GRID PARAMETER
                    for alph in alpha_range:
                        alpha = round(alph,1)
                        ## GRID PARAMETER
                        for beta in beta_range:

                            print(f'starting dense regions decomposition with alpha {alpha}, beta {beta}, gamma {gamma}', flush=True)

                            time_tmp = time.time()
                            large_sigma = tuple([sigma_ * gamma for sigma_ in sigma])
                            spots_post_decomposition, dense_regions, reference_spot = detection.decompose_dense(
                                im, spots, voxel_size_z, voxel_size_yx, psf_z, psf_yx,
                                alpha=alpha,  # alpha impacts the number of spots per candidate region
                                beta=beta,  # beta impacts the number of candidate regions to decompose
                                gamma=gamma)  # gamma the filtering step to denoise the image

                            time6 = int(round((time.time() - time_tmp)*1000))
                            print(f'ran dense regions decomposition. New #spots={spots_post_decomposition.shape[0]}', flush=True)
                            
                            if spots_post_decomposition.shape[0]<thr_n_spots:
                                print('#spots after decomposition < #spots before. Not saving results.')
                                
                            else:

                                print(f'Times:denseregdecomp{time6}', flush=True)

                                df = pd.DataFrame(data=spots_post_decomposition, columns=["z", "y", "x"])
                                df = df[["x","y","z"]]
                                df_path = os.path.join(os.path.dirname(im_path), 
                                                    (f'{conditions_str}_alpha{alpha}_beta{beta}_gamma{gamma}_noSubLocalization.csv'))
                                df.to_csv(df_path, index=False)
                                # In case we want to run further analysis:
                                np.save(f'{df_path[:-4]}.npy', spots_post_decomposition)
                                print(f'{df_path} saved', flush=True)
                                

im = stack.read_image(im_path)
print(f'processing image: {im_path}', flush=True)

## GRID PARAMETER
for sig_z_delta in sigma_z_range:

    sigma_z = sig_z_delta + default_sig_z

    ## GRID PARAMETER
    for sig_yx_delta in sigma_yx_range:
        sigma_yx = sig_yx_delta + default_sig_yx

        psf_z = sigma_z * voxel_size_z
        psf_yx = sigma_yx * voxel_size_yx

        sigma = (sigma_z, sigma_yx, sigma_yx)

        process_im(im, sigma, voxel_size_z, voxel_size_yx, psf_z, psf_yx, 
                   im_path, gamma_range, alpha_range, beta_range)

