package info.math.heartmeasure;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import com.jwetherell.heart_rate_monitor.ImageProcessing;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.Toast;
//import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {
	//	Curve
	private Timer timer = new Timer();
	private TimerTask task;
	private static int gx;
	private static int j;
	
	private static double flag=1;
	private Handler handler;
	private String title = "pulse";
	private XYSeries series;
	private XYMultipleSeriesDataset mDataset;
	private GraphicalView chart;
	private XYMultipleSeriesRenderer renderer;
	private Context context;
	private int addX = -1;
	double addY;
	int[] xv = new int[300];
	int[] yv = new int[300];
	int[] hua=new int[]{9,10,11,12,13,14,13,12,11,10,9,8,7,6,7,8,9,10,11,10,10};

	//	private static final String TAG = "HeartRateMonitor";
	private static final AtomicBoolean processing = new AtomicBoolean(false);
	private static SurfaceView preview = null;
	private static SurfaceHolder previewHolder = null;
	private static Camera camera = null;
	//	private static View image = null;
	private static TextView text = null;
	private static TextView text1 = null;
	private static TextView text2 = null;
	private static WakeLock wakeLock = null;
	private static int averageIndex = 0;
	private static final int averageArraySize = 4;
	private static final int[] averageArray = new int[averageArraySize];

	public static enum TYPE {
		GREEN, RED
	};

	private static TYPE currentType = TYPE.GREEN;

	public static TYPE getCurrent() {
		return currentType;
	}

	private static int beatsIndex = 0;
	private static final int beatsArraySize = 3;
	private static final int[] beatsArray = new int[beatsArraySize];
	private static double beats = 0;
	private static long startTime = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//		Curve
		context = getApplicationContext();

		// Here get the layout of main interface，the chart is drawn in this layout.
		LinearLayout layout = (LinearLayout)findViewById(R.id.linearLayout1);

		// This class include all the curve points, a collection of points. a curve is drawn by these points.
		series = new XYSeries(title);

		// Create a instance of a set, this set is used to make a chart
		mDataset = new XYMultipleSeriesDataset();

		// a point is added to this set
		mDataset.addSeries(series);

		//the following parts are one to set the curve style and properties and renderer is a handler to draw a chart
		int color = Color.GREEN;
		PointStyle style = PointStyle.CIRCLE;
		renderer = buildRenderer(color, style, true);

		//Set the chart style.
		setChartSettings(renderer, "X", "Y", 0, 300, 4, 16, Color.WHITE, Color.WHITE);

		//Generate a chart
		chart = ChartFactory.getLineChartView(context, mDataset, renderer);

		//Add a chart to layout. 
		layout.addView(chart, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));


		/*	       thread = new Thread(){
	    	   public void arrayList(int u) { 
	    		   ArrayList arrayList = new ArrayList();
	    		   arrayList.add(HardwareControler.readADC());   			
	   		}
	       };*/
		// With a timer the chart is refreshed.
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				//        		Refresh chart
				updateChart();
				super.handleMessage(msg);
			}
		};

		task = new TimerTask() {
			@Override
			public void run() {
				Message message = new Message();
				message.what = 1;
				handler.sendMessage(message);
			}
		};

		timer.schedule(task, 1,20);           //Curve

		preview = (SurfaceView) findViewById(R.id.preview);
		previewHolder = preview.getHolder();
		previewHolder.addCallback(surfaceCallback);
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		//		image = findViewById(R.id.image);
		text = (TextView) findViewById(R.id.text);
		text1 = (TextView) findViewById(R.id.text1);
		text2 = (TextView) findViewById(R.id.text2);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
	}

	//	Curve
	@Override
	public void onDestroy() {
		//When this app close, close the Timer
		timer.cancel();
		super.onDestroy();
	};


	protected XYMultipleSeriesRenderer buildRenderer(int color, PointStyle style, boolean fill) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

		// Set the style of chart , including color, the size of point and thickness and so on.
		XYSeriesRenderer r = new XYSeriesRenderer();
		r.setColor(Color.RED);
