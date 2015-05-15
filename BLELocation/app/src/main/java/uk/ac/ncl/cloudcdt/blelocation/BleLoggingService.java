package uk.ac.ncl.cloudcdt.blelocation;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.Context;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.joda.time.DateTime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * TODO: close file when app closed?
 * helper methods.
 */
public class BleLoggingService extends IntentService
{
	public static final String TAG = "BleLoggingService";

	/**
	 * BLE objects responsible for interacting
	 * with BLE hardware on device.
	 */
	private BluetoothAdapter bluetoothAdapter;
	public static boolean started = false;
	public static boolean finished = false;
	public static boolean scanning;

	/**
	 * Our logfile to store collected data in.
	 */
	private File logfile;
	private static CSVPrinter writer;

	// Intent actions that this service can perform
	public static final String ACTION_INIT_LOG_FILE = "uk.ac.ncl.cloudcdt.blelocation.action.INIT_LOG_FILE";
	public static final String ACTION_SCAN_AND_LOG_FINGERPRINT = "uk.ac.ncl.cloudcdt.blelocation.action.SCAN_AND_LOG_FINGERPRINT";
	public static final String ACTION_SCAN_AND_LOG_MOTION = "uk.ac.ncl.cloudcdt.blelocation.action.SCAN_AND_LOG_MOTION";

	//  Intent parameters
	private static final String EXTRA_FINGERPRINT = "uk.ac.ncl.cloudcdt.blelocation.extra.FINGERPRINT";
	private static final String EXTRA_GRID_X = "uk.ac.ncl.cloudcdt.blelocation.extra.GRID_X";
	private static final String EXTRA_GRID_Y = "uk.ac.ncl.cloudcdt.blelocation.extra.GRID_Y";
	private static final String EXTRA_SCAN_LENGTH = "uk.ac.ncl.cloudcdt.blelocation.extra.SCAN_LENGTH";

	private static BluetoothAdapter.LeScanCallback scanCallback;

	private static Context context;

	public BleLoggingService()
	{
		super("BleLoggingService");
		scanning = false;
	}

	/**
	 * Starts this service to perform action Foo with the given parameters. If
	 * the service is already performing a task this action will be queued.
	 *
	 * @see IntentService
	 */
	public static void startActionInitLogFile(Context c, boolean fingerprint)
	{
		context = c;
		Intent intent = new Intent(context, BleLoggingService.class);
		intent.setAction(ACTION_INIT_LOG_FILE);
		intent.putExtra(EXTRA_FINGERPRINT, fingerprint);
		context.startService(intent);
	}

	/**
	 * Starts this service to perform action Baz with the given parameters. If
	 * the service is already performing a task this action will be queued.
	 *
	 * @see IntentService
	 */
	public static void startActionScanAndLogFingerprint(Context c, int scanLength, String x, String y)
	{
		context = c;
		Intent intent = new Intent(context, BleLoggingService.class);
		intent.setAction(ACTION_SCAN_AND_LOG_FINGERPRINT);
		intent.putExtra(EXTRA_SCAN_LENGTH, scanLength);
		intent.putExtra(EXTRA_GRID_X, x);
		intent.putExtra(EXTRA_GRID_Y, y);
		context.startService(intent);
	}

	public static void startActionScanAndLogMotion(Context c)
	{
		context = c;
		Intent intent = new Intent(context, BleLoggingService.class);
		intent.setAction(ACTION_SCAN_AND_LOG_MOTION);
		context.startService(intent);
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		Log.i(TAG, "HANDLING INTENT");
		Log.i(TAG, intent.getAction().toString());

		if(intent != null)
		{
			final String action = intent.getAction();

			if(ACTION_INIT_LOG_FILE.equals(action))
			{
				boolean fingerprint = intent.getBooleanExtra(EXTRA_FINGERPRINT, true);
				handleActionInitLogFile(fingerprint);
			}
			else if(ACTION_SCAN_AND_LOG_FINGERPRINT.equals(action))
			{
				final int scanLength = intent.getIntExtra(EXTRA_SCAN_LENGTH, 10);
				String x = intent.getStringExtra(EXTRA_GRID_X);
				String y = intent.getStringExtra(EXTRA_GRID_Y);

				// begin scanning for these coordinates
				handleActionScanAndLog(scanLength, x, y);
			}
			else if(ACTION_SCAN_AND_LOG_MOTION.equals(action))
			{
				// scan indefinitely until told to stop
				handleActionScanAndLog(-1, "", "");
			}
		}
	}

