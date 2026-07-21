import SwiftUI
import RoadtrippinShared

@main
struct RoadtrippinApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    IosAuthBridge.shared.handleDeepLink(url: url.absoluteString)
                }
        }
    }
}
