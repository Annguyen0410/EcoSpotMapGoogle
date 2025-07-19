package com.zaigame.ecospotmapgoogle.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zaigame.ecospotmapgoogle.R
import com.zaigame.ecospotmapgoogle.RecentActivityItem
import java.text.SimpleDateFormat
import java.util.*

class RecentActivityAdapter : RecyclerView.Adapter<RecentActivityAdapter.ActivityViewHolder>() {

    private var activities: List<RecentActivityItem> = emptyList()

    fun updateActivities(newActivities: List<RecentActivityItem>) {
        activities = newActivities
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(activities[position])
    }

    override fun getItemCount(): Int = activities.size

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivActivityIcon: ImageView = itemView.findViewById(R.id.iv_activity_icon)
        private val tvActivityTitle: TextView = itemView.findViewById(R.id.tv_activity_title)
        private val tvActivityDetails: TextView = itemView.findViewById(R.id.tv_activity_details)
        private val tvPointsEarned: TextView = itemView.findViewById(R.id.tv_points_earned)

        fun bind(activity: RecentActivityItem) {
            tvActivityTitle.text = activity.title
            tvActivityDetails.text = activity.details
            tvPointsEarned.text = activity.pointsEarned
            ivActivityIcon.setImageResource(activity.iconResId)

            // Set click listener for activity details
            itemView.setOnClickListener {
                showActivityDetails(activity)
            }
        }

        private fun showActivityDetails(activity: RecentActivityItem) {
            val context = itemView.context
            
            // Extract location name from activity title
            val locationName = activity.title.replace("Checked in at ", "")
            
            val message = """
                📍 Location: $locationName
                
                ${activity.details}
                
                🌟 Points earned: ${activity.pointsEarned}
                
                🗺️ Would you like to:
                • View on map
                • Get directions
                • Share this activity
            """.trimIndent()

            val options = arrayOf("View on Map", "Get Directions", "Share Activity", "Close")
            
            MaterialAlertDialogBuilder(context)
                .setTitle("Activity Details")
                .setMessage(message)
                .setIcon(activity.iconResId)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> viewOnMap(locationName)
                        1 -> getDirections(locationName)
                        2 -> shareActivity(activity)
                        3 -> { /* Close dialog */ }
                    }
                }
                .show()
        }

        private fun viewOnMap(locationName: String) {
            val context = itemView.context
            
            // Create a geo URI for viewing on map
            val geoUri = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(locationName)}")
            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, geoUri)
            
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Fallback to web search if no map app is available
                val searchIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, 
                    android.net.Uri.parse("https://www.google.com/maps/search/${android.net.Uri.encode(locationName)}"))
                context.startActivity(searchIntent)
            }
        }

        private fun getDirections(locationName: String) {
            val context = itemView.context
            
            // Create a navigation intent
            val navUri = android.net.Uri.parse("google.navigation:q=${android.net.Uri.encode(locationName)}")
            val navIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, navUri)
            navIntent.setPackage("com.google.android.apps.maps")
            
            if (navIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(navIntent)
            } else {
                // Fallback to general navigation
                val geoUri = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(locationName)}")
                val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, geoUri)
                context.startActivity(fallbackIntent)
            }
        }

        private fun shareActivity(activity: RecentActivityItem) {
            val context = itemView.context
            val locationName = activity.title.replace("Checked in at ", "")
            
            val shareText = """
                🌱 Just checked in at $locationName!
                
                ${activity.details}
                ${activity.pointsEarned} points earned! 🎉
                
                Join me in exploring eco-friendly spots! 🌿
            """.trimIndent()

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, "Share Activity")
            context.startActivity(shareIntent)
        }
    }
} 