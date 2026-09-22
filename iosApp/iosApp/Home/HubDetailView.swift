import SwiftUI
import ComposeApp

enum HubDestination: Identifiable, Equatable {
    case artist(String)
    case project(username: String, slug: String)

    var id: String {
        switch self {
        case .artist(let username): "artist:\(username)"
        case .project(let username, let slug): "project:\(username)/\(slug)"
        }
    }

    init?(href: String?) {
        guard let href else { return nil }
        let path = URLComponents(string: href)?.path ?? href
        let parts = path.split(separator: "/")
        guard let first = parts.first, first.hasPrefix("@"), first.count > 1 else { return nil }
        let username = String(first.dropFirst())
        if parts.count == 1 {
            self = .artist(username)
        } else if parts.count == 2 {
            self = .project(username: username, slug: String(parts[1]))
        } else {
            return nil
        }
    }
}

struct HubDetailView: View {
    let destination: HubDestination
    let repository: HubRepository
    let onSignIn: () -> Void
    @State private var openedDestination: HubDestination?

    @Environment(\.dismiss) private var dismiss
    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization

    var body: some View {
        NavigationStack {
            Group {
                switch openedDestination ?? destination {
                case .artist(let username):
                    HubArtistDetailView(username: username, repository: repository, onOpen: { openedDestination = $0 }, onSignIn: onSignIn)
                case .project(let username, let slug):
                    HubProjectDetailView(username: username, slug: slug, repository: repository, onOpen: { openedDestination = $0 }, onSignIn: onSignIn)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.hidden, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button { dismiss() } label: {
                        Image(systemName: "xmark")
                    }
                    .accessibilityLabel(localization.string("home_hub_dismiss", fallback: "Close"))
                }
                ToolbarItem(placement: .topBarTrailing) {
                    if let url = currentShareURL {
                        ShareLink(item: url) {
                            Image(systemName: "square.and.arrow.up")
                        }
                        .accessibilityLabel(localization.string("home_hub_detail_share", fallback: "Share"))
                    }
                }
            }
        }
        .tint(theme.onSurface)
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .presentationCornerRadius(28)
        .background(theme.surface)
    }

    private var currentShareURL: URL? {
        switch openedDestination ?? destination {
        case .artist(let username):
            URL(string: "https://projects.launchpadders.com/@\(username)")
        case .project:
            nil
        }
    }
}

private struct HubArtistDetailView: View {
    let username: String
    let repository: HubRepository
    let onOpen: (HubDestination) -> Void
    let onSignIn: () -> Void

    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization
    @State private var artist: ComposeApp.HubArtist?
    @State private var projects: [ComposeApp.HubProject] = []
    @State private var nextCursor: String?
    @State private var isLoading = true
    @State private var isLoadingMore = false
    @State private var isFollowing = false
    @State private var followersCount = 0
    @State private var followPending = false
    @State private var error: String?
    @State private var actionError: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                HubDetailBackdrop(url: imageURL(artist?.avatarUrl), symbol: "person.crop.circle.fill")
                    .frame(height: 150)
                    .overlay(alignment: .bottom) {
                        HubDetailImage(url: imageURL(artist?.avatarUrl), symbol: "person.crop.circle.fill")
                            .frame(width: 108, height: 108)
                            .clipShape(Circle())
                            .overlay(Circle().stroke(theme.surface, lineWidth: 5))
                            .offset(y: 42)
                    }
                    .zIndex(1)

