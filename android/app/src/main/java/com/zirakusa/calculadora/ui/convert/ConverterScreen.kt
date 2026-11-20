package com.zirakusa.calculadora.ui.convert

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.calculadora.R
import com.zirakusa.calculadora.ui.CalcColors
import com.zirakusa.calculadora.ui.Round

/** One choice in a row's list: its code, what it is called, and a badge (a flag, a symbol). */
data class Choice(val code: String, val name: String, val badge: String)

/** Shared by the currency and unit converters: rows, a footer line, an optional ⋮ menu and the converter keypad. */
@Composable fun ConverterScreen(title: String, state: ConverterState, choices: List<Choice>, footer: String,
    menu: List<Pair<String, () -> Unit>>, onBack: () -> Unit, top: @Composable () -> Unit = {}) {
    var picking by remember { mutableStateOf<Int?>(null) }
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(CalcColors.Background).systemBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Round(Icons.AutoMirrored.Outlined.ArrowBack, R.string.back, onClick = onBack)
            Text(title, Modifier.weight(1f).padding(horizontal = 16.dp), fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (menu.isNotEmpty()) Box {
                Round(Icons.Outlined.MoreVert, R.string.more) { open = true }
                DropdownMenu(open, { open = false }) { menu.forEach { (label, action) -> DropdownMenuItem({ Text(label) }, { open = false; action() }) } }
            }
        }
        top()
        state.codes.indices.forEach { row ->
            val choice = choices.firstOrNull { it.code == state.codes[row] }
            Row(Modifier.fillMaxWidth().clickable { state.activate(row) }.padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(choice?.badge.orEmpty(), Modifier.width(52.dp), fontSize = 30.sp)
                Row(Modifier.clickable { picking = row }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(state.codes[row], fontSize = 22.sp)
                    Icon(Icons.Default.ArrowDropDown, null, tint = CalcColors.Muted)
                }
                Text(state.shown(row), Modifier.weight(1f), fontSize = 30.sp, maxLines = 1, overflow = TextOverflow.StartEllipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    color = if (row == state.active) CalcColors.Accent else CalcColors.Text)
            }
            HorizontalDivider(color = CalcColors.Chip)
        }
        Spacer(Modifier.weight(1f))
        Text(footer, Modifier.fillMaxWidth().padding(vertical = 12.dp), color = CalcColors.Muted, fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        ConverterKeypad(state, Modifier.fillMaxWidth().fillMaxHeight(.62f))
    }
    picking?.let { row ->
        AlertDialog({ picking = null }, confirmButton = {}, title = { Text(stringResource(R.string.choose)) }, text = {
            LazyColumn(Modifier.heightIn(max = 460.dp)) {
                items(choices, key = { it.code }) { choice ->
                    Row(Modifier.fillMaxWidth().clickable { state.pick(row, choice.code); picking = null }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(choice.badge, Modifier.width(44.dp), fontSize = 22.sp)
                        Column { Text(choice.code); Text(choice.name, color = CalcColors.Muted, fontSize = 13.sp) }
                    }
                }
            }
        })
    }
}

/** 7 8 9 | AC (tall) · 4 5 6 · 1 2 3 | ⌫ (tall) · 00 0 , — as on the phone's converter. */
@Composable private fun ConverterKeypad(state: ConverterState, modifier: Modifier) {
    val gap = 12.dp
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        Column(Modifier.weight(3f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
            listOf(listOf("7", "8", "9"), listOf("4", "5", "6"), listOf("1", "2", "3"), listOf("00", "0", ",")).forEach { row ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { label -> Pad(label, Modifier.weight(1f).fillMaxHeight(), CalcColors.Key) { state.type(label) } }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
            Pad("AC", Modifier.weight(1f).fillMaxWidth(), CalcColors.Operator, state::clear)
            Box(Modifier.weight(1f).fillMaxWidth().clip(CircleShape).background(CalcColors.Operator).clickable(onClick = state::back),
                contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Outlined.Backspace, stringResource(R.string.key_back), tint = Color.White) }
        }
    }
}

@Composable private fun Pad(label: String, modifier: Modifier, color: Color, onClick: () -> Unit) {
    Box(modifier.clip(CircleShape).background(color).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = Color.White, fontSize = 30.sp)
    }
}
