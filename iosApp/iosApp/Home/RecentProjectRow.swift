//
//  RecentProjectRow.swift
//  iosApp
//
//  Created by Copilot
//  Copyright © 2025 Anthony Hofmeister. All rights reserved.
//

import SwiftUI
import ComposeApp

struct RecentProjectRow: View {
    let project: RecentWorkspace
    let onOpen: () -> Void
    let onEdit: (() -> Void)?
    let onRemove: (() -> Void)?
    let onDeleteLocal: (() -> Void)?

    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization

    private var folderPath: String {
        abbreviatePath(project.path)
    }

    var body: some View {
        Button(action: onOpen) {
            HStack(alignment: .center, spacing: 14) {
                // Styled Icon Badge conforming strictly to AmethystTheme tokens
                ZStack {
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(theme.secondary)
                        .frame(width: 36, height: 36)

                    Image(systemName: fileIcon(for: project.path))
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(theme.secondaryForeground)
                }

                // Title + path
                VStack(alignment: .leading, spacing: 3) {
                    Text(project.title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(theme.foreground)
                        .lineLimit(1)

                    Text(folderPath)
                        .font(.system(size: 13, weight: .regular))
                        .foregroundStyle(theme.mutedForeground)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }

                Spacer()

                // Subtle interaction chevron indicator
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(theme.mutedForeground.opacity(0.5))
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .listRowBackground(theme.muted)
        .swipeActions(edge: .trailing, allowsFullSwipe: onDeleteLocal == nil) {
            if let onDeleteLocal {
                Button(action: onDeleteLocal) {
                    Label(localization.string("home_projects_delete_local", fallback: "Delete Local Project"), systemImage: "trash.slash")
                }
                .tint(theme.destructive)
            }
            if let onRemove {
                Button(role: .destructive, action: onRemove) {
                    Label(localization.string("common_remove", fallback: "Remove"), systemImage: "trash")
                }
            }
            if let onEdit {
                Button(action: onEdit) {
                    Label(localization.string("home_projects_item_menu_edit", fallback: "Edit"), systemImage: "pencil")
                }
                .tint(theme.primary)
            }
        }
        .contextMenu {
            Button(action: onOpen) {
                Label(localization.string("home_projects_item_menu_open", fallback: "Open"), systemImage: "folder.badge.arrow.up")
            }
            if let onEdit {
                Button(action: onEdit) {
                    Label(localization.string("home_projects_item_menu_edit", fallback: "Edit Details"), systemImage: "pencil")
                }
            }
            if onRemove != nil || onDeleteLocal != nil { Divider() }
            if let onRemove {
                Button(role: .destructive, action: onRemove) {
                    Label(localization.string("home_projects_item_menu_remove", fallback: "Remove from Recent"), systemImage: "trash")
                }
            }
            if let onDeleteLocal {
                // The project stays in the list until the confirmation action runs.
                Button(action: onDeleteLocal) {
                    Label(localization.string("home_projects_delete_local", fallback: "Delete Local Project"), systemImage: "trash.slash")
                }
            }
        }
    }

    // MARK: - Helpers

    private func fileIcon(for path: String) -> String {
        let ext = (path as NSString).pathExtension.lowercased()
        switch ext {
        case "als":             return "music.note.list"
        case "zip":             return "archivebox"
        case "approj":          return "waveform"
        default:                return "doc"
        }
    }

    private func abbreviatePath(_ path: String) -> String {
        let dir = (path as NSString).deletingLastPathComponent
        guard !dir.isEmpty else { return path }

        let home = NSHomeDirectory()
        if dir.hasPrefix(home) {
            return "~" + dir.dropFirst(home.count)
        }
        return dir
    }
}
