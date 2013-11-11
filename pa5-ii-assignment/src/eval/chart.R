# Create a chart comparing the algorithms

library("ggplot2")
library("grid")

all.data <- read.csv("target/analysis/eval-results.csv")
changeMe = c('NNbrs')
all.data[changeMe][is.na(all.data[changeMe])] <- 0

all.metric <- aggregate(cbind(RMSE.ByRating, RMSE.ByUser, nDCG, TopN.nDCG)
                        ~ Algorithm + NNbrs,
                        data=all.data, mean)

print(all.metric)

drawChart <- function(column) {
    chart <- qplot(column, Algorithm, color=NNbrs, data=all.metric)
    chart <- chart + geom_point() + geom_text(aes(label=NNbrs), hjust=-0.50, vjust=-0.50, size=1)
    return(chart)
}

addLabel <- function(chart) {
    chart <- chart + geom_point() + geom_text(aes(label=NNbrs), hjust=-0.50, vjust=-0.50, size=1)
    return(chart)
}

chart.RMSE.ByRating <- qplot(RMSE.ByRating, Algorithm, color=NNbrs, data=all.metric)
chart.RMSE.ByRating <- addLabel(chart.RMSE.ByRating)
chart.RMSE.ByUser <- qplot(RMSE.ByUser, Algorithm, color=NNbrs, data=all.metric)
chart.RMSE.ByUser <- addLabel(chart.RMSE.ByUser)
chart.nDCG <- qplot(nDCG, Algorithm, color=NNbrs, data=all.metric)
chart.nDCG <- addLabel(chart.nDCG)
chart.TopN.nDCG <- qplot(TopN.nDCG, Algorithm, color=NNbrs, data=all.metric)
chart.TopN.nDCG <- addLabel(chart.TopN.nDCG)

chart.build <- ggplot(all.data, aes(Algorithm, BuildTime / 1000)) +
  geom_boxplot() +
  ylab("Build time (seconds)")

chart.test <- ggplot(all.data, aes(Algorithm, (TestTime / 1000))) +
  geom_boxplot() +
  coord_trans(y="log10") +
  ylab("Test time (seconds)")

print("Outputting to target/analysis/accuracy.pdf")
pdf("target/analysis/accuracy.pdf", paper="letter", width=0, height=0)
error.layout <- grid.layout(nrow=4, heights=unit(0.25, "npc"))
pushViewport(viewport(layout=error.layout, layout.pos.col=1))
print(chart.RMSE.ByUser, vp=viewport(layout.pos.row=1))
print(chart.RMSE.ByRating, vp=viewport(layout.pos.row=2))
print(chart.nDCG, vp=viewport(layout.pos.row=3))
print(chart.TopN.nDCG, vp=viewport(layout.pos.row=4))
popViewport()
dev.off()

print("Outputting to target/analysis/speed.pdf")
pdf("target/analysis/speed.pdf", paper="letter", width=0, height=0)
times.layout <- grid.layout(nrow=2, heights=unit(0.5, "npc"))
pushViewport(viewport(layout=times.layout, layout.pos.col=1))
print(chart.build, vp=viewport(layout.pos.row=1))
print(chart.test, vp=viewport(layout.pos.row=2))
popViewport()
dev.off()
