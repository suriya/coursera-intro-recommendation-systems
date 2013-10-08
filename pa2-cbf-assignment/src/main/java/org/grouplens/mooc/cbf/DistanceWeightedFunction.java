package org.grouplens.mooc.cbf;

import java.util.List;

import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.pref.Preference;

public class DistanceWeightedFunction implements UserProfileCreationHelper {

    @Override
    public double weightOfItem(List<Rating> userRatings, Preference p) {
        if (p == null) {
            return 0.0;
        }
        double avgRating = averageRating(userRatings);
        double itemRating = p.getValue();
        return (itemRating - avgRating);
    }

    // We call averageRating again and again for the same user. That's okay for now.
    private double averageRating(List<Rating> userRatings) {
        long numRatings = 0;
        double sumRatings = 0.0;
        for (Rating r: userRatings) {
            // In LensKit, ratings are expressions of preference
            Preference p = r.getPreference();
            // We'll never have a null preference. But in LensKit, ratings can have null
            // preferences to express the user unrating an item
            if (p == null) {
                continue;
            }
            double rating = p.getValue();
            sumRatings += rating;
            numRatings += 1;
        }
        return sumRatings / numRatings;
    }

}
