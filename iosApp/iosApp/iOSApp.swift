import SwiftUI
import ComposeApp

#if DEBUG
@MainActor
private enum ProjectBenchmarkGate {
    static var started = false
}
#endif

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        return .portrait
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    init() {
        SentrySetupKt.initializeSentry()

        let theme = AmethystTheme(darkMode: true)
        let unselectedColor = UIColor(theme.tabBarMutedForeground)
        let itemAppearance = UITabBarItemAppearance()
        itemAppearance.normal.iconColor = unselectedColor
        itemAppearance.normal.titleTextAttributes = [.foregroundColor: unselectedColor]

        let appearance = UITabBarAppearance()
        appearance.configureWithDefaultBackground()
        appearance.backgroundColor = UIColor(theme.glassSurface)
        appearance.stackedLayoutAppearance = itemAppearance
        appearance.inlineLayoutAppearance = itemAppearance
        appearance.compactInlineLayoutAppearance = itemAppearance

        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
        UITabBar.appearance().unselectedItemTintColor = unselectedColor
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.dark)
#if DEBUG
                .task {
                    let arguments = ProcessInfo.processInfo.arguments
                    guard let index = arguments.firstIndex(of: "--benchmark-project"),
                          arguments.indices.contains(index + 1),
                          !ProjectBenchmarkGate.started else { return }
                    ProjectBenchmarkGate.started = true
                    let started = CFAbsoluteTimeGetCurrent()
                    HomeSwiftBridge.shared.openWorkspaceFromPath(
                        path: arguments[index + 1],
                        onSuccess: {
                            print("ProjectBenchmark result=success durationMs=\(Int((CFAbsoluteTimeGetCurrent() - started) * 1000))")
                            fflush(stdout)
                        },
                        onError: {
                            print("ProjectBenchmark result=error message=\($0) durationMs=\(Int((CFAbsoluteTimeGetCurrent() - started) * 1000))")
                            fflush(stdout)
                        }
                    )
                }
#endif
        }
    }
}
