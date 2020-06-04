package com.b4.simonsays;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.b4.simonsays.mqtt.MessageListener;
import com.b4.simonsays.mqtt.MqttManager;
import com.b4.simonsays.mqtt.MqttSettings;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class WaitingFragment extends Fragment implements MessageListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MqttManager.getInstance().setMessageListener(this);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_waiting, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RotateAnimation rotateAnimation = new RotateAnimation(0f, 350f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setDuration(700);

        final ImageView loadingImageView = view.findViewById(R.id.iv_waiting);
        loadingImageView.startAnimation(rotateAnimation);
    }

    @Override
    public void onMessageArrived(MqttMessage message) {
        if (message.toString().equals(MqttSettings.ESP_READY_MESSAGE)) {
            Intent intent = new Intent(getContext(), GameActivity.class);
            startActivity(intent);
        }
    }
}