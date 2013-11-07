import edu.umn.cs.recsys.QueryDAOProvider
import edu.umn.cs.recsys.dao.CSVItemTagDAO
import edu.umn.cs.recsys.dao.TagFile
import edu.umn.cs.recsys.dao.TitleFile
import edu.umn.cs.recsys.ii.SimpleItemItemScorer
import org.grouplens.lenskit.ItemScorer
import org.grouplens.lenskit.baseline.ItemMeanRatingItemScorer
import org.grouplens.lenskit.baseline.UserMeanBaseline
import org.grouplens.lenskit.baseline.UserMeanItemScorer
import org.grouplens.lenskit.data.dao.ItemDAO
import org.grouplens.lenskit.data.dao.UserEventDAO
import org.grouplens.lenskit.eval.data.CSVDataSource
import org.grouplens.lenskit.eval.data.crossfold.RandomOrder
import org.grouplens.lenskit.eval.data.subsample.SubsampleMode
import org.grouplens.lenskit.eval.data.traintest.GenericTTDataSet
import org.grouplens.lenskit.eval.metrics.predict.CoveragePredictMetric
import org.grouplens.lenskit.eval.metrics.predict.NDCGPredictMetric
import org.grouplens.lenskit.eval.metrics.predict.RMSEPredictMetric
import org.grouplens.lenskit.eval.metrics.topn.ItemSelectors
import org.grouplens.lenskit.knn.NeighborhoodSize
import org.grouplens.lenskit.knn.user.UserUserItemScorer
import org.grouplens.lenskit.transform.normalize.MeanCenteringVectorNormalizer
import org.grouplens.lenskit.transform.normalize.VectorNormalizer
import org.grouplens.lenskit.vectors.similarity.CosineVectorSimilarity
import org.grouplens.lenskit.vectors.similarity.VectorSimilarity

// common configuration to make tags available
// needed for both some algorithms and for metrics
// this defines a variable containing a Groovy closure, if you care about that kind of thing
tagConfig = {
    bind ItemDAO to CSVItemTagDAO
    set TagFile to new File("${project.config.dataDir}/movie-tags.csv")
    set TitleFile to new File("${project.config.dataDir}/movie-titles.csv")
}

fullData = crossfold("FullData") {
    source csvfile("MOOCRatings") {
        file "${project.config.dataDir}/ratings.csv"
        delimiter ","
        domain {
            minimum 0.5
            maximum 5.0
            precision 0.5
        }
    }
    test "target/crossfold-5/test.%d.csv"
    train "target/crossfold-5/train.%d.csv"

    // hold out 5 random items from each user
    order RandomOrder
    holdout 5

    // split users into 5 sets
    partitions 5
}

// create a version of each data partition with 1/2 the training ratings
partialData = fullData.collect { GenericTTDataSet ds ->
    def training = ds.trainingData as CSVDataSource
    def trainingFile = training.file
    def sampled = subsample {
        source training
        mode SubsampleMode.RATING
        fraction 0.5
        output new File(trainingFile.parentFile, "sampled-" + trainingFile.name)
    }
    ds.copyBuilder()
      .setTrain(sampled)
      .setName("PartialData")
      .setAttribute("DataSet", "PartialData")
      .build()
}

// create data sets with partial training but full query
assert partialData.size() == fullData.size()
partialTrainData = (0..(partialData.size() - 1)).collect { int i ->
    def full = fullData[i] as GenericTTDataSet
    def partial = partialData[i] as GenericTTDataSet
    partial.copyBuilder()
           .setName("PartialTrainData.${i}")
           .setAttribute("DataSet", "PartialTrainData")
           .setQuery(full.trainingData)
           .build()
}

// Run a train-test evaluation
trainTest {
    dataset fullData
    dataset partialData
    dataset partialTrainData

    // Three different types of output for analysis.
    output "${project.config.analysisDir}/eval-results.csv"
    userOutput "${project.config.analysisDir}/eval-user.csv"

    metric CoveragePredictMetric
    metric RMSEPredictMetric
    metric NDCGPredictMetric

    // Compute nDCG trying to recommend lists of 10 from all items
    // This suffers from similar problems as the unary ratings case!
    metric topNnDCG {
        candidates ItemSelectors.allItems()
        exclude ItemSelectors.trainingItems()
        listSize 10
    }

    algorithm("PersMean") {
        include tagConfig
        bind ItemScorer to UserMeanItemScorer
        bind (UserMeanBaseline, ItemScorer) to ItemMeanRatingItemScorer
    }
    for (nnbrs in [5, 10, 15, 20, 25, 30, 40, 50, 75, 100]) {
        algorithm("UserUser") {
             include tagConfig

             // Attributes let you specify additional properties of the algorithm.
             // They go in the output file, so you can do things like plot accuracy by neighborhood size
             attributes["NNbrs"] = nnbrs

             // use the user-user rating predictor
             bind ItemScorer to UserUserItemScorer

             set NeighborhoodSize to nnbrs

             bind VectorNormalizer to MeanCenteringVectorNormalizer
             bind VectorSimilarity to CosineVectorSimilarity
             at(ItemScorer) {
                 bind UserEventDAO toProvider QueryDAOProvider
             }
        }

        algorithm("CustomItemItem") {
            include tagConfig

            // Attributes let you specify additional properties of the algorithm.
            // They go in the output file, so you can do things like plot accuracy by neighborhood size
            attributes["NNbrs"] = nnbrs

            // use the item-item rating predictor
            bind ItemScorer to SimpleItemItemScorer

            set NeighborhoodSize to nnbrs

            at(ItemScorer) {
                bind UserEventDAO toProvider QueryDAOProvider
            }
        }
    }
}
