package com.zaigame.ecospotmapgoogle.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zaigame.ecospotmapgoogle.R
import com.zaigame.ecospotmapgoogle.models.Badge
import java.text.SimpleDateFormat
import java.util.*

class BadgeAdapter : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    private var badges: List<Badge> = emptyList()

    fun updateBadges(newBadges: List<Badge>) {
        badges = newBadges
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        holder.bind(badges[position])
    }

    override fun getItemCount(): Int = badges.size

    class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardBadge: View = itemView.findViewById(R.id.card_badge)
        private val ivBadgeIcon: ImageView = itemView.findViewById(R.id.iv_badge_icon)
        private val tvBadgeName: TextView = itemView.findViewById(R.id.tv_badge_name)
        private val tvBadgeDescription: TextView = itemView.findViewById(R.id.tv_badge_description)
        private val ivBadgeStatus: ImageView = itemView.findViewById(R.id.iv_badge_status)

        fun bind(badge: Badge) {
            tvBadgeName.text = badge.name
            tvBadgeDescription.text = badge.description
            ivBadgeIcon.setImageResource(badge.iconResId)

            // Update visual state based on whether badge is unlocked
            if (badge.isUnlocked) {
                cardBadge.alpha = 1.0f
                ivBadgeStatus.visibility = View.VISIBLE
                ivBadgeIcon.setColorFilter(itemView.context.getColor(android.R.color.holo_green_dark))
            } else {
                cardBadge.alpha = 0.6f
                ivBadgeStatus.visibility = View.GONE
                ivBadgeIcon.setColorFilter(itemView.context.getColor(android.R.color.darker_gray))
            }

            // Set click listener for badge details
            itemView.setOnClickListener {
                showBadgeDetails(badge)
            }
        }

        private fun showBadgeDetails(badge: Badge) {
            val context = itemView.context
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

            val dialogTitle = if (badge.isUnlocked) "Badge Unlocked!" else "Badge Details"

            val unlockDateText = if (badge.isUnlocked && badge.unlockedDate != null) {
                "\n\n🏆 Unlocked on: ${dateFormat.format(badge.unlockedDate)}"
            } else ""

            val progressText = if (!badge.isUnlocked) {
                "\n\n📊 Points required: ${badge.pointsRequired}"
            } else ""

            val message = """
                ${badge.description}
                
                🎯 Requirements: ${getBadgeRequirements(badge)}
                $unlockDateText
                $progressText
            """.trimIndent()

            MaterialAlertDialogBuilder(context)
                .setTitle(dialogTitle)
                .setMessage(message)
                .setIcon(badge.iconResId)
                .setPositiveButton("Got it!", null)
                .show()
        }

        private fun getBadgeRequirements(badge: Badge): String {
            return when (badge.name) {
                "First Steps" -> "Complete your first check-in"
                "Explorer" -> "Visit 5 different eco-spots"
                "Regular" -> "Check in for 7 consecutive days"
                "Contributor" -> "Suggest 3 new eco-spots"
                "Sustainability Champion" -> "Earn 1000 points"
                "Community Builder" -> "Help 10 other users"
                else -> "Complete specific achievements"
            }
        }
    }
}