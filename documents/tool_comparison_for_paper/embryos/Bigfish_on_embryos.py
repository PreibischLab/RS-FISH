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



ims_path = '/YOURPATH/RSFISH_embryos'
output_path = 'YOUROUTPUTPATH'

files = ['C0_CB428_22_cropped_4065.tif', 'C0_N2_1093_cropped_3801.tif', 'C1_CB428_22_cropped_4065.tif', 
'C2_CB428_22_cropped_4065.tif', 'C0_N2_1121_cropped_3785.tif', 'C2_N2_1121_cropped_3785.tif', 'C0_N2_1449_cropped_3615.tif', 
 'C2_N2_1449_cropped_3615.tif', 'C0_N2_1639_cropped_3974.tif', 'C1_N2_1449_cropped_3615.tif', 'C2_N2_702_cropped_1620.tif',
'C0_N2_702_cropped_1620.tif', 'C2_RNAi_dpy27_1369_cropped_5751.tif']

sigmas_xy = [1.25, 1.3, 1.9, 1.75, 2.0, 2.0, 1.75, 2.0, 2.0, 2.0, 1.25, 1.0, 2.0]
sigmas_z = [2.25, 2.5, 2.25, 1.75, 2.5, 2.25, 2.5, 2.5, 2.25, 2.5, 2.25, 1.5, 1.5]


thrs = [500, 428, 170, 220, 156, 340, 250, 130, 170, 370, 445, 820, 222]

# set voxel size:
voxel_size_z = 1
voxel_size_yx = 1


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



## for embryos images due to time - not running anything after spot detection.
only_detect_spot = True
#print('only detect spots',only_detect_spot)


#all_ims_dirs = sorted(glob(os.path.join(path,ims_path,"*")))

paths_ims_to_process = [os.path.join(ims_path,f) for f in files]

# Get default sigma value from dir name:


def process_im(im, sigma, voxel_size_z, voxel_size_yx, psf_z, psf_yx, thr, im_path, only_detect_spot, output_path):
    
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


    print(f'starting detect_spots sigmas {sigma} thr {thr}')
    
    ## Detect spots
    time_tmp = time.time()
    spots, _ = detection.spots_thresholding(rna_log, mask, thr)
    time4 = int(round((time.time() - time_tmp)*1000))
    print('ran detect spots', flush=True)


    n_spots = spots.shape[0]

    print(f'sigmas={sigma} threshold={thr:.3f}, detected #spots={spots.shape[0]}', flush=True)
    
    conditions_str = (f'FishQuant_results_{os.path.basename(im_path)}'
                      f'_sigz{sigma[0]}sigyx{sigma[1]}thr{thr:.3f}')
    
    ## save the spots (int)
    df = pd.DataFrame(data=spots, columns=["z", "y", "x"])
    df = df[["x","y","z"]]
    df_path = os.path.join(output_path, f'{conditions_str}_noSubLocalization.csv')
    df.to_csv(df_path, index=False)
    # just in case we want to run it further:
    #np.save(f'{df_path[:-4]}.npy', spots)
    print(f'{df_path} saved', flush=True)

    if not only_detect_spot:

        time_tmp = time.time()
        spots_subpixel = detection.fit_subpixel(im, spots, voxel_size_z, voxel_size_yx, psf_z, psf_yx)
        time5 = int(round((time.time() - time_tmp)*1000))
        print('ran fit gaussian for subpixel localization', flush=True)

        print(f'Times:log{time1}_localmax{time2}_findspots{time4}_gaussfit{time5}', flush=True)
        print(f'Times w gaussian: {time1 + time2 + time4 + time5}', flush=True)
        print(f'Times without gaussian: {time1 + time2 + time4}', flush=True)

        df = pd.DataFrame(data=spots_subpixel, columns=["z", "y", "x"])
        df = df[["x","y","z"]].round(3)
        df_path = os.path.join(output_path, f'{conditions_str}_withSubLocalization.csv')
        df.to_csv(df_path, index=False)
        print(f'{df_path} saved', flush=True)

        print('')
                                

for i,im_path in enumerate(paths_ims_to_process):

    im = stack.read_image(im_path)
    print(f'processing image: {im_path}', flush=True)


    sigma_z = sigmas_z[i]

    sigma_yx = sigmas_xy[i] 

    thr = thrs[i]

    psf_z = sigma_z * voxel_size_z
    psf_yx = sigma_yx * voxel_size_yx

    sigma = (sigma_z, sigma_yx, sigma_yx)

    print(f'starting to process image with sigmas {sigma}')

    process_im(im, sigma, voxel_size_z, voxel_size_yx, psf_z, psf_yx, thr, im_path, only_detect_spot, output_path)

