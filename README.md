<div align="center">
  
# RS-FISH documentation

</div>

_**RS-FISH: Precise, interactive, fast, and scalable FISH spot detection**_

Ella Bahry\*, Laura Breimann\*, Marwan Zouinkhi\*, Leo Epstein, Klim Kolyvanov, Xi Long, Kyle I S Harrington, Timothée Lionnet, Stephan Preibisch  
*[bioRxiv](https://www.biorxiv.org/content/10.1101/2021.03.09.434205v1)* 2021, doi: https://doi.org/10.1101/2021.03.09.434205  

\* equal contribution  

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/detection_preview.png" alt="RS_FISH screenshot detection preview" width="300">

### Content

* [_**1.	Abstract & Availability**_](#abstract)
* [_**2.Download & Installation**_](#download)
* [_**3.	Calculating Anisotropy Coefficient**_](#anisotropy)
* [_**4.	RS-FISH tutorial**_](#tutorial)
* [_**5.	Show detections**_](#detections)
* [_**6.	Batch processing using RS-FISH**_](#batch)
* [_**7. Running RS-FISH on large volumes**_](#spark)
* [_**8. Filtering detections using a binary mask**_](#mask)
* [_**9. Choosing the right parameters for RS-FISH**_](#parameters)

<br />
<br />

<div style="text-align: justify">
 
### 1.	Abstract & Availability<a name="abstract">
</a> 

Fluorescent in-situ hybridization (FISH)-based methods are powerful tools to study molecular processes with subcellular resolution, relying on accurate identification and localization of diffraction-limited spots in microscopy images. We developed the Radial Symmetry-FISH (RS-FISH) software that accurately, robustly, and quickly detects single-molecule spots in two and three dimensions, making it applicable to several key assays, including single-molecule FISH (smFISH), spatial transcriptomics, and spatial genomics. RS-FISH allows interactive parameter tuning and scales to large sets of images and tera-byte sized image volumes such as entire brain scans using straight-forward distributed processing on workstations, clusters, and in the cloud.
  
 <br />
<br />
Availability and implementation:
  
RS-FISH is implemented as open-source in Java/ImgLib2 and provided as a macro-scriptable Fiji plugin and stand-alone command-line application capable of cluster and cloud execution.   

Code source, tutorial, documentation, and example images are available at:  https://github.com/PreibischLab/RS-FISH and https://github.com/PreibischLab/RS-FISH-Spark 

<br />

 ### 2.	Download & Installation <a name="download">
</a> 
  

The RS-FISH plugin can be downloaded via the Fiji Updater. Go to ```Help > Update …```, click ```Manage update sites``` and select **Radial Symmetry** in the list. Then click ```“Apply changes”``` and restart Fiji. You will now find the RS-FISH plugin under ```Plugins```.  

The source code is available on [GitHub](https://github.com/PreibischLab/RadialSymmetryLocalization ). If you encounter bugs or want to report a feature request, please report everything there.  


The simulation data that was generated to benchmark the method can be downloaded [here](https://github.com/PreibischLab/RadialSymmetryLocalization/tree/master/documents/Simulation_of_data ). The smFISH image of the _C. elegans_ embryo can be found [here](https://github.com/PreibischLab/RadialSymmetryLocalization/tree/master/documents/Example_smFISH_images )  

#### 2.1. Installation as command-line tools

Command-line installation requires maven (>=3.8.1) and OpenJDK8 (or newer) on Ubuntu:
```bash
sudo apt-get install openjdk-8-jdk maven
```
On other platforms, please find your way and report back if interested.

Next, please check out this repository and go into the folder

```
git clone https://github.com/PreibischLab/RS-FISH.git
cd RS-FISH
```

Install into your favorite local binary `$PATH` (or leave empty for using the checked out directory):
```bash
./install $HOME/bin
```
All dependencies will be downloaded and managed by maven automatically.

This currently installs three tools, `rs-fish, rs-fish-anisotropy, csv-overlay`. Please execute them in order to get detailed execution instructions.
<br />
<br />

#### 2.2. Installation using Docker container

Alternatively to installing RS-FISH as a command line tool on your operating system, you can also use it directly from our Docker container. 

To build the docker container, git pull the repository and then execute the following command:

```
docker build -t rs_fish:2.3.1 .
```

To pull the docker container directly from Dockerhub, run the following command:

```
docker pull wuennemannflorian/rs_fish:2.3.1
```

You can then run RS-FISH from the container using the following command:

```
docker run -v [your_local_input_folder]:/input -v [your_local_output_folder]:/output rs-fish:2.3.1 /RS-FISH/rs-fish [rs_fish_parameter] --image=/input/[your_file.tif] --output=/output/[your_file.csv]
```


### 3.	Calculating Anisotropy Coefficient<a name="anisotropy">
</a> 

<br />

Since the effective size of objects along the z-axis is usually different than in the x- and y-axis of your images, you should correct this to achieve a more accurate smFISH detection. To estimate your anisotropy coefficient, you can acquire a fluorescent bead image on the same microscope, using the same settings and equipment or you can use the smFISH image directly.  

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/one_spot.png" alt="RS_FISH screenshot one spot" width="400">

Open the image with the beads or the smFISH detections and navigate to the ```Plugins > RS-FISH > Tools > Calculate Anisotropy Coefficient```.
You will see the dialog window:  

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/calculate_anisotropy_coefficient.png" alt="RS_FISH screenshot anisotropy menu" width="400">

Make sure your bead image is selected in the **Image** drop-down menu. Next, you can choose between two **Detection methods**: **Gauss fit** or **Radial Symmetry**. If you have fewer detections Gaussian fit might be the better choice, however, both methods usually provide reasonable results. It can even be useful to simply average the results of both methods. The resulting number can be visually confirmed by turning the input image around its x or y-axis (```Image > Stacks > Reslice > Top```) as it simply describes the ratio of the size in z versus xy.  
After you choose a detection method, two windows will open once you press **OK**.   

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/adjust_DoG.png" alt="RS_FISH screenshot DoG menu" width="400">

In the **Adjust difference-of-gaussian values** window, you can choose **Sigma** and **Threshold** values to detect the majority of subpixel resolution spots.  

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/one_spot_detected.png" alt="RS_FISH screenshot one spot detection" width="400">

Once you are done – press the **Done** button.  

Depending on the number of spots, the calculations might take some time as the Gaussian fit is slower and the RS-RANSAC needs to iterate over a range of potential anisotropy coefficients. The program will calculate the corresponding anisotropy coefficient, which shows how we should squeeze the objects in the z-axis to make them look radially symmetric.  

The Log window will show the corresponding anisotropy value, and it should be transferred to the next step automatically.  

_Important: It is OK to skip this step if the objects are more or less round in 3D. The plugin will be able to do a decent job even with the default value of the anisotropy coefficient. However, we advise performing this prior to actual RS detection._  


### 4.	Running RS-FISH<a name="tutorial">
</a> 

_**Localizing Spots**_

The main RS-FISH plugin can be found under: ```Plugins > RS-FISH > RS-FISH```. There are two different modes of processing images: **interactive** and **advanced**. The Interactive method is used to adjust the parameters for further dataset processing or is the right choice if single images need to be processed. The interactive mode provides the visual feedback necessary to adjust the advanced automated processing parameters on large datasets.    

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/smFISH_embryo.png" alt="Embryo with smFISH spots" width="600">

_**Interactive mode**_

Open a 2D or 3D single-channel image for analysis and navigate to the ```Plugins``` menu under ```RS-FISH > RS-FISH```.
A window will pop up.  

Ensure that the correct image is chosen in the **Image** drop-down menu. 
Next, you choose the **Mode** that you want to run RS-FISH in. For finding the best parameters or analyzing a small set of images, choose the **Interactive Mode**.   

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/RS_interactive.png" alt="RS_FISH interactive menu" width="600">

**Anisotropy coefficient** defines how much the z-resolution differs from the x/y-resolution. The parameter would be set automatically if you ran the **Calculate Anisotropy Coefficient** plugin before-hand (```Plugins > RS-FISH > Calculate Anisotropy Coefficient```). In general, 1.0 gives a good result if the spots are somewhat round in 3D. You can choose to use the same anisotropy coefficient for computing the Difference of Gaussian (DoG), which will lead to a more robust DoG detection for anisotropic spots.  

There are various options for **Robust fitting Computation**.   

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/select_RANSAC.png" alt="RS_FISH RANSAC dropdown menu" width="400">

* **RANSAC** defines if you want to use radial symmetry with robust outlier removal, it will identify all gradients within every local patch that supports the same center point (Fig. 1)  
* **No RANSAC** for the use of radial symmetry without robust outlier removal, simply all gradients of a local spot will be used to compute the center point (classic RS)  
* **Multiconsensus RANSAC** will iteratively run robust outlier removal on each local patch until all sets of gradients are identified that support a center point. This allows RS-FISH to potentially find multiple points within each local patch that was identified using DoG detections.  
  
The next option you can choose is to **compute the min and max intensity from image**. If you _deselect_ the option, in the next step you can choose range of intensity of the image, which might be a good option if you are processing a set of images with the same accquisition parameters.  
 
<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/image_min_max.png" alt="RS_FISH image intensity selection window" width="400">  
  
There are two options in RS-FISH to calculate the spot intensity value. Each spot’s associated intensity values are by default computed using linear interpolation at the spot’s sub-pixel location. By selecting the option **Refine spot intensity with Gaussian fit on inliers** the spot intensity can be refined by fitting a Gaussian to the subset of pixels that support the spot as identified by RS-RANSAC.
 

The last option in the **Visualization** section is whether you want to add the detected spots directly to the ROI Manager at the end of the detection.

Once you are done with the settings, press the **OK** button.  
In the second step, and based on your selection, multiple windows will open.  

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/adjust_DoG_values.png" alt="RS_FISH screenshot DoG vals slide bars" width="400">
<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/detection_preview.png" alt="RS_FISH detection preview" width="600">

In the **Difference of Gaussian** window, you can adjust the parameters for the initial detection of the spots. The goal of this step is to minimize false detections. Adjust the **Sigma** and **Threshold** slider so that the red circles in the image detect as many single spots as possible. Try to slightly find more spots if you chose RANSAC; the RANSAC window allows additional restrictive settings. If you are working with a 3D stack, it helps to constantly move through z while adjusting the parameters as the red circle appears only in the z-slices where the signal is the strongest. It can help to adjust the yellow preview box during this step. If the image is very large, it can help to choose a smaller box to speed up the visualization (the detection will be performed in the whole image). 

_Important: If you choose to run RANSAC robust fitting, don’t click the Done button on the Difference of Gaussian window at this step; simply continue setting the parameters in the Adjust RANSAC values window._

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/adjust_RANSAC_values.png" alt="RS_FISH screenshot RANSAC vals menu" width="400">

The **Adjust RANSAC values** dialog allows you to find the right setting for the robust outlier removal. The **Support Region Radius** defines the radius for each spot in which gradients are extracted for RS. You might want to play with this parameter. Sometimes it is helpful to increase the radius and decrease the Inlier Ratio at the same time. The **Inlier ratio** defines the ratio of the gradients (i.e. pixels) that have to support the RS center point (Simply speaking, the ratio of pixels should 'belong' to the current spot), given the **Max error** that defines maximally allowed error for RS fitting (see Fig. 1). 

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/RANSAC_preview.png" alt="RS_FISH RANSAC preview" width="600">

While moving the sliders, you will see the updates in both image windows. Firstly the **RANSAC preview** window displays the pixels used by RANSAC and the error values at each of the used pixels. The second window is the initial image window with the preview of the detections. Additionally to the red circles, the blue crosses indicate spots that were detected using RANSAC outlier removal. So the goal of this part is to find all spots with a red circle and a blue cross inside while not detecting noise or background.   
 
 
The background removal step allows you to remove a non-planar background prior to computing the RS. It will try to estimate a plane using the intensity values of the edges of each local patch using any of the outlined methods. _Note: constant backgrounds do not need to be removed, only if strong gradients are present._

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/background.png" alt="RS_FISH screenshot background menu" width="400">

Once the parameters are adjusted, hit any of the **Done** buttons and wait a bit while the computations are performed.

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/intensity_distribution.png" alt="RS_FISH screenshot intensity distribution histogram" width="600">
<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/display_spots.png" alt="RS_FISH display spots" width="600">

In the next window, you have the option of thresholding the detected spots based on their intensity. The **Intensity distribution** window displays all detected spots and their corresponding intensity value as a histogram. By clicking at an intensity value in the histogram, the blue thresholding bar can be adjusted. All spots that currently pass the thresholding are displayed in the image window and marked by a red circle. If you are satisfied with the selected spots, press the **OK** button and continue to the final results table. 

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/final_log.png" alt="RS_FISH log" width="600">

The **Log** window gives you a summary of all spots found at every step and the final number of detections. The **Results table** contains the spot coordinates, time, channel, and intensity values in the corresponding columns. You can save the results and use them in the **Show Detections** part of the plugin to visualize all found spots’ locations. 

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/results_csv.png" alt="RS_FISH result table" width="400">


_**Advanced mode**_

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/RS_advanced.png" alt="RS_FISH advance mode menu" width="600">

In the **Advanced mode**, you can skip the interactive setting of parameters and only use already known parameters for the computation. After choosing **Advanced** in the first window, you will reach the second window to set the parameters for spot detection. If you previously used the interactive mode to find the best parameters, they will be saved and set as default for the advanced mode. 

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/Advanced_options.png" alt="RS_FISH advance mode set parameters menu" width="600">

After you press **OK**, the computation is done in all RS-FISH steps automatically, and the same **Results table** as above is either saved or displayed. 

**Scripting / headless**

When using the advanced mode you can simply record the parameters you used for running RS-FISH (```Plugins > Macro > Record```). You are then able to apply it to a set of images using the Fiji/ImageJ macro language.

### 5.	Show Detections<a name="detections">
</a>  


After RS-FISH computed all spots, the results table can be saved as CSV or directly be used to visualize the detected spots. There are three ways to visulaize the RS-FISH detected spots:

* **ROI Manager**
* **Fiji/ImageJ overlay**
* **Big Data Viewer** 

If the option to transfer spots directly to ROI manager was chosen in the begining, the **ROI manager** will pop up with the results table in the last step. 

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/ROI_Manager.png" alt="Fiji ROI Manager with detected spot coordinates" width="400">


The **Show Detections (ImageJ/Fiji)** plugin (```Plugins > RS-FISH > Tools > Show Detections (ImageJ/Fiji)```) can be used to overlay all spots stored in a CSV onto the current image for visual inspection of the final result using Fiji. The detected spots will be highlighted by red circles that are largest in the z position where the center of the spot is. 


<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/detections_tool_fiji.png" alt="Show detections in Fiji tool" width="600">

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/show_detections.png" alt="Embryo with smFISH spots detection overlay" width="600">

The **Show Detections (BigDataViewer)** plugin (```Plugins > RS-FISH > Tools > Show Detections (BigDataViewer)```) can be used to visualize the spots using the Big Data Viewer. In the first window are four options whether to open a saved image or CSV or use the currently active image and table. The intensity of the overlay points can be changed with the sigma slider. 


<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/detection_tool_bdv.png" alt="Show detection in the Big data viewer" width="600">

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/options_bdv.png" alt="Options for the display in the Big data viewer" width="600">

<img src="https://github.com/PreibischLab/RadialSymmetryLocalization/blob/master/documents/Tutorial_screenshots/bdv.png" alt="Overlay of detected points using the Big Data Viewer" width="800">


### 6.	Batch processing using RS-FISH<a name="batch">
</a> 

For batch processing instructions and running on computing cluster please see the README in the [example_scripts](https://github.com/PreibischLab/RS-FISH/tree/master/documents/example_scripts) folder.


### 7.	Running RS-FISH on large volumes<a name="spark">
</a> 

There is a dedicated repository for [Spark](http://spark.apache.org)-based execution of RS-FISH on large (and also smaller) volumes: https://github.com/PreibischLab/RS-FISH-Spark
  
  
  
  
### 8.	Filtering detections using a binary mask<a name="mask">
</a> 
  
To filter RS-FISH detections, a binary mask (with values of 0 and 1) can be used to identify parts that are inside a cell or selection and outside the structure.
  
 <img src="https://github.com/PreibischLab/RS-FISH/blob/master/documents/Tutorial_screenshots/filter_neurons.png" alt="Image of smFISH spot detection in neuron, filtered with a binary mask" width="700">
  
 
After creating a binary mask with the same dimensions as the original image using Fiji or a different software, the ```Mask filtering``` tool can be used. The tool is located under ```Plugins > RS-FISH > Tools > Mask filtering```. In the interface, one can specify the input file or folder with csv files and the mask file or folder as well as the output folder. The output is a new csv file with detections only in the specified areas of the image. 
  
  
 <img src="https://github.com/PreibischLab/RS-FISH/blob/master/documents/Tutorial_screenshots/mask_filter.png" alt="Screenshot of the mask filtering tool" width="800">
  

  
### 9.	Choosing the right parameters for RS-FISH<a name="parameters">
</a>   

RS-FISH allows the user to choose between different detection methods to localize spots in the image. The tool offers three localization methods that build on each other: 
  1. radial symmetry (‘No RANSAC’)
  2. radial symmetry + RANSAC (the default option)  
  3. radial symmetry + multi-consensus RANSAC 
 
The default option is radial symmetry detection of spots with additional RANSAC outlier removal. In cases where the noise level is good, and an even higher computation speed is preferred, the RANSAC outlier removal step can be skipped by choosing ```“no RANSAC”```. In cases where many spots potentially overlap, we recommend choosing multi-consensus RANSAC spot detection, which will try to separate close points by detecting gradients in several rounds. 

The next step is to choose the parameters for detection, which is simplified by the plugin’s visual feedback. Using the default spot detection method, there are two windows with parameters to set and two different visual feedback cues (see the Tutorial RS-FISH section). The red circle marks spots detected using the radial symmetry parameters, and the blue cross confirms those spots that pass the RANSAC outlier removal. The detection of the red circle can be adjusted in size (```sigma```) and the threshold of the ```signal intensity``` of the DoG pre-detected spots.

The RANSAC parameters determine which and how the gradients are used to localize each spot. The ```support region radius``` determines the number of gradients computed in a local patch around each DoG pre-detected spot. We propose a default radius of 3 pixels which means a 7x7[x7] pixel patch resulting in 216 gradients for the 3D case. Decreasing this number might allow more spots to be found but will decrease localization precision as fewer gradients are used to define the center. The ```inlier ratio``` describes the number of gradients that support the detection's center point. Increasing the inlier ratio increases the robustness of the detected spot but can decrease the total number of detected spots, especially for lower SNR spots. The ```max error threshold``` defines the maximally allowed error for the intersection point defined by the gradients; i.e., how far away from the intersection point can each gradient be (only in a perfect scenario, all gradients intersect perfectly in the same point, see Supp. Fig 1d). Decreasing the number restricts the error, thus decreasing the number of false detections but potentially also the number of real detections, especially if the noise level is higher. 

For the multi-consensus selection, a window pops up where parameters for additional rounds of spot detection can be defined. It is important to realize that for multi-consensus to work well, the max error in the RANSAC settings should be set relatively low since otherwise, the gradients of multiple points are allowed to intersect in a single spot. A typical setting could be 0.5 - 0.75 pixels. It is important to first run RS-FISH in RANSAC mode before setting the multi-consensus parameters. The log record will output two critical parameters: the ```average #inliers``` and ```stdev #inliers```. The ```average #inliers``` defines how many inliers intersect in each spot on average, while the ```stdev #inliers``` describes its variability. The parameters for multi-consensus RANSAC must be defined relative to those two values. The ```“Initial #inlier threshold for new spot (avg - n*stdev) n=”``` asks the user how many inliers are required for it to be considered a new spot next to an already found one. For example, let us assume a 7x7x7 local neighborhood with 216 total candidate gradients. If ```average #inliers``` was 80 and ```stdev #inliers``` was 6, then defining n=8 would require at least 80-48=32 inliers for a new spot to be defined. Keep in mind that only around 136 gradients (=216-80) are left to be evaluated per local patch. Once a new spot was identified, we re-center the local patch around its location and re-compute it. Thus the second parameter, ```“Final #inlier threshold for new spot (avg - n*stdev) n=”```, is usually chosen higher, e.g., n=6, thus requiring 80-32=48 inliers for a final confirmation of a new spot.
  
Additionally, the min number of inliers describes the number of inlier gradients minimally required to localize a point. This value is supposed to prevent the fitting of noise spots that are expected to have a low number of gradients that agree on a specific spot. Typically a value of 30 inliers is a good starting point. Note that if you set ```“Initial #inlier threshold for new spot (avg - n*stdev) n=”``` and ```“Final #inlier threshold for new spot (avg - n*stdev) n=”``` very high, only the ```”min number of inliers”``` will be relevant, which somewhat simplifies the process.

  
 <img src="https://github.com/PreibischLab/RS-FISH/blob/master/documents/Tutorial_screenshots/workflow.png" alt="Image explaining the workflow of RS-FISH" width="800">  
  

License: GPLv2
