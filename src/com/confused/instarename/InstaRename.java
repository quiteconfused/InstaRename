package com.confused.instarename;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.confused.instarename.post.MultipartPost;
import com.confused.instarename.post.PostParameter;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.Notification;
import android.content.Intent;
import android.app.PendingIntent;
import android.view.View;
import android.view.View.OnClickListener;

public class InstaRename extends Activity
{
	private PhotosObserver instUploadObserver = new PhotosObserver();
	private static String detected=null;
	private static String result=null;
	private static String renamed=null;
	
	private static TextView tv_detected = null;
	private static TextView tv_result = null;
	private static TextView tv_renamed = null;
	
	private static ImageView jpgView = null;
	private static AutoCompleteTextView avt = null;
	
	Button button;
	Button next;
	Button prev;
	
	static String image_path = Environment.getExternalStorageDirectory().getPath()+"/DCIM/Camera/";
	
   	File image_dir = null;
	static File[] images = null; 
	static int current_image = 0;
	
    // Need handler for callbacks to the UI thread
    final static Handler mHandler = new Handler();
    
    public static void updateResultsInUi(){
    	
		jpgView.setImageBitmap(decodeSampledBitmapFromPath(images[current_image].getAbsolutePath(), 200, 200));
	 
		tv_detected.setText("Detected: " + detected);
		tv_result.setText("Result: " + result);
		tv_renamed.setText("Renamed: " + renamed);
    }

    // Create runnable for posting
    final static Runnable mUpdateResults = new Runnable() {
        public void run() {
            updateResultsInUi();
        }
    };
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		tv_detected = (TextView)findViewById(R.id.detected);
		tv_detected.setText("Detected:");
		
		tv_result = (TextView)findViewById(R.id.result);
		tv_result.setText("Result:");
		
		tv_renamed = (TextView)findViewById(R.id.renamed);
		tv_result.setText("Renamed:");
		
		avt = (AutoCompleteTextView)findViewById(R.id.image_dir);
		avt.setText(image_path.subSequence(0, image_path.lastIndexOf('/')));
		
		try{
			Log.d("Reading files", image_path);
			image_dir = new File(image_path);
			images = image_dir.listFiles();
			jpgView = (ImageView)findViewById(R.id.imageview);
			jpgView.setImageBitmap(decodeSampledBitmapFromPath(images[current_image].getAbsolutePath(), 200, 200));
		} finally {
			
			
		}


		addListenerOnButton();
		addListenerOnSingleButton();
		addListenerOnNextButton();
		addListenerOnPrevButton();
		
		this.getApplicationContext().getContentResolver().registerContentObserver(	MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, instUploadObserver);
		Log.d("INSTANT", "registered content observer");
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (detected != null && result != null && renamed != null) {
			updateResultsInUi();

		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.getApplicationContext().getContentResolver().unregisterContentObserver(instUploadObserver);
		Log.d("INSTANT", "unregistered content observer");
	}	
	
