import SwiftUI
import ComposeApp

@Observable
@MainActor
final class HubSearchViewModel {
    private(set) var result: ComposeApp.HubSearchResult?
    private(set) var filteredProjects: [ComposeApp.HubProject] = []
    private(set) var nextCursor: String?
    private(set) var activeQuery = ""
    private(set) var activeFilterKey = ""
    private(set) var isLoading = false
    private(set) var isLoadingMore = false
    private(set) var loadMoreFailed = false
    private(set) var hasLoaded = false
    private(set) var errorMessage: String?

    private let repository: HubRepository
    private var searchGeneration = 0

    init(repository: HubRepository) {
        self.repository = repository
    }

    func search(query rawQuery: String, filters: HubProjectFilters) async {
        let query = rawQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        searchGeneration += 1
        let generation = searchGeneration
        activeQuery = query
        activeFilterKey = filters.requestKey
        result = nil
        filteredProjects = []
        nextCursor = nil
        hasLoaded = false
        isLoadingMore = false
        loadMoreFailed = false
        errorMessage = nil
        guard !query.isEmpty || filters.isActive else {
            isLoading = false
            return
        }

        isLoading = true
        do {
            try await Task.sleep(for: .milliseconds(300))
            guard !Task.isCancelled, generation == searchGeneration else { return }

            let response = query.isEmpty ? nil : try await fetchSearch(query: query)
            let page = filters.isActive ? try await fetchProjects(query: query, filters: filters, cursor: nil) : nil
            guard !Task.isCancelled, generation == searchGeneration else { return }
            result = response
            filteredProjects = page?.items ?? []
            nextCursor = page?.nextCursor
            hasLoaded = true
        } catch is CancellationError {
            return
        } catch {
            guard !Task.isCancelled, generation == searchGeneration else { return }
            errorMessage = IosLocalizationBridge.shared.string(
                key: "home_hub_search_error",
                fallback: "Search failed. Please try again."
            )
        }

        if generation == searchGeneration { isLoading = false }
    }

    func loadMore(query: String, filters: HubProjectFilters, cursor: String) async {
        guard !isLoading, !isLoadingMore, nextCursor == cursor, activeQuery == query,
              activeFilterKey == filters.requestKey else { return }
        let generation = searchGeneration
        isLoadingMore = true
        loadMoreFailed = false
        do {
            let page = try await fetchProjects(query: query, filters: filters, cursor: cursor)
            guard generation == searchGeneration else { return }
            let seenIDs = Set(filteredProjects.map(\.id))
            filteredProjects.append(contentsOf: page.items.filter { !seenIDs.contains($0.id) })
            nextCursor = page.nextCursor
        } catch {
            guard generation == searchGeneration else { return }
            loadMoreFailed = true
        }
        if generation == searchGeneration { isLoadingMore = false }
    }

    private func fetchSearch(query: String) async throws -> ComposeApp.HubSearchResult {
        try await withCheckedThrowingContinuation { continuation in
            repository.search.execute(query: query, limit: 30) { value, error in
                if let value {
                    continuation.resume(returning: value)
                } else {
                    continuation.resume(throwing: error ?? HubSearchError.missingResponse)
                }
            }
        }
    }

    private func fetchProjects(query: String, filters: HubProjectFilters, cursor: String?) async throws -> ComposeApp.HubProjectPage {
        try await withCheckedThrowingContinuation { continuation in
            repository.browseProjects.execute(
                cursor: cursor,
                limit: 24,
                compatibility: filters.selectedCompatibility,
                type: filters.selectedType,
                sort: filters.selectedSort,
                query: query.isEmpty ? nil : query,
                difficulty: filters.selectedDifficulty
            ) { page, error in
                if let page {
                    continuation.resume(returning: page)
                } else {
                    continuation.resume(throwing: error ?? HubSearchError.missingResponse)
                }
            }
        }
    }

    func clear() {
        searchGeneration += 1
        activeQuery = ""
        activeFilterKey = ""
        result = nil
        filteredProjects = []
        nextCursor = nil
        hasLoaded = false
        loadMoreFailed = false
        errorMessage = nil
        isLoading = false
        isLoadingMore = false
    }
}

private enum HubSearchError: Error {
    case missingResponse
}

struct HubSearchView: View {
    let query: String
    @Bindable var viewModel: HubSearchViewModel
    let repository: HubRepository
    let onOpen: (HubDestination) -> Void
    @State private var filters = HubProjectFilters()

    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization

