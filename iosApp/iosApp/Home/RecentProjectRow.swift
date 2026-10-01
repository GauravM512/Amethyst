import SwiftUI
import ComposeApp

struct RecentProjectRow: View {
    let project: RecentWorkspace
    let isStoredImport: Bool
    let isHubDownload: Bool
    let hubProject: ComposeApp.HubProject?
    let repository: HubRepository
    let onOpen: () -> Void
    let onViewHub: (() -> Void)?
    let onViewArtist: (() -> Void)?
    let onEdit: (() -> Void)?
    let onDeleteLocal: (() -> Void)?

    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization
    @State private var localAuthor: String?

    var body: some View {
        HStack(spacing: 12) {
            Button(action: onOpen) {
                HStack(spacing: 12) {
                    artwork
                    VStack(alignment: .leading, spacing: 4) {
                        Text(project.title)
                            .font(.body.weight(.semibold))
                            .foregroundStyle(theme.foreground)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                        if let hubProject {
                            HStack(spacing: 6) {
                                avatar(for: hubProject)
                                Text(hubProject.artist.displayName.isEmpty ? "@\(hubProject.artist.username)" : hubProject.artist.displayName)
                                    .lineLimit(1)
                            }
                        } else if !isHubDownload && isAmethystProject {
                            Text(localAuthor ?? localization.string("home_project_unknown_author", fallback: "Unknown Author"))
                                .lineLimit(1)
                        } else {
                            Text(projectKind).lineLimit(1)
                        }
                    }
                    .font(.subheadline)
                    .foregroundStyle(theme.mutedForeground)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            Menu {
                actions
            } label: {
                Image(systemName: "ellipsis")
                    .font(.body.weight(.semibold))
                    .foregroundStyle(theme.mutedForeground)
                    .frame(width: 32, height: 44)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel(localization.string("home_projects_item_options_desc", fallback: "Project options"))
        }
        .padding(.vertical, 5)
        .listRowBackground(theme.background)
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            if let onDeleteLocal {
                Button(action: onDeleteLocal) {
                    Label(localization.string("home_projects_delete_local", fallback: "Delete Local Project"), systemImage: "trash.slash")
                }
                .tint(theme.destructive)
            }
        }
        .contextMenu { actions }
        .task(id: "\(project.path):\(project.lastOpened)") {
            guard !isHubDownload, isAmethystProject else { return }
            let details = try? await HomeRepository.shared.loadProjectDetails(path: project.path)
            let author = details?.author.trimmingCharacters(in: .whitespacesAndNewlines)
            localAuthor = author?.isEmpty == false ? author : nil
        }
    }

    @ViewBuilder
    private var actions: some View {
        Button(action: onOpen) {
            Label(localization.string("home_projects_item_menu_open", fallback: "Open"), systemImage: "folder.badge.arrow.up")
        }
        if let onViewHub {
            Button(action: onViewHub) {
                Label(localization.string("home_projects_view_hub", fallback: "View on Hub"), systemImage: "globe")
            }
        }
        if let onViewArtist {
            Button(action: onViewArtist) {
                Label(localization.string("home_projects_view_artist", fallback: "View Artist"), systemImage: "person.crop.circle")
            }
        }
        if let onEdit {
            Button(action: onEdit) {
                Label(localization.string("home_projects_item_menu_edit", fallback: "Edit Details"), systemImage: "pencil")
            }
        }
        if onDeleteLocal != nil { Divider() }
        if let onDeleteLocal {
            Button(action: onDeleteLocal) {
                Label(localization.string("home_projects_delete_local", fallback: "Delete Local Project"), systemImage: "trash.slash")
            }
        }
    }

    @ViewBuilder
    private var artwork: some View {
        if let hubProject, let url = imageURL(hubProject.thumbnailUrl) {
            HubCachedAsyncImage(url: url) { phase in
                if let image = phase.image {
                    image.resizable().scaledToFill()
                } else {
                    fallbackArtwork
                }
            }
            .frame(width: 58, height: 58)
            .clipped()
            .clipShape(RoundedRectangle(cornerRadius: 10))
        } else {
            fallbackArtwork
                .frame(width: 58, height: 58)
                .clipShape(RoundedRectangle(cornerRadius: 10))
        }
    }

    private var fallbackArtwork: some View {
        Rectangle()
            .fill(theme.surfaceContainerHigh)
            .overlay {
                Image(systemName: isStoredImport ? "square.grid.3x3.square" : fileIcon)
                    .font(.system(size: 20))
                    .foregroundStyle(theme.mutedForeground)
            }
    }

    private func avatar(for project: ComposeApp.HubProject) -> some View {
        HubCachedAsyncImage(url: imageURL(project.artist.avatarUrl)) { phase in
            if let image = phase.image {
                image.resizable().scaledToFill()
            } else {
                Image(systemName: "person.crop.circle.fill")
                    .resizable()
                    .foregroundStyle(theme.mutedForeground)
            }
        }
        .frame(width: 18, height: 18)
        .clipShape(Circle())
    }

    private func imageURL(_ value: String?) -> URL? {
        guard let value, !value.isEmpty else { return nil }
        return URL(string: repository.client.resolveUrl(pathOrUrl: value))
    }

    private var projectKind: String {
        if isStoredImport { return localization.string("home_projects_recent_downloaded", fallback: "Downloaded") }
        switch (project.path as NSString).pathExtension.lowercased() {
        case "ame": return localization.string("home_hub_catalog_amethyst", fallback: "Amethyst")
        case "als": return localization.string("home_hub_catalog_ableton", fallback: "Ableton")
        case "approj": return localization.string("home_hub_catalog_apollo", fallback: "Apollo")
        default: return localization.string("home_projects_recent_local", fallback: "Local project")
        }
    }

    private var isAmethystProject: Bool {
        (project.path as NSString).pathExtension.lowercased() == "ame"
    }

    private var fileIcon: String {
        switch (project.path as NSString).pathExtension.lowercased() {
        case "als": return "music.note.list"
        case "zip": return "archivebox"
        case "approj": return "waveform"
        default: return "doc"
        }
    }
}
