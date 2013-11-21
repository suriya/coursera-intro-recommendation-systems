package edu.umn.cs.recsys.svd;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.umn.cs.recsys.dao.*;
import org.grouplens.lenskit.ItemScorer;
import org.grouplens.lenskit.RecommenderBuildException;
import org.grouplens.lenskit.core.LenskitConfiguration;
import org.grouplens.lenskit.core.LenskitRecommender;
import org.grouplens.lenskit.data.dao.EventDAO;
import org.grouplens.lenskit.data.dao.ItemDAO;
import org.grouplens.lenskit.data.dao.UserDAO;
import org.grouplens.lenskit.vectors.SparseVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SVDMain {
    private static final Logger logger = LoggerFactory.getLogger("ii-assignment");
    private static final Pattern USER_ITEM_PAT = Pattern.compile("(\\d+):(\\d+)");

    /**
     * Main entry point to the program.
     * @param args The <tt>user:item</tt> pairs to score.
     */
    public static void main(String[] args) {
        SVDMain program = initialize(args);
        program.run();
    }

    /**
     * Parse arguments and set up an SVD runner.
     * @param args The command line arguments.
     * @return The SVD program, configured and ready to run.
     */
    public static SVDMain initialize(String[] args) {
        BaselineMode baselineMode = BaselineMode.GLOBAL_MEAN;
        Map<Long,Set<Long>> toScore = Maps.newHashMap();
        for (String arg: args) {
            logger.debug("parsing argument: {}", arg);
            if (arg.equals("--global-mean")) {
                baselineMode = BaselineMode.GLOBAL_MEAN;
            } else if (arg.equals("--user-mean")) {
                baselineMode = BaselineMode.USER_MEAN;
            } else if (arg.equals("--item-mean")) {
                baselineMode = BaselineMode.ITEM_MEAN;
            } else if (arg.equals("--user-item-mean")) {
                baselineMode = BaselineMode.USER_ITEM_MEAN;
            } else if (arg.equals("--all")) {
                toScore = null;
            } else if (arg.startsWith("--")) {
                throw new IllegalArgumentException("unknown flag " + arg);
            } else {
                Matcher m = USER_ITEM_PAT.matcher(arg);
                if (m.matches()) {
                    long uid = Long.parseLong(m.group(1));
                    long iid = Long.parseLong(m.group(2));
                    if (!toScore.containsKey(uid)) {
                        toScore.put(uid, Sets.<Long>newHashSet());
                    }
                    toScore.get(uid).add(iid);
                } else {
                    throw new IllegalArgumentException("unparseable argument " + arg);
                }
            }
        }
        return new SVDMain(baselineMode, toScore);
    }

    BaselineMode baselineMode;
    Map<Long,Set<Long>> toScore;

    /**
     * Construct a new SVD program.
     * @param base The baseline mode.
     * @param requests The items to score for each user.
     */
    public SVDMain(BaselineMode base, Map<Long,Set<Long>> requests) {
        baselineMode = base;
        toScore = requests;
    }

    /**
     * Create the LensKit recommender configuration.
     * @return The LensKit recommender configuration.
     */
    // LensKit configuration API generates some unchecked warnings, turn them off
    @SuppressWarnings("unchecked")
    private LenskitConfiguration configureRecommender() {
        LenskitConfiguration config = new LenskitConfiguration();
        // configure the rating data source
        config.bind(EventDAO.class)
              .to(MOOCRatingDAO.class);
        config.set(RatingFile.class)
              .to(new File("data/ratings.csv"));

        // use custom item and user DAOs
        // our item DAO has title information
        config.bind(ItemDAO.class)
              .to(MOOCItemDAO.class);
        config.addRoot(UserDAO.class);
        // and title file
        config.set(TitleFile.class)
              .to(new File("data/movie-titles.csv"));

        // our user DAO can look up by user name
        config.bind(UserDAO.class)
              .to(MOOCUserDAO.class);
        config.addRoot(UserDAO.class);
        config.set(UserFile.class)
              .to(new File("data/users.csv"));

        // use the item-item scorer you will implement to score items
        config.bind(ItemScorer.class)
              .to(SVDItemScorer.class);
        baselineMode.configure(config);
        config.set(LatentFeatureCount.class)
              .to(10);
        return config;
    }

    public void run() {
        LenskitConfiguration config = configureRecommender();
        LenskitRecommender rec;
        try {
            rec = LenskitRecommender.build(config);
        } catch (RecommenderBuildException e) {
            logger.error("error building recommender", e);
            System.exit(2);
            throw new AssertionError(); // to de-confuse unreachable code detection
        }

        // Get the item title DAO, so we can look up movie titles
        ItemTitleDAO titleDAO = rec.get(ItemTitleDAO.class);

        // Get the item scorer and go!
        ItemScorer scorer = rec.getItemScorer();
        assert scorer != null;

        if (toScore == null) {
            logger.debug("loading user/item sets");
            UserDAO userDAO = rec.get(UserDAO.class);
            if (userDAO == null) {
                logger.error("no user DAO");
                System.exit(2);
            }
            toScore = Maps.newHashMap();
            for (Long user: userDAO.getUserIds()) {
                toScore.put(user, titleDAO.getItemIds());
            }
        }

        logger.info("scoring for {} users", toScore.size());
        for (Map.Entry<Long,Set<Long>> scoreRequest: toScore.entrySet()) {
            long user = scoreRequest.getKey();
            Set<Long> items = scoreRequest.getValue();
            logger.info("scoring {} items for user {}", items.size(), user);
            // We call the score method that takes a set of items.
            // AbstractItemScorer delegates this method to the one you are supposed to implement.
            SparseVector scores = scorer.score(user, items);
            for (long item: items) {
                String score;
                if (scores.containsKey(item)) {
                    score = String.format(Locale.ROOT, "%.4f", scores.get(item));
                } else {
                    score = "NA";
                }
                String title = titleDAO.getItemTitle(item);
                System.out.format("%d,%d,%s,%s\n", user, item, score, title);
            }
        }
    }
}
