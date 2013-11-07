package edu.umn.cs.recsys.ii;

import org.grouplens.grapht.annotation.DefaultProvider;
import org.grouplens.lenskit.core.Shareable;
import org.grouplens.lenskit.scored.ScoredId;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
@Shareable
@DefaultProvider(SimpleItemItemModelBuilder.class)
public class SimpleItemItemModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Long,List<ScoredId>> neighborhoods;

    /**
     * Create a new item-item model.
     * @param nbrhoods A mapping of items to neighborhoods.  The neighborhoods
     *                 must be sorted by similarity in non-increasing order (most
     *                 similar neighbors first).  They should not have any negative
     *                 scores.
     */
    public SimpleItemItemModel(Map<Long,List<ScoredId>> nbrhoods) {
        neighborhoods = nbrhoods;
    }

    /**
     * Get the neighbors of an item.
     * @return The neighbors of the item, sorted by decreasing score.
     */
    public List<ScoredId> getNeighbors(long item) {
        List<ScoredId> nbrs = neighborhoods.get(item);
        if (nbrs == null) {
            return Collections.emptyList();
        } else {
            return nbrs;
        }
    }
}
