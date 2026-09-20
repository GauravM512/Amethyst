//
//  ProjectCreationSheet.swift
//  iosApp
//
//  Created by Copilot
//  Copyright © 2025 Anthony Hofmeister. All rights reserved.
//

import SwiftUI
import ComposeApp

/// Sheet for creating a new project or editing an existing one.
///
/// - Pass `editPath = nil` for creation mode.
/// - Pass `editPath = "/path/to/file.ame"` for edit mode.
struct ProjectCreationSheet: View {
    let editPath: String?
    let viewModel: HomeViewModel

    @Environment(\.dismiss) private var dismiss
    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization

    @State private var name: String = ""
    @State private var author: String = ""
    @State private var nameErrorVisible = false

    private var isEditing: Bool { editPath != nil }

    var body: some View {
        NavigationStack {
            Form {
                // Project name
                Section {
                    TextField(
                        localization.string("home_project_creation_sheet_name_label", fallback: "Project name"),
                        text: $name,
                        prompt: Text(localization.string("home_project_creation_sheet_name_placeholder", fallback: "My next performance"))
                            .foregroundStyle(theme.mutedForeground.opacity(0.72))
                    )
                        .foregroundStyle(theme.foreground)
                        .autocorrectionDisabled()
                        .onChange(of: name) { _, _ in
                            if nameErrorVisible && !name.trimmingCharacters(in: .whitespaces).isEmpty {
                                nameErrorVisible = false
                            }
                        }
                } header: {
                    Text(localization.string("home_project_creation_sheet_name_label", fallback: "Project Name"))
                } footer: {
                    if nameErrorVisible {
                        Label(localization.string("home_project_creation_sheet_name_error", fallback: "Please enter a project name."), systemImage: "exclamationmark.circle.fill")
                            .foregroundStyle(.red)
                            .font(.footnote)
                    } else {
                        Text(localization.string("home_project_creation_name_summary", fallback: "Shown in your workspace and recent projects."))
                    }
                }
                .listRowBackground(theme.muted)

                // Author
                Section {
                    TextField(
                        localization.string("home_project_creation_sheet_author_label", fallback: "Author"),
                        text: $author,
                        prompt: Text(localization.string("home_project_creation_sheet_author_placeholder", fallback: "Your name"))
                            .foregroundStyle(theme.mutedForeground.opacity(0.72))
                    )
                        .foregroundStyle(theme.foreground)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.words)
                } header: {
                    Text(localization.string("home_project_creation_sheet_author_label", fallback: "Author"))
                } footer: {
                    Text(localization.string("home_project_creation_sheet_author_desc", fallback: "Saved as your default. Leave blank to fall back to \"Unknown Author\"."))
                }
                .listRowBackground(theme.muted)
            }
            .scrollContentBackground(.hidden)
            .background(theme.background.ignoresSafeArea())
            .navigationTitle(
                isEditing
                    ? localization.string("home_project_creation_sheet_edit_title", fallback: "Edit Project")
                    : localization.string("home_project_creation_sheet_new_title", fallback: "New Project")
            )
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(localization.string("home_project_creation_sheet_cancel", fallback: "Cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(
                        isEditing
                            ? localization.string("home_project_creation_sheet_save", fallback: "Save")
                            : localization.string("home_project_creation_sheet_create", fallback: "Create")
                    ) {
                        submit()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
        .tint(theme.glassForeground)
        .onAppear {
            if isEditing {
                // Pre-fill fields are set via task below
            } else {
                author = viewModel.localAuthor()
            }
        }
        .task(id: editPath) {
            guard let path = editPath else { return }
            if let details = try? await HomeRepository.shared.loadProjectDetails(path: path) {
                name   = details.name
                author = details.author
            }
        }
    }

    // MARK: - Submit

    private func submit() {
        let trimmedName = name.trimmingCharacters(in: .whitespaces)
        guard !trimmedName.isEmpty else {
            nameErrorVisible = true
            return
        }

        dismiss()

        if let path = editPath {
            viewModel.updateProject(path: path, name: trimmedName, author: author)
        } else {
            viewModel.createProject(name: trimmedName, author: author)
        }
    }

}
