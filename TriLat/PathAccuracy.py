import math
import matplotlib.pyplot as plt
import numpy as np
import csv

from os import listdir
from os.path import isfile, join

from RadioProp import circular_error

def calculate_distance(start, end):
    """ Calculates the euclidean distance between the supplied start and end coordinates.

    Arguments:
        start - tuple (x,y) - coordinate of the start point
        end - tuple (x,y) - coordinate of the end point

    Returns:
        A float representing the distance between the supplied points in the units in which the coordinates are given
    """

    return math.sqrt(math.pow(start[0]-end[0],2) + math.pow(start[1]-end[1],2))

def get_total_path_length(waypoints):
    """ Gets the total path length for a given list of coordinate tuples.

    Arguments:
        waypoints - List of tuples - List containing (x,y) coordinates of each of the waypoints in the path

    Returns:
        A float of the total length of the path containing the supplied waypoints
    """
    total = 0.0

    for i, start in enumerate(waypoints):

        if i != len(waypoints)-1:
            end = waypoints[i+1]
            total = total + calculate_distance(start,end)

    return total

def get_intermediate_points(start, end, d, deficit = None):
    """ Given a start and end coordinate this method will divide the resulting line into segments of length d and return a list of points defining the start and end of each segment.

    eg, the line may look like this is d fits into the segment an integer number of times:

        |start|---d---|P1|---d---|P2|---d---|P3|---d---|end|

    or like this if not:

        |start|---d---|P1|---d---|P2|---d---|P3|---d-

    In both cases the method will return:

        [P1, P2, P3]

    Arguments:
        start - tuple (x,y) - coordinate of the start point
        end - tuple (x,y) - coordinate of the end point
        d - number - the length of each segment
        deficit - number - The length remaining from the last segment

    Returns:
        A list of coordinate tuples, containing the points length d along the line
    """
    d = float(d)

    #Calculate the total length of the line
    r = calculate_distance(start, end)

    #If the distance increment is longer than the supplied segment stop the function
    if deficit:
        deficit = float(deficit)
        if r < d-deficit:
            return None, r
    else:
        if r < d:
            return None, r

    #Calculate the number segments that will fit between the start and end coordinates, taking into account any remainder from the last segment if supplied
    if deficit:
        n = math.floor((r-(d-deficit))/d) + 1 #Plus 1 to account for the 1st point that is less than d away from the start
    else:
        n = math.floor(r/d)

    #Calculate the proportion of the line length that the distance d corresponds too
    rd = d/r

    #Calculate the x and y axis increment values
    a = math.fabs(end[0] - start[0]) * rd
    b = math.fabs(end[1] - start[1]) * rd

    points = list ()

    #Calculate the positive x and y booleans
    if end[0] > start[0]:
        posx = True
    else:
        posx = False

    if end[1] > start[1]:
        posy = True
    else:
        posy = False

    #Calcualte the first point
    if deficit:
        #Calcualte the proportion of the line length that the distance d corresponds too taking into account any remainder from the last segment
        rd1 = (d-deficit)/r

        #Calculate the x and y axis increment values for the first point including the deficit
        a1 = math.fabs(end[0] - start[0]) * rd1
        b1 = math.fabs(end[1] - start[1]) * rd1

        if posx:
            x = start[0] + a1
        else:
            x = start[0] - a1

        if posy:
            y = start[1] + b1
        else:
            y = start[1] - b1

        point1 = (x,y)
    else:
        if posx:
            x = start[0] + a
        else:
            x = start[0] - a

        if posy:
            y = start[1] + b
        else:
            y = start[1] - b

        point1 = (x,y)

    points.append(point1)

    #Add additional point based on the number that will fit into this segment
    for i in range(int(n)-1):
        prev_point = points[i]

        if posx:
            x = prev_point[0] + a
        else:
            x = prev_point[0] -  a

        if posy:
            y = prev_point[1] + b
        else:
            y = prev_point[1] - b

        point = (x, y)

        points.append(point)

    #Calcualte the remaining distance in this segment, not covered by the d length segments
    remainder = calculate_distance(start, end) - calculate_distance(start, points[len(points)-1])

    return points, remainder

