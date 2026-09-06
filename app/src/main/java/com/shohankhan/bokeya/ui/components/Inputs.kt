package com.shohankhan.bokeya.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shohankhan.bokeya.core.BanglaDate
import com.shohankhan.bokeya.core.BanglaNumbers
import com.shohankhan.bokeya.core.CurrencyFormatter
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.ui.theme.Radius
import com.shohankhan.bokeya.ui.theme.bokeya
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Currency-aware amount field. Accepts only digits and a single decimal separator,
 * shows a live formatted preview and never permits a negative amount.
 */
@Composable
fun MoneyInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "টাকার পরিমাণ",
    isError: Boolean = false,
    errorText: String? = null,
    autoFocus: Boolean = false,
) {
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { raw ->
                val normalized = BanglaNumbers.toWesternDigits(raw)
                    .filter { it.isDigit() || it == '.' }
                val singleDot = buildString {
                    var seenDot = false
                    for (ch in normalized) {
                        if (ch == '.') {
                            if (seenDot) continue
                            seenDot = true
                        }
                        append(ch)
                    }
                }
                val limited = if (singleDot.contains('.')) {
                    val (whole, frac) = singleDot.split('.', limit = 2)
                    whole.take(12) + "." + frac.take(2)
                } else {
                    singleDot.take(12)
                }
                onValueChange(limited)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            leadingIcon = {
                Text(
                    CurrencyFormatter.SYMBOL,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 14.dp),
                )
            },
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
            ),
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(Radius.md),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.bokeya.surface2,
                unfocusedContainerColor = MaterialTheme.bokeya.surface1,
            ),
        )
        val parsed = Money.parseOrNull(value)
        if (isError && errorText != null) {
            Text(
                errorText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 14.dp, top = 4.dp),
            )
        } else if (parsed != null && parsed.isPositive) {
            Text(
                CurrencyFormatter.format(parsed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.bokeya.muted,
                modifier = Modifier.padding(start = 14.dp, top = 4.dp),
            )
        }
    }
}

@Composable
fun BokeyaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(Radius.md),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.bokeya.surface2,
            unfocusedContainerColor = MaterialTheme.bokeya.surface1,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "তারিখ",
) {
    var showPicker by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
        shape = RoundedCornerShape(Radius.md),
        color = MaterialTheme.bokeya.surface2,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.bokeya.muted)
                Text(
                    BanglaDate.full(date),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
            Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text("ঠিক আছে") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("বাতিল") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

data class SelectorOption(val id: String, val label: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipSelector(
    options: List<SelectorOption>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    wrap: Boolean = true,
) {
    Column(modifier.fillMaxWidth()) {
        label?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.bokeya.muted)
            Spacer(Modifier.height(8.dp))
        }
        if (wrap) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                options.forEach { SelectableChip(it, it.id == selectedId, onSelect) }
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { SelectableChip(it, it.id == selectedId, onSelect) }
            }
        }
    }
}

@Composable
private fun SelectableChip(option: SelectorOption, selected: Boolean, onSelect: (String) -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.bokeya.surface2,
        shape = RoundedCornerShape(Radius.pill),
        modifier = Modifier.clickable { onSelect(option.id) },
    ) {
        Text(
            option.label,
            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.sm),
        color = MaterialTheme.bokeya.surface2,
    ) {
        Row(Modifier.padding(4.dp)) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    Modifier
                        .weight(1f)
                        .height(42.dp)
                        .padding(2.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                            RoundedCornerShape(Radius.pill),
                        )
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
