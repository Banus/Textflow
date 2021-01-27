package com.example.googlemap;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tapwithus.sdk.TapListener;
import com.tapwithus.sdk.TapSdk;
import com.tapwithus.sdk.TapSdkFactory;
import com.tapwithus.sdk.airmouse.AirMousePacket;
import com.tapwithus.sdk.mouse.MousePacket;

public class TapController implements TapListener {

    private static final String TAG = "TapListener";
    private static final boolean DEMO_MODE = false;

    private TapSdk tap;
    private Talker talker;

    private static Talker.Command[] commands;
    private static  Talker.Command[] commands_step = {
            Talker.Command.SELECT,       // THUMB
            Talker.Command.NEXT,         // INDEX
            Talker.Command.BACK,         // MIDDLE
            Talker.Command.CANCEL,       // RING
            Talker.Command.SYS_CANCEL    // PINKY
    };
    private static  Talker.Command[] commands_flow = {
            Talker.Command.SELECT,       // THUMB
            Talker.Command.NONE,         // INDEX
            Talker.Command.NONE,         // MIDDLE
            Talker.Command.CANCEL,       // RING
            Talker.Command.SYS_CANCEL    // PINKY
    };

    TapController(Context context, Talker talker) {
        tap = TapSdkFactory.getDefault(context);
        tap.registerTapListener(this);
        this.talker = talker;
        commands = talker.USE_FLOW ? commands_flow : commands_step;
    }

    @Override
    public void onBluetoothTurnedOn() {
        String tap_id = tap.getConnectedTaps().iterator().next();
        tap.startMode(tap_id, TapSdk.MODE_CONTROLLER);
    }

    @Override
    public void onBluetoothTurnedOff() {

    }

    @Override
    public void onTapStartConnecting(@NonNull String tapIdentifier) {

    }

    @Override
    public void onTapConnected(@NonNull String tapIdentifier) {
        if (DEMO_MODE) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        talker.cycle();
                    } catch (Exception ex) {
                        Log.e(TAG, ex.toString());
                    }
                }
            };
            thread.start();
        }
    }

    @Override
    public void onTapDisconnected(@NonNull String tapIdentifier) {

    }

    @Override
    public void onTapResumed(@NonNull String tapIdentifier) {
    }

    @Override
    public void onTapChanged(@NonNull String tapIdentifier) {
    }

    @Override
    public void onControllerModeStarted(@NonNull String tapIdentifier) {

    }

    @Override
    public void onTextModeStarted(@NonNull String tapIdentifier) {

    }

    @Override
    public void onTapInputReceived(@NonNull String tapIdentifier, int data) {
        boolean[] fingers = TapSdk.toFingers(data);
        Log.d(TAG, java.util.Arrays.toString(fingers));

        for(int i = 0; i < fingers.length; ++i) {
            if(fingers[i] && commands[i] != Talker.Command.NONE) {
                talker.command(commands[i]);
                break;
            }
        }
    }

    @Override
    public void onMouseInputReceived(@NonNull String tapIdentifier, @NonNull MousePacket data) {

    }

    @Override
    public void onAirMouseInputReceived(@NonNull String tapIdentifier, @NonNull AirMousePacket data) {

    }

    @Override
    public void onError(@NonNull String tapIdentifier, int code, @NonNull String description) {

    }
}
