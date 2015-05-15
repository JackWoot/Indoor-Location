from trilat import trilaterate
from data_processing import *
from RadioProp import *
import numpy as np
import csv

def eliminate_points(results, n = 3, threshold = None):
    """ Returns points and associated distances based on either the top n rssi values or on point above a supplied threshold. Supplying a threshold will override the n top value option.

    Arguments:
        results - list of dictionaries - each dictionary contains a point, distance and rss key
        n - integer - number of beacons to select from the top of a list sorted by rss
        threshold - number - optional threshold value, if a beacons rss is below this value it will be excluded

    Returns:
        points - list of tuples indicating the beacon position
        distances - list of floats indicating the estimated distance from the beacon
    """
    if n and threshold:
        print "Warning both n and threshold supplied, using threshold only"

    points = list()
    distances = list()

    if n > len(results):
        n = len(results)
        print "supplied n is greater than number of supplied beacons - has been set to equal number of beacons"
    elif n < 3:
        n = 3
        print "n must be greater than or equal to 3 - has been reset to 3"

    if threshold:
        #If a threshold rss is supplied then sort through the restults and select those greater than or less than it
        for row in results:
            if row.get("rss") >= threshold:
                points.append(row.get("point"))
                distances.append(row.get("distance"))

    else:
        #If no threshold value is supplied sort the results by rss decending
        sorted_results = sorted(results, key=lambda k: k['rss'], reverse=True)

        #Add the first n values in the sorted results list to the return lists
        for i in range(n):
            points.append(sorted_results[i].get("point"))
            distances.append(sorted_results[i].get("distance"))

    return points, distances

def locate(windows, beacons, n = 3, threshold = None, summary = True):
    """ Given a set of rss readings (and associated meta data) this method will estimate a location for each reading using trilateration.

    Arguments:
        windows - list of time window dictionaries - Each contains a start time, ground truth (if available) and a dictionary of beacons with their associated measures
        n - integer - number of beacons to select from the top of a list sorted by rss
        threshold - number - optional threshold value, if a beacons rss is below this value it will be excluded
        summary - boolean - sets whether a summary of these point estimations should be printed to the console

    Returns:
        A list of dictionaries each of which contains an estimated x and y position, the ground truth x and y and the circular error for this estimate
    """
    results = list()
    circular_errors = list()

    #Calculate a distance estimate for each point
    for window in windows:

        output = list()

        for beacon in window["Beacons"].keys():

            #get the recieved and transmitted power for this beacon at this fingerprint location
            rss = float(window["Beacons"][beacon][0]["rssi"])
            tss = float(window["Beacons"][beacon][0]["power"])

            #Estimate the distance from this beacon given the rss and tss
            #distance = estimate_distance("FS1m", rss, tss)
            distance = None

            #If the estimate distance returns None then it is within 1m and we should just use the estimote distance estimate
            if not distance:
                distance = float(window["Beacons"][beacon][0]["distanceEstimate"])
                #print "Recieved power greater than transmitted - Assuming estimote distance: {}".format(distance)
            else:
                #print "Distance estimate: {} estimote estimate: {}".format(distance, rd["beacons"][beacon]["EMdistance"])
                pass

            #Get the center point for the beacon circle
            try:
                point = beacons[beacon]

                #Add the centerpoint, distance and rss value in a dictionry to the results list
                output.append({"point":point, "distance":distance, "rss":rss})

            except KeyError:
                #print "Beacon {} in readings but not in beacon file".format(beacon)
                pass

        if len(output) > 2:
            #From the returned set estimated distances from each beacon, eliminate points based on the supplied n and threshold values
            points, distances = eliminate_points(output, n, threshold)

            #Estimate the measurement location
            P = trilaterate(points, distances)

            #Get the grount truth coordinates - if available
            gtx = window.get("gtx",None)
            gty = window.get("gty",None)

            if gtx and gty:
                #Get the cricular error for this prediction
                ce = circular_error((gtx,gty),P)

                #Store the results for this reading
                results.append({"x":P[0], "y":P[1], "gtx":gtx, "gty":gty, "CE":ce})

                #Record the circular error for the console output
                circular_errors.append(ce)

                if summary:
                    print "{} Points Processed".format(len(results))
                    print "Average circular error: {}".format(np.mean(circular_errors))
                    print "Circular error range: {} - {}".format(min(circular_errors), max(circular_errors))
            else:
                print "No Ground Truth"
                #Store the results for this reading
                results.append({"x":P[0], "y":P[1]})

                if summary:
                    print "{} Points Processed".format(len(results))
        else:
            "Not enough beacons in window - skipping to next window"

    return results

