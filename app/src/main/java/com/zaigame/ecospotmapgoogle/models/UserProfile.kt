package com.zaigame.ecospotmapgoogle.models

import java.util.Date

/**
 * User profile data class for gamification system
 * Stores user information, points, badges, and activity history
 */
data class UserProfile(
    val userId: String,
    val username: String,
    val email: String,
    val totalPoints: Int = 0,
    val level: Int = 1,
    val badges: List<Badge> = emptyList(),
    val checkIns: List<CheckIn> = emptyList(),
    val suggestedSpots: List<SuggestedSpot> = emptyList(),
    val dateJoined: Date = Date(),
    val lastActive: Date = Date(),
    val sustainabilityScore: Int = 0,
    val totalEcoActions: Int = 0
)

/**
 * Badge system for gamification
 */
data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val iconResId: Int,
    val pointsRequired: Int,
    val category: BadgeCategory,
    val isUnlocked: Boolean = false,
    val unlockedDate: Date? = null
)

/**
 * Badge categories for different types of achievements
 */
enum class BadgeCategory {
    EXPLORER,      // Visit different types of eco-spots
    CONTRIBUTOR,   // Suggest new spots
    REGULAR,       // Regular check-ins
    SUSTAINABILITY, // High sustainability score
    COMMUNITY,     // Help other users
    SPECIAL        // Special events or achievements
}

/**
 * Check-in record for user activity tracking
 */
data class CheckIn(
    val id: String,
    val spotId: String,
    val spotName: String,
    val spotType: EcoSpotType,
    val checkInDate: Date,
    val pointsEarned: Int,
    val location: LatLng,
    val notes: String? = null,
    val photos: List<String> = emptyList()
)

/**
 * User-suggested eco-spot for community contribution
 */
data class SuggestedSpot(
    val id: String,
    val name: String,
    val description: String,
    val spotType: EcoSpotType,
    val location: LatLng,
    val address: String,
    val suggestedBy: String,
    val suggestedDate: Date,
    val status: SpotStatus = SpotStatus.PENDING,
    val photos: List<String> = emptyList(),
    val contactInfo: String? = null,
    val hours: String? = null,
    val tags: List<String> = emptyList(),
    val upvotes: Int = 0,
    val downvotes: Int = 0
)

/**
 * Status of suggested spots
 */
enum class SpotStatus {
    PENDING,    // Waiting for review
    APPROVED,   // Verified and added to main database
    REJECTED,   // Not suitable or duplicate
    UNDER_REVIEW // Being reviewed by moderators
}

/**
 * Eco-spot types for categorization
 */
enum class EcoSpotType {
    FARMERS_MARKET,
    RECYCLING_CENTER,
    THRIFT_STORE,
    COMMUNITY_GARDEN,
    ELECTRIC_VEHICLE_CHARGING,
    SUSTAINABLE_RESTAURANT,
    ZERO_WASTE_STORE,
    REPAIR_CAFE,
    COMPOSTING_SITE,
    BIKE_SHARE_STATION,
    PUBLIC_TRANSPORT,
    GREEN_SPACE,
    SOLAR_INSTALLATION,
    WATER_REFILL_STATION,
    BULK_FOOD_STORE,
    OTHER
}

/**
 * Location data class for coordinates
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double
) 