//
//  ProjectsTabView.swift
//  iosApp
//
//  Created by Copilot
//  Copyright © 2025 Anthony Hofmeister. All rights reserved.
//

import SwiftUI
import ComposeApp
import UniformTypeIdentifiers
import UIKit

/// Root view for the "Projects" tab.
///
/// Displays the recent-projects list with header actions and routes to
/// the creation sheet, edit sheet, and Ableton import wizard via the
/// shared `HomeViewModel`.
struct ProjectsTabView: View {
    @Bindable var viewModel: HomeViewModel
    @State private var projectToDelete: RecentWorkspace?
    @State private var recentRowFrames: [String: CGRect] = [:]

    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.recentProjects.isEmpty {
                    emptyState
                } else {
                    recentList
                }
            }
            .navigationTitle(localization.string("home_projects_title", fallback: "Recent Projects"))
            .navigationBarTitleDisplayMode(.large)
            .background(theme.background)
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    // Open existing project
                    Button {
                        viewModel.showingFilePicker = true
                    } label: {
                        Image(systemName: "folder.badge.plus")
                    }
                    .accessibilityLabel(localization.string("home_projects_action_open_desc", fallback: "Open Project"))

                    // Create new project
                    Button {
                        viewModel.activeSheet = .createProject
                    } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel(localization.string("home_projects_action_new_desc", fallback: "New Project"))
                }
            }
        }
        .tint(theme.glassForeground)
        // ── File picker ────────────────────────────────────────────────
        .fileImporter(
            isPresented: $viewModel.showingFilePicker,
            allowedContentTypes: [
                UTType(filenameExtension: "ame")    ?? .data,
                UTType(filenameExtension: "als")    ?? .data,
                UTType(filenameExtension: "zip")    ?? .data,
                UTType(filenameExtension: "approj") ?? .data,
                .zip,
                .data,
            ]
        ) { result in
            switch result {
            case .success(let url):
                viewModel.openFile(url: url)
            case .failure(let error):
                viewModel.errorMessage = error.localizedDescription
            }
        }
        // ── Sheet routing ──────────────────────────────────────────────
        .sheet(item: $viewModel.activeSheet) { sheet in
            switch sheet {
            case .createProject:
                ProjectCreationSheet(editPath: nil, viewModel: viewModel)

            case .editProject(let path):
                ProjectCreationSheet(editPath: path, viewModel: viewModel)

            case .abletonWizard(let path):
                AbletonImportWizardSheet(importPath: path, viewModel: viewModel)
            }
        }
        // ── Error alert ────────────────────────────────────────────────
        .alert(localization.string("common_error_title", fallback: "Something went wrong"), isPresented: errorBinding) {
            Button(localization.string("common_ok", fallback: "OK"), role: .cancel) { viewModel.errorMessage = nil }
        } message: {
            if let msg = viewModel.errorMessage {
                Text(msg)
            }
        }
    }

    // MARK: - Subviews

    private var recentList: some View {
        List {
            ForEach(viewModel.recentProjects, id: \.path) { project in
                RecentProjectRow(
                    project: project,
                    onOpen:   { viewModel.openRecent(project) },
                    onEdit:   !viewModel.isStoredImport(path: project.path) && project.path.lowercased().hasSuffix(".ame")
                        ? { viewModel.activeSheet = .editProject(path: project.path) } : nil,
                    onRemove: viewModel.isStoredImport(path: project.path)
                        ? nil : { viewModel.removeRecent(path: project.path) },
                    onDeleteLocal: viewModel.isStoredImport(path: project.path) ? { projectToDelete = project } : nil
                )
                .background {
                    GeometryReader { proxy in
                        Color.clear.preference(
                            key: RecentRowFrameKey.self,
                            value: [project.path: proxy.frame(in: .global)]
                        )
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .background(theme.background)
        .background {
            DeleteProjectConfirmationPresenter(
                project: $projectToDelete,
                rowFrames: recentRowFrames,
                prompt: localization.string("home_projects_delete_local_confirm", fallback: "Delete this downloaded project from this device?"),
                deleteTitle: localization.string("home_projects_delete_local", fallback: "Delete Local Project"),
                onDelete: { viewModel.deleteStoredImport(path: $0) }
            )
        }
        .onPreferenceChange(RecentRowFrameKey.self) { recentRowFrames = $0 }
    }

    private var emptyState: some View {
        VStack(spacing: 0) {
            Spacer()

            VStack(spacing: 12) {
                Image(systemName: "folder")
                    .font(.system(size: 56))
                    .foregroundStyle(theme.mutedForeground.opacity(0.6))
                    .padding(.bottom, 4)

                Text(localization.string("home_projects_empty_title", fallback: "No Recent Projects"))
                    .font(.title2.weight(.bold))
                    .foregroundStyle(theme.foreground)

                Text(localization.string("home_projects_empty_desc", fallback: "Open an existing workspace or create a new project to get started."))
                    .font(.subheadline)
                    .foregroundStyle(theme.mutedForeground)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
            }

            VStack(spacing: 8) {
                Button(action: { viewModel.activeSheet = .createProject }) {
                    Label(localization.string("home_projects_new_button", fallback: "New Project"), systemImage: "plus")
                        .font(.body.weight(.semibold))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .tint(theme.primary)

                Button(action: { viewModel.showingFilePicker = true }) {
                    Label(localization.string("home_projects_open_button", fallback: "Open File"), systemImage: "folder.badge.plus")
                        .font(.body.weight(.semibold))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .controlSize(.large)
                .tint(theme.primary)
            }
            .padding(.top, 24)
            .padding(.horizontal, 16)

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(theme.background)
    }

    // MARK: - Helpers

    private var errorBinding: Binding<Bool> {
        Binding(
            get:  { viewModel.errorMessage != nil },
            set:  { if !$0 { viewModel.errorMessage = nil } }
        )
    }
}

private struct RecentRowFrameKey: PreferenceKey {
    static var defaultValue: [String: CGRect] = [:]

    static func reduce(value: inout [String: CGRect], nextValue: () -> [String: CGRect]) {
        value.merge(nextValue(), uniquingKeysWith: { _, next in next })
    }
}

private struct DeleteProjectConfirmationPresenter: UIViewControllerRepresentable {
    @Binding var project: RecentWorkspace?
    let rowFrames: [String: CGRect]
    let prompt: String
    let deleteTitle: String
    let onDelete: (String) -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        UIViewController()
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {
        guard let project, let frame = rowFrames[project.path],
              context.coordinator.presentingPath == nil else { return }

        let path = project.path
        context.coordinator.presentingPath = path
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            guard self.project?.path == path, let window = controller.view.window,
                  let presenter = window.rootViewController else {
                context.coordinator.presentingPath = nil
                return
            }

            let alert = UIAlertController(title: prompt, message: nil, preferredStyle: .actionSheet)
            alert.modalPresentationStyle = .popover
            alert.addAction(UIAlertAction(title: deleteTitle, style: .destructive) { _ in
                onDelete(path)
                self.project = nil
                context.coordinator.presentingPath = nil
            })

            if let popover = alert.popoverPresentationController {
                popover.sourceView = window
                popover.sourceRect = CGRect(x: window.bounds.midX, y: frame.maxY, width: 1, height: 1)
                popover.permittedArrowDirections = .up
                popover.delegate = context.coordinator
            }
            alert.presentationController?.delegate = context.coordinator
            presenter.present(alert, animated: true)
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator(project: $project) }

    final class Coordinator: NSObject, UIPopoverPresentationControllerDelegate {
        var project: Binding<RecentWorkspace?>
        var presentingPath: String?

        init(project: Binding<RecentWorkspace?>) {
            self.project = project
        }

        func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
            .none
        }

        func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
            project.wrappedValue = nil
            presentingPath = nil
        }
    }
}
