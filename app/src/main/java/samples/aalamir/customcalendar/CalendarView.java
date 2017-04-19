package samples.aalamir.customcalendar;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import static java.util.Calendar.FRIDAY;
import static java.util.Calendar.MONDAY;
import static java.util.Calendar.SATURDAY;
import static samples.aalamir.customcalendar.MainActivity.getEvents;

/**
 * Created by a7med on 28/06/2015.
 */
public class CalendarView extends LinearLayout {
        // for logging
        private static final String LOGTAG = "Calendar View";

        // default date format
        private static final String DATE_FORMAT = "MMM yyyy";

        // calendar "mode" -- single line or normal calendar (1 for normal, 2 for single)
        private int mode = 1;

        // how many days to show, defaults to six weeks, 42 days
        private int daysCount;

        // date format
        private String dateFormat;

        // today's date
        private Calendar currentDate = Calendar.getInstance();
        // calendar to be manipulated
        private Calendar calendarDate = Calendar.getInstance();

        //event handling
        private EventHandler eventHandler = null;

    // internal components
    private LinearLayout header;
    private ImageView btnPrev;
    private ImageView btnNext;
    private TextView txtDate;
    private GridView grid;
    private CalendarAdapter adapter;
    private GridViewHandler gridHandler;
    private HashSet<Date> eventDates;
    private LayoutTransition lt;


    // seasons' rainbow
    int[] rainbow = new int[]{
            R.color.summer,
            R.color.fall,
            R.color.winter,
            R.color.spring

    };

    // month-season association (northern hemisphere, sorry australia :)
    int[] monthSeason = new int[]{2, 2, 3, 3, 3, 0, 0, 0, 1, 1, 1, 2};

    public CalendarView(Context context) {
        super(context);
    }

