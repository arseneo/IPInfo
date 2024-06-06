package com.mirea.kt.ipinfo;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class IPInfoFragment extends Fragment {

    private EditText ipInput;
    private TextView ipInfo, currentIPTextView;
    private Button searchButton, shareButton;
    private double latitude;
    private double longitude;

    private static final String TAG = "IPInfoFragment";

    private OkHttpClient client = new OkHttpClient();
    private Executor executor = Executors.newFixedThreadPool(2);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ip_info, container, false);

        ipInput = view.findViewById(R.id.ip_input);
        ipInfo = view.findViewById(R.id.ip_info);
        currentIPTextView = view.findViewById(R.id.current_ip);
        searchButton = view.findViewById(R.id.search_button);
        shareButton = view.findViewById(R.id.share_button);

        fetchCurrentIP();

        currentIPTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentIP = currentIPTextView.getText().toString().replace("Текущий IP: ", "").trim();
                fetchIPInfo(currentIP);
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = ipInput.getText().toString().trim();
                if (!ip.isEmpty()) {
                    fetchIPInfo(ip);
                } else {
                    Toast.makeText(getContext(), "Введите IP адрес", Toast.LENGTH_SHORT).show();
                }
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String info = ipInfo.getText().toString();
                if (!info.isEmpty()) {
                    shareIPInfo(info);
                } else {
                    Toast.makeText(getContext(), "Пока нечем делиться", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    private void fetchCurrentIP() {
        String url = "https://api.ipify.org?format=json";
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Ошибка получения текущего IP: " + e.getMessage());
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Ошибка получения текущего IP:", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        String currentIP = jsonObject.optString("ip");
                        getActivity().runOnUiThread(() -> {
                            currentIPTextView.setText("Текущий IP: " + currentIP);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка обработки текущего IP:", e);
                    }
                } else {
                    Log.e(TAG, "Ошибка обработки текущего IP: " + response.message());
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Ошибка обработки текущего IP:", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void fetchIPInfo(String ip) {
        if (!isNetworkAvailable()) {
            Toast.makeText(getContext(), "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
            return;
        }

        ipInfo.setText("");

        String url1 = "https://ipinfo.io/" + ip + "/json";
        //String url2 = "https://ipapi.co/" + ip + "/json/";
        String url3 = "https://api.ipgeolocation.io/ipgeo?apiKey=da61f66a8e354306b77aebda46013adc&ip=" + ip;

        executor.execute(() -> fetchFromUrl(url1, "Источник ipinfo.io"));
        //executor.execute(() -> fetchFromUrl(url2, "Источник ipapi.co"));
        executor.execute(() -> fetchFromUrl(url3, "Источник ipgeolocation.io"));
    }

    private void fetchFromUrl(String url, String source) {
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Ошибка получения информации от: " + source + ": " + e.getMessage());
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Ошибка получения информации от: " + source, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    getActivity().runOnUiThread(() -> processIPInfo(responseBody, source));
                } else {
                    Log.e(TAG, "Ошибка получения информации от: " + source + ": " + response.message());
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Ошибка получения информации от: " + source, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void processIPInfo(String response, String source) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            StringBuilder info = new StringBuilder();
            info.append("\n").append(source).append(":\n");
            info.append("IP: ").append(jsonObject.optString("ip")).append("\n");
            if (jsonObject.has("country")){
                info.append("Код страны: ").append(jsonObject.optString("country")).append("\n");
            }
            if (jsonObject.has("country_name")){
                info.append("Cтрана: ").append(jsonObject.optString("country_name")).append("\n");
            }
            if (jsonObject.has("region")){
                info.append("Регион: ").append(jsonObject.optString("region")).append("\n");
            }
            info.append("Город: ").append(jsonObject.optString("city")).append("\n");
            if (jsonObject.has("org")){
                info.append("Провайдер: ").append(jsonObject.optString("org")).append("\n");
            }
            if (jsonObject.has("organization")){
                info.append("Провайдер: ").append(jsonObject.optString("organization")).append("\n");
            }
            if (jsonObject.has("latitude") && jsonObject.has("longitude")) {
                latitude = jsonObject.optDouble("latitude");
                longitude = jsonObject.optDouble("longitude");
                info.append("Широта: ").append(latitude).append("\n");
                info.append("Долгота: ").append(longitude).append("\n");

                getActivity().runOnUiThread(() -> {
                    ipInfo.setOnClickListener(v -> openMap(latitude, longitude));
                });
            }

            getActivity().runOnUiThread(() -> {
                ipInfo.append(info.toString());
                Log.d(TAG, "Информация об IP (" + source + "): " + info.toString());
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка обработки информации об IP", e);
        }
    }

    private void openMap(double latitude, double longitude) {
        Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        if (mapIntent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(getContext(), "No map application found", Toast.LENGTH_SHORT).show();
        }
    }





    private void shareIPInfo(String info) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, info);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Поделиться информацией об IP"));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