                VStack(spacing: 0) {
                    if let artist {
                        Text(artist.displayName.isEmpty ? artist.username : artist.displayName)
                            .font(.largeTitle.weight(.bold))
                            .multilineTextAlignment(.center)
                            .foregroundStyle(theme.onSurface)
                            .padding(.top, 60)

                        Text("@\(artist.username)")
                            .font(.subheadline)
                            .foregroundStyle(theme.onSurfaceVariant)
                            .padding(.top, 3)

                        HStack(spacing: 18) {
                            Label("\(projectsCount.formatted()) \(localization.string("home_hub_detail_projects", fallback: "projects"))", systemImage: "square.stack")
                            Label("\(followersCount.formatted()) \(localization.string("home_hub_followers", fallback: "followers"))", systemImage: "person.2")
                        }
                        .font(.subheadline)
                        .foregroundStyle(theme.onSurfaceVariant)
                        .padding(.top, 18)

                        Button {
                            Task { await toggleFollow() }
                        } label: {
                            if followPending { ProgressView().tint(isFollowing ? theme.onSurface : theme.primaryForeground) }
                            else {
                                Label(
                                    isFollowing ? localization.string("home_hub_following", fallback: "Following") : localization.string("home_hub_follow", fallback: "Follow"),
                                    systemImage: isFollowing ? "checkmark" : "plus"
                                )
                            }
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(isFollowing ? theme.surfaceContainerHigh : theme.primary)
                        .disabled(followPending)
                        .padding(.top, 22)

                        if !artist.bio.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            Text(artist.bio)
                                .font(.body)
                                .foregroundStyle(theme.onSurface)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.top, 32)
                        }

                        VStack(alignment: .leading, spacing: 16) {
                            Text(localization.string("home_hub_detail_projects_title", fallback: "Projects"))
                                .font(.title2.weight(.bold))
                            if projects.isEmpty {
                                Text(localization.string("home_hub_detail_no_projects", fallback: "No published projects yet."))
                                    .foregroundStyle(theme.onSurfaceVariant)
                            } else {
                                ForEach(projects, id: \.id) { project in
                                    Button {
                                        onOpen(.project(username: username, slug: project.slug))
                                    } label: {
                                        HubProjectRow(project: project, repository: repository)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            if nextCursor != nil {
                                Button(isLoadingMore ? localization.string("home_hub_detail_loading", fallback: "Loading…") : localization.string("home_hub_detail_load_more", fallback: "Load more")) {
                                    Task { await loadProjects() }
                                }
                                .disabled(isLoadingMore)
                                .frame(maxWidth: .infinity)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.top, 38)
                    } else if isLoading {
                        ProgressView().padding(.top, 78)
                    } else {
                        HubDetailError(message: error ?? localization.string("home_hub_detail_artist_unavailable", fallback: "Artist unavailable")) {
                            Task { await load() }
                        }
                        .padding(.top, 70)
                    }
                }
                .frame(maxWidth: 720)
                .padding(.horizontal, 24)
                .padding(.bottom, 48)
                .frame(maxWidth: .infinity)
                .background(theme.surface)
            }
        }
        .ignoresSafeArea(edges: .top)
        .task(id: username) { await load() }
        .alert("Hub", isPresented: Binding(
            get: { actionError != nil },
            set: { if !$0 { actionError = nil } }
        )) {
            Button("OK", role: .cancel) { actionError = nil }
        } message: {
            Text(actionError ?? "")
        }
    }

    private var projectsCount: Int { artist.map { Int(clamping: $0.publishedProjectCount) } ?? 0 }

    private func imageURL(_ value: String?) -> URL? {
        guard let value else { return nil }
        return URL(string: repository.client.resolveUrl(pathOrUrl: value))
    }

    private func load() async {
        isLoading = true
        error = nil
        do {
            let value: ComposeApp.HubArtist = try await withCheckedThrowingContinuation { continuation in
                repository.getArtist.execute(username: username) { value, error in
                    if let value { continuation.resume(returning: value) }
                    else { continuation.resume(throwing: error ?? HubDetailErrorType.noResponse) }
                }
            }
            artist = value
            isFollowing = value.isFollowing
            followersCount = Int(clamping: value.followersCount)
            projects = []
            nextCursor = nil
            await loadProjects()
        } catch {
            self.error = localization.string("home_hub_detail_artist_error", fallback: "The artist could not be loaded.")
        }
        isLoading = false
    }

