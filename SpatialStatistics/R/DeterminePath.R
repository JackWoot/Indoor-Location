setwd("/Users/jonny/Documents/Study/locationcode/SpatialStatistics/")
source("R/TidyData.r")
source("R/EmpiricalKriging.R")
source("R/FingerprintKalmanFilter.R")
library(ggplot2)
library(gridExtra)

trackPath = function(radioMap = TRUE, path, x, P) {
  data = cleanUnlabelledData(path)
  data = data %>%
    group_by(timeWindow = cut(time, breaks = "1 sec"), address) %>%
    summarise(medianRssi = mean(rssi), sdRssi = sd(rssi))
  data = na.omit(data)
  
  # Path finding on unlabelled walk up the corridor
  if (radioMap) {
    radioMaps = generate_radio_maps(data = "Data/20150429_combined_xy.csv")
    output = kalman_runner(data = data, radioMaps = radioMaps, x = x, P = P)
  } else {
    training = cleanData("Data/20150429_combined_xy.csv")
    output = kalman_runner_nomap(data = data, training = training, x = x, P = P)
  }
  
  output = cbind(1:nrow(output), output)
  colnames(output) <- c("timestep", "filtered_x", "filtered_y")
  return(output)
}

paths = paste0(rep("Data/BT - Path ", 6), 1:6, rep(".csv", 6))
for (i in 1:length(paths)) {
  path1 = trackPath(radioMap = TRUE, path = paths[i], x = matrix(c(0,0), nrow = 2), P = diag(1, nrow = 2))
  write.csv(x = path1, file = paste0("Data/Path", i, "_FKF.csv"), row.names = FALSE)
}

source("R/InterpolatedKNN.R")

paths = paste0(rep("Data/BT - Path ", 6), 1:6, rep(".csv", 6))
for (i in 1:length(paths)) {
  path = cleanUnlabelledData(paths[i]) 
  path = path %>%
    group_by(timeWindow = cut(time, breaks = "1 sec"), address) %>%
    summarise(medianRssi = mean(rssi), sdRssi = sd(rssi))
  path1 = knn(5, get_distances_nomap(test = path, train = train))
  write.csv(x = path1, file = paste0("Data/Path", i, "_5NN.csv"), row.names = FALSE)
}
