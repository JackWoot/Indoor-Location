setwd("/Users/jack/Documents/University/BLE_Indoor_Location/locationcode/SpatialStatistics/")
source("TidyData.R")
library(gridExtra)
library(ggplot2)
library(tidyr)

data = cleanData("Data/finalOutput.csv")
medianFiltered = medianFilter(window = 0.5, data = data)

# Naïve Bayes
library(e1071)
library(caret)

# Return a confusion Matrix For each fold
# Using a matrix with average value for the NAs
naiveBayes1 <- function(data) {
  folds = createFolds(bayesDf$x, k = 10)
  conf.matrices = list()
  i = 1
  for (fold in folds) {
    training = data[-fold,]
    labels = data[fold,]$x
    test = data[fold,-1]
    
    classifier = naiveBayes(x ~ ., data = training)
    pred = predict(classifier, newdata = test)
    conf.matrix = table(pred, labels, dnn = list("predicted", "actual"))
    conf.matrices[[i]] = conf.matrix
    i = i + 1
  }
  return(conf.matrices)
}

# This one uses NA values and returns things
naiveBayes2 <- function() {
  data = medianFiltered %>% 
    group_by(x, address, time) %>%
    summarise(median = sum(median)) %>%
    spread(address, median) %>%
    dplyr::select(-time)
  
  data$x = as.factor(data$x)
  folds = createFolds(y = data$x, k = 10)
  conf.matrices = list()
  i = 1
  for (fold in folds) {
    training = data[-fold,]
    labels = data[fold,]$x
    test = data[fold,-1]
    
    classifier = naiveBayes(x ~ ., data = training)
    pred = sapply(1:length(fold), function(i) as.numeric(as.character(predict(classifier, newdata = test[i,]))))
    pred = sapply(1:length(fold), function(i) pred[[i]][1])
    
    conf.matrix = table(pred, labels, dnn = list("predicted", "actual"))
    conf.matrices[[i]] = conf.matrix
    i = i + 1
  }
  return(conf.matrices)
}

cf.mat = naiveBayes2()


TP = cf.mat[[1]][1,1]


# k-NN code

knn <- function() {
	data = medianFiltered %>% 
		group_by(x, y, address, time) %>%
		summarise(pos=paste("(", x, ",",y, ")", sep=""), median = sum(median)) %>%
		spread(address, median) %>%
		dplyr::select(-x, -y, -time)
	
	data$pos = as.factor(data$pos)
	
	#data$address = as.factor(data$address)
	
	data[is.na(data)] = 0
	
	folds = createFolds(y = data$pos, k = 10)
	conf.matrices = list()
	for (i in 1:length(folds)) {
		training = data[-folds[[i]],]
		labels = data[folds[[i]],]$pos
		test = data[folds[[i]],-1]
		
		classifier = knn3(pos ~ ., data = training, na.action=na.omit)
		pred = predict(classifier, newdata = test, type=c("class"))
		
		conf.matrix = table(pred, labels, dnn = list("predicted", "actual"))
		conf.matrices[[i]] = conf.matrix
	}
	return(conf.matrices)
}

knn.cf.mat = knn()

