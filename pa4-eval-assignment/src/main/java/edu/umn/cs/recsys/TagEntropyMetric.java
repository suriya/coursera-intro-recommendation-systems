package edu.umn.cs.recsys;

import com.google.common.collect.ImmutableList;

import edu.umn.cs.recsys.dao.ItemTagDAO;

import org.grouplens.lenskit.core.LenskitRecommender;
import org.grouplens.lenskit.eval.algorithm.AlgorithmInstance;
import org.grouplens.lenskit.eval.data.traintest.TTDataSet;
import org.grouplens.lenskit.eval.metrics.AbstractTestUserMetric;
import org.grouplens.lenskit.eval.metrics.TestUserMetricAccumulator;
import org.grouplens.lenskit.eval.metrics.topn.ItemSelectors;
import org.grouplens.lenskit.eval.traintest.TestUser;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;
import org.grouplens.lenskit.vectors.VectorEntry.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A metric that measures the tag entropy of the recommended items.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class TagEntropyMetric extends AbstractTestUserMetric {
    private final int listSize;
    private final List<String> columns;

    private static final Logger logger = LoggerFactory.getLogger(TagEntropyMetric.class);
    /**
     * Construct a new tag entropy metric.
     *
     * @param nitems The number of items to request.
     */
    public TagEntropyMetric(int nitems) {
        listSize = nitems;
        // initialize column labels with list length
        columns = ImmutableList.of(String.format("TagEntropy@%d", nitems));
    }

    /**
     * Make a metric accumulator.  Metrics operate with <em>accumulators</em>, which are created
     * for each algorithm and data set.  The accumulator measures each user's output, and
     * accumulates the results into a global statistic for the whole evaluation.
     *
     * @param algorithm The algorithm being tested.
     * @param data The data set being tested with.
     * @return An accumulator for analyzing this algorithm and data set.
     */
    @Override
    public TestUserMetricAccumulator makeAccumulator(AlgorithmInstance algorithm, TTDataSet data) {
        return new TagEntropyAccumulator();
    }

    /**
     * Return the labels for the (global) columns returned by this metric.
     * @return The labels for the global columns.
     */
    @Override
    public List<String> getColumnLabels() {
        return columns;
    }

    /**
     * Return the lables for the per-user columns returned by this metric.
     */
    @Override
    public List<String> getUserColumnLabels() {
        // per-user and global have the same fields, they just differ in aggregation.
        return columns;
    }


    private class TagEntropyAccumulator implements TestUserMetricAccumulator {
        private double totalEntropy = 0;
        private int userCount = 0;

        private Set<Long> movieTagIds(TagVocabulary vocab, ItemTagDAO tagDAO, long movie) {
            Set<Long> tagIds = new HashSet<Long>();
            List<String> movieTags = tagDAO.getItemTags(movie);
            for (String tag : movieTags) {
                long tagid = vocab.getTagId(tag);
                tagIds.add(tagid);
            }
            return tagIds;
        }

        private Map<Long, Set<Long>> movieTagIdMap(TagVocabulary vocab, ItemTagDAO tagDAO, List<ScoredId> recommendations) {
            Map<Long, Set<Long>> moviemap = new HashMap<Long, Set<Long>>();
            for (ScoredId scoredId : recommendations) {
                long movie = scoredId.getId();
                Set<Long> tagIds = movieTagIds(vocab, tagDAO, movie);
                moviemap.put(movie, tagIds);
            }
            return moviemap;
        }

        /**
         * A vector of tagids just within the movies in the set.
         * @param recommendations
         * @return
         */
        private MutableSparseVector moviesetVocab(TagVocabulary vocab, ItemTagDAO tagDAO, Map<Long, Set<Long>> moviemap) {
            Set<Long> subsetVocab = new HashSet<Long>();
            for (long movie : moviemap.keySet()) {
                Set<Long> tagids = moviemap.get(movie);
                for (long tagid : tagids) {
                    subsetVocab.add(tagid);
                }
            }
            return MutableSparseVector.create(subsetVocab);
        }

        private MutableSparseVector tagProbabilities(TagVocabulary vocab, ItemTagDAO tagDAO, List<ScoredId> recommendations) {
            Map<Long, Set<Long>> moviemap = movieTagIdMap(vocab, tagDAO, recommendations);
            double movieCount = moviemap.size();
            assert (movieCount == recommendations.size());
            MutableSparseVector tagProbabilities = moviesetVocab(vocab, tagDAO, moviemap);
            logger.info("Tag vocabulary size: {}", tagProbabilities.keyDomain().size());
            for (VectorEntry entry : tagProbabilities.fast(State.EITHER)) {
                long tagid = entry.getKey();
                double probability = 0.0;
                for (long movie : moviemap.keySet()) {
                    Set<Long> movietags = moviemap.get(movie);
                    double numTags = movietags.size();
                    if (movietags.contains(tagid)) {
                        probability += (1.0 / movieCount) * (1.0 / numTags);
                    }
                }
                tagProbabilities.set(entry, probability);
            }
            return tagProbabilities;
        }


        private double entropy(TagVocabulary vocab, ItemTagDAO tagDAO, List<ScoredId> recommendations) {
            MutableSparseVector tagProbs = tagProbabilities(vocab, tagDAO, recommendations);
            MutableSparseVector logProbs = MutableSparseVector.create(tagProbs.keyDomain());
            for (VectorEntry entry : logProbs.fast(State.UNSET)) {
                long tagid = entry.getKey();
                double probability = tagProbs.get(tagid);
                double logprob = Math.log(probability) / Math.log(2);
                logProbs.set(entry, logprob);
            }
            return -1.0 * logProbs.dot(tagProbs);
        }

        /**
         * Evaluate a single test user's recommendations or predictions.
         * @param testUser The user's recommendation result.
         * @return The values for the per-user columns.
         */
        @Nonnull
        @Override
        public Object[] evaluate(TestUser testUser) {
            List<ScoredId> recommendations =
                    testUser.getRecommendations(listSize,
                                                ItemSelectors.allItems(),
                                                ItemSelectors.trainingItems());
            if (recommendations == null) {
                return new Object[1];
            }
            LenskitRecommender lkrec = (LenskitRecommender) testUser.getRecommender();
            ItemTagDAO tagDAO = lkrec.get(ItemTagDAO.class);
            TagVocabulary vocab = lkrec.get(TagVocabulary.class);

            logger.info("Got recommendations: {}", recommendations.size());
            double entropy = entropy(vocab, tagDAO, recommendations);
            logger.info("Entropy: {}", entropy);

            totalEntropy += entropy;
            userCount += 1;
            return new Object[]{entropy};
        }

        /**
         * Get the final aggregate results.  This is called after all users have been evaluated, and
         * returns the values for the columns in the global output.
         *
         * @return The final, aggregated columns.
         */
        @Nonnull
        @Override
        public Object[] finalResults() {
            // return a single field, the average entropy
            return new Object[]{totalEntropy / userCount};
        }
    }
}
