package com.annie.memento.platform

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate

actual fun todayEpochDay(): Long {
    val components = NSCalendar.currentCalendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
        fromDate = NSDate(),
    )
    return epochDayOf(components.year.toInt(), components.month.toInt(), components.day.toInt())
}
