package edu.umn.cs.recsys.svd;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.grouplens.lenskit.ItemScorer;
import org.grouplens.lenskit.baseline.BaselineScorer;
import org.grouplens.lenskit.core.Transient;
import org.grouplens.lenskit.cursors.Cursor;
import org.grouplens.lenskit.data.dao.ItemDAO;
import org.grouplens.lenskit.data.dao.UserDAO;
import org.grouplens.lenskit.data.dao.UserEventDAO;
import org.grouplens.lenskit.data.event.Event;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.event.Ratings;
import org.grouplens.lenskit.data.history.UserHistory;
import org.grouplens.lenskit.indexes.IdIndexMapping;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Model builder that computes the SVD model.
 */
public class SVDModelBuilder implements Provider<SVDModel> {
    private static final Logger logger = LoggerFactory.getLogger(SVDModelBuilder.class);

    private final UserEventDAO userEventDAO;
    private final UserDAO userDAO;
    private final ItemDAO itemDAO;
    private final ItemScorer baselineScorer;
    private final int featureCount;

    /**
     * Construct the model builder.
     * @param uedao The user event DAO.
     * @param udao The user DAO.
     * @param idao The item DAO.
     * @param baseline The baseline scorer (this will be used to compute means).
     * @param nfeatures The number of latent features to train.
     */
    @Inject
    public SVDModelBuilder(@Transient UserEventDAO uedao,
                           @Transient UserDAO udao,
                           @Transient ItemDAO idao,
                           @Transient @BaselineScorer ItemScorer baseline,
                           @LatentFeatureCount int nfeatures) {
        logger.debug("user DAO: {}", udao);
        userEventDAO = uedao;
        userDAO = udao;
        itemDAO = idao;
        baselineScorer = baseline;
        featureCount = nfeatures;
    }

    /**
     * Build the SVD model.
     *
     * @return A singular value decomposition recommender model.
     */
    @Override
    public SVDModel get() {
        // Create index mappings of user and item IDs.
        // You can use these to find row and columns in the matrix based on user/item IDs.
        IdIndexMapping userMapping = IdIndexMapping.create(userDAO.getUserIds());
        logger.debug("indexed {} users", userMapping.size());
        IdIndexMapping itemMapping = IdIndexMapping.create(itemDAO.getItemIds());
        logger.debug("indexed {} items", itemMapping.size());

        // We have to do 2 things:
        // First, prepare a matrix containing the rating data.
        RealMatrix matrix = createRatingMatrix(userMapping, itemMapping);

        // Second, compute its factorization
        // All the work is done in the constructor
        SingularValueDecomposition svd = new SingularValueDecomposition(matrix);

        // Third, truncate the decomposed matrix
        // TODO Truncate the matrices and construct the SVD model

        // TODO Replace this throw line with returning the model when you are finished
        throw new UnsupportedOperationException("SVD model not yet implemented");
    }

    /**
     * Build a rating matrix from the rating data.  Each user's ratings are first normalized
     * by subtracting a baseline score (usually a mean).
     *
     * @param userMapping The index mapping of user IDs to column numbers.
     * @param itemMapping The index mapping of item IDs to row numbers.
     * @return A matrix storing the <i>normalized</i> user ratings.
     */
    private RealMatrix createRatingMatrix(IdIndexMapping userMapping, IdIndexMapping itemMapping) {
        final int nusers = userMapping.size();
        final int nitems = itemMapping.size();

        // Create a matrix with users on rows and items on columns
        logger.info("creating {} by {} rating matrix", nusers, nitems);
        RealMatrix matrix = MatrixUtils.createRealMatrix(nusers, nitems);

        // populate it with data
        Cursor<UserHistory<Event>> users = userEventDAO.streamEventsByUser();
        try {
            for (UserHistory<Event> user: users) {
                // Get the row number for this user
                int u = userMapping.getIndex(user.getUserId());
                MutableSparseVector ratings = Ratings.userRatingVector(user.filter(Rating.class));
                MutableSparseVector baselines = MutableSparseVector.create(ratings.keySet());
                baselineScorer.score(user.getUserId(), baselines);
                // TODO Populate this user's row with their ratings, minus the baseline scores
            }
        } finally {
            users.close();
        }

        return matrix;
    }
}
