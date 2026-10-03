package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random

object MidnightMixer {

    /**
     * Génère une graine déterministe basée sur la date courante (YYYYMMDD).
     * Exemple : 3 octobre 2026 -> 20261003L
     */
    fun getTodaySeed(calendar: Calendar = Calendar.getInstance()): Long {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return (year * 10000L) + (month * 100L) + day
    }

    /**
     * Mélange déterministe d'une liste selon la graine fournie (Fisher-Yates).
     * Tous les utilisateurs auront le même ordre le même jour.
     */
    fun <T> shuffleWithSeed(items: List<T>, seed: Long): List<T> {
        if (items.size <= 1) return items.toList()
        val mutable = items.toMutableList()
        val random = Random(seed)
        for (i in mutable.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = mutable[i]
            mutable[i] = mutable[j]
            mutable[j] = temp
        }
        return mutable
    }

    /**
     * Formatage lisible de la date pour le bandeau "Mix de Minuit"
     */
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE)
        return sdf.format(Date()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRANCE) else it.toString() }
    }

    /**
     * Calcule le temps restant en millisecondes avant le prochain minuit (heure locale)
     */
    fun getMillisUntilMidnight(): Long {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (midnight.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }

    fun formatCountdown(millis: Long): String {
        val totalSecs = millis / 1000
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return String.format(Locale.ROOT, "%02dh %02dm %02ds", hours, mins, secs)
    }
}
