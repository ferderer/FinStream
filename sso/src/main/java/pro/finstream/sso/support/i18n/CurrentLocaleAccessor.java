package pro.finstream.sso.support.i18n;

import java.time.DayOfWeek;
import java.time.temporal.WeekFields;
import java.util.Locale;
import org.springframework.context.i18n.LocaleContextHolder;

public interface CurrentLocaleAccessor {

    default String language() {
        return LocaleContextHolder.getLocale().getLanguage();
    }

    default Locale locale() {
        return LocaleContextHolder.getLocale();
    }

    default DayOfWeek firstDayOfWeek() {
        return WeekFields.of(LocaleContextHolder.getLocale()).getFirstDayOfWeek();
    }
}
