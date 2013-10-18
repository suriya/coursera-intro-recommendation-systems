package edu.umn.cs.recsys;

import org.grouplens.grapht.annotation.DefaultProvider;
import org.grouplens.lenskit.basic.AbstractItemScorer;
import org.grouplens.lenskit.core.Shareable;
import org.grouplens.lenskit.core.Transient;
import org.grouplens.lenskit.cursors.Cursor;
import org.grouplens.lenskit.data.dao.EventDAO;
import org.grouplens.lenskit.data.dao.ItemDAO;
import org.grouplens.lenskit.data.event.Event;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Provider;
import java.io.Serializable;

/**
 * Score items by popularity (rating count).
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
@Shareable
@DefaultProvider(PopularityItemScorer.Builder.class)
public class PopularityItemScorer extends AbstractItemScorer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final SparseVector itemPopularity;

    private PopularityItemScorer(SparseVector pops) {
        itemPopularity = pops;
    }

    @Override
    public void score(long l, @Nonnull MutableSparseVector vectorEntries) {
        vectorEntries.set(itemPopularity);
    }

    public static class Builder implements Provider<PopularityItemScorer> {
        private final EventDAO eventDAO;
        private final ItemDAO itemDAO;

        @Inject
        public Builder(@Transient EventDAO edao, @Transient ItemDAO idao) {
            eventDAO = edao;
            itemDAO = idao;
        }

        @Override
        public PopularityItemScorer get() {
            MutableSparseVector vec = MutableSparseVector.create(itemDAO.getItemIds(), 0);
            Cursor<Event> stream = eventDAO.streamEvents();
            try {
                for (Event e: stream) {
                    vec.add(e.getItemId(), 1);
                }
            } finally {
                stream.close();
            }
            return new PopularityItemScorer(vec);
        }
    }
}
