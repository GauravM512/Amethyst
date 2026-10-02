//
//  HomeViewModel.swift
//  iosApp
//
//  Created by Copilot
//  Copyright © 2025 Anthony Hofmeister. All rights reserved.
//

import Foundation
import ComposeApp
import UniformTypeIdentifiers

// MARK: - Sheet routing

enum HomeSheet: Identifiable {
    case createProject
    case editProject(path: String)
    case abletonWizard(path: String)

    var id: String {
        switch self {
        case .createProject:              return "create"
        case .editProject(let path):      return "edit-\(path)"
        case .abletonWizard(let path):    return "ableton-\(path)"
        }
    }
}

struct RecentProjectItem: Identifiable {
    let project: RecentWorkspace
    let hubID: String?
    let isStoredImport: Bool
    let canDelete: Bool

    var id: String { project.path }
}

// MARK: - ViewModel

@Observable
@MainActor
final class HomeViewModel {

    // Published state
    var recentProjects: [RecentWorkspace] = []
    private(set) var localProjects: [RecentProjectItem] = []
    private(set) var downloadedProjects: [RecentProjectItem] = []
    @ObservationIgnored private var authorTasks: [String: Task<String?, Never>] = [:]
    var isLoading = false
    var loadingProgress: Double = 0.0
    var loadingTitle: String? = nil
    var loadingStatusText: String = IosLocalizationBridge.shared.string(
        key: "home_loading_default_status",
        fallback: "Preparing..."
    )
    var loadingDetailText: String? = nil
    var errorMessage: String? = nil
    var activeSheet: HomeSheet? = nil
    var isWorkspaceOpen = false
    var showingFilePicker = false

    private var pendingHubProjects: [(HubProjectDeepLink, HubRepository)] = []
    private var isOpeningHubProject = false
    var showsHubWorkspaceChangeAlert = false
    var isSavingHubWorkspace = false

    // Ableton wizard: pending import parameters set by openFile() or getZipFormat callback
    var pendingAbletonPath: String? = nil

    // ── Lifecycle ──────────────────────────────────────────────────────────

    init() {
        loadRecents()
        startObservingLoadingProgress()
    }

    private func startObservingLoadingProgress() {
        HomeSwiftBridge.shared.observeLoadingProgress(
            onUpdate: { [weak self] progress, title, statusText, detailText in
                Task { @MainActor [weak self] in
                    guard let self else { return }
                    let p = Double(truncating: progress as NSNumber)
                    self.loadingProgress = p
                    self.loadingTitle = title
                    self.loadingStatusText = statusText
                    self.loadingDetailText = detailText
                }
            },
            onFinished: {}
        )
    }

    func loadRecents() {
        let raw = HomeSwiftBridge.shared.recentWorkspaces()
        recentProjects = ((raw as? [RecentWorkspace]) ?? []).filter {
            FileManager.default.fileExists(atPath: $0.path)
        }
        let items = recentProjects.map { project in
            RecentProjectItem(
                project: project,
                hubID: HomeSwiftBridge.shared.mobileProjectForPath(path: project.path)?.hubProjectId,
                isStoredImport: isStoredImport(path: project.path),
                canDelete: canDeleteLocalProject(path: project.path)
            )
        }
        localProjects = items.filter { $0.hubID == nil }
        downloadedProjects = items.filter { $0.hubID != nil }
        authorTasks.removeAll()
    }

    func projectAuthor(path: String) async -> String? {
        if let task = authorTasks[path] {
            return await task.value
        }
        let task = Task<String?, Never> {
            let details = try? await HomeRepository.shared.loadProjectDetails(path: path)
            let author = details?.author.trimmingCharacters(in: .whitespacesAndNewlines)
            return author?.isEmpty == false ? author : nil
        }
        authorTasks[path] = task
        return await task.value
    }

    // ── Recent projects ────────────────────────────────────────────────────

    func removeRecent(path: String) {
        HomeSwiftBridge.shared.removeRecentWorkspace(path: path)
        loadRecents()
    }