def get_incremented_points_along_path(waypoints, d):
    """Given a list of waypoint tuples and a distance d, this method will return a new list of waypoints each d apart along the original waypoint path

    Arguments:
        waypoints - List of (x,y) tuples - The waypoint defining the base path
        d - number - The distance that the new points should be spaced along the original path

    Returns:
        A list of (x,y) tuples, containing the new path
    """
    d = float(d)
    new_waypoints = list()

    #Add the start point as the 1st new waypoint
    new_waypoints.append(waypoints[0])

    remainder = 0.0

    for i, start in enumerate(waypoints):
        #We do not want to use the last point as a start point so check we are not at the end before proceeding
        if i < len(waypoints)-1:
            #Set the end way point for this segment to be the next one along
            end = waypoints[i+1]

            #Get the new waypoints for this segment and any left over length
            points, remainder = get_intermediate_points(start, end, d, remainder)

            #print "Remainder: {}".format(remainder)

            #If there are points - ie if the length of the segment is greater than d - then add them to the new waypoint list
            if points:
                for point in points:
                    new_waypoints.append(point)

    #Add the original end point as the last new waypoint - if d fits into the path a integer number of times the last predicted point and the end point may be very close so run a check to avoid this
    if math.fabs(new_waypoints[-1][0] - waypoints[-1][0]) > 0.01 and math.fabs(new_waypoints[-1][1] - waypoints[-1][1]) > 0.01:
        new_waypoints.append(waypoints[-1])

    return new_waypoints

def plot_points(waypoints1, waypoints2):

    x1, y1 = zip(*waypoints1)
    plt.plot(x1, y1, 'xr-')

    x2, y2 = zip(*waypoints2)
    plt.plot(x2, y2, '.b-')

    plt.show()

def save_plot_points(waypoints1, waypoints2, output_path):

    x1, y1 = zip(*waypoints1)
    plt.plot(x1, y1, 'xr-')

    x2, y2 = zip(*waypoints2)
    plt.plot(x2, y2, '.b-')

    plt.savefig(output_path, format = "png")
    plt.close()

def get_path_errors(ground_truth_path, test_path):
    """ Given a ground truth path of waypoint tuples and a test path. This method will compare the accuracy of the test path against the ground truth by dividing the waypoint path into the same number of evenly spaced nodes as the test path and calculating the circular error between each pair of ground truth/test nodes.

    Arguments:
        ground_truth_path - List of (x,y) tuples - This is the known path
        test_path - List of (x,y) tuples - This is the estimated path to be compared against the ground truth path

    Returns:
        A list of circular error values the same length as the test path.
    """

    n1 = len(ground_truth_path)
    r1 = get_total_path_length(ground_truth_path)

    n2 = len(test_path)
    r2 = get_total_path_length(test_path)

    #calculate the segment distance needed for the ground truth path to have the same number of points as the test path
    d = float(r1/(n2-1))

    #Create the new ground truth waypoint list
    new_gt_path = get_incremented_points_along_path(ground_truth_path, d)

    #Debug output
    print "Number of waypoints in Ground Truth path: {}".format(n1)
    print "Ground Truth path length: {}".format(r1)
    print "Number of waypoints in test path: {}".format(n2)
    print "Test path length: {}".format(r2)
    print "Distance for new ground truth path segments: {}".format(d)
    print "Number of points in new Ground Truth path: {}".format(len(new_gt_path))

    #Calculate the circular error for each point in the test path
    errors = list()
    for i, gt in enumerate(new_gt_path):
        errors.append(circular_error(gt, test_path[i]))

    return errors

