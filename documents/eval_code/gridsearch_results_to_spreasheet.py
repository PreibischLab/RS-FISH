import pandas as pd
#TODO split this into two seperate programs; its a bit big.
#read in file from gridsearch CSV 
stats_file = "/home/lpe/Desktop/gridsearchresults.csv"

program = []
image_used = []
sim_settings = []
missed_detections = []
false_detections = []
number_of_spots = []
params_used = []
index = []
euc_dist = []
with open(stats_file) as csvfile:
    next(csvfile)
    file_in = np.asarray(list(csv.reader(csvfile,delimiter=",")))
    for entry in file_in:
        program.append(entry[2])
        params_used.append(entry[9])
        image_used.append(entry[1])
        sim_settings.append(entry[3])
        missed_detections.append(entry[5])
        false_detections.append(entry[6])
        number_of_spots.append(entry[8])
        euc_dist.append(entry[7])
        index.append(entry[0])

################# Process results of grid search evaluation
####This chunk of code finds the detection with the lowest detection score (points missed +erroneous point created/ points in corresponding ground truth file) per images per parameter
####The output files with low detection scores are then evaluated on how close the detections are to their associated points. This is contained in the euc_dist 1d array. With this info we find per iamge/setting the best output file across Fishquant and Radial symmetry that have reallly low detection scores and low mean euclidena distance measure ments

Read in files convert to array/type
image_used = np.asarray(image_used)
sim_settings = np.asarray(sim_settings)
number_of_spots = np.asarray(number_of_spots)
false_detections = np.asarray(false_detections)
missed_detections = np.asarray(missed_detections)
params_used = np.asarray(params_used)
euc_dist = np.asarray(euc_dist)
index = np.asarray(index)

tick_spacing=1


euc_dist = euc_dist.astype('float')
index = index.astype('int')
number_of_spots = number_of_spots.astype('int')
false_detections = false_detections.astype('int')
missed_detections = missed_detections.astype('int')
program = np.asarray(program)
simulation_settings = (np.unique(sim_settings))
simulation_settings = np.sort(simulation_settings)


euc_sanity_check = []
whats_the_ind = []

color = ["g","b","r","y","m","c","deeppink"]


paramus = []
rs_val_plot = []
fq_val_plot = []

fq_x = []
rs_x = []

plt.figure(figsize=(15,15))

unq_images = np.unique(image_used)

x_tick_labels = []


#For each image setting subset and iterate over 1d arrays based on settings used to generate images
counter = 0
color_counter = 0
for setting in simulation_settings:
    print(setting)
    exp_filter = (np.asarray(sim_settings) == setting)
    params_usedsubset = params_used[exp_filter]
    img_subset = image_used[exp_filter]
    false_subset = false_detections[exp_filter]
    missed_subset = missed_detections[exp_filter]
    spots_subset = number_of_spots[exp_filter]
    program_subsets = program[exp_filter]
    params_usedsubset = params_used[exp_filter]
    index_subset = index[exp_filter]
    euc_dist_subset = euc_dist[exp_filter]
    
    unq_imgs = np.unique(img_subset)
# Again subset and iterate over each image contaed with in settings folde
    for img in unq_imgs:
        #print(img)
        matching_ims = (np.asarray((img_subset) == img))

        params_usedimg = params_usedsubset[matching_ims]
        img_subset_img = img_subset[matching_ims]
        false_subset_img = false_subset[matching_ims]
        missed_subset_img = missed_subset[matching_ims]
        spots_subset_img = spots_subset[matching_ims]
        program_subset_img = program_subsets[matching_ims]
        index_subset_img = index_subset[matching_ims]
        euc_dist_subset_img = euc_dist_subset[matching_ims]
        
 # Which Fishquant output file has the lowest deteection score       
        program_subset = (program_subset_img == "FQ")

        false_subset_f = false_subset_img[program_subset]
        missed_subset_f = missed_subset_img[program_subset]
        spots_subset_f = spots_subset_img[program_subset]
        params_used_f = params_usedimg[program_subset]
        index_f = index_subset_img[program_subset]
        euc_dist_f = euc_dist_subset_img[program_subset]
        if(len(spots_subset_f) > 0):
            lowest_val = (false_subset_f + missed_subset_f)/spots_subset_f
            val_plot_append_fq = np.min(lowest_val)
            lowest_vals = lowest_val == val_plot_append_fq

            #print(np.argmin(lowest_val))
            fq_x.append(counter)
            fq_val_plot.append(val_plot_append_fq)
            euc_lowest = euc_dist_f[lowest_vals]
            index_lowest =index_f[lowest_vals]
