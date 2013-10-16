package edu.umn.cs.recsys.uu;

import java.util.List;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

import org.grouplens.lenskit.basic.AbstractItemScorer;
import org.grouplens.lenskit.data.dao.ItemEventDAO;
import org.grouplens.lenskit.data.dao.UserEventDAO;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.history.History;
import org.grouplens.lenskit.data.history.RatingVectorUserHistorySummarizer;
import org.grouplens.lenskit.data.history.UserHistory;
import org.grouplens.lenskit.data.pref.Preference;
import org.grouplens.lenskit.vectors.ImmutableSparseVector;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;
import org.grouplens.lenskit.vectors.similarity.CosineVectorSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * User-user item scorer.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleUserUserItemScorer extends AbstractItemScorer {

    private static final Logger logger = LoggerFactory.getLogger(SimpleUserUserItemScorer.class);

    private static final CosineVectorSimilarity cvs = new CosineVectorSimilarity();

    private final UserEventDAO userDao;
    private final ItemEventDAO itemDao;

    @Inject
    public SimpleUserUserItemScorer(UserEventDAO udao, ItemEventDAO idao) {
        userDao = udao;
        itemDao = idao;
    }

    private static void meanCenterVector(MutableSparseVector v) {
        double mean = v.mean();
        v.add(-mean);
    }

    /**
     * Compute similarity score between two users, given their ratings
     * vector.
     */
    private static double similarity(SparseVector v1, SparseVector v2) {
        MutableSparseVector mv1 = v1.mutableCopy();
        MutableSparseVector mv2 = v2.mutableCopy();
        meanCenterVector(mv1);
        meanCenterVector(mv2);
        return cvs.similarity(mv1, mv2);
    }

    /**
     * Get all ratings given to 'item' by 'users', i.e. one rating for each
     * user in the set 'users'.
     */
    private SparseVector getItemRatings(long item, LongSortedSet users) {
        MutableSparseVector itemRatingsSubset = MutableSparseVector.create(users);
        List<Rating> itemRatings = itemDao.getEventsForItem(item, Rating.class);
        if (itemRatings == null) {
            return itemRatingsSubset;
        }
        for (Rating r : itemRatings) {
            Preference p = r.getPreference();
            if (p == null) {
                continue;
            }
            long user = p.getUserId();
            if (! users.contains(user)) {
                continue;
            }
            double rating = p.getValue();
            itemRatingsSubset.set(user, rating);
        }
        return itemRatingsSubset;
    }

    /**
     * Get the mean rating made by each user in the set 'users'.
     */
    private SparseVector getMeanRatings(LongSortedSet users) {
        MutableSparseVector meanRatings = MutableSparseVector.create(users);
        for (VectorEntry ve : meanRatings.fast(VectorEntry.State.UNSET)) {
            long user = ve.getKey();
            double mean = getUserRatingVector(user).mean();
            meanRatings.set(ve, mean);
        }
        return meanRatings.freeze();
    }

    /**
     * Return the predicted rating of 'item' for 'user'.
     */
    private double predictedRating(long user, long item) {
        // All sparse vectors have the key domain equal to the neighborhood.
        SparseVector similarities = neighborhood(user, item);
        logger.info("User {}'s neighborhood size: {}", user, similarities.size());
        SparseVector itemRatings = getItemRatings(item, similarities.keySet());
        SparseVector meanRatings = getMeanRatings(similarities.keySet());
        double denominator = 0.0;
        for (VectorEntry e : similarities.fast(VectorEntry.State.SET)) {
            double similarity = e.getValue();
            denominator += Math.abs(similarity);
        }
        MutableSparseVector itemRatingsMutable = itemRatings.mutableCopy();
        itemRatingsMutable.subtract(meanRatings);
        double numerator = itemRatingsMutable.dot(similarities);
        return (numerator / denominator) + getUserRatingVector(user).mean();
    }

    /**
     * Return the neighborhood of 'user' among those who have rated 'item'.
     */
    private ImmutableSparseVector neighborhood(long user, long item) {
        // Get vsers that rated 'item'
        LongSet vsers = this.itemDao.getUsersForItem(item);
        logger.info("# ratings for item {} = {} (might include user {})", item, vsers.size(), user);
        vsers.remove(user);
        MutableSparseVector similarities = MutableSparseVector.create(vsers);
        // Compute similarities between user and vsers
        SparseVector userVector = getUserRatingVector(user);
        for (VectorEntry e : similarities.fast(VectorEntry.State.UNSET)) {
            long vser = e.getKey();
            SparseVector vserVector = getUserRatingVector(vser);
            double similarity = similarity(userVector, vserVector);
            similarities.set(e, similarity);
        }
        // Sorted vsers by similarity score
        LongArrayList sortedvsers = similarities.keysByValue(true);
        int topNsize = Math.min(30, sortedvsers.size());
        long[] topNvsers = new long[topNsize];
        // Create similarity vector for just the top 30 vsers
        sortedvsers.getElements(0, topNvsers, 0, topNsize);
        MutableSparseVector topNsimilarity = MutableSparseVector.create(topNvsers);
        for (long vser : topNvsers) {
            topNsimilarity.set(vser, similarities.get(vser));
        }
        return topNsimilarity.freeze();
    }

    @SuppressWarnings("unused")
	private static void printUserVector(SparseVector userVector, long user) {
        for (VectorEntry e : userVector.fast(VectorEntry.State.SET)) {
            long item = e.getKey();
            double rating = e.getValue();
            logger.info("User {} rated item {} as {}", user, item, rating);
        }
        logger.info("Mean rating {}", userVector.mean());
    }

    @Override
    public void score(long user, @Nonnull MutableSparseVector scores) {
        logger.info("Scoring for user {}", user);
        // TODO Score items for this user using user-user collaborative filtering

        // This is the loop structure to iterate over items to score
        for (VectorEntry e: scores.fast(VectorEntry.State.EITHER)) {
            long item = e.getKey();
            double prediction = predictedRating(user, item);
            scores.set(e, prediction);
        }
    }

    /**
     * Get a user's rating vector.
     * @param user The user ID.
     * @return The rating vector.
     */
    private SparseVector getUserRatingVector(long user) {
        UserHistory<Rating> history = userDao.getEventsForUser(user, Rating.class);
        if (history == null) {
            history = History.forUser(user);
        }
        return RatingVectorUserHistorySummarizer.makeRatingVector(history);
    }
}
