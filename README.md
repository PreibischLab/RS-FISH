<div align="center">
  
# RS-FISH documentation

</div>

_**For further details have a look at the paper:**_

<br />

_**RS-FISH: Precise, interactive and scalable smFISH spot detection using Radial Symmetry**_
Ella Bahry, Laura Breimann, Leo Epstein, Klim Kolyvanov, Kyle I S Harrington, Timothée Lionnet, Stephan Preibisch
bioRxiv XX; doi: XX

### Content

* _**1.	Introduction & Overview**_
* _**2.	Download**_
* _**3.	RS-FISH tutorial**_
* _**4.	Batch processing with RS-FISH**_

<br />
<br />

<div style="text-align: justify">
 
### 1.	Abstract & Availability
Abstract:
Studying transcription using single-molecule RNA-FISH (smFISH) is a powerful method to gain insights into gene regulation on a single cell basis, relying on accurate identification of sub-resolution fluorescent spots in microscopy images. Here we present Radial Symmetry-FISH (RS-FISH), which can robustly and quickly detect even close single-molecule spots in two and three dimensions with high precision, allows interactive parameter tuning, and can easily be applied to large sets of images. 

Availability and implementation:
RS-FISH is implemented as open-source in Java/ImgLib2 and provided as a macro-scriptable Fiji plugin. Code source, tutorial, documentation, and example images are available at:  https://github.com/PreibischLab/RadialSymmetryLocalization 

Implementation of Radial Symmetry Localization for Java using ImgLib2 (http://imglib2.net)

After the Nature Methods paper "Rapid, accurate particle tracking by calculation of radial symmetry centers" by Raghuveer Parthasarathy (http://www.nature.com/nmeth/journal/v9/n7/abs/nmeth.2071.html)

Including derivation for 3d + integration with RANSAC

<br />
<br />

### 2.	Download

The RS-FISH plugin can be downloaded via the Fiji Updater. Go to ```Help > Update …```, click ```Manage update sites``` and select Radial Symmetry in the list. Then click ```“Apply changes”``` and restart Fiji. You will now find the RS-FISH plugin under ```Plugins```.

The source code is available on [GitHub](https://github.com/PreibischLab/RadialSymmetryLocalization ). If you encounter bugs or want to report a feature request, please report everything there.

<br />

The simulation data that generated to benchmark the method can be downloaded [here](https://github.com/PreibischLab/RadialSymmetryLocalization/documents/Simulation_of_data). The smFISH image of the C. elegans emrbyo can be found [here](https://github.com/PreibischLab/RadialSymmetryLocalization/documents/Example_smFISH_images/)

<br />
<br />







#

License: GPLv2
