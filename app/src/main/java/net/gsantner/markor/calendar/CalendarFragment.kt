package org.markor.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.Month
import com.kizitonwose.calendar.core.Week
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.YearMonth
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import org.markor.R
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class CalendarFragment : Fragment() {

    private var selectedDate: LocalDate? = null
    private lateinit var calendarView: CalendarView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        calendarView = view.findViewById(R.id.calendarView)

        val today = LocalDate.now()
        val currentMonth = YearMonth(today.year, today.month)
        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)

        calendarView.setup(startMonth, endMonth, java.time.DayOfWeek.SUNDAY)
        calendarView.scrollToMonth(currentMonth)

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View): DayViewContainer = DayViewContainer(view)

            override fun bind(container: DayViewContainer, data: WeekDay) {
                container.textView.text = data.date.dayOfMonth.toString()

                container.textView.isVisible = data.position == DayPosition.MonthDate

                container.textView.setOnClickListener {
                    selectedDate = if (selectedDate == data.date) null else data.date
                    calendarView.notifyDayChanged(data.date)
                }

                // Basic highlight for selected date
                container.textView.setBackgroundResource(
                    if (selectedDate == data.date) R.drawable.selected_day_bg else 0
                )
            }
        }
    }

    class DayViewContainer(view: View) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.dayText)
    }
}