#Which of low detection fq output files per settings/image combo has lowest average euclidean distance
            print(euc_lowest[np.argmin(euc_lowest)],index_lowest[np.argmin(euc_lowest)])
            euc_sanity_check.append(euc_lowest[np.argmin(euc_lowest)])
            whats_the_ind.append(index_lowest[np.argmin(euc_lowest)])
        program_subset = (program_subset_img == "RS")

        false_subset_f = false_subset_img[program_subset]
        missed_subset_f = missed_subset_img[program_subset]
        spots_subset_f = spots_subset_img[program_subset]
        params_used_f = params_usedimg[program_subset]
        index_f = index_subset_img[program_subset]
        euc_dist_f = euc_dist_subset_img[program_subset]

        if(len(spots_subset_f) > 0):
#Which of low detection fq output files per settings/image combo has lowest average euclidean distance
            lowest_val = (false_subset_f + missed_subset_f)/spots_subset_f
            val_plot_append_rs = np.min(lowest_val)
            lowest_vals = lowest_val == val_plot_append_rs
	    euc_lowest = euc_dist_f[lowest_vals]
            index_lowest =index_f[lowest_vals]            
            tha_ind= index_lowest[np.argmin(euc_lowest)]            
            print(euc_lowest[np.argmin(euc_lowest)],index_lowest[np.argmin(euc_lowest)])
            rs_val_plot.append(val_plot_append_rs)
            rs_x.append(counter)
            paramus.append(params_used_f[np.argmin(lowest_val)] +"---"+ setting +"---"+ img)
            euc_sanity_check.append(euc_lowest[np.argmin(euc_lowest)])
            whats_the_ind.append(index_lowest[np.argmin(euc_lowest)])
        counter = counter + 1
        #print(counter)
        x_tick_labels.append(img)
    plt.axvline(x=counter - 0.25, label='The simulation params = {}'.format(setting),c = color[color_counter])
    #print(color_counter)
    color_counter = color_counter + 1

####Run the arrays that contain the best image/setting program pairs thru matplotlib to create a plot to visualize results
plt.xticks(np.arange(0, len((unq_images)), 1),x_tick_labels,rotation=90)

plt.scatter(rs_x, rs_val_plot, label = "radial symmetry", marker = "s")
plt.scatter(fq_x, fq_val_plot, label = "fish quant")

plt.xlabel("image_index")
plt.ylabel("(missed_detections+ false detections)/spot #")
plt.ylim(0.0,0.25)
plt.legend()
##############################
##############################
#Read in results of gridsearch quantification script...This text file has fields needed for further steps. Also read in from parse fishquant 
#In the NEAR future I will consolidate to reduce script length and file number

#Read in gridsearch results
stats_file = "/home/lpe/Desktop/gridsearchresults.txt"

program = []
image_used = []
sim_settings = []
missed_detections = []
false_detections = []
number_of_spots = []
params_used = []
index = []
euc_dist = []

with open(stats_file) as csvfile:
    next(csvfile)
    file_in = np.asarray(list(csv.reader(csvfile,delimiter="\t")))
    for entry in file_in:
        program.append(entry[0])
        params_used.append(entry[2])
        image_used.append(entry[1])
        sim_settings.append(entry[3])
        missed_detections.append(entry[4])
        false_detections.append(entry[5])
        euc_dist.append(entry[6])
#read in parsed fishquant results
fq_time = []
fq_run_strings = []
which_log = []
fq_times = "/home/lpe/Desktop/exe_times/fishquant_parsed.txt"
with open(fq_times) as csvfile:
    file_in = np.asarray(list(csv.reader(csvfile,delimiter=",")))
    for entry in file_in:
        fq_time.append(entry[0])
        fq_run_strings.append(entry[1])
        which_log.append(entry[2]) #
fq_time = np.asarray(fq_time)
fq_run_strings = np.asarray(fq_run_strings)
which_log = np.asarray(which_log)

