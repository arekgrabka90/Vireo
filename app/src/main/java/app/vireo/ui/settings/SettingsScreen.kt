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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.ui.components.FocusableBox
import app.vireo.ui.components.ProfileAvatar
import app.vireo.ui.components.ScreenTitle

private data class SettingsCategory(
    val key: String,
    val title: Int,
    val description: Int,
    val icon: String,
    val isProfile: Boolean,
    val route: String? = null,
)

private val ProfileCategories = listOf(
    SettingsCategory(
        "profile", R.string.settings_category_profile,
        R.string.settings_category_profile_desc, "👤", true,
    ),
    SettingsCategory(
        "appearance", R.string.settings_category_appearance,
        R.string.settings_category_appearance_desc, "🎨", true,
    ),
    SettingsCategory(
        "playback", R.string.settings_category_playback,
        R.string.settings_category_playback_desc, "▶", true,
    ),
    SettingsCategory(
        "live", R.string.settings_category_live,
        R.string.settings_category_live_desc, "📺", true,
    ),
    SettingsCategory(
        "epg", R.string.settings_category_epg,
        R.string.settings_category_epg_desc, "🗓", true,
    ),
    SettingsCategory(
        "library", R.string.settings_category_library,
        R.string.settings_category_library_desc, "▤", true,
    ),
    SettingsCategory(
        "discovery", R.string.settings_category_discovery,
        R.string.settings_category_discovery_desc, "⌕", true,
    ),
    SettingsCategory(
        "parental", R.string.settings_category_parental,
        R.string.settings_category_parental_desc, "🔒", true,
    ),
    SettingsCategory(
        "content", R.string.settings_category_content,
        R.string.settings_category_content_desc, "🌐", true,
    ),
)

private val AppCategories = listOf(
    SettingsCategory(
        "sources", R.string.settings_category_sources,
        R.string.settings_category_sources_desc, "☁", false,
        route = "settings/sources",
    ),
    SettingsCategory(
        "notifications", R.string.settings_category_notifications,
        R.string.settings_category_notifications_desc, "🔔", false,
    ),
    SettingsCategory(
        "device", R.string.settings_category_device,
        R.string.settings_category_device_desc, "▣", false,
    ),
    SettingsCategory(
        "accessibility", R.string.settings_category_accessibility,
        R.string.settings_category_accessibility_desc, "◉", false,
    ),
    SettingsCategory(
        "privacy", R.string.settings_category_privacy,
        R.string.settings_category_privacy_desc, "◈", false,
    ),
    SettingsCategory(
        "downloads", R.string.settings_category_downloads,
        R.string.settings_category_downloads_desc, "↓", false,
    ),
    SettingsCategory(
        "integrations", R.string.settings_category_integrations,
        R.string.settings_category_integrations_desc, "⌘", false,
    ),
    SettingsCategory(
        "about", R.string.settings_category_about,
        R.string.settings_category_about_desc, "ⓘ", false,
    ),
)

@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit,
    onSwitchProfile: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val active by vm.active.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isTelevision =
        (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
            Configuration.UI_MODE_TYPE_TELEVISION
    val isWideLayout = configuration.screenWidthDp >= 700 || isTelevision

    var selectedKey by rememberSaveable { mutableStateOf("profile") }

    val selected = (ProfileCategories + AppCategories)
        .firstOrNull { it.key == selectedKey }
        ?: ProfileCategories.first()

    val colors = MaterialTheme.colorScheme
    val panelShape = RoundedCornerShape(20.dp)

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
                        SettingsSectionTitle(
                            stringResource(R.string.settings_profile_section),
                        )
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
                        SettingsSectionTitle(
                            stringResource(R.string.settings_app_section),
                        )
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
                    .border(
                        1.dp,
                        colors.outline.copy(alpha = 0.28f),
                        panelShape,
                    )
                    .padding(
                        horizontal = if (isTelevision) 34.dp else 28.dp,
                        vertical = 28.dp,
                    ),
            ) {
                SettingsDetail(
                    category = selected,
                    profileName = active?.name,
                    onNavigate = onNavigate,
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 22.dp,
            ),
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

            item {
                SettingsSectionTitle(
                    stringResource(R.string.settings_profile_section),
                )
            }
            items(ProfileCategories, key = { it.key }) { category ->
                SettingsCategoryRow(
                    category = category,
                    selected = selectedKey == category.key,
                    onClick = { selectedKey = category.key },
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                SettingsSectionTitle(
                    stringResource(R.string.settings_app_section),
                )
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
                    onNavigate = onNavigate,
                )
            }
        }
    }
}

@Composable
private fun SettingsHeader(
    profileName: String?,
    profileColor: String?,
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
                ProfileAvatar(profileName, profileColor.orEmpty(), 48.dp)
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
        modifier = Modifier.padding(
            start = 8.dp,
            top = 8.dp,
            bottom = 5.dp,
        ),
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
            Text(
                category.icon,
                style = MaterialTheme.typography.titleMedium,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    stringResource(category.title),
                    color = colors.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.SemiBold
                        else FontWeight.Medium,
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
    onNavigate: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            category.icon,
            style = MaterialTheme.typography.headlineLarge,
        )

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

        if (category.key == "profile" && !profileName.isNullOrBlank()) {
            Text(
                stringResource(R.string.settings_profile_current, profileName),
                color = colors.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (category.route != null) {
            Spacer(Modifier.height(4.dp))
            FocusableBox(
                onClick = { onNavigate(category.route) },
            ) {
                Text(
                    stringResource(R.string.settings_open_section),
                    color = colors.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.settings_section_not_ready),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
