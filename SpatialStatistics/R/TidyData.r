# Include Some Packages (includes tbl_df)
library(dplyr)

cleanData <- function(csvFile) {
  data = read.csv(csvFile)
  
  # This formats the dates nicely
  op <- options(digits.secs=3)
  data$time = strptime(data$time, format = "%Y-%m-%d %H:%M:%OS")
  data$time = as.POSIXct(data$time)
  data = tbl_df(data)
  
  # Only Include addresses in the study
  data$address = as.character(data$address)
  studyAddresses = c("C6:18:65:8E:6F:0F",
                     "F8:8C:82:6C:A1:76",
                     "D9:2C:5A:6D:AC:B3",
                     "EF:D2:F5:A6:74:84",
                     "FF:8D:D4:DF:73:9F",
                     "DC:09:88:62:AD:D4",
                     "EE:B8:FB:A3:E9:5A",
                     "D7:9A:DC:4D:04:68",
                     "CC:59:BE:86:FA:E6",
                     "EA:59:E1:89:06:3F",
                     "DC:06:74:F0:B2:77",
                     "FD:97:5D:25:CB:AC",
                     "FA:EE:15:9C:20:83",
                     "F5:C1:15:0F:BC:1B",
                     "C6:63:2D:1E:70:CA",
                     "E4:44:B8:FF:DD:34",
                     "D6:16:7D:C9:15:2C",
                     "F5:DE:2C:82:71:6F",
                     "C6:9A:05:52:F1:81")
  data = data %>%
    filter(address %in% studyAddresses)
  
  # Rename estimotes MAC addresses
  
  data$address[data$address == "C6:18:65:8E:6F:0F"] = "A1"
  data$address[data$address == "F8:8C:82:6C:A1:76"] = "A2"
  data$address[data$address == "D9:2C:5A:6D:AC:B3"] = "A3"
  data$address[data$address == "EF:D2:F5:A6:74:84"] = "A4"
  data$address[data$address == "FF:8D:D4:DF:73:9F"] = "A5"
  data$address[data$address == "DC:09:88:62:AD:D4"] = "A6"
  data$address[data$address == "EE:B8:FB:A3:E9:5A"] = "A7"
  data$address[data$address == "D7:9A:DC:4D:04:68"] = "A8"
  data$address[data$address == "CC:59:BE:86:FA:E6"] = "B1"
  data$address[data$address == "EA:59:E1:89:06:3F"] = "B2"
  data$address[data$address == "DC:06:74:F0:B2:77"] = "B3"
  data$address[data$address == "FD:97:5D:25:CB:AC"] = "B4"
  data$address[data$address == "FA:EE:15:9C:20:83"] = "B5"
  data$address[data$address == "F5:C1:15:0F:BC:1B"] = "B6"
  data$address[data$address == "C6:63:2D:1E:70:CA"] = "B7"
  data$address[data$address == "E4:44:B8:FF:DD:34"] = "B8"
  data$address[data$address == "D6:16:7D:C9:15:2C"] = "B9"
  data$address[data$address == "F5:DE:2C:82:71:6F"] = "B10"
  data$address[data$address == "C6:9A:05:52:F1:81"] = "B11"
    
  # stop x being a factor
  data$x = as.numeric(as.character(data$x))
  
  return(data)
}

