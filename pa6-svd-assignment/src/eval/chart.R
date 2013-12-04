# Create a chart comparing the algorithms

library("ggplot2")
library("grid")

all.data <- read.csv("target/analysis/eval-results.csv")
changeMe = c('FeatureCount')
all.data[changeMe][is.na(all.data[changeMe])] <- 0

all.metric <- aggregate(cbind(RMSE.ByRating, RMSE.ByUser, nDCG, TopN.nDCG)
                        ~ Algorithm + FeatureCount + DataSet,
                        data=all.data, mean)

print(all.metric)

drawChart <- function(column) {
    chart <- qplot(column, Algorithm, color=FeatureCount, data=all.metric)
    chart <- chart + geom_point() + geom_text(aes(label=FeatureCount), hjust=-0.50, vjust=-0.50, size=1)
    return(chart)
}

addLabel <- function(chart) {
    chart <- chart + geom_point() + geom_text(aes(label=FeatureCount), hjust=-0.50, vjust=-0.50, size=1)
    return(chart)
}

fulldata.metric <- all.metric[all.metric$DataSet == "FullData", ]
print(fulldata.metric)
fulldata.chart.RMSE.ByRating <- qplot(RMSE.ByRating, Algorithm, color=FeatureCount, data=fulldata.metric)
fulldata.chart.RMSE.ByRating <- addLabel(fulldata.chart.RMSE.ByRating)
fulldata.chart.RMSE.ByUser <- qplot(RMSE.ByUser, Algorithm, color=FeatureCount, data=fulldata.metric)
fulldata.chart.RMSE.ByUser <- addLabel(fulldata.chart.RMSE.ByUser)
fulldata.chart.nDCG <- qplot(nDCG, Algorithm, color=FeatureCount, data=fulldata.metric)
fulldata.chart.nDCG <- addLabel(fulldata.chart.nDCG)
fulldata.chart.TopN.nDCG <- qplot(TopN.nDCG, Algorithm, color=FeatureCount, data=fulldata.metric)
fulldata.chart.TopN.nDCG <- addLabel(fulldata.chart.TopN.nDCG)
print("Outputting to target/analysis/accuracy-fulldata.pdf")
pdf("target/analysis/accuracy-fulldata.pdf", paper="letter", width=0, height=0)
error.layout <- grid.layout(nrow=4, heights=unit(0.25, "npc"))
pushViewport(viewport(layout=error.layout, layout.pos.col=1))
print(fulldata.chart.RMSE.ByUser, vp=viewport(layout.pos.row=1))
print(fulldata.chart.RMSE.ByRating, vp=viewport(layout.pos.row=2))
print(fulldata.chart.nDCG, vp=viewport(layout.pos.row=3))
print(fulldata.chart.TopN.nDCG, vp=viewport(layout.pos.row=4))
popViewport()
dev.off()

partialdata.metric <- all.metric[all.metric$DataSet == "PartialData", ]
print(partialdata.metric)
partialdata.chart.RMSE.ByRating <- qplot(RMSE.ByRating, Algorithm, color=FeatureCount, data=partialdata.metric)
partialdata.chart.RMSE.ByRating <- addLabel(partialdata.chart.RMSE.ByRating)
partialdata.chart.RMSE.ByUser <- qplot(RMSE.ByUser, Algorithm, color=FeatureCount, data=partialdata.metric)
partialdata.chart.RMSE.ByUser <- addLabel(partialdata.chart.RMSE.ByUser)
partialdata.chart.nDCG <- qplot(nDCG, Algorithm, color=FeatureCount, data=partialdata.metric)
partialdata.chart.nDCG <- addLabel(partialdata.chart.nDCG)
partialdata.chart.TopN.nDCG <- qplot(TopN.nDCG, Algorithm, color=FeatureCount, data=partialdata.metric)
partialdata.chart.TopN.nDCG <- addLabel(partialdata.chart.TopN.nDCG)
print("Outputting to target/analysis/accuracy-partialdata.pdf")
pdf("target/analysis/accuracy-partialdata.pdf", paper="letter", width=0, height=0)
error.layout <- grid.layout(nrow=4, heights=unit(0.25, "npc"))
pushViewport(viewport(layout=error.layout, layout.pos.col=1))
print(partialdata.chart.RMSE.ByUser, vp=viewport(layout.pos.row=1))
print(partialdata.chart.RMSE.ByRating, vp=viewport(layout.pos.row=2))
print(partialdata.chart.nDCG, vp=viewport(layout.pos.row=3))
print(partialdata.chart.TopN.nDCG, vp=viewport(layout.pos.row=4))
popViewport()
dev.off()
