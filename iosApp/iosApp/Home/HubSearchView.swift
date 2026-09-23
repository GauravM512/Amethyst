import SwiftUI
import ComposeApp

@Observable
@MainActor
final class HubSearchViewModel {
    private(set) var result: ComposeApp.HubSearchResult?
    private(set) var activeQuery = ""
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    private let repository: HubRepository
    private var searchGeneration = 0

    init(repository: HubRepository) {
        self.repository = repository
    }

    func search(query rawQuery: String) async {
        let query = rawQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        searchGeneration += 1
        let generation = searchGeneration
        activeQuery = query
        result = nil
        errorMessage = nil
        guard !query.isEmpty else {
            isLoading = false
            return
        }

        isLoading = true
        do {
            try await Task.sleep(for: .milliseconds(300))
            guard !Task.isCancelled, generation == searchGeneration else { return }

            let response: ComposeApp.HubSearchResult = try await withCheckedThrowingContinuation { continuation in
                repository.search.execute(query: query, limit: 30) { value, error in
                    if let value {
                        continuation.resume(returning: value)
                    } else {
                        continuation.resume(throwing: error ?? HubSearchError.missingResponse)
                    }
                }
            }
            guard !Task.isCancelled, generation == searchGeneration else { return }
            result = response
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

    func clear() {
        searchGeneration += 1
        activeQuery = ""
        result = nil
        errorMessage = nil
        isLoading = false
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

    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization

    private var trimmedQuery: String {
        query.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        Group {
            if trimmedQuery.isEmpty {
                ContentUnavailableView(
                    localization.string("home_hub_search_prompt_title", fallback: "Search artists and projects"),
                    systemImage: "magnifyingglass",
                    description: Text(localization.string("home_hub_search_prompt_description", fallback: "Explore all of Amethyst Hub."))
                )
            } else if viewModel.activeQuery != trimmedQuery || viewModel.isLoading {
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
                        Task { await viewModel.search(query: trimmedQuery) }
                    }
                }
            } else if let result = viewModel.result {
                if result.artists.isEmpty && result.projects.isEmpty {
                    ContentUnavailableView.search(text: trimmedQuery)
                } else {
                    resultsList(result)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(theme.surface)
        .task(id: query) { await viewModel.search(query: query) }
        .onDisappear { viewModel.clear() }
    }

    private func resultsList(_ result: ComposeApp.HubSearchResult) -> some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 28) {
                if !result.artists.isEmpty {
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

                if !result.projects.isEmpty {
                    resultSection(
                        title: localization.string("home_hub_search_projects", fallback: "Projects"),
                        count: result.projectCount,
                        shown: result.projects.count
                    ) {
                        ForEach(result.projects, id: \.id) { project in
                            Button {
                                onOpen(.project(username: project.artist.username, slug: project.slug))
                            } label: {
                                projectRow(project)
                            }
                            .buttonStyle(.plain)

                            if project.id != result.projects.last?.id {
                                Divider().padding(.leading, 78)
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

    private func projectRow(_ project: ComposeApp.HubProject) -> some View {
        HStack(spacing: 14) {
            HubSearchArtwork(url: imageURL(project.thumbnailUrl), symbol: "square.grid.3x3.square")
                .frame(width: 64, height: 64)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text(project.title)
                    .font(.headline)
                    .foregroundStyle(theme.onSurface)
                    .lineLimit(2)
                Text(project.artist.displayName.isEmpty ? "@\(project.artist.username)" : project.artist.displayName)
                    .font(.subheadline)
                    .foregroundStyle(theme.onSurfaceVariant)
                    .lineLimit(1)
            }

            Spacer(minLength: 8)
            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(theme.onSurfaceVariant)
        }
        .padding(.vertical, 11)
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
        AsyncImage(url: url) { phase in
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
