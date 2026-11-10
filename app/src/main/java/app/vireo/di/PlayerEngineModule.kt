package app.vireo.di

import app.vireo.player.engine.Media3PlayerEngine
import app.vireo.player.engine.PlayerEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class PlayerEngineModule {

    @Binds
    @ViewModelScoped
    abstract fun bindPlayerEngine(
        implementation: Media3PlayerEngine,
    ): PlayerEngine
}
