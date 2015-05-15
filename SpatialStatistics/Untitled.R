setwd('~/Documents/University/Indoor-Location/locationcode/SpatialStatistics/')

library(ggplot2)
source("R/TidyData.R")
data = cleanData("Combined_XY.csv")

data = data %>% group_by(x,y,address) %>% summarise(count = n()) %>% filter()

estimote = "A2" # Enter an estimote name here

ggplot(data %>% filter(address == estimote & x > 0 & x < 10), aes(x = x, y = y, label = count)) +
	geom_text() +
	ggtitle("Number of advertisement packets received from beacon 'A2'\n after scanning for a minute at a range of locations.")

ggplot(data %>% filter(x > 0 & x < 10), aes(x = x, y = y, label = count)) +
	geom_text() +
	facet_wrap(~address) +
	ggtitle("Don't forget to put a good title")