def accuracy_summary(results, percentages, n = None, threshold = None):
    """ Produces a summary of the accuracy of these results.

    Arguments:
        results - list of dictionaries - the dictionaries should contain a circular error key "CE"
        percentages - list of numbers - Percentage values to use in the summary eg [50,75] would give the value that 50% and 75% of the estimations were within.
        n - integer - The number of beacons used for this results set (Default = None)
        threshold - number - The threshold setting for this result set (Default = None)

    Returns:
        A dictionary where the keys are Average, Min and Max and then each of the percentage values followed by a '%' symbol

    """
    #Get a list of circular errors
    circular_errors = list()
    for result in results:
        try:
            circular_errors.append(result["CE"])
        except KeyError:
            #For some reason the last window in the returned list from the filter methods stores None as its gtx and gty, util I fix this ignore any results without a circular error it
            pass

    #Add the basic statistics to the summary
    summary = dict()
    if n:
        summary["Number of Beacons Used"] = n

    if threshold:
        summary["Threshold Used"] = threshold

    summary["Min"] = min(circular_errors)
    summary["Max"] = max(circular_errors)
    summary["Average"] = np.mean(circular_errors)
    summary["SD"] = np.std(circular_errors)

    #Sort the circular errors into acesnding order
    circular_errors.sort()

    #Total number of items in the ce list
    n = float(len(circular_errors))

    #Add values for each percentage
    for pc in percentages:

        #Convert this percentage into a array index
        index = int((n-1.0) * (pc/100.0))

        #Add the corresponding ce value
        key = "{}%".format(pc)
        summary[key] = circular_errors[index]

    return summary

def save_dictionary(dictionary_list, filename):

    with open(filename, 'wb') as csvfile:
        #Set the header list
        fieldnames = dictionary_list[0].keys()

        #Create the writer object
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames, restval='NA')

        #Write the header containing the column names to the csv file
        writer.writeheader()

        #Write the results to the csv file
        writer.writerows(dictionary_list)

def get_location_accuracy(input_file, beacon_file, output_dir, window_filter):

    #Get the measured fingerprints at each calibration location
    readings = get_windows(input_file, timestep = 1, window_filter = window_filter, measure = "distanceEstimate")

    #Get the coordinates of each beacon
    beacons = get_beacon_locations_from_file(beacon_file)

    accuracy_list = list()
    for n in range(3,14):
        print "Running location with {} closest beacons".format(n)
        #Create a location estimation for each reading
        results = locate(readings, beacons, n , summary = False)
        save_dictionary(results, "{}output{}.csv".format(output_dir,n))
        #Add the accuracy summary for this result to the list
        accuracy_list.append(accuracy_summary(results, [80,90,95], n = n))

    #Save an accuracy summary of the results to file
    save_dictionary(accuracy_list, "{}accuracy.csv".format(output_dir))

if __name__ == "__main__":

    beacon_file = "/home/tom/Dropbox/Work/CDT Group Project/Data/EstimoteLocations.csv"

    #Test the location estimates
    #Nexus 5 data
    location_input = "/home/tom/Dropbox/Work/CDT Group Project/Data/Nexus 5 Recollection/Collection 2015-04-29/combined_xy.csv"

    #Xperia Data
    #location_input = "/home/tom/Dropbox/Work/CDT Group Project/Data/Xperia Collection/Combined/Combined_XY.csv"

    output_dir = "Data/"
    get_location_accuracy(location_input, beacon_file, output_dir, median_filter)

    #Test the path accuracy
    #input_file = "/home/tom/Dropbox/Work/CDT Group Project/Data/Movement/BT - Path 3.csv"

    #Get the measured fingerprints at each calibration location
    #readings = get_windows(input_file, timestep = 1, window_filter = median_filter, measure = "distanceEstimate")

    #Get the coordinates of each beacon
    #beacons = get_beacon_locations_from_file(beacon_file)

    #Get movement results
    #results = locate(readings, beacons, 5 , summary = False)
    #Save results
    #save_dictionary(results, "Data/Path/Input/output_path3.csv")
