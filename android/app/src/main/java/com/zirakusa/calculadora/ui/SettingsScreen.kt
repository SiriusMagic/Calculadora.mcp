package com.zirakusa.calculadora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.calculadora.IconCard
import com.zirakusa.calculadora.R
import com.zirakusa.calculadora.mcp.showMcpOptions


@Composable fun SettingsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(CalcColors.Background).systemBarsPadding().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Round(Icons.AutoMirrored.Outlined.ArrowBack, R.string.back, onClick = onBack)
            Text(stringResource(R.string.settings), Modifier.padding(start = 16.dp), fontSize = 22.sp)
        }
        IconCard(CalcColors.Chip, CalcColors.Text, CalcColors.Muted, CalcColors.Accent)
        val activity = LocalActivity.current
        OutlinedButton({ activity?.let(::showMcpOptions) }, enabled = activity != null) {
            Text(stringResource(R.string.mcp_option_title))
        }
    }
}
