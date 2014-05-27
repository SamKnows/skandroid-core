package com.samknows.measurement.activity.components;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;
import com.samknows.libcore.R;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class MapPopupInfoWindowAdapterUnused implements InfoWindowAdapter {
		  LayoutInflater inflater=null;

		  public MapPopupInfoWindowAdapterUnused(LayoutInflater inflater) {
		    this.inflater=inflater;
		  }

		  @Override
		  public View getInfoWindow(Marker marker) {
		    return(null);
		  }

		  @Override
		  public View getInfoContents(Marker marker) {
		    View popup=inflater.inflate(R.layout.map_popup_infowindowadapter_unused, null);

		    TextView tv=(TextView)popup.findViewById(R.id.title);

		    tv.setText(marker.getTitle());
		    tv=(TextView)popup.findViewById(R.id.snippet);
		    tv.setText(marker.getSnippet());

		    return(popup);
		  }
		}
