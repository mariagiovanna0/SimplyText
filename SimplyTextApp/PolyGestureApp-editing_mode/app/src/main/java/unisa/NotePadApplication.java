package unisa;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import android.app.Application;
import android.content.res.XmlResourceParser;
import android.os.Environment;
import com.example.android.polyGestureApp.R;

public class NotePadApplication extends Application {
	private List<Task> tasks;
	private String username;
	public final String[] SIZE_ITEMS = {"small", "medium", "large"};
	private final float[] sizes = {2.5F, 3F, 4F};

	private float fontSize = sizes[1];
	private DataOutputStream dos;
	
	public int sequenceIndex;
	public final String[] SEQUENCE = {"TE" , "ET"};
	public boolean firstSequence = true;	//true if it's the first sequence, false if it's the second one
	
    //Logged Actions
	
	public static final String APP_NOTEPAD = "PolyGesture";
	//public static final String APP_KEYBOARD = "GesturalKb";
	
	public static final String EPERIMENT_STARTED = "ExperimentStarted";
	public static final String TASK_STARTED = "TaskStarted";
	public static final String TASK_COMPLETED = "TaskCompleted";
	public static final String TASK_REVERTED = "TaskReverted";
	public static final String INPUT_EVENT = "InputEvent";

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
        XmlResourceParser in = getResources().getXml(R.xml.tasks);
        TaskParser tParser = new TaskParser();
        tasks = tParser.parse(in);
		
		super.onCreate();
	}
	
	public void initLogFile(){
		//close current log file
		if (dos != null)
			try {
				dos.close();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		
		File file = new File(Environment.getExternalStorageDirectory(), username + "_" + SEQUENCE[sequenceIndex] + "_" + fontSize + ".log");
		for (int i = 2; file.exists(); i++) {
			file = new File(Environment.getExternalStorageDirectory(), username + "_" + SEQUENCE[sequenceIndex] + "_" + fontSize + "_(" + i + ").log");
		}

		if(!file.exists()) {
			try {
				file.createNewFile();
				file.canWrite();
				file.canRead();

				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		try {
			dos = new DataOutputStream(new FileOutputStream(file,true));
			//write the first row in the log file
			writeLog(APP_NOTEPAD, EPERIMENT_STARTED, username);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeLog(String application, String event, String params) {
		try {
			if (dos!=null){
				long timestamp = new Date().getTime();
				String logRow = timestamp+"\t"+application+"\t"+event+"\t"+params;
				dos.writeBytes(logRow);
				dos.writeBytes("\n");
				//Log.i("ciao", logRow);
			}
		}

		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeLog(String timestamp, String application, String event, String params) {
		try {
			if (dos!=null){
				String logRow = timestamp+"\t"+application+"\t"+event+"\t"+params;
				dos.writeBytes(logRow);
				dos.writeBytes("\n");
				//Log.i("ciao", logRow);
			}
		}

		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeLog(String logRow) {
		try {
			if (dos!=null){
				dos.writeBytes(logRow);
				dos.writeBytes("\n");
				//Log.i("ciao", logRow);
			}
		}

		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<Task> getTasks() {
		return tasks;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public float getFontSize() {
		return fontSize;
	}

	public int getFontSizeIndex() {
		for (int i=0; i<sizes.length; i++)
			if (fontSize == sizes[i])
				return i;
		return 0;
	}

	public void setFontSize(int size) {
		this.fontSize = sizes[size];
	}	
}