/*
 * Copyright (C) 2013 Andre Gregori and Mark Garro 
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

package com.amgregori.androidgo;

import java.util.Collection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

/**
 * UI stuff.
 * 
 */
public class MainActivity extends SherlockActivity {
	//instance variables
	private Game game;
	private int boardSize;
	private Typeface fontAwesome;
	private GridView gridView;
	private TextView whiteCount;
	private TextView blackCount;
	private MenuItem passItem;

	//constants
	private static final String GAME_KEY = "game";
	private static final String BOARD_SIZE_KEY = "board_size";

	/**
	 * Sets up board, buttons, menus, etc.
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		setContentView(R.layout.activity_main);

		gridView = (GridView) findViewById(R.id.gridview);
		whiteCount = (TextView) findViewById(R.id.white_taken);
		blackCount = (TextView) findViewById(R.id.black_taken);
		fontAwesome = Typefaces.get(this, "fonts/fontawesome-webfont.ttf");

		if(savedInstanceState == null){
			game = newGameFromSettings();
		}else{
			game = savedInstanceState.getParcelable(GAME_KEY);
			boardSize = savedInstanceState.getInt(BOARD_SIZE_KEY);
		}
		refreshCaptured();
		setupBoard();

		SparseIntArray historyButtons = new SparseIntArray();
		historyButtons.put(R.id.controlButtonFirst, Game.FIRST);
		historyButtons.put(R.id.controlButtonPrev, Game.PREVIOUS);
		historyButtons.put(R.id.controlButtonNext, Game.NEXT);
		historyButtons.put(R.id.controlButtonLast, Game.LAST);

		for(int i = 0; i < historyButtons.size(); i++){
			int resourceId = historyButtons.keyAt(i);
			final int stepDirection = historyButtons.valueAt(i);

			Button button = (Button) findViewById(resourceId);
			button.setTypeface(fontAwesome);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					game.stepHistory(stepDirection);
					refreshCaptured();
					refreshPassItem();
					setupBoard();
				}
			});
		}
	}

	private ImageAdapter setupBoard(){
		ImageAdapter adapter = new ImageAdapter(this);
		gridView.setNumColumns(boardSize);
		gridView.setAdapter(adapter);
		return adapter;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState){
		outState.putParcelable(GAME_KEY, game);
		outState.putInt(BOARD_SIZE_KEY, boardSize);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		game = savedInstanceState.getParcelable(GAME_KEY);
		boardSize = savedInstanceState.getInt(BOARD_SIZE_KEY);
	}

	// Accessor methods

	/**
	 * Get the <code>Game</code> object currently referenced by this UI.
	 * @return	Current <code>Game</code>
	 */
	protected Game getGame(){
		return game;
	}

	/**
	 * Return the current game's board size.
	 * @return	Number of vertical or horizontal lines.
	 * For example, a 19x19 board will return 19.
	 */
	protected int getBoardSize(){
		return boardSize;
	}

	// other methods

	/**
	 * Update the <code>TextView</code>s displaying the number of
	 * captured stones.
	 */
	protected void refreshCaptured(){
		whiteCount.setText(String.valueOf(game.getCapturedStones(Game.WHITE)));
		blackCount.setText(String.valueOf(game.getCapturedStones(Game.BLACK)));		
	}

	private Game newGameFromSettings(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int koRule = prefs.getString("ko","0").equals("0") ? Game.SITUATIONAL : Game.POSITIONAL;
		boolean suicideRule = prefs.getString("suicide","0").equals("1") ? true : false;
		boardSize = Integer.parseInt(prefs.getString("board_size", "19"));

		return new Game(koRule, suicideRule, boardSize);
	}

