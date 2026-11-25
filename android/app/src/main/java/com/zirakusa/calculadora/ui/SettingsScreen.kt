package com.zirakusa.calculadora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.calculadora.IconCard
import com.zirakusa.calculadora.R

/** Ajustes: the app's icon. The Modo MCP is turned on from Arka, so it only says so. */
@Composable fun SettingsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(CalcColors.Background).systemBarsPadding().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Round(Icons.AutoMirrored.Outlined.ArrowBack, R.string.back, onClick = onBack)
            Text(stringResource(R.string.settings), Modifier.padding(start = 16.dp), fontSize = 22.sp)
        }
        IconCard(CalcColors.Chip, CalcColors.Text, CalcColors.Muted, CalcColors.Accent)
        Text(stringResource(R.string.mcp_by_arka), color = CalcColors.Muted, fontSize = 13.sp)
    }
}
