package edu.umn.cs.recsys.svd;

import org.apache.commons.math3.linear.RealMatrix;
import org.grouplens.lenskit.ItemScorer;
import org.grouplens.lenskit.baseline.BaselineScorer;
import org.grouplens.lenskit.basic.AbstractItemScorer;
import org.grouplens.lenskit.data.dao.UserEventDAO;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.history.History;
import org.grouplens.lenskit.data.history.RatingVectorUserHistorySummarizer;
import org.grouplens.lenskit.data.history.UserHistory;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * SVD-based item scorer.
 */
public class SVDItemScorer extends AbstractItemScorer {
    private static final Logger logger = LoggerFactory.getLogger(SVDItemScorer.class);
    private final SVDModel model;
    private final ItemScorer baselineScorer;
    private final UserEventDAO userEvents;

    /**
     * Construct an SVD item scorer using a model.
     * @param m The model to use when generating scores.
     * @param uedao A DAO to get user rating profiles.
     * @param baseline The baseline scorer (providing means).
     */
    @Inject
    public SVDItemScorer(SVDModel m, UserEventDAO uedao,
                         @BaselineScorer ItemScorer baseline) {
        model = m;
        baselineScorer = baseline;
        userEvents = uedao;
    }

    /**
     * Score items in a vector. The key domain of the provided vector is the
     * items to score, and the score method sets the values for each item to
     * its score (or unsets it, if no score can be provided). The previous
     * values are discarded.
     *
     * @param user   The user ID.
     * @param scores The score vector.
     */
    @Override
    public void score(long user, @Nonnull MutableSparseVector scores) {
        // TODO Score the items in the key domain of scores

        for (VectorEntry e: scores.fast(VectorEntry.State.EITHER)) {
            long item = e.getKey();
            // TODO Set the scores
        }
    }

    /**
     * Get a user's ratings.
     * @param user The user ID.
     * @return The ratings to retrieve.
     */
    private SparseVector getUserRatingVector(long user) {
        UserHistory<Rating> history = userEvents.getEventsForUser(user, Rating.class);
        if (history == null) {
            history = History.forUser(user);
        }

        return RatingVectorUserHistorySummarizer.makeRatingVector(history);
    }
}