    private func loadProjects() async {
        guard !isLoadingMore else { return }
        isLoadingMore = true
        defer { isLoadingMore = false }
        do {
            let cursor = nextCursor
            let page: ComposeApp.HubProjectPage = try await withCheckedThrowingContinuation { continuation in
                repository.getArtistProjects.execute(username: username, cursor: cursor, limit: 24) { value, error in
                    if let value { continuation.resume(returning: value) }
                    else { continuation.resume(throwing: error ?? HubDetailErrorType.noResponse) }
                }
            }
            projects.append(contentsOf: page.items)
            nextCursor = page.nextCursor
        } catch {
            actionError = localization.string("home_hub_detail_projects_error", fallback: "Projects could not be loaded.")
        }
    }

    private func toggleFollow() async {
        guard !followPending else { return }
        guard repository.client.isAuthenticated else { onSignIn(); return }
        followPending = true
        defer { followPending = false }
        do {
            let result: ComposeApp.HubFollowResult = try await withCheckedThrowingContinuation { continuation in
                let completion: (ComposeApp.HubFollowResult?, Error?) -> Void = { value, error in
                    if let value { continuation.resume(returning: value) }
                    else { continuation.resume(throwing: error ?? HubDetailErrorType.noResponse) }
                }
                if isFollowing { repository.unfollowArtist.execute(username: username, completionHandler: completion) }
                else { repository.followArtist.execute(username: username, completionHandler: completion) }
            }
            isFollowing = result.following
            followersCount = Int(clamping: result.followersCount)
        } catch {
            actionError = localization.string("home_hub_follow_error", fallback: "Follow status could not be updated.")
        }
    }
}

private struct HubProjectDetailView: View {
    let username: String
    let slug: String
    let repository: HubRepository
    let onOpen: (HubDestination) -> Void
    let onSignIn: () -> Void

    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization
    @State private var project: ComposeApp.HubProject?
    @State private var liked = false
    @State private var likesCount = 0
    @State private var likePending = false
    @State private var isLoading = true
    @State private var error: String?
    @State private var actionError: String?

