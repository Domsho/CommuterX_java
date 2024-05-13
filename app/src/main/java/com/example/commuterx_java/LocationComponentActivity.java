package com.example.commuterx_java;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.mapbox.maps.MapInitOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.locationcomponent.*;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationPuckComponent;
import com.mapbox.maps.plugin.locationcomponent.PermissionsManager;
import com.mapbox.maps.plugin.locationcomponent.PermissionsListener;
import com.mapbox.maps.plugin.locationcomponent.BearingSource;
import java.util.List;

public class LocationComponentActivity extends AppCompatActivity {
    private MapView mapView;
    private LocationComponentPlugin locationComponent;

}