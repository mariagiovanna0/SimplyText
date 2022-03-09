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

import java.util.List;

import unisa.NotePadApplication;
import unisa.Task;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.example.android.polyGestureApp.R;
import com.example.android.polyGestureApp.NotePad.Notes;

/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the intent if there is one, otherwise defaults to displaying the
 * contents of the {@link NotePadProvider}
 */
public class NotesList extends ListActivity implements android.view.View.OnClickListener{
    private static final String TAG = "NotesList";

    public static final String RESET_ACTION = "com.android.notepad.action.RESET";
    
    // Menu item ids
    public static final int MENU_ITEM_DELETE = Menu.FIRST;
    public static final int MENU_ITEM_INSERT = Menu.FIRST + 1;
    public static final int MENU_ITEM_RESET = Menu.FIRST + 2;
    public static final int MENU_ITEM_RESET_NOTE = Menu.FIRST + 3;
    public static final int MENU_ITEM_QUIT = Menu.FIRST + 4;

    /**
     * The columns we are interested in from the database
     */
    private static final String[] PROJECTION = new String[] {
            Notes._ID, // 0
            Notes.TITLE, // 1
            Notes.STATUS, // 2
    };

    /** The index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_STATUS = 2;
    
    //click in the StartDialog
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        // If no data was given in the intent (because we were started
        // as a MAIN activity), then use our default content provider.
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(Notes.CONTENT_URI);
        }
        
        final String action = intent.getAction();
        if (RESET_ACTION.equals(action)) {
        	
	        NotePadApplication application = (NotePadApplication)getApplicationContext();
	        
	        StartDialog myDialog = new StartDialog(this, application.getUsername(),
	        		application.getFontSizeIndex(), new OnReadyListener());
            myDialog.show();
        
        }
        

        // Inform the list we provide context menus for items
        getListView().setOnCreateContextMenuListener(this);
        
        // Perform a managed query. The Activity will handle closing and requerying the cursor
        // when needed.
        Cursor cursor = managedQuery(getIntent().getData(), PROJECTION, null, null,
//                Notes.DEFAULT_SORT_ORDER);
                Notes.CREATION_SORT_ORDER);
        
        // Used to map notes entries from the database to views
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.noteslist_item, cursor,
                new String[] { Notes.TITLE, Notes.STATUS}, new int[] { android.R.id.text1, android.R.id.text2});
        setListAdapter(adapter);
    }
    
    private class OnReadyListener implements StartDialog.ReadyListener {
        @Override
        public void ready(String name, int choice) {
        	NotePadApplication application = (NotePadApplication)getApplicationContext();
        	application.setUsername(name);
        	application.setFontSize(choice);
        	application.initLogFile();
//        	Toast.makeText(getApplicationContext(), name+" "+choice, Toast.LENGTH_SHORT).show();
        	
        	
            Intent intent = getIntent();
            if (intent.getData() == null) {
                intent.setData(Notes.CONTENT_URI);
            }
	        ContentValues values = new ContentValues();
	        getContentResolver().delete(intent.getData(), "", null);
	        

	        List<Task> tasks = application.getTasks();
	        
	        for(int i=0; i<tasks.size(); i++){
	        	Task task = tasks.get(i);
		        
		        values.put(Notes.MODIFIED_DATE, System.currentTimeMillis());
		        values.put(Notes.TITLE, task.getTitle());
		        values.put(Notes.NOTE, task.getStart());
		        values.put(Notes.STATUS, Notes.STATUS_CREATED);
		        getContentResolver().insert(intent.getData(), values);
	        }
        }
    }
    
//    public boolean confirm(String title){
//    	AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
//    	alertbox.setMessage(title);
//    	alertbox.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//    		public void onClick(DialogInterface arg0, int arg1) {
//    			//Toast.makeText(getApplicationContext(), "'Yes' button clicked", Toast.LENGTH_SHORT).show();
//    		};
//    	});
//        alertbox.setNegativeButton("No", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface arg0, int arg1) {
//                Toast.makeText(getApplicationContext(), "'No' button clicked", Toast.LENGTH_SHORT).show();
//            }
//        });
//    	alertbox.show();
//    }

    @Override
    public void onBackPressed() {
        openOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // This is our one standard application action -- inserting a
        // new note into the list.
//        menu.add(0, MENU_ITEM_INSERT, 0, R.string.menu_insert)
//                .setShortcut('3', 'a')
//                .setIcon(android.R.drawable.ic_menu_add);
        
        menu.add(0, MENU_ITEM_RESET, 0, R.string.menu_reset)
        		.setShortcut('4', 'r')
        		.setIcon(android.R.drawable.ic_menu_revert);

        menu.add(0, MENU_ITEM_QUIT, 0, R.string.menu_quit)
                .setIcon(android.R.drawable.ic_menu_close_clear_cancel);

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return true;
    }

//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        super.onPrepareOptionsMenu(menu);
//        final boolean haveItems = getListAdapter().getCount() > 0;
//
//        // If there are any notes in the list (which implies that one of
//        // them is selected), then we need to generate the actions that
//        // can be performed on the current selection.  This will be a combination
//        // of our own specific actions along with any extensions that can be
//        // found.
//        if (haveItems) {
//            // This is the selected item.
//            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());
//
//            // Build menu...  always starts with the EDIT action...
//            Intent[] specifics = new Intent[1];
//            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);
//            MenuItem[] items = new MenuItem[1];
//
//            // ... is followed by whatever other actions are available...
//            Intent intent = new Intent(null, uri);
//            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
//            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0, null, specifics, intent, 0,
//                    items);
//
//            // Give a shortcut to the edit action.
//            if (items[0] != null) {
//                items[0].setShortcut('1', 'e');
//            }
//        } else {
//            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
//        }
//
//        return true;
//    }

    @Override
    // queste azioni sono state disattivate, il codice non è raggiungibile
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case MENU_ITEM_INSERT:
    		// Launch activity to insert a new item
    		startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
    		return true;

    	case MENU_ITEM_RESET:
    		// Launch activity to reset notes list
    		startActivity(new Intent(RESET_ACTION, getIntent().getData()));
    		finish();
    		return true;

        case MENU_ITEM_QUIT:
    		finish();
    		return true;
        }
    	return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Setup the menu header
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Add a menu item to delete the note
//        menu.add(0, MENU_ITEM_DELETE, 0, R.string.menu_delete);
        
        // Add a menu item to reset the note
        menu.add(0, MENU_ITEM_RESET_NOTE, 0, R.string.menu_reset_note);
    }
        
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
            case MENU_ITEM_DELETE: {
                // Delete the note that the context menu is for
                Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);
                getContentResolver().delete(noteUri, null, null);
                return true;
            }
            case MENU_ITEM_RESET_NOTE: {
            	Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);
            	ContentValues values = new ContentValues();
    	        NotePadApplication application = (NotePadApplication)getApplicationContext();
    	        Task task = application.getTasks().get(info.position);
  		        
   		        values.put(Notes.MODIFIED_DATE, System.currentTimeMillis());
  		        values.put(Notes.TITLE, task.getTitle());
   		        values.put(Notes.NOTE, task.getStart());
   		        values.put(Notes.STATUS, Notes.STATUS_CREATED);
   		        getContentResolver().update(noteUri, values, null, null);        	
            }
        }
        return false;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
        
        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            // The caller is waiting for us to return a note selected by
            // the user.  The have clicked on one, so return it now.
            setResult(RESULT_OK, new Intent().setData(uri));
        } 
        else{
        	// Launch activity to view/edit the currently selected item
        	Cursor cursor = (Cursor) getListAdapter().getItem(position);
        	if (cursor == null) {
        		// For some reason the requested item isn't available, do nothing
        		return;
        	}

        	// Setup the menu header
        	if (cursor.getString(COLUMN_INDEX_STATUS).equals(NotePad.Notes.STATUS_CREATED))
        		startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }

}
