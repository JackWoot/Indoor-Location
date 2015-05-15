import sqlite3
import csv
import datetime
from collections import Counter

def get_beacon_locations_from_file(beacon_file):
    """ Gets the beacon locations from the supplied database file.

    Arguments:
        beacon_file - String - Path to the csv file to be read

    Returns:
        A dictionary where each key is a beacon id and this links to a tuple (x,y) of its position
    """
    beacons = dict()

    with open(beacon_file, 'rb') as csvfile:

        reader = csv.DictReader(csvfile)

        for row in reader:
            beacons[row["MAC Address"]] = (float(row["x"]),float(row["y"]))

    return beacons

def get_raw_readings(readings_file):
    """ Takes a csv and returns a list of dictionaries keyed to the column names of the csv

    Arguments:
        readings_file - String - Path to the csv file to be read

    Returns:
        A list of dictionaries keyed to the column names of the csv
    """

    readings = list()

    with open(readings_file, 'rb') as csvfile:

        reader = csv.DictReader(csvfile)

        for row in reader:
            readings.append(row)

    return readings

def sort_readings_by_time(readings, timestep):
    """ Sorts a list of raw reading dictionaries into a list of time step windows, each of which contains a dictionary of start time, time step and a dictionary of beacon ids linked to a list of rssi readings.

    Arguments:
        readings - List of dictionaries - Containing the raw signal readings
        timestep - Integer - Length of each time window in seconds

    Returns:
        A list of dictionaries. Each dictionary is a time window with the following keys:

            Start - Start time of the window
            Timestep - The length of the window in seconds
            gtx and gty - If ground truth information is available in the raw readings these coordinates are included
            Beacons - This links to a dictionary which is keyed with the Unique IDs of each beacons recorded during the time window. Each of these Beacon ID keys link to a list of raw reading dictionaries that are from this beacon within the this time window.
    """
    step = datetime.timedelta(seconds=timestep)

    time_window_start = datetime.datetime.strptime(readings[0].get("time").replace("T"," ").replace("Z", ""), "%Y-%m-%d %H:%M:%S.%f")
    time_window_end = time_window_start + step

    #Initialise the first time window
    window = dict()
    window["Start"] = readings[0].get("time").replace("T"," ").replace("Z", "")
    window["Timestep"] = timestep
    window["Beacons"] = dict()

    #Initialise the list for storing all the time windows
    sorted_readings = list()
    sorted_readings.append(window)

    #Initialise the list for storing ground truth values - if available
    gt_list = list()

    n = len(readings)

    for i, reading in enumerate(readings):

        #print "Processing reading {} of {}".format(i,n)

        current_timestamp = datetime.datetime.strptime(reading.get("time").replace("T"," ").replace("Z", ""), "%Y-%m-%d %H:%M:%S.%f")

        #Get the ground truth coordinates for this reading if they exist
        x = reading.get("x", None)
        y = reading.get("y", None)

        if x and y:
            gt_list.append("{},{}".format(x,y))

        #Check if this entry is beyond the current time window, if so then reset the window dictionary and move the time window forward
        if current_timestamp > time_window_end:

            if x and y:
                remove_non_gt_readings(window, gt_list)
                gt_list = list()

            window = dict()
            window["Start"] = time_window_end.strftime("%Y-%m-%d %H:%M:%S.%f")
            window["Timestep"] = timestep
            window["Beacons"] = dict()
            time_window_end = time_window_end + step
            sorted_readings.append(window)

        #Add this reading to the appropriate beacon list in the window dictionary
        try:
            window["Beacons"][reading.get("address")].append(reading)
        except KeyError:
            window["Beacons"][reading.get("address")] = [reading]

    return remove_empty_windows(sorted_readings)

def remove_empty_windows(sorted_readings):
    """ Removes empty time windows from a list of time window dictionaries.

    Arguments:
        sorted_readings - List of time window dictionaries

    Returns:
        A new list of time window dictionaries without the empty time windows
    """
    filtered_sorted_readings = list()

    for window in sorted_readings:
        if len(window.get("Beacons").keys()) > 1:
            filtered_sorted_readings.append(window)

    return filtered_sorted_readings

def remove_non_gt_readings(window, gt_list):
    """ If the input data for the time windowing method is from stationary fingerprinting with ground truth information there may be a situation where a time window included data from 2 seperate locations. This method will calculate which ground truth position this window occupied the most and remove any data not from that position from the window.

    Arguments:
        window - Time window dictionary
        gt_list - List - Containing a sequence of ground truth "x,y" strings from every reading in the time window.

    Returns:
        The same window object that was supplied but with readings from the least prevalent ground truth position removed.
    """

    def remove_reading(reading, x , y):
        return float(reading.get("x")) != x and float(reading.get("y")) != y

    #Get the most common ground truth string from the ground truth list
    mode_gt = Counter(gt_list).most_common(1)[0][0]
    x = float(mode_gt.split(",")[0])
    y = float(mode_gt.split(",")[1])

    for beacon in window["Beacons"].keys():
        readings = window["Beacons"][beacon]
        #Cycle through the readings for this beacon and note the index of any reading which does not match the mode ground truth coordinates
        readings[:] = [reading for reading in readings if not remove_reading(reading, x, y)]

    #Add the mode ground truth coordinates to the window dictionary
    window["gtx"] = x
    window["gty"] = y

    #Return the original window with the edited lists
    return window