    var body: some View {
        GeometryReader { available in
            let imageWidth = min(max(available.size.width - 48, 0), 720)
            let imageHeight = imageWidth * 9 / 16
            let overhang = max(100, imageHeight - 105)
            ScrollView {
                VStack(spacing: 0) {
                    HubDetailBackdrop(url: thumbnailURL, symbol: "square.grid.3x3.square")
                        .frame(height: 185)
                        .overlay {
                            GeometryReader { proxy in
                                HubDetailImage(url: thumbnailURL, symbol: "square.grid.3x3.square")
                                    .frame(width: imageWidth, height: imageHeight)
                                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                                    .position(x: proxy.size.width / 2, y: proxy.size.height + overhang - imageHeight / 2)
                            }
                        }
                        .zIndex(1)

                    VStack(alignment: .leading, spacing: 0) {
                        if let project {
                            HStack(alignment: .top, spacing: 12) {
                                HubDetailImage(url: creatorAvatarURL, symbol: "person.crop.circle.fill")
                                    .frame(width: 44, height: 44)
                                    .clipShape(Circle())

                                VStack(alignment: .leading, spacing: 4) {
                                    Text(project.title)
                                        .font(.title2.weight(.bold))
                                        .foregroundStyle(theme.onSurface)
                                        .fixedSize(horizontal: false, vertical: true)
                                    Button {
                                        onOpen(.artist(project.artist.username))
                                    } label: {
                                        Text(project.artist.displayName.isEmpty ? "@\(project.artist.username)" : project.artist.displayName)
                                            .font(.subheadline)
                                    }
                                    .buttonStyle(.plain)
                                    .foregroundStyle(theme.glassForeground)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .padding(.top, overhang + 16)

                            if let url = downloadURL {
                                Link(destination: url) {
                                    Label(localization.string("home_hub_detail_download", fallback: "Download"), systemImage: descriptionContent.externalDownloadURL == nil ? "arrow.down" : "arrow.up.right")
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(theme.primary)
                                .accessibilityHint(descriptionContent.externalDownloadURL == nil ? "" : localization.string("home_hub_detail_external_hint", fallback: "Opens external download page"))
                                .controlSize(.large)
                                .padding(.top, 24)
                            }

                            HStack(spacing: 0) {
                                metric("eye", value: project.views, label: localization.string("home_hub_detail_views", fallback: "Views"))
                                metric("arrow.down", value: project.downloadsCount, label: localization.string("home_hub_detail_downloads", fallback: "Downloads"))
                                metric("heart", value: Int64(likesCount), label: localization.string("home_hub_detail_likes", fallback: "Likes"))
                            }
                            .padding(.vertical, 24)

                            Divider()

                            if !descriptionContent.text.isEmpty {
                                Text(localization.string("home_hub_detail_about", fallback: "About"))
                                    .font(.title2.weight(.bold))
                                    .padding(.top, 28)
                                Text(descriptionContent.text)
                                    .font(.body)
                                    .foregroundStyle(theme.onSurface)
                                    .padding(.top, 10)
                            }

                            VStack(alignment: .leading, spacing: 12) {
                                Text(localization.string("home_hub_detail_details", fallback: "Details"))
                                    .font(.title2.weight(.bold))
                                LabeledContent(localization.string("home_hub_detail_format", fallback: "Format"), value: project.projectType.name.capitalized)
                                LabeledContent(localization.string("home_hub_detail_compatibility", fallback: "Compatibility"), value: project.compatibility.name.capitalized)
                                LabeledContent(localization.string("home_hub_detail_difficulty", fallback: "Difficulty")) {
                                    HubDifficultyRating(value: project.difficulty)
                                }
                                if let publishedDate = formattedPublishedDate(for: project) {
                                    LabeledContent(localization.string("home_hub_detail_published", fallback: "Published"), value: publishedDate)
                                }
                                if project.overrideDownloadUrl != nil {
                                    LabeledContent(localization.string("home_hub_detail_amethyst_file", fallback: "Amethyst .ame")) {
                                        HStack(spacing: 4) {
                                            Text(project.overrideName ?? "Amethyst .ame")
                                                .lineLimit(1)
                                                .truncationMode(.middle)
                                            if let size = project.overrideSize?.int64Value {
                                                Text("(\(ByteCountFormatter.string(fromByteCount: size, countStyle: .file)))")
                                            }
                                        }
                                    }
                                }
                                if let packageName = project.packageName {
                                    LabeledContent(localization.string("home_hub_detail_package_file", fallback: "Package file")) {
                                        Text(packageName)
                                            .lineLimit(1)
                                            .truncationMode(.middle)
                                    }
                                } else if let externalURL = descriptionContent.externalDownloadURL,
                                          let host = URL(string: externalURL)?.host {
                                    LabeledContent(localization.string("home_hub_detail_download_source", fallback: "Download source"), value: host)
                                }
                                if let size = project.packageSize?.int64Value {
                                    LabeledContent(localization.string("home_hub_detail_size", fallback: "File size"), value: ByteCountFormatter.string(fromByteCount: size, countStyle: .file))
                                }
                                if let checksum = project.packageSha256, !checksum.isEmpty {
                                    LabeledContent("SHA-256") {
                                        Button {
                                            UIPasteboard.general.string = checksum
                                        } label: {
                                            Label("\(checksum.prefix(10))…", systemImage: "doc.on.doc")
                                                .font(.footnote.monospaced())
                                        }
                                        .accessibilityLabel(localization.string("home_hub_detail_copy_checksum", fallback: "Copy SHA-256 checksum"))
                                    }
                                }
                            }
                            .padding(.top, 34)
                        } else if isLoading {
                            ProgressView()
                                .frame(maxWidth: .infinity)
                                .padding(.top, 60)
                        } else {
                            HubDetailError(message: error ?? localization.string("home_hub_detail_project_unavailable", fallback: "Project unavailable")) {
                                Task { await load() }
                            }
                            .padding(.top, 60)
                        }
                    }
                    .frame(maxWidth: 720, alignment: .leading)
                    .padding(.horizontal, 24)
                    .padding(.bottom, 48)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.surface)
                }
            }
        }
        .ignoresSafeArea(edges: .top)
        .task(id: "\(username)/\(slug)") { await load() }
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                Button {
                    Task { await toggleLike() }
                } label: {
                    Image(systemName: liked ? "heart.fill" : "heart")
                }
                .disabled(project == nil || likePending)
                .accessibilityLabel(liked ? localization.string("home_hub_detail_unlike", fallback: "Unlike project") : localization.string("home_hub_detail_like", fallback: "Like project"))

                if let url = shareURL {
                    ShareLink(item: url) {
                        Image(systemName: "square.and.arrow.up")
                    }
                    .accessibilityLabel(localization.string("home_hub_detail_share", fallback: "Share"))
                }
            }
        }
        .alert("Hub", isPresented: Binding(
            get: { actionError != nil },
            set: { if !$0 { actionError = nil } }
        )) {
            Button("OK", role: .cancel) { actionError = nil }
        } message: {
            Text(actionError ?? "")
        }
    }

