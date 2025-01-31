package com.example.riskseeker;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.example.riskseeker.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap map;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private UiSettings mUiSettings;
    private Marker marcador;
    public LocationManager locationManager;
    public String provider;
    double latitud;
    double longitud;

    //private EditText SearchText;
    private static final String TAG = "MapActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //SearchText = findViewById(R.id.input_search);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        obtenerLocalizacion();
    }


    private void obtenerLocalizacion() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            actualizarUbicacion();
            }
        else{
            activarUbicacion();
        }
    }

    private void activarUbicacion(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.activar)
                .setMessage(R.string.mensaje_ubicacion)
                .setCancelable(false)

                //Si se selecciona la opción de activar la ubicación se ejecutara la ventana  para activarlo
                .setPositiveButton(R.string.boton_activar, (dialogInterface, i) -> {
                    Intent activarUbicacion = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(activarUbicacion);
                    finish();
                })

                //----------------------Falta determina que se hará sino se activa la ubicación----------------------------------//
                .setNegativeButton(R.string.boton_noactivar, (dialogInterface, i) -> finish())
                .show();

    }
    @SuppressLint("MissingPermission")
    protected void actualizarUbicacion(){
        //Criterio de aplicación para seleccionar un proveedor de ubicación
        Criteria criterio = new Criteria();
        provider = String.valueOf(locationManager.getBestProvider(criterio, true));

        Location localizacion = locationManager.getLastKnownLocation(provider);
        if (localizacion != null) {
            latitud = localizacion.getLatitude();
            longitud = localizacion.getLongitude();
        }
        else {
            locationManager.requestLocationUpdates(provider, 1000, 0, this);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onLocationChanged(Location location) {

        //remove location callback:
        locationManager.removeUpdates(this);

        //open the map:
        latitud = location.getLatitude();
        longitud = location.getLongitude();
        onMapReady(map);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        LatLng ubicacion = new LatLng(latitud, longitud);
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        marcador = googleMap.addMarker(new MarkerOptions()
                .position(ubicacion)
                .title("Tú").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)));

        googleMap.setOnMarkerClickListener(this);
        map.moveCamera(CameraUpdateFactory.newLatLng(ubicacion));
        map.moveCamera(CameraUpdateFactory.zoomTo(15));

        mUiSettings = map.getUiSettings();

        //Cargar heatmap
        heatMap(map);
       // searchLocalitation();
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        if(marker.equals(marcador)){
            Intent cargar_reportes = new Intent(getApplicationContext(),ReporteActivity.class);
            startActivity(cargar_reportes);
        }
        return false;
    }

    public void Formulario(View view) {
        Intent cargarMapa = new Intent(getApplicationContext(),FormularioReporteActivity.class);
        startActivity(cargarMapa);
    }

    /*
    private void searchLocalitation() {
        SearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {
                    geoLocate();
                }

                return false;
            }
        });
    }

    private void geoLocate(){

        String searchString = SearchText.getText().toString();

        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();
        try{
            list = geocoder.getFromLocationName(searchString, 1);
        }catch (IOException e){
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage() );
        }
        if(list.size() > 0){
            Address address = list.get(0);
            Log.d(TAG, "geoLocate: found a location: " + address.toString());

        }
    } */

    //Heatmap
    private void heatMap(GoogleMap googleMap) {
        map = googleMap;
        List<LatLng> reports = new ArrayList<>();
        // Datos dummy para que no este tan vacio
        reports.add(new LatLng(-33.03764855235323, -71.59483864850512));
        reports.add(new LatLng(-33.03849849235025, -71.5943638975067));
        reports.add(new LatLng(-33.037862162391804, -71.595436781072));
        reports.add(new LatLng(-33.037519824453916, -71.59443884144254));
        reports.add(new LatLng(-33.03810810497309, -71.59459436330765));
        
        String TAG = "readData";
        // Referencia a reportes
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        DatabaseReference ref = mDatabase.child("Reporte");
        // TODO: falta agregar un geohash para traer de firebase solo los reportes mas cercanos a x radio https://firebaseopensource.com/projects/firebase/geofire-android/
        // Por ahora la query trae todos los reportes
        // Leer de firebase
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Obtiene datos y se actualiza 
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    Double lat = ds.child("latitud").getValue(Double.class);
                    Double lon = ds.child("longitud").getValue(Double.class);
                    reports.add(new LatLng(lat, lon));
                }

                // Crea el heatmap.
                HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                        .data(reports)
                        .radius(30)
                        .build();
                // Añadir heatmap overlay al mapa
                TileOverlay overlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Error
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }
}