	public void createNotification(String filename, String reference){

		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
  
		//
		// Create your notification
		int icon = R.drawable.icon;
		CharSequence tickerText = "Image Renamed";
		long when = System.currentTimeMillis();
	 
		Notification notification = new Notification( icon, tickerText, when);
		  
		Context context = getApplicationContext();
		CharSequence contentTitle = reference; 
		CharSequence contentText = filename + " renamed to " + reference;
		Intent notificationIntent = new Intent(this, InstaRename.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		  
		notification.setLatestEventInfo( context, contentTitle, contentText, contentIntent);

		//
		// Send the notification
		nm.notify( 0, notification );	
		 
		Log.d("INSTANT", "Responded with notification " + filename);
	}
	
	public static String getResult(File file){
		String tempresult = null;
		String result2 = sendPostRequest(file);

		StringTokenizer tok = new StringTokenizer(result2, "<>");
		String previous_entity=null;
		
		while(tok.hasMoreElements()){
			String nextitem = tok.nextElement().toString();
			
			if(nextitem.startsWith("Visually similar")){
				try{
					if(previous_entity.contains("q=") && previous_entity.contains("&amp")){
						int start = previous_entity.indexOf("q=")+2;
						int end = previous_entity.indexOf("&amp", start);
						String contents = previous_entity.substring( start , end);
						contents = contents.replace('+', ' ');
					
						Log.d("Result:", contents);
				
						tempresult = contents;
					} else {
						
						
					}
				} catch (Exception e){
					e.printStackTrace();						
				}
				break;
			}
			
			if(nextitem.startsWith("a")){
				StringTokenizer tok2 = new StringTokenizer(nextitem);
				
				while(tok2.hasMoreElements()){
					String subitem = tok2.nextElement().toString();
					if( subitem.startsWith("href") ){
						previous_entity=nextitem;
					}
				}
			}
		}
		
		return tempresult;
	}
	
	public static void performRenameAndNotify(String value, File file){
		
		detected = file.getName();
		
		if(value!=null){
			String path = file.getPath();
			String base = file.getName();
			
			int lastindex = path.lastIndexOf('/');
			path=path.substring(1,lastindex+1);
			
			while(base.contains("/")){
				int next_loc = base.indexOf('/');
				base = base.substring(next_loc);
			}
			
			File file2 = new File( path + value + "-" + base);
			try{
				if(base.startsWith(value, 0)){
					result = value;
					renamed = file.getName();
				}
				else if(!file.renameTo(file2)){
					result = value;
					renamed = file.getName();
					
				} else {
					result = value;
					renamed = file2.getName();
				}
			}
			catch(Exception e){
				Log.d("performRenameAndNotify: ", e.getMessage());
			}
		} else {
	
			result = "<NONE>";
			renamed = file.getName();
		}
	}
		
	private class PhotosObserver extends ContentObserver {
		public PhotosObserver() {
			super(null);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			Media media = readFromMediaStore(getApplicationContext(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			String tempresult = getResult(media.file);
			performRenameAndNotify(tempresult, media.file);
			createNotification(renamed, result);
			updateResultsInUi();
		}
	}
	
	private Media readFromMediaStore(Context context, Uri uri) {
		Cursor cursor = context.getContentResolver().query(uri, null, null, null, "date_added DESC");
		Media media = null;
		if (cursor.moveToNext()) {
			int dataColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
			String filePath = cursor.getString(dataColumn);
			int mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE);
			String mimeType = cursor.getString(mimeTypeColumn);
			media = new Media(new File(filePath), mimeType);
		}
		cursor.close();
		return media;
	}
	
	private class Media {
		private File file;
		@SuppressWarnings("unused")
		private String type;

		public Media(File file, String type) {
			this.file = file;
			this.type = type;
		}
	}
    
    private static String sendPostRequest(File filename) {
    	String response = null;
    	
    	Log.d("SendPostRequest", "sendPostRequest");
    	@SuppressWarnings("rawtypes")
		List<PostParameter> params = new ArrayList<PostParameter>();
    	params.add(new PostParameter<String>("image_url", ""));
    	params.add(new PostParameter<String>("btnG", "Search"));
    	params.add(new PostParameter<File>("encoded_image", filename));
    	params.add(new PostParameter<String>("image_content", ""));
    	params.add(new PostParameter<String>("filename", ""));
    	params.add(new PostParameter<String>("hl", "en"));
    	params.add(new PostParameter<String>("safe", "off"));
    	params.add(new PostParameter<String>("bih", ""));
    	params.add(new PostParameter<String>("biw", ""));
    	
    	try {
    		MultipartPost post = new MultipartPost(params);
    		Log.d("INSTANT", "multipart post created");
    		response = post.send("http://www.google.com/searchbyimage/upload", "http://images.google.com");
    		
    	} catch (Exception e) {
    		Log.e("INSTANT", "Bad Post", e);
		}
    	
    	params.clear();
    	params = null;
    	return response;
    }
    
    public static int calculateInSampleSize( BitmapFactory.Options options, int reqWidth, int reqHeight){
    	final int height = options.outHeight;
    	final int width = options.outWidth;
    	int inSampleSize = 1;
    	
    	if(height > reqHeight || width > reqWidth){
    		if(width>height){
    			inSampleSize = Math.round((float)height/(float)reqHeight);
    		} else {
    			inSampleSize = Math.round((float)width/(float)reqWidth);
    		}
    	}
    	return inSampleSize;
    }
    
    public static Bitmap decodeSampledBitmapFromPath( String path, int reqWidth, int reqHeight){
    	final BitmapFactory.Options options = new BitmapFactory.Options();
    	options.inJustDecodeBounds = true;
    	BitmapFactory.decodeFile(path, options);
    	
    	options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
    	
    	options.inJustDecodeBounds = false;
    	return BitmapFactory.decodeFile(path, options);
    }

    public void addListenerOnSingleButton() {
    	button = (Button) findViewById(R.id.button2);
 
    	button.setOnClickListener(new OnClickListener() {
    		public void onClick(View arg0) {
    			image_path = avt.getEditableText().toString();
     			Thread t = performOnBackgroundThreadSingleFile();
    		}
     	});
 	}
    
    
    public void addListenerOnButton() {
    	button = (Button) findViewById(R.id.button1);
 
    	button.setOnClickListener(new OnClickListener() {
    		public void onClick(View arg0) {
     			Thread t = performOnBackgroundThread();
    		}
     	});
 	}

    public void addListenerOnPrevButton(){
    	prev = (Button) findViewById(R.id.prev);
    	
    	prev.setOnClickListener(new OnClickListener() {
    		public void onClick(View arg0){
    			image_path = avt.getEditableText().toString();
    			image_dir = new File(image_path);
    			images = image_dir.listFiles();
    			
    			current_image--;
    			if(current_image<0)
    				current_image=images.length-1;
    			Log.d("Prev", "Trying to Display " + images[current_image].getAbsolutePath());
		    	try{
    				detected = images[current_image].getName();
    				renamed = "";
    				result = "";
					updateResultsInUi();
		    	} catch(Exception e){
		    		Log.d("Prev", e.getMessage());
		    	}
            }
    	});
    }    
    
    public void addListenerOnNextButton(){
    	next = (Button) findViewById(R.id.next);
    	
    	next.setOnClickListener(new OnClickListener() {
    		public void onClick(View arg0){
    			image_path = avt.getEditableText().toString();    			
    			image_dir = new File(image_path);
    			images = image_dir.listFiles();
    			
    			current_image++;
    			if(current_image>images.length-1)
    				current_image=0;
    			Log.d("Next", "Trying to Display " + images[current_image].getAbsolutePath());
    			try{
    				detected = images[current_image].getName();
    				renamed = "";
    				result = "";
					updateResultsInUi();
		    	} catch(Exception e){
		    		Log.d("Next", e.getMessage());
		    	}
            }
    	});
    }

    public static Thread performOnBackgroundThread() {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                	File f = new File(image_path);
                	File[] files = f.listFiles();
                	for( int i=0; i<files.length; i++){
                		if(files[i].isFile() && ( files[i].getPath().endsWith(".jpg") || files[i].getPath().endsWith(".bmp") ||files[i].getPath().endsWith(".png") ||files[i].getPath().endsWith(".gif") )){
                			current_image = i;
                			Log.d("Inside thread", files[i].getPath());
        					String tempresult = getResult(files[i]);
        					performRenameAndNotify(tempresult, files[i]);
        					mHandler.post(mUpdateResults);
        	        		Log.d("Stuff", "Result is: " + tempresult);
                		}
                	}
                } finally {

                }
            }
        };
        t.start();
        return t;
    }
    
    public static Thread performOnBackgroundThreadSingleFile() {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
           			Log.d("Inside thread", images[current_image].getPath());
					String tempresult = getResult(images[current_image]);
					performRenameAndNotify(tempresult, images[current_image]);
					mHandler.post(mUpdateResults);
	        		Log.d("Stuff", "Result is: " + tempresult);                
                } finally {

                }
            }
        };
        t.start();
        return t;
    }
}

