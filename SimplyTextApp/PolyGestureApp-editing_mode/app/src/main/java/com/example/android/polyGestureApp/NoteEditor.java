/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.polyGestureApp;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import unisa.NotePadApplication;
import unisa.SpannableDiff;
import unisa.SpellChecker;
import unisa.Task;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.inputmethodservice.Keyboard;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.polyGestureApp.NotePad.Notes;

/**
 * A generic activity for editing a note in a database.  This can be used
 * either to simply view a note {@link Intent#ACTION_VIEW}, view and edit a note
 * {@link Intent#ACTION_EDIT}, or create a new note {@link Intent#ACTION_INSERT}.  
 */
public class NoteEditor extends Activity implements View.OnTouchListener
{
	private static final String TAG = "Notes";

	/**
	 * Standard projection for the interesting columns of a normal note.
	 */
	private static final String[] PROJECTION = new String[] {
		Notes._ID, // 0
		Notes.NOTE, // 1
		Notes.TITLE, // 2
	};
	/** The index of the note column */
	private static final int COLUMN_INDEX_NOTE = 1;
	//private static final int COLUMN_INDEX_TITLE = 2;

	// This is our state data that is stored when freezing.
	private static final String ORIGINAL_CONTENT = "origContent";

	// Identifiers for our menu items.
	private static final int REVERT_ID = Menu.FIRST;
	private static final int DISCARD_ID = Menu.FIRST + 1;
	private static final int DELETE_ID = Menu.FIRST + 2;

	// The different distinct states the activity can be run in.
	private static final int STATE_EDIT = 0;
	private static final int STATE_INSERT = 1;

	private int mState;
	private boolean mNoteOnly = false;
	private Uri mUri;
	private Cursor mCursor;
	public EditText mText;
	public MyNewTextView myTextView;
	public Button finish;
	private String mOriginalContent;
	private Task currentTask;
	public int cursorIndex;
	//Logger
	//	private FileHandler handler;
	//	private static Logger logger = Logger.getLogger("global");
	//	private File file = null;
	//	private String data = "vuoto";
	public SpellChecker sChecker = new SpellChecker();	
	private NotePadApplication application;


	/**
	 * A custom EditText that draws lines between each line of text that is displayed.
	 */
	public static class LinedEditText extends EditText {
		private Rect mRect;
		private Paint mPaint;
		private Task task;
		private NoteEditor ne;
		private long startTime = 0;
		private String old = "";
		private boolean taskFinished = false;
		private long firstTouchEventTime = -1;

		@Override
		public void setText(CharSequence text, BufferType type) {
			if (task == null)
				super.setText(text, type);
			else
				super.setText(SpannableDiff.highlightEditDiff(text, task.getEnd()),
						BufferType.EDITABLE);
		}

		public NoteEditor getNe() {
			return this.ne;
		}

		public void setNe(NoteEditor ne) {
			this.ne = ne;
		}

		public void setTask(Task task) {
			this.task = task;
		}

		public Task getTask() {
			return task;
		}

		public static boolean completed(String text, String objective){
			if(text.replaceAll("  ", " ").equals(objective))
				return true;
			String[] textLines = text.split("\\r?\\n");
			String[] objLines = objective.split("\\r?\\n"); 
			//Log.i("junk","mio[" + text + "][" + objective+"]");
			int minLines = Math.min(textLines.length, objLines.length);
			int maxLines = Math.max(textLines.length, objLines.length);
			int i;
			for (i=0; i<minLines; i++){
				if (!textLines[i].trim().equals(objLines[i]))
					return false;
			}
			//there can be an arbitrary number of void lines at the end of the text without affecting the test 
			while (i<maxLines){
				if (i<textLines.length && !(textLines[i].trim().length()==0))
					return false;
				if (i<objLines.length && !(objLines[i].trim().length()==0))
					return false;
			}

			return true;
		}

