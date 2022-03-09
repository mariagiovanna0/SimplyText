package com.example.android.polyGestureApp;

import unisa.NotePadApplication;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.example.android.polyGestureApp.R;

public class StartDialog extends Dialog {
	public interface ReadyListener {
		public void ready(String name, int choice);
	}

	private String name;
	private int choice;
	private ReadyListener readyListener;
	EditText etName;

	public StartDialog(Context context, String name, int choice,
			ReadyListener readyListener) {
		super(context);
		this.name = name;
		this.choice = choice;
		this.readyListener = readyListener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_dialog);
		setTitle("Start New Experiment");

		ListView list = (ListView) findViewById(R.id.ListView01);
		final NotePadApplication application = (NotePadApplication)getContext().getApplicationContext();

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getContext(), 
				android.R.layout.simple_list_item_single_choice, application.SIZE_ITEMS);
		list.setAdapter(adapter);
		list.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
		list.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView arg0, View arg1, int arg2, long arg3) {
				// TODO Auto-generated method stub
//				application.setFontSize((int)arg3);
				choice = (int)arg3;
			}
		});

		ListView list2 = (ListView) findViewById(R.id.ListView02);

		ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this.getContext(), 
				android.R.layout.simple_list_item_single_choice, application.SEQUENCE);
		list2.setAdapter(adapter2);
		list2.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
		list2.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView arg0, View arg1, int arg2, long arg3) {
				// TODO Auto-generated method stub
//				application.setFontSize((int)arg3);
				application.sequenceIndex = (int)arg3;
			}
		});
		
		Button buttonOK = (Button) findViewById(R.id.Button01);
		buttonOK.setOnClickListener(new OKListener());
		Button buttonCancel = (Button) findViewById(R.id.Button02);
		buttonCancel.setOnClickListener(new CancelListener());
		etName = (EditText) findViewById(R.id.EditText01);
		etName.setText(name == null ? "" : name.replace("finger", "").replace("stylus", ""));
	}

	private class OKListener implements android.view.View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (((ListView) findViewById(R.id.ListView01)).getCheckedItemPosition() == AdapterView.INVALID_POSITION ||
					((ListView) findViewById(R.id.ListView02)).getCheckedItemPosition() == AdapterView.INVALID_POSITION) {
				return;
			}
			String name = String.valueOf(etName.getText());
			//String regex = "[0-9]+[a-z]+_(finger|stylus)";
			if (!name.isEmpty() ) {
				readyListener.ready(name, choice);
				StartDialog.this.dismiss();
			} else {
				Toast.makeText(v.getContext(), "Inserire nome" , Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private class CancelListener implements android.view.View.OnClickListener {
		@Override
		public void onClick(View v) {
			StartDialog.this.dismiss();
		}
	}
}