######################## This code organizes the number of accurate detections made, average euclidean distance between detections
######################## For the best/most accurate detections across all images for both Fishquant and RS
######################## With this code you can construct csv/table of detections and their associated detection statistics
time_arr = []
name_arr = []
name_arr1 = []

program_arr = []
image_name_arr = []
settings_arr = []
detections_made = []
number_of_detections = []
made_over_missed = []
euc_arr =[]
no_points = []

rtime_arr = []
rname_arr = []
rname_arr1 = []

rprogram_arr = []
rimage_name_arr = []
rsettings_arr = []
rdetections_made = []
rnumber_of_detections = []
rmade_over_missed = []
reuc_arr =[]
rno_points = []
localization_arr = []
#######subset fishquant 1d arrays into smaller more acccurate 1d arrays with runtime
for ind in whats_the_ind:
    #find out if fishquant or not
    what_used = program[ind]
    if what_used == "FishQuant":
        fishquant_match_string = (sim_settings[ind] +"/FishQuant_results_"+ image_used[ind] + ".tif_" + params_used[ind] + ".csv")
        fqm = fishquant_match_string
        #print(fq_time[fishquant_match_string == fq_run_strings],fishquant_match_string,which_log[fishquant_match_string == fq_run_strings])
        if fq_time[fishquant_match_string == fq_run_strings].shape[0] >0:
            time_arr.append((fq_time[fishquant_match_string == fq_run_strings][0]))
            name_arr.append(fishquant_match_string)
            settings_arr.append(sim_settings[ind])
            program_arr.append(program[ind])
            image_name_arr.append(image_used[ind])
            detections_made.append(int(missed_detections[ind]))
            euc_arr.append(float(euc_dist[ind]))
            localization_arr.append(fqm.split("_")[-1].split("Localization")[0])
            no_points.append(int(fishquant_match_string.split("Poiss_")[1].split("spots")[0]))
            print(fishquant_match_string,(fq_time[fishquant_match_string == fq_run_strings][0]),which_log[fishquant_match_string == fq_run_strings],fq_time[fishquant_match_string == fq_run_strings][0])
        else:
            name_arr1.append(ind)
#######Subset rs 1d arrays into smaller more accurate 1d arrays with onetime also parse runtime from radial symmetry runtime logs
    else:
        rs_time = []
        rs_run_string =  []
        rs_fl= "sig" + params_used[ind].split("sig")[1].split("inRat")[0]
        rs_file_name = ("/home/lpe/Desktop/exe_times/ExeTimes/" + "exeTimeRSgridSearch_" +rs_fl + ".txt")
        #print(rs_file_name)
        with open(rs_file_name) as csvfile:
            file_in = np.asarray(list(csv.reader(csvfile,delimiter=",")))
            for count, entry in enumerate(file_in, start=1):
                if count % 2 != 0:
                    rs_time.append(entry[1])
                    rs_run_string.append(entry[0])
        rs_time = np.asarray(rs_time)
        rs_run_string = np.asarray(rs_run_string)

        sm= (image_used[ind])
        ss= (sim_settings[ind])
        sp = (program[ind])
        ps = params_used[ind]
        search_string = (directory + ss + "/" + sp + "_results_"  + sm + ".tif_" + ps + ".txt")

        rs_rt = rs_time[search_string == rs_run_string][0]
        rsettings_arr.append(sim_settings[ind])
        rprogram_arr.append(program[ind])
        rimage_name_arr.append(image_used[ind])
        rdetections_made.append(int(missed_detections[ind]))
        reuc_arr.append(float(euc_dist[ind]))
        rtime_arr.append(rs_rt)
        rno_points.append(int(search_string.split("Poiss_")[1].split("spots")[0]))
        print(rs_rt)
####### Same as above but parse the results from the fishquant excetion file (Due to quirks in the fq logs two seperate files were used to parse runtimes from fq grid search runs)
fq_time = []
fq_run_strings = []
which_log = []
fq_times = "/home/lpe/Desktop/exe_times/fishquant_parsed_except.txt"
with open(fq_times) as csvfile:
    file_in = np.asarray(list(csv.reader(csvfile,delimiter=",")))
    for entry in file_in:
        fq_time.append(entry[0])
        fq_run_strings.append(entry[1])
        which_log.append(entry[2])