		@Override
		protected void onTextChanged(CharSequence text, int start, int before, int after) {
			long time = SystemClock.uptimeMillis();
			if (ne != null && !text.toString().equals(old)){
				old = text.toString();
				SpannableString checked = ne.sChecker.underlineErrors(text);
				ne.mText.setTextKeepState(checked);
			}

			if (task != null && startTime == 0 && text.toString().trim().equals(Html.fromHtml(task.getStart()).toString().trim())){
				NotePadApplication npa = (NotePadApplication) getContext().getApplicationContext();
				npa.writeLog(NotePadApplication.APP_NOTEPAD, NotePadApplication.TASK_STARTED, task.getTitle() + "\n");
				startTime = System.currentTimeMillis();
			}

			if (task != null && !taskFinished && completed(text.toString(), Html.fromHtml(task.getEnd()).toString())){
				taskFinished = true;
				long duration = System.currentTimeMillis() - startTime;
				NotePadApplication npa = (NotePadApplication) getContext().getApplicationContext();
				String mes = " in " + convertToSeconds(duration) + " seconds (at " + time + ", effective time: " + convertToSeconds(time - firstTouchEventTime) + ")";
				npa.writeLog(NotePadApplication.APP_NOTEPAD, NotePadApplication.TASK_COMPLETED, task.getTitle() + mes + "\n\n***************************************\n\n");
				Toast toast = Toast.makeText(ne, "Task Completed" + mes + "!", Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.TOP, 0, 50);
				toast.show();
//				ne.finish.setClickable(true);
			}
			NotePadApplication npa = (NotePadApplication) getContext().getApplicationContext();
			npa.writeLog(NotePadApplication.APP_NOTEPAD, NotePadApplication.INPUT_EVENT, "TextChanged '"+text+"' "+start+" "+before+" "+after + "\n");
			//        	writeLog(NotePadApplication.INPUT_EVENT, "TextChanged '"+text+"' "+start+" "+before+" "+after);
			//        	Log.i("ciao", "text changed: "+text+" "+start+" "+before+" "+after);
			super.onTextChanged(text, start, before, after);
		}

		private static String convertToSeconds(long time){
			String str = time+"";

			int lengthFrac = 3;
			if (str.length() < lengthFrac)
				lengthFrac = str.length();

			String seconds = str.substring(0, str.length()-lengthFrac);
			String millis = str.substring(str.length()-lengthFrac);

			return seconds+"\""+millis;
		}

		@Override
		public void onBeginBatchEdit() {
			NotePadApplication npa = (NotePadApplication) getContext().getApplicationContext();
			npa.writeLog(NotePadApplication.APP_NOTEPAD, NotePadApplication.INPUT_EVENT, "BeginBatchEdit\n");
			//			writeLog(NotePadApplication.INPUT_EVENT, "BeginBatchEdit");
			//			Log.i("ciao", "onBeginBatchEdit");
			super.onBeginBatchEdit();
		}

		@Override
		public void onEditorAction(int actionCode) {
			NotePadApplication npa = (NotePadApplication) getContext().getApplicationContext();
			npa.writeLog(NotePadApplication.APP_NOTEPAD, NotePadApplication.INPUT_EVENT, "EditorAction\n");
			//			writeLog(NotePadApplication.INPUT_EVENT, "EditorAction");
			//			Log.i("ciao", "onEditorAction");
			super.onEditorAction(actionCode);
		}

		@Override
		protected void onFocusChanged(boolean focused, int direction,
				Rect previouslyFocusedRect) {
			NotePadApplication npa = (NotePadApplication) getContext().getApplicationContext();
			npa.writeLog(NotePadApplication.APP_NOTEPAD, NotePadApplication.INPUT_EVENT, "FocusChanged "+focused+" "+direction+" "+previouslyFocusedRect + "\n");
			//			writeLog(NotePadApplication.INPUT_EVENT, "FocusChanged "+focused+" "+direction+" "+previouslyFocusedRect);
			//			Log.i("ciao", "onFocusChanged "+focused+" "+direction+" "+previouslyFocusedRect);
			super.onFocusChanged(focused, direction, previouslyFocusedRect);
		}

		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			NotePadApplication npa = (NotePadApplication) getContext().getApplicationContext();
			npa.writeLog(NotePadApplication.APP_NOTEPAD, NotePadApplication.INPUT_EVENT, "KeyDown "+keyCode+" "+event + "\n");
			//			writeLog(NotePadApplication.INPUT_EVENT, "KeyDown "+keyCode+" "+event);
			//			Log.i("ciao", "onKeyDown");
			return super.onKeyDown(keyCode, event);
		}

		@Override
		protected void onSelectionChanged(int selStart, int selEnd) {
			NotePadApplication npa = (NotePadApplication) getContext().getApplicationContext();
			npa.writeLog(NotePadApplication.APP_NOTEPAD, NotePadApplication.INPUT_EVENT, "SelectionChanged "+selStart+" "+selEnd + "\n");
			//			writeLog(NotePadApplication.INPUT_EVENT, "SelectionChanged "+selStart+" "+selEnd);
			//			Log.i("ciao", "onSelectionChanged "+selStart+" "+selEnd);
			super.onSelectionChanged(selStart, selEnd);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (firstTouchEventTime == -1) firstTouchEventTime = event.getEventTime();
			NotePadApplication npa = (NotePadApplication) getContext().getApplicationContext();
			npa.writeLog(NotePadApplication.APP_NOTEPAD, NotePadApplication.INPUT_EVENT, "TouchEvent (cursorPosition=" + getSelectionStart() + "): " + event);
			//			writeLog(NotePadApplication.INPUT_EVENT, "TouchEvent");
			//			Log.i("ciao", "onTouchEvent");
			return super.onTouchEvent(event);
		}

