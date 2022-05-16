package com.example.bluetooth;

import android.app.Application;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.util.JsonUtils;
import com.naver.maps.geometry.LatLng;
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
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

//        naverMap.addOnLocationChangeListener(new NaverMap.OnLocationChangeListener() {
//            @Override
//            public void onLocationChange(@NonNull Location location) {
//                lat = location.getLatitude();  // 위도
//                lon = location.getLongitude();  // 경도
//
////                Toast.makeText(getApplicationContext(), lat + "°N, " + lon + "°E", Toast.LENGTH_SHORT).show();
//            }
//        });

        // add new (정적 데이터 추가)
        showMarkers();
    }

    // 화장실 데이터 가져오기
    // 2022-05-17 03:37 현재 공공데이터 포털 사이트 오류. 일단 동적으로 화장실 위치 설정
    public void showMarkers() {
        MarkersData markersData = new MarkersData();
        ArrayList<Markers> dataArr = markersData.getData();
        for(Markers data : dataArr) {
            Marker marker = new Marker();
            marker.setWidth(50);
            marker.setHeight(50);
            marker.setPosition(new LatLng(data.getLatitude(), data.getLongitude()));
            marker.setZIndex(0);
            marker.setIcon(OverlayImage.fromResource(R.drawable.navermap_default_marker_icon_red));
            marker.setCaptionText(data.getName());
            marker.setMap(naverMap);
        }
    }
}

//class ToiletData {
//    Double latitude;
//    Double longitude;
//    String name;
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setLatitude(Double latitude) {
//        this.latitude = latitude;
//    }
//
//    public Double getLatitude() {
//        return latitude;
//    }
//
//    public void setLongitude(Double longitude) {
//        this.longitude = longitude;
//    }
//
//    public Double getLongitude() {
//        return longitude;
//    }
//
//    @Override
//    public String toString() {
//        return "TestData{" +
//                "name='" + name + '\'' +
//                ", latitude=" + latitude +
//                ", longitude=" + longitude +
//                '}';
//    }
//}

//class ToiletApiData {
//    String apiUrl = "http://openapi.gimje.go.kr/rest/toilet";
////    StringBuilder apiUrl = new StringBuilder("http://openapi.gimje.go.kr/rest/toilet");
//    String apiKey = "q3dZVPW3y72QlLMB6T1ivx%2FPx%2BYy68xXFSEAKO0Bso%2BpJiz8nj0ftXQ9TGzGNHKtIZyjdXkpmGgHV3wc121Tqw%3D%3D";  // Encoding
////    String apiKey = q3dZVPW3y72QlLMB6T1ivx/Px+Yy68xXFSEAKO0Bso+pJiz8nj0ftXQ9TGzGNHKtIZyjdXkpmGgHV3wc121Tqw==;  // Decoding
//
//    public ArrayList<ToiletData> getData() {
//        ArrayList<ToiletData> dataArr = new ArrayList<ToiletData>();
//        Thread thread = new Thread() {
//            @Override
//            public void run() {
//                try {
//                    String fullUrl = apiUrl + "?ServiceKey=" + apiKey + "&pageNo=1&numOfRows=10";  // [+ "&returnType=XML"] ? 추가 파라미터 기입할 것
//                    System.out.println("fullUrl = " + fullUrl);
//                    URL url = new URL(fullUrl);
//                    InputStream is = url.openStream();
//
//                    XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
//                    XmlPullParser parser = xmlFactory.newPullParser();
//                    parser.setInput(is, "utf-8");
//
//                    boolean nameState = false;
//                    boolean latState = false;
//                    boolean longState = false;
//                    String name = "";
//                    String latitude = "";
//                    String longitude = "";
//
//                    while(parser.getEventType() != XmlPullParser.END_DOCUMENT) {
//                        int type = parser.getEventType();
//                        ToiletData data = new ToiletData();
//
//                        if(type == XmlPullParser.START_TAG) {
//                            if(parser.getName().equals("col")) {
//                                if(parser.getAttributeValue(0).equals("dataValue"))  // "화장실명"
//                                    nameState = true;
//                                else if(parser.getAttributeValue(0).equals("posy"))  // 위도
//                                    latState = true;
//                                else if(parser.getAttributeValue(0).equals("posy"))  // 경도
//                                    longState = true;
//                            }
//                        }
//                        else if(type == XmlPullParser.END_TAG && parser.getName().equals("item"))  { // 음?
//                            data.setName(name);
//                            data.setLatitude(Double.valueOf(latitude));
//                            data.setLongitude(Double.valueOf(longitude));
//
//                            dataArr.add(data);
//                        }
//                        type = parser.next();
//                    }
//               } catch (MalformedURLException e) {
//                    e.printStackTrace();
//                } catch (XmlPullParserException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//        try {
//            thread.start();
//            thread.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        return dataArr;
//    }
//}

class MarkersData {
    String[] nameArr = new String[] {
            "미디어랩스관", "의료과학관", "신한은행 순천향대", "산학협력관", "공학관", "유니토피아관", "향설도서관", "인문사회과학관", "우체국 순천향대", "멀티미디어관", "순청향대 본관", "신창휴게소", "S-OIL", "오일뱅크", "신창역", "자연과학관", "앙뜨레프레너관", "향설3생활관", "향설2생활관", "글로벌빌리지", "학예관", "지역혁신관", "신창119 안전센터", "체육관", "학성사 1,2관", "생활관 3관"
    };

    Double[] latArr = new Double[] {
            36.77194401287416, 36.771097195642305, 36.770800394683654, 36.76997596563322, 36.76928145029192, 36.76917853057986, 36.768739986608594, 36.768386466552954, 36.768668761005046, 36.76915688220257, 36.76840550384599, 36.7689894066761, 36.77416575619874, 36.77764583534626, 36.76948881773688, 36.76960909240391, 36.76882748782919, 36.76809794715735, 36.76817844715358, 36.76730445085661, 36.76970168266969, 36.77056666806766, 36.77118913529503, 36.77060048851375, 36.77228294629353, 36.77144976544267
    };

    Double[] longArr = new Double[] {
            126.93174039112881, 126.93214717780299, 126.93314992431995, 126.93331585228043, 126.93215717830218, 126.93341174889667, 126.93077997541373, 126.92728570656408, 126.93228092511046, 126.93485681942323, 126.92896577871369, 126.92597465701579, 126.93260093224056, 126.93580164193797, 126.9506830513866, 126.92997273830555, 126.93411769418793, 126.93477915115766, 126.9336926278629, 126.93385018656058, 126.93431856048542, 126.93416100986067, 126.93543179182905, 126.93025746018971, 126.93353227708523, 126.93416585529629
    };

    public ArrayList<Markers> getData() {
        ArrayList<Markers> dataArr = new ArrayList<Markers>();
        for(int i = 0; i < nameArr.length; i++) {
            Markers marker = new Markers();
            marker.setName(nameArr[i]);
            marker.setLatitude(latArr[i]);
            marker.setLongitude(longArr[i]);
            dataArr.add(marker);
        }
        return dataArr;
    }
}

class Markers {
    // Marker 의 이름/경도/위도만 임의로 설정후 ArrayList 에 추가
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
}
