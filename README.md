# Radial Symmetry Localization
Implementation of Radial Symmetry Localization for Java using ImgLib2 (http://imglib2.net)

After the Nature Methods paper "Rapid, accurate particle tracking by calculation of radial symmetry centers" by Raghuveer Parthasarathy (http://www.nature.com/nmeth/journal/v9/n7/abs/nmeth.2071.html)

Including derivation for 3d + integration with RANSAC

License: GPLv2, written by Stephan Preibisch & Timothee Lionnet


# Running pipeline on cluster 

Important to mention:

1. Preprocessing step
- Subtract background (median filtering);
- Subtract `medianMedianPerPlane` from each pixel;
- Normalize image between [min_intensity, max_intensity] => [0, 1].

2. RS step 
- Run radial symmetry;
- Filter spots outside ROI;
- Run z-correction on the detected spots.

3. Python step
- Plot distibutions of the spots;
- Fit Gamma distibution over the histogram;
- Find `center` of the distibution. 

4. Preprocessing step
- Normalize image between [min_intensity, max_intensity] => [0, `center`];

5. RS step
- Run radial symmetry.

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

Some constants that are used below in naming convetion.

- `<type>` -- {`DPY-23_EX`, `MDH-1`}
- `<channel>` -- image channel {0, 1, 2, 3, 4};
- `<line>` -- {`SEA-12`, `N2`};
- `<id>` -- can be any integer number;

Here are the folders with description of the files contained and format.

- `centers` -- format: `all-centers.csv` contains centers of all images from the batch; 
- `channels` -- format: `<channel>-<line>_<id>.tif`; images split into channels.
- `csv` -- format: `<channel>-<line>_<id>.csv`; csv-files with the detections after 1st RS run.
- `csv-2` -- format: `<channel>-<line>_<id>.csv`; csv-files with the detections after 2nd RS run.
- `csv-before` -- format: `<channel>-<line>_<id>.csv`; csv-files with the detections after 1st RS run but _before z-correction_.
- `csv-parameters` -- format: `<channel>-<line>_<id>.csv`; csv-files with the z-correction parameters (quadratic fit).
- `histograms` -- format: `<type>/<channel>-<line>_<id>.pdf`; subfolders for each `<type>` with distribution of detections for each separate image;
- `median` -- format: `<channel>-<line>_<id>.tif`; images after 1st iteration of processing.
- `median-2` -- format: `<channel>-<line>_<id>.tif`; images after 2nd iteration of processing.
- `roi` -- format: `<line>_<id>.tif`; masks.
- `smFISH-database` -- format: `<line>-Table 1.csv`; csv-file with all information about the images. 
- `zCorrected` -- format: `<channel>-<line>_<id>.tif`; images after 1st z-correction.
- `zCorrected-2` -- format: `<channel>-<line>_<id>.tif`; images after 2nd z-correction.

If you are looking for GUI step-by-step guide: https://imagej.net/Radial_Symmetry.


- it is a 2 step project for radial symmetry;
- in between steps we run python script;


Files to cover:
- Name of the class with the main function
- how cluster .sh file looks like
- how do you set the parameters 
- what are the input variables 
- folder structure, write a script to create all necessary folders before hand
- remember to write what type of are stored in the folders 


Pipeline description 
- Filtering images (Median, normalization, z-correction) > what is set to what
- Computations of the centers for the dected spots
- Filtering images (Normalization) > what is set to what
- Perfectly defined spots

TODO:
- link to the specific class files  