		@Override
		public void onWindowFocusChanged(boolean hasWindowFocus) {
			NotePadApplication npa = (NotePadApplication) getContext().getApplicationContext();
			npa.writeLog(NotePadApplication.APP_NOTEPAD, NotePadApplication.INPUT_EVENT, "WindowFocusChanged "+hasWindowFocus + "\n");
			//			writeLog(NotePadApplication.INPUT_EVENT, "WindowFocusChanged "+hasWindowFocus);
			//			Log.i("ciao", "onWindowFocusChanged");
			super.onWindowFocusChanged(hasWindowFocus);
		}

		// we need this constructor for LayoutInflater
		public LinedEditText(Context context, AttributeSet attrs) {
			super(context, attrs);

			mRect = new Rect();
			mPaint = new Paint();
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setColor(0x800000FF);
		}

		//        public LinedEditText(Context context) {
		//            super(context);
		//            
		//            mRect = new Rect();
		//            mPaint = new Paint();
		//            mPaint.setStyle(Paint.Style.STROKE);
		//            mPaint.setColor(0x800000FF);
		//            
		//            npa = (NotePadApplication) context.getApplicationContext();
		//        }
		//        public LinedEditText(Context context, AttributeSet attrs, int defStyle){
		//            super(context, attrs, defStyle);
		//            
		//            mRect = new Rect();
		//            mPaint = new Paint();
		//            mPaint.setStyle(Paint.Style.STROKE);
		//            mPaint.setColor(0x800000FF);
		//            
		//            npa = (NotePadApplication) context.getApplicationContext();
		//        }

		@Override
		protected void onDraw(Canvas canvas) {
/*			int count = getLineCount();
			Rect r = mRect;
			Paint paint = mPaint;

			for (int i = 0; i < count; i++) {
				int baseline = getLineBounds(i, r);

				canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
			}
*/
			super.onDraw(canvas);
		}

		@Override
		public boolean onPrivateIMECommand(String action, Bundle data){
			if (action.equals("log")){
				//        		String timestamp = data.getString(null);
				//        		String event = data.getString(null);
				//        		String params = data.getString(null);
				String all = data.getString(null);
				//    			Toast.makeText(getContext().getApplicationContext(), all, Toast.LENGTH_SHORT).show();
				NotePadApplication npa = (NotePadApplication) getContext().getApplicationContext();
				//       		npa.writeLog(timestamp, NotePadApplication.APP_KEYBOARD, event, params);
				npa.writeLog(all);
				//        	Log.i("ciao", "private IME command called: "+action+" "+data.getInt(null));
				return true;
			}
			else 
				return false;
		}
	}
//************************
	ImageButton buttonBackward, buttonForward, buttonSelect, buttonCopia, buttonTaglia, buttonIncolla, buttonSu, buttonGiu, buttonAnnulla, buttonGhost, buttonMove;
	RelativeLayout arrows;
	ViewGroup global;
	String text;
	boolean flag=false; //gestione della selezione
	//ArrayList<String> backup= new ArrayList<String>(); //salvataggio ultima modifica  mText
	String backup;
	int start=-1;
	//metodo muovere sotto usato
	public boolean moveDown(Layout layout) {
		int start = mText.getSelectionStart();
		int end = mText.getSelectionEnd();

		if (start != end) {
			int min = Math.min(start, end);
			int max = Math.max(start, end);

			mText.setSelection(max);

			if (min == 0 && max == mText.length()) {
				return false;
			}

			return true;
		} else {
			int line = layout.getLineForOffset(end);

			if (line < layout.getLineCount() - 1) {
				int move;

				if (layout.getParagraphDirection(line) ==
						layout.getParagraphDirection(line + 1)) {
					float h = layout.getPrimaryHorizontal(end);
					move = layout.getOffsetForHorizontal(line + 1, h);
				} else {
					move = layout.getLineStart(line + 1);
				}

				mText.setSelection(move);
				return true;
			}
		}

		return false;
	}
	float dX, dY;
	@Override
	public boolean onTouch(View view, MotionEvent event) {

		switch (event.getAction()) {

			case MotionEvent.ACTION_DOWN:

				dX = arrows.getX() - event.getRawX();
				dY = arrows.getY() - event.getRawY();
				break;

			case MotionEvent.ACTION_MOVE:

				arrows.animate()
						.x(event.getRawX() + dX)
						.y(event.getRawY() + dY)
						.setDuration(0)
						.start();
				break;
			default:
				return false;
		}
		return true;
	}



//************************

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		application = (NotePadApplication) getApplicationContext();


		final Intent intent = getIntent();
		//        initLogger();