fq_time = np.asarray(fq_time)
fq_run_strings = np.asarray(fq_run_strings)
which_log = np.asarray(which_log)


for indo in name_arr1:
    print(ind)
    #find out if fishquant or not
    #if fq_time[ind == fq_run_strings].shape[0] >0:
    print(indo)
    fishquant_match_string = (sim_settings[indo] +"/FishQuant_results_"+ image_used[indo] + ".tif_" + params_used[indo] + ".csv")
    fqm = fishquant_match_string
    time_arr.append((fq_time[fishquant_match_string == fq_run_strings][0]))
    settings_arr.append(sim_settings[indo])
    program_arr.append(program[indo])
    image_name_arr.append(image_used[indo])
    detections_made.append(int(missed_detections[indo]))
    euc_arr.append(float(euc_dist[indo]))
    localization_arr.append(fqm.split("_")[-1].split("Localization")[0])
    no_points.append(int(fishquant_match_string.split("Poiss_")[1].split("spots")[0]))
    print(fishquant_match_string,(fq_time[fishquant_match_string == fq_run_strings][0]),which_log[fishquant_match_string == fq_run_strings],fq_time[fishquant_match_string == f
#################### Format 1d arrays such that they can be used by pandas

df = pd.DataFrame()

a = image_name_arr
b = settings_arr

image_settings = np.asarray([m+str(n) for m,n in zip(b,a)])

a = rimage_name_arr
b = rsettings_arr

rimage_settings = np.asarray([m+str(n) for m,n in zip(b,a)])

rimage_settings[0] == image_settings
counter = 0
simple_list = []
for image in rimage_settings:
    image == image_settings
    ind_r = (image == image_settings)
    simple_list.append([str(image),
                        int(np.asarray(rtime_arr)[counter]),
                        int(np.asarray(time_arr)[ind_r][0]),
                        round(int(np.asarray(rdetections_made)[counter])/int(np.asarray(rno_points)[counter]),4),
                        round(int(np.asarray(detections_made)[ind_r])/int(np.asarray(rno_points)[counter]),4),
                        round(float(np.asarray(reuc_arr)[counter]),4),
                        round(float(np.asarray(euc_arr)[ind_r]),4),
                        int(np.asarray(rno_points)[counter]),
                        str(np.asarray(localization_arr)[ind_r][0])])
    
    counter = counter + 1
#Construct a pandas dataframe from array
df=pd.DataFrame(simple_list,columns=['img_used','rs_time','fq_time','rs_missed / #_points','fq_missed / #_points','euc_dist_rs','euc_dist_fq','# of points',"localization_status"])


################Format pandas dataframe bolding the best entries per each column across fishquant and radial symmetry, Export to excel spreadsheet
def select_col(x):
    c1 = 'font-weight: bold'
    c2 = '' 
    #compare columns
    mask = x['fq_time'] > x['rs_time']
    #DataFrame with same index and columns names as original filled empty strings
    df1 =  pd.DataFrame(c2, index=x.index, columns=x.columns)
    #modify values of df1 column by boolean mask
    df1.loc[mask, 'rs_time'] = c1
    return df1

def select_col2(x):
    c1 = 'font-weight: bold'
    c2 = '' 
    #compare columns
    mask = x['rs_missed / #_points'] < x['fq_missed / #_points']
    #DataFrame with same index and columns names as original filled empty strings
    df1 =  pd.DataFrame(c2, index=x.index, columns=x.columns)
    #modify values of df1 column by boolean mask
    df1.loc[mask, 'rs_missed / #_points'] = c1
    return df1

def select_col1(x):
    c1 = 'font-weight: bold'
    c2 = '' 
    #compare columns
    mask = x['euc_dist_fq'] > x['euc_dist_rs']
    #DataFrame with same index and columns names as original filled empty strings
    df1 =  pd.DataFrame(c2, index=x.index, columns=x.columns)
    #modify values of df1 column by boolean mask
    df1.loc[mask, 'euc_dist_rs'] = c1
    return df1

df.style\
    .apply(select_col, axis=None)\
    .apply(select_col1, axis=None)\
    .apply(select_col2, axis=None)\
    .to_excel('styled.xlsx', engine='openpyxl')

