/*******************************************************************************
 * Copyright (C) 2012 The Android Money Manager Ex Project
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 ******************************************************************************/
package com.money.manager.ex;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.money.manager.ex.R;
import com.money.manager.ex.database.TableAccountList;
import com.money.manager.ex.database.TableCurrencyFormats;
import com.money.manager.ex.database.TablePayee;
import com.money.manager.ex.fragment.BaseFragmentActivity;
import com.money.manager.ex.fragment.BaseListFragment;
/**
 * 
 * @author Alessandro Lazzari (lazzari.ale@gmail.com)
 * @version 1.0.0
 * 
 */
public class CurrencyFormatsListActivity extends BaseFragmentActivity {
	@SuppressWarnings("unused")
	private static final String LOGCAT = CurrencyFormatsListActivity.class.getSimpleName();
	private static final String FRAGMENTTAG = CurrencyFormatsListActivity.class.getSimpleName() + "_Fragment";
	public static final String INTENT_RESULT_CURRENCYID = "CurrencyListActivity:ACCOUNTID";
	public static final String INTENT_RESULT_CURRENCYNAME = "CurrencyListActivity:ACCOUNTNAME";
	// ID loader
	private static final int ID_LOADER_CURRENCY = 0;
	// istance fragment list
	private CurrencyFormatsLoaderListFragment listFragment = new CurrencyFormatsLoaderListFragment();
	// database table
	private static TableCurrencyFormats mCurrency = new TableCurrencyFormats();
	private static String mAction = Intent.ACTION_EDIT;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// enabled home to come back
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		// take intent
		Intent intent = getIntent();
		if (intent != null && !(TextUtils.isEmpty(intent.getAction()))) {
			mAction = intent.getAction();
		}
		
		FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentById(android.R.id.content) == null) {
            fm.beginTransaction().add(android.R.id.content, listFragment, FRAGMENTTAG).commit();
        }
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// intercept key back
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			CurrencyFormatsLoaderListFragment fragment = (CurrencyFormatsLoaderListFragment)getSupportFragmentManager().findFragmentByTag(FRAGMENTTAG);
			if (fragment != null) {
				fragment.setResultAndFinish();
			}
		}
		return super.onKeyUp(keyCode, event);
	}
	
	public static class CurrencyFormatsLoaderListFragment extends BaseListFragment
		implements LoaderManager.LoaderCallbacks<Cursor> {
		// ID menu item
		private static final int MENU_ITEM_ADD = 1;
		// filter
		private String mCurFilter;
		private int mLayout;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			// show search into actionbar
			setShowMenuItemSearch(true);

			setEmptyText(getActivity().getResources().getString(R.string.account_empty_list));
			setHasOptionsMenu(true);
			
			mLayout = mAction.equals(Intent.ACTION_PICK) ? android.R.layout.simple_list_item_multiple_choice : android.R.layout.simple_list_item_1; 
			// associate adapter
			SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
                    mLayout, null,
                    new String[] { TableCurrencyFormats.CURRENCYNAME  },
                    new int[] { android.R.id.text1 }, 0);
			setListAdapter(adapter);
			
			registerForContextMenu(getListView());
			getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			
			setListShown(false);
			getLoaderManager().initLoader(ID_LOADER_CURRENCY, null, this);
		}
		
		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			super.onCreateOptionsMenu(menu, inflater);
			// item add
            MenuItem itemadd = menu.add(0, MENU_ITEM_ADD, MENU_ITEM_ADD, R.string.add);
            itemadd.setIcon(android.R.drawable.ic_menu_add);
            itemadd.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        }
        
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			// take cursor and move into position
			Cursor cursor = ((SimpleCursorAdapter)getListAdapter()).getCursor();
			cursor.moveToPosition(info.position);
			// set currency name
			menu.setHeaderTitle(cursor.getString(cursor.getColumnIndex(TableCurrencyFormats.CURRENCYNAME)));
			// compose menu
			String[] menuItems = getResources().getStringArray(R.array.context_menu);
			for (int i = 0; i < menuItems.length; i ++) {
				menu.add(Menu.NONE, i, i, menuItems[i]);
			}
		}
		
		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
			case android.R.id.home:
				break;
			case MENU_ITEM_ADD:
				startCurrencyFormatActivity(null);
				break;
			}
			return super.onOptionsItemSelected(item);
		}
		
		@Override
		public boolean onContextItemSelected(MenuItem item) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			// take cursor and move to position
			Cursor cursor = ((SimpleCursorAdapter)getListAdapter()).getCursor();
			cursor.moveToPosition(info.position);
			
			// check item selected
			switch (item.getItemId()) {
			case 0: //EDIT
				startCurrencyFormatActivity(cursor.getInt(cursor.getColumnIndex(TableCurrencyFormats.CURRENCYID)));
				break;
			case 1: //DELETE
				ContentValues contentValues = new ContentValues();
				contentValues.put(TableAccountList.CURRENCYID, cursor.getInt(cursor.getColumnIndex(TableCurrencyFormats.CURRENCYID)));
				if (new TablePayee().canDelete(getActivity(), contentValues, TableAccountList.class.getName())) {
					showDialogDeleteCurrency(cursor.getInt(cursor.getColumnIndex(TableCurrencyFormats.CURRENCYID)));
				} else {
					new AlertDialog.Builder(getActivity())
							.setTitle(R.string.attention)
							.setMessage(R.string.currency_can_not_deleted)
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setPositiveButton(android.R.string.ok, new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							})
							.create().show();
				}
				break;
			}
			return false;
		}
		
        @Override
        public boolean onQueryTextChange(String newText) {
            // Called when the action bar search text has changed.  Update
            // the search filter, and restart the loader to do a new query
            // with this filter.
            mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
            getLoaderManager().restartLoader(ID_LOADER_CURRENCY, null, this);
            return true;
        }

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			switch (id) {
			case ID_LOADER_CURRENCY:
				String select = null;
				if (!TextUtils.isEmpty(mCurFilter)) {
					select = TableCurrencyFormats.CURRENCYNAME + " LIKE '" + mCurFilter + "%'"; 
				}
				return new CursorLoader(getActivity(), mCurrency.getUri(), mCurrency.getAllColumns(), select, null, "upper(" + TableCurrencyFormats.CURRENCYNAME + ")");
			}

			return null;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			switch (loader.getId()) {
			case ID_LOADER_CURRENCY:
				((SimpleCursorAdapter)getListAdapter()).swapCursor(data);
	            if (isResumed()) {
	                setListShown(true);
	            } else {
	                setListShownNoAnimation(true);
	            }
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			switch (loader.getId()) {
			case ID_LOADER_CURRENCY:
				// ((SimpleCursorAdapter)getListAdapter()).swapCursor(null);
			}
		}

		private void showDialogDeleteCurrency(final int currencyId) {
			// config alert dialog
			AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
			alertDialog.setTitle(R.string.delete_currency);
			alertDialog.setMessage(R.string.confirmDelete);
			// set listener on positive button
			alertDialog.setPositiveButton(android.R.string.ok,
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (getActivity().getContentResolver().delete(mCurrency.getUri(), TableCurrencyFormats.CURRENCYID + "=" + currencyId, null) == 0) {
								Toast.makeText(getActivity(), R.string.db_delete_failed, Toast.LENGTH_SHORT).show();
							}
							// restart loader
							getLoaderManager().restartLoader(ID_LOADER_CURRENCY, null, CurrencyFormatsLoaderListFragment.this);
						}
					});
			// set listener on negative button
			alertDialog.setNegativeButton(android.R.string.cancel, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			// create dialog and show
			alertDialog.create().show();
		}
		
		@Override
		protected void setResult() {
			if (mAction.equals(Intent.ACTION_PICK)) {
				// create intent
				Intent result = new Intent();
				Cursor cursor = ((SimpleCursorAdapter)getListAdapter()).getCursor();
				
				for(int i = 0; i < getListView().getCount(); i ++) {
					if (getListView().isItemChecked(i)) {
						cursor.moveToPosition(i);
						result.putExtra(INTENT_RESULT_CURRENCYID, cursor.getInt(cursor.getColumnIndex(TableCurrencyFormats.CURRENCYID)));
						result.putExtra(INTENT_RESULT_CURRENCYNAME, cursor.getString(cursor.getColumnIndex(TableCurrencyFormats.CURRENCYNAME)));

						break;
					}
				}
				// set result and exit
				getActivity().setResult(Activity.RESULT_OK, result);
			}
			return;
		}
		
		private void startCurrencyFormatActivity(Integer currencyId) {
			// create intent, set Account ID
			Intent intent = new Intent(getActivity(), CurrencyFormatsActivity.class);
			// check transId not null
			if (currencyId != null) {
				intent.putExtra(CurrencyFormatsActivity.KEY_CURRENCY_ID, currencyId);
				intent.setAction(Intent.ACTION_EDIT);
			} else {
				intent.setAction(Intent.ACTION_INSERT);
			}
			// launch activity
			startActivity(intent);
		}
	}
}