/*		if(intent.getExtras() != null)
		{
			intent.setAction(Intent.ACTION_EDIT);
			mUri = Uri.parse(intent.getStringExtra("task"));
			setContentView(intent.getIntExtra("layout", -1));
			application.sequenceIndex = Math.abs(1 - application.sequenceIndex);
			application.firstSequence = false;
		}
		// Do some setup based on the action being performed.
		else
		{
*/
		final String action = intent.getAction();
		if (Intent.ACTION_EDIT.equals(action)) {
			// Requested to edit: set that state, and the data being edited.
			mState = STATE_EDIT;
			mUri = intent.getData();

		} else if (Intent.ACTION_INSERT.equals(action)) {
			// Requested to insert: set that state, and create a new entry
			// in the container.
			mState = STATE_INSERT;
			mUri = getContentResolver().insert(intent.getData(), null);

			// If we were unable to create a new note, then just finish
			// this activity.  A RESULT_CANCELED will be sent back to the
			// original activity if they requested a result.
			if (mUri == null) {
				Log.e(TAG, "Failed to insert new note into " + getIntent().getData());
				finish();
				return;
			}

			// The new entry was created, so assume all will end well and
			// set the result to be returned.
			setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

		} else {
			// Whoops, unknown action!  Bail.
			Log.e(TAG, "Unknown action, exiting");
			finish();
			return;
		}

//		}	
		List<Task> tasks = application.getTasks();
		int current = Integer.parseInt(mUri.getLastPathSegment()) - 1;
		currentTask = tasks.get(current);

		sChecker.initDictionary(Html.fromHtml(currentTask.getEnd()).toString());

		float fontSize = application.getFontSize();

		if (application.sequenceIndex == 0) {// Set the layout for this activity.  You can find it in res/layout/note_editor.xml
			setContentView(R.layout.note_editor);

			// The text view for our note, identified by its ID in the XML file.

			myTextView = (MyNewTextView) findViewById(R.id.note1);
			mText = (MyNewTextView) findViewById(R.id.note1);
			myTextView.setNe(this);

			myTextView.setTask(currentTask);

			if (fontSize != 0)
				myTextView.setTextSize(TypedValue.COMPLEX_UNIT_MM, fontSize);

		} else if (application.sequenceIndex == 1) {
			setContentView(R.layout.note_editor2);

			// The text view for our note, identified by its ID in the XML file.
			mText = (EditText) findViewById(R.id.note);
			((LinedEditText) mText).setNe(this);

			((LinedEditText) mText).setTask(currentTask);

			if (fontSize != 0)
				mText.setTextSize(TypedValue.COMPLEX_UNIT_MM, fontSize);

		} else {
			// Whoops, unknown action!  Bail.
			Log.e(TAG, "Unknown action, exiting");
			finish();
			return;
		}

		// Get the note!
		mCursor = managedQuery(mUri, PROJECTION, null, null, null);


		// If an instance of this activity had previously stopped, we can
		// get the original text it started with.
		if (savedInstanceState != null) {
			mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
		}

		//load task info
		//        XmlResourceParser taskParser = getResources().getXml(R.xml.tasks);

