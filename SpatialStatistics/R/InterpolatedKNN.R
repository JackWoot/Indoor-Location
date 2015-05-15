setwd("/Users/jonny/Documents/Study/locationcode/SpatialStatistics")
source("R/EmpiricalKriging.R")
source("R/TidyData.r")

# k-NN code

get_distances <- function(test) {
  test = test %>% 
    group_by(address, timeWindow) %>%
		dplyr::select(address, timeWindow, medianRssi)
  
  estimotes = test$estimotes %>% unique()
  
	training = radioMaps %>%
    filter(address %in% (test$address %>% unique())) %>%
		group_by(x, y, address, RSSI) %>%
		summarise(pos=paste("(", x, ",", y, ")", sep=""))

  diffsquared = training %>%
    inner_join(test, by = "address") %>%
    mutate(diffsquared = (medianRssi - RSSI)**2)
  
  Distances = diffsquared %>% 
    group_by(timeWindow, pos, x, y) %>% 
    summarise(sumdiff = sum(diffsquared)) %>% 
    group_by(timeWindow, pos, x, y) %>%
    summarise(distance = sqrt(sumdiff))
  
  return(Distances)
}

knn = function(k, distances) {
   distances = distances %>%
    group_by(timeWindow) %>% 
    mutate(ranking = row_number(distance)) %>%
    filter(ranking <= k)
   
  distances = distances %>%
    group_by(timeWindow) %>%
    summarise(x = mean(x), y = mean(y))
  
  return(distances)
}

knn_accuracy = function(knn_output, test) {
  colnames(test) <- c("timeWindow", "address", "true_x", "true_y", "medianRssi", "sdRssi", "count", "time")
  
  accuracy = test %>%
    inner_join(knn_output, by = "timeWindow") %>%
    mutate(difference = sqrt((true_x-x)**2 + (true_y-y)**2)) %>%
    dplyr::select(timeWindow, difference)
  
  quantiles = quantile(accuracy$difference, probs = c(0.8,0.9,0.95))
  
  return(list("EightyPercent" = quantiles[1], "NinetyPercent" = quantiles[2], "NinetyFivePercent" = quantiles[3], "Mean Circular Error" = mean(accuracy$difference), "Max" = max(accuracy$difference), "SD" = sd(accuracy$difference)))
}

get_distances_nomap <- function(test, train) {
  test = test %>% 
    group_by(address, timeWindow) %>%
		dplyr::select(address, timeWindow, medianRssi)
  
  estimotes = test$address %>% unique()
  
	training = train %>%
    filter(address %in% estimotes) %>%
		group_by(x, y, address) %>%
		summarise(averageRssi = mean(rssi), sdRssi = sd(rssi)) %>%
	  mutate(pos=paste("(", x, ",", y, ")", sep=""))

  diffsquared = training %>%
    inner_join(test, by = "address") %>%
    mutate(diffsquared = (medianRssi - averageRssi)**2)
  
  Distances = diffsquared %>% 
    group_by(timeWindow, pos, x, y) %>% 
    summarise(sumdiff = sum(diffsquared)) %>% 
    group_by(timeWindow, pos, x, y) %>%
    summarise(distance = sqrt(sumdiff))
  
  return(Distances)
}

get_distances_nomap_mahal <- function(test, train) {
  test = test %>% 
    group_by(address, timeWindow) %>%
		dplyr::select(address, timeWindow, medianRssi)
  
  estimotes = test$address %>% unique()
  
	training = train %>%
    filter(address %in% estimotes) %>%
		group_by(x, y, address) %>%
		summarise(averageRssi = mean(rssi), sdRssi = sd(rssi)) %>%
	  mutate(pos=paste("(", x, ",", y, ")", sep=""))

  diffsquared = training %>%
    inner_join(test, by = "address") %>%
    mutate(diffsquared = (medianRssi - averageRssi)**2/(sdRssi**2))
  
  Distances = diffsquared %>% 
    group_by(timeWindow, pos, x, y) %>% 
    summarise(sumdiff = sum(diffsquared)) %>% 
    group_by(timeWindow, pos, x, y) %>%
    summarise(distance = sqrt(sumdiff))
  
  return(Distances)
}

get_distances_mahal <- function(test) {
  test = test %>% 
    group_by(address, timeWindow) %>%
		dplyr::select(address, timeWindow, medianRssi)
  
  estimotes = test$estimotes %>% unique()
  
	training = radioMaps %>%
    filter(address %in% (test$address %>% unique())) %>%
		group_by(x, y, address, RSSI) %>%
		summarise(pos=paste("(", x, ",", y, ")", sep=""))

  diffsquared = training %>%
    inner_join(test, by = "address") %>%
    mutate(diffsquared = (medianRssi - RSSI)**2)
  
  Distances = diffsquared %>% 
    group_by(timeWindow, pos, x, y) %>% 
    summarise(sumdiff = sum(diffsquared)) %>% 
    group_by(timeWindow, pos, x, y) %>%
    summarise(distance = sqrt(sumdiff))
  
  return(Distances)
}