package com.snowi.snuzznoise.presentation.feature.profile.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class ActivityProfile(
    val id: String = "",
    val name: String = "",
    val threshold: Int = 60,
    val iconName: String = "Meeting"
) {
    @Composable
    fun getIcon(): ImageVector {
        return iconMap[iconName] ?: Icons.Default.Tune
    }
}

val iconMap = mapOf(
    "Meeting" to Icons.Default.Group,
    "Office" to Icons.Default.Work,
    "Lecture" to Icons.Default.PresentToAll,
    "Collaboration" to Icons.Default.People,
    "Conference" to Icons.Default.VideoCall,
    "Presentation" to Icons.Default.Slideshow,
    "Interview" to Icons.Default.RecordVoiceOver,
    "Workshop" to Icons.Default.Build,
    "Focus" to Icons.Default.CenterFocusStrong,
    "Deep Work" to Icons.Default.Visibility,
    "Study" to Icons.Default.School,
    "Library" to Icons.Default.LocalLibrary,
    "Reading" to Icons.AutoMirrored.Filled.MenuBook,
    "Research" to Icons.Default.Search,
    "Writing" to Icons.Default.Edit,
    "Exam" to Icons.Default.Quiz,
    "Quiet Zone" to Icons.Default.SelfImprovement,
    "Meditation" to Icons.Default.Spa,
    "Sleep" to Icons.Default.Bedtime,
    "Rest" to Icons.Default.Weekend,
    "Relaxation" to Icons.Default.NaturePeople,
    "Yoga" to Icons.Default.FitnessCenter,
    "Music" to Icons.Default.MusicNote,
    "Gaming" to Icons.Default.SportsEsports,
    "Movies" to Icons.Default.Movie,
    "Social" to Icons.Default.Groups,
    "Party" to Icons.Default.Celebration,
    "Podcast" to Icons.Default.Podcasts,
    "Cooking" to Icons.Default.Restaurant,
    "Exercise" to Icons.Default.FitnessCenter,
    "Commute" to Icons.Default.DirectionsCar,
    "Walking" to Icons.AutoMirrored.Filled.DirectionsWalk,
    "Shopping" to Icons.Default.ShoppingCart,
    "Cleaning" to Icons.Default.CleaningServices,
    "Coding" to Icons.Default.Code,
    "Morning" to Icons.Default.WbSunny,
    "Evening" to Icons.Default.NightlightRound,
    "Night" to Icons.Default.DarkMode,
    "Weekend" to Icons.Default.Weekend,
    "Do Not Disturb" to Icons.Default.DoNotDisturb,
    "Silent" to Icons.AutoMirrored.Filled.VolumeOff,
    "Vibrate" to Icons.Default.Vibration,
    "Emergency" to Icons.Default.Emergency,
    "Custom" to Icons.Default.Settings
)