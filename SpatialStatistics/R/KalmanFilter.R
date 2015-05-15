library(dlm)
setwd("/Users/jonny/Documents/Study/locationcode/SpatialStatistics/")
source("TidyData.r")
nexus5 = cleanData("Data/combined_xy_nexus5.csv")
nexus5 = medianFilter(data = nexus5, window = 0.5)

# Create a matrix of outputs
y = nexus5 %>%
  select(timeWindow, address, medianRssi) %>%
  spread(address, medianRssi) %>%
  select(-x, -timeWindow) %>%
  as.matrix()

# Initialise a DLM
GG = matrix(c(1,0,1,0,0,1,0,1,0,0,1,0,0,0,0,1), byrow = T, ncol = 4)
FF = matrix(c(1,0,0,0,0,1,0,0), byrow = T, ncol = 4)

dlm(list(m0 = matrix(c(0,0)), C0 = matrix(c(1,1)), FF = FF, V = 1, GG = GG, W = 1))

# Do a kalman filter
dlmFilter(y, )