package com.phani.voicelock.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.phani.voicelock.lock.LockService
import com.phani.voicelock.voiceLockApp

private object Routes {
    const val ONBOARDING = "onboarding"
    const val ENROLL_FIRST = "enroll_first"
    const val HOME = "home"
    const val ENROLL = "enroll"
    const val APPS = "apps"
    const val SETTINGS = "settings"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val prefs = ctx.voiceLockApp.prefs
    val vm: MainViewModel = viewModel()

    NavHost(
        navController = nav,
        startDestination = if (prefs.onboardingDone) Routes.HOME else Routes.ONBOARDING,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onContinue = { nav.navigate(Routes.ENROLL_FIRST) })
        }
        composable(Routes.ENROLL_FIRST) {
            EnrollScreen(
                onBack = { nav.popBackStack() },
                onFinished = {
                    prefs.onboardingDone = true
                    vm.refresh()
                    nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                vm = vm,
                onToggleLock = { on ->
                    vm.setLockEnabled(on)
                    if (on) LockService.start(ctx) else LockService.stop(ctx)
                },
                onApps = { nav.navigate(Routes.APPS) },
                onVoice = { nav.navigate(Routes.ENROLL) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.ENROLL) {
            EnrollScreen(
                onBack = { nav.popBackStack() },
                onFinished = { vm.refresh(); nav.popBackStack() },
            )
        }
        composable(Routes.APPS) { AppsScreen(vm, onBack = { nav.popBackStack() }) }
        composable(Routes.SETTINGS) {
            SettingsScreen(vm, onBack = { nav.popBackStack() }, onRerecord = { nav.navigate(Routes.ENROLL) })
        }
    }
}
