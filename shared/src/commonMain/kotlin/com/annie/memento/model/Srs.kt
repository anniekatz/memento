package com.annie.memento.model

// SRS
//local device time based

const val MAX_MASTERY = 15
const val DEFAULT_NEW_CARDS_PER_DAY = 15

enum class ReviewGrade { Red, Orange, Yellow, Green }

// review: due today or mastered
enum class ReviewMode { Due, Mastered }

data class SrsUpdate(val mastery: Int, val nextReviewDay: Long?)

// mastery == days until next review
fun srsAfterGrade(currentMastery: Int, grade: ReviewGrade, today: Long): SrsUpdate = when (grade) {
    ReviewGrade.Red -> SrsUpdate(0, today) // wrong: mastery set 0, back in review deck
    ReviewGrade.Orange -> srsForMastery(currentMastery - 2, today) // hard: -2 mastery
    ReviewGrade.Yellow -> srsForMastery(currentMastery, today) // good: mastery same
    ReviewGrade.Green -> srsForMastery(currentMastery + 1, today) // easy: +1 mastery
}

// grading during a mastered review
fun srsAfterMasteredGrade(grade: ReviewGrade, today: Long): SrsUpdate? = when (grade) {
    ReviewGrade.Red -> srsForMastery(1, today) // wrong; mastery reset to 1
    ReviewGrade.Orange -> srsForMastery(3, today) // hard; mastery reset to 3
    ReviewGrade.Yellow -> srsForMastery(7, today) // good; mastery reset to 7
    ReviewGrade.Green -> null // easy: still mastered
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
