package com.vvs.sessions;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.vvs.VVS;

public class TimeUtils {

	public static String getFormattedLocaleTime(Timestamp time) throws ParseException {
    	return getFormattedLocaleTime(time, TimeZone.getDefault());
    }
    
    // reference: https://www.youtube.com/watch?v=-5wpm-gesOY
    public static String getFormattedLocaleTime(Timestamp time, TimeZone timezone) throws ParseException {
    	Calendar calendar = Calendar.getInstance();
    	calendar.setTime(time);
    	SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    	Date date = utcFormat.parse(utcFormat.format(calendar.getTime()));
    	SimpleDateFormat sdf = new SimpleDateFormat(VVS.TIME_FORMAT);
    	sdf.setTimeZone(timezone);
    	return sdf.format(date);
    }
	
}
