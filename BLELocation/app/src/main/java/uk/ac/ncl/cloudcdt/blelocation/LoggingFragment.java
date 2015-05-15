package uk.ac.ncl.cloudcdt.blelocation;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;

import static android.app.Activity.*;

public class LoggingFragment extends Fragment
{
	private final static String TAG = "LoggingFragment";

	private final static int REQUEST_ENABLE_BT = 1;

	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 5000;

	private BluetoothAdapter bluetoothAdapter;

	private BroadcastReceiver broadcastReceiver;

	/**
	 * Buttons responsible for scanning
	 */
	private Button startLoggingButton;
	private Button nextScanButton;
	private Button stopLoggingButton; // TODO: close file?

	private TextView console;

	private Context context;

	/**
	 * Background service which will perform our scans and
	 * log the data. (This means scans can also continue when
	 * device is sleeping).
	 */
	private BleLoggingService bleLoggingService;

	private AlertDialog scanDialog;

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_fingerprinting, null);
		context = getActivity();

		// Initialise our BLE logging service that can run in the background
		bleLoggingService = new BleLoggingService();

		// Activity console for logging useful output
		console = (TextView) view.findViewById(R.id.console);

		// Initialise alert dialog used for inputting grid square and scan info
		final View dialogView = inflater.inflate(R.layout.scan_settings_dialog, null);
		scanDialog = new AlertDialog.Builder(context)
			.setTitle("Scan settings for grid square")
			.setView(dialogView)
			.setPositiveButton(R.string.scan, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					startLoggingButton.setEnabled(false);
					nextScanButton.setEnabled(false);

					// get x & y coords and scan length values
					EditText xCoordView = (EditText) dialogView.findViewById(R.id.x_coord);
					EditText yCoordView = (EditText) dialogView.findViewById(R.id.y_coord);
					EditText scanLengthView = (EditText) dialogView.findViewById(R.id.scan_length);

					// TODO: input validation
					String x = xCoordView.getText().toString();
					String y = yCoordView.getText().toString();

					// convert coords to 2dp
					x = String.format("%.2f", Float.valueOf(x));
					y = String.format("%.2f", Float.valueOf(y));

					if(x.isEmpty() || y.isEmpty())
					{
						Toast toast = Toast.makeText(context, "Invalid coordinates.", Toast.LENGTH_LONG);
						toast.show();

						nextScanButton.setEnabled(true);

						return;
					}

					int scanLength = Integer.valueOf( scanLengthView.getText().toString() );

					// append to activity "console"
					DateTime dt = new DateTime();
					console.append(String.format(dt.toString("HH:mm:ss") + ": scanning %ds for co-ordinates (%s, %s).\n", scanLength, x, y));

					// start scanning for given coordinates
					//bleLoggingService.startMotionDetect(context); // TODO: make static?
					BleLoggingService.startActionScanAndLogFingerprint(context, scanLength, x, y);
				}
			})
			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					nextScanButton.setEnabled(true);
				}
			})
			.create();

		// perform BLE scan on button press
		startLoggingButton = (Button) view.findViewById(R.id.start);
		startLoggingButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// init log file
				BleLoggingService.startActionInitLogFile(context, true);
				BleLoggingService.finished = false;

				startLoggingButton.setEnabled(false);
				stopLoggingButton.setEnabled(true);

				// prompt user for first grid location code
				showScanDialog();
			}
		});

		// perform BLE scan on button press
		nextScanButton = (Button) view.findViewById(R.id.next_scan);
		nextScanButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// prompt user for first grid location code
				showScanDialog();
			}
		});

		// perform BLE scan on button press
		stopLoggingButton = (Button) view.findViewById(R.id.stop);
		stopLoggingButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// enable start button and disable stop
				startLoggingButton.setEnabled(true);
				stopLoggingButton.setEnabled(false);

				// clear console
				console.setText("");

				BleLoggingService.started = false;

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

				String action = intent.getAction();

				if(action.equals(BleLoggingService.ACTION_SCAN_AND_LOG_FINGERPRINT))
				{
					// start alarm/notification
					//sendNotification();

					// print to console
					DateTime dt = new DateTime();
					console.append(dt.toString("HH:mm:ss") + ": finished scan.\n");

					// allow user to start next scan
					nextScanButton.setEnabled(true);

					// prompt user for coords of next grid square
					showScanDialog();
				}
			}
		};

		return view;
	}

	@Override
	public void onStart()
	{
		super.onStart();
		LocalBroadcastManager.getInstance(context)
			.registerReceiver(broadcastReceiver, new IntentFilter(BleLoggingService.ACTION_SCAN_AND_LOG_FINGERPRINT));

		if(BleLoggingService.started && !BleLoggingService.scanning)
		{
			nextScanButton.setEnabled(true);

			showScanDialog();

			// TODO: update console correct timestamp
		}
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

	private void showScanDialog()
	{
		scanDialog.show();
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
