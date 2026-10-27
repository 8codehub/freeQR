import SwiftUI
import QRStudio

@main
struct QRStudioApp: App {
    var body: some Scene {
        WindowGroup { EditorHost().ignoresSafeArea() }
    }
}
private struct EditorHost: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
