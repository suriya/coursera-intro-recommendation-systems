package org.grouplens.mooc.cbf;

import java.util.List;

import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.pref.Preference;

public interface UserProfileCreationHelper {
    /**
     * The weight of a particular item when creating a user's profile.
     *
     * @param userRatings all Ratings made by the user
     * @param p the particular rating preference that we want to weight
     * @return
     */
    public double weightOfItem(List<Rating> userRatings, Preference p);
}
