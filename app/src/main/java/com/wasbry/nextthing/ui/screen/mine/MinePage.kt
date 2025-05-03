package com.wasbry.nextthing.ui.screen.mine

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wasbry.nextthing.database.TodoDatabase
import com.wasbry.nextthing.database.repository.PersonalTimeRepository
import com.wasbry.nextthing.ui.componet.mine.PersonalTimeControl
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModel
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModelFactory

@Composable
fun MinePage(viewModel: PersonalTimeViewModel) {
    PersonalTimeControl(viewModel = viewModel)
}