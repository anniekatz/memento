package com.annie.memento.platform

import java.time.LocalDate

actual fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
