package com.example.haushaltsheld.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.haushaltsheld.model.Task
import com.example.haushaltsheld.util.UserColorHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for the calendar screen (MVVM).
 * Holds displayed month/week state and task data; builds day cells for the grid.
 */
class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _monthCells = MutableLiveData<List<MonthDayCell>>(emptyList())
    val monthCells: LiveData<List<MonthDayCell>> = _monthCells

    private val _weekItems = MutableLiveData<List<WeekDayItem>>(emptyList())
    val weekItems: LiveData<List<WeekDayItem>> = _weekItems

    private val _monthYearLabel = MutableLiveData("")
    val monthYearLabel: LiveData<String> = _monthYearLabel

    private val _weekRangeLabel = MutableLiveData("")
    val weekRangeLabel: LiveData<String> = _weekRangeLabel

    /** First day of the currently displayed month */
    var displayedMonthFirst: Calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        private set

    /** Monday of the currently displayed week */
    var displayedWeekStart: Calendar = getWeekStart(Calendar.getInstance())
        private set

    private var tasksByDateKey: Map<String, List<Task>> = emptyMap()
    private var userIdToColorMap: Map<String, Int> = emptyMap()

    private fun getWeekStart(cal: Calendar): Calendar {
        val out = cal.clone() as Calendar
        val day = out.get(Calendar.DAY_OF_WEEK)
        val delta = (day - Calendar.MONDAY + 7) % 7
        out.add(Calendar.DAY_OF_MONTH, -delta)
        out.set(Calendar.HOUR_OF_DAY, 0)
        out.set(Calendar.MINUTE, 0)
        out.set(Calendar.SECOND, 0)
        out.set(Calendar.MILLISECOND, 0)
        return out
    }

    /** Set the displayed month and rebuild the 7×6 day grid (1–31 + empty cells). */
    fun setDisplayedMonth(year: Int, month: Int) {
        displayedMonthFirst = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        _monthYearLabel.value = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(displayedMonthFirst.time)
        buildMonthCells()
    }

    /** Move displayed month by delta (-1 or +1). */
    fun adjustMonth(delta: Int) {
        displayedMonthFirst.add(Calendar.MONTH, delta)
        _monthYearLabel.value = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(displayedMonthFirst.time)
        buildMonthCells()
    }

    private fun buildMonthCells() {
        val year = displayedMonthFirst.get(Calendar.YEAR)
        val month = displayedMonthFirst.get(Calendar.MONTH)
        val first = Calendar.getInstance().apply { set(year, month, 1) }
        val firstWeekday = first.get(Calendar.DAY_OF_WEEK)
        val mondayFirstOffset = (firstWeekday - Calendar.MONDAY + 7) % 7
        val maxDay = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayKey = dateKeyFormat.format(Date())

        val cells = mutableListOf<MonthDayCell>()
        for (i in 0 until 42) {
            val dayNumber: Int?
            val date: Date?
            if (i < mondayFirstOffset || i >= mondayFirstOffset + maxDay) {
                dayNumber = null
                date = null
            } else {
                dayNumber = i - mondayFirstOffset + 1
                val cal = Calendar.getInstance().apply { set(year, month, dayNumber!!) }
                date = cal.time
            }
            val key = if (date != null) dateKeyFormat.format(date) else null
            val dayTasks = if (key != null) tasksByDateKey[key] ?: emptyList() else emptyList()
            val colors = dayTasks.map { userIdToColorMap[it.assignedUserId] ?: 0 }.distinct()
            val isToday = key == todayKey
            cells.add(MonthDayCell(dayNumber, date, colors, isToday))
        }
        _monthCells.value = cells
    }

    /** Update task data and rebuild month cells (e.g. after loading from Firestore). */
    fun setTasksByDate(tasks: List<Task>) {
        val byDate = mutableMapOf<String, MutableList<Task>>()
        val userIds = mutableSetOf<String>()
        val cal = Calendar.getInstance()
        for (task in tasks) {
            cal.timeInMillis = task.date.time
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val key = dateKeyFormat.format(cal.time)
            byDate.getOrPut(key) { mutableListOf() }.add(task)
            if (task.assignedUserId.isNotEmpty()) userIds.add(task.assignedUserId)
        }
        tasksByDateKey = byDate
        userIdToColorMap = UserColorHelper.buildUserIdToColorMap(app, userIds)
        buildMonthCells()
    }

    /** Set the displayed week (Monday) and rebuild the 7 day items. */
    fun setDisplayedWeek(weekStart: Calendar) {
        displayedWeekStart = weekStart.clone() as Calendar
        val end = displayedWeekStart.clone() as Calendar
        end.add(Calendar.DAY_OF_MONTH, 6)
        _weekRangeLabel.value = SimpleDateFormat("dd.MM.", Locale.getDefault()).format(displayedWeekStart.time) +
                " - " + SimpleDateFormat("dd.MM.", Locale.getDefault()).format(end.time)
        buildWeekItems()
    }

    /** Move displayed week by 7 days. */
    fun adjustWeek(delta: Int) {
        displayedWeekStart.add(Calendar.DAY_OF_MONTH, delta)
        val end = displayedWeekStart.clone() as Calendar
        end.add(Calendar.DAY_OF_MONTH, 6)
        _weekRangeLabel.value = SimpleDateFormat("dd.MM.", Locale.getDefault()).format(displayedWeekStart.time) +
                " - " + SimpleDateFormat("dd.MM.", Locale.getDefault()).format(end.time)
        buildWeekItems()
    }

    private fun buildWeekItems() {
        val dayNames = arrayOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
        val todayKey = dateKeyFormat.format(Date())
        val items = mutableListOf<WeekDayItem>()
        val cal = displayedWeekStart.clone() as Calendar
        for (i in 0..6) {
            val date = cal.time
            val key = dateKeyFormat.format(date)
            val dayTasks = tasksByDateKey[key] ?: emptyList()
            val colors = dayTasks.map { userIdToColorMap[it.assignedUserId] ?: 0 }.distinct()
            items.add(WeekDayItem(date, dayNames[i], cal.get(Calendar.DAY_OF_MONTH), colors, key == todayKey))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        _weekItems.value = items
    }

    /** Call after setTasksByDate when week view is visible. */
    fun refreshWeekItems() {
        buildWeekItems()
    }

    fun getUserIdToColorMap(): Map<String, Int> = userIdToColorMap

    /** Get tasks for a specific date key (e.g. "2026-02-09"). */
    fun getTasksForDate(dateKey: String): List<Task> {
        return tasksByDateKey[dateKey] ?: emptyList()
    }
}

/** One cell in the month grid (empty or day 1–31). */
data class MonthDayCell(
    val dayNumber: Int?,
    val date: Date?,
    val taskColors: List<Int>,
    val isToday: Boolean = false
)

/** One day in the week row. */
data class WeekDayItem(
    val date: Date,
    val dayName: String,
    val dayNumber: Int,
    val taskColors: List<Int>,
    val isToday: Boolean = false
)
