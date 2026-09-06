package com.shohankhan.bokeya.ui.screens

import androidx.compose.runtime.saveable.Saver
import java.time.LocalDate

/** Keeps date pickers intact across configuration changes and process death. */
val LocalDateSaver: Saver<LocalDate, Long> = Saver(
    save = { it.toEpochDay() },
    restore = { LocalDate.ofEpochDay(it) },
)