def median_filter(windows, measure = "rssi"):
    """ Takes a list of time windows (which each contain a dictionary of beacon ids linked to sequential readings of rssi) and filters the readings for each beacon using a median filter.
    """
    new_windows = list()

    for window in windows:
        #Create new entry for this time window
        new_window = dict()
        new_window["Start"] = window["Start"]
        new_window["Timestep"] = window["Timestep"]
        new_window["Beacons"] = dict()

        x = window.get("gtx", None)
        y = window.get("gty", None)

        if x and y:
            new_window["gtx"] = x
            new_window["gty"] = y

        for beacon_id in window["Beacons"].keys():
            readings = window["Beacons"][beacon_id]

            #Get a list of the measures you want to filter
            measures = list()
            for reading in readings:
                measures.append(reading[measure])

            #Choose the median value
            measures.sort()
            mid_value = measures[int(len(measures)/2)]

            #Find the entry in the readings list that matches the median value and add that to the new windows beaon dictionary
            for reading in readings:
                if reading[measure] == mid_value:
                    new_window["Beacons"][beacon_id] = [reading]

        #Add the new filtered window to the new_windows list
        new_windows.append(new_window)

    return new_windows

def average_filter(windows, measure = None):
    """ Takes a list time windows (which each contain a dictionary of beacon ids linked to sequential readings of rssi) and returns the average  for each beacon at each position. A optional filter function can be applied.
    """
    new_windows = list()

    for window in windows:
        #Create new entry for this time window
        new_window = dict()
        new_window["Start"] = window["Start"]
        new_window["Timestep"] = window["Timestep"]
        new_window["Beacons"] = dict()

        x = window.get("gtx", None)
        y = window.get("gty", None)

        if x and y:
            new_window["gtx"] = x
            new_window["gty"] = y

        for beacon_id in window["Beacons"].keys():
            readings = window["Beacons"][beacon_id]

            #Get a list of each of the measures you want to average
            rssi_list = list()
            power_list = list()
            distance_list = list()

            for reading in readings:
                rssi_list.append(float(reading["rssi"]))
                power_list.append(float(reading["power"]))
                distance_list.append(float(reading["distanceEstimate"]))

            new_window["Beacons"][beacon_id] = [{"power":sum(power_list)/float(len(power_list)), "rssi": sum(rssi_list)/float(len(rssi_list)),"distanceEstimate":sum(distance_list)/float(len(distance_list))}]

        #Add the new filtered window to the new_windows list
        new_windows.append(new_window)

    return new_windows

def get_windows(readings_file, timestep = 1, window_filter = average_filter, measure = "rssi"):

    raw_readings = get_raw_readings(readings_file)

    windows = sort_readings_by_time(raw_readings, timestep)

    filtered_windows = window_filter(windows, measure = "rssi")

    return filtered_windows

if __name__ == "__main__":

    timestep = 10

    readings = get_raw_readings("/home/tom/Dropbox/Work/CDT Group Project/Data/Nexus 5 Recollection/Room 2/Room2_xy.csv")

    sorted_readings = sort_readings_by_time(readings, timestep)

#    for i, s in enumerate(sorted_readings):
#        print "Window {}:".format(i)
#        print "Start Time: {}".format(s.get("Start"))
#        print "Ground Truth: {},{}".format(s.get("gtx"),s.get("gty"))
#        print "Number of beacons: {}".format(len(s["Beacons"].keys()))
#        total = 0
#        for beacon in s.get("Beacons").keys():
#            total = total + len(s["Beacons"].get(beacon))
#            #print "\tBeacon: " + beacon
#            #print "\t\tReadings in {} sec timestep: {}".format(timestep, len(s.get(beacon)))
#        print "Number of readings: {}".format(total)
#
#    print "Total number of {} second windows: {}".format(timestep, len(sorted_readings))

    filtered_readings = median_filter(sorted_readings)
    #filtered_readings = average_filter(sorted_readings)

    for i, s in enumerate(filtered_readings):
        print "Window {}:".format(i)
        print "Start Time: {}".format(s.get("Start"))
        print "Ground Truth: {},{}".format(s.get("gtx"),s.get("gty"))
        print "Number of beacons: {}".format(len(s["Beacons"].keys()))
        total = 0
        for beacon in s.get("Beacons").keys():
            total = total + len(s["Beacons"].get(beacon))
        print "Number of readings: {}".format(total)

    print "Total number of {} second windows: {}".format(timestep, len(filtered_readings))
