package uk.ac.ncl.cloudcdt.blelocation;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Utils;
import com.estimote.sdk.connection.BeaconConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static android.bluetooth.BluetoothAdapter.LeScanCallback;
import static com.estimote.sdk.connection.BeaconConnection.BeaconCharacteristics;
import static com.estimote.sdk.connection.BeaconConnection.ConnectionCallback;
import static com.estimote.sdk.connection.BeaconConnection.WriteCallback;


public class BeaconManagerFragment extends Fragment
{
	public static final String TAG = "BeaconManagerFragment";

	private Context context;
	private BluetoothAdapter bluetoothAdapter;
	private BeaconConnection beaconConnection;

	private boolean scanning = false;

	/**
	 * Data structures for holding scan data.
	 */
	private HashSet<String> discovered;
	private List<HashMap<String, String>> data;
	private HashMap<String, HashMap<String, Integer>> becaonChars;

	/**
	 * ListView member variables.
	 */
	private SimpleAdapter adapter;
	private ListView listView;

	/**
	 * Button for starting the scanning process.
	 */
	private Button scanButton;
	private ProgressDialog progressDialog;

	public static final int SCAN_LENGTH = 60;

	public BeaconManagerFragment()
	{
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// inflate the fragment layout xml file
		View view = inflater.inflate(R.layout.fragment_beacon_manager, container, false);
		context = getActivity();

		progressDialog = new ProgressDialog(context);

		// init scan button that begins the scanning process when clicked
		scanButton = (Button) view.findViewById(R.id.scan_button);
		scanButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				data.clear();
				listView.invalidateViews();
				progressDialog.setMessage("Scanning for beacons...");
				progressDialog.setCancelable(false);
				progressDialog.show();
				scan(SCAN_LENGTH);
			}
		});

		// initialise our data structures for holding scan data
		data = new ArrayList<>();
		discovered = new HashSet<>();
		becaonChars = new HashMap<>();

		// init list view for displaying scan data
		listView = (ListView) view.findViewById(R.id.list);

		// when an item is clicked, prompt user with alert dialog to
		// take input for writing beacon settings.
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				HashMap<String, String> beaconMap = data.get(position);
				final String address = beaconMap.get("address");
				final String advertiseRate = beaconMap.get("advertise");
				final String broadcastPower = beaconMap.get("power");

				// open options dialog for beacon
				promptBeaconSettings(address);
			}
		});

		// list view uses android simple_list_item_2 layout which has 2 TextViews;
		// a larger and bold top line and a smaller bottom one.
		String[] cols = { "address", "data" };
		adapter = new SimpleAdapter(context, data, android.R.layout.simple_list_item_2, cols,
			new int[] { android.R.id.text1, android.R.id.text2 });
		listView.setAdapter(adapter);

		// Initialise Bluetooth adapter/scanner
		final BluetoothManager bluetoothManager =
			(BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();

		return view;
	}

	/**
	 * Prompt the user with an AlertDialog that takes
	 * input for the settings to write to the beacon.
	 *
	 * TODO: error handling
	 *
	 * @param address - mac address of the estimote beacon.
	 */
	private void promptBeaconSettings(final String address)
	{
		// inflate the layout that the alert dialog should use
		LayoutInflater inflater = LayoutInflater.from(context);
		final View view = inflater.inflate(R.layout.dialog_beacon_settings, null);

		// init input views
		final EditText advertiseRateView = (EditText) view.findViewById(R.id.advertise_rate);
		final EditText powerView = (EditText) view.findViewById(R.id.broadcast_power);

		// get current beacon characteristics (which were fetched during scan)
		int currentAdvertiseRate = becaonChars.get(address).get("advertise");
		int currentPower = becaonChars.get(address).get("power");

		// set text to current beacon setting values
		advertiseRateView.setText(String.valueOf(currentAdvertiseRate));
		powerView.setText(String.valueOf(currentPower));

		// build and show the dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		AlertDialog dialog = builder.setTitle(R.string.beacon_options)
			.setView(view)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					// get the inputted values from the views
					int newAdvertiseRate = Integer.valueOf(advertiseRateView.getText().toString());
					int newPower = Integer.valueOf(powerView.getText().toString());

					// TODO: error handling

					Log.i(TAG, "Writing to beacon " + address + " " + newAdvertiseRate + " " + newPower);

					// write the beacon settings
					setBeaconSettings(address, newAdvertiseRate, newPower);
				}
			})
			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{

				}
			})
			.create();

		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		dialog.show();
	}

	/**
	 * Set the estimote beacon's settings with the supplied arguments.
	 * First we must do a regular scan, and once the beacon with the
	 * supplied MAC address is found, we can connect to it and write
	 * the new values.
	 *
	 * // TODO: error handling if scan is already active?
	 * // TODO: input validation
	 *
	 * @param address - mac address of the beacon.
	 * @param advertiseRate - advertise rate of the beacon (in millis).
	 * @param power - broadcasting power of the beacon.
	 */
	private void setBeaconSettings(final String address, final int advertiseRate, final int power)
	{
		progressDialog.setMessage("Modifying beacon settings...");
		progressDialog.show();

		final Handler toastHandler = new Handler();

		final LeScanCallback leScanCallback = new LeScanCallback()
		{
			boolean discovered = false;

			@Override
			public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
			{

				// discovered device has the mac address we want
				if(!discovered && device.getAddress().equals(address))
				{
					discovered = true;

					// init estimote SDK Beacon object from scan data
					Beacon beacon = Utils.beaconFromLeScan(device, rssi, scanRecord);

					// create a new beacon connection object that writes our new settings to it
					// once successfully connected.
					beaconConnection = new BeaconConnection(context, beacon, new ConnectionCallback()
					{
						boolean advertiseChangeSuccess = false;
						boolean powerChangeSuccess = false;

						// string res id of toast message to display on connection close
						int stringID;

						@Override
						public void onAuthenticated(BeaconCharacteristics beaconCharacteristics)
						{
							beaconConnection.writeAdvertisingInterval(advertiseRate, new WriteCallback()
							{
								@Override
								public void onSuccess()
								{
									Log.i(TAG, "Successfully wrote advertising rate to beacon: " + address);

									advertiseChangeSuccess = true;
								}

								@Override
								public void onError()
								{
									Log.e(TAG, "Error: could not write advertising rate to beacon: " + address);
								}
							});

							beaconConnection.writeBroadcastingPower(power, new WriteCallback()
							{
								@Override
								public void onSuccess()
								{
									Log.i(TAG, "Successfully wrote broadcasting rate to beacon: " + address);

									powerChangeSuccess = true;
								}

								@Override
								public void onError()
								{
									Log.e(TAG, "Error: could not write broadcasting power to beacon: " + address);
								}
							});
						}

						@Override
						public void onAuthenticationError()
						{
							Log.e(TAG, "Error: could not authenticate with beacon: " + address + " (on write settings)");
						}

						/**
						 * On disconnect from beacon, display a toast notifying success/failure
						 * of the settings write.
						 */
						@Override
						public void onDisconnected()
						{
							if(advertiseChangeSuccess && powerChangeSuccess)
							{
								// both success
								stringID = R.string.modify_beacon_settings_success;
							}
							else if(!advertiseChangeSuccess && powerChangeSuccess)
							{
								// advertise change failure
								stringID = R.string.modify_beacon_settings_advertise_failure;
							}
							else if(advertiseChangeSuccess && !powerChangeSuccess)
							{
								// power change failure
								stringID = R.string.modify_beacon_settings_power_failure;
							}
							else
							{
								// both change failure
								stringID = R.string.modify_beacon_settings_both_failure;
							}

							// Display toast to the user
							toastHandler.post(new Runnable()
							{
								@Override
								public void run()
								{
									Toast toast = Toast.makeText(context, stringID, Toast.LENGTH_SHORT);
									toast.show();
								}
							});
						}
					});

					// start the connection
					beaconConnection.authenticate();

					// close the connect 5 seconds later TODO: make this a constant/user defined value
					//									TODO: alternatively, keep attempting connection until success
					Handler handler = new Handler();
					handler.postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							Log.i(TAG, "CLOSING BEACON CONNECTION");
							beaconConnection.close();
							progressDialog.dismiss();
						}
					}, 20 * 1000);
				}
			}
		};

		// start a regular BLE scan
		bluetoothAdapter.startLeScan(leScanCallback);

		scanning = true;

		// stop scan after 5 seconds TODO: make this a constant/user defined value
		Handler handler = new Handler();
		handler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				bluetoothAdapter.stopLeScan(leScanCallback);
				scanning = false;

				Log.i(TAG, "FINISHED SCANNING");

			}
		}, 20 * 1000);
	}

	/**
	 * Perform a BLE scan. On each discovery, connect to beacon
	 * and poll its RSSI (using scanCallback above).
	 *
	 * TODO: fix when progress dialog is hidden. must be done after EVERY beacon connection has been closed.
	 * (probably easiest to do it by delaying by (scan length + max connection length) and then dismissing)
	 */
	private void scan(int length)
	{
		// Ensures Bluetooth is available on the device and it is enabled.
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
		{
			Toast toast = Toast.makeText(context,
				"Error: bluetooth is not enabled.", Toast.LENGTH_LONG);
			toast.show();
			return;
		}

		final Handler updateUI = new Handler();

		// when device is discovered, add info to our data structures for
		// displaying in our ListView. only necessary to do once per device
		final LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback()
		{
			@Override
			public void onLeScan(BluetoothDevice device, final int rssi, byte[] scanRecord)
			{

				Log.i(TAG, "Discovered: " + device);

				final String address = device.getAddress();

				// if this device has already been recorded during this scan
				// then return.
				if(discovered.contains(address))
				{
					return;
				}


				// estimate distance using estimote SDK, and fetch power level of the received packet
				Beacon beacon = Utils.beaconFromLeScan(device, rssi, scanRecord);

				if(beacon == null)
				{
					return;
				}

				// otherwise, add it to the set of already discovered devices.
				discovered.add(address);

				final double distanceEstimate = Utils.computeAccuracy(beacon);
				final int powerLevel = beacon.getMeasuredPower();

				//Log.i(TAG, address + ": " + rssi);

				// now we must connect to the beacon to read its currently defined broadcast power
				// and advertising rate settings.
				final BeaconConnection beaconConnection =
					new BeaconConnection(context, beacon, new ConnectionCallback()
					{
						@Override
						public void onAuthenticated(BeaconCharacteristics beaconCharacteristics)
						{
							// read the setting values
							int currentAdvertise = beaconCharacteristics.getAdvertisingIntervalMillis();
							int currentPower = beaconCharacteristics.getBroadcastingPower();

							System.out.println("BEACON SETTINGS: " + currentAdvertise + " " + currentPower);

							// formulate all the beacon data into a human readable string that we
							// will display to the user in the ListView.
							String dataStr = String.format("distance estimate: %.2f\nrssi: %d\nadvertise: %d\ntransmission power: %d",
								distanceEstimate, rssi, currentAdvertise, powerLevel);

							// add beacon entry to the data structure (this will be displayed to the
							// user) in the ListView.
							HashMap<String, String> entryMap = new HashMap<>();
							entryMap.put("address", address);
							entryMap.put("data", dataStr);
							data.add(entryMap);

							// save current beacon setting values which we can display to the
							// user as default values in the 'modify settings' dialog.
							HashMap<String, Integer> beaconCharEntry = new HashMap<>();
							beaconCharEntry.put("advertise", currentAdvertise);
							beaconCharEntry.put("power", currentPower);
							becaonChars.put(address, beaconCharEntry);

							updateUI.post(new Runnable()
							{
								@Override
								public void run()
								{
									listView.invalidateViews();
								}
							});
						}

						@Override
						public void onAuthenticationError()
						{
							Log.e(TAG, "Error: could not authenticate with beacon: " + address + " (after discovery)");
							discovered.remove(address);
						}

						@Override
						public void onDisconnected()
						{

						}
					});

				// start the connection
				beaconConnection.authenticate();

				// close conection after 5 seconds TODO: change to constant or user defined value
				final Handler connectionHandler = new Handler();
				connectionHandler.postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						beaconConnection.close();
					}
				}, SCAN_LENGTH * 1000);
			}
		};

		// start the BLE scan
		bluetoothAdapter.startLeScan(scanCallback);
		scanning = true;

		// stop the scan after a length of time
		Handler handler = new Handler();
		handler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				bluetoothAdapter.stopLeScan(scanCallback);
				discovered.clear();
				progressDialog.dismiss();
				scanning = false;

				Log.i(TAG, "FINISHED SCANNING");

			}
		}, SCAN_LENGTH * 1000);
	}
}
