package com.annie.memento.model

// SRS
//local device time based

const val MAX_MASTERY = 15
const val DEFAULT_NEW_CARDS_PER_DAY = 15

enum class ReviewGrade { Red, Yellow, Green }

data class SrsUpdate(val mastery: Int, val nextReviewDay: Long?)

fun srsAfterGrade(currentMastery: Int, grade: ReviewGrade, today: Long): SrsUpdate = when (grade) {
    ReviewGrade.Red -> SrsUpdate(0, today) // stays due
    ReviewGrade.Yellow -> SrsUpdate(0, today + 1) //review tomorrow
    ReviewGrade.Green -> { //add to mastery count - mastery == 15 greens in a row
        val mastery = (currentMastery + 1).coerceAtMost(MAX_MASTERY)
        SrsUpdate(mastery, if (mastery >= MAX_MASTERY) null else today + mastery)
    }
}

fun srsForMastery(mastery: Int, today: Long): SrsUpdate {
    val clamped = mastery.coerceIn(0, MAX_MASTERY)
    return SrsUpdate(clamped, if (clamped >= MAX_MASTERY) null else today + maxOf(1, clamped))
}

val Card.isMastered: Boolean get() = seen && mastery >= MAX_MASTERY

fun Card.isDueOn(day: Long): Boolean =
    seen && mastery < MAX_MASTERY && nextReviewDay != null && nextReviewDay <= day

data class SrsDeckStatus(
    val dueToday: Int,
    val unseen: Int,
    val learnedToday: Int,
    val learnableToday: Int,
    val mastered: Int,
)

fun srsDeckStatus(cards: List<Card>, newCardsPerDay: Int, today: Long): SrsDeckStatus {
    val unseen = cards.count { !it.seen }
    val learnedToday = cards.count { it.seenDay == today }
    return SrsDeckStatus(
        dueToday = cards.count { it.isDueOn(today) },
        unseen = unseen,
        learnedToday = learnedToday,
        learnableToday = learnableToday(unseen, learnedToday, newCardsPerDay),
        mastered = cards.count { it.isMastered },
    )
}

fun learnableToday(unseen: Int, learnedToday: Int, newCardsPerDay: Int): Int =
    minOf(unseen, (newCardsPerDay - learnedToday).coerceAtLeast(0))
