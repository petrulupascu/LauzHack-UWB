/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.hefr.etu.zoutao_wen.tangoapplication;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;


/**
 * Queries the user for a new udp_ip
 */
public class SetUdpIpDFragment extends DialogFragment {//just a frame that create two methods in the interface to let be accomplish

    private static final String TAG = "coucou";
    EditText mUdpIpEditText;
    CallbackListener mCallbackListener;
    Button mOkButton;
    Button mCancelButton;

    interface CallbackListener {
        void onSetUdpIpOk(String new_udp_ip);
        void onSetUdpIpCancelled();
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbackListener = (CallbackListener) activity;
    }


    @Override
    public View onCreateView(LayoutInflater inflator, ViewGroup container,
                             Bundle savedInstanceState) {
        View dialogView = inflator.inflate(R.layout.set_ip_dialog, container, false);

        mUdpIpEditText = (EditText) dialogView.findViewById(R.id.name_set_ip);
        mUdpIpEditText.setText(getResources().getString(R.string.udp_ip_saved));
        setCancelable(false);

        // we want the numeric keyboard
        mUdpIpEditText.setRawInputType(Configuration.KEYBOARD_QWERTY);
        // auto focus hack. (Automatically show the keyboard)
        (new Handler()).postDelayed(new Runnable() {
            public void run() {
                // It simulate a click on the editbox
                mUdpIpEditText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN , 0, 0, 0));
                mUdpIpEditText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
                // moves the cursor to the end of the text.
                mUdpIpEditText.setSelection(mUdpIpEditText.getText().length());
            }
        }, 50);



        mOkButton = (Button) dialogView.findViewById(R.id.ok_set_ip);
        // what to do when OK is clicked
        mOkButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // we send the new ip to the callback
                String new_udp_ip = mUdpIpEditText.getText().toString();
                mCallbackListener.onSetUdpIpOk(new_udp_ip);
                dismiss();
            }
        });
        mCancelButton = (Button) dialogView.findViewById(R.id.cancel_set_ip);
        mCancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallbackListener.onSetUdpIpCancelled();
                dismiss();
            }
        });


        return dialogView;
    }
}
