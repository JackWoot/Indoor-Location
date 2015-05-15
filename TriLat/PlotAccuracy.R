library(ggplot2)

#Read the accuracy file
trilatAcc = read.csv("Data/accuracy.csv")[-1,]

#Create the plot
ggplot(trilatAcc, aes(x = Number.of.Beacons.Used,
                      y = Average,
                      ymax = Average + SD,
                      ymin = Average - SD)) +
  geom_point() +
  theme_bw() +
  geom_errorbar() +
  xlab("Number of beacons") +
  ylab("Average Accuracy (m)") +
  scale_x_discrete(breaks = 1:13) +
  ggtitle("Average Accuracy by Number of Beacons")
