package com.example.bluetooth;

import android.app.Application;
import android.location.Location;
import android.os.Bundle;
import android.util.Xml;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.util.JsonUtils;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

public class NaverMapActivity extends AppCompatActivity implements OnMapReadyCallback  {
    private MapView mapView;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;  // 현재 위치를 반환하는 구현체
    private NaverMap naverMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navermap_main);

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(this);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        ToiletApiData toiletApiData = new ToiletApiData();
        Thread t = new Thread() {
            @Override
            public void run() {
                toiletData = toiletApiData.getData();
            }
        };
        try {
            t.start();
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if(!locationSource.isActivated()) {
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);  // 권한 거부시 추적 않음
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // 현재 자신의 위치 띄우기
    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        // 지도가 움직일 때 이벤트 발생
        naverMap.addOnCameraChangeListener(new NaverMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(int i, boolean b)  {
                removeMarkers();
                LatLng currentLocation = getCurrentLocation(naverMap);
                // 카메라의 위치가 바뀌면 for 문을 10000번 돌아야함. DP와 같은 알고리즘 적용하면 성능 향상 가능
                for(ToiletData data : toiletData) {
                    if(!checkOutOfRange(currentLocation, new LatLng(data.getLatitude(), data.getLongitude()))) {
                        continue;
                    }
                    Marker marker = new Marker();
                    marker.setPosition(new LatLng(data.getLatitude(), data.getLongitude()));
                    marker.setMap(naverMap);
                    marker.setCaptionText(data.getName());
                    activeMarkers.add(marker);
                }
            }
        });
        // 정적 데이터 추가
//        showMarkers();
    }

    private Vector<ToiletData> toiletData;
    private Vector<Marker> activeMarkers;


    // 현재 보고있는 위치의 인근 5키로 미터의 화장실만 표시
    public final static double REFERENCE_LAT_X3 = 5 / 109.958489129649955;
    public final static double REFERENCE_LNG_X3 = 5 / 88.74;
    public boolean checkOutOfRange(LatLng currentLocation, LatLng markerLocation) {
        boolean outOfRangeLat = Math.abs(currentLocation.latitude - markerLocation.latitude) <= REFERENCE_LAT_X3;
        boolean outOfRangeLng = Math.abs(currentLocation.longitude - markerLocation.longitude) <= REFERENCE_LNG_X3;
        return outOfRangeLat && outOfRangeLng;
    }

    private void removeMarkers() {
        if(activeMarkers == null) {
            activeMarkers = new Vector<Marker>();
            return;
        }
        for (Marker marker : activeMarkers) {
            marker.setMap(null);
        }
        activeMarkers = new Vector<Marker>();
    }

    public LatLng getCurrentLocation(NaverMap naverMap) {
        CameraPosition cameraPosition = naverMap.getCameraPosition();
        return new LatLng(cameraPosition.target.latitude, cameraPosition.target.longitude);
    }
}

// API 호출 및 사용 코드

class ToiletData {
    Double latitude;
    Double longitude;
    String name;
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return "TestData{" +
                "name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    boolean checkData() {
        return latitude > 0 && longitude > 0 && name.length() > 0;
    }
}

class ToiletApiData {
    String apiUrl = "http://api.data.go.kr/openapi/tn_pubr_public_toilet_api";
    String apiKey = "q3dZVPW3y72QlLMB6T1ivx%2FPx%2BYy68xXFSEAKO0Bso%2BpJiz8nj0ftXQ9TGzGNHKtIZyjdXkpmGgHV3wc121Tqw%3D%3D";  // Encoding

    public Vector<ToiletData> getData() {
        Vector<ToiletData> dataArr = new Vector<ToiletData>();
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    String fullUrl = apiUrl + "?ServiceKey=" + apiKey + "&numOfRows=10000&type=xml";  // 화장실 데이터 10000개 저장
                    System.out.println("fullUrl = " + fullUrl);
                    URL url = new URL(fullUrl);
                    InputStream is = url.openStream();

                    XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = xmlFactory.newPullParser();
                    parser.setInput(is, "utf-8");

                    boolean nameState = false;
                    boolean latState = false;
                    boolean longState = false;
                    String name = "";
                    String latitude = "";
                    String longitude = "";

                    while(parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                        int type = parser.getEventType();
                        ToiletData data = new ToiletData();
                        if(type == XmlPullParser.START_DOCUMENT) {
                            System.out.println("Start Document");
                        }
                        else if(type == XmlPullParser.START_TAG) {
                            String cur_tag = parser.getName();
                            switch (cur_tag) {
                                case "toiletNm":
                                    nameState = true;
                                    break;
                                case "latitude":
                                    latState = true;
                                    break;
                                case "longitude":
                                    longState = true;
                                    break;
                                default: break;
                            }
                        } else if(type == XmlPullParser.TEXT) {
                            if(nameState) {
                                name = parser.getText();
                                nameState = false;
                            } else if(latState) {
                                latitude = parser.getText();
                                latState = false;
                            } else if(longState)  {
                                longitude = parser.getText();
                                longState = false;
                            }
                        }
                        else if(type == XmlPullParser.END_TAG & parser.getName().equals("item")) {
                            try {
                                data.setName(name);
                                data.setLatitude(Double.valueOf(latitude));
                                data.setLongitude(Double.valueOf(longitude));
                                dataArr.add(data);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        type = parser.next();
                    }
               } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        try {
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return dataArr;
    }
}