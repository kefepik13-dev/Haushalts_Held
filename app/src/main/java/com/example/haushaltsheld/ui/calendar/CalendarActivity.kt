package com.example.haushaltsheld.ui.calendar

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.haushaltsheld.R
import com.example.haushaltsheld.databinding.ActivityCalendarBinding
import com.example.haushaltsheld.databinding.BottomSheetDayTasksBinding
import com.example.haushaltsheld.databinding.DialogTaskDetailBinding
import com.example.haushaltsheld.model.Task
import com.example.haushaltsheld.ui.task.TaskAdapter
import com.example.haushaltsheld.util.UserColorHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * Calendar screen (MVVM). Shows a 7×6 month grid with days 1–31, weekday headers (Mo–So),
 * and a week view. Each day is clickable; tasks open in a BottomSheet.
 */
class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private lateinit var viewModel: CalendarViewModel
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var currentGroupId: String? = null
    private var isMonthView = true

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = androidx.lifecycle.ViewModelProvider(this)[CalendarViewModel::class.java]
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            finish()
            return
        }

        setupToolbar()
        setupViewToggle()
        setupMonthNavigation()
        setupWeekNavigation()
        setupMonthGrid()
        setupWeekRecycler()
        observeViewModel()
        // Build and show the day grid immediately (1–31)
        viewModel.setDisplayedMonth(
            viewModel.displayedMonthFirst.get(Calendar.YEAR),
            viewModel.displayedMonthFirst.get(Calendar.MONTH)
        )
        viewModel.setDisplayedWeek(viewModel.displayedWeekStart)
        loadUserGroup()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun observeViewModel() {
        viewModel.monthYearLabel.observe(this) { binding.tvMonthYear.text = it }
        viewModel.weekRangeLabel.observe(this) { binding.tvWeekRange.text = it }
        viewModel.monthCells.observe(this) { cells ->
            (binding.monthGrid.adapter as? MonthGridAdapter)?.setCells(cells)
        }
        viewModel.weekItems.observe(this) { items ->
            (binding.weekDaysRecycler.adapter as? WeekDaysAdapter)?.setItems(items)
        }
    }

    private fun setupViewToggle() {
        binding.tabLayoutView.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                isMonthView = (tab?.position == 0)
                binding.monthViewContainer.visibility = if (isMonthView) View.VISIBLE else View.GONE
                binding.weekViewContainer.visibility = if (isMonthView) View.GONE else View.VISIBLE
                if (isMonthView) {
                    loadTasksForMonth()
                } else {
                    viewModel.refreshWeekItems()
                    loadTasksForWeek()
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupMonthNavigation() {
        binding.btnPrevMonth.setOnClickListener {
            viewModel.adjustMonth(-1)
            loadTasksForMonth()
        }
        binding.btnNextMonth.setOnClickListener {
            viewModel.adjustMonth(1)
            loadTasksForMonth()
        }
    }

    private fun setupWeekNavigation() {
        binding.btnPrevWeek.setOnClickListener {
            viewModel.adjustWeek(-7)
            loadTasksForWeek()
        }
        binding.btnNextWeek.setOnClickListener {
            viewModel.adjustWeek(7)
            loadTasksForWeek()
        }
    }

    private fun setupMonthGrid() {
        binding.monthGrid.layoutManager = GridLayoutManager(this, 7)
        binding.monthGrid.adapter = MonthGridAdapter(emptyList()) { date -> onDayClicked(date) }
    }

    private fun setupWeekRecycler() {
        binding.weekDaysRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.weekDaysRecycler.adapter = WeekDaysAdapter(emptyList()) { date -> onDayClicked(date) }
    }

    private fun onDayClicked(date: Date) {
        val groupId = currentGroupId
        if (groupId == null) {
            Toast.makeText(this, getString(R.string.please_create_or_join_group), Toast.LENGTH_SHORT).show()
            return
        }

        val calendar = Calendar.getInstance().apply { time = date }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.time

        firestore.collection("tasks")
            .whereEqualTo("groupId", groupId)
            .whereGreaterThanOrEqualTo("date", com.google.firebase.Timestamp(startOfDay))
            .whereLessThan("date", com.google.firebase.Timestamp(endOfDay))
            .get()
            .addOnSuccessListener { documents ->
                val tasks = documents.map { doc -> toTask(doc) }.sortedBy { it.date }
                val userIds = tasks.map { it.assignedUserId }.filter { it.isNotEmpty() }.toSet()
                val colorMap = UserColorHelper.buildUserIdToColorMap(this, userIds)
                showDayTasksBottomSheet(date, tasks, colorMap)
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.no_tasks_for_this_date), Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDayTasksBottomSheet(date: Date, tasks: List<Task>, colorMap: Map<String, Int>) {
        val sheetBinding = BottomSheetDayTasksBinding.inflate(layoutInflater)
        val sheet = BottomSheetDialog(this).apply { setContentView(sheetBinding.root) }

        sheetBinding.tvSheetTitle.text = displayDateFormat.format(date)
        sheetBinding.tvSheetSubtitle.visibility = View.VISIBLE

        if (tasks.isEmpty()) {
            sheetBinding.rvSheetTasks.visibility = View.GONE
            sheetBinding.tvSheetEmpty.visibility = View.VISIBLE
        } else {
            sheetBinding.rvSheetTasks.visibility = View.VISIBLE
            sheetBinding.tvSheetEmpty.visibility = View.GONE
            sheetBinding.rvSheetTasks.layoutManager = LinearLayoutManager(this)
            sheetBinding.rvSheetTasks.adapter = TaskAdapter(tasks, { task ->
                showTaskDetailDialog(task)
                sheet.dismiss()
            }, colorMap)
        }

        sheetBinding.btnSheetClose.setOnClickListener { sheet.dismiss() }
        sheet.show()
    }

    private fun loadUserGroup() {
        val currentUser = auth.currentUser ?: return
        firestore.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                currentGroupId = document.getString("groupId")
                if (currentGroupId != null) {
                    if (isMonthView) loadTasksForMonth() else loadTasksForWeek()
                } else {
                    Toast.makeText(this, getString(R.string.please_create_or_join_group), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun loadTasksForMonth() {
        val groupId = currentGroupId ?: return
        val start = viewModel.displayedMonthFirst.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)
        val end = start.clone() as Calendar
        end.add(Calendar.MONTH, 1)

        firestore.collection("tasks")
            .whereEqualTo("groupId", groupId)
            .whereGreaterThanOrEqualTo("date", com.google.firebase.Timestamp(start.time))
            .whereLessThan("date", com.google.firebase.Timestamp(end.time))
            .get()
            .addOnSuccessListener { documents ->
                val tasks = documents.map { doc -> toTask(doc) }
                viewModel.setTasksByDate(tasks)
            }
    }

    private fun loadTasksForWeek() {
        val groupId = currentGroupId ?: return
        val start = viewModel.displayedWeekStart.time
        val endCal = viewModel.displayedWeekStart.clone() as Calendar
        endCal.add(Calendar.DAY_OF_MONTH, 7)
        val end = endCal.time

        firestore.collection("tasks")
            .whereEqualTo("groupId", groupId)
            .whereGreaterThanOrEqualTo("date", com.google.firebase.Timestamp(start))
            .whereLessThan("date", com.google.firebase.Timestamp(end))
            .get()
            .addOnSuccessListener { documents ->
                val tasks = documents.map { doc -> toTask(doc) }
                viewModel.setTasksByDate(tasks)
                viewModel.refreshWeekItems()
            }
    }

    private fun toTask(doc: com.google.firebase.firestore.DocumentSnapshot): Task {
        val data = doc.data ?: return Task()
        return Task(
            id = doc.id,
            groupId = data["groupId"] as? String ?: "",
            title = data["title"] as? String ?: "",
            description = data["description"] as? String ?: "",
            assignedUserId = data["assignedUserId"] as? String ?: "",
            assignedUserName = data["assignedUserName"] as? String ?: "",
            date = (data["date"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
            status = data["status"] as? String ?: "open"
        )
    }

    private fun showTaskDetailDialog(task: Task) {
        val dialogBinding = DialogTaskDetailBinding.inflate(layoutInflater)
        dialogBinding.tvDetailTitle.text = task.title
        dialogBinding.tvDetailDescription.text = if (task.description.isEmpty()) getString(R.string.no_description) else task.description
        dialogBinding.tvDetailAssignedUser.text = task.assignedUserName
        dialogBinding.tvDetailStatus.text = task.status.uppercase()
        if (task.status == "completed") {
            dialogBinding.tvDetailStatus.setBackgroundColor(getColor(android.R.color.holo_green_light))
        } else {
            dialogBinding.tvDetailStatus.setBackgroundColor(getColor(android.R.color.holo_blue_light))
        }
        dialogBinding.tvDetailStatus.setTextColor(getColor(android.R.color.white))

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
            .apply {
                dialogBinding.btnCloseDetail.setOnClickListener { dismiss() }
                show()
            }
    }

    // --- Month grid adapter: 42 cells (7×6), each day is a clickable view ---
    private class MonthGridAdapter(
        private var cells: List<MonthDayCell>,
        private val onDayClick: (Date) -> Unit
    ) : RecyclerView.Adapter<MonthGridAdapter.DayVH>() {

        fun setCells(newCells: List<MonthDayCell>) {
            cells = newCells
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayVH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
            return DayVH(v)
        }

        override fun onBindViewHolder(holder: DayVH, position: Int) {
            holder.bind(cells[position], onDayClick)
        }

        override fun getItemCount(): Int = cells.size

        class DayVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
            private val dotsContainer: LinearLayout = itemView.findViewById(R.id.dotsContainer)
            private val dayCellRoot: View = itemView.findViewById(R.id.dayCellRoot)

            fun bind(cell: MonthDayCell, onDayClick: (Date) -> Unit) {
                tvDayNumber.text = cell.dayNumber?.toString() ?: ""
                tvDayNumber.visibility = if (cell.dayNumber != null) View.VISIBLE else View.INVISIBLE

                val ctx = itemView.context
                if (cell.isToday) {
                    dayCellRoot.setBackgroundColor(ctx.getColor(R.color.calendar_today_background))
                    tvDayNumber.setTextColor(ctx.getColor(R.color.calendar_today_text))
                } else {
                    val attr = TypedValue()
                    ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, attr, true)
                    dayCellRoot.setBackgroundResource(attr.resourceId)
                    tvDayNumber.setTextColor(ctx.getColor(android.R.color.black))
                }

                dotsContainer.removeAllViews()
                val size = (4 * ctx.resources.displayMetrics.density).toInt()
                for (color in cell.taskColors.take(5)) {
                    val dot = View(ctx)
                    val drawable = GradientDrawable().apply {
                        setShape(GradientDrawable.OVAL)
                        setColor(color)
                    }
                    dot.background = drawable
                    val lp = LinearLayout.LayoutParams(size, size)
                    lp.marginEnd = (2 * ctx.resources.displayMetrics.density).toInt()
                    dotsContainer.addView(dot, lp)
                }

                dayCellRoot.isClickable = cell.date != null
                dayCellRoot.isFocusable = cell.date != null
                dayCellRoot.setOnClickListener { cell.date?.let { onDayClick(it) } }
            }
        }
    }

    // --- Week row adapter ---
    private class WeekDaysAdapter(
        private var items: List<WeekDayItem>,
        private val onDayClick: (Date) -> Unit
    ) : RecyclerView.Adapter<WeekDaysAdapter.WeekDayVH>() {

        fun setItems(newItems: List<WeekDayItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekDayVH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_week_day, parent, false)
            return WeekDayVH(v)
        }

        override fun onBindViewHolder(holder: WeekDayVH, position: Int) {
            holder.bind(items[position], onDayClick)
        }

        override fun getItemCount(): Int = items.size

        class WeekDayVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvDayName: TextView = itemView.findViewById(R.id.tvWeekDayName)
            private val tvDayNumber: TextView = itemView.findViewById(R.id.tvWeekDayNumber)
            private val dotsContainer: LinearLayout = itemView.findViewById(R.id.weekDayDots)
            private val root: View = itemView.findViewById(R.id.weekDayRoot)

            fun bind(item: WeekDayItem, onDayClick: (Date) -> Unit) {
                tvDayName.text = item.dayName
                tvDayNumber.text = item.dayNumber.toString()

                val ctx = itemView.context
                if (item.isToday) {
                    root.setBackgroundColor(ctx.getColor(R.color.calendar_today_background))
                    tvDayNumber.setTextColor(ctx.getColor(R.color.calendar_today_text))
                } else {
                    val attr = TypedValue()
                    ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, attr, true)
                    root.setBackgroundResource(attr.resourceId)
                    tvDayNumber.setTextColor(ctx.getColor(R.color.primary))
                }

                dotsContainer.removeAllViews()
                val size = (4 * ctx.resources.displayMetrics.density).toInt()
                for (color in item.taskColors.take(5)) {
                    val dot = View(ctx)
                    val drawable = GradientDrawable().apply {
                        setShape(GradientDrawable.OVAL)
                        setColor(color)
                    }
                    dot.background = drawable
                    val lp = LinearLayout.LayoutParams(size, size)
                    lp.marginEnd = (2 * ctx.resources.displayMetrics.density).toInt()
                    dotsContainer.addView(dot, lp)
                }

                root.setOnClickListener { onDayClick(item.date) }
            }
        }
    }
}
