package com.zirakusa.calculadora.ui.convert

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.graphics.Color
import com.zirakusa.calculadora.ui.CalcColors
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirakusa.calculadora.R
import com.zirakusa.calculadora.engine.Category
import com.zirakusa.calculadora.engine.Units

/** Conversor de unidades: a category (length, mass, temperature…) and two units of it. Works offline. */
@Composable fun UnitsScreen(onBack: () -> Unit) {
    var category by remember { mutableStateOf(Category.LENGTH) }
    val state = remember(category) {
        ConverterState(category.units.take(2).map { it.symbol }, 6) { a, f, t -> Units.convert(a, category.unit(f), category.unit(t)) }
    }
    ConverterScreen(stringResource(R.string.units), state, category.units.map { Choice(it.symbol, it.symbol, "") }, "",
        emptyList(), onBack) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Category.entries.forEach { c -> FilterChip(c == category, { category = c }, { Text(stringResource(categoryName(c))) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CalcColors.Accent, selectedLabelColor = Color.Black)) }
        }
    }
}

fun categoryName(c: Category) = when (c) {
    Category.LENGTH -> R.string.unit_length; Category.MASS -> R.string.unit_mass; Category.TEMPERATURE -> R.string.unit_temperature
    Category.AREA -> R.string.unit_area; Category.VOLUME -> R.string.unit_volume; Category.SPEED -> R.string.unit_speed
    Category.DATA -> R.string.unit_data; Category.TIME -> R.string.unit_time
}
