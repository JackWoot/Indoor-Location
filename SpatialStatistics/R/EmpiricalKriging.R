library(tidyr)
library(geoR)

generateRadioMap = function(x_from = 0, x_to = 10, y_from = 0, y_to = 8, time_window = 0.5, estimote, data) {
  # Chop into a square
  data = data %>% filter(x <= x_to & x >= x_from & y <= y_to & y >= y_from)

  x = seq(x_from, x_to, by = time_window)
  y = seq(y_from, y_to, by = time_window)
  # Set up a prediction grid with gridlines every 0.5 meters
  locs = as.matrix(expand.grid(seq(0, 10, by = 0.5), seq(0, 8, by = 0.5)))
  
  # Create geodata for a given estimote
  estimoteRssi = data %>% 
    group_by(x,y,address) %>%
    summarise(RSSI = round(mean(rssi), 0)) %>%
    filter(address == estimote) %>%
    as.geodata(coords.col = 1:2, data.col = 4)
  
  # MLE
  lh = likfit(estimoteRssi, ini.cov.pars = c(100,10), messages = FALSE)
  
  # Simple Kriging
  krig = krige.conv(geodata = estimoteRssi, loc = locs, 
                    krige = krige.control(type = "ok", obj.mod = lh), output = list(messages = FALSE))
  # Put it in a dataframe
  radioMap = data.frame(x = locs[,1], y = locs[,2],
                         variance = krig$krige.var,
                         RSSI = krig$predict, 
                         address = rep(estimote, length(krig$predict)))
  radioMap$address = as.character(radioMap$address)
  return(tbl_df(radioMap))
}

# Plot a radio map using the dataframe
plotRadioMap = function(estimote, radioMapData) {
  oneAddress = radioMapData %>% filter(address == estimote)

  p = ggplot(oneAddress, aes(x = x, y = y, fill = RSSI, z = RSSI)) +
    geom_tile() + geom_contour() +
    theme_bw()
  q = ggplot(oneAddress, aes(x = x, y = y, fill = variance)) +
    geom_tile() + theme_bw()
  return(arrangeGrob(p,q, nrow = 1, main = paste("Interpolated Signal Strength Map and Associated Variance For", estimote)))
}

# Once we have a map for each estimote similar to the below dataframe
# We can then calculate the Euclidean distance between a point we want to find and one in the 
# approximated radio map, if R is a set of N RSSI observations we want to categorise and M is a point
# in the radio Map then: D = srqt(sum((R-M)^2/N))

# We then weight the distance using a gaussian Kernel, L = exp(-D/2s^2), 
# where s is the standard deviation of R (does that mean the SD of all estimotes?)


mapEstimate = function(radioMaps, testData, minimumDataPoints) {
  # Calculate the bounding boxes
  x_from = min(radioMaps$x)
  x_to = max(radioMaps$x)
  y_from = min(radioMaps$y)
  y_to = max(radioMaps$y)
  
  # Chop the test data to the size of the radio map data
  testData = testData %>% filter(x <= x_to & x >= x_from & y <= y_to & y >= y_from)
  
  # Average the RSSI at each coordinate in the test dataframe
  testData = testData %>%
    group_by(x,y,address) %>%
    summarise(RSSI = mean(rssi), sd = sd(rssi), count = n(), averageDistance = mean(distanceEstimate)) %>%
    filter(count > 1)
  
  probabilities = testData %>%
    inner_join(radioMaps, by = "address") %>% 
    group_by(x.y, y.y, x.x, y.x) %>%
    summarise(distance = sqrt(sum(RSSI.x-RSSI.y)**2/n()), sd = mean(sd)) %>%
    mutate(Probability = exp(-distance/(2*sd**2))) %>%
    mutate(predicted_x = x.y, predicted_y = y.y, actual_x = x.x, actual_y = y.x)
  
  return(probabilities)
}

plotMapEstimate = function(probabilities) {
  # Small DF of predicted and actual points
  predictedPoint = probabilities %>%
    group_by(actual_x, actual_y) %>%
    summarise(predicted_x = predicted_x[which.max(Probability)], predicted_y = predicted_y[which.max(Probability)], Probability = max(Probability))
  
  # A plot showcasing the MAP estimates and the actual positions of the test data, so far not so good!
  p = ggplot(probabilities, aes(x = predicted_x, y = predicted_y, fill = Probability, z = Probability)) +
     geom_tile() +
     geom_contour() +
     facet_wrap(~actual_x+actual_y) +
     geom_point(data = predictedPoint, aes(x = predicted_x, y = predicted_y), shape = 1, size = 5) +
     geom_point(data = predictedPoint, aes(x = actual_y, y = actual_y), shape = 4, size = 5) +
     ggtitle("MLE Estimate and Actual Location overlayed on probability Heat Map")
  
  return(p)
}

