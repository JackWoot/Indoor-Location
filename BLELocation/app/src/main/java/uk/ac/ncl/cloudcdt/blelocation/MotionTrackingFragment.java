package uk.ac.ncl.cloudcdt.blelocation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.joda.time.DateTime;

import static android.app.Activity.*;

public class MotionTrackingFragment extends Fragment
{
	private final static String TAG = "MotionLoggingFragment";

	private final static int REQUEST_ENABLE_BT = 1;

	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 5000;

	private BluetoothAdapter bluetoothAdapter;

	private BroadcastReceiver broadcastReceiver;

	/**
	 * Buttons responsible for scanning
	 */
	private Button startLoggingButton;
	private Button stopLoggingButton; // TODO: close file?

	private TextView console;

	private Context context;

	/**
	 * Background service which will perform our scans and
	 * log the data. (This means scans can also continue when
	 * device is sleeping).
	 */
	private MotionTrackingService motionTrackingService;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_motion_tracking, null);
		context = getActivity();

		// Initialise our BLE logging service that can run in the background
		motionTrackingService = new MotionTrackingService();
		MotionTrackingService.context = context;

		// Activity console for logging useful output
		console = (TextView) view.findViewById(R.id.console);

		// perform BLE scan on button press
		startLoggingButton = (Button) view.findViewById(R.id.start);
		startLoggingButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// init log file
				//BleLoggingService.startActionInitLogFile(context, false);
				MotionTrackingService.startActionInitLogFile(context);

				startLoggingButton.setEnabled(false);
				stopLoggingButton.setEnabled(true);

				//BleLoggingService.startActionScanAndLogMotion(context);
				MotionTrackingService.startActionLogMotion(context);
			}
		});

		// perform BLE scan on button press
		stopLoggingButton = (Button) view.findViewById(R.id.stop);
		stopLoggingButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				BleLoggingService.finished = true;
				MotionTrackingService.stopped = true;

				// enable start button and disable stop
				startLoggingButton.setEnabled(true);
				stopLoggingButton.setEnabled(false);

				// clear console
				console.setText("");

				// close file?
			}
		});

		// Initialise Bluetooth adapter/scanner
		final BluetoothManager bluetoothManager =
			(BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();

		// Ensures Bluetooth is available on the device and it is enabled. If not,
		// displays a dialog requesting user permission to enable Bluetooth.
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
		{
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		else
		{
			// Bluetooth is switched on, enable scan button
			startLoggingButton.setEnabled(true);
		}

		// Initialise broadcast receiver (for receiving messages from BLE logging service)
		broadcastReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				// TODO: error handling?

				// start alarm/notification
				//sendNotification();

				// print to console
				DateTime dt = new DateTime();
				console.append(dt.toString("HH:mm:ss") + ": finished scan.\n");

				//bleLoggingService.stopMotionDetect();
			}
		};

		return view;
	}

	@Override
	public void onStart()
	{
		super.onStart();
		LocalBroadcastManager.getInstance(context)
			.registerReceiver(broadcastReceiver, new IntentFilter(MotionTrackingService.ACTION_LOG_MOTION));

//		if(MotionTrackingService.started && !MotionTrackingService.scanning)
//		{
//			// TODO: update console correct timestamp
//		}
	}

	@Override
	public void onStop()
	{
		LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
		super.onStop();
	}

	/**
	 * If user turns on their bluetooth, enable the scan button,
	 * otherwise leave disabled.
	 *
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch(requestCode)
		{
			case REQUEST_ENABLE_BT:
				Log.d(TAG, String.valueOf(resultCode));

				if(resultCode == RESULT_OK)
				{
					// enable fragments to start performing their required functions
					startLoggingButton.setEnabled(true);
				}
				break;
		}
	}

	/**
	 * Initialise the buttons and their event listeners for this Activity
	 */
	private void initButtons()
	{
		View view = getView();


		// stop scan button. flush data in buffer to file.
		/*finishedLoggingButton = (Button) findViewById(R.id.stop_button);
		finishedLoggingButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{

			}
		});*/
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if(id == R.id.action_settings)
		{
			return true;
		}

		return super.onOptionsItemSelected(item);
	}


}
