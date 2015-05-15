# setwd("/Users/jonny/Documents/Study/locationcode/SpatialStatistics/")
# source("TidyData.r")
# data = cleanData("Data/finalOutput.csv")

# “A detective investigating a crime needs both tools and understanding. 
# If he has no fingerprint powder, he will fail to find fingerprints on most surfaces. 
# If he does not understand where the criminal is likely to have put his fingers, he will 
# not look in the right places. Equally, the analyst of data needs both tools and understanding” Tukey 1977

# Some exploratory Data analysis packages
library(ggplot2)
library(gridExtra)

# a plot showing a few sensors and how the data looks over time at distinct coordinates

# addresses = levels(data$address)[-1]
# 
# firstAddresses = data %>% 
#   filter(address %in% addresses[1:5]) %>% 
#   dplyr::select(x, y, time, rssi, address) %>%
#   group_by(address, x) %>%
#   mutate(count = n()) %>%
#   filter(count >= 10)
# 
# p = ggplot(firstAddresses, aes(x = time, y = rssi, colour = address)) + 
#   geom_line() + 
#   scale_x_datetime() + 
#   facet_wrap(~x, scales = "free")
# 
# # Showing how RSSI varies at each x coordinate, y = 0
# 
# rssiDistance = data %>% 
#   group_by(address, x) %>%
#   summarise(SdRssi = sd(rssi), AverageRssi = mean(rssi), NumberOfMeasurements = n()) %>%
#   filter(NumberOfMeasurements >= 10)
# 
# q = ggplot(rssiDistance, aes(x = x, y = AverageRssi)) + 
#   geom_point() +
#   facet_wrap(~address) +
#   geom_errorbar(aes(ymin = AverageRssi - SdRssi, ymax = AverageRssi + SdRssi))

# How to clean the signal
plotMedianFilter = function(time_window, estimote, x_pos = -3.34, y_pos = 7.25, data) {
   oneAddress = data %>% 
    filter(address == estimote) %>% 
    dplyr::select(x, y, time, rssi, address) %>%
    group_by(address, x, y) %>%
    mutate(count = n()) %>%
    filter(count >= 10 & x == x_pos & y == y_pos)
  
  p1 = ggplot(oneAddress, aes(x = time, y = rssi, group = 1)) +
    geom_line() +
    theme_bw() +
    scale_y_continuous(limits = c(-100,-70)) +
    ggtitle("Raw signal data")
  
  medianFiltered = medianFilter(window = time_window, data = data)
  
  # Lets make a plot
  p2 = ggplot(medianFiltered %>% filter(address == estimote & x == x_pos & y == y_pos), aes(x = time, y = medianRssi)) +
    geom_line() +
    theme_bw() +
    scale_y_continuous(limits = c(-100,-70)) 
  
  return(arrangeGrob(p1,p2, main = "Signal Data with median Filter with 0.5 second window"))
}

# Moving Average Filter

#averageFilter = data %>%
#  filter(address == "CC:59:BE:86:FA:E6" & y == 0) %>%
#  group_by(time = cut(time, breaks = timeWindows), address) %>%
#  mutate(median = median(as.numeric(rssi))) %>%
#  select(x,y,address, time, median) %>%
#  group_by(address, x) %>%
#  mutate(count = n()) %>%
#  filter(count >= 10 & x == 5) %>%
#  unique()

# Change the time back to time (from factor)
# averageFilter$time = as.POSIXct(strptime(averageFilter$time, format = "%Y-%m-%d %H:%M:%OS"))

# p3 = ggplot(averageFilter, aes(x = time, y = median, group = 1)) +
#   geom_line() +
#   theme_bw() +
#   scale_y_continuous(limits = c(-100,-70)) +
#   facet_wrap(~x, scales = "free_x") +
#   ggtitle("Signal Data with Moving Average Filter with 0.5 second window")

# Exploring RSSI measurements at points
# tmpdf = medianFiltered %>% 
#          group_by(x, y, address) %>% 
#          summarise(RSSI = mean(median), count = n()) %>% 
#          filter(address == "A2" & count > 10)
# 
# p1 = ggplot(tmpdf, aes(x = x, y = y, size = RSSI)) + geom_point()
# p2 = ggplot(tmpdf, aes(x = x, y = y, size = count)) + geom_point()