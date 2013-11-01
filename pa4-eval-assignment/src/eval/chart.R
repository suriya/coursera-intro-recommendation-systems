# Create a chart comparing the algorithms

library("ggplot2")
library("grid")

all.data <- read.csv("target/analysis/eval-results.csv")
changeMe = c('NNbrs')
all.data[changeMe][is.na(all.data[changeMe])] <- 0
# transform(all.data, NNbrs=as.character(NNbrs))
# print(all.data[changeMe])
# print (all.data)

all.ranking <- aggregate(cbind(TopN.nDCG, TagEntropy10)
                     ~ Algorithm + NNbrs,
                     data=all.data, mean)

all.prediction <- aggregate(cbind(RMSE.ByRating, RMSE.ByUser, nDCG)
                     ~ Algorithm + NNbrs,
                     data=all.data, mean)

#   rmse.both <- rbind(
#     data.frame(Algorithm=all.agg$Algorithm, RMSE=all.agg$RMSE.ByRating,
#                mode="Global"),
#     data.frame(Algorithm=all.agg$Algorithm, RMSE=all.agg$RMSE.ByUser,
#                mode="Per-User"))

# chart.rmse <- qplot(RMSE, Algorithm, data=rmse.both, shape=mode, color=mode)

print(all.ranking)
print(all.prediction)

chart.RMSE.ByRating <- qplot(RMSE.ByRating, Algorithm, color=NNbrs, data=all.prediction)
chart.RMSE.ByRating <- chart.RMSE.ByRating + geom_point() + geom_text(aes(label=NNbrs), hjust=-0.50, vjust=-0.50, size=1)
chart.RMSE.ByUser <- qplot(RMSE.ByUser, Algorithm, color=NNbrs, data=all.prediction)
chart.RMSE.ByUser <- chart.RMSE.ByUser + geom_point() + geom_text(aes(label=NNbrs), hjust=-0.50, vjust=-0.50, size=1)
chart.nDCG <- qplot(nDCG, Algorithm, color=NNbrs, data=all.prediction)
chart.nDCG <- chart.nDCG + geom_point() + geom_text(aes(label=NNbrs), hjust=-0.50, vjust=-0.50, size=1)
chart.TopN.nDCG <- qplot(TopN.nDCG, Algorithm, color=NNbrs, data=all.ranking)
chart.TopN.nDCG <- chart.TopN.nDCG + geom_point() + geom_text(aes(label=NNbrs), hjust=-0.50, vjust=-0.50, size=1)
chart.TagEntropy10 <- qplot(TagEntropy10, Algorithm, color=NNbrs, data=all.ranking)
chart.TagEntropy10 <- chart.TagEntropy10 + geom_point() + geom_text(aes(label=NNbrs), hjust=-0.50, vjust=-0.50, size=1)

chart.build <- ggplot(all.data, aes(Algorithm, BuildTime / 1000)) +
  geom_boxplot() +
  ylab("Build time (seconds)")

chart.test <- ggplot(all.data, aes(Algorithm, (TestTime / 1000))) +
  geom_boxplot() +
  coord_trans(y="log10") +
  ylab("Test time (seconds)")

print("Outputting to target/analysis/accuracy.pdf")
pdf("target/analysis/accuracy.pdf", paper="letter", width=0, height=0)
error.layout <- grid.layout(nrow=5, heights=unit(0.2, "npc"))
pushViewport(viewport(layout=error.layout, layout.pos.col=1))
print(chart.RMSE.ByUser, vp=viewport(layout.pos.row=1))
print(chart.RMSE.ByRating, vp=viewport(layout.pos.row=2))
print(chart.nDCG, vp=viewport(layout.pos.row=3))
print(chart.TopN.nDCG, vp=viewport(layout.pos.row=4))
print(chart.TagEntropy10, vp=viewport(layout.pos.row=5))
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
