package com.mredrock.cyxbs.network.func;

import com.google.gson.Gson;
import com.mredrock.cyxbs.MainApp;
import com.redrock.common.ContextProvider;
import com.redrock.common.config.Config;
import com.mredrock.cyxbs.model.Course;
import com.mredrock.cyxbs.ui.widget.CourseListAppWidget;
import com.mredrock.cyxbs.util.FileUtils;
import com.mredrock.cyxbs.util.SchoolCalendar;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import io.reactivex.functions.Function;


/**
 * Manage cache file for {@link CourseListAppWidget} and call its update
 * @author Haruue Icymoon haruue@caoyue.com.cn
 */

public class AppWidgetCacheAndUpdateFunc implements Function<List<Course>, List<Course>> {

    @Override
    public List<Course> apply(List<Course> courses)throws Exception {
        List<Course> weekCourses = new UserCourseFilterFunc(new SchoolCalendar().getWeekOfTerm()).apply(courses);
        List<Course> dayCourses = new UserCourseFilterByWeekDayFunc(new GregorianCalendar().get(Calendar.DAY_OF_WEEK)).apply(weekCourses);
        // List<Course> dayCourses = new UserCourseFilterByWeekDayFunc(Calendar.THURSDAY).call(weekCourses);
        FileUtils.writeStringToFile(new Gson().toJson(dayCourses), new File(ContextProvider.getContext().getFilesDir().getAbsolutePath() + "/" + Config.APP_WIDGET_CACHE_FILE_NAME));
        CourseListAppWidget.updateNow(ContextProvider.getContext());
        return courses;
    }

    public static void deleteCache() {
        try {
            //noinspection ResultOfMethodCallIgnored
            new File(ContextProvider.getContext().getFilesDir().getAbsolutePath() + "/" + Config.APP_WIDGET_CACHE_FILE_NAME).delete();
            CourseListAppWidget.updateNow(ContextProvider.getContext());
        } catch (Exception ignored) {

        }
    }
}
