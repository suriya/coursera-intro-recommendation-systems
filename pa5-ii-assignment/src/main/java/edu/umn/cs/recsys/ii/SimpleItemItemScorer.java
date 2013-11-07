package edu.umn.cs.recsys.ii;

import org.grouplens.lenskit.basic.AbstractItemScorer;
import org.grouplens.lenskit.data.dao.UserEventDAO;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.history.History;
import org.grouplens.lenskit.data.history.RatingVectorUserHistorySummarizer;
import org.grouplens.lenskit.data.history.UserHistory;
import org.grouplens.lenskit.knn.NeighborhoodSize;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.lenskit.vectors.ImmutableSparseVector;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.pattern.SpacePadder;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleItemItemScorer extends AbstractItemScorer {
    private static final Logger logger = LoggerFactory.getLogger(SimpleItemItemScorer.class);
    private final SimpleItemItemModel model;
    private final UserEventDAO userEvents;
    private final int neighborhoodSize;

    @Inject
    public SimpleItemItemScorer(SimpleItemItemModel m, UserEventDAO dao,
                                @NeighborhoodSize int nnbrs) {
        model = m;
        userEvents = dao;
        neighborhoodSize = nnbrs;
    }

    private void printNeighborhood(long item, List<ScoredId> neighbors) {
        logger.info("Printing the {} neighbors of item {}", neighbors.size(), item);
        int numprinted = 0;
        for (ScoredId scoredId : neighbors) {
            long jtem = scoredId.getId();
            double similarity = scoredId.getScore();
            logger.info("   jtem: {}, similarity: {}", jtem, similarity);
            numprinted++;
            if (numprinted >= 10) {
                break;
            }
        }
    }

    /**
     * The rating predicted for 'item' for 'user'
     */
    public double predictedRating(long user, long item) {
        List<ScoredId> neighbors = model.getNeighbors(item);
        printNeighborhood(item, neighbors);
        SparseVector userRatings = getUserRatingVector(user);
        Map<Long, Double> ratedNeighbors = new HashMap<Long, Double>();
        int numNeighbors = 0;
        for (ScoredId scoredId : neighbors) {
            long jtem = scoredId.getId();
            double similarity = scoredId.getScore();
            if (userRatings.containsKey(jtem)) {
                ratedNeighbors.put(jtem, similarity);
                numNeighbors++;
                if (numNeighbors >= neighborhoodSize) {
                    break;
                }
            }
        }
        SparseVector similarities = ImmutableSparseVector.create(ratedNeighbors);
        return similarities.dot(userRatings) / similarities.sum();
    }

    /**
     * Score items for a user.
     * @param user The user ID.
     * @param scores The score vector.  Its key domain is the items to score, and the scores
     *               (rating predictions) should be written back to this vector.
     */
    @Override
    public void score(long user, @Nonnull MutableSparseVector scores) {

        for (VectorEntry e: scores.fast(VectorEntry.State.EITHER)) {
            long item = e.getKey();
            // TODO Score this item and save the score into scores
            double prediction = predictedRating(user, item);
            scores.set(e, prediction);
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
