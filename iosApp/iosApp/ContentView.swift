import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
            .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
            .onAppear {
                UIApplication.shared.isIdleTimerDisabled = true
            }
            .onChange(of: scenePhase) { phase in
                if phase == .active {
                    UIApplication.shared.isIdleTimerDisabled = true
                }
            }
    }
}



