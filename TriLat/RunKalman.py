import subprocess
import csv

from os import listdir
from os.path import isfile, join

from PathAccuracy import get_waypoint_path_dictionary, get_path_errors, get_path_number

def get_initial_point(results_file):

    with open(results_file, 'rb') as csvfile:
        reader = csv.DictReader(csvfile)

        initial = reader.next()

    return (initial["x"], initial["y"])

def run_KF(input_dir, output_dir, measurement_variance, P0 = None, use_first_point_to_initialise = False, use_waypoint_to_initialise = False, waypoint_dictionary = None):

    #Get a list of input files
    files = [ f for f in listdir(input_dir) if isfile(join(input_dir,f)) ]

    for file in files:
        #Set the input filepath
        print "Processing file: {}".format(file)
        input_file ="{}{}".format(input_dir, file)

        #Create the outputfile name using the input filename
        output_file="{}{}{}".format(output_dir, file.split(".")[0], "_KF.csv")

        if use_first_point_to_initialise:
            #Get the first point in the estimated path to initialise the KF
            initial = get_initial_point(input_file)
            x0 = initial[0]
            y0 = initial[1]

            #Initialise the
            if not P0:
                P0 = 10.0

            #Run the KF and save the results
            subprocess.call(["python", "../SpatialStatistics/Python/KalmanRunner.py", input_file,  output_file, "{}".format(MVAR), "{}".format(x0), "{}".format(y0), "{}".format(P0)])
        elif use_waypoint_to_initialise:

            path = get_path_number(file)
            initial = waypoint_dictionary[path][0]
            x0 = initial[0]
            y0 = initial[1]

            #Initialise the
            if not P0:
                P0 = 10.0

            #Run the KF and save the results
            subprocess.call(["python", "../SpatialStatistics/Python/KalmanRunner.py", input_file,  output_file, "{}".format(MVAR), "{}".format(x0), "{}".format(y0), "{}".format(P0)])
        else:
            #Run the KF and save the results
            subprocess.call(["python", "../SpatialStatistics/Python/KalmanRunner.py", input_file,  output_file, "{}".format(MVAR)])

if __name__ == "__main__":

    #Set the measurement variance for the kalman filter
    MVAR=5.58

    #Set the path to the folder holding data files of the sequential position estimates
    DATAIN = "Data/Path/Input/"

    #Set the folder for the KF Filter Output
    DATAOUT = "Data/Path/KF_Output/"

    #Assume an origin start and a default estimations variance (P0 = 10)
    #run_KF(DATAIN, "{}{}".format(DATAOUT,"OriginStart/"), MVAR, use_first_point_to_initialise = False)

    #Use the first trilat estimation as the initial start position and a default estimations variance (P0 = 10)
    #run_KF(DATAIN, "{}{}".format(DATAOUT,"TrilatStart/"), MVAR, use_first_point_to_initialise = True)

    #Use the first ground truth waypoint position to initilaise the KF
    #waypoint_file = "/home/tom/Dropbox/Work/CDT Group Project/Data/Movement/Waypoints.csv"
    #key_file = "/home/tom/Dropbox/Work/CDT Group Project/Data/Movement/Path Key.csv"

    #wps = get_waypoint_path_dictionary(waypoint_file, key_file)
    #run_KF(DATAIN, "{}{}".format(DATAOUT,"WaypointStart/"), MVAR, use_first_point_to_initialise = False, use_waypoint_to_initialise = True, waypoint_dictionary = wps )

    #Run the KNN results through the CV KF
    run_KF("/home/bob/Dropbox/Work/CDT Group Project/Data/Movement/KNN_Paths/", "{}{}".format(DATAOUT,"KNN/"), MVAR, use_first_point_to_initialise = True)
