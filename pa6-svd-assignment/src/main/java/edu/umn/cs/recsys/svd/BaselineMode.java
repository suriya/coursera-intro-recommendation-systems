package edu.umn.cs.recsys.svd;

import org.grouplens.lenskit.ItemScorer;
import org.grouplens.lenskit.baseline.*;
import org.grouplens.lenskit.core.LenskitConfiguration;

/**
 * Baseline modes for the SVD recommender.  Used by {@link SVDMain} to control configuration. You
 * don't need to do anything with these.
 */
public enum BaselineMode {
    GLOBAL_MEAN {
        @Override
        public void configure(LenskitConfiguration config) {
            config.within(SVDItemScorer.class)
                  .bind(BaselineScorer.class,ItemScorer.class)
                  .to(GlobalMeanRatingItemScorer.class);
        }
    },
    USER_MEAN {
        @Override
        public void configure(LenskitConfiguration config) {
            config.within(SVDItemScorer.class)
                  .bind(BaselineScorer.class,ItemScorer.class)
                  .to(UserMeanItemScorer.class);
        }
    },
    ITEM_MEAN {
        @Override
        public void configure(LenskitConfiguration config) {
            config.within(SVDItemScorer.class)
                  .bind(BaselineScorer.class,ItemScorer.class)
                  .to(ItemMeanRatingItemScorer.class);
        }
    },
    USER_ITEM_MEAN {
        @Override
        public void configure(LenskitConfiguration config) {
            config.within(SVDItemScorer.class)
                  .bind(BaselineScorer.class,ItemScorer.class)
                  .to(UserMeanItemScorer.class);
            config.within(SVDItemScorer.class)
                  .bind(UserMeanBaseline.class,ItemScorer.class)
                  .to(ItemMeanRatingItemScorer.class);
        }
    };
    public abstract void configure(LenskitConfiguration config);
}
