//
//  ContentView.swift
//  iosApp
//
//  Created by Anthony Hofmeister on 30.10.25.
//  Copyright © 2025 Anthony Hofmeister. All rights reserved.
//

import UIKit
import SwiftUI
import ComposeApp

// MARK: - Workspace host (KMP Compose)

private class OrientationContainerViewController: UIViewController {
    let childViewController: UIViewController
    var forcedLandscape: Bool = false {
        didSet {
            if oldValue != forcedLandscape {
                setNeedsUpdateOfSupportedInterfaceOrientations()
                if #available(iOS 16.0, *) {
                    if let windowScene = self.view.window?.windowScene {
                        let orientations: UIInterfaceOrientationMask = forcedLandscape ? .landscape : .all
                        let preferences = UIWindowScene.GeometryPreferences.iOS(interfaceOrientations: orientations)
                        windowScene.requestGeometryUpdate(preferences) { error in
                            print("Geometry update failed: \(error)")
                        }
                    }
                } else {
                    let value = forcedLandscape ? UIInterfaceOrientation.landscapeLeft.rawValue : UIInterfaceOrientation.unknown.rawValue
                    UIDevice.current.setValue(value, forKey: "orientation")
                    UIViewController.attemptRotationToDeviceOrientation()
                }
            }
        }
    }

    init(child: UIViewController) {
        self.childViewController = child
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        addChild(childViewController)
        childViewController.view.frame = view.bounds
        childViewController.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(childViewController.view)
        childViewController.didMove(toParent: self)
    }

    override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return forcedLandscape ? .landscape : .all
    }

    override var preferredInterfaceOrientationForPresentation: UIInterfaceOrientation {
        return forcedLandscape ? .landscapeLeft : .portrait
    }
}

private struct WorkspaceView: UIViewControllerRepresentable {
    let darkMode: Bool
    let onBack: () -> Void

    func makeUIViewController(context: Context) -> OrientationContainerViewController {
        let child = MainViewControllerKt.WorkspaceViewController(darkMode: darkMode, onBack: onBack)
        let container = OrientationContainerViewController(child: child)
        
        IosWorkspaceBridge.shared.onOrientationChanged = { [weak container] landscape in
            DispatchQueue.main.async {
                container?.forcedLandscape = landscape.boolValue
            }
        }
        
        return container
    }

    func updateUIViewController(_ uiViewController: OrientationContainerViewController, context: Context) {}
}

// MARK: - Root content

