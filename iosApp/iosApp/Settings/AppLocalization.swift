//
//  AppLocalization.swift
//  iosApp
//
//  Keeps native SwiftUI screens on the same locale and string resources as Compose.
//

import Foundation
import ComposeApp

@Observable
@MainActor
final class AppLocalization {
    private(set) var languageTag: String

    init() {
        let languageTag = IosLocalizationBridge.shared.languageTag
        self.languageTag = languageTag
        IosLocalizationBridge.shared.activateLanguage(languageTag: languageTag)
    }

    func string(_ key: String?, fallback: String) -> String {
        _ = languageTag
        guard let key, !key.isEmpty else { return fallback }
        return IosLocalizationBridge.shared.string(key: key, fallback: fallback)
    }

    func languageDidChange(to languageTag: String) {
        IosLocalizationBridge.shared.activateLanguage(languageTag: languageTag)
        self.languageTag = languageTag
    }
}
