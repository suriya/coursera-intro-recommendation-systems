package edu.umn.cs.recsys;

import org.grouplens.lenskit.data.dao.EventDAO;
import org.grouplens.lenskit.data.dao.PrefetchingUserEventDAO;
import org.grouplens.lenskit.data.dao.UserEventDAO;
import org.grouplens.lenskit.eval.data.traintest.QueryData;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * DAO shim to let scorers use the query data
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class QueryDAOProvider implements Provider<UserEventDAO> {
    private final EventDAO queryEvents;

    @Inject
    public QueryDAOProvider(@QueryData EventDAO qEvents) {
        queryEvents = qEvents;
    }
    @Override
    public UserEventDAO get() {
        return new PrefetchingUserEventDAO(queryEvents);
    }
}