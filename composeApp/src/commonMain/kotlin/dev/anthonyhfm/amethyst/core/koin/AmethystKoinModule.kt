package dev.anthonyhfm.amethyst.core.koin

import dev.anthonyhfm.amethyst.core.midi.midiKoinModule
import dev.anthonyhfm.amethyst.workspace.WorkspaceRepository
import dev.anthonyhfm.amethyst.workspace.WorkspaceViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val amethystKoinModule = module {
    includes(midiKoinModule)

    single { WorkspaceRepository() }

    viewModel { WorkspaceViewModel(get(), get(), get()) }
}