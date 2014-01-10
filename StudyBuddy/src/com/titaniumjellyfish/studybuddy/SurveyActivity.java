package com.titaniumjellyfish.studybuddy;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TimeFormatException;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;
import com.titaniumjellyfish.studybuddy.database.CommentEntry;
import com.titaniumjellyfish.studybuddy.database.CrowdEntry;
import com.titaniumjellyfish.studybuddy.database.Globals;
import com.titaniumjellyfish.studybuddy.database.RoomLocationEntry;
import com.titaniumjellyfish.studybuddy.database.TitaniumDb;

public class SurveyActivity extends OrmLiteBaseActivity<TitaniumDb> implements OnTimeSetListener, OnDateSetListener{

	
	SeekBar productivitySeekbar, crowdSeekbar;
	Button dateButton, timeButton;
	AutoCompleteTextView buildingAuto;
	Spinner roomSpinner;
	
	
	int year =-1, month=-1, day=-1, hour=-1, minute=-1;
	
	int crowdedness = -1;
	int productivity = -1;
	
	RoomLocationEntry mEntry;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_survey);
		Bundle extras = getIntent().getExtras();
		if (extras != null){
			long id = extras.getLong(Globals.KEY_ROOMLOC_ID);
			try {
				Dao<RoomLocationEntry, Long> roomDao = getHelper().dao(RoomLocationEntry.class, Long.class);
				mEntry = roomDao.queryForId(id);
			} catch (SQLException e) {
				e.printStackTrace();
				Log.e(Globals.LOGTAG, "Couldn't find ID " + id + "in the database", e);
				throw new AngryJulienException("Error finding ID " + id + "in the database" + e.getMessage());
			}
		}
//		View v =  findViewById(R.id.)
		
		crowdSeekbar = (SeekBar) findViewById(R.id.crowdSeekbar);
		crowdSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekbar, int position, boolean fromUser) {
				crowdedness = position;
				TextView crowdLabel = (TextView) findViewById(R.id.crowdBarLabel);
				String label = getString(R.string.ui_survey_crowd_label) + " ("	+ (crowdedness+1) + ": ";
				switch (crowdedness) {
					case 0:
						label += "Empty)";
						break;
					case 1:
						label += "Signs of life)";
						break;
					case 2:
						label += "Crowded)";
						break;
					case 3:
						label += "Full)";
						break;
				}
				crowdLabel.setText(label);
			}
			public void onStopTrackingTouch(SeekBar seekBar) {}
			public void onStartTrackingTouch(SeekBar seekBar) {}
		});
		
		productivitySeekbar = (SeekBar) findViewById(R.id.productivitySeekbar);
		productivitySeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekbar, int position, boolean fromUser) {
				productivity = position;
				TextView crowdLabel = (TextView) findViewById(R.id.productivityBarLabel);
				String label = getString(R.string.ui_survey_productivity_label) + " ("	+ (productivity+1) + ": ";
				switch (productivity) {
					case 0:
						label += "Distracted)";
						break;
					case 1:
						label += "Barely)";
						break;
					case 2:
						label += "Average)";
						break;
					case 3:
						label += "Very)";
						break;
					case 4:
						label += "Crushed it)";
						break;
				}
				crowdLabel.setText(label);
			}
			public void onStopTrackingTouch(SeekBar seekBar) {}
			public void onStartTrackingTouch(SeekBar seekBar) {}
		});
			
		dateButton = (Button) findViewById(R.id.survey_botton_date);
		
		dateButton.setOnClickListener(new OnClickListener() {
			private Calendar d = new GregorianCalendar();
//			private Date d;
			
			@Override
			public void onClick(View v) {
				final Calendar now = Calendar.getInstance();
				int hour = now.get(Calendar.HOUR_OF_DAY),
					year = now.get(Calendar.YEAR), 
					month = now.get(Calendar.MONTH), 
					day = now.get(Calendar.DAY_OF_MONTH);
				
				new DatePickerDialog(SurveyActivity.this,  SurveyActivity.this, year, month, day).show();			
			}
		});
		
	
		timeButton = (Button) findViewById(R.id.survey_button_time);
		timeButton.setOnClickListener(new OnClickListener() {
			private Calendar d = new GregorianCalendar();
//			private Date d;
			
			@Override
			public void onClick(View v) {
				final Calendar now = Calendar.getInstance();
				int hour = now.get(Calendar.HOUR_OF_DAY),
					minute = now.get(Calendar.MINUTE);
				
				new TimePickerDialog(SurveyActivity.this,  SurveyActivity.this, hour, minute, false).show();			
			}
		});
		
		buildingAuto = (AutoCompleteTextView) findViewById(R.id.survey_building_autocomplete);
		roomSpinner = (Spinner) findViewById(R.id.survey_room_spinner);
		
		if (mEntry != null){
			buildingAuto.setText(mEntry.getBuildingName());
			buildingAuto.setEnabled(false);
			
			String[] room = {mEntry.getRoomName()};
			
			roomSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, room));
			roomSpinner.setEnabled(false);
		}
		else{
			//TODO update room spinner based on building auto
		}
	}
	
	private boolean verifyFields(){
		return (year != -1 &&
				month != -1 &&
				day != -1 &&
				hour != -1 &&
				minute != -1 &&
				crowdedness != -1 &&
				productivity != -1);
	}
	
	public void onClickOk(View view) {	
		if (!verifyFields()){
			Toast.makeText(this,  "Please chose all fields, including a date and a time for your survey",  Toast.LENGTH_SHORT).show();
			return;
		}
		
		double myCrowd = mEntry.my_crowd;
		int mySur = mEntry.my_n_surveys;
		double myProd = mEntry.my_productivity;
		
		mEntry.my_productivity = ((myProd * mySur) + productivity) / (mySur + 1);
		mEntry.my_crowd = ((myCrowd * mySur) + crowdedness) / (mySur + 1);
		mEntry.my_n_surveys += 1;
		
		mEntry.setLocal(true);
		mEntry.my_capacity = mEntry.getCapacity();
		
		
		Toast.makeText(this, "Thank you!", Toast.LENGTH_SHORT).show();
		finish();
	}
	
	/**
	 * Overrides finish to add slide animation
	 */
  public void finish() {
    super.finish();
    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
  }
  
	public void onStartTrackingTouch(SeekBar arg0) {}
	public void onStopTrackingTouch(SeekBar arg0) {}

	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		
		GregorianCalendar cal = new GregorianCalendar(year,  monthOfYear,  dayOfMonth);
		String str = DateFormat.getDateFormat(this).format(cal.getTime());
		
		dateButton.setText(str);
		this.year = year;
		this.month = monthOfYear;
		this.day = dayOfMonth;
		
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		GregorianCalendar cal = new GregorianCalendar();
		cal.set(GregorianCalendar.HOUR_OF_DAY, hourOfDay);
		cal.set(GregorianCalendar.MINUTE, minute);
		
		String str = DateFormat.getTimeFormat(this).format(cal.getTime());
		timeButton.setText(str);
		this.hour = hourOfDay;
		this.minute = minute;
	}
}