	/**
	 * Handle action Foo in the provided background thread with the provided
	 * parameters.
	 */
	private void handleActionInitLogFile(boolean fingerprint)
	{
		started = true;

		// TODO: some proper error handling

		// TODO: check that there is enough space on disk by calling getFreeSpace() and getTotalSpace.
		if(!isExternalStorageWritable())
		{
			Log.e(TAG, "External storage not writable.");
			return;
		}

		DateTime dt = new DateTime();
		String filename = String.format("ble_logging_%s.csv", dt.toString("yyyy-MM-dd-HH-mm-ss"));

		logfile = new File(
			Environment.getExternalStorageDirectory(),
			filename
		);

		BufferedWriter out;
		try {
			if(!logfile.exists())
			{
				logfile.createNewFile();
			}

			out = new BufferedWriter(new FileWriter(logfile));

			if(fingerprint)
			{
				writer = CSVFormat.DEFAULT.withHeader("x", "y", "address", "rssi", "power", "distanceEstimate", "time").print(out);
			}
			else
			{
				writer = CSVFormat.DEFAULT.withHeader("address", "rssi", "power", "distanceEstimate", "time").print(out);
			}

		} catch(IOException e) {
			e.printStackTrace();
			return;
		}

		Log.d(TAG, "File created at: " + logfile);
	}

	/**
	 * Handle action Baz in the provided background thread with the provided
	 * parameters.
	 *
	 * TODO: check log file has been initialised.
	 */
	private void handleActionScanAndLog(final int scanLength, final String x, final String y)
	{
		// Initialise Bluetooth adapter/scanner
		final BluetoothManager bluetoothManager =
			(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();

		// Ensures Bluetooth is available on the device and it is enabled.
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
		{
			Log.e(TAG, "Error: Bluetooth is not enabled.");
			return;
		}

		scanCallback = new BluetoothAdapter.LeScanCallback()
		{
			@Override
			public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
			{
				// fetch address and RSSI values
				String address = device.getAddress();

				// estimate distance using estimote SDK
				Beacon beacon = Utils.beaconFromLeScan(device, rssi, scanRecord);

				if(beacon == null)
				{
					// don't log
					return;
				}

				double distanceEstimate = Utils.computeAccuracy(beacon);

				int powerLevel = beacon.getMeasuredPower();

				Log.i(TAG, address + ": " + rssi);

				// write to log file
				DateTime date = new DateTime();
				List<String> entry = new ArrayList<String>();

				if(scanLength > 0)
				{
					entry.add(String.valueOf(x));
					entry.add(String.valueOf(y));
				}

				entry.add(device.getAddress());
				entry.add(String.valueOf(rssi));
				entry.add(String.valueOf(powerLevel));
				entry.add(String.format("%.2f", distanceEstimate));
				entry.add(date.toString("yyyy-MM-dd HH:mm:ss.SSS"));

				try {
					writer.printRecord(entry);
					writer.flush();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		};

		if(scanLength == -1)
		{
			scan(10, false);
		}
		else
		{
			scan(scanLength, true);
		}
	}

	/**
	 * Perform a BLE scan. On each discovery, connect to beacon
	 * and poll its RSSI (using scanCallback above).
	 *
	 * TODO: make stop button actually stop scanning
	 */
	private void scan(int length, boolean fingerprinting)
	{
		if(finished)
		{
			return;
		}

		bluetoothAdapter.startLeScan(scanCallback);

		//bluetoothScanner.startScan(filters, scanSettings, scanCallback);

		try {
			scanning = true;

			// Note: was using Handler postDelayed() before, but this does not
			// work in an IntentService due to handler being removed after
			// onHandleIntent() has finished executing.
			Thread.sleep(length * 1000);

			bluetoothAdapter.stopLeScan(scanCallback);
			scanning = false;

			if(fingerprinting)
			{
				// send broadcast to allow MainActivity to respond to scan finishing.
				LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(this);
				Intent intent = new Intent(ACTION_SCAN_AND_LOG_FINGERPRINT);
				broadcaster.sendBroadcast(intent);

				sendNotification();

				Log.i(TAG, "FINISHED SCANNING");
			}
			else
			{
				scan(10, false);
			}

		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void sendNotification()
	{
		NotificationManager mNotificationManager =
			(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		int NOTIFICATION_ID = 100;
		String ringtone = "content://settings/system/notification_sound";
		String msg = "Scan finished.";

		//int defaults = Notification.DEFAULT_LIGHTS;
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
			.setContentTitle(getString(R.string.app_name))
			.setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setSound(Uri.parse(ringtone))
			.setContentText(msg);

		//mBuilder.setContentIntent(contentIntent);
		//defaults |= Notification.DEFAULT_VIBRATE;

		//mBuilder.setDefaults(defaults);

		Notification notification = mBuilder.build();
		notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
		notification.ledARGB = 0xff00ff00;
		notification.ledOnMS = 500;
		notification.ledOffMS = 500;

		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}

	/**
	 *  Checks if external storage is available for read and write
	 */
	public boolean isExternalStorageWritable()
	{
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}

	/**
	 *  Checks if external storage is available to at least read
	 */
	public boolean isExternalStorageReadable()
	{
		String state = Environment.getExternalStorageState();

		return Environment.MEDIA_MOUNTED.equals(state) ||
			Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
	}
}