    private var trimmedQuery: String {
        query.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var requestKey: String { "\(trimmedQuery)|\(filters.requestKey)" }

    private var hasResults: Bool {
        !(viewModel.result?.artists.isEmpty ?? true)
            || (filters.isActive ? !viewModel.filteredProjects.isEmpty : !(viewModel.result?.projects.isEmpty ?? true))
    }

    var body: some View {
        VStack(spacing: 0) {
            HubProjectFilterBar(filters: $filters)
                .padding(.horizontal, 20)
                .padding(.top, 8)

            Group {
                if trimmedQuery.isEmpty && !filters.isActive {
                    ContentUnavailableView(
                        localization.string("home_hub_search_prompt_title", fallback: "Search artists and projects"),
                        systemImage: "magnifyingglass",
                        description: Text(localization.string("home_hub_search_prompt_description", fallback: "Explore all of Amethyst Hub."))
                    )
                } else if viewModel.activeQuery != trimmedQuery || viewModel.activeFilterKey != filters.requestKey || viewModel.isLoading {
                    ProgressView()
                        .controlSize(.large)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let errorMessage = viewModel.errorMessage {
                    ContentUnavailableView {
                        Label(localization.string("home_hub_search_error_title", fallback: "Search unavailable"), systemImage: "wifi.exclamationmark")
                    } description: {
                        Text(errorMessage)
                    } actions: {
                        Button(localization.string("home_hub_retry", fallback: "Try Again")) {
                            Task { await viewModel.search(query: trimmedQuery, filters: filters) }
                        }
                    }
                } else if viewModel.hasLoaded {
                    if !hasResults {
                        if trimmedQuery.isEmpty {
                            ContentUnavailableView(
                                localization.string("home_hub_catalog_empty", fallback: "No projects found"),
                                systemImage: "line.3.horizontal.decrease.circle",
                                description: Text(localization.string("home_hub_catalog_empty_description", fallback: "Try another search or change your filters."))
                            )
                        } else {
                            ContentUnavailableView.search(text: trimmedQuery)
                        }
                    } else {
                        resultsList()
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(theme.background.ignoresSafeArea())
        .task(id: requestKey) { await viewModel.search(query: query, filters: filters) }
        .onDisappear { viewModel.clear() }
    }

    private func resultsList() -> some View {
        let result = viewModel.result
        let projects = filters.isActive ? viewModel.filteredProjects : (result?.projects ?? [])
        let projectCount = filters.isActive ? Int64(viewModel.filteredProjects.count) : (result?.projectCount ?? 0)
        return ScrollView {
            LazyVStack(alignment: .leading, spacing: 28) {
                if let result, !result.artists.isEmpty {
                    resultSection(
                        title: localization.string("home_hub_search_artists", fallback: "Artists"),
                        count: result.artistCount,
                        shown: result.artists.count
                    ) {
                        ForEach(result.artists, id: \.username) { artist in
                            Button {
                                onOpen(.artist(artist.username))
                            } label: {
                                artistRow(artist)
                            }
                            .buttonStyle(.plain)

                            if artist.username != result.artists.last?.username {
                                Divider().padding(.leading, 68)
                            }
                        }
                    }
                }

                if !projects.isEmpty {
                    resultSection(
                        title: localization.string("home_hub_search_projects", fallback: "Projects"),
                        count: projectCount,
                        shown: projects.count
                    ) {
                        ForEach(projects, id: \.id) { project in
                            Button {
                                onOpen(.project(username: project.artist.username, slug: project.slug))
                            } label: {
                                HubProjectListRow(project: project, repository: repository)
                            }
                            .buttonStyle(.plain)

                            if project.id != projects.last?.id {
                                Divider().padding(.leading, 78)
                            }
                        }

                        if filters.isActive, let cursor = viewModel.nextCursor {
                            if viewModel.loadMoreFailed {
                                Button(localization.string("home_hub_retry", fallback: "Try Again")) {
                                    Task { await viewModel.loadMore(query: trimmedQuery, filters: filters, cursor: cursor) }
                                }
                                .frame(maxWidth: .infinity)
                            } else {
                                ProgressView()
                                    .frame(maxWidth: .infinity)
                                    .padding(.top, 16)
                                    .id(cursor)
                                    .onAppear {
                                        Task { await viewModel.loadMore(query: trimmedQuery, filters: filters, cursor: cursor) }
                                    }
                            }
                        }
                    }
                }
            }
            .frame(maxWidth: 720, alignment: .leading)
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 110)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
    }

    private func resultSection<Content: View>(
        title: String,
        count: Int64,
        shown: Int,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(title)
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(theme.onSurface)
                Text(count.formatted())
                    .font(.subheadline)
                    .foregroundStyle(theme.onSurfaceVariant)
            }

            LazyVStack(spacing: 0) {
                content()
            }

            if count > Int64(shown) {
                Text(localization.string("home_hub_search_limited", fallback: "Showing the first 30 results"))
                    .font(.footnote)
                    .foregroundStyle(theme.onSurfaceVariant)
            }
        }
    }

    private func artistRow(_ artist: ComposeApp.HubArtist) -> some View {
        HStack(spacing: 14) {
            HubSearchArtwork(url: imageURL(artist.avatarUrl), symbol: "person.crop.circle.fill")
                .frame(width: 54, height: 54)
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 3) {
                Text(artist.displayName.isEmpty ? artist.username : artist.displayName)
                    .font(.headline)
                    .foregroundStyle(theme.onSurface)
                Text("@\(artist.username)")
                    .font(.subheadline)
                    .foregroundStyle(theme.onSurfaceVariant)
            }
            .lineLimit(1)

            Spacer(minLength: 8)
            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(theme.onSurfaceVariant)
        }
        .padding(.vertical, 10)
        .contentShape(Rectangle())
    }

    private func imageURL(_ value: String?) -> URL? {
        guard let value, !value.isEmpty else { return nil }
        return URL(string: repository.client.resolveUrl(pathOrUrl: value))
    }
}

private struct HubSearchArtwork: View {
    let url: URL?
    let symbol: String

    @Environment(\.amethystTheme) private var theme

    var body: some View {
        HubCachedAsyncImage(url: url) { phase in
            if let image = phase.image {
                image.resizable().scaledToFill()
            } else {
                Rectangle()
                    .fill(theme.surfaceContainerHigh)
                    .overlay {
                        Image(systemName: symbol)
                            .font(.title3)
                            .foregroundStyle(theme.onSurfaceVariant)
                    }
            }
        }
        .clipped()
        .accessibilityHidden(true)
    }
}
