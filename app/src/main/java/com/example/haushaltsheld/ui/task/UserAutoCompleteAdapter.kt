package com.example.haushaltsheld.ui.task

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.example.haushaltsheld.R
import com.example.haushaltsheld.model.User

/**
 * AutoCompleteTextView adapter for user selection.
 * Displays user-friendly values (name + email) but stores user ID/email internally.
 * 
 * Usage:
 * ```
 * val adapter = UserAutoCompleteAdapter(context, users)
 * autoCompleteTextView.setAdapter(adapter)
 * // When user selects: adapter.getItem(position) returns User object
 * ```
 */
class UserAutoCompleteAdapter(
    context: Context,
    private val users: List<User>
) : ArrayAdapter<User>(context, R.layout.item_user_autocomplete, users), Filterable {

    private var filteredUsers: List<User> = users

    override fun getCount(): Int = filteredUsers.size

    override fun getItem(position: Int): User? = filteredUsers.getOrNull(position)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_user_autocomplete, parent, false)

        val user = filteredUsers[position]
        val tvName: TextView = view.findViewById(R.id.tvUserName)
        val tvEmail: TextView = view.findViewById(R.id.tvUserEmail)

        // Display user-friendly values: name (bold) and email (below)
        if (user.name.isNotEmpty()) {
            tvName.text = user.name
            tvEmail.text = user.email
            tvEmail.visibility = View.VISIBLE
        } else {
            // Fallback: show email as name if no name available
            tvName.text = user.email
            tvEmail.visibility = View.GONE
        }

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val filterPattern = constraint?.toString()?.lowercase() ?: ""

                filteredUsers = if (filterPattern.isEmpty()) {
                    users
                } else {
                    users.filter { user ->
                        user.name.lowercase().contains(filterPattern) ||
                        user.email.lowercase().contains(filterPattern)
                    }
                }

                results.values = filteredUsers
                results.count = filteredUsers.size
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null) {
                    filteredUsers = results.values as List<User>
                    notifyDataSetChanged()
                }
            }
        }
    }

    /**
     * Get the selected user's email (for storing in task).
     */
    fun getSelectedUserEmail(position: Int): String? {
        return getItem(position)?.email
    }

    /**
     * Get the selected user's ID (for storing in task).
     */
    fun getSelectedUserId(position: Int): String? {
        return getItem(position)?.id
    }
}
