import SwiftUI
import ComposeApp

struct HubProjectsView: View {
    let repository: HubRepository
    let revision: String
    let onOpen: (HubDestination) -> Void

    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization
    @State private var searchText = ""
    @State private var filters = HubProjectFilters()
    @State private var projects: [ComposeApp.HubProject] = []
    @State private var nextCursor: String?
    @State private var isLoading = true
    @State private var isLoadingMore = false
    @State private var hasLoaded = false
    @State private var loadFailed = false
    @State private var loadMoreFailed = false
    @State private var loadGeneration = 0
    @FocusState private var isSearchFocused: Bool

    private var requestKey: String {
        [revision, searchText.trimmingCharacters(in: .whitespacesAndNewlines), filters.requestKey].joined(separator: "|")
    }

    private var hasActiveFilters: Bool {
        !searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || filters.isActive
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 18) {
                Text(localization.string("home_hub_catalog_subtitle", fallback: "Discover projects from the Amethyst community."))
                    .font(.subheadline)
                    .foregroundStyle(theme.onSurfaceVariant)

                searchField
                HubProjectFilterBar(filters: $filters, onChange: { isSearchFocused = false })

                if isLoading && !hasLoaded {
                    ProgressView()
                        .controlSize(.large)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 90)
                } else if loadFailed && projects.isEmpty {
                    ContentUnavailableView {
                        Label(localization.string("home_hub_catalog_error", fallback: "Projects unavailable"), systemImage: "wifi.exclamationmark")
                    } description: {
                        Text(localization.string("home_hub_error_generic", fallback: "The Hub could not be loaded. Please try again."))
                    } actions: {
                        Button(localization.string("home_hub_retry", fallback: "Try Again")) {
                            Task { await reload() }
                        }
                    }
                    .frame(maxWidth: .infinity)
                } else if hasLoaded && projects.isEmpty {
                    ContentUnavailableView {
                        Label(localization.string("home_hub_catalog_empty", fallback: "No projects found"), systemImage: "line.3.horizontal.decrease.circle")
                    } description: {
                        Text(localization.string("home_hub_catalog_empty_description", fallback: "Try another search or change your filters."))
                    } actions: {
                        if hasActiveFilters {
                            Button(localization.string("home_hub_catalog_reset", fallback: "Reset filters")) { resetFilters() }
                        }
                    }
                    .frame(maxWidth: .infinity)
                } else if hasLoaded {
                    LazyVStack(spacing: 0) {
                        ForEach(projects, id: \.id) { project in
                            VStack(spacing: 0) {
                                Button {
                                    isSearchFocused = false
                                    onOpen(.project(username: project.artist.username, slug: project.slug))
                                } label: {
                                    HubProjectListRow(project: project, repository: repository)
                                }
                                .buttonStyle(.plain)

                                if project.id != projects.last?.id {
                                    Divider().padding(.leading, 78)
                                }
                            }
                        }
                    }

                    if let cursor = nextCursor {
                        if loadMoreFailed {
                            Button(localization.string("home_hub_retry", fallback: "Try Again")) {
                                Task { await loadMore(cursor: cursor) }
                            }
                            .frame(maxWidth: .infinity)
                        } else {
                            ProgressView()
                                .frame(maxWidth: .infinity)
                                .id(cursor)
                                .onAppear { Task { await loadMore(cursor: cursor) } }
                        }
                    }
                }
            }
            .frame(maxWidth: 720, alignment: .leading)
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 110)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
        .refreshable { await reload() }
        .background(theme.background.ignoresSafeArea())
        .navigationTitle(localization.string("home_hub_catalog_title", fallback: "Explore Projects"))
        .navigationBarTitleDisplayMode(.large)
        .task(id: requestKey) { await reload(debounce: true) }
    }

    private var searchField: some View {
        HStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(theme.onSurfaceVariant)
            TextField(localization.string("home_hub_catalog_search", fallback: "Search projects"), text: $searchText)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.search)
                .focused($isSearchFocused)
                .onSubmit { isSearchFocused = false }
            if !searchText.isEmpty {
                Button {
                    searchText = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(theme.onSurfaceVariant)
                }
                .accessibilityLabel(localization.string("home_hub_catalog_clear_search", fallback: "Clear search"))
            }
        }
        .padding(.horizontal, 16)
        .frame(height: 48)
        .background(theme.surfaceContainerHigh, in: Capsule())
    }

    private func resetFilters() {
        isSearchFocused = false
        searchText = ""
        filters = HubProjectFilters()
    }

    private func reload(debounce: Bool = false) async {
        loadGeneration += 1
        let generation = loadGeneration
        isLoading = true
        hasLoaded = false
        loadFailed = false
        loadMoreFailed = false
        isLoadingMore = false
        projects = []
        nextCursor = nil

        do {
            if debounce {
                try await Task.sleep(for: .milliseconds(300))
                guard !Task.isCancelled, generation == loadGeneration else { return }
            }
            let page = try await fetchPage(cursor: nil)
            guard !Task.isCancelled, generation == loadGeneration else { return }
            projects = page.items
            nextCursor = page.nextCursor
            hasLoaded = true
        } catch is CancellationError {
            return
        } catch {
            guard !Task.isCancelled, generation == loadGeneration else { return }
            loadFailed = true
        }
        if generation == loadGeneration { isLoading = false }
    }

    private func loadMore(cursor: String) async {
        guard !isLoading, !isLoadingMore, nextCursor == cursor else { return }
        let generation = loadGeneration
        isLoadingMore = true
        loadMoreFailed = false
        do {
            let page = try await fetchPage(cursor: cursor)
            guard generation == loadGeneration else { return }
            let seenIDs = Set(projects.map(\.id))
            projects.append(contentsOf: page.items.filter { !seenIDs.contains($0.id) })
            nextCursor = page.nextCursor
        } catch {
            guard generation == loadGeneration else { return }
            loadMoreFailed = true
        }
        if generation == loadGeneration { isLoadingMore = false }
    }

    private func fetchPage(cursor: String?) async throws -> ComposeApp.HubProjectPage {
        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        return try await withCheckedThrowingContinuation { continuation in
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
                    continuation.resume(throwing: error ?? HubCatalogError.missingResponse)
                }
            }
        }
    }

}

private enum HubCatalogError: Error {
    case missingResponse
}
