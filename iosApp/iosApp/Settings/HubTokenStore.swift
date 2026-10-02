import Foundation
import Security
import ComposeApp

/// The small iOS storage adapter for the shared Hub account service.
final class HubTokenStore: HubSessionStore {
    private let service = "dev.anthonyhfm.amethyst.hub"
    private let account = "session"
    private let legacyAccessKey = "amethyst.hub.accessToken"
    private let legacyRefreshKey = "amethyst.hub.refreshToken"

    private struct Tokens: Codable {
        let access: String
        let refresh: String
    }

    func load() -> HubSessionTokens? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var item: CFTypeRef?
        if SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess,
           let data = item as? Data,
           let tokens = try? JSONDecoder().decode(Tokens.self, from: data) {
            return HubSessionTokens(accessToken: tokens.access, refreshToken: tokens.refresh, expiresIn: 300)
        }

        // Preserve sessions saved by older app versions, then remove the plain UserDefaults copy.
        let defaults = UserDefaults.standard
        guard let access = defaults.string(forKey: legacyAccessKey),
              let refresh = defaults.string(forKey: legacyRefreshKey) else { return nil }
        if persist(access: access, refresh: refresh) {
            defaults.removeObject(forKey: legacyAccessKey)
            defaults.removeObject(forKey: legacyRefreshKey)
        }
        return HubSessionTokens(accessToken: access, refreshToken: refresh, expiresIn: 300)
    }

    func save(tokens: HubSessionTokens?) {
        guard let tokens else {
            clear()
            return
        }
        _ = persist(access: tokens.accessToken, refresh: tokens.refreshToken)
    }

    @discardableResult
    private func persist(access: String, refresh: String) -> Bool {
        guard let data = try? JSONEncoder().encode(Tokens(access: access, refresh: refresh)) else { return false }
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        let update: [String: Any] = [kSecValueData as String: data]
        let status = SecItemUpdate(query as CFDictionary, update as CFDictionary)
        if status == errSecSuccess { return true }
        guard status == errSecItemNotFound else { return false }

        var addition = query
        addition[kSecValueData as String] = data
        addition[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        return SecItemAdd(addition as CFDictionary, nil) == errSecSuccess
    }

    private func clear() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        SecItemDelete(query as CFDictionary)
        let defaults = UserDefaults.standard
        defaults.removeObject(forKey: legacyAccessKey)
        defaults.removeObject(forKey: legacyRefreshKey)
    }
}
