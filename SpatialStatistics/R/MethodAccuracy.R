setwd("/Users/jonny/Documents/Study/locationcode/SpatialStatistics")
source("R/TidyData.r")
source("R/EmpiricalKriging.R")
source("R/InterpolatedKNN.R")
library(ggplot2)
library(gridExtra)
train = cleanData("Data/20150429_combined_xy.csv")
 
# Import the test data
estimotes = train$address %>% unique()

# Build Radio Map
radioMaps = generate_radio_maps()

# TODO: Accuracy by number of Estimotes

# Accuracy map with all estimotes
probs = mapEstimate(radioMaps = radioMaps, testData = test)
# X is the actual point, O is the predicted one
plotMapEstimate(probabilities = probs)
ggsave("Plots/BestAccuracy.pdf")
mapAccuracy(probs)

# Accuracy of KNN
# Median Filter the Test Data
test = cleanData("Data/combined_test_xy.csv")
test = test %>%
		medianFilter(window = 60)
test = na.omit(test)

distances = get_distances(test)
knn_results = sapply(seq(1, 9, by = 2), function(i) knn_accuracy(knn(i, distances),test))
colnames(knn_results) <- seq(1, 9, by = 2)
t(knn_results)
write.csv(file = "Data/Knn_Results_map.csv", x = t(knn_results))

distances_nomap = get_distances_nomap(test = test, train = train)
knn_results_nomap = sapply(seq(1, 9, by = 2), function(i) knn_accuracy(knn(i, distances_nomap),test))
colnames(knn_results_nomap) <- seq(1, 9, by = 2)
t(knn_results_nomap)
write.csv(file = "Data/Knn_Results_nomap.csv", x = t(knn_results_nomap))

## Accuracy of KNN with Mahalanobis distance:

distances_mahal = get_distances_nomap_mahal(test = test, train = train)
knn_results_nomap = sapply(seq(1, 9, by = 2), function(i) knn_accuracy(knn(i, distances_mahal),test))
colnames(knn_results_nomap) <- seq(1, 9, by = 2)
t(knn_results_nomap)
