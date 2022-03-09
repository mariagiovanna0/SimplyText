package unisa;

import com.example.android.polyGestureApp.NotePad.Notes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.TextView;

public class StatusView extends TextView {

	

	public StatusView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub

	}


	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		String text = getText().toString();
		
		if (text.equals(Notes.STATUS_COMPLETED)){
			setTextColor(Color.GREEN);
		}
		else if (text.equals(Notes.STATUS_FAILED)){
			setTextColor(Color.RED);
		}
	
		super.onDraw(canvas);
	}
	
	

}