	private void refreshPassItem(){
		if(game.isRunning()){
			passItem.setEnabled(true);
			passItem.setTitle(R.string.pass_turn);
		}else{
			passItem.setEnabled(false);
			passItem.setTitle(R.string.game_over);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		passItem = menu.add(R.string.pass_turn)
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT)
				.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						game.passTurn();
						refreshPassItem();
						return true;
					}
				});;

				SubMenu subMenu1 = menu.addSubMenu("Action Item");
				subMenu1.add(R.string.new_game)
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT)
				.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						game = newGameFromSettings();
						refreshCaptured();
						setupBoard();
						refreshPassItem();
						return true;
					}
				});
				subMenu1.add("Settings").setIntent(new Intent(this, Settings.class));
				subMenu1.add("About").setIntent(new Intent(this, AboutActivity.class));

				MenuItem subMenu1Item = subMenu1.getItem();
				subMenu1Item.setIcon(R.drawable.ic_action_settings)
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);

				return super.onCreateOptionsMenu(menu);
	}
	
	/**
	 * 
	 * Adapter used the update the board graphics.
	 *
	 */
	private class ImageAdapter extends BaseAdapter {
		private MainActivity mainActivity;
		private int[] mThumbIds;

		public ImageAdapter(Context c) {
			mainActivity = (MainActivity) c;
			char[] position = mainActivity.getGame().getPosition();
			mThumbIds =  new int[position.length];

			for(int i = 0; i < position.length; i++){
				mThumbIds[i] = makeImageResource(i, position[i]);
			}
		}

		public int getCount() {
			return mThumbIds.length;
		}

		public void setImageResource(int index, char color){
			mThumbIds[index] =  makeImageResource(index, color);
		}

		public Object getItem(int index) {
			return null;
		}

		public long getItemId(int index) {
			return 0;
		}

		// create a new ImageView for each item referenced by the Adapter
		@SuppressLint("NewApi")
		public View getView(int index, View convertView, ViewGroup parent) {
			ImageView imageView;
			if (convertView == null) {  // if it's not recycled, initialize some attributes
				imageView = new ImageView(mainActivity);
				int height;
				if (Build.VERSION.SDK_INT >= 16){
					height = ((GridView) parent).getColumnWidth();
				}else{
					height = ((GridView) parent).getWidth() / mainActivity.getBoardSize();
				}
				imageView.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.MATCH_PARENT, height));
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setPadding(0, 0, 0, 0);
				imageView.setOnClickListener(new ImageView.OnClickListener() {
					@Override
					public void onClick(View iV) {
						// TODO Auto-generated method stub
						Game game = mainActivity.getGame();
						int index =  ((GridView) iV.getParent()).getPositionForView(iV);
						updateBoard(game.setStone(index));
						mainActivity.refreshCaptured();
					}
				});
			} else {
				imageView = (ImageView) convertView;
			}
			imageView.setImageResource(mThumbIds[index]);
			return imageView;
		}

		private int makeImageResource(int index, char color){
			String strColor;
			String drawableName;
			int b = mainActivity.getBoardSize();

			switch(color){
			case Game.WHITE:
				strColor = "white";
				break;
			case Game.BLACK:
				strColor = "black";
				break;
			case Game.EMPTY:
			default:
				strColor = "empty";
				break;
			}

			if(index == 0){
				// Top-Left
				drawableName = "go_" + strColor + "_tl";
			}else if(index == b-1){
				// Top-Right
				drawableName = "go_" + strColor + "_tr";        		
			}else if(index > 0 && index < b-1){
				// Top
				drawableName = "go_" + strColor + "_t";
			}else if(index == b*(b-1)){
				// Bottom-Left
				drawableName = "go_" + strColor + "_bl";
			}else if(index == b*b-1){
				// Bottom-Right
				drawableName = "go_" + strColor + "_br";
			}else if(index > b*(b-1) && index < b*b-1){
				// Bottom
				drawableName = "go_" + strColor + "_b";
			}else if(index % b == b-1){
				// Right
				drawableName = "go_" + strColor + "_r";
			}else if(index % b == 0){
				// Left
				drawableName = "go_" + strColor + "_l";
			}else{
				// Middle
				drawableName = "go_" + strColor;        		
			}

			return mainActivity.getResources().getIdentifier(drawableName, "drawable", mainActivity.getPackageName());
		}

		private void updateBoard(Collection<Integer> indices){
			char[] position = mainActivity.getGame().getPosition();
			for(int index : indices){
				setImageResource(index, position[index]);
				notifyDataSetChanged();
			}
		}

		protected void refreshBoard(){
			char[] position = mainActivity.getGame().getPosition();
			for(int i = 0; i < position.length; i++)
				setImageResource(i, position[i]);
			notifyDataSetChanged();
		}
	}
}