package com.b4.simonsays;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.fragment.NavHostFragment;

import com.b4.simonsays.mqtt.MqttManager;
import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

import static android.Manifest.permission.CAMERA;

public class StartFragment extends Fragment implements ZXingScannerView.ResultHandler {

    private ZXingScannerView scannerView;
    private Button scanButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView imageView = view.findViewById(R.id.animationImage);
        AnimationDrawable startAnimation = (AnimationDrawable) imageView.getBackground();
        startAnimation.run();

        //handleResult(new Result("Shining Saphires", null, null, null));
        scanButton = view.findViewById(R.id.scan_button);

        scannerView = view.findViewById(R.id.scannerView);
        scannerView.setVisibility(View.GONE);
        scanButton.setVisibility(View.VISIBLE);

        scanButton.setOnClickListener(view1 -> startScannerView());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void startScannerView() {
        if (!checkPermission()) {
            requestPermission();
        }

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        scannerView.setVisibility(View.VISIBLE);
        scanButton.setVisibility(View.GONE);
        scannerView.setResultHandler(this);
        scannerView.startCamera();
    }

    //checks if ScannerActivity has permission to use camera
    private boolean checkPermission() {
        Context context = getContext();
        if (context != null) {
            return (ContextCompat.checkSelfPermission(context, CAMERA) == PackageManager.PERMISSION_GRANTED);
        }

        return false;
    }

    private void requestPermission() {
        Activity activity = getActivity();
        if (activity != null) {
            ActivityCompat.requestPermissions(activity, new String[]{CAMERA}, 3);
        }
    }

    @Override
    public void handleResult(Result result) {
        if (result.getText().equals("Shining Saphires")) {

            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

            // Connect to MQTT broker:
            MqttManager.getInstance().connect(this.getContext());

            // Navigate to WaitingFragment
            NavHostFragment.findNavController(StartFragment.this)
                    .navigate(R.id.action_StartFragment_to_WaitingFragment);

        } else {
            Toast.makeText(getContext(), R.string.unrecognized_qr_code, Toast.LENGTH_SHORT).show();
            scannerView.setVisibility(View.VISIBLE);
            scannerView.setResultHandler(this);
            scannerView.startCamera();
        }
    }
}