struct ContentView: View {
    private enum HomeTab: Hashable {
        case projects
        case browser
        case arcade
        case profile
    }

    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.colorScheme)  private var colorScheme

    @State private var viewModel = HomeViewModel()
    @State private var settingsViewModel = SettingsViewModel()
    @State private var accountViewModel: AccountViewModel
    @State private var hubFeedViewModel: HubFeedViewModel
    @State private var hubSearchViewModel: HubSearchViewModel
    @State private var localization = AppLocalization()
    @State private var profileTabAvatar: UIImage?
    @State private var showSettingsSheet = false
    @State private var showSplashScreen = true
    @State private var selectedHomeTab: HomeTab = .projects
    @State private var hubSearchText = ""

    init() {
        let accountViewModel = AccountViewModel()
        _accountViewModel = State(initialValue: accountViewModel)
        _hubFeedViewModel = State(
            initialValue: HubFeedViewModel(repository: accountViewModel.repository)
        )
        _hubSearchViewModel = State(
            initialValue: HubSearchViewModel(repository: accountViewModel.repository)
        )
    }

    private var theme: AmethystTheme {
        AmethystTheme(darkMode: colorScheme == .dark)
    }

    var body: some View {
        ZStack {
            Group {
                if viewModel.isWorkspaceOpen {
                    WorkspaceView(darkMode: colorScheme == .dark) {
                        viewModel.workspaceClosed()
                    }
                    .ignoresSafeArea()
                    .onAppear {
                        IosWorkspaceBridge.shared.onShowSettings = {
                            showSettingsSheet = true
                        }
                        IosWorkspaceBridge.shared.createLiquidGlassEffect = {
                            if #available(iOS 26.0, *) {
                                let effect = UIGlassEffect(style: .regular)
                                effect.isInteractive = true
                                return effect
                            } else {
                                return UIBlurEffect(style: .systemThinMaterial)
                            }
                        }
                        if #available(iOS 26.0, *) {
                            IosWorkspaceBridge.shared.createLiquidGlassContainerEffect = {
                                let effect = UIGlassContainerEffect()
                                effect.spacing = 10
                                return effect
                            }
                        } else {
                            IosWorkspaceBridge.shared.createLiquidGlassContainerEffect = nil
                        }
                        IosWorkspaceBridge.shared.createLiquidGlassButtonConfiguration = {
                            if #available(iOS 26.0, *) {
                                var configuration = UIButton.Configuration.glass()
                                configuration.cornerStyle = .capsule
                                configuration.indicator = .none
                                return configuration._bridgeToObjectiveC()
                            } else {
                                var configuration = UIButton.Configuration.bordered()
                                configuration.cornerStyle = .capsule
                                return configuration._bridgeToObjectiveC()
                            }
                        }
                    }
                    .onDisappear {
                        IosWorkspaceBridge.shared.onOrientationChanged = nil
                        IosWorkspaceBridge.shared.onShowSettings = nil
                    }
                    .sheet(isPresented: $showSettingsSheet) {
                        SettingsTabView(
                            viewModel: settingsViewModel,
                            accountViewModel: accountViewModel,
                            showsCloseButton: true
                        )
                    }
                } else {
                    homeTabView
                }
            }

            if viewModel.isLoading {
                LoadingScreenView(
                    progress: viewModel.loadingProgress,
                    title: viewModel.loadingTitle,
                    statusText: viewModel.loadingStatusText,
                    detailText: viewModel.loadingDetailText
                )
                .ignoresSafeArea()
                .transition(.opacity)
            }

            if showSplashScreen {
                SplashScreenView {
                    showSplashScreen = false
                }
                .ignoresSafeArea()
                .transition(.opacity)
            }
        }
        .environment(localization)
        .environment(\.locale, Locale(identifier: localization.languageTag))
        .animation(.easeInOut(duration: 0.25), value: viewModel.isLoading)
        .onAppear {
            UIApplication.shared.isIdleTimerDisabled = true
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                UIApplication.shared.isIdleTimerDisabled = true
            }
        }
        .onOpenURL { url in
            handleIncomingURL(url)
        }
    }

    private func handleIncomingURL(_ url: URL) {
        if url.scheme?.caseInsensitiveCompare("amethyst") == .orderedSame {
            print("Received amethyst deep link: \(url.absoluteString)")
            return
        }

        viewModel.openFile(url: url)
    }

    // MARK: - Home tab bar

    private var homeTabView: some View {
        TabView(selection: $selectedHomeTab) {
            ProjectsTabView(
                viewModel: viewModel,
                repository: accountViewModel.repository,
                onShowProfile: { selectedHomeTab = .profile }
            )
                .tag(HomeTab.projects)
                .tabItem {
                    Label(localization.string("home_nav_tab_projects", fallback: "Projects"), systemImage: "folder")
                }

            HubTabView(
                viewModel: hubFeedViewModel,
                searchViewModel: hubSearchViewModel,
                searchText: $hubSearchText,
                sessionRevision: accountViewModel.sessionRevision,
                onShowProfile: { selectedHomeTab = .profile },
                onOpenDownloadedFile: { url, projectID, title in
                    viewModel.openDownloadedFile(url: url, projectID: projectID, title: title)
                }
            )
            .tag(HomeTab.browser)
            .tabItem {
                Label(localization.string("home_nav_tab_browser", fallback: "Hub"), systemImage: "globe")
            }

            NavigationStack {
                ZStack {
                    theme.background.ignoresSafeArea()
                    VStack(spacing: 12) {
                        Image(systemName: "gamecontroller")
                            .font(.largeTitle)
                            .foregroundStyle(theme.mutedForeground)
                        Text(localization.string("home_arcade_wip", fallback: "Work in Progress"))
                            .font(.headline)
                            .foregroundStyle(theme.foreground)
                        Text(localization.string("home_arcade_empty", fallback: "Nothing to see here yet."))
                            .font(.subheadline)
                            .foregroundStyle(theme.mutedForeground)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .navigationTitle(localization.string("home_arcade_title", fallback: "Arcade"))
                }
            }
            .tint(theme.glassForeground)
            .tag(HomeTab.arcade)
            .tabItem {
                Label(localization.string("home_nav_tab_arcade", fallback: "Arcade"), systemImage: "gamecontroller")
            }

            SettingsTabView(
                viewModel: settingsViewModel,
                accountViewModel: accountViewModel
            )
                .tag(HomeTab.profile)
                .tabItem {
                    Label {
                        Text(localization.string("profile_title", fallback: "Profile"))
                    } icon: {
                        if let profileTabAvatar {
                            Image(uiImage: profileTabAvatar)
                                .renderingMode(.original)
                        } else {
                            Image(systemName: "person.crop.circle")
                        }
                    }
                }
        }
        .tint(theme.primary)
        .toolbarBackground(theme.glassSurface, for: .tabBar)
        .toolbarBackground(.visible, for: .tabBar)
        .amethystThemed()
        .task(id: accountViewModel.resolvedAvatarURL) {
            await loadProfileTabAvatar()
        }
    }

    @MainActor
    private func loadProfileTabAvatar() async {
        guard let avatarURL = accountViewModel.resolvedAvatarURL else {
            profileTabAvatar = nil
            return
        }

        do {
            let (data, _) = try await URLSession.shared.data(from: avatarURL)
            guard !Task.isCancelled, let image = UIImage(data: data) else { return }
            profileTabAvatar = image.circularTabBarIcon()
        } catch {
            guard !Task.isCancelled else { return }
            profileTabAvatar = nil
        }
    }
}

private extension UIImage {
    func circularTabBarIcon(diameter: CGFloat = 26) -> UIImage {
        let size = CGSize(width: diameter, height: diameter)
        let renderer = UIGraphicsImageRenderer(size: size)

        return renderer.image { _ in
            let bounds = CGRect(origin: .zero, size: size)
            UIBezierPath(ovalIn: bounds).addClip()

            let scale = max(diameter / self.size.width, diameter / self.size.height)
            let drawSize = CGSize(width: self.size.width * scale, height: self.size.height * scale)
            let drawRect = CGRect(
                x: (diameter - drawSize.width) / 2,
                y: (diameter - drawSize.height) / 2,
                width: drawSize.width,
                height: drawSize.height
            )
            draw(in: drawRect)
        }
        .withRenderingMode(.alwaysOriginal)
    }
}
