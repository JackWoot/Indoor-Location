package uk.ac.ncl.cloudcdt.blelocation;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;

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
public class MotionTrackingService extends IntentService
{
	public static final String TAG = "MotionLoggingService";
	private static final int SENSOR_DELAY = 1000000;

	public static Context context;

	/**
	 * Scan and collect data for total of 60 seconds.
	 */
	public static final int length = 60;

	private boolean started = false;
	public static boolean stopped = false;

	/**
	 * Our logfile to store collected data in.
	 */
	private File logfile;
	private static CSVPrinter writer;

	// Intent actions that this service can perform
	public static final String ACTION_INIT_LOG_FILE = "uk.ac.ncl.cloudcdt.blelocation.action.INIT_LOG_FILE";
	public static final String ACTION_LOG_MOTION = "uk.ac.ncl.cloudcdt.blelocation.action.LOG_MOTION";
	public static final String ACTION_STOP_LOG_MOTION = "uk.ac.ncl.cloudcdt.blelocation.action.STOP_LOG_MOTION";

	private static BluetoothAdapter.LeScanCallback scanCallback;

	private static SensorManager sensorManager;
	private static Sensor compassSensor;
	private static Sensor accelerometerSensor;

	private static SensorEvent accelerometerEvent;
	private static SensorEvent compassEvent;

	//private static Context context;

	private static SensorEventListener accelerometerListener = new SensorEventListener()
	{
		@Override
		public void onSensorChanged(SensorEvent event)
		{
			accelerometerEvent = event;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy)
		{

		}
	};

	private static SensorEventListener compassListener = new SensorEventListener()
	{
		@Override
		public void onSensorChanged(SensorEvent event)
		{
			compassEvent = event;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy)
		{

		}
	};


	public MotionTrackingService()
	{
		super("MotionTrackingService");
	}

	private static void registerMotionListeners(Context context)
	{
		// get sensor manager for device
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

		// get accelerometer sensor and add listener
		accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(accelerometerListener, accelerometerSensor, SENSOR_DELAY);

		// get magnetic field sensor and add listener
		compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		sensorManager.registerListener(compassListener, compassSensor, SENSOR_DELAY);
	}

	private static void unregisterMotionListeners()
	{
		sensorManager.unregisterListener(accelerometerListener);
		sensorManager.unregisterListener(compassListener);
	}

	/**
	 * Starts this service to perform action Foo with the given parameters. If
	 * the service is already performing a task this action will be queued.
	 *
	 * @see IntentService
	 */
	public static void startActionInitLogFile(Context c)
	{
		Intent intent = new Intent(c, MotionTrackingService.class);
		intent.setAction(ACTION_INIT_LOG_FILE);
		c.startService(intent);
	}

	/**
	 * Starts this service to perform action Baz with the given parameters. If
	 * the service is already performing a task this action will be queued.
	 *
	 * @see IntentService
	 */
	public static void startActionLogMotion(Context c)
	{
		Intent intent = new Intent(c, MotionTrackingService.class);
		intent.setAction(ACTION_LOG_MOTION);
		c.startService(intent);
	}

	public static void stopActionLogMotion(Context c)
	{
		Intent intent = new Intent(c, MotionTrackingService.class);
		intent.setAction(ACTION_STOP_LOG_MOTION);
		c.startService(intent);
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		if(intent != null)
		{
			final String action = intent.getAction();

			if(ACTION_INIT_LOG_FILE.equals(action))
			{
				handleActionInitLogFile();
			}
			else if(ACTION_LOG_MOTION.equals(action))
			{
				// begin scanning for these coordinates
				registerMotionListeners(context);
				handleActionLogMotion();
			}
			else if(ACTION_STOP_LOG_MOTION.equals(action))
			{
				stopped = true;
				unregisterMotionListeners();
			}
		}
	}

	/**
	 * Handle action Foo in the provided background thread with the provided
	 * parameters.
	 */
	private void handleActionInitLogFile()
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
		String filename = String.format("motion_logging_%s.csv", dt.toString("yyyy-MM-dd-HH-mm-ss"));

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

			writer = CSVFormat.DEFAULT.withHeader("azimuth", "pitch", "roll", "accel_x", "accel_y",
				"accel_z", "compass_x_uncalib", "compass_y_uncalib", "compass_z_uncalib", "time")
				.print(out);

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
	private void handleActionLogMotion()
	{
		Runnable logger = new Runnable()
		{
			@Override
			public void run()
			{
				System.out.println("FUCKING EXECUTE FFS YOU PIECE OF FUCKING SHIT");
				while(!stopped)
				{
					// quit if accelerometer or magnetic field sensors are not yet ready
					if(accelerometerEvent == null || compassEvent == null)
					{
						continue;
					}

					System.out.println("POLL");

					// calculate rotation matrix needed to calculate orientation
					float[] rotationMatrix = new float[9];
					SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerEvent.values, compassEvent.values);

					// calculate orientation data
					float[] orientation = new float[3];
					SensorManager.getOrientation(rotationMatrix, orientation);

					float[] acceleromter = accelerometerEvent.values;
					float[] compass = compassEvent.values;

					// write to log file
					DateTime date = new DateTime();
					List<String> entry = new ArrayList<String>();

					// orientation calculations
					entry.add(String.format("%.3f", orientation[0]));
					entry.add(String.format("%.3f", orientation[1]));
					entry.add(String.format("%.3f", orientation[2]));

					// accelerometer readings
					entry.add(String.format("%.3f", acceleromter[0]));
					entry.add(String.format("%.3f", acceleromter[1]));
					entry.add(String.format("%.3f", acceleromter[2]));

					// magnetic field readings
					entry.add(String.format("%.3f", compass[0]));
					entry.add(String.format("%.3f", compass[1]));
					entry.add(String.format("%.3f", compass[2]));

					entry.add(date.toString("yyyy-MM-dd HH:mm:ss.SSS"));

					try
					{
						writer.printRecord(entry);
						writer.flush();

						Thread.sleep(SENSOR_DELAY / 1000); // microseconds to millis
					} catch(IOException e)
					{
						e.printStackTrace();
					} catch(InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
		};

		new Thread(logger).start();
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
