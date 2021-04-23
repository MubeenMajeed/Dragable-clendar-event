package com.techgitsol.android_draggable_event;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends Activity {
    // Values will come from policy
    private static final int MIN_15_BLOCKS_DURATION = 2;
    public static final int MAX_TIME_BLOCKS = 12;

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int TOTAL_NUMBER_OF_HOUR_BLOCKS = 25;
    private static final int UNIT_DURATION = 15;
    public static final String SOUVENIR_VIEW_TAG = "CanvasView";
    private static final String NO_LABEL = "noLabel";
    private static final String FIFTEEN = ":15";
    private static final String THIRTY = ":30";
    private static final String FORTY_FIVE = ":45";
    public static final String MINUTE_LABEL = "minuteLabel";
    public static final String NOW_LAYOUT = "nowLabel";
    private int mode = NONE;
    private int originalTimeBlockHeight;
    private int minBlockHeight;
    private int originalTimeBlockParentHeight;
    private int heightAtUp;
    private PointF start = new PointF();
    private PointF dragStart = new PointF();
    private RelativeLayout.LayoutParams params;
    private LinearLayout backgroundTimeTable;
    private int leftMargin;
    private int rightMargin;
    private LinkedList<Integer> lineNumberToScreenPosition;
    private Map<Integer, String> hourToLabel;
    private int timeLabelWidth;
    private int timeSlotMargin;
    private Map.Entry<Integer, String> positionToLabelEntry;
    private FrameLayout.LayoutParams timeBlockParentParams;
    private int upperLimitPosition;

    LinearLayout timeBlock, dragHandle, headerLayout, rootLayout;
    RelativeLayout timeBlockParent;
    ScrollView scrollView;
    TextView label;
    FrameLayout frame;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        label = ((TextView) findViewById(R.id.label));
        scrollView = ((ScrollView) findViewById(R.id.scrollView));
        headerLayout = ((LinearLayout) findViewById(R.id.headerLayout));
        frame = ((FrameLayout) findViewById(R.id.frame));
        rootLayout = ((LinearLayout) findViewById(R.id.rootLayout));

        timeBlockParent = ((RelativeLayout) findViewById(R.id.timeBlockParent));
        timeBlock = ((LinearLayout) findViewById(R.id.timeBlock));
        dragHandle = ((LinearLayout) findViewById(R.id.dragHandle));

        timeBlock.setOnTouchListener(getDragTimeBlockTouchListener());
        dragHandle.setOnTouchListener(getDragHandleTouchListener());


//        to get the all value from xml
//        Relative layout ma jo layout us ki value
        params = (RelativeLayout.LayoutParams) timeBlock.getLayoutParams();
//        Frame layout ma jo value us ki value
        timeBlockParentParams = (FrameLayout.LayoutParams) timeBlockParent.getLayoutParams();


        originalTimeBlockHeight = params.height;

        minBlockHeight = originalTimeBlockHeight * MIN_15_BLOCKS_DURATION;

        originalTimeBlockParentHeight = timeBlockParentParams.height;


        heightAtUp = originalTimeBlockHeight;

        backgroundTimeTable = new LinearLayout(this);

        backgroundTimeTable.setOrientation(LinearLayout.VERTICAL);

        backgroundTimeTable.setTag("backgroundTimeTable");

        // Build timetable
        int timeSlotScale = originalTimeBlockHeight * 4;
        timeLabelWidth = getResources().getDimensionPixelSize(R.dimen.pad_40dp);
        timeSlotMargin = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        int separatorLeftMargin = getResources().getDimensionPixelSize(R.dimen.pad_5dp);

//      hash map contain two type of valur
        hourToLabel = new HashMap<>();

        for (int hour = 0; hour < TOTAL_NUMBER_OF_HOUR_BLOCKS; hour++) {


//            LinearLayout >> LinaerLayout.matchparent
            LinearLayout timeSlot = new LinearLayout(this);
            LinearLayout.LayoutParams timeSlotLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, timeSlotScale);
            timeSlotLayoutParams.leftMargin = timeSlotMargin;
            timeSlot.setLayoutParams(timeSlotLayoutParams);

            TextView label = new TextView(this);
//            label.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            showTime(label, hour, 0, DateFormat.is24HourFormat(this));
//            set height and width of textview
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(timeLabelWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            label.setLayoutParams(labelParams);




            View separator = new View(this);
            LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.pad_1dp));
            separatorParams.leftMargin = separatorLeftMargin;
            separator.setLayoutParams(separatorParams);
            separator.setTag(String.valueOf(hour));
            separator.setBackgroundColor(getResources().getColor(R.color.gray));

            hourToLabel.put(hour, label.getText().toString());

            timeSlot.setGravity(Gravity.CENTER_VERTICAL);
            timeSlot.addView(label);
            timeSlot.addView(separator);

            backgroundTimeTable.addView(timeSlot);
        }

        leftMargin = timeLabelWidth + separatorLeftMargin + timeSlotMargin;
        rightMargin = 0;
        setLeftMargin(leftMargin);
        setRightMargin(rightMargin);

        FrameLayout.LayoutParams backgroundTimeTableParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        backgroundTimeTable.setLayoutParams(backgroundTimeTableParams);
        frame.addView(backgroundTimeTable, 0);

        lineNumberToScreenPosition = new LinkedList();

        ViewTreeObserver observer = rootLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                for (int hour = 0; hour < TOTAL_NUMBER_OF_HOUR_BLOCKS; hour++) {
                    final int finalHour = hour;
                    final View separator = backgroundTimeTable.findViewWithTag(String.valueOf(hour));
                    int [] locationOnScreen = new int[2];

                    separator.getLocationOnScreen(locationOnScreen);

                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    int topOffset = metrics.heightPixels - rootLayout.getMeasuredHeight();
                    lineNumberToScreenPosition.add(finalHour, locationOnScreen[1] - topOffset - headerLayout.getLayoutParams().height + 1);
                    Calendar cal = getNextTimeMultipleOfFifteen();
                    int selectedHour = cal.get(Calendar.HOUR_OF_DAY);
                    int selectedMinute = cal.get(Calendar.MINUTE);
                    if (finalHour == TOTAL_NUMBER_OF_HOUR_BLOCKS - 1) {
                        initiallyDropTimeBlock(selectedHour, selectedMinute);
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (lineNumberToScreenPosition.size() == 25) {
            Calendar cal = getNextTimeMultipleOfFifteen();
            int selectedHour = cal.get(Calendar.HOUR_OF_DAY);
            int selectedMinute = cal.get(Calendar.MINUTE);
            initiallyDropTimeBlock(selectedHour, selectedMinute);
        }
    }

    private void setRightMargin(int value) {
        params.rightMargin = value;
    }

    private void setLeftMargin(int value) {
        params.leftMargin = value;
    }

    public void initiallyDropTimeBlock(final int selectedHour, final int selectedMinute) {

        int fifteen = lineNumberToScreenPosition.get(selectedHour) + (lineNumberToScreenPosition.get(selectedHour + 1) - lineNumberToScreenPosition.get(selectedHour)) / 4;
        int thirty = lineNumberToScreenPosition.get(selectedHour) + (lineNumberToScreenPosition.get(selectedHour + 1) - lineNumberToScreenPosition.get(selectedHour)) / 2;
        int fortyFive = lineNumberToScreenPosition.get(selectedHour) + 3 * (lineNumberToScreenPosition.get(selectedHour + 1) - lineNumberToScreenPosition.get(selectedHour)) / 4;
        int defaultMinutes = lineNumberToScreenPosition.get(selectedHour);

        TreeMap<Integer, String> labelAndPositionMap = new TreeMap<>();

        switch (selectedMinute) {
            case 15:
                setTopMargin(fifteen);
                upperLimitPosition = fifteen;
                labelAndPositionMap.put(fifteen, FIFTEEN);
                break;
            case 30:
                setTopMargin(thirty);
                upperLimitPosition = thirty;
                labelAndPositionMap.put(thirty, THIRTY);
                break;
            case 45:
                setTopMargin(fortyFive);
                upperLimitPosition = fortyFive;
                labelAndPositionMap.put(fortyFive, FORTY_FIVE);
                break;
            default:
                setTopMargin(defaultMinutes);
                upperLimitPosition = defaultMinutes;
                labelAndPositionMap.put(defaultMinutes, NO_LABEL);
                break;

        }

        // Add a red line showing current time
        frame.removeView(frame.findViewWithTag(NOW_LAYOUT));
        final LinearLayout nowLayout = new LinearLayout(this);
        nowLayout.setTag(NOW_LAYOUT);
        nowLayout.setOrientation(LinearLayout.VERTICAL);

        final TextView timeToDisplay = new TextView(this);
        showTime(timeToDisplay, selectedHour, selectedMinute, DateFormat.is24HourFormat(this));

        View nowLine = new View(this);
        nowLine.setBackgroundColor(getResources().getColor(R.color.colorAccent));

        LinearLayout.LayoutParams timeToDisplayParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        timeToDisplayParams.gravity = Gravity.RIGHT;
        timeToDisplayParams.rightMargin = getResources().getDimensionPixelSize(R.dimen.pad_5dp);
        timeToDisplay.setLayoutParams(timeToDisplayParams);

        LinearLayout.LayoutParams nowLineParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.pad_1dp));
        nowLine.setLayoutParams(nowLineParams);

        nowLayout.addView(timeToDisplay);
        nowLayout.addView(nowLine);

        final FrameLayout.LayoutParams nowLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        nowLayout.setLayoutParams(nowLayoutParams);

        final ViewTreeObserver vto = nowLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                nowLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                nowLayoutParams.topMargin = upperLimitPosition - nowLayout.getBottom();
                nowLayoutParams.leftMargin = leftMargin;
                nowLayout.setLayoutParams(nowLayoutParams);
            }
        });

        frame.addView(nowLayout, 2);

        scrollView.scrollTo(0, (scrollView.getChildAt(0).getHeight() / TOTAL_NUMBER_OF_HOUR_BLOCKS) * selectedHour);

        // Show minutes
        positionToLabelEntry = labelAndPositionMap.firstEntry();
        showMinuteLabel();
        setDurationLabel(UNIT_DURATION * MIN_15_BLOCKS_DURATION);
        setTimeBlockHeight(minBlockHeight);
        setTimeBlockPosition(true);
    }

    private void setTopMargin(int value) {
        timeBlockParentParams.topMargin = value;
    }

    public void getClosestTimeBlock(int position, boolean isBottom) {
        int low = 0;
        int high = lineNumberToScreenPosition.size()-1;
        TreeMap<Integer, String> labelAndPositionMap = new TreeMap<>();

        while(low <= high) {
            int middle = (low+high) /2;
            if (lineNumberToScreenPosition.size() > 2) {
                if (position > lineNumberToScreenPosition.get(middle)) {
                    low = middle + 1;
                } else if (position < lineNumberToScreenPosition.get(middle)) {
                    high = middle - 1;
                } else { // The element has been found
                    labelAndPositionMap.put(position, NO_LABEL);
                    positionToLabelEntry = labelAndPositionMap.firstEntry();

                    // Set new duration
                    if (isBottom) {
                        int factor = (positionToLabelEntry.getKey() - timeBlockParentParams.topMargin) / originalTimeBlockHeight;
                        int totalMinutes = factor * 15;
                        setDurationLabel(totalMinutes);
                    }
                    return;
                }
            } else {
                break;
            }
        }

        boolean lowIsLast = low == lineNumberToScreenPosition.size() - 1;

        int hourBlockSize = Math.abs(lineNumberToScreenPosition.get(low) - lineNumberToScreenPosition.get(high));

        int [] fifteenMinutesBlocks = {lineNumberToScreenPosition.get(high),  //first 15 min block (:00)
                lineNumberToScreenPosition.get(high)  + hourBlockSize / 4, // second 15 min block (:15)
                lineNumberToScreenPosition.get(high) + hourBlockSize / 2, // third 15 min block (:30)
                lineNumberToScreenPosition.get(high) + 3 * hourBlockSize / 4, // fourth 15 min block (:45)
                lineNumberToScreenPosition.get(high) + hourBlockSize}; // next hour (no label)

        int distance = Math.abs(fifteenMinutesBlocks[0] - position);
        int idx = 0;
        for(int c = 1; c < fifteenMinutesBlocks.length; c++){
            int cdistance = Math.abs(fifteenMinutesBlocks[c] - position);
            if(cdistance < distance){
                idx = c;
                distance = cdistance;
            }
        }
        int theNumber = fifteenMinutesBlocks[idx];

        switch (idx) {
            case 0:
                labelAndPositionMap.put(theNumber, NO_LABEL); // first 15 min block (:00)
                break;
            case 1:
                labelAndPositionMap.put(theNumber, FIFTEEN); // second 15 min block (:15)
                break;
            case 2:
                labelAndPositionMap.put(theNumber, THIRTY); // third 15 min block (:30)
                break;
            case 3:
                labelAndPositionMap.put(theNumber, FORTY_FIVE); // fourth 15 min block (:45)
                break;
            case 4:
                if (lowIsLast) {
                    labelAndPositionMap.put(theNumber - hourBlockSize / 4, FORTY_FIVE); // go back to fourth 15 min block (:45)
                } else {
                    labelAndPositionMap.put(theNumber, NO_LABEL); //  next hour (no label)
                }
                break;
        }

        positionToLabelEntry = labelAndPositionMap.firstEntry();

        // Set Duration
        if (isBottom) {
            int factor = (positionToLabelEntry.getKey() - timeBlockParentParams.topMargin) / originalTimeBlockHeight;
            int totalMinutes = factor * 15;
            setDurationLabel(totalMinutes);
        }
    }

    private void setDurationLabel(int totalMinutes) {
        // Set new duration
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (hours == 0) {
            label.setText("Duration: " + minutes + " mins");
        } else {
            if (minutes == 0) {
                label.setText("Duration: " + hours + "h");
            } else {
                label.setText("Duration: " + String.format("%dh %02d", hours, minutes) + " mins");
            }
        }
        label.setTextColor(getResources().getColor(R.color.white));
    }

    private OnTouchListener getDragHandleTouchListener() {
        return new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Prevent other touch events from stealing
                scrollView.requestDisallowInterceptTouchEvent(true);
                timeBlock.requestDisallowInterceptTouchEvent(true);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dragStart.set(event.getX(), event.getY());
                        break;
                    case MotionEvent.ACTION_UP:
                        // Allow other touch events to steal
                        scrollView.requestDisallowInterceptTouchEvent(false);
                        timeBlock.requestDisallowInterceptTouchEvent(false);

                        // Readjust height
                        int adjustedHeight = positionToLabelEntry.getKey() - timeBlockParentParams.topMargin;
                        if (heightAtUp < adjustedHeight) {
                            expandTimeBlock(heightAtUp, adjustedHeight);
                        } else {
                            collapseTimeBlock(adjustedHeight, heightAtUp);
                        }

                        break;
                    case MotionEvent.ACTION_MOVE:
                        int draggingDistance = (int) (event.getY() - dragStart.y);
                        int newHeight = params.height + draggingDistance;

                        params.height = newHeight;

                        setTimeBlockPosition(false);
                        getClosestToBottomOfTimeBlockAndShowMinuteLabel();
                        heightAtUp = params.height;
                        break;
                }
                return true;
            }
        };
    }

    private OnTouchListener getDragTimeBlockTouchListener() {
        return new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Prevent other touch events from stealing
                scrollView.requestDisallowInterceptTouchEvent(true);

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        // Start tracing finger movement
                        start.set(event.getX(), event.getY());
                        enableDragMode();
                        break;
                    case MotionEvent.ACTION_UP:
                        enableNoneMode(timeBlock);
                        // Allow other touch events to steal
                        scrollView.requestDisallowInterceptTouchEvent(false);

                        // Adjust block position
                        if (positionToLabelEntry != null) {
                            setTopMargin(positionToLabelEntry.getKey());
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mode == DRAG) {
                            // Set dragging distance
                            int draggingDistance = (int) (event.getY() - start.y);
                            boolean goingDown = draggingDistance > 0 ? true : false;

                            // Define dragging limits
                            int timeTableTopLimitLocation = lineNumberToScreenPosition.get(0);
                            int timeTableBottomLimitLocation = lineNumberToScreenPosition.get(lineNumberToScreenPosition.size() - 1);
                            int timeBlockBottomLimitLocation = timeTableBottomLimitLocation - params.height;

                            // Remove if you only want to allow events after red line
                            upperLimitPosition = timeTableTopLimitLocation;

                            int potentialTopMargin = timeBlockParentParams.topMargin + draggingDistance;

                            // Readjust top margin to prevent going over limits
                            if (potentialTopMargin < upperLimitPosition) {
                                potentialTopMargin = upperLimitPosition;
                            }
                            if (potentialTopMargin > timeBlockBottomLimitLocation) {
                                potentialTopMargin = timeBlockBottomLimitLocation;
                            }

                            // Make it scroll when finger is close to window top/down
                            DisplayMetrics metrics = new DisplayMetrics();
                            getWindowManager().getDefaultDisplay().getMetrics(metrics);

                            Rect rectangle = new Rect();
                            getWindow().getDecorView().getWindowVisibleDisplayFrame(rectangle);
                            int statusBarHeight = rectangle.top;

                            if ((event.getRawY() <= (statusBarHeight + event.getY() + getResources().getDimensionPixelSize(R.dimen.pad_10dp) + headerLayout.getLayoutParams().height) && !goingDown)
                                    || (event.getRawY() >= metrics.heightPixels - (timeBlock.getHeight() - event.getY()) && goingDown)
                                    || (timeBlockParentParams.topMargin + params.height >= lineNumberToScreenPosition.get(lineNumberToScreenPosition.size() - 1) && goingDown)) {
                                scrollView.smoothScrollBy(0, draggingDistance);
                            }

                            // Set top margin to position choose by finger
                            setTopMargin(potentialTopMargin);

                            // Get closest time block and show minutes
                            getClosestTimeBlockAndShowMinuteLabel();

                            // Adjust time block position to position that makes sense
                            setTimeBlockPosition(false);
                        }
                        break;
                }
                return true;
            }
        };
    }

    private void setTimeBlockHeight(int value) {
        params.height = value;
        timeBlockParentParams.height = params.height + (originalTimeBlockParentHeight - originalTimeBlockHeight);
    }

    private void setTimeBlockPosition(boolean initiallyDropping) {
        int maxHeight = originalTimeBlockHeight * MAX_TIME_BLOCKS;
        int timeTableTopLimitLocation = lineNumberToScreenPosition.get(0);
        int timeTableBottomLimitLocation = lineNumberToScreenPosition.get(lineNumberToScreenPosition.size() - 1);

        // Adjust new height to prevent going over height limits
        if (params.height <= minBlockHeight) {
            setTimeBlockHeight(minBlockHeight);
        } else if (params.height >= maxHeight) {
            setTimeBlockHeight(maxHeight);
        } else {
            setTimeBlockHeight(params.height);
        }

        // Adjust new height to prevent going over timetable limits
        if (timeBlockParentParams.topMargin + params.height >= timeTableBottomLimitLocation) {
            if (params.height == minBlockHeight && initiallyDropping) {
                initiallyDropTimeBlock(0, 0); // TODO drop to next valid spot on the next day
                // TODO go to next day
            } else {
                setTimeBlockHeight(timeTableBottomLimitLocation - timeBlockParentParams.topMargin);
            }
        }

        timeBlock.setLayoutParams(params);
    }

    private void getClosestTimeBlockAndShowMinuteLabel() {
        getClosestTimeBlock(timeBlockParent.getTop(), false);
        showMinuteLabel();
    }

    private void getClosestToBottomOfTimeBlockAndShowMinuteLabel() {
        getClosestTimeBlock(timeBlockParent.getTop() + params.height, true);
        showMinuteLabel();
    }

    private void showMinuteLabel() {
        frame.removeView(frame.findViewWithTag(MINUTE_LABEL));
        if (!NO_LABEL.equals(positionToLabelEntry.getValue())) {
            TextView minuteLabel = new TextView(this);
            minuteLabel.setText(positionToLabelEntry.getValue());
            minuteLabel.setTag(MINUTE_LABEL);

            FrameLayout.LayoutParams minuteParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            minuteParams.topMargin = positionToLabelEntry.getKey() - originalTimeBlockHeight / 2;
            minuteParams.leftMargin = timeLabelWidth/2 + timeSlotMargin;
            minuteLabel.setLayoutParams(minuteParams);

            frame.addView(minuteLabel, 2);
        }
    }

    private void enableDragMode() {
        timeBlock.setBackgroundColor(getResources().getColor(R.color.gray));
        mode = DRAG;

        // Remember where I was
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels - leftMargin - rightMargin;

        captureUsingDrawingCache(timeBlock, width, params.height);

        timeBlock.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
    }

    private void captureUsingDrawingCache(View targetView, int width, int height){
        targetView.buildDrawingCache();
        Bitmap b1 = targetView.getDrawingCache();
        // copy this bitmap otherwise destroying the cache will destroy
        // the bitmap for the referencing drawable and you'll not
        // get the captured view
        Bitmap b = b1.copy(Bitmap.Config.ARGB_8888, false);
        BitmapDrawable d = new BitmapDrawable(getResources(), b);
        View canvasView = new View(this);
        canvasView.setTag(SOUVENIR_VIEW_TAG);
        FrameLayout.LayoutParams canvasParams = new FrameLayout.LayoutParams(width, height);
        canvasParams.topMargin = timeBlockParentParams.topMargin;
        canvasParams.leftMargin = leftMargin;
        canvasView.setLayoutParams(canvasParams);
        canvasView.setBackground(d);
        frame.addView(canvasView, 1);
        targetView.destroyDrawingCache();
    }

    private void enableNoneMode(View v) {
        LinearLayout linearLayout = (LinearLayout) v;
        linearLayout.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        mode = NONE;

        // Forget where I was
        frame.removeView(frame.findViewWithTag(SOUVENIR_VIEW_TAG));
    }

    public void expandTimeBlock(final int initialHeight, final int targetHeight) {
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                setTimeBlockHeight(initialHeight + (int) ((targetHeight - initialHeight) * interpolatedTime));
                setTimeBlockPosition(false);
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int)((targetHeight - initialHeight) / timeBlock.getContext().getResources().getDisplayMetrics().density) * 10);
        timeBlock.startAnimation(a);
    }

    public void collapseTimeBlock(final int targetHeight, final int initialHeight) {
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                setTimeBlockHeight(initialHeight - (int)((initialHeight - targetHeight) * interpolatedTime));
                setTimeBlockPosition(false);
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int)((initialHeight - targetHeight) / timeBlock.getContext().getResources().getDisplayMetrics().density) * 10);
        timeBlock.startAnimation(a);
    }

    public void showTime(TextView time, int hour, int min, boolean is24h) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        if (!is24h) {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
            time.setText(sdf.format(cal.getTime()).toLowerCase().replace(":00", ""));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("H:mm");
            time.setText(sdf.format(cal.getTime()).toLowerCase());
        }
    }

    public Calendar getNextTimeMultipleOfFifteen() {
        Calendar cal = Calendar.getInstance();
        // Extract hour and minute
        int hourComponent = cal.get(Calendar.HOUR_OF_DAY);
        int minuteComponent = cal.get(Calendar.MINUTE);

        // Next multiple of 5
        int nextMultipleOfFifteen = (int) Math.ceil((((float)minuteComponent + 0.1) / 15)) * 15;

        // Check if need to increase hour and add zeros
        switch(nextMultipleOfFifteen) {
            case 60:
                hourComponent = hourComponent + 1;
                minuteComponent = 0;
                break;
            case 15:
                minuteComponent = 15;
                break;
            default:
                minuteComponent = nextMultipleOfFifteen;
        }

        if (hourComponent == 24) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
            hourComponent = 0;
        }

        cal.set(Calendar.HOUR_OF_DAY, hourComponent);
        cal.set(Calendar.MINUTE, minuteComponent);

        return cal;
    }
}