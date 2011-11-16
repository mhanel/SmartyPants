package com.smartypants;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;
import com.smartypants.R;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;


public class SmartyPantsActivity extends Activity implements Runnable, OnClickListener{
	private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";

	protected static final String TAG = "hello android";

	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	
	private TextView debugtext = null;
	private Button buttonStart = null;
	private TextView textStatus = null;
	private TextView textColor = null;
	private ProgressBar progressBar = null;
	
	private String whatColor = "";

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
        textStatus = (TextView) findViewById(R.id.textStatus);
        textColor = (TextView) findViewById(R.id.textColor);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
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
				startColorChanging();				
			} else if (buttonStart.getTag() == "started") {
				buttonStart.setText("Start game!");
				buttonStart.setTag("stopped");
				Log.i(TAG, "Game stopped!");
				mHandler.removeCallbacks(mWaitRunnable);
				textColor.setText("COLOR");
				textColor.setTextColor(Color.BLACK);
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
	
	private Handler mHandler = new Handler();
	private Runnable mWaitRunnable = new Runnable() {
	    public void run() {
	    	Random rnd = new Random();
	    	int i = rnd.nextInt(3);
	    	if(i == 0) {
	    		textColor.setText("RED");
	    	}
	    	else if(i == 1) {
	    		textColor.setText("BLUE");
	    	}
	    	else if(i == 2) {
	    		textColor.setText("GREEN");
	    	}
	    	int j = rnd.nextInt(3);
	    	if(j == 0) {
	    		textColor.setTextColor(Color.RED);
	    		whatColor = "RED";
	    	}
	    	else if(j == 1) {
	    		textColor.setTextColor(Color.BLUE);
	    		whatColor = "BLUE";
	    	}
	    	else if(j == 2) {
	    		textColor.setTextColor(Color.GREEN);
	    		whatColor = "GREEN";
	    	}
	    	
	    	mHandler.postDelayed(mWaitRunnable, 3000);
	    }
	};
	
	private void startColorChanging() {
		mHandler.postDelayed(mWaitRunnable, 3000);
	}
	
	private void handleInput(String in) {
		if(in.equals(whatColor)) {
			int i = progressBar.getProgress();
			i += 1;
			progressBar.setProgress(i);
		}
		else {
			int i = progressBar.getProgress();
			i -= 1;
			progressBar.setProgress(i);
		}
		if(progressBar.getProgress() < 250) {
			textStatus.setText("Hohlbirne");
		}
		else if(progressBar.getProgress() >= 250 && progressBar.getProgress() < 500) {
			textStatus.setText("Schwammerl");
		}
		else if(progressBar.getProgress() >= 500 && progressBar.getProgress() < 750) {
			textStatus.setText("Birne");
		}
		else if(progressBar.getProgress() >= 750) {
			textStatus.setText("Smartbirne");
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

	}
	
	// Instantiating the Handler associated with the main thread.
	private Handler messageHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {  
			switch(msg.what) {
				case MESSAGE_BUTTON_PRESSED:
					try {  
			        	byte load = (Byte) msg.obj; 
			        	
			        	// handle input and send color string to handleInput
			        	if (load == 13) {
			        		handleInput("RED");
			        		debugtext.setText("RED button pressed.");
			        	} else if (load == 8) {
			        		handleInput("GREEN");
			        		debugtext.setText("GREEN button pressed.");
			        	} else if (load == 4) {
			        		handleInput("BLUE");
			        		debugtext.setText("BLUE button pressed.");
			        	}
		            } catch (Exception e) {
		            }
		            break;
			}
		}
	};
}