package app.vireo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.vireo.domain.model.Title
import app.vireo.player.PlayerScreen
import app.vireo.ui.detail.DetailScreen
import app.vireo.ui.catalog.TmdbCatalogScreen
import app.vireo.ui.catalog.TmdbDetailScreen
import app.vireo.domain.model.ContentType
import app.vireo.ui.list.TitleListScreen
import app.vireo.ui.profiles.ProfileEditScreen
import app.vireo.ui.profiles.ProfileSelectScreen
import app.vireo.ui.profiles.SplashScreen
import app.vireo.ui.shell.MainShell
import app.vireo.ui.theme.VireoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VireoTheme { VireoNav() } }
    }
}

private fun NavController.openDetail(t: Title) {
    val name = Uri.encode(t.name.ifBlank { t.id })
    navigate("detail/${t.type.name}/${Uri.encode(t.id)}/$name")
}

@Composable
private fun VireoNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onHasProfiles = {
                    nav.navigate("profiles") { popUpTo("splash") { inclusive = true } }
                },
                onNoProfiles = {
                    nav.navigate("profile_edit/0") { popUpTo("splash") { inclusive = true } }
                },
            )
        }
        composable("profiles") {
            ProfileSelectScreen(
                onSelected = {
                    nav.navigate("main") { popUpTo("profiles") { inclusive = true } }
                },
                onEdit = { id -> nav.navigate("profile_edit/$id") },
                onAdd = { nav.navigate("profile_edit/0") },
            )
        }
        composable("profile_edit/{id}") {
            ProfileEditScreen(onDone = {
                if (!nav.popBackStack("profiles", false)) {
                    nav.navigate("profiles") { popUpTo("profile_edit/{id}") { inclusive = true } }
                }
            })
        }
        composable("main") {
            MainShell(
                onOpenCategory = { type, category ->
                    nav.navigate("list/${type.name}/${Uri.encode(category.id)}/${Uri.encode(category.name)}")
                },
                onOpenTitle = { t -> nav.openDetail(t) },
                onOpenTmdbTitle = { type, id, name ->
                    nav.navigate("tmdb_detail/${type.name}/$id/${Uri.encode(name)}")
                },
                onOpenTmdbCatalog = { type ->
                    nav.navigate("tmdb_catalog/${type.name}")
                },
                onSwitchProfile = {
                    nav.navigate("profiles") { popUpTo("main") { inclusive = true } }
                },
            )
        }
        composable("list/{type}/{categoryId}/{name}") {
            TitleListScreen(
                onBack = { nav.popBackStack() },
                onOpenTitle = { t -> nav.openDetail(t) },
                onPlay = { nav.navigate("player") },
            )
        }
        composable("detail/{type}/{id}/{name}") {
            DetailScreen(
                onBack = { nav.popBackStack() },
                onPlay = { nav.navigate("player") },
            )
        }

        composable("tmdb_catalog/{type}") {
            TmdbCatalogScreen(
                onBack = { nav.popBackStack() },
                onOpenTitle = { type, id, name ->
                    nav.navigate("tmdb_detail/${type.name}/$id/${Uri.encode(name)}")
                },
            )
        }
        composable("tmdb_detail/{type}/{id}/{name}") { entry ->
            val type = runCatching {
                ContentType.valueOf(entry.arguments?.getString("type") ?: "MOVIE")
            }.getOrDefault(ContentType.MOVIE)
            val id = entry.arguments?.getString("id")?.toIntOrNull() ?: 0
            val name = entry.arguments?.getString("name").orEmpty()
            TmdbDetailScreen(
                type = type,
                tmdbId = id,
                name = name,
                onBack = { nav.popBackStack() },
            )
        }
        composable("player") {
            PlayerScreen(onBack = { nav.popBackStack() })
        }
    }
}
