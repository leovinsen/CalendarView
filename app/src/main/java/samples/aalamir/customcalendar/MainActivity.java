package samples.aalamir.customcalendar;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

import samples.aalamir.customcalendar.CalendarView.GridItemContainer;

public class MainActivity extends ActionBarActivity
{
	private static HashSet<Date> events;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		events = new HashSet<>();

		final CalendarView cv = ((CalendarView)findViewById(R.id.calendar_view));
		cv.updateCalendar(events);

		final TextView txt = (TextView)findViewById(R.id.textContainer);

		// assign event handler
		cv.setEventHandler(new CalendarView.EventHandler()
		{
			@Override
			public void onDayShortPress(GridItemContainer gic){
				//When user clicks on one of the grid
				DateFormat df = SimpleDateFormat.getDateInstance();
				txt.setText(df.format(gic.getDate()) + "    " + gic.isSelected() );

			}

			@Override
			public void onDayLongPress(Date date)
			{
				// show returned day
				events.add(date);
				Toast.makeText(MainActivity.this, "LONG PRESS", Toast.LENGTH_LONG).show();
			}
		});
	}

	public static HashSet<Date> getEvents(){
		return MainActivity.events;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings)
		{
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
     * Created by asus on 09/04/2017.
     */


}
