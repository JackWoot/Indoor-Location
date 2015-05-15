from PathAccuracy import path_accuracy_by_directory

waypoint_file = "/home/bob/Dropbox/Work/CDT Group Project/Data/Movement/Waypoints.csv"
key_file = "/home/bob/Dropbox/Work/CDT Group Project/Data/Movement/Path Key.csv"

directory_list = list()
directory_list.append("/home/bob/Dropbox/Work/CDT Group Project/Data/Movement/Trilat KF/KF_Output/OriginStart/")
directory_list.append("/home/bob/Dropbox/Work/CDT Group Project/Data/Movement/Trilat KF/KF_Output/TrilatStart/")
directory_list.append("/home/bob/Dropbox/Work/CDT Group Project/Data/Movement/Trilat KF/KF_Output/WaypointStart/")
directory_list.append("/home/bob/Dropbox/Work/CDT Group Project/Data/Movement/FKF_Output/")
directory_list.append("/home/bob/Dropbox/Work/CDT Group Project/Data/Movement/KNN_Paths/")
directory_list.append("/home/bob/Dropbox/Work/CDT Group Project/Data/Movement/Trilat KF/Input/")
directory_list.append("/home/bob/Dropbox/Work/CDT Group Project/Data/Movement/KNN KF/")

output_directory = "/home/bob/Dropbox/Work/CDT Group Project/Data/Movement/Path Accuracy/"

path_accuracy_by_directory(waypoint_file, key_file, directory_list, output_directory, plot = True)
