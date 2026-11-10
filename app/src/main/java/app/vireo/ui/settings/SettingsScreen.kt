package app.vireo.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.data.local.ProfileSettingsEntity
import app.vireo.ui.components.FocusableBox
import app.vireo.ui.components.ProfileAvatar
import app.vireo.ui.components.ScreenTitle
import androidx.compose.runtime.withFrameNanos

private data class SettingsCategory(
    val key: String,
    val title: Int,
    val description: Int,
    val icon: String,
    val isProfile: Boolean,
    val route: String? = null,
)

private val ProfileCategories = listOf(
    SettingsCategory("profile", R.string.settings_category_profile, R.string.settings_category_profile_desc, "◉", true),
    SettingsCategory("appearance", R.string.settings_category_appearance, R.string.settings_category_appearance_desc, "◈", true),
    SettingsCategory("playback", R.string.settings_category_playback, R.string.settings_category_playback_desc, "▶", true),
    SettingsCategory("live", R.string.settings_category_live, R.string.settings_category_live_desc, "▣", true),
    SettingsCategory("epg", R.string.settings_category_epg, R.string.settings_category_epg_desc, "▦", true),
    SettingsCategory("library", R.string.settings_category_library, R.string.settings_category_library_desc, "▤", true),
    SettingsCategory("discovery", R.string.settings_category_discovery, R.string.settings_category_discovery_desc, "⌕", true),
    SettingsCategory("parental", R.string.settings_category_parental, R.string.settings_category_parental_desc, "◇", true),
    SettingsCategory("content", R.string.settings_category_content, R.string.settings_category_content_desc, "◎", true),
)

private val AppCategories = listOf(
    SettingsCategory("sources", R.string.settings_category_sources, R.string.settings_category_sources_desc, "☁", false, "settings/sources"),
    SettingsCategory("notifications", R.string.settings_category_notifications, R.string.settings_category_notifications_desc, "◌", false),
    SettingsCategory("device", R.string.settings_category_device, R.string.settings_category_device_desc, "▣", false),
    SettingsCategory("accessibility", R.string.settings_category_accessibility, R.string.settings_category_accessibility_desc, "◉", false),
    SettingsCategory("privacy", R.string.settings_category_privacy, R.string.settings_category_privacy_desc, "◇", false),
    SettingsCategory("downloads", R.string.settings_category_downloads, R.string.settings_category_downloads_desc, "↓", false),
    SettingsCategory("integrations", R.string.settings_category_integrations, R.string.settings_category_integrations_desc, "⌘", false),
    SettingsCategory("about", R.string.settings_category_about, R.string.settings_category_about_desc, "ⓘ", false),
)

private val languageChoices = listOf("system", "pl-PL", "en-US", "es-ES")
private val themeChoices = listOf("system", "dark", "light")
private val accentChoices = listOf("violet", "blue", "teal", "amber", "rose")
private val hudChoices = listOf("classic", "chips")
private val epgChoices = listOf(60, 180, 360, 720, 1440)