//		finish.setClickable(false);

		//*************************************************************************************************
		if (application.sequenceIndex == 0) {
			buttonBackward = (ImageButton) findViewById(R.id.buttonBackward);
			buttonForward = (ImageButton) findViewById(R.id.buttonForward);
			buttonSu = (ImageButton) findViewById(R.id.buttonSu);
			buttonGiu = (ImageButton) findViewById(R.id.buttonGiu);
			buttonSelect = (ImageButton) findViewById(R.id.buttonSelect);
			buttonCopia = (ImageButton) findViewById(R.id.buttonCopia);
			buttonTaglia = (ImageButton) findViewById(R.id.buttonTaglia);
			buttonIncolla = (ImageButton) findViewById(R.id.buttonIncolla);
			buttonAnnulla = (ImageButton) findViewById(R.id.buttonAnnulla);
			buttonMove = (ImageButton) findViewById(R.id.buttonmove);
			buttonGhost= (ImageButton) findViewById(R.id.buttonghost);
			arrows = (RelativeLayout) findViewById(R.id.arrows);
			global = (ViewGroup) findViewById(R.id.sample_main_layout);
			buttonMove.setOnTouchListener(this);
			mText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

				}

				@Override
				public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
					//TODO
				}

				@Override
				public void afterTextChanged(Editable editable) {
					buttonCopia.setVisibility(View.INVISIBLE);
					buttonTaglia.setVisibility(View.INVISIBLE);
					if(buttonIncolla.getVisibility()==View.INVISIBLE)
						buttonSelect.setVisibility(View.VISIBLE);
					buttonAnnulla.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
					flag=false;
				}
			});
			global.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (start != -1)
						mText.setSelection(start, start);
					start = -1;
					}
			});

			mText.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					arrows.setVisibility(View.VISIBLE);
					backup=mText.getText().toString();
					if (start != -1 && buttonIncolla.getVisibility() == View.INVISIBLE && buttonSelect.getVisibility()== View.INVISIBLE) {
						mText.setSelection(start, mText.getSelectionEnd());
					}
				}
			});
			buttonGhost.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					arrows.setVisibility(View.INVISIBLE);
				}
			});

		buttonAnnulla.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if(backup.equals(mText.getText().toString())==false)
					{
						mText.setText(backup);
						buttonAnnulla.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
					}
					else {
						if (buttonSelect.getVisibility() == View.VISIBLE) {
							flag = false;
							if (mText.getSelectionStart() != mText.getSelectionEnd()) {
								mText.setSelection(start, start);
								buttonAnnulla.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
							} else {
								//arrows.setVisibility(View.INVISIBLE);
								buttonAnnulla.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
								start = -1;
							}
						}
						if (buttonSelect.getVisibility() == View.INVISIBLE) {
							flag = true;
							buttonSelect.setVisibility(View.VISIBLE);
							buttonCopia.setVisibility(View.INVISIBLE);
							buttonTaglia.setVisibility(View.INVISIBLE);
						}
						if (buttonIncolla.getVisibility() == View.VISIBLE) {
							//buttonCopia.setVisibility(View.VISIBLE);
							buttonIncolla.setVisibility(View.INVISIBLE);
							buttonSelect.setVisibility(View.VISIBLE);
							//buttonTaglia.setVisibility(View.VISIBLE);
						}
					}

				}
			});
			buttonForward.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mText.getSelectionEnd() < mText.getText().toString().length()) {
						if (buttonSelect.getVisibility() == View.INVISIBLE && flag == true) {
							mText.setSelection(start, mText.getSelectionEnd() + 1);
						} else {
							mText.setSelection(mText.getSelectionEnd() + 1);
						}

					} else {
						//fine stringa, non posso muovere il cursore
					}
				}
			});

			buttonBackward.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mText.getSelectionStart() > 0) {
						if (buttonSelect.getVisibility() == View.INVISIBLE && flag == true) {
							mText.setSelection(start, mText.getSelectionEnd() - 1);
						} else {
							mText.setSelection(mText.getSelectionEnd() - 1);
						}

					} else {
						//fine stringa non posso muovere il cursore
					}
				}
			});

			buttonSu.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Layout layout = mText.getLayout();
					int end = mText.getSelectionEnd();
					int move=0;
					if (false) {
						int min = Math.min(start, end);
						int max = Math.max(start, end);
						mText.setSelection(max);
						if (min == 0 && max == mText.length()) {
						}
					}

					else {
						int line = layout.getLineForOffset(end);

						if (line <= layout.getLineCount() - 1 && line>0) {

							if (layout.getParagraphDirection(line) ==
									layout.getParagraphDirection(line - 1)) {
								float h = layout.getPrimaryHorizontal(end);
								move = layout.getOffsetForHorizontal(line - 1, h);
							} else {
								move = layout.getLineStart(line - 1);
							}
							if (buttonSelect.getVisibility() == View.INVISIBLE && flag == true) {
								mText.setSelection(start,move);
							}
							else
							{mText.setSelection(move);}


						}
					}

				}
			});
			
			buttonGiu.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {

					Layout layout = mText.getLayout();
						int start = mText.getSelectionStart();
						int end = mText.getSelectionEnd();

						if (false) {
							int min = Math.min(start, end);
							int max = Math.max(start, end);

							mText.setSelection(max);

							if (min == 0 && max == mText.length()) {

							}
						} else {
							int line = layout.getLineForOffset(end);

							if (line < layout.getLineCount() - 1) {
								int move;

								if (layout.getParagraphDirection(line) ==
										layout.getParagraphDirection(line + 1)) {
									float h = layout.getPrimaryHorizontal(end);
									move = layout.getOffsetForHorizontal(line + 1, h);
								} else {
									move = layout.getLineStart(line + 1);
								}
								if (buttonSelect.getVisibility() == View.INVISIBLE && flag == true) {
									mText.setSelection(start,move);
								}
								else
								{mText.setSelection(move);}
							}
						}

				}
			});

			buttonSelect.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if(false)
					    arrows.setVisibility(View.INVISIBLE);
					start = mText.getSelectionStart();
					flag = true;
					buttonSelect.setVisibility(View.INVISIBLE);

					buttonCopia.setVisibility(View.VISIBLE);
					buttonTaglia.setVisibility(View.VISIBLE);
					buttonAnnulla.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
				}
			});

			buttonCopia.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					flag = false;
					if(false) arrows.setVisibility(View.INVISIBLE);//new
					buttonCopia.setVisibility(View.INVISIBLE);
					buttonTaglia.setVisibility(View.INVISIBLE);
					buttonIncolla.setVisibility(View.VISIBLE);
					if (mText.getSelectionStart() <= mText.getSelectionEnd()) {
						text = mText.getText().toString().substring(mText.getSelectionStart(), mText.getSelectionEnd());
					} else {
						text = mText.getText().toString().substring(mText.getSelectionEnd(), mText.getSelectionStart());
					}
				}
			});

			buttonTaglia.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					flag = false;
					if(false) arrows.setVisibility(View.INVISIBLE);
					buttonCopia.setVisibility(View.INVISIBLE);
					buttonTaglia.setVisibility(View.INVISIBLE);
					buttonIncolla.setVisibility(View.VISIBLE);
					buttonSelect.setVisibility(View.INVISIBLE);
					String myText = mText.getText().toString();
					int s=0;
					if (mText.getSelectionStart() <= mText.getSelectionEnd()) {
						s=mText.getSelectionStart();
						text = mText.getText().toString().substring(mText.getSelectionStart(), mText.getSelectionEnd());
						myText = myText.substring(0, mText.getSelectionStart()) + myText.substring(mText.getSelectionEnd(), mText.length());
					} else {
						s=mText.getSelectionEnd();
						text = mText.getText().toString().substring(mText.getSelectionEnd(), mText.getSelectionStart());
						myText = myText.substring(0, mText.getSelectionEnd()) + myText.substring(mText.getSelectionStart(), mText.length());
					}
					mText.setText(myText);
					mText.setSelection(s);
				}
			});

			buttonIncolla.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if(false)arrows.setVisibility(View.INVISIBLE);
					buttonIncolla.setVisibility(View.INVISIBLE);
					buttonSelect.setVisibility(View.VISIBLE);
					mText.getText().insert(mText.getSelectionStart(), text);
					start = -1;

				}
			});

		}

		
           //frecce tasti volume
	/*	@Override
		public boolean dispatchKeyEvent (KeyEvent event){
			int action = event.getAction();
			int keyCode = event.getKeyCode();
			switch (keyCode) {
				//destra
				case KeyEvent.KEYCODE_VOLUME_UP:
					if (action == KeyEvent.ACTION_DOWN) {
						mText.setSelection(mText.getSelectionEnd() + 1);
					}
					return true;
				//sinistra
				case KeyEvent.KEYCODE_VOLUME_DOWN:
					if (action == KeyEvent.ACTION_DOWN) {
						mText.setSelection(mText.getSelectionEnd() - 1);
					}
					return true;
				default:
					return super.dispatchKeyEvent(event);
			}
		}*/
	}
