dir = 'YOURINPUTPATH';
timeFile = "/YOURPATHTOSAVETIMES/times.txt";

names = newArray('C0_CB428_22_cropped_4065.tif', 'C0_N2_1093_cropped_3801.tif', 'C1_CB428_22_cropped_4065.tif', 
'C2_CB428_22_cropped_4065.tif', 'C0_N2_1121_cropped_3785.tif', 'C2_N2_1121_cropped_3785.tif', 'C0_N2_1449_cropped_3615.tif', 
 'C2_N2_1449_cropped_3615.tif', 'C0_N2_1639_cropped_3974.tif', 'C1_N2_1449_cropped_3615.tif', 'C2_N2_702_cropped_1620.tif',
'C0_N2_702_cropped_1620.tif', 'C2_RNAi_dpy27_1369_cropped_5751.tif');



for (i=0; i<13; i++) {

	open("/home/ella/Desktop/Selected_simulation/embryos_FISH/" + names[i]);

	startTime = getTime();

	run("RS-FISH", "image=" + names[i] + " " + 
			"mode=Advanced anisotropy=0.6500 robust_fitting=[RANSAC] use_anisotropy " + 
			"image_min=0 image_max=65535 sigma=1.203 threshold=0.0025 support=3 " +
			"min_inlier_ratio=0.30 max_error=1.12237 spot_intensity_threshold=0 " +
			"background=[No background subtraction] background_subtraction_max_error=0.05 " +
			"background_subtraction_min_inlier_ratio=0.10 results_file=[/home/ella/Desktop/" + dir + "/" + names[i] + ".csv] " +
			"use_multithreading num_threads=40 block_size_x=128 block_size_y=128 block_size_z=16");

	exeTime = getTime() - startTime; //in miliseconds
	File.append(exeTime + "\n ", timeFile);
	
	close();

}