# Radial Symmetry Localization
Implementation of Radial Symmetry Localization for Java using ImgLib2 (http://imglib2.net)

After the Nature Methods paper "Rapid, accurate particle tracking by calculation of radial symmetry centers" by Raghuveer Parthasarathy (http://www.nature.com/nmeth/journal/v9/n7/abs/nmeth.2071.html)

Including derivation for 3d + integration with RANSAC

License: GPLv2, written by Stephan Preibisch & Timothee Lionnet

# Tutorial 

If you are looking for GUI step-by-step guide: https://imagej.net/Radial_Symmetry.

# Running pipeline on cluster 

Pipeline description 
- Filtering images (Median, normalization, z-correction) > what is set to what
- Computations of the centers for the dected spots
- Filtering images (Normalization) > what is set to what
- Perfectly defined spots


Important to mention:

1. Preprocessing step
- Subtract background (median filtering) per plane;
- Subtract `medianMedianPerPlane` from each pixel;
- Normalize image between [min_intensity, max_intensity] => [0, 1].

2. RS step 
- Run radial symmetry (see parameters' values below);
- Filter spots outside embryo (ROI);
- Run z-correction on the detected spots (quadratic fit over intensities and z-location).

3. Python step
- Plot distibutions of the spots;
- Fit Gamma distibution over the histogram (Gamma is more stable than Gaussian);
- Find `center` of the distibution. 

4. Preprocessing step
- Normalize image between [min_intensity, max_intensity] => [0, `center`];

5. RS step
- Run radial symmetry (see parameters' values below).

**Folders structure:**

```
|-- centers
|-- channels
|-- csv
|-- csv-2
|-- csv-before
|-- csv-parameters
|-- histograms
|   |-- DPY-23_EX
|   `-- MDH-1
|-- median
|-- median-2
|-- roi
|-- smFISH-database
|-- zCorrected
`-- zCorrected-2
```

Some constants that are used below in naming convention.

- `<type>` -- {`DPY-23_EX`, `MDH-1`}
- `<channel>` -- image channel {0, 1, 2, 3, 4};
- `<line>` -- {`SEA-12`, `N2`};
- `<id>` -- can be any integer number;

Here are the folders with description of the files contained and format.

- `centers` -- format: `all-centers.csv` contains centers of all images from the batch; 
- `channels` -- format: `<channel>-<line>_<id>.tif`; images split into channels.
- `csv` -- format: `<channel>-<line>_<id>.csv`; csv-files with the detections after 1st RS run.
- `csv-2` -- format: `<channel>-<line>_<id>.csv`; csv-files with the detections after 2nd RS run.
- `csv-before` -- format: `<channel>-<line>_<id>.csv`; csv-files with the detections after 1st RS run but _before_ z-correction.
- `csv-parameters` -- format: `<channel>-<line>_<id>.csv`; csv-files with the z-correction parameters (quadratic fit) in the order `a_0`, `a_1`, `a_2`.
- `histograms` -- format: `<type>/<channel>-<line>_<id>.pdf`; subfolders for each `<type>` with distribution of detections for each separate image after 1st RS run;
- `histograms-2` -- format: `<type>/<channel>-<line>_<id>.pdf`; subfolders for each `<type>` with distribution of detections for each separate image after 2nd RS run;
- `median` -- format: `<channel>-<line>_<id>.tif`; images after 1st iteration of processing.
- `median-2` -- format: `<channel>-<line>_<id>.tif`; images after 2nd iteration of processing.
- `roi` -- format: `<line>_<id>.tif`; masks.
- `smFISH-database` -- format: `<line>-Table 1.csv`; csv-file with all information about the images. 
- `zCorrected` -- format: `<channel>-<line>_<id>.tif`; images after 1st z-correction.
- `zCorrected-2` -- format: `<channel>-<line>_<id>.tif`; images after 2nd z-correction.




Files to cover:
- Name of the class with the main function
- how cluster .sh file looks like
- how do you set the parameters 
- what are the input variables 
- folder structure, write a script to create all necessary folders before hand
- remember to write what type of are stored in the folders 


TODO:
- link to the specific class files  


**Important:**
- The parameters for Radial Symmetry runs do not change from experiment-to-experiment and are set to the best I've found empirically: 

```
dogSigma = 1.5
dogThreshold = 0.0081
supportRadius = 3
inlierRatio = 0.37
maxError = 0.5034
anisotropyCoefficient = 1.08
useRansac = true		
```

[File](https://github.com/PreibischLab/RadialSymmetryLocalization/blob/intron-cluster/src/main/java/cluster/radial/symmetry/process/parameters/ParametersFirstRun.java) with the parameters.

For the detailed description of the parameters refer to step-by-step [guide](https://imagej.net/Radial_Symmetry#Detailed_Tutorial).