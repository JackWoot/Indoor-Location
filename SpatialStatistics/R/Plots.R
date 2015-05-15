setwd("/Users/jonny/Documents/Study/locationcode/SpatialStatistics/")
source("R/TidyData.r")
source("R/EmpiricalKriging.R")
library(ggplot2)
library(gridExtra)
train = cleanData("Data/combined_xy_nexus5.csv")

estimotes = train$address %>% unique()
radioMaps = data.frame()

for (estimote in estimotes) {
  radioMaps = rbind(radioMaps, generateRadioMapThreshold(estimote = estimote, data = train))
}
g = plotRadioMap(estimote = "A2", radioMapData = radioMaps)
ggsave(file = "Plots/RadioMap.pdf")

oneAddress = radioMaps %>% filter(address == "A2")

A2_Location = oneAddress %>% group_by(x,y) %>% summarise(A2x = 6.65, A2y = 5.7, RSSI = max(RSSI), variance = max(variance))

p = ggplot(oneAddress, aes(x = x, y = y, fill = RSSI, z = RSSI)) +
  geom_tile() + geom_contour() +
  theme_bw() + geom_point(data = A2_Location, aes(x = A2x, y = A2y), shape = 1, size = 5)
q = ggplot(oneAddress, aes(x = x, y = y, fill = variance)) +
  geom_tile() + theme_bw() + geom_point(data = A2_Location, aes(x = A2x, y = A2y), shape = 1, size = 5)
grid.arrange(p,q, nrow = 1)


source("R/ExploratoryDataAnalysis.R")
g1 = plotMedianFilter(time_window = 0.5, estimote = "A2", data = train)
ggsave("Plots/Signal.pdf", g1)

# Import the test data
test = cleanData("Data/test-xperia.csv")
predictions = mapEstimate(radioMaps = radioMaps, testData = test)
# X is the actual point, O is the predicted one
plotMapEstimate(probabilities = predictions)
mapAccuracy(predictions)
mapAverageAccuracy(predictions)
ggsave("Plots/MapEstimate.pdf")