    func isStoredImport(path: String) -> Bool {
        guard let documents = try? FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false) else { return false }
        let projects = documents.appendingPathComponent("Amethyst/Projects", isDirectory: true).standardizedFileURL.path
        let original = URL(fileURLWithPath: path).deletingLastPathComponent()
        return original.lastPathComponent == "Original" && original.deletingLastPathComponent().standardizedFileURL.path.hasPrefix(projects + "/")
    }

    func canDeleteLocalProject(path: String) -> Bool {
        managedProjectDeletionURL(path: path) != nil
    }

    func deleteLocalProject(path: String) {
        guard let target = managedProjectDeletionURL(path: path) else { return }
        do {
            try FileManager.default.removeItem(at: target)
            HomeSwiftBridge.shared.removeRecentWorkspace(path: path)
            loadRecents()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func managedProjectDeletionURL(path: String) -> URL? {
        guard let documents = try? FileManager.default.url(
            for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false
        ) else { return nil }
        let amethyst = documents.appendingPathComponent("Amethyst", isDirectory: true)
            .standardizedFileURL.resolvingSymlinksInPath()
        let file = URL(fileURLWithPath: path).standardizedFileURL.resolvingSymlinksInPath()
        let parent = file.deletingLastPathComponent()

        if parent.lastPathComponent == "Original" {
            let projectDirectory = parent.deletingLastPathComponent()
            let projectsRoot = amethyst.appendingPathComponent("Projects", isDirectory: true)
            if projectDirectory.deletingLastPathComponent() == projectsRoot {
                return projectDirectory
            }
        }

        let supported = ["ame", "als", "zip", "approj"]
        return parent == amethyst && supported.contains(file.pathExtension.lowercased()) ? file : nil
    }

    func openRecent(_ project: RecentWorkspace) {
        if !HomeSwiftBridge.shared.hasConvertedMobileProject(path: project.path) {
            let ext = (project.path as NSString).pathExtension.lowercased()
            if ext == "als" {
                pendingAbletonPath = project.path
                activeSheet = .abletonWizard(path: project.path)
                return
            }
            if ext == "zip" {
                detectZipAndRoute(storedPath: project.path)
                return
            }
        }
        startLoading(localized("home_projects_opening_project_msg", fallback: "Opening Project"))
        HomeSwiftBridge.shared.openRecentWorkspace(
            project: project,
            onSuccess: { [weak self] in Task { @MainActor [weak self] in self?.handleWorkspaceOpened() } },
            onError:   { [weak self] msg in Task { @MainActor [weak self] in self?.handleError(msg) } }
        )
    }

    // ── Project creation / editing ─────────────────────────────────────────

    func createProject(name: String, author: String) {
        startLoading(localized("home_project_creation_create_project", fallback: "Creating Project"))
        HomeSwiftBridge.shared.createProject(
            name: name,
            author: author,
            onSuccess: { [weak self] in Task { @MainActor [weak self] in self?.handleWorkspaceOpened() } },
            onError:   { [weak self] msg in Task { @MainActor [weak self] in self?.handleError(msg) } }
        )
    }

    func updateProject(path: String, name: String, author: String) {
        startLoading(localized("home_project_creation_save_changes", fallback: "Saving Changes"))
        HomeSwiftBridge.shared.updateProject(
            path: path,
            name: name,
            author: author,
            onSuccess: { [weak self] in Task { @MainActor [weak self] in self?.handleWorkspaceOpened() } },
            onError:   { [weak self] msg in Task { @MainActor [weak self] in self?.handleError(msg) } }
        )
    }

    // ── File picker ────────────────────────────────────────────────────────

    func openFile(url: URL) {
        importLocalFile(url: url, projectID: UUID().uuidString, title: url.deletingPathExtension().lastPathComponent, hubProjectID: nil, isSecurityScoped: true)
    }

    func openDownloadedFile(url: URL, projectID: String, title: String) {
        importLocalFile(url: url, projectID: "hub-\(projectID)", title: title, hubProjectID: projectID, isSecurityScoped: false)
    }

    func openHubProject(link: HubProjectDeepLink, repository: HubRepository) {
        if pendingHubProjects.contains(where: { $0.0.projectId == link.projectId }) {
            return
        }
        pendingHubProjects.append((link, repository))
        openNextHubProject()
    }

    func saveAndOpenPendingHubProject() {
        guard !isSavingHubWorkspace else { return }
        isSavingHubWorkspace = true
        HomeRepository.shared.saveOpenMobileWorkspace { saved, _ in
            Task { @MainActor in
                self.isSavingHubWorkspace = false
                if saved?.boolValue == true {
                    self.openNextHubProject()
                } else {
                    self.showsHubWorkspaceChangeAlert = true
                }
            }
        }
    }

    func discardAndOpenPendingHubProject() {
        openNextHubProject(discardUnsavedChanges: true)
    }

    func cancelPendingHubProjects() {
        pendingHubProjects.removeAll()
        showsHubWorkspaceChangeAlert = false
    }

    private func openNextHubProject(discardUnsavedChanges: Bool = false) {
        guard !isOpeningHubProject, !isLoading, !isSavingHubWorkspace, !pendingHubProjects.isEmpty else { return }
        if isWorkspaceOpen, WorkspaceRepository.shared.hasUnsavedChanges(), !discardUnsavedChanges {
            showsHubWorkspaceChangeAlert = true
            return
        }
        let (link, repository) = pendingHubProjects.removeFirst()
        isOpeningHubProject = true
        activeSheet = nil
        errorMessage = nil
        startLoading(localized("home_hub_detail_downloading", fallback: "Downloading"))
        Task {
            do {
                let project: ComposeApp.HubProject = try await withCheckedThrowingContinuation { continuation in
                    link.resolve(repository: repository) { project, error in
                        if let project {
                            continuation.resume(returning: project)
                        } else {
                            continuation.resume(throwing: error ?? HubProjectDownloadError.invalidResponse)
                        }
                    }
                }
                let plan = HubProjectImportPlan(project: project, repository: repository)
                guard plan.canDownloadAndOpen, let source = plan.source else {
                    throw HubProjectDownloadError.unavailable
                }
                let url = try await HubProjectDownloadUseCase().execute(
                    source: source,
                    suggestedFilename: plan.suggestedFilename,
                    expectedSize: plan.expectedSize,
                    onProgress: { [weak self] progress in
                        Task { @MainActor [weak self] in
                            guard let self else { return }
                            self.loadingProgress = max(self.loadingProgress, progress)
                        }
                    }
                )
                await storeAndRouteLocalFile(
                    url: url,
                    projectID: "hub-\(project.id)",
                    title: project.title,
                    hubProjectID: project.id,
                    isSecurityScoped: false,
                    automaticOpen: true
                )
            } catch {
                handleError(localized(
                    "home_hub_detail_download_error",
                    fallback: "The project could not be downloaded. Check that the file is publicly accessible."
                ))
            }
            isOpeningHubProject = false
            openNextHubProject()
        }
    }

    private func importLocalFile(url: URL, projectID: String, title: String, hubProjectID: String?, isSecurityScoped: Bool) {
        Task {
            await storeAndRouteLocalFile(
                url: url,
                projectID: projectID,
                title: title,
                hubProjectID: hubProjectID,
                isSecurityScoped: isSecurityScoped
            )
        }
    }

    private func storeAndRouteLocalFile(url: URL, projectID: String, title: String, hubProjectID: String?, isSecurityScoped: Bool, automaticOpen: Bool = false) async {
        let ext = url.pathExtension.lowercased()
        guard ["ame", "approj", "als", "zip"].contains(ext) else {
            handleError(localized("home_error_unsupported_project_format", fallback: "Unsupported project file format: .%1$s")
                .replacingOccurrences(of: "%1$s", with: ext))
            return
        }
        do {
            let stored = try await Task.detached(priority: .userInitiated) {
                let scoped = isSecurityScoped && url.startAccessingSecurityScopedResource()
                guard !isSecurityScoped || scoped else {
                    throw CocoaError(.fileReadNoPermission)
                }
                defer {
                    if scoped { url.stopAccessingSecurityScopedResource() }
                    if !isSecurityScoped { try? FileManager.default.removeItem(at: url.deletingLastPathComponent()) }
                }
                return try MobileProjectFiles.importOriginal(from: url, projectID: projectID)
            }.value
            HomeSwiftBridge.shared.registerMobileProject(
                id: projectID,
                title: title,
                originalPath: stored.url.path,
                importedAt: Int64(Date().timeIntervalSince1970 * 1000),
                hubProjectId: hubProjectID,
                sourceHash: stored.sha256
            )
            loadRecents()
            isLoading = false
            if automaticOpen {
                openIndexedFile(path: stored.url.path)
            } else {
                routeImportedFile(path: stored.url.path, ext: ext)
            }
        } catch {
            handleError(localized("home_projects_file_read_failed", fallback: "Failed to read the selected file."))
        }
    }

    private func routeImportedFile(path storedPath: String, ext: String) {
        switch ext {
        case "ame", "approj":
            openIndexedFile(path: storedPath)

        case "als":
            pendingAbletonPath = storedPath
            activeSheet = .abletonWizard(path: storedPath)

        case "zip":
            startLoading(localized("home_projects_loading_project_msg", fallback: "Loading Project"))
            detectZipAndRoute(storedPath: storedPath)

        default:
            break
        }
    }

    func openIndexedFile(path: String) {
        startLoading(localized("home_projects_loading_project_msg", fallback: "Loading Project"))
        HomeSwiftBridge.shared.openWorkspaceFromPath(
            path: path,
            onSuccess: { [weak self] in
                Task { @MainActor [weak self] in
                    self?.handleWorkspaceOpened()
                    self?.loadRecents()
                }
            },
            onError: { [weak self] msg in Task { @MainActor [weak self] in self?.handleError(msg) } }
        )
    }

    // ── Ableton import ─────────────────────────────────────────────────────

    func importAbleton(path: String, palettePath: String?, apolloPath: String?) {
        startLoading(localized("home_import_wizard_translating_message", fallback: "Translating your Ableton Live-Set"))
        activeSheet = nil
        HomeSwiftBridge.shared.importAbletonProject(
            path: path,
            palettePath: palettePath,
            apolloPath: apolloPath,
            onSuccess: { [weak self] in
                Task { @MainActor [weak self] in
                    self?.handleWorkspaceOpened()
                    self?.loadRecents()
                }
            },
            onError: { [weak self] msg in Task { @MainActor [weak self] in self?.handleError(msg) } }
        )
    }

    // ── Workspace lifecycle ────────────────────────────────────────────────

    func workspaceClosed() {
        isWorkspaceOpen = false
        loadRecents()
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    func localAuthor() -> String {
        HomeSwiftBridge.shared.localAuthor()
    }

    private func detectZipAndRoute(storedPath: String) {
        HomeSwiftBridge.shared.getZipFormat(path: storedPath) { [weak self] format in
            Task { @MainActor [weak self] in
                guard let self else { return }
                switch format {
                case "ABLETON":
                    self.isLoading = false
                    self.pendingAbletonPath = storedPath
                    self.activeSheet = .abletonWizard(path: storedPath)
                    self.openNextHubProject()
                case "ABLETON_APOLLO", "UNIPAD":
                    self.openIndexedFile(path: storedPath)
                default:
                    self.isLoading = false
                    self.handleError(self.localized(
                        "home_error_unsupported_project_format",
                        fallback: "Unsupported project file format: .%1$s"
                    ).replacingOccurrences(of: ".%1$s", with: "ZIP archive"))
                }
            }
        }
    }

    private func startLoading(_ text: String) {
        loadingProgress = 0.0
        loadingTitle = nil
        loadingStatusText = text
        loadingDetailText = nil
        isLoading   = true
    }

    private func localized(_ key: String, fallback: String) -> String {
        IosLocalizationBridge.shared.string(key: key, fallback: fallback)
    }

    private func handleWorkspaceOpened() {
        isLoading       = false
        isWorkspaceOpen = true
        openNextHubProject()
    }

    private func handleError(_ message: String) {
        isLoading     = false
        errorMessage  = message
        openNextHubProject()
    }
}
