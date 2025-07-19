package com.zaigame.ecospotmapgoogle.services

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zaigame.ecospotmapgoogle.R
import com.zaigame.ecospotmapgoogle.models.*
import java.util.*

/**
 * Service for handling gamification features including points, badges, and user progression
 */
class GamificationService(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("gamification_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // Points system
    companion object {
        const val POINTS_CHECK_IN = 10
        const val POINTS_FIRST_VISIT = 25
        const val POINTS_SPOT_SUGGESTION = 50
        const val POINTS_DAILY_STREAK = 5
        const val POINTS_WEEKLY_GOAL = 100
        const val POINTS_BADGE_UNLOCK = 25
        
        const val POINTS_PER_LEVEL = 100
        const val MAX_LEVEL = 100
    }
    
    /**
     * Get current user profile from local storage
     */
    fun getCurrentUserProfile(): UserProfile? {
        val userJson = sharedPreferences.getString("current_user", null)
        return if (userJson != null) {
            gson.fromJson(userJson, UserProfile::class.java)
        } else {
            null
        }
    }
    
    /**
     * Save user profile to local storage
     */
    fun saveUserProfile(profile: UserProfile) {
        val userJson = gson.toJson(profile)
        sharedPreferences.edit().putString("current_user", userJson).apply()
    }
    
    /**
     * Create a new user profile
     */
    fun createUserProfile(userId: String, username: String, email: String): UserProfile {
        val profile = UserProfile(
            userId = userId,
            username = username,
            email = email,
            badges = getDefaultBadges()
        )
        saveUserProfile(profile)
        return profile
    }
    
    /**
     * Process a check-in and award points
     */
    fun processCheckIn(spotId: String, spotName: String, spotType: EcoSpotType, location: LatLng, notes: String? = null): CheckInResult {
        val profile = getCurrentUserProfile() ?: return CheckInResult(false, 0, "User profile not found")
        
        // Check if this is a first-time visit to this spot
        val isFirstVisit = !profile.checkIns.any { it.spotId == spotId }
        
        // Calculate points earned
        var pointsEarned = POINTS_CHECK_IN
        if (isFirstVisit) {
            pointsEarned += POINTS_FIRST_VISIT
        }
        
        // Check for daily streak bonus
        val dailyStreakBonus = calculateDailyStreakBonus(profile)
        pointsEarned += dailyStreakBonus
        
        // Create check-in record
        val checkIn = CheckIn(
            id = UUID.randomUUID().toString(),
            spotId = spotId,
            spotName = spotName,
            spotType = spotType,
            checkInDate = Date(),
            pointsEarned = pointsEarned,
            location = location,
            notes = notes
        )
        
        // Update user profile
        val updatedProfile = profile.copy(
            totalPoints = profile.totalPoints + pointsEarned,
            checkIns = profile.checkIns + checkIn,
            lastActive = Date(),
            totalEcoActions = profile.totalEcoActions + 1
        )
        
        // Check for level up
        val levelUpResult = checkForLevelUp(updatedProfile)
        val finalProfile = levelUpResult.profile
        
        // Check for new badges
        val badgeResult = checkForNewBadges(finalProfile)
        val finalProfileWithBadges = badgeResult.profile
        
        // Save updated profile
        saveUserProfile(finalProfileWithBadges)
        
        return CheckInResult(
            success = true,
            pointsEarned = pointsEarned,
            message = buildCheckInMessage(pointsEarned, isFirstVisit, dailyStreakBonus, levelUpResult.leveledUp, badgeResult.newBadges)
        )
    }
    
    /**
     * Suggest a new eco-spot
     */
    fun suggestSpot(name: String, description: String, spotType: EcoSpotType, location: LatLng, address: String, contactInfo: String? = null, hours: String? = null): SuggestionResult {
        val profile = getCurrentUserProfile() ?: return SuggestionResult(false, 0, "User profile not found")
        
        val suggestedSpot = SuggestedSpot(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            spotType = spotType,
            location = location,
            address = address,
            suggestedBy = profile.userId,
            suggestedDate = Date(),
            contactInfo = contactInfo,
            hours = hours
        )
        
        // Award points for suggestion
        val pointsEarned = POINTS_SPOT_SUGGESTION
        
        // Update user profile
        val updatedProfile = profile.copy(
            totalPoints = profile.totalPoints + pointsEarned,
            suggestedSpots = profile.suggestedSpots + suggestedSpot,
            lastActive = Date()
        )
        
        // Check for level up and badges
        val levelUpResult = checkForLevelUp(updatedProfile)
        val badgeResult = checkForNewBadges(levelUpResult.profile)
        val finalProfile = badgeResult.profile
        
        saveUserProfile(finalProfile)
        
        return SuggestionResult(
            success = true,
            pointsEarned = pointsEarned,
            message = "Spot suggestion submitted! +$pointsEarned points earned",
            suggestedSpot = suggestedSpot
        )
    }
    
    /**
     * Check if user leveled up and update accordingly
     */
    private fun checkForLevelUp(profile: UserProfile): LevelUpResult {
        val newLevel = (profile.totalPoints / POINTS_PER_LEVEL) + 1
        val leveledUp = newLevel > profile.level && newLevel <= MAX_LEVEL
        
        return if (leveledUp) {
            val updatedProfile = profile.copy(level = newLevel)
            LevelUpResult(updatedProfile, true, newLevel)
        } else {
            LevelUpResult(profile, false, profile.level)
        }
    }
    
    /**
     * Check for new badges based on user activity
     */
    private fun checkForNewBadges(profile: UserProfile): BadgeResult {
        val newBadges = mutableListOf<Badge>()
        val updatedBadges = profile.badges.toMutableList()
        
        // Check for explorer badge (visit different spot types)
        val uniqueSpotTypes = profile.checkIns.map { it.spotType }.distinct().size
        if (uniqueSpotTypes >= 5 && !hasBadge(profile, "explorer_5")) {
            val badge = createBadge("explorer_5", "Explorer", "Visited 5 different types of eco-spots", BadgeCategory.EXPLORER, true)
            newBadges.add(badge)
            updatedBadges.add(badge)
        }
        
        // Check for contributor badge
        if (profile.suggestedSpots.size >= 3 && !hasBadge(profile, "contributor_3")) {
            val badge = createBadge("contributor_3", "Contributor", "Suggested 3 eco-spots", BadgeCategory.CONTRIBUTOR, true)
            newBadges.add(badge)
            updatedBadges.add(badge)
        }
        
        // Check for regular badge (daily check-ins)
        val recentCheckIns = profile.checkIns.filter { 
            val daysDiff = (Date().time - it.checkInDate.time) / (1000 * 60 * 60 * 24)
            daysDiff <= 7
        }
        if (recentCheckIns.size >= 7 && !hasBadge(profile, "regular_7")) {
            val badge = createBadge("regular_7", "Regular", "Checked in 7 days in a row", BadgeCategory.REGULAR, true)
            newBadges.add(badge)
            updatedBadges.add(badge)
        }
        
        // Check for sustainability badge
        if (profile.sustainabilityScore >= 1000 && !hasBadge(profile, "sustainability_1000")) {
            val badge = createBadge("sustainability_1000", "Sustainability Champion", "Reached 1000 sustainability points", BadgeCategory.SUSTAINABILITY, true)
            newBadges.add(badge)
            updatedBadges.add(badge)
        }
        
        val updatedProfile = profile.copy(badges = updatedBadges)
        return BadgeResult(updatedProfile, newBadges)
    }
    
    /**
     * Calculate daily streak bonus
     */
    private fun calculateDailyStreakBonus(profile: UserProfile): Int {
        val today = Calendar.getInstance().apply { 
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val hasCheckedInToday = profile.checkIns.any { 
            val checkInDate = Calendar.getInstance().apply { time = it.checkInDate }
            checkInDate.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        }
        
        val hasCheckedInYesterday = profile.checkIns.any {
            val checkInDate = Calendar.getInstance().apply { time = it.checkInDate }
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            checkInDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
        }
        
        return if (hasCheckedInToday && hasCheckedInYesterday) POINTS_DAILY_STREAK else 0
    }
    
    /**
     * Check if user has a specific badge
     */
    private fun hasBadge(profile: UserProfile, badgeId: String): Boolean {
        return profile.badges.any { it.id == badgeId }
    }
    
    /**
     * Create a badge
     */
    private fun createBadge(id: String, name: String, description: String, category: BadgeCategory, isUnlocked: Boolean): Badge {
        return Badge(
            id = id,
            name = name,
            description = description,
            iconResId = getBadgeIcon(category),
            pointsRequired = 0,
            category = category,
            isUnlocked = isUnlocked,
            unlockedDate = if (isUnlocked) Date() else null
        )
    }
    
    /**
     * Get badge icon resource ID based on category
     */
    private fun getBadgeIcon(category: BadgeCategory): Int {
        return when (category) {
            BadgeCategory.EXPLORER -> R.drawable.ic_compass
            BadgeCategory.CONTRIBUTOR -> R.drawable.ic_edit
            BadgeCategory.REGULAR -> R.drawable.ic_history
            BadgeCategory.SUSTAINABILITY -> R.drawable.ic_send
            BadgeCategory.COMMUNITY -> R.drawable.ic_share
            BadgeCategory.SPECIAL -> R.drawable.ic_star
        }
    }
    
    /**
     * Get default badges for new users
     */
    private fun getDefaultBadges(): List<Badge> {
        return listOf(
            createBadge("welcome", "Welcome", "Joined the eco-community", BadgeCategory.SPECIAL, true),
            createBadge("explorer_5", "Explorer", "Visited 5 different types of eco-spots", BadgeCategory.EXPLORER, false),
            createBadge("contributor_3", "Contributor", "Suggested 3 eco-spots", BadgeCategory.CONTRIBUTOR, false),
            createBadge("regular_7", "Regular", "Checked in 7 days in a row", BadgeCategory.REGULAR, false),
            createBadge("sustainability_1000", "Sustainability Champion", "Reached 1000 sustainability points", BadgeCategory.SUSTAINABILITY, false)
        )
    }
    
    /**
     * Build check-in message with all bonuses
     */
    private fun buildCheckInMessage(pointsEarned: Int, isFirstVisit: Boolean, dailyStreakBonus: Int, leveledUp: Boolean, newBadges: List<Badge>): String {
        val message = StringBuilder("Check-in successful! +$pointsEarned points")
        
        if (isFirstVisit) {
            message.append(" (First visit bonus!)")
        }
        
        if (dailyStreakBonus > 0) {
            message.append(" (+$dailyStreakBonus daily streak)")
        }
        
        if (leveledUp) {
            message.append(" 🎉 LEVEL UP!")
        }
        
        if (newBadges.isNotEmpty()) {
            message.append(" 🏆 New badge: ${newBadges.first().name}")
        }
        
        return message.toString()
    }
    
    /**
     * Get user statistics
     */
    fun getUserStats(): UserStats {
        val profile = getCurrentUserProfile() ?: return UserStats()
        
        return UserStats(
            totalPoints = profile.totalPoints,
            level = profile.level,
            totalCheckIns = profile.checkIns.size,
            uniqueSpotsVisited = profile.checkIns.map { it.spotId }.distinct().size,
            spotsSuggested = profile.suggestedSpots.size,
            badgesUnlocked = profile.badges.count { it.isUnlocked },
            totalBadges = profile.badges.size,
            sustainabilityScore = profile.sustainabilityScore,
            daysActive = calculateDaysActive(profile)
        )
    }
    
    /**
     * Calculate days since user joined
     */
    private fun calculateDaysActive(profile: UserProfile): Int {
        val daysDiff = (Date().time - profile.dateJoined.time) / (1000 * 60 * 60 * 24)
        return daysDiff.toInt()
    }

    /**
     * Add points to user profile for various activities
     */
    fun addPoints(points: Int, reason: String) {
        val profile = getCurrentUserProfile() ?: return
        
        val updatedProfile = profile.copy(
            totalPoints = profile.totalPoints + points,
            lastActive = Date()
        )
        
        // Check for level up
        val levelUpResult = checkForLevelUp(updatedProfile)
        val finalProfile = levelUpResult.profile
        
        saveUserProfile(finalProfile)
    }

    /**
     * Perform a check-in with simplified parameters (for demo purposes)
     */
    fun performCheckIn(spotId: String, spotName: String, spotType: EcoSpotType, latitude: Double, longitude: Double): Int {
        val location = LatLng(latitude, longitude)
        val result = processCheckIn(spotId, spotName, spotType, location)
        return result.pointsEarned
    }
}

/**
 * Result classes for gamification operations
 */
data class CheckInResult(
    val success: Boolean,
    val pointsEarned: Int,
    val message: String
)

data class SuggestionResult(
    val success: Boolean,
    val pointsEarned: Int,
    val message: String,
    val suggestedSpot: SuggestedSpot? = null
)

data class LevelUpResult(
    val profile: UserProfile,
    val leveledUp: Boolean,
    val newLevel: Int
)

data class BadgeResult(
    val profile: UserProfile,
    val newBadges: List<Badge>
)

data class UserStats(
    val totalPoints: Int = 0,
    val level: Int = 1,
    val totalCheckIns: Int = 0,
    val uniqueSpotsVisited: Int = 0,
    val spotsSuggested: Int = 0,
    val badgesUnlocked: Int = 0,
    val totalBadges: Int = 0,
    val sustainabilityScore: Int = 0,
    val daysActive: Int = 0
) 