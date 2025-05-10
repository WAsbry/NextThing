package com.wasbry.nextthing.ui.screen.mine

import androidx.compose.runtime.Composable
import com.wasbry.nextthing.ui.componet.personaltime.PersonalTimeControl
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModel

@Composable
fun MinePage(viewModel: PersonalTimeViewModel) {
    PersonalTimeControl(viewModel = viewModel)
}