package edu.umn.cs.recsys.ii;

import org.grouplens.lenskit.basic.AbstractGlobalItemScorer;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;
import org.grouplens.lenskit.vectors.VectorEntry.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global item scorer to find similar items.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleGlobalItemScorer extends AbstractGlobalItemScorer {
    private static final Logger logger = LoggerFactory.getLogger(SimpleGlobalItemScorer.class);
    private final SimpleItemItemModel model;

    @Inject
    public SimpleGlobalItemScorer(SimpleItemItemModel mod) {
        model = mod;
    }

    /**
     * Return a map of an item's neighbors mapped to similarity.
     * @param item
     * @return
     */
    private Map<Long, Double> neighborsMap(long item) {
        List<ScoredId> neighbors = model.getNeighbors(item);
        Map<Long, Double> neighborsMap = new HashMap<Long, Double>();
        for (ScoredId scoredId : neighbors) {
            long jtem = scoredId.getId();
            double similarity = scoredId.getScore();
            neighborsMap.put(jtem, similarity);
        }
        return neighborsMap;
    }

    /**
     * Score item with respect to a set of reference items.
     * @param referenceItems The reference items.
     * @param item The item to be scored
     */
    private double myGlobalScore(@Nonnull Collection<Long> referenceItems, long item) {
        Map<Long, Double> nbrsMap = neighborsMap(item);
        MutableSparseVector referenceSimilarities = MutableSparseVector.create(referenceItems, 0.0);
        for (VectorEntry entry : referenceSimilarities.fast(State.EITHER)) {
            long jtem = entry.getKey();
            if (nbrsMap.containsKey(jtem)) {
//                logger.info("Getting value for item {} from nbrsMap {}", jtem, nbrsMap.get(jtem));
                double similarity = nbrsMap.get(jtem);
                referenceSimilarities.set(entry, similarity);
            }
        }
        return referenceSimilarities.sum();
    }

    /**
     * Score items with respect to a set of reference items.
     * @param items The reference items.
     * @param scores The score vector. Its domain is the items to be scored, and the scores should
     *               be stored into this vector.
     */
    @Override
    public void globalScore(@Nonnull Collection<Long> items, @Nonnull MutableSparseVector scores) {
        for (VectorEntry entry : scores.fast(State.EITHER)) {
            long item = entry.getKey();
            double score = myGlobalScore(items, item);
            scores.set(entry, score);
        }
    }
}
