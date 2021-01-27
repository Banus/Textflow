package com.example.googlemap;

import android.content.Context;

import com.google.maps.GeoApiContext;
import com.google.maps.PlacesApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.PlaceType;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.RankBy;
import com.google.maps.model.LatLng;
import java.io.IOException;

class NearbySearch {

    private String api_key;

    NearbySearch(Context context) {
        api_key = context.getString(R.string.google_maps_key);
    }

    PlacesSearchResponse run(LatLng location, PlaceType placeType){
        PlacesSearchResponse request = new PlacesSearchResponse();
        GeoApiContext context = new GeoApiContext.Builder().apiKey(api_key).build();

        try {
            request = PlacesApi.nearbySearchQuery(context, location)
                    .radius(5000)
                    .rankby(RankBy.PROMINENCE)
                    .language("en")
                    .type(placeType)
                    .await();
        } catch (ApiException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return request;
    }
}