//******************************************************************************************************


	@Override
	protected void onResume() {
		super.onResume();

		// If we didn't have any trouble retrieving the data, it is now
		// time to get at the stuff.
		if (mCursor != null) {
			// Make sure we are at the one and only row in the cursor.
			mCursor.moveToFirst();

			// Modify our overall title depending on the mode we are running in.
			if (mState == STATE_EDIT) {
				setTitle(getText(R.string.title_edit));
			} else if (mState == STATE_INSERT) {
				setTitle(getText(R.string.title_create));
			}

			// This is a little tricky: we may be resumed after previously being
			// paused/stopped.  We want to put the new text in the text view,
			// but leave the user where they were (retain the cursor position
			// etc).  This version of setText does that for us.
			String note = mCursor.getString(COLUMN_INDEX_NOTE);
			if(application.sequenceIndex == 0)
			{
				myTextView.setTextKeepState(Html.fromHtml(note));
				myTextView.setSelection(myTextView.getText().length());
				myTextView.postDelayed(new Runnable() {
					@Override
					public void run() {
						((InputMethodManager) myTextView.getContext()
								.getSystemService(Context.INPUT_METHOD_SERVICE))
								.hideSoftInputFromWindow(getCurrentFocus()
										.getWindowToken(), 0);
					}
				}, 100);
			}
			else if(application.sequenceIndex == 1)
			{
				mText.setTextKeepState(Html.fromHtml(note));
				mText.setSelection(mText.getText().length());
				mText.postDelayed(new Runnable() {
					@Override
					public void run() {
						((InputMethodManager) mText.getContext()
								.getSystemService(Context.INPUT_METHOD_SERVICE))
								.hideSoftInputFromWindow(getCurrentFocus()
										.getWindowToken(), 0);
					}
				}, 100);
			}
			else
			{
				// Whoops, unknown action!  Bail.
				Log.e(TAG, "Unknown action, exiting");
				finish();
				return;
			}

			// If we hadn't previously retrieved the original text, do so
			// now.  This allows the user to revert their changes.
			if (mOriginalContent == null) {
				mOriginalContent = note;
			}

		} else {
			setTitle(getText(R.string.error_title));
			if(application.sequenceIndex == 0)
			{
				myTextView.setText(getText(R.string.error_message));
			}
			else if(application.sequenceIndex == 1)
			{
				mText.setText(getText(R.string.error_message));    
			}
		}
		//        String title = mCursor.getString(COLUMN_INDEX_NOTE);

		KeyboardModeBroadcastReceiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent){
				String action = intent.getAction();

				switch (action){
					case "keyboard_mode": {
						boolean editingMode = intent.getBooleanExtra("editingMode", false);
						if (myTextView != null) {
							myTextView.onModeChange(editingMode);
						}
						break;
					}
					case "keyboard_action": {
						String menuAction = intent.getStringExtra("action");
						switch (menuAction) {
							case "copy": onClickCopy(); break;
							case "cut": onClickCut(); break;
							case "paste": onClickPaste(); break;
							default: break;
						}

						break;
					}
					case "keyboard_event": {
						long eventTime = intent.getLongExtra("eventTime", 0);
						long downTime = intent.getLongExtra("downTime", 0);
						int action1 = intent.getIntExtra("action", 0);
						float pointX = intent.getFloatExtra("x", 0);
						float pointY = intent.getFloatExtra("y", 0);
						int metaState = intent.getIntExtra("metaState", 0);

						if (myTextView != null) {
							myTextView.onKeyboradEvent(eventTime, downTime, action1, pointX, pointY, metaState);
						}
						break;
					}
				}
			}

		};

		registerReceiver(KeyboardModeBroadcastReceiver,new IntentFilter("keyboard_mode"));
		registerReceiver(KeyboardModeBroadcastReceiver,new IntentFilter("keyboard_action"));
		registerReceiver(KeyboardModeBroadcastReceiver,new IntentFilter("keyboard_event"));
	}

	BroadcastReceiver KeyboardModeBroadcastReceiver;



	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Save away the original text, so we still have it if the activity
		// needs to be killed while paused.
		outState.putString(ORIGINAL_CONTENT, mOriginalContent);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// The user is going somewhere else, so make sure their current
		// changes are safely saved away in the provider.  We don't need
		// to do this if only editing.
		if (mCursor != null) {
			String text = "";
//			try{
				if(application.sequenceIndex == 0)//if(Math.abs(1 - application.sequenceIndex) == 0)
					text = myTextView.getText().toString();
				else if(application.sequenceIndex == 1)//else if(Math.abs(1 - application.sequenceIndex) == 1)
					text = mText.getText().toString();
				else
				{
					// Whoops, unknown action!  Bail.
					Log.e(TAG, "Unknown action, exiting");
					finish();
					return;
				}
/*			}catch(NullPointerException e){
				Toast toast = Toast.makeText(this, "Back Button pressed. Exiting from app", Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.TOP, 0, 50);
				toast.show();
				return;
			}
*/			
			int length = text.length();

			// If this activity is finished, and there is no text, then we
			// do something a little special: simply delete the note entry.
			// Note that we do this both for editing and inserting...  it
			// would be reasonable to only do it when inserting.
			if (isFinishing() && (length == 0) && !mNoteOnly) {
				setResult(RESULT_CANCELED);
				deleteNote();

				// Get out updates into the provider.
			} else {
				ContentValues values = new ContentValues();

				// This stuff is only done when working with a full-fledged note.
				if (!mNoteOnly) {
					// Bump the modification time to now.
					values.put(Notes.MODIFIED_DATE, System.currentTimeMillis());

					// If we are creating a new note, then we want to also create
					// an initial title for it.
					if (mState == STATE_INSERT) {
						String title = text.substring(0, Math.min(30, length));
						if (length > 30) {
							int lastSpace = title.lastIndexOf(' ');
							if (lastSpace > 0) {
								title = title.substring(0, lastSpace);
							}
						}
						values.put(Notes.TITLE, title);
					}
					else if (mState == STATE_EDIT) {
						if(application.sequenceIndex == 0)
						{
							if (MyNewTextView.completed(text, Html.fromHtml(currentTask.getEnd()).toString()))
								values.put(Notes.STATUS, Notes.STATUS_COMPLETED);

							else
								values.put(Notes.STATUS, Notes.STATUS_FAILED);
						}
						else if(application.sequenceIndex == 1)
						{
							if (LinedEditText.completed(text, Html.fromHtml(currentTask.getEnd()).toString()))
								values.put(Notes.STATUS, Notes.STATUS_COMPLETED);

							else
								values.put(Notes.STATUS, Notes.STATUS_FAILED);
						}
					}
				}

				// Write our text back into the provider.
				values.put(Notes.NOTE, text);
				// Commit all of our changes to persistent storage. When the update completes
				// the content provider will notify the cursor of the change, which will
				// cause the UI to be updated.
				getContentResolver().update(mUri, values, null, null);
			}
		}

		unregisterReceiver(KeyboardModeBroadcastReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// Build the menus that are shown when editing.
		if (mState == STATE_EDIT) {
			menu.add(0, REVERT_ID, 0, R.string.menu_revert)
			.setShortcut('0', 'r')
			.setIcon(android.R.drawable.ic_menu_revert);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		switch (item.getItemId()) {
		case DELETE_ID:
			deleteNote();
			finish();
			break;
		case DISCARD_ID:
			cancelNote();
			break;
		case REVERT_ID:
			cancelNote();
			NotePadApplication npa = (NotePadApplication) getApplicationContext();
			if(application.sequenceIndex == 0)
				npa.writeLog(NotePadApplication.APP_NOTEPAD, NotePadApplication.TASK_REVERTED, myTextView.getTask().getTitle() + "\n");
			else if(application.sequenceIndex == 1)
				npa.writeLog(NotePadApplication.APP_NOTEPAD, NotePadApplication.TASK_REVERTED, ((LinedEditText)mText).getTask().getTitle() + "\n");
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Take care of canceling work on a note.  Deletes the note if we
	 * had created it, otherwise reverts to the original text.
	 */
	private final void cancelNote() {
		if (mCursor != null) {
			if (mState == STATE_EDIT) {
				// Put the original note text back into the database
				mCursor.close();
				mCursor = null;
				ContentValues values = new ContentValues();
				values.put(Notes.NOTE, mOriginalContent);
				getContentResolver().update(mUri, values, null, null);
			} else if (mState == STATE_INSERT) {
				// We inserted an empty note, make sure to delete it
				deleteNote();
			}
		}
		setResult(RESULT_CANCELED);
		finish();
	}

	/**
	 * Take care of deleting a note.  Simply deletes the entry.
	 */
	private final void deleteNote() {
		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
			getContentResolver().delete(mUri, null, null);
			if(application.sequenceIndex == 0)
				myTextView.setText("");
			else if(application.sequenceIndex == 1)
				mText.setText("");
		}
	}

	public NotePadApplication getApp() {
		return application;
	}

/*
	public void nextClick(View v){
		if(application.firstSequence)
		{
			Intent intent = new Intent(this.getApplicationContext(), NoteEditor.class);
			if(application.sequenceIndex == 0)
			{
				intent.putExtra("layout", R.layout.note_editor2);
			}
			else if(application.sequenceIndex == 1)
			{
				intent.putExtra("layout", R.layout.note_editor);
				//setContentView(R.layout.note_editor);
			}
			cancelNote();
			intent.putExtra("task", mUri.toString());
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			application.startActivity(intent);
		}
		else
		{
			//cancelNote();
			application.firstSequence = true;
			application.sequenceIndex = Math.abs(1 - application.sequenceIndex);
			application.stopService(getIntent());
			finish();
		}
	}
*/
	//	public void initLogger() {
	//		if(data.equals("vuoto") && file == null) {
	//			long dt = new Date().getTime();
	//			data = Long.toString(dt);
	//
	//			file = new File(Environment.getExternalStorageDirectory() , "NoteEditor.log");
	//			if(!file.exists()) {
	//				try {
	//					file.createNewFile();
	//					file.canWrite();
	//					file.canRead();
	//				} catch (IOException e1) {
	//					e1.printStackTrace();
	//				}
	//			}
	//
	//			String pattern = file.getAbsolutePath();
	//			try { 
	//				handler = new FileHandler(pattern, true); 
	//				handler.setFormatter(new MyFormatter());
	//				logger.addHandler(handler); 
	//			} catch (IOException e) { 
	//				e.printStackTrace(); 
	//			}
	//		}
	//	}
	//    
	//	public static void writeLog(String movimento,String log) {
	//
	//		LogRecord record = new LogRecord(Level.INFO, movimento + " - " + log); 
	//		logger.log(record);
	//
	//	}

	//	public void onBackPressed() {
	////		startActivity(new Intent(Intent.ACTION_MAIN, getIntent().getData()));
	////		if (!remove){
	////			super.onBackPressed();
	////		}else{
	////		super.onBackPressed();
	//			Intent goToA = new Intent(Intent.ACTION_VIEW);
	//			goToA.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	//			startActivity(goToA);
	////		}
	//	}

	public void onClickCopy() {
		if (myTextView != null) {
			myTextView.onClickCopy();
		}
	}

	public void onClickCut() {
		if (myTextView != null) {
			myTextView.onClickCut();
		}
	}

	public void onClickPaste() {
		if (myTextView != null) {
			myTextView.onClickPaste();
		}
	}
}