    public CalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initControl(context, attrs);
    }

    public CalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initControl(context, attrs);
    }

    /**
     * Load control xml layout
     */
    private void initControl(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.control_calendar, this);


        eventDates = getEvents();
        //Toast.makeText(this.getContext(), currentDate.getTime().toString(), Toast.LENGTH_LONG).show();

        loadDateFormat(attrs);
        assignUiElements();
        assignClickHandlers();

        updateCalendar(eventDates);
    }

    private void loadDateFormat(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.CalendarView);

        try {
            // try to load provided date format, and fallback to default otherwise
            dateFormat = ta.getString(R.styleable.CalendarView_dateFormat);
            if (dateFormat == null)
                dateFormat = DATE_FORMAT;
        } finally {
            ta.recycle();
        }
    }

    private void assignUiElements() {
        // layout is inflated, assign local variables to components
        header = (LinearLayout) findViewById(R.id.calendar_header);
        btnPrev = (ImageView) findViewById(R.id.calendar_prev_button);
        btnNext = (ImageView) findViewById(R.id.calendar_next_button);
        txtDate = (TextView) findViewById(R.id.calendar_date_display);
        grid = (GridView) findViewById(R.id.calendar_grid);
        lt = new LayoutTransition();
        lt.setDuration(LayoutTransition.CHANGE_DISAPPEARING, 100);
        lt.setDuration(LayoutTransition.CHANGE_APPEARING, 300);
        grid.setLayoutTransition(lt);
        gridHandler = new GridViewHandler(new GridItemContainer(currentDate.getTime()));
    }

    private void assignClickHandlers() {
        // add one month and refresh UI
        btnNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mode == 1) {
                    calendarDate.add(Calendar.MONTH, 1);
                } else if (mode == 2) {
                    calendarDate.add(Calendar.WEEK_OF_MONTH, 1);
                }

                updateCalendar(eventDates);
            }
        });

        // subtract one month and refresh UI
        btnPrev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mode == 1) {
                    calendarDate.add(Calendar.MONTH, -1);
                } else if (mode == 2) {
                    calendarDate.add(Calendar.WEEK_OF_MONTH, -1);
                }

                updateCalendar(eventDates);
            }
        });

        //temporary button to switch between large and single line calendar
        txtDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mode == 1) {

                    mode = 2;
                    updateCalendar(eventDates);
                } else if (mode == 2) {

                    mode = 1;
                    updateCalendar(eventDates);
                }
            }
        });

        //short-pressing a day
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View cell, int position, long id) {
                GridItemContainer lastDate = gridHandler.getLastDate();


//                lastDate.setSelected(false);

                gridHandler.setLastDate(gridHandler.getCells().get(position));
                eventHandler.onDayShortPress(gridHandler.getLastDate());
                gridHandler.getLastDate().setSelected(true);
                adapter.notifyDataSetChanged();
                //handle short press

            }
        });

        // long-pressing a day
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View cell, int position, long id) {
                // handle long-press

                if (eventHandler == null)
                    return false;

                eventHandler.onDayLongPress((Date) adapterView.getItemAtPosition(position));
                return true;
            }
        });
    }

    /**
     * Display dates correctly in grid
     */
    public void updateCalendar(HashSet<Date> events) {
        gridHandler.clearCells();
        Calendar calendar = (Calendar) calendarDate.clone();

        switch (mode) {
            case 1:
                daysCount = 35;
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case 2:
                daysCount = 7;
                break;
        }

        // determine the cell for current month's beginning
        int monthBeginningCell = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        if (mode == 1) {
            if (
                    (calendar.get(Calendar.DAY_OF_WEEK) == SATURDAY && calendar.getActualMaximum(Calendar.DAY_OF_MONTH) == 30) ||
                            (calendar.get(Calendar.DAY_OF_WEEK) == FRIDAY && calendar.getActualMaximum(Calendar.DAY_OF_MONTH) == 31) ||
                            (calendar.get(Calendar.DAY_OF_WEEK) == MONDAY && calendar.getActualMaximum(Calendar.DAY_OF_MONTH) == 28)
                    )
                daysCount = 42;
        }
        // move calendar backwards to the beginning of the week
        calendar.add(Calendar.DAY_OF_MONTH, -monthBeginningCell);

        while (gridHandler.getCells().size() < daysCount) {
            //fill cells
            Date d = calendar.getTime();
            GridItemContainer gic = new GridItemContainer(d);
            //allows the last chosen date to stay when mode is changed
            if(gic.getDate().equals(gridHandler.getLastDate().getDate())){
                gic.setSelected(true);
            }
            gridHandler.add(gic);
            //shift 1 date forward
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        // update grid
        adapter = new CalendarAdapter(getContext(), gridHandler.getCells(), events);
        grid.setAdapter(adapter);

        // update title
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        txtDate.setText(sdf.format(calendarDate.getTime()));

        // set header color according to current season
        int month = calendarDate.get(Calendar.MONTH);
        int season = monthSeason[month];
        int color = rainbow[season];

        header.setBackgroundColor(getResources().getColor(color));
    }

    private class CalendarAdapter extends ArrayAdapter<GridItemContainer> {
        // days with events
        private HashSet<Date> eventDays;

        // for view inflation
        private LayoutInflater inflater;

        public CalendarAdapter(Context context, ArrayList<GridItemContainer> days, HashSet<Date> eventDays) {
            super(context, R.layout.control_calendar_day, days);
            this.eventDays = eventDays;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(final int position, View view, ViewGroup parent) {
            // day in question
            GridItemContainer gic = getItem(position);
            Date date = gic.getDate();

            int day = date.getDate();
            int month = date.getMonth();

            // month being focused
            int thisMonth = calendarDate.get(Calendar.MONTH);

            // inflate item if it does not exist yet
            if (view == null)
                view = inflater.inflate(R.layout.control_calendar_day, parent, false);

            if (gic.isSelected()){
                view.setBackgroundColor(Color.RED);
                gic.setSelected(false);
            } else {
                view.setBackgroundColor(Color.TRANSPARENT);
            }

            // clear styling
            ((TextView) view).setTypeface(null, Typeface.NORMAL);
            ((TextView) view).setTextColor(Color.BLACK);

            if (month != thisMonth) {
                // if this day is outside current month, grey it out
                ((TextView) view).setTextColor(getResources().getColor(R.color.greyed_out));
            }
            // set text
            ((TextView) view).setText(String.valueOf(date.getDate()));

            return view;
        }
    }

    /**
     * Assign event handler to be passed needed events
     */
    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    /**
     * This interface defines what events to be reported to
     * the outside world
     */
    public interface EventHandler {
        void onDayShortPress(GridItemContainer gic);

        void onDayLongPress(Date date);
    }

    //Class for the grid items. Allows the storage of additional variables such as isSelected.
    public static class GridItemContainer {

        private Date date;
        private boolean selected;

        public GridItemContainer(Date d){
            this.date = d;
            this.selected = false;
        }

        public boolean isSelected(){
            return this.selected;
        }

        public void setSelected(boolean value){
            this.selected = value;
        }

        public Date getDate(){
            return this.date;
        }

    }


    public static class GridViewHandler {

        private ArrayList<GridItemContainer> cells;
        private GridItemContainer lastDateSelected;

        public GridViewHandler(GridItemContainer gic){
            this.cells = new ArrayList<>();
            this.lastDateSelected = gic;
        }

        public ArrayList<GridItemContainer> getCells(){
            return cells;
        }

        public GridItemContainer getLastDate(){
            return lastDateSelected;
        }

        public void setLastDate(GridItemContainer gic){
            this.lastDateSelected = gic;
        }

        public void clearCells(){
            this.cells.clear();
        }

        public void add(GridItemContainer gic){
            this.cells.add(gic);
        }

    }

}
