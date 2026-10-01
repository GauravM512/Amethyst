import SwiftUI
import ComposeApp

struct HubProjectListRow: View {
    let project: ComposeApp.HubProject
    let repository: HubRepository

    @Environment(\.amethystTheme) private var theme

    var body: some View {
        HStack(spacing: 14) {
            HubCachedAsyncImage(url: imageURL) { phase in
                if let image = phase.image {
                    image.resizable().scaledToFill()
                } else {
                    Rectangle()
                        .fill(theme.surfaceContainerHigh)
                        .overlay {
                            Image(systemName: "square.grid.3x3.square")
                                .font(.title3)
                                .foregroundStyle(theme.onSurfaceVariant)
                        }
                }
            }
            .frame(width: 64, height: 64)
            .clipped()
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .accessibilityHidden(true)

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

    private var imageURL: URL? {
        guard let value = project.thumbnailUrl, !value.isEmpty else { return nil }
        return URL(string: repository.client.resolveUrl(pathOrUrl: value))
    }
}