    private var thumbnailURL: URL? {
        guard let value = project?.thumbnailUrl else { return nil }
        return URL(string: repository.client.resolveUrl(pathOrUrl: value))
    }
    private var creatorAvatarURL: URL? {
        guard let value = project?.artist.avatarUrl else { return nil }
        return URL(string: repository.client.resolveUrl(pathOrUrl: value))
    }
    private var descriptionContent: HubProjectDescription {
        HubProjectDescription(project?.description_ ?? "")
    }
    private var downloadURL: URL? {
        guard let value = descriptionContent.externalDownloadURL ?? project?.overrideDownloadUrl ?? project?.downloadUrl else { return nil }
        return URL(string: repository.client.resolveUrl(pathOrUrl: value))
    }
    private var shareURL: URL? { URL(string: "https://projects.launchpadders.com/@\(username)/\(slug)") }

    private func formattedPublishedDate(for project: ComposeApp.HubProject) -> String? {
        let timestamp = project.publishedAt?.int64Value ?? project.created
        guard timestamp > 0 else { return nil }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: localization.languageTag)
        formatter.dateStyle = .long
        return formatter.string(from: Date(timeIntervalSince1970: TimeInterval(timestamp)))
    }

    private func metric(_ symbol: String, value: Int64, label: String) -> some View {
        VStack(spacing: 4) {
            Label(value.formatted(.number.notation(.compactName)), systemImage: symbol)
                .font(.headline)
            Text(label)
                .font(.caption)
                .foregroundStyle(theme.onSurfaceVariant)
        }
        .frame(maxWidth: .infinity)
    }

    private func load() async {
        isLoading = true
        error = nil
        do {
            let value: ComposeApp.HubProject = try await withCheckedThrowingContinuation { continuation in
                repository.getPublishedProject.execute(username: username, slug: slug) { value, error in
                    if let value { continuation.resume(returning: value) }
                    else { continuation.resume(throwing: error ?? HubDetailErrorType.noResponse) }
                }
            }
            project = value
            liked = value.isLiked
            likesCount = Int(clamping: value.likesCount)
        } catch {
            self.error = localization.string("home_hub_detail_project_error", fallback: "The project could not be loaded.")
        }
        isLoading = false
    }

    private func toggleLike() async {
        guard let project, !likePending else { return }
        guard repository.client.isAuthenticated else { onSignIn(); return }
        likePending = true
        defer { likePending = false }
        do {
            let result: ComposeApp.HubLikeResult = try await withCheckedThrowingContinuation { continuation in
                repository.toggleProjectLike.execute(projectId: project.id) { value, error in
                    if let value { continuation.resume(returning: value) }
                    else { continuation.resume(throwing: error ?? HubDetailErrorType.noResponse) }
                }
            }
            liked = result.liked
            likesCount = Int(clamping: result.likesCount)
        } catch {
            actionError = localization.string("home_hub_detail_like_error", fallback: "Like status could not be updated.")
        }
    }
}

