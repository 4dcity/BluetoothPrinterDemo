package com.will.bluetoothprinterdemo.ui;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.will.bluetoothprinterdemo.utils.BluetoothUtil;

import java.io.IOException;

public abstract class BasePrintActivity extends AppCompatActivity {

    String tag = getClass().getSimpleName();
    private BluetoothSocket mSocket;
    private BluetoothStateReceiver mBluetoothStateReceiver;
    private AsyncTask mConnectTask;
    private ProgressDialog mProgressDialog;

    /**
     * 蓝牙连接成功后回调，该方法在子线程执行，可执行耗时操作
     */
    public abstract void onConnected(BluetoothSocket socket, int taskType);


    /**
     * 蓝牙状态发生变化时回调
     */
    public void onBluetoothStateChanged(Intent intent) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initReceiver();
    }

    @Override
    protected void onStop() {
        cancelConnectTask();
        closeSocket();
        super.onStop();
    }

    protected void closeSocket() {
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                mSocket = null;
                e.printStackTrace();
            }
        }
    }

    protected void cancelConnectTask() {
        if (mConnectTask != null) {
            mConnectTask.cancel(true);
            mConnectTask = null;
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mBluetoothStateReceiver);
        super.onDestroy();
    }

    private void initReceiver() {
        mBluetoothStateReceiver = new BluetoothStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothStateReceiver, filter);
    }

    /**
     * 检查蓝牙状态，如果已打开，则查找已绑定设备
     *
     * @return
     */
    public boolean checkBluetoothState() {
        if (BluetoothUtil.isBluetoothOn()) {
            return true;
        } else {
            BluetoothUtil.openBluetooth(this);
            return false;
        }
    }

    public void connectDevice(BluetoothDevice device, int taskType) {
        if (checkBluetoothState() && device != null) {
            mConnectTask = new ConnectBluetoothTask(taskType).execute(device);
        }
    }


    class ConnectBluetoothTask extends AsyncTask<BluetoothDevice, Integer, BluetoothSocket> {

        int mTaskType;

        public ConnectBluetoothTask(int taskType) {
            this.mTaskType = taskType;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog("请稍候...");
            super.onPreExecute();
        }

        @Override
        protected BluetoothSocket doInBackground(BluetoothDevice... params) {
            if(mSocket != null){
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mSocket = BluetoothUtil.connectDevice(params[0]);;
            onConnected(mSocket, mTaskType);
            return mSocket;
        }

        @Override
        protected void onPostExecute(BluetoothSocket socket) {
            mProgressDialog.dismiss();
            if (socket == null || !socket.isConnected()) {
                toast("连接打印机失败");
            } else {
                toast("成功！");
            }

            super.onPostExecute(socket);
        }
    }


    protected void showProgressDialog(String message) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.setMessage(message);
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    protected void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 监听蓝牙状态变化的系统广播
     */
    class BluetoothStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            switch (state) {
                case BluetoothAdapter.STATE_TURNING_ON:
                    toast("蓝牙已开启");
                    break;

                case BluetoothAdapter.STATE_TURNING_OFF:
                    toast("蓝牙已关闭");
                    break;
            }
            onBluetoothStateChanged(intent);
        }
    }

}