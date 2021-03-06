package br.com.fiap.mapline.listeners;

import android.content.Context;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import br.com.fiap.mapline.util.BitMapUtil;
import br.com.fiap.mapline.util.MyMapUtil;

/**
 * Created by VyMajoriss on 5/18/2016.
 */
public class MyMapRefChildEventListener implements ChildEventListener {

    String polylineId;
    Firebase mapRef;
    GoogleMap map;
    Context context;

    public MyMapRefChildEventListener(Context context, String polylineId, Firebase mapRef, GoogleMap map) {
        this.context = context;
        this.polylineId = polylineId;
        this.mapRef = mapRef;
        this.map = map;
    }

    HashMap<String, PolylineOptions> polylineOptionsMap = new HashMap<>();
    HashMap<String, List<Polyline>> polylineHashMap = new HashMap<>();
    HashMap<String, List<LatLng>> latLngHashMap = new HashMap<>();
    HashMap<String, Integer> colorHashMap = new HashMap<>();
    HashMap<String, Marker> markerHashMap = new HashMap<>();
    HashMap<String, String> avatarHashMap = new HashMap<>();

    @Override
    public void onChildAdded(DataSnapshot snap, String s) {

        if (!snap.getKey().equals(polylineId)) {

            polylineOptionsMap.put(snap.getKey(), new PolylineOptions());
            polylineHashMap.put(snap.getKey(), new ArrayList<Polyline>());
            latLngHashMap.put(snap.getKey(), new ArrayList<LatLng>());
            markerHashMap.put(snap.getKey(), null);
            avatarHashMap.put(snap.getKey(), null);

            snap.child("details").getRef().addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    int color = Integer.valueOf(dataSnapshot.child("color").getValue().toString());
                    String avatar = dataSnapshot.child("avatar").getValue().toString();
                    colorHashMap.put(dataSnapshot.getRef().getParent().getKey(), color);
                    avatarHashMap.put(dataSnapshot.getRef().getParent().getKey(), avatar);
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {}
            });

            mapRef.child(snap.getKey()).addChildEventListener(new ChildEventListener() {

                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    String parentKey = dataSnapshot.getRef().getParent().getKey();
                    try {
                        if (!dataSnapshot.getKey().equals("details")) {
                            if (markerHashMap.get(parentKey) != null) {
                                markerHashMap.get(parentKey).remove();
                            }

                            LatLng latLng = new Gson().fromJson((String) dataSnapshot.getValue(), LatLng.class);
                            polylineOptionsMap.put(parentKey, polylineOptionsMap.get(parentKey).color(colorHashMap.get(parentKey)).add(latLng));
                            List<Polyline> polyTempArray = polylineHashMap.get(parentKey);
                            polyTempArray.add(map.addPolyline(polylineOptionsMap.get(parentKey)));
                            polylineHashMap.put(parentKey, polyTempArray);
                            List<LatLng> latlngTempArray = latLngHashMap.get(parentKey);
                            latlngTempArray.add(latLng);
                            latLngHashMap.put(parentKey, latlngTempArray);
                            markerHashMap.put(parentKey, map.addMarker(new MarkerOptions()
                                    .position(MyMapUtil.getCenter(latLngHashMap.get(parentKey)))
                                    .icon(BitmapDescriptorFactory.fromBitmap(BitMapUtil.getMarkerBitmapFromView(avatarHashMap.get(parentKey), context)))));
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    //Removes last
                    String parentKey = dataSnapshot.getRef().getParent().getKey();
                    if (!parentKey.equals(polylineId) || !dataSnapshot.getKey().equals("color")) {
                        List<LatLng> latLngs = latLngHashMap.get(parentKey);
                        if (markerHashMap.get(parentKey) != null) {
                            markerHashMap.get(parentKey).remove();
                        }

                        try {
                            LatLng latLng = new Gson().fromJson((String) dataSnapshot.getValue(), LatLng.class);
                            MyMapUtil.removeLastLatLng(latLng, latLngs.listIterator(latLngs.size()));

                            for (Polyline polyline : polylineHashMap.get(parentKey)) {
                                polyline.remove();
                            }

                            int color = polylineOptionsMap.get(parentKey).getColor();
                            polylineOptionsMap.put(parentKey, new PolylineOptions().color(color).addAll(latLngs));
                            List<Polyline> polyTempArray = polylineHashMap.get(parentKey);
                            polyTempArray.add(map.addPolyline(polylineOptionsMap.get(parentKey)));
                            polylineHashMap.put(parentKey, polyTempArray);

                            if (!polylineOptionsMap.get(parentKey).getPoints().isEmpty()) {
                                markerHashMap.put(parentKey, map.addMarker(new MarkerOptions()
                                        .position(MyMapUtil.getCenter(latLngHashMap.get(parentKey)))
                                        .icon(BitmapDescriptorFactory.fromBitmap(BitMapUtil.getMarkerBitmapFromView(avatarHashMap.get(parentKey), context)))));
                            }
                        } catch (Exception e) {

                        }
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

                @Override
                public void onCancelled(FirebaseError firebaseError) {}
            });

        }
    }

    @Override
    public void onChildChanged(DataSnapshot snap, String s) {}

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {

        if (!dataSnapshot.getKey().equals(polylineId) || dataSnapshot.getKey().equals("color")) {
            for (Polyline polyline : polylineHashMap.get(dataSnapshot.getKey())) {
                polyline.remove();
            }
            polylineOptionsMap.put(dataSnapshot.getKey(), new PolylineOptions());
        }
    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

    @Override
    public void onCancelled(FirebaseError firebaseError) {}

}