private struct HubDifficultyRating: View {
    let value: Int32

    var body: some View {
        let difficulty = min(max(Int(value), 0), 10)
        HStack(spacing: 3) {
            ForEach(0..<5, id: \.self) { index in
                Image(systemName: difficulty >= (index + 1) * 2 ? "star.fill" : difficulty == index * 2 + 1 ? "star.leadinghalf.filled" : "star")
                    .font(.caption)
            }
            Text(String(format: "%.1f / 5.0", Double(difficulty) / 2))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .padding(.leading, 4)
        }
        .foregroundStyle(Color(red: 1, green: 0.72, blue: 0.01))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(String(format: "%.1f / 5.0", Double(difficulty) / 2))
    }
}

private struct HubProjectRow: View {
    let project: ComposeApp.HubProject
    let repository: HubRepository
    @Environment(\.amethystTheme) private var theme

    var body: some View {
        HStack(spacing: 14) {
            HubDetailImage(url: project.thumbnailUrl.flatMap { URL(string: repository.client.resolveUrl(pathOrUrl: $0)) }, symbol: "square.grid.3x3.square")
                .frame(width: 72, height: 72)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            VStack(alignment: .leading, spacing: 4) {
                Text(project.title)
                    .font(.headline)
                    .foregroundStyle(theme.onSurface)
                    .lineLimit(2)
                Text(project.projectType.name.capitalized)
                    .font(.subheadline)
                    .foregroundStyle(theme.onSurfaceVariant)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(theme.onSurfaceVariant)
        }
        .padding(.vertical, 6)
        .contentShape(Rectangle())
    }
}

private struct HubDetailBackdrop: View {
    let url: URL?
    let symbol: String
    @Environment(\.amethystTheme) private var theme

    var body: some View {
        GeometryReader { proxy in
            HubDetailImage(url: url, symbol: symbol)
                .frame(width: proxy.size.width, height: proxy.size.height)
                .blur(radius: 34)
                .overlay(theme.surface.opacity(0.3))
                .clipped()
        }
        .background(theme.surfaceContainer)
        .accessibilityHidden(true)
    }
}

private struct HubDetailImage: View {
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
                            .font(.system(size: 32, weight: .medium))
                            .foregroundStyle(theme.onSurfaceVariant)
                    }
            }
        }
        .clipped()
        .accessibilityHidden(true)
    }
}

private struct HubDetailError: View {
    let message: String
    let retry: () -> Void
    @Environment(AppLocalization.self) private var localization

    var body: some View {
        ContentUnavailableView {
            Label(localization.string("home_hub_detail_could_not_load", fallback: "Could not load"), systemImage: "wifi.exclamationmark")
        } description: {
            Text(message)
        } actions: {
            Button(localization.string("home_hub_retry", fallback: "Try Again"), action: retry)
        }
    }
}

private enum HubDetailErrorType: Error { case noResponse }

private struct HubProjectDescription {
    let text: String
    let externalDownloadURL: String?

    init(_ raw: String) {
        let pattern = #"<!--\s*glacier-meta:\s*(\{[\s\S]*?\})\s*-->"#
        guard let expression = try? NSRegularExpression(pattern: pattern),
              let match = expression.firstMatch(in: raw, range: NSRange(raw.startIndex..., in: raw)),
              let fullRange = Range(match.range, in: raw),
              let jsonRange = Range(match.range(at: 1), in: raw) else {
            text = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            externalDownloadURL = nil
            return
        }

        let data = Data(raw[jsonRange].utf8)
        let metadata = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
        let external = (metadata?["externalDownloadUrl"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        if let external, let url = URL(string: external), ["http", "https"].contains(url.scheme?.lowercased() ?? "") {
            externalDownloadURL = external
        } else {
            externalDownloadURL = nil
        }
        var clean = raw
        clean.removeSubrange(fullRange)
        text = clean.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
