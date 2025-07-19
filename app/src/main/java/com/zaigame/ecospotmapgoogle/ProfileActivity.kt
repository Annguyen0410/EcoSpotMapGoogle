package com.zaigame.ecospotmapgoogle

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.zaigame.ecospotmapgoogle.adapters.BadgeAdapter
import com.zaigame.ecospotmapgoogle.adapters.RecentActivityAdapter
import com.zaigame.ecospotmapgoogle.models.Badge
import com.zaigame.ecospotmapgoogle.models.CheckIn
import com.zaigame.ecospotmapgoogle.services.GamificationService
import com.zaigame.ecospotmapgoogle.models.UserProfile
import com.zaigame.ecospotmapgoogle.models.EcoSpotType
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var gamificationService: GamificationService
    private lateinit var badgeAdapter: BadgeAdapter
    private lateinit var recentActivityAdapter: RecentActivityAdapter

    // UI Components
    private lateinit var tvUsername: TextView
    private lateinit var tvLevel: TextView
    private lateinit var tvTotalPoints: TextView
    private lateinit var tvProgressPercentage: TextView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var tvCheckinsCount: TextView
    private lateinit var tvSpotsVisited: TextView
    private lateinit var tvSpotsSuggested: TextView
    private lateinit var tvDaysActive: TextView
    private lateinit var rvBadges: RecyclerView
    private lateinit var rvRecentActivity: RecyclerView
    private lateinit var fabQuickActions: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize services
        gamificationService = GamificationService(this)

        // Initialize UI components
        initializeViews()
        setupToolbar()
        setupRecyclerViews()
        loadUserProfile()
        setupQuickActions()
    }

    private fun initializeViews() {
        tvUsername = findViewById(R.id.tv_username)
        tvLevel = findViewById(R.id.tv_level)
        tvTotalPoints = findViewById(R.id.tv_total_points)
        tvProgressPercentage = findViewById(R.id.tv_progress_percentage)
        progressBar = findViewById(R.id.progress_bar)
        tvCheckinsCount = findViewById(R.id.tv_checkins_count)
        tvSpotsVisited = findViewById(R.id.tv_spots_visited)
        tvSpotsSuggested = findViewById(R.id.tv_spots_suggested)
        tvDaysActive = findViewById(R.id.tv_days_active)
        rvBadges = findViewById(R.id.rv_badges)
        rvRecentActivity = findViewById(R.id.rv_recent_activity)
        fabQuickActions = findViewById(R.id.fab_quick_actions)
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Profile"
    }

    private fun setupRecyclerViews() {
        // Setup Badges RecyclerView
        badgeAdapter = BadgeAdapter()
        rvBadges.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvBadges.adapter = badgeAdapter

        // Setup Recent Activity RecyclerView
        recentActivityAdapter = RecentActivityAdapter()
        rvRecentActivity.layoutManager = LinearLayoutManager(this)
        rvRecentActivity.adapter = recentActivityAdapter
    }

    private fun loadUserProfile() {
        val userProfile = gamificationService.getCurrentUserProfile()
        
        if (userProfile != null) {
            // Create user profile if it doesn't exist
            displayUserProfile(userProfile)
        } else {
            // Create a demo profile for testing
            val demoProfile = gamificationService.createUserProfile(
                userId = "demo_user_${System.currentTimeMillis()}",
                username = "Eco Explorer",
                email = "demo@example.com"
            )
            displayUserProfile(demoProfile)
        }
    }

    private fun displayUserProfile(profile: UserProfile) {
        // Update profile header
        tvUsername.text = profile.username
        tvLevel.text = "Level ${profile.level}"
        tvTotalPoints.text = "${profile.totalPoints} pts"

        // Update progress bar
        val pointsForCurrentLevel = (profile.level - 1) * GamificationService.POINTS_PER_LEVEL
        val pointsInCurrentLevel = profile.totalPoints - pointsForCurrentLevel
        val progressPercentage = (pointsInCurrentLevel.toFloat() / GamificationService.POINTS_PER_LEVEL * 100).toInt()
        
        tvProgressPercentage.text = "$progressPercentage%"
        progressBar.progress = progressPercentage

        // Update statistics
        val stats = gamificationService.getUserStats()
        tvCheckinsCount.text = stats.totalCheckIns.toString()
        tvSpotsVisited.text = stats.uniqueSpotsVisited.toString()
        tvSpotsSuggested.text = stats.spotsSuggested.toString()
        tvDaysActive.text = stats.daysActive.toString()

        // Update badges
        badgeAdapter.updateBadges(profile.badges)

        // Update recent activity
        val recentCheckIns = profile.checkIns.take(5).map { checkIn ->
            RecentActivityItem(
                title = "Checked in at ${checkIn.spotName}",
                details = "+${checkIn.pointsEarned} points • ${formatTimeAgo(checkIn.checkInDate)}",
                pointsEarned = "+${checkIn.pointsEarned}",
                iconResId = getSpotTypeIcon(checkIn.spotType)
            )
        }
        recentActivityAdapter.updateActivities(recentCheckIns)
    }

    private fun setupQuickActions() {
        fabQuickActions.setOnClickListener {
            // Show quick actions menu (check-in, suggest spot, etc.)
            showQuickActionsDialog()
        }
    }

    private fun showQuickActionsDialog() {
        val actions = arrayOf(
            "Quick Check-in",
            "Suggest New Spot",
            "View Nearby Spots",
            "Share Profile"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Quick Actions")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> performQuickCheckIn()
                    1 -> suggestNewSpot()
                    2 -> viewNearbySpots()
                    3 -> shareProfile()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performQuickCheckIn() {
        // For demo purposes, we'll simulate a quick check-in
        val demoSpot = "Demo Eco Spot"
        val pointsEarned = gamificationService.performCheckIn(
            spotId = "demo_spot_${System.currentTimeMillis()}",
            spotName = demoSpot,
            spotType = EcoSpotType.FARMERS_MARKET,
            latitude = 0.0,
            longitude = 0.0
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Check-in Successful!")
            .setMessage("You've checked in at $demoSpot and earned $pointsEarned points!")
            .setPositiveButton("Great!") { _, _ ->
                loadUserProfile() // Refresh the profile
            }
            .show()
    }

    private fun suggestNewSpot() {
        // Navigate to spot suggestion dialog
        showSpotSuggestionDialog()
    }

    private fun showSpotSuggestionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_suggest_spot, null)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Suggest New Eco Spot")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                // Handle spot suggestion submission
                MaterialAlertDialogBuilder(this)
                    .setTitle("Thank You!")
                    .setMessage("Your suggestion has been submitted for review. You'll earn 50 points if approved!")
                    .setPositiveButton("OK") { _, _ ->
                        // Award points for suggestion
                        gamificationService.addPoints(50, "Spot Suggestion")
                        loadUserProfile() // Refresh the profile
                    }
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun viewNearbySpots() {
        // For demo purposes, show a dialog with nearby spots
        val nearbySpots = listOf(
            "Green Market - 0.5 km away",
            "Recycling Center - 1.2 km away",
            "Thrift Store - 0.8 km away",
            "Community Garden - 1.5 km away"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Nearby Eco Spots")
            .setItems(nearbySpots.toTypedArray()) { _, which ->
                // Handle spot selection
                MaterialAlertDialogBuilder(this)
                    .setTitle("Navigate to ${nearbySpots[which]}")
                    .setMessage("Would you like to get directions to this location?")
                    .setPositiveButton("Get Directions") { _, _ ->
                        openNavigation(nearbySpots[which])
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun shareProfile() {
        val userProfile = gamificationService.getCurrentUserProfile()
        if (userProfile != null) {
            val shareText = """
                Check out my eco-friendly journey!
                
                🌱 Level ${userProfile.level} Eco Explorer
                🏆 ${userProfile.badges.size} badges earned
                📍 ${gamificationService.getUserStats().totalCheckIns} check-ins
                🌍 ${gamificationService.getUserStats().uniqueSpotsVisited} unique spots visited
                
                Join me in making the world greener! 🌿
            """.trimIndent()

            val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }

            val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Profile")
            startActivity(shareIntent)
        }
    }

    private fun formatTimeAgo(date: Date): String {
        val now = Date()
        val diffInMillis = now.time - date.time
        val diffInHours = diffInMillis / (1000 * 60 * 60)
        val diffInDays = diffInHours / 24

        return when {
            diffInDays > 0 -> "$diffInDays days ago"
            diffInHours > 0 -> "$diffInHours hours ago"
            else -> "Just now"
        }
    }

    private fun getSpotTypeIcon(spotType: EcoSpotType): Int {
        return when (spotType) {
            EcoSpotType.FARMERS_MARKET -> R.drawable.ic_farmers_market
            EcoSpotType.RECYCLING_CENTER -> R.drawable.ic_recycling
            EcoSpotType.THRIFT_STORE -> R.drawable.ic_thrift_store
            else -> android.R.drawable.ic_menu_myplaces
        }
    }

    private fun openNavigation(locationName: String) {
        // Extract location name and create navigation intent
        val cleanLocationName = locationName.split(" - ")[0]
        
        // Create a geo URI for navigation
        val geoUri = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(cleanLocationName)}")
        val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, geoUri)
        
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // Fallback to web search if no map app is available
            val searchIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, 
                android.net.Uri.parse("https://www.google.com/maps/search/${android.net.Uri.encode(cleanLocationName)}"))
            startActivity(searchIntent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile data when returning to this activity
        loadUserProfile()
    }
}

/**
 * Data class for recent activity items
 */
data class RecentActivityItem(
    val title: String,
    val details: String,
    val pointsEarned: String,
    val iconResId: Int
) 