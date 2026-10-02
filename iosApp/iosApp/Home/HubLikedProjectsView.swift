import SwiftUI
import ComposeApp

struct HubLikedProjectsView: View {
    let repository: HubRepository
    let revision: String
    let onOpen: (HubDestination) -> Void
    let onSignIn: () -> Void

    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization
    @State private var projects: [ComposeApp.HubProject] = []
    @State private var nextCursor: String?
    @State private var isLoading = true
    @State private var isLoadingMore = false
    @State private var needsSignIn = false
    @State private var hasLoaded = false
    @State private var loadFailed = false
    @State private var loadMoreFailed = false
    @State private var loadGeneration = 0

    private let columns = [GridItem(.adaptive(minimum: 320, maximum: 560), spacing: 24, alignment: .top)]

    var body: some View {
        Group {
            if needsSignIn {
                ContentUnavailableView {
                    Label(localization.string("home_hub_liked_title", fallback: "Liked Projects"), systemImage: "heart")
                } description: {
                    Text(localization.string("home_hub_liked_sign_in", fallback: "Sign in to see the projects you like."))
                } actions: {
                    Button(localization.string("home_hub_sign_in", fallback: "Sign in"), action: onSignIn)
                }
            } else if isLoading && !hasLoaded {
                ProgressView()
                    .controlSize(.large)
            } else if loadFailed && projects.isEmpty {
                ContentUnavailableView {
                    Label(localization.string("home_hub_liked_error", fallback: "Couldn’t load liked projects"), systemImage: "wifi.exclamationmark")
                } description: {
                    Text(localization.string("home_hub_error_generic", fallback: "The Hub could not be loaded. Please try again."))
                } actions: {
                    Button(localization.string("home_hub_retry", fallback: "Try Again")) {
                        Task { await reload() }
                    }
                }
            } else if hasLoaded && projects.isEmpty {
                ContentUnavailableView(
                    localization.string("home_hub_liked_empty", fallback: "No liked projects yet"),
                    systemImage: "heart",
                    description: Text(localization.string("home_hub_liked_empty_description", fallback: "Projects you like will appear here."))
                )
            } else {
                projectGrid
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(theme.surface.ignoresSafeArea())
        .navigationTitle(localization.string("home_hub_liked_title", fallback: "Liked Projects"))
        .navigationBarTitleDisplayMode(.large)
        .task(id: revision) { await reload() }
    }

    private var projectGrid: some View {
        ScrollView {
            LazyVStack(spacing: 24) {
                LazyVGrid(columns: columns, alignment: .center, spacing: 0) {
                    ForEach(projects, id: \.id) { project in
                        VStack(spacing: 0) {
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
                    }
                }

                if let cursor = nextCursor {
                    if loadMoreFailed {
                        Button(localization.string("home_hub_retry", fallback: "Try Again")) {
                            Task { await loadMore(cursor: cursor) }
                        }
                    } else {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                            .id(cursor)
                            .onAppear { Task { await loadMore(cursor: cursor) } }
                    }
                }
            }
            .frame(maxWidth: 1160)
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 110)
            .frame(maxWidth: .infinity)
        }
        .refreshable { await reload() }
    }

    private func reload() async {
        loadGeneration += 1
        let generation = loadGeneration
        nextCursor = nil
        isLoading = true
        loadFailed = false
        loadMoreFailed = false
        needsSignIn = !repository.client.isAuthenticated
        guard !needsSignIn else {
            projects = []
            hasLoaded = false
            isLoading = false
            return
        }

        do {
            let page = try await fetchPage(cursor: nil)
            guard generation == loadGeneration else { return }
            projects = page.items
            nextCursor = page.nextCursor
            hasLoaded = true
        } catch {
            guard generation == loadGeneration else { return }
            needsSignIn = isAuthenticationError(error)
            loadFailed = !needsSignIn
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
            needsSignIn = isAuthenticationError(error)
            loadMoreFailed = !needsSignIn
        }
        if generation == loadGeneration { isLoadingMore = false }
    }

    private func fetchPage(cursor: String?) async throws -> ComposeApp.HubProjectPage {
        try await withCheckedThrowingContinuation { continuation in
            repository.getLikedProjects.execute(cursor: cursor, limit: 24) { page, error in
                if let page {
                    continuation.resume(returning: page)
                } else {
                    continuation.resume(throwing: error ?? HubLikedProjectsError.missingResponse)
                }
            }
        }
    }

    private func isAuthenticationError(_ error: Error) -> Bool {
        let nsError = error as NSError
        if let hubError = nsError.kotlinException as? HubApiException {
            return hubError.statusCode == 401 || hubError.errorCode == "authentication_required"
        }
        return error.localizedDescription.contains("authentication_required")
    }
}

private enum HubLikedProjectsError: Error {
    case missingResponse
}