@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit,
    onSwitchProfile: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val active by vm.active.collectAsStateWithLifecycle()
    val profileSettings by vm.profileSettings.collectAsStateWithLifecycle()
    val operation by vm.operationState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isTelevision =
        (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
            Configuration.UI_MODE_TYPE_TELEVISION
    val isWideLayout = configuration.screenWidthDp >= 700 || isTelevision

    var selectedKey by rememberSaveable { mutableStateOf("profile") }
    var resetTarget by remember { mutableStateOf<String?>(null) }
    var restoreTarget by remember { mutableStateOf<String?>(null) }
    val profileResetRequester = remember { FocusRequester() }
    val appResetRequester = remember { FocusRequester() }

    val selected = (ProfileCategories + AppCategories)
        .firstOrNull { it.key == selectedKey }
        ?: ProfileCategories.first()

    val colors = MaterialTheme.colorScheme
    val panelShape = RoundedCornerShape(20.dp)

    // Wait until AlertDialog has left composition before restoring D-pad focus.
    LaunchedEffect(restoreTarget) {
        val target = restoreTarget
        if (target != null) {
            withFrameNanos { }
            if (target == "profile") {
                runCatching { profileResetRequester.requestFocus() }
            } else {
                runCatching { appResetRequester.requestFocus() }
            }
            restoreTarget = null
        }
    }

    fun dismissResetDialog(target: String) {
        resetTarget = null
        restoreTarget = target
    }

    if (isWideLayout) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(
                    horizontal = if (isTelevision) 36.dp else 28.dp,
                    vertical = 22.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(
                modifier = Modifier
                    .width(if (isTelevision) 340.dp else 300.dp)
                    .fillMaxHeight(),
            ) {
                SettingsHeader(
                    profileName = active?.name,
                    profileColor = active?.color,
                    onSwitchProfile = onSwitchProfile,
                )

                Spacer(Modifier.height(18.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    item {
                        SettingsSectionTitle(stringResource(R.string.settings_profile_section))
                    }
                    items(ProfileCategories, key = { it.key }) { category ->
                        SettingsCategoryRow(
                            category = category,
                            selected = selectedKey == category.key,
                            onClick = { selectedKey = category.key },
                        )
                    }
                    item {
                        Spacer(Modifier.height(14.dp))
                        SettingsSectionTitle(stringResource(R.string.settings_app_section))
                    }
                    items(AppCategories, key = { it.key }) { category ->
                        SettingsCategoryRow(
                            category = category,
                            selected = selectedKey == category.key,
                            onClick = { selectedKey = category.key },
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(panelShape)
                    .background(colors.surface.copy(alpha = 0.72f))
                    .border(1.dp, colors.outline.copy(alpha = 0.28f), panelShape)
                    .padding(
                        horizontal = if (isTelevision) 34.dp else 28.dp,
                        vertical = 28.dp,
                    ),
            ) {
                SettingsDetail(
                    category = selected,
                    profileName = active?.name,
                    profileSettings = profileSettings,
                    operation = operation,
                    onNavigate = onNavigate,
                    onUpdateProfile = vm::updateProfile,
                    onResetProfile = { resetTarget = "profile" },
                    onResetApp = { resetTarget = "app" },
                    profileResetRequester = profileResetRequester,
                    appResetRequester = appResetRequester,
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SettingsHeader(
                    profileName = active?.name,
                    profileColor = active?.color,
                    onSwitchProfile = onSwitchProfile,
                )
                Spacer(Modifier.height(18.dp))
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_profile_section)) }
            items(ProfileCategories, key = { it.key }) { category ->
                SettingsCategoryRow(
                    category = category,
                    selected = selectedKey == category.key,
                    onClick = { selectedKey = category.key },
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                SettingsSectionTitle(stringResource(R.string.settings_app_section))
            }
            items(AppCategories, key = { it.key }) { category ->
                SettingsCategoryRow(
                    category = category,
                    selected = selectedKey == category.key,
                    onClick = { selectedKey = category.key },
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                SettingsDetail(
                    category = selected,
                    profileName = active?.name,
                    profileSettings = profileSettings,
                    operation = operation,
                    onNavigate = onNavigate,
                    onUpdateProfile = vm::updateProfile,
                    onResetProfile = { resetTarget = "profile" },
                    onResetApp = { resetTarget = "app" },
                    profileResetRequester = profileResetRequester,
                    appResetRequester = appResetRequester,
                )
            }
        }
    }

    val dialogTarget = resetTarget
    if (dialogTarget != null) {
        val isProfile = dialogTarget == "profile"
        AlertDialog(
            onDismissRequest = { dismissResetDialog(dialogTarget) },
            title = {
                Text(
                    stringResource(
                        if (isProfile) R.string.settings_reset_profile_title
                        else R.string.settings_reset_app_title
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        if (isProfile) R.string.settings_reset_profile_message
                        else R.string.settings_reset_app_message
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isProfile) vm.resetCurrentProfile()
                        else vm.resetAppSettings()
                        dismissResetDialog(dialogTarget)
                    },
                ) {
                    Text(stringResource(R.string.settings_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissResetDialog(dialogTarget) }) {
                    Text(stringResource(R.string.settings_reset_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsHeader(
    profileName: String?,
    profileColor: Int?,
    onSwitchProfile: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenTitle(stringResource(R.string.nav_settings))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (profileName != null) {
                ProfileAvatar(profileName, profileColor ?: 0, 48.dp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_active_profile),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    profileName ?: stringResource(R.string.settings_no_active_profile),
                    color = colors.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }

        FocusableBox(
            onClick = onSwitchProfile,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(R.string.switch_profile),
                color = colors.onSurface,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 5.dp),
    )
}

@Composable
private fun SettingsCategoryRow(
    category: SettingsCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    FocusableBox(
        onClick = onClick,
        selected = selected,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Text(category.icon, style = MaterialTheme.typography.titleMedium)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    stringResource(category.title),
                    color = colors.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
                Text(
                    stringResource(category.description),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun SettingsDetail(
    category: SettingsCategory,
    profileName: String?,
    profileSettings: ProfileSettingsEntity?,
    operation: SettingsOperationState,
    onNavigate: (String) -> Unit,
    onUpdateProfile: ((ProfileSettingsEntity) -> ProfileSettingsEntity) -> Unit,
    onResetProfile: () -> Unit,
    onResetApp: () -> Unit,
    profileResetRequester: FocusRequester,
    appResetRequester: FocusRequester,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(category.icon, style = MaterialTheme.typography.headlineLarge)
        Text(
            stringResource(category.title),
            color = colors.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(category.description),
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )

        if (operation.isSaving) {
            Text(
                stringResource(R.string.settings_saving),
                color = colors.primary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        operation.error?.let {
            Text(
                stringResource(R.string.settings_save_error),
                color = colors.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (category.isProfile) {
            if (!profileName.isNullOrBlank() && category.key == "profile") {
                Text(
                    stringResource(R.string.settings_profile_current, profileName),
                    color = colors.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            when (category.key) {
                "appearance", "playback", "live", "epg", "library", "content" -> {
                    if (profileSettings == null) {
                        Text(
                            stringResource(R.string.settings_loading),
                            color = colors.onSurfaceVariant,
                        )
                    } else {
                        ProfileSettingsControls(
                            category = category.key,
                            settings = profileSettings,
                            onUpdate = onUpdateProfile,
                        )
                    }
                }

                "parental" -> Text(
                    stringResource(R.string.settings_parental_existing),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )

                "discovery" -> Text(
                    stringResource(R.string.settings_section_not_ready),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (category.key == "profile") {
                Spacer(Modifier.height(6.dp))
                FocusableBox(
                    onClick = onResetProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(profileResetRequester),
                    focusRequester = profileResetRequester,
                ) {
                    Text(
                        stringResource(R.string.settings_reset_profile),
                        color = colors.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        } else {
            when {
                category.route != null -> {
                    FocusableBox(onClick = { onNavigate(category.route) }) {
                        Text(
                            stringResource(R.string.settings_open_section),
                            color = colors.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                category.key == "about" -> {
                    Text(
                        stringResource(R.string.settings_reset_app_hint),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    FocusableBox(
                        onClick = onResetApp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(appResetRequester),
                        focusRequester = appResetRequester,
                    ) {
                        Text(
                            stringResource(R.string.settings_reset_app),
                            color = colors.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                else -> Text(
                    stringResource(R.string.settings_section_not_ready),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ProfileSettingsControls(
    category: String,
    settings: ProfileSettingsEntity,
    onUpdate: ((ProfileSettingsEntity) -> ProfileSettingsEntity) -> Unit,
) {
    when (category) {
        "appearance" -> {
            ChoiceRow(
                R.string.settings_theme,
                themeLabel(settings.themeMode),
            ) {
                onUpdate {
                    it.copy(themeMode = nextChoice(it.themeMode, themeChoices))
                }
            }
            ChoiceRow(
                R.string.settings_accent,
                accentLabel(settings.accentColor),
            ) {
                onUpdate {
                    it.copy(accentColor = nextChoice(it.accentColor, accentChoices))
                }
            }
        }

        "playback" -> {
            SwitchRow(
                R.string.settings_subtitles_enabled,
                settings.subtitlesEnabled,
            ) { checked ->
                onUpdate { it.copy(subtitlesEnabled = checked) }
            }
            SubtitleSizeControl(settings.subtitleSizeSp) { size ->
                onUpdate { it.copy(subtitleSizeSp = size) }
            }
            SwitchRow(
                R.string.settings_autoplay_next,
                settings.autoplayNext,
            ) { checked ->
                onUpdate { it.copy(autoplayNext = checked) }
            }
        }

        "content" -> {
            ChoiceRow(
                R.string.settings_content_language,
                languageLabel(settings.contentLanguage),
            ) {
                onUpdate {
                    it.copy(contentLanguage = nextChoice(it.contentLanguage, languageChoices))
                }
            }
            ChoiceRow(
                R.string.settings_audio_language,
                languageLabel(settings.preferredAudioLanguage),
            ) {
                onUpdate {
                    it.copy(preferredAudioLanguage = nextChoice(it.preferredAudioLanguage, languageChoices))
                }
            }
            ChoiceRow(
                R.string.settings_subtitle_language,
                languageLabel(settings.preferredSubtitleLanguage),
            ) {
                onUpdate {
                    it.copy(preferredSubtitleLanguage = nextChoice(it.preferredSubtitleLanguage, languageChoices))
                }
            }
        }

        "live" -> {
            ChoiceRow(
                R.string.settings_live_tech_style,
                if (settings.liveTvHudTechStyle == "chips") {
                    stringResource(R.string.settings_hud_chips)
                } else {
                    stringResource(R.string.settings_hud_classic)
                },
            ) {
                onUpdate {
                    it.copy(liveTvHudTechStyle = nextChoice(it.liveTvHudTechStyle, hudChoices))
                }
            }
            SwitchRow(
                R.string.settings_live_epg_description,
                settings.liveTvHudShowEpgDescription,
            ) { checked ->
                onUpdate { it.copy(liveTvHudShowEpgDescription = checked) }
            }
            SwitchRow(
                R.string.settings_live_last_channel,
                settings.liveTvStartOnLastChannel,
            ) { checked ->
                onUpdate { it.copy(liveTvStartOnLastChannel = checked) }
            }
        }

        "epg" -> {
            ChoiceRow(
                R.string.settings_epg_refresh,
                intervalLabel(settings.epgRefreshIntervalMinutes),
            ) {
                onUpdate {
                    it.copy(
                        epgRefreshIntervalMinutes = nextChoice(
                            it.epgRefreshIntervalMinutes,
                            epgChoices,
                        )
                    )
                }
            }
        }

        "library" -> {
            SwitchRow(
                R.string.settings_show_watched,
                settings.showWatched,
            ) { checked ->
                onUpdate { it.copy(showWatched = checked) }
            }
            SwitchRow(
                R.string.settings_continue_watching_enabled,
                settings.continueWatchingEnabled,
            ) { checked ->
                onUpdate { it.copy(continueWatchingEnabled = checked) }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            stringResource(title),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ChoiceRow(
    title: Int,
    value: String,
    onClick: () -> Unit,
) {
    FocusableBox(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                value,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun SubtitleSizeControl(
    sizeSp: Int,
    onSizeChange: (Int) -> Unit,
) {
    var sliderValue by remember(sizeSp) { mutableStateOf(sizeSp.toFloat()) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.settings_subtitle_size),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "${sliderValue.toInt()} sp",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = 12f..48f,
            onValueChangeFinished = {
                onSizeChange(sliderValue.toInt().coerceIn(12, 48))
            },
        )
    }
}

private fun <T> nextChoice(current: T, choices: List<T>): T {
    val index = choices.indexOf(current)
    return choices[(index + 1).mod(choices.size)]
}

@Composable
private fun themeLabel(value: String): String = when (value) {
    "light" -> stringResource(R.string.settings_theme_light)
    "dark" -> stringResource(R.string.settings_theme_dark)
    else -> stringResource(R.string.settings_theme_system)
}

@Composable
private fun accentLabel(value: String): String = when (value) {
    "blue" -> stringResource(R.string.settings_accent_blue)
    "teal" -> stringResource(R.string.settings_accent_teal)
    "amber" -> stringResource(R.string.settings_accent_amber)
    "rose" -> stringResource(R.string.settings_accent_rose)
    else -> stringResource(R.string.settings_accent_violet)
}

@Composable
private fun languageLabel(value: String): String = when (value) {
    "pl-PL" -> stringResource(R.string.settings_language_polish)
    "en-US" -> stringResource(R.string.settings_language_english)
    "es-ES" -> stringResource(R.string.settings_language_spanish)
    else -> stringResource(R.string.settings_language_system)
}

@Composable
private fun intervalLabel(minutes: Int): String = when (minutes) {
    60 -> stringResource(R.string.settings_epg_1h)
    180 -> stringResource(R.string.settings_epg_3h)
    360 -> stringResource(R.string.settings_epg_6h)
    720 -> stringResource(R.string.settings_epg_12h)
    else -> stringResource(R.string.settings_epg_24h)
}