cleanUnlabelledData <- function(csvFile) {
  data = read.csv(csvFile)
  
  # This formats the dates nicely
  op <- options(digits.secs=3)
  data$time = strptime(data$time, format = "%Y-%m-%d %H:%M:%OS")
  data$time = as.POSIXct(data$time)
  data = tbl_df(data)
  
  # Only Include addresses in the study
  data$address = as.character(data$address)
  studyAddresses = c("C6:18:65:8E:6F:0F",
                     "F8:8C:82:6C:A1:76",
                     "D9:2C:5A:6D:AC:B3",
                     "EF:D2:F5:A6:74:84",
                     "FF:8D:D4:DF:73:9F",
                     "DC:09:88:62:AD:D4",
                     "EE:B8:FB:A3:E9:5A",
                     "D7:9A:DC:4D:04:68",
                     "CC:59:BE:86:FA:E6",
                     "EA:59:E1:89:06:3F",
                     "DC:06:74:F0:B2:77",
                     "FD:97:5D:25:CB:AC",
                     "FA:EE:15:9C:20:83",
                     "F5:C1:15:0F:BC:1B",
                     "C6:63:2D:1E:70:CA",
                     "E4:44:B8:FF:DD:34",
                     "D6:16:7D:C9:15:2C",
                     "F5:DE:2C:82:71:6F",
                     "C6:9A:05:52:F1:81")
  data = data %>%
    filter(address %in% studyAddresses)
  
  # Rename estimotes MAC addresses
  
  data$address[data$address == "C6:18:65:8E:6F:0F"] = "A1"
  data$address[data$address == "F8:8C:82:6C:A1:76"] = "A2"
  data$address[data$address == "D9:2C:5A:6D:AC:B3"] = "A3"
  data$address[data$address == "EF:D2:F5:A6:74:84"] = "A4"
  data$address[data$address == "FF:8D:D4:DF:73:9F"] = "A5"
  data$address[data$address == "DC:09:88:62:AD:D4"] = "A6"
  data$address[data$address == "EE:B8:FB:A3:E9:5A"] = "A7"
  data$address[data$address == "D7:9A:DC:4D:04:68"] = "A8"
  data$address[data$address == "CC:59:BE:86:FA:E6"] = "B1"
  data$address[data$address == "EA:59:E1:89:06:3F"] = "B2"
  data$address[data$address == "DC:06:74:F0:B2:77"] = "B3"
  data$address[data$address == "FD:97:5D:25:CB:AC"] = "B4"
  data$address[data$address == "FA:EE:15:9C:20:83"] = "B5"
  data$address[data$address == "F5:C1:15:0F:BC:1B"] = "B6"
  data$address[data$address == "C6:63:2D:1E:70:CA"] = "B7"
  data$address[data$address == "E4:44:B8:FF:DD:34"] = "B8"
  data$address[data$address == "D6:16:7D:C9:15:2C"] = "B9"
  data$address[data$address == "F5:DE:2C:82:71:6F"] = "B10"
  data$address[data$address == "C6:9A:05:52:F1:81"] = "B11"
    
  return(data)
}

medianFilter <- function(window = 0.5, data) {
  minMaxTime = data %>%
    group_by(x, y) %>%
    summarise(minTime = min(time), maxTime = max(time))
  
  positions = nrow(data %>% group_by(x,y) %>% summarise())
  
  timeWindows = lapply(1:positions, function(i) seq(minMaxTime[i,3]$minTime, minMaxTime[i,4]$maxTime, by = window))
  timeWindows <- do.call("c", timeWindows)
  
  medianFiltered = data %>%
    group_by(timeWindow = cut(time, breaks = timeWindows), address, x, y) %>%
    summarise(medianRssi = median(as.numeric(rssi)), sdRssi = sd(as.numeric(rssi)), count = n()) %>%
    filter(count >= 2) %>%
    unique()
  
  medianFiltered$time = as.POSIXct(strptime(medianFiltered$time, format = "%Y-%m-%d %H:%M:%OS"))
  return(medianFiltered)
}

# Replace NA values with the average value for a given sensor and position
# We could extend this using regression, which would be much better
replaceNaAverage <- function(data) {
  averageRssi = data %>%
    group_by(x, y, address) %>%
    summarise(averageRssi = mean(median), count = n())
  
  noNAData = data %>%
    group_by(x, address, time) %>%
    summarise(median = sum(median)) %>%
    spread(address, median) %>%
    gather(address, median, 3:19) %>%
    inner_join(averageRssi, by = c("x", "address")) %>%
    mutate(newMedian = ifelse(is.na(median), averageRssi, median)) %>%
    dplyr::select(x, time, address, newMedian) %>%
    spread(address, newMedian)
  
  return(noNAData)
}

# Vague attempt at gaussian Filter (this does not work!)
# I obviously don't understand how to construct a gaussian filter yet
gaussianFilter <- function(window = 5, data) {
  minMaxTime = data %>%
    group_by(x, y) %>%
    summarise(minTime = min(time), maxTime = max(time))
  
  timeWindows = lapply(1:30, function(i) seq(minMaxTime[i,3]$minTime, minMaxTime[i,4]$maxTime, by = window))
  timeWindows <- do.call("c", timeWindows)
  
  meanSd = data %>%
    group_by(interval = cut(time, breaks = timeWindows), address, x, y) %>%
    summarise(mean = mean(rssi), sd = sd(rssi), count = n())
  
  gaussianFilter = data %>%
    group_by(interval = cut(time, breaks = timeWindows), address, x, y) %>%
    inner_join(meanSd, by = c("address", "interval", "x", "y")) %>%
    mutate(smoothed = dnorm(rssi, mean = mean, sd = sd)) %>%
    dplyr::select(x, y, time, smoothed, rssi) %>%
    unique()
  
  gaussianFilter$time = as.POSIXct(strptime(gaussianFilter$time, format = "%Y-%m-%d %H:%M:%OS"))
  return(gaussianFilter)
}

# Replace NA Values using EM