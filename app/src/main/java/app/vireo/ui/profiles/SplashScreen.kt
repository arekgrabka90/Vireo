package app.vireo.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import app.vireo.data.ProfileRepository
import app.vireo.ui.components.Wordmark
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val repo: ProfileRepository,
) : ViewModel() {
    suspend fun hasProfiles(): Boolean = repo.count() > 0
}

@Composable
fun SplashScreen(
    onHasProfiles: () -> Unit,
    onNoProfiles: () -> Unit,
    vm: SplashViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        delay(700)
        if (vm.hasProfiles()) onHasProfiles() else onNoProfiles()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Wordmark(size = 44)
    }
}
