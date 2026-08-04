package com.annie.memento.model

// bulk import payload

data class ImportSideSpec(
    val text: String,
    val examples: List<String>,
    val notes: String?,
    val imagePaths: List<String>,
    val audioPaths: List<String>,
    val isRichText: Boolean,
)

data class ImportCardSpec(
    val levelPosition: Int?,
    val front: ImportSideSpec,
    val back: ImportSideSpec,
    val tagNames: Set<String>,
    //srs state carried into export/import for now
    val seen: Boolean = false,
    val seenDay: Long? = null,
    val mastery: Int = 0,
    val nextReviewDay: Long? = null,
)

data class ImportDeckSpec(
    val name: String,
    val description: String?,
    val frontName: String,
    val backName: String,
    val levels: List<LevelDraft>,
    val tags: List<TagDraft>,
    val cards: List<ImportCardSpec>,
    val photoPath: String? = null,
    val isSrs: Boolean = false,
    val newCardsPerDay: Int = DEFAULT_NEW_CARDS_PER_DAY,
)
