package org.grouplens.mooc.cbf;

import java.util.List;

import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.pref.Preference;

public class SimpleWeightFunction implements UserProfileCreationHelper {

    @Override
    public double weightOfItem(List<Rating> userRatings, Preference p) {
        if (p == null) {
            return 0.0;
        }
        if (p.getValue() < 3.5) {
            return 0.0;
        }
        return 1.0;
    }
}
