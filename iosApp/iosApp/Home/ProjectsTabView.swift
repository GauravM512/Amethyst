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
    let repository: HubRepository
    let onShowProfile: () -> Void
    @State private var projectToDelete: RecentWorkspace?
    @State private var recentRowFrames: [String: CGRect] = [:]
    @State private var hubProjects: [String: ComposeApp.HubProject] = [:]
    @State private var hubDestination: HubDestination?

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
        .sheet(item: $hubDestination) { destination in
            HubDetailView(
                destination: destination,
                repository: repository,
                onSignIn: {
                    hubDestination = nil
                    onShowProfile()
                },
                onOpenDownloadedFile: { url, projectID, title in
                    hubDestination = nil
                    viewModel.openDownloadedFile(url: url, projectID: projectID, title: title)
                }
            )
        }
        .task(id: downloadKey) {
            await loadHubProjects()
        }
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
            if !localProjects.isEmpty {
                sectionHeading(localization.string("home_projects_local_section", fallback: "Local Projects"), isFirst: true)
                ForEach(localProjects, id: \.path) { project in
                    recentRow(project)
                        .listRowSeparator(project.path == localProjects.last?.path ? .hidden : .visible, edges: .bottom)
                }
            }
            if !downloadedProjects.isEmpty {
                sectionHeading(
                    localization.string("home_projects_downloaded_section", fallback: "Downloaded"),
                    isFirst: localProjects.isEmpty
                )
                ForEach(downloadedProjects, id: \.path) { project in
                    recentRow(project)
                        .listRowSeparator(project.path == downloadedProjects.last?.path ? .hidden : .visible, edges: .bottom)
                }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(theme.background)
        .background {
            DeleteProjectConfirmationPresenter(
                project: $projectToDelete,
                rowFrames: recentRowFrames,
                prompt: localization.string("home_projects_delete_local_confirm", fallback: "Delete this project and its files from this device?"),
                deleteTitle: localization.string("home_projects_delete_local", fallback: "Delete Local Project"),
                onDelete: { viewModel.deleteLocalProject(path: $0) }
            )
        }
        .onPreferenceChange(RecentRowFrameKey.self) { recentRowFrames = $0 }
    }

    private func sectionHeading(_ title: String, isFirst: Bool) -> some View {
        Text(title)
            .font(.title3.weight(.bold))
            .foregroundStyle(theme.foreground)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, isFirst ? 0 : 12)
            .padding(.bottom, 2)
            .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
            .listRowSeparator(.hidden)
            .listRowBackground(theme.background)
    }

    private var downloadedProjects: [RecentWorkspace] {
        viewModel.recentProjects.filter { hubID(for: $0) != nil }
    }

    private var localProjects: [RecentWorkspace] {
        viewModel.recentProjects.filter { hubID(for: $0) == nil }
    }

    private var downloadKey: String {
        downloadedProjects.map { "\($0.path):\($0.title)" }.joined(separator: "|")
    }

    private func hubID(for project: RecentWorkspace) -> String? {
        HomeSwiftBridge.shared.mobileProjectForPath(path: project.path)?.hubProjectId
    }

    private func recentRow(_ project: RecentWorkspace) -> some View {
        let isStoredImport = viewModel.isStoredImport(path: project.path)
        let canDelete = viewModel.canDeleteLocalProject(path: project.path)
        let hubProject = hubID(for: project).flatMap { hubProjects[$0] }
        return RecentProjectRow(
            project: project,
            isStoredImport: isStoredImport,
            isHubDownload: hubID(for: project) != nil,
            hubProject: hubProject,
            repository: repository,
            onOpen: { viewModel.openRecent(project) },
            onViewHub: hubProject.map { item in
                { hubDestination = .project(username: item.artist.username, slug: item.slug) }
            },
            onViewArtist: hubProject.map { item in
                { hubDestination = .artist(item.artist.username) }
            },
            onEdit: !isStoredImport && project.path.lowercased().hasSuffix(".ame")
                ? { viewModel.activeSheet = .editProject(path: project.path) } : nil,
            onDeleteLocal: canDelete ? { projectToDelete = project } : nil
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

    private func loadHubProjects() async {
        for project in downloadedProjects {
            guard !Task.isCancelled, let id = hubID(for: project), hubProjects[id] == nil else { continue }
            do {
                let page: ComposeApp.HubProjectPage = try await withCheckedThrowingContinuation { continuation in
                    repository.browseProjects.execute(
                        cursor: nil, limit: 50, compatibility: nil, type: nil,
                        sort: nil, query: project.title, difficulty: nil
                    ) { page, error in
                        if let page {
                            continuation.resume(returning: page)
                        } else {
                            continuation.resume(throwing: error ?? RecentProjectHubError.missingResponse)
                        }
                    }
                }
                if let match = page.items.first(where: { $0.id == id }) {
                    hubProjects[id] = match
                }
            } catch {
                // Local projects remain available when Hub metadata cannot be reached.
            }
        }
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

private enum RecentProjectHubError: Error {
    case missingResponse
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