# Compute Accuracy
mapAverageAccuracy = function(predictions) {
 predictions = predictions %>%
    group_by(actual_x, actual_y) %>%
    summarise(predicted_x = predicted_x[which.max(Probability)], predicted_y = predicted_y[which.max(Probability)], Probability = max(Probability)) 
 
 predictions = predictions %>% mutate(dist = sqrt((actual_x - predicted_x)^2 + (actual_y - predicted_y)^2))
 
 return(mean(predictions$dist))
}

# Compute Accuracy
sdAccuracy = function(predictions) {
 predictions = predictions %>%
    group_by(actual_x, actual_y) %>%
    summarise(predicted_x = predicted_x[which.max(Probability)], predicted_y = predicted_y[which.max(Probability)], Probability = max(Probability)) 
 
 predictions = predictions %>% mutate(dist = sqrt((actual_x - predicted_x)^2 + (actual_y - predicted_y)^2))
 
 return(sd(predictions$dist))
}

mapAccuracy = function(probs) {
  predictions = probs %>%
    group_by(actual_x, actual_y) %>%
    summarise(predicted_x = predicted_x[which.max(Probability)], predicted_y = predicted_y[which.max(Probability)], Probability = max(Probability)) 
 
  predictions = predictions %>% mutate(dist = sqrt((actual_x - predicted_x)^2 + (actual_y - predicted_y)^2))
  return(list("RMSE" = mean(predictions$dist), quantile(predictions$dist, probs = c(0.8,0.9,0.95)), "Max" = max(predictions$dist)))
}

mapPredictions = function(probs) {
  predictions = probs %>%
    group_by(actual_x, actual_y) %>%
    summarise(predicted_x = predicted_x[which.max(Probability)], predicted_y = predicted_y[which.max(Probability)], Probability = max(Probability)) 
  
  return(predictions)
}

selectEstimotes = function(numberOfEstimotes = 6, estimotes, testData = test, radioMaps = radioMap) {
  SelectedEstimotes = combn(estimotes, numberOfEstimotes)
  AccuracyDf = data.frame()
  for (j in 1:ncol(SelectedEstimotes)) {
    subsetRadioMap = radioMaps %>% filter(address %in% SelectedEstimotes[,j])
    probs = mapEstimate(radioMaps = subsetRadioMap, testData = testData)
    accuracy = mapAverageAccuracy(probs)
    predictions = probs %>%
      group_by(actual_x, actual_y) %>%
      summarise(predicted_x = predicted_x[which.max(Probability)],
                predicted_y = predicted_y[which.max(Probability)], 
                Probability = max(Probability)) 
 
    if (!is.nan(accuracy) & nrow(predictions) == 7) {
      AccuracyDf = rbind(AccuracyDf, data.frame(numberOfEstimotes = numberOfEstimotes,
                                                estimotes = paste(SelectedEstimotes[,j], collapse = ", "), 
                                                EightyPercent = mapAccuracy(predictions)[1],
                                                NinetyPercent = mapAccuracy(predictions)[2],
                                                NinetyFivePercent = mapAccuracy(predictions)[3],
                                                AverageAccuracy = mapAverageAccuracy(predictions),
                                                sdAccuracy = sdAccuracy(predictions)))
    } 
  }
  return(AccuracyDf)
}

generateRadioMapThreshold = function(x_from = 0, x_to = 10, y_from = 0, y_to = 8, threshold = -100, estimote, data, resolution = 0.5) {
    # Chop into a square
    data = data %>% filter(x <= x_to & x >= x_from & y <= y_to & y >= y_from)
  
    # Set up a prediction grid with gridlines every 0.5 meters
    locs = as.matrix(expand.grid(seq(0, 10, by = resolution), seq(0, 8, by = resolution)))
    
    # Create geodata for a given estimote
    estimoteRssi = data %>% 
      group_by(x, y, address) %>% 
      summarise(RSSI = mean(rssi)) %>%
      filter(address == estimote & RSSI > -100) %>%
      as.geodata(coords.col = 1:2, data.col = 4)
    
    # MLE
    lh = likfit(estimoteRssi, ini.cov.pars = c(100,10), messages = FALSE)
    
    # Nugget value is the variance at a given measurement point
    tausq = sd(data[data$address == estimote,]$rssi)**2
    
    # Simple Kriging
    krig = krige.conv(geodata = estimoteRssi, loc = locs, 
                      krige = krige.control(type = "ok", obj.mod = lh, nugget = tausq), output = list(messages = FALSE))
    # Put it in a dataframe
    radioMap = data.frame(x = locs[,1], y = locs[,2],
                           variance = krig$krige.var,
                           RSSI = krig$predict, 
                           address = rep(estimote, length(krig$predict)))
    radioMap$address = as.character(radioMap$address)
    return(tbl_df(radioMap)) 
}

generate_radio_maps = function(data = "Data/20150429_combined_xy.csv") {
  training = cleanData(data)
  estimotes = training$address %>% unique()
  radioMaps = data.frame()
  for (estimote in estimotes) {
    radioMaps = rbind(radioMaps, generateRadioMapThreshold(estimote = estimote, threshold = -100, data = training))
  }
  return(radioMaps)
}