//		r.setPointStyle(null);
//		r.setFillPoints(fill);
		r.setLineWidth(1);
		renderer.addSeriesRenderer(r);
		return renderer;
	}

	protected void setChartSettings(XYMultipleSeriesRenderer renderer, String xTitle, String yTitle,
			double xMin, double xMax, double yMin, double yMax, int axesColor, int labelsColor) {
		// About the rendering of the chart can be found in api documentation
		renderer.setChartTitle(title);
		renderer.setXTitle(xTitle);
		renderer.setYTitle(yTitle);
		renderer.setXAxisMin(xMin);
		renderer.setXAxisMax(xMax);
		renderer.setYAxisMin(yMin);
		renderer.setYAxisMax(yMax);
		renderer.setAxesColor(axesColor);
		renderer.setLabelsColor(labelsColor);
		renderer.setShowGrid(true);
		renderer.setGridColor(Color.GREEN);
		renderer.setXLabels(20);
		renderer.setYLabels(10);
		renderer.setXTitle("Time");
		renderer.setYTitle("mmHg");
		renderer.setYLabelsAlign(Align.RIGHT);
		renderer.setPointSize((float) 3 );
		renderer.setShowLegend(false);
	}

	private void updateChart() {

		//Set the next increase point
		//    	addX = 10;
		//addY = (int)(Math.random() * 90 + 50);  
		//		addY = (int)(HardwareControler.readADC());
		//    	addY=10+addY;
		//    	if(addY>1400)
		//    		addY=10;
		if(flag==1)
			addY=10;
		else{
//			addY=250;
			flag=1;
			if(gx<200){
				if(hua[20]>1){
					Toast.makeText(MainActivity.this, "Please use your fingertips to cover the camera lens!", Toast.LENGTH_SHORT).show();
					hua[20]=0;}
				hua[20]++;
				return;}
			else
				hua[20]=10;
			j=0;
			
		}
		if(j<20){
			addY=hua[j];
			j++;
		}
			
		//Remove the old data from collection points.
		mDataset.removeSeries(series);

		//Analyzing the current point in the end how much focus point, because the screen can accommodate a total of 100, so when more than 100 points, the length is always 100
		int length = series.getItemCount();
		int bz=0;
		//		addX = length;
		if (length > 300) {
			length = 300;
			bz=1;
		}
		addX = length;
		//
		for (int i = 0; i < length; i++) {
			xv[i] = (int) series.getX(i) -bz;
			yv[i] = (int) series.getY(i);
		}

		//First,empty set of points, in order to make a new set of points and prepare
		series.clear();
		mDataset.addSeries(series);

		series.add(addX, addY);
		for (int k = 0; k < length; k++) {
			series.add(xv[k], yv[k]);
		}
		chart.invalidate();
	}                                                                           //Curve


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onResume() {
		super.onResume();
		wakeLock.acquire();
		camera = Camera.open();
		startTime = System.currentTimeMillis();
	}

	@Override
	public void onPause() {
		super.onPause();
		wakeLock.release();
		camera.setPreviewCallback(null);
		camera.stopPreview();
		camera.release();
		camera = null;
	}

	private static PreviewCallback previewCallback = new PreviewCallback() {

		public void onPreviewFrame(byte[] data, Camera cam) {
			if (data == null)
				throw new NullPointerException();
			Camera.Size size = cam.getParameters().getPreviewSize();
			if (size == null)
				throw new NullPointerException();
			if (!processing.compareAndSet(false, true))
				return;
			int width = size.width;
			int height = size.height;
			// Process image
			int imgAvg = ImageProcessing.decodeYUV420SPtoRedAvg(data.clone(),height,width);
			gx=imgAvg;
			text1.setText("The average pixel value is : "+String.valueOf(imgAvg));
			//imgAvg of Pixel average value, Log
			//			Log.i(TAG, "imgAvg=" + imgAvg);
			if (imgAvg == 0 || imgAvg == 255) {
				processing.set(false);
				return;
			}

			int averageArrayAvg = 0;
			int averageArrayCnt = 0;
			for (int i = 0; i < averageArray.length; i++) {
				if (averageArray[i] > 0) {
					averageArrayAvg += averageArray[i];
					averageArrayCnt++;
				}
			}

			int rollingAverage = (averageArrayCnt > 0)?(averageArrayAvg/averageArrayCnt):0;
			TYPE newType = currentType;
			if (imgAvg < rollingAverage) {
				newType = TYPE.RED;
				if (newType != currentType) {
					beats++;
					flag=0;
					text2.setText("The number of pulses is   "+String.valueOf(beats));
					//					Log.e(TAG, "BEAT!! beats=" + beats);
				}
			} else if (imgAvg > rollingAverage) {
				newType = TYPE.GREEN;
			}

			if (averageIndex == averageArraySize)
				averageIndex = 0;
			averageArray[averageIndex] = imgAvg;
			averageIndex++;

			// Transitioned from one state to another to the same
			if (newType != currentType) {
				currentType = newType;
				//				image.postInvalidate();
			}
//Get System End Time（ms）
			long endTime = System.currentTimeMillis();
			double totalTimeInSecs = (endTime - startTime) / 1000d;
			if (totalTimeInSecs >= 2) {
				double bps = (beats / totalTimeInSecs);
				int dpm = (int) (bps * 60d);
				if (dpm < 30 || dpm > 180||imgAvg<200) {
					//Get System start Time（ms）
					startTime = System.currentTimeMillis();
					//beats : Total heartbeat
					beats = 0;
					processing.set(false);
					return;
				}
				//				Log.e(TAG, "totalTimeInSecs=" + totalTimeInSecs + " beats="+ beats);
				if (beatsIndex == beatsArraySize)
					beatsIndex = 0;
				beatsArray[beatsIndex] = dpm;
				beatsIndex++;
				int beatsArrayAvg = 0;
				int beatsArrayCnt = 0;
				for (int i = 0; i < beatsArray.length; i++) {
					if (beatsArray[i] > 0) {
						beatsArrayAvg += beatsArray[i];
						beatsArrayCnt++;
					}
				}
				int beatsAvg = (beatsArrayAvg / beatsArrayCnt);
				text.setText("Your heart rate is : "+String.valueOf(beatsAvg)+"  zhi : "+String.valueOf(beatsArray.length)
						+"    "+String.valueOf(beatsIndex)+"    "+String.valueOf(beatsArrayAvg)+"    "+String.valueOf(beatsArrayCnt));
//Get System time（ms）
				startTime = System.currentTimeMillis();
				beats = 0;
			}
			processing.set(false);
		}
	};

	private static SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

		public void surfaceCreated(SurfaceHolder holder) {
			try {
				camera.setPreviewDisplay(previewHolder);
				camera.setPreviewCallback(previewCallback);
			} catch (Throwable t) {
				//				Log.e("PreviewDemo-surfaceCallback","Exception in setPreviewDisplay()", t);
			}
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			Camera.Parameters parameters = camera.getParameters();
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			Camera.Size size = getSmallestPreviewSize(width, height, parameters);
			if (size != null) {
				parameters.setPreviewSize(size.width, size.height);
				//				Log.d(TAG, "Using width=" + size.width + " height="	+ size.height);
			}
			camera.setParameters(parameters);
			camera.startPreview();
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			// Ignore
		}
	};

	private static Camera.Size getSmallestPreviewSize(int width, int height,
			Camera.Parameters parameters) {
		Camera.Size result = null;
		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;
					if (newArea < resultArea)
						result = size;
				}
			}
		}
		return result;
	}
}