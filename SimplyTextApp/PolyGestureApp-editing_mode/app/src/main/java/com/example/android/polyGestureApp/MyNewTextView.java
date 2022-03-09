package com.example.android.polyGestureApp;

import it.unisa.di.cluelab.polyrec.Gesture;
import it.unisa.di.cluelab.polyrec.PolyRecognizerGSS;
import it.unisa.di.cluelab.polyrec.geom.Rectangle2D;
import it.unisa.di.cluelab.polyrec.Result;
import it.unisa.di.cluelab.polyrec.TPoint;

import java.util.regex.*;
import java.util.ArrayList;
import java.util.Map.Entry;

import unisa.NotePadApplication;
import unisa.SpannableDiff;
import unisa.Task;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Scroller;
import android.widget.Toast;

import static android.content.Context.CLIPBOARD_SERVICE;

public class MyNewTextView extends EditText {

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
    public MyNewTextView(Context context, AttributeSet attrs) {
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

    public void onModeChange(boolean editingMode) {
        //TODO
    }

    public void onKeyboradEvent(long eventTime, long downTime, int action1, float pointX, float pointY, int metaState) {
        //TODO
    }

    public void onClickCopy() {
        //TODO
    }

    public void onClickCut() {
        //TODO
    }

    public void onClickPaste() {
        //TODO
    }
}