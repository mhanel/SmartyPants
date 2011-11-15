package com.smartypants;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;
import com.smartypants.R;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


public class SmartyPantsActivity extends Activity implements Runnable, OnClickListener{
	private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";

	protected static final String TAG = "hello android";

	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	
	private TextView debugtext = null;
	private Button buttonStart = null;

	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;
	
	private static final int MESSAGE_BUTTON_PRESSED = 1;
	
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory " + accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	mUsbManager = UsbManager.getInstance(this);
    	mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
    	IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    	filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
    	registerReceiver(mUsbReceiver, filter);

    	if (getLastNonConfigurationInstance() != null) {
    		mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
    		openAccessory(mAccessory);
    	}

    	setContentView(R.layout.main);
    		
        debugtext = (TextView) findViewById(R.id.debugfield);
        buttonStart = (Button) findViewById(R.id.buttonStart); 

        buttonStart.setTag("stopped");
        buttonStart.setOnClickListener(startListener);
    }
    
    /** Implement OnClickListener for start button */
    private OnClickListener startListener = new OnClickListener() {
		public void onClick(View v) {
			if (buttonStart.getTag() == "stopped") {
				buttonStart.setText("Stop game!");
				buttonStart.setTag("started");
				Log.i(TAG, "Game started!");
			} else if (buttonStart.getTag() == "started") {
				buttonStart.setText("Start game!");
				buttonStart.setTag("stopped");
				Log.i(TAG, "Game stopped!");
			}
		}
    };
    
	@Override
	public void onResume() {
		super.onResume();

		if (mInputStream != null && mOutputStream != null) {
			return;
		}

		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory, mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null");
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		closeAccessory();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}
    
    private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Thread thread = new Thread(null, this, "DemoKit");
			thread.start();
			Log.d(TAG, "accessory opened");
			//enableControls(true);
		} else {
			Log.d(TAG, "accessory open fail");
		}
	}
    
    
	public void run() {	
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		while (ret >= 0) {
			try {
				ret = mInputStream.read(buffer); // this will be always positive, as long as the stream is not closed
			} catch (IOException e) {
				break;
			}

			i = 0;
			while (i < ret) {
				Message m = Message.obtain(messageHandler, MESSAGE_BUTTON_PRESSED);
				m.obj = buffer[i];
				messageHandler.sendMessage(m);
				i++;
			}
		}
	}

	private void closeAccessory() {
		//enableControls(false);

		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}
	
	public void sendCommand(byte message) {
		if (message > 255)
			message = (byte) 255;

		if (mOutputStream != null && message != 0) {
			try {
				mOutputStream.write(message);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}

	@Override
	public void onClick(View v) {
		// Send some message to Arduino board, e.g. "13"
//		sendCommand((byte)13);
	}
	
	// Instantiating the Handler associated with the main thread.
	private Handler messageHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {  
			switch(msg.what) {
				case MESSAGE_BUTTON_PRESSED:
					try {  
			        	byte load = (Byte) msg.obj; 
			        	debugtext.setText("Received message: " + String.valueOf(load));
		            } catch (Exception e) {
		            }
		            break;
			}
		}
	};
}