def get_waypoint_path_dictionary(waypoint_file, path_key):
    """ Gets a dictionary keyed to each path index which links to a list of (x,y) tuples that make up the path.

    Arguments:
        waypoint_file - String - Path to the csv file which defines the waypoint index and its x and y coordinates.
        path_key - String - Path to the csv file containing the key to each path which is a path index followed by a number of columns listing the waypoints that make up that path

    Returns:
        A dictionary keyed to each path index which links to a list of (x,y) tuples that make up the path.
    """
    #Get a doctionary containing each of the waypoint coordinates
    with open(waypoint_file, 'rb') as wpfile:
        reader = csv.DictReader(wpfile)

        wps = dict()

        for row in reader:
            wps[int(row["Waypoint"])] = (float(row["x"]),float(row["y"]))


    wp_paths = dict()

    #Get a dictionary conating each of the paths and the waypoints that it contains
    with open(path_key, 'rb') as keyfile:
        reader = csv.DictReader(keyfile)

        for row in reader:
            path = int(row["Path"])
            i = 0
            wplist = list()
            try:
                while True:
                    i = i + 1
                    key = "Waypoint {}".format(i)
                    if row[key]:
                        wplist.append(wps[int(row[key])])
            except KeyError:
                wp_paths[path] = wplist

    return wp_paths

def get_path_number(filename):

    numbers = [int(s) for s in filename if s.isdigit()]

    return numbers[0]

def get_path(path_file):

    points = list()

    with open(path_file, 'rb') as pathfile:

        reader = csv.DictReader(pathfile)

        for row in reader:
            points.append((float(row["x"]),float(row["y"])))

    return points

def path_accuracy_by_directory(waypoint_file, path_key, list_of_test_directories, output_directory, plot = False):

    wps = get_waypoint_path_dictionary(waypoint_file, path_key)

    results = dict()

    output_filepath = "{}{}".format(output_directory,"Path_Accuracy.csv")

    with open(output_filepath, 'wb') as resultfile:

        writer = csv.DictWriter(resultfile, fieldnames = ["Directory","Filename","Ground Truth Path Length","Test Path Length","Average Circular Error","SD of Circular Errors"])
        writer.writeheader()

        for directory in list_of_test_directories:

            print "\nProcessing directory: {}\n".format(directory)

            #Get a list of input files
            files = [ f for f in listdir(directory) if isfile(join(directory,f)) ]

            for file in files:

                print "\nProcessing file: {}\n".format(file)

                #Get the path number for this file
                path = get_path_number(file)

                #Get the waypoints for this path
                ground_truth_path = wps[path]

                #Get the test path from this file
                test_path = get_path("{}{}".format(directory,file))

                if plot:
                    #create plot filename
                    dirtag = directory.split("/")[-2]
                    fname = file.split(".")[0]
                    plotname = "{}{}{}{}{}".format(output_directory,dirtag,"_",fname,".png")
                    save_plot_points(ground_truth_path, test_path, plotname)

                #Get the list of errors for this test path
                errors = get_path_errors(ground_truth_path, test_path)

                writer.writerow({"Directory":directory,"Filename":file,"Ground Truth Path Length":get_total_path_length(ground_truth_path),"Test Path Length":get_total_path_length(test_path),"Average Circular Error":np.mean(errors),"SD of Circular Errors":np.std(errors)})

if __name__ == "__main__":

    #Test script for path accuracy

    waypoint_file = "/home/tom/Dropbox/Work/CDT Group Project/Data/Movement/Waypoints.csv"
    key_file = "/home/tom/Dropbox/Work/CDT Group Project/Data/Movement/Path Key.csv"

    wps = get_waypoint_path_dictionary(waypoint_file, key_file)

    test_path = get_path("/home/tom/Dropbox/Work/CDT Group Project/Data/Movement/Trilat KF/KF_Output/OriginStart/output_path3_KF.csv")

    path = 6

    for p in wps[path]:
        print p

    plot_points(wps[path], test_path)

    gtp = get_incremented_points_along_path(wps[path], 0.89875)

    for gt in gtp:
        print gt

    plot_points(gtp, test_path)
    #save_plot_points(gtp, test_path, "test.png")

    print get_path_errors(wps[path], test_path)
