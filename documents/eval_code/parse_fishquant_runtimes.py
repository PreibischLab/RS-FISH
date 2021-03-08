######With this code you may parse runtime logs Generate from fishquant

import glob

#These two functions parse a line of text into A: runtime with gaussian colocalization and B: runtime with out colocalization
def g_local_parser(input_name):
    lt = input_name.split("log")[1]
    lt = (int(lt.split("_")[0]))
    locmax = input_name.split("localmax")[1]
    locmax = (int(locmax.split("_")[0]))
    spots = input_name.split("findspots")[1]
    spots = (int(spots.split("_")[0]))
    findthr = input_name.split("findthr")[1]
    findthr = (int(findthr.split("_")[0]))
    gauss = input_name.split("gaussfit")[1]
    gauss = (int(gauss.split("_")[0]))
    #print(input,gauss,locmax,spots,lt)
    som = (gauss + locmax+ spots+lt+ findthr)
    return(som)
def no_g_local_parser(input_name):
    lt = input_name.split("log")[1]
    lt = (int(lt.split("_")[0]))
    locmax = input_name.split("localmax")[1]
    locmax = (int(locmax.split("_")[0]))
    spots = input_name.split("findspots")[1]
    spots = (int(spots.split("_")[0]))
    findthr = input_name.split("findthr")[1]
    findthr = (int(findthr.split("_")[0]))
    #print(input,gauss,locmax,spots,lt)
    som = (locmax+ spots+lt + findthr)
    return(som)

#Parse fishquant logs

logs_list = glob.glob("/home/lpe/Desktop/exe_times/sanity_check/Logs_FQ/*")


########Block of code reads in log files and depending on the type of colocalization used by fishquant parse a string into runtime. Parsed runtimes are written to a comma delimited? document with their name and log for further use as well as sanity checking
write_too = open("/home/lpe/Desktop/exe_times/fishquant_parsed.txt", "w+")
for log_name in logs_list:
    f = open(log_name,'r')
    sum_l = 0
    prevLine = ""
    while True:
        x = f.readline()
        if "Times:" in x:
            if "_gaussfit" in x:
                sum_l = g_local_parser(x)
                file_name = f.readline()
                file_name = (str(file_name)[76:-7])
                write_too.write("%i,%s,%s\n" % (sum_l,file_name,log_name.split("/")[-1][:-4]))
            if 'dense' in x:
                c_comp = int(x.split("comp")[1])
                cd_gains = sum_l + c_comp
                file_name = f.readline()
                file_name = (str(file_name)[76:-7])
                write_too.write("%i,%s,%s\n" % (cd_gains,file_name,log_name.split("/")[-1][:-4]))
            if "_gaussfit"  not in x and "spots" in x:
                sum_l = no_g_local_parser(x)
                file_name = prevLine
                file_namel= (str(file_name)[76:-7])
                write_too.write("%i,%s,%s\n" % (sum_l,file_namel,log_name.split("/")[-1][:-4]))
        prevLine = x

        if not x: break
############ Same as before but do to irregular output of runtime in logfiles code had to be created to catch an exception.
write_too.close()
write_too = open("/home/lpe/Desktop/exe_times/fishquant_parsed_except.txt", "w+")
for log_name in logs_list:
    print(log_name)
    f = open(log_name,'r')
    while True:
        file_namel = ''
        x = f.readline()
        if "sigmas=(" in x:
            while "Time" not in x:
                if "FishQuant_results" in x:
                    file_namel = x
                x = f.readline()
                if not x: break
            #print(x)
            if "Time" in x:
                sum_l = no_g_local_parser(x)
                file_namel= (str(file_namel)[76:-7])
                write_too.write("%i,%s,%s\n" % (sum_l,file_namel,log_name.split("/")[-1][:-4]))
        if not x: break
write_too.close()
############### In the end we generate two files that have all the fishquant runtime we will need moving forward
