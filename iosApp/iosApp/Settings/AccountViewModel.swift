//
//  AccountViewModel.swift
//  iosApp
//
//  Copyright © 2026 Anthony Hofmeister. All rights reserved.
//

import Foundation
import ComposeApp
import CommonCrypto

@Observable
@MainActor
final class AccountViewModel {
    enum AuthMode: String, CaseIterable {
        case signIn
        case register

        var title: String {
            switch self {
            case .signIn: return "Sign in"
            case .register: return "Create account"
            }
        }
    }

    var account: HubAccount?
    var authMode: AuthMode = .signIn
    var username = ""
    var displayName = ""
    var email = ""
    var password = ""
    var confirmation = ""
    var mfaCode = ""
    var mfaChallenge: String?
    var isBusy = false
    var errorMessage: String?
    var successMessage: String?
    private(set) var sessionRevision = 0

    let repository: HubRepository
    private let defaults = UserDefaults.standard

    private enum Keys {
        static let accessToken = "amethyst.hub.accessToken"
        static let refreshToken = "amethyst.hub.refreshToken"
    }

    init() {
        let sessionDefaults = UserDefaults.standard
        let accessToken = sessionDefaults.string(forKey: Keys.accessToken)
        let refreshToken = sessionDefaults.string(forKey: Keys.refreshToken)
        repository = HubRepository(
            baseUrl: HubApiClient.companion.DEFAULT_BASE_URL,
            bearerToken: accessToken,
            refreshToken: refreshToken,
            onSessionChanged: { tokens in
                if let tokens {
                    sessionDefaults.set(tokens.accessToken, forKey: Keys.accessToken)
                    sessionDefaults.set(tokens.refreshToken, forKey: Keys.refreshToken)
                } else {
                    sessionDefaults.removeObject(forKey: Keys.accessToken)
                    sessionDefaults.removeObject(forKey: Keys.refreshToken)
                }
            }
        )

        if repository.client.isAuthenticated {
            refreshAccount()
        }
    }

    var isSignedIn: Bool { account != nil }

    var resolvedAvatarURL: URL? {
        guard let avatarPath = account?.avatarUrl, !avatarPath.isEmpty else { return nil }
        if avatarPath.hasPrefix("http://") || avatarPath.hasPrefix("https://") {
            return URL(string: avatarPath)
        }
        let separator = avatarPath.hasPrefix("/") ? "" : "/"
        return URL(string: HubApiClient.companion.DEFAULT_BASE_URL + separator + avatarPath)
    }

    private var normalizedUsername: String {
        username
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "@", with: "")
    }

    func prepareForAuthentication() {
        clearMessages()
        mfaChallenge = nil
        mfaCode = ""
    }

    func refreshAccount() {
        guard repository.client.isAuthenticated else { return }

        repository.getAccount.execute { [weak self] account, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let account {
                    self.account = account
                    self.username = account.username
                    self.displayName = account.displayName
                    self.email = account.email ?? ""
                } else if error != nil {
                    self.signOutLocally()
                }
            }
        }
    }

    func submitAuth() {
        clearMessages()
        let cleanUsername = normalizedUsername
        guard !cleanUsername.isEmpty else {
            errorMessage = sharedString("account_error_username_required", fallback: "Please enter a username.")
            return
        }
        guard !password.isEmpty else {
            errorMessage = sharedString("account_error_password_required", fallback: "Please enter your password.")
            return
        }
        if authMode == .register && password != confirmation {
            errorMessage = sharedString("account_error_password_mismatch", fallback: "The passwords do not match.")
            return
        }

        isBusy = true
        let plainPassword = password
        let selectedMode = authMode
        let registrationDisplayName = displayName
        let registrationEmail = email

        Task { [weak self] in
            do {
                let passwordHash = try await PasswordPrehasher.hash(plainPassword, username: cleanUsername)
                guard let self else { return }
                if selectedMode == .register {
                    self.register(
                        username: cleanUsername,
                        passwordHash: passwordHash,
                        displayName: registrationDisplayName,
                        email: registrationEmail
                    )
                } else {
                    self.login(
                        username: cleanUsername,
                        passwordHash: passwordHash,
                        legacyPassword: plainPassword
                    )
                }
            } catch {
                self?.finish(errorMessage: error.localizedDescription)
            }
        }
    }

    private func register(username: String, passwordHash: String, displayName: String, email: String) {
        let input = HubRegisterInput(
            username: username,
            password: passwordHash,
            displayName: displayName,
            email: email
        )
        repository.register_.execute(input: input) { [weak self] _, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let error {
                    self.finish(errorMessage: error.localizedDescription)
                    return
                }
                self.login(username: username, passwordHash: passwordHash, legacyPassword: nil)
            }
        }
    }

    private func login(username: String, passwordHash: String, legacyPassword: String?) {
        performLogin(
            username: username,
            password: passwordHash,
            legacyFallback: legacyPassword
        )
    }

    private func performLogin(username: String, password: String, legacyFallback: String?) {
        let input = HubLoginInput(
            username: username,
            password: password,
            remember: true
        )
        repository.login.execute(input: input) { [weak self] result, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let error, let legacyFallback, self.isInvalidCredentials(error) {
                    self.performLogin(
                        username: username,
                        password: legacyFallback,
                        legacyFallback: nil
                    )
                    return
                }
                self.handleAuthResult(result, error: error)
            }
        }
    }

    func submitMFA() {
        clearMessages()
        guard let challenge = mfaChallenge, !mfaCode.isEmpty else { return }
        isBusy = true
        repository.completeMfa.execute(
            input: HubMfaInput(challenge: challenge, code: mfaCode)
        ) { [weak self] result, error in
            Task { @MainActor [weak self] in
                self?.handleAuthResult(result, error: error)
            }
        }
    }

    func requestPasswordReset() {
        clearMessages()
        let identifier = username.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: "@", with: "")
        guard !identifier.isEmpty else {
            errorMessage = sharedString("account_reset_username_first", fallback: "Enter your username first.")
            return
        }
        isBusy = true
        repository.requestPasswordReset.execute(username: identifier) { [weak self] _, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.isBusy = false
                if let error {
                    self.errorMessage = error.localizedDescription
                } else {
                    self.successMessage = sharedString("account_reset_sent", fallback: "If recovery is available, instructions will be sent shortly.")
                }
            }
        }
    }

    private func handleAuthResult(_ result: HubAuthResult?, error: Error?) {
        if let error {
            finish(errorMessage: error.localizedDescription)
            return
        }
        guard let result else {
            finish(errorMessage: sharedString("account_error_missing_login_result", fallback: "The server returned no login result."))
            return
        }
        if let challenge = result.challenge {
            mfaChallenge = challenge
            mfaCode = ""
            isBusy = false
            return
        }
        password = ""
        confirmation = ""
        mfaChallenge = nil
        mfaCode = ""
        isBusy = false
        if let account = result.account {
            setAccount(account)
        } else {
            refreshAccount()
        }
    }

    func updateProfile(displayName: String, bio: String, completion: @escaping (Error?) -> Void) {
        guard !isBusy else { return }
        isBusy = true
        repository.updateArtistProfile.execute(
            input: HubArtistProfileInput(displayName: displayName, bio: bio)
        ) { [weak self] account, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.isBusy = false
                if let account { self.setAccount(account) }
                completion(error)
            }
        }
    }

    func updateAvatar(data: Data, mimeType: String) {
        guard !isBusy else { return }
        isBusy = true
        let input = HubAvatarInput(
            data: data.base64EncodedString(),
            mimeType: mimeType,
            avatarUrl: ""
        )
        repository.setAccountAvatar.execute(input: input) { [weak self] account, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.isBusy = false
                if let account {
                    self.setAccount(account)
                } else if let error {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }

    func changePassword(current: String, newPassword: String, confirmation: String, completion: @escaping (Error?) -> Void) {
        guard newPassword == confirmation else {
            completion(AccountViewModelError.passwordMismatch)
            return
        }
        guard !isBusy else { return }
        isBusy = true

        let accountUsername = account?.username ?? normalizedUsername
        Task { [weak self] in
            do {
                let currentHash = try await PasswordPrehasher.hash(current, username: accountUsername)
                let newHash = try await PasswordPrehasher.hash(newPassword, username: accountUsername)
                guard let self else { return }
                self.performPasswordChange(
                    currentCredential: currentHash,
                    newPasswordHash: newHash,
                    legacyFallback: current,
                    completion: completion
                )
            } catch {
                self?.isBusy = false
                completion(error)
            }
        }
    }

    func changeEmail(password: String, newEmail: String, code: String, completion: @escaping (Error?) -> Void) {
        guard !isBusy else { return }
        isBusy = true

        let accountUsername = account?.username ?? normalizedUsername
        Task { [weak self] in
            do {
                let passwordHash = try await PasswordPrehasher.hash(password, username: accountUsername)
                guard let self else { return }
                self.performEmailChange(
                    passwordCredential: passwordHash,
                    newEmail: newEmail,
                    code: code,
                    legacyFallback: password,
                    completion: completion
                )
            } catch {
                self?.isBusy = false
                completion(error)
            }
        }
    }

    func removeEmail(password: String, code: String, completion: @escaping (Error?) -> Void) {
        guard !isBusy else { return }
        isBusy = true

        let accountUsername = account?.username ?? normalizedUsername
        Task { [weak self] in
            do {
                let passwordHash = try await PasswordPrehasher.hash(password, username: accountUsername)
                guard let self else { return }
                self.performEmailRemoval(
                    passwordCredential: passwordHash,
                    code: code,
                    legacyFallback: password,
                    completion: completion
                )
            } catch {
                self?.isBusy = false
                completion(error)
            }
        }
    }

    private func performPasswordChange(
        currentCredential: String,
        newPasswordHash: String,
        legacyFallback: String?,
        completion: @escaping (Error?) -> Void
    ) {
        repository.changePassword.execute(
            input: HubPasswordChangeInput(password: currentCredential, newPassword: newPasswordHash, code: "")
        ) { [weak self] _, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let error, let legacyFallback, self.isInvalidCredentials(error) {
                    self.performPasswordChange(
                        currentCredential: legacyFallback,
                        newPasswordHash: newPasswordHash,
                        legacyFallback: nil,
                        completion: completion
                    )
                    return
                }
                self.isBusy = false
                completion(error)
            }
        }
    }

    private func performEmailChange(
        passwordCredential: String,
        newEmail: String,
        code: String,
        legacyFallback: String?,
        completion: @escaping (Error?) -> Void
    ) {
        repository.changeEmail.execute(
            input: HubEmailChangeInput(password: passwordCredential, email: newEmail, code: code)
        ) { [weak self] _, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let error, let legacyFallback, self.isInvalidCredentials(error) {
                    self.performEmailChange(
                        passwordCredential: legacyFallback,
                        newEmail: newEmail,
                        code: code,
                        legacyFallback: nil,
                        completion: completion
                    )
                    return
                }
                self.isBusy = false
                completion(error)
            }
        }
    }

    private func performEmailRemoval(
        passwordCredential: String,
        code: String,
        legacyFallback: String?,
        completion: @escaping (Error?) -> Void
    ) {
        repository.removeEmail.execute(
            input: HubSensitiveInput(password: passwordCredential, code: code)
        ) { [weak self] _, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let error, let legacyFallback, self.isInvalidCredentials(error) {
                    self.performEmailRemoval(
                        passwordCredential: legacyFallback,
                        code: code,
                        legacyFallback: nil,
                        completion: completion
                    )
                    return
                }
                self.isBusy = false
                if error == nil { self.refreshAccount() }
                completion(error)
            }
        }
    }

    func signOut() {
        guard !isBusy else { return }
        isBusy = true
        repository.logout.execute(sessionId: nil, all: false) { [weak self] _, _ in
            Task { @MainActor [weak self] in
                self?.signOutLocally()
            }
        }
    }

    func loadSessions(completion: @escaping ([HubSession], Error?) -> Void) {
        repository.getSessions.execute { sessions, error in
            Task { @MainActor in
                completion(sessions?.sessions ?? [], error)
            }
        }
    }

    private func setAccount(_ account: HubAccount) {
        let becameSignedIn = self.account == nil
        self.account = account
        username = account.username
        displayName = account.displayName
        email = account.email ?? ""
        clearMessages()
        if becameSignedIn {
            sessionRevision += 1
        }
    }

    private func finish(errorMessage: String) {
        isBusy = false
        self.errorMessage = errorMessage
    }

    private func clearMessages() {
        errorMessage = nil
        successMessage = nil
    }

    private func isInvalidCredentials(_ error: Error) -> Bool {
        let nsError = error as NSError
        if let hubError = nsError.kotlinException as? HubApiException {
            return hubError.errorCode == "invalid_credentials"
        }
        return error.localizedDescription.contains("invalid_credentials")
    }

    private func signOutLocally() {
        let hadSession = account != nil || repository.client.isAuthenticated
        repository.client.clearSession()
        defaults.removeObject(forKey: Keys.accessToken)
        defaults.removeObject(forKey: Keys.refreshToken)
        account = nil
        password = ""
        confirmation = ""
        mfaChallenge = nil
        isBusy = false
        if hadSession {
            sessionRevision += 1
        }
    }
}

enum AccountViewModelError: LocalizedError {
    case passwordMismatch

    var errorDescription: String? {
        switch self {
        case .passwordMismatch:
            return sharedString("account_error_password_mismatch", fallback: "The passwords do not match.")
        }
    }
}

private func sharedString(_ key: String, fallback: String) -> String {
    IosLocalizationBridge.shared.string(key: key, fallback: fallback)
}

private enum PasswordPrehasher {
    private static let prefix = "$prehash$v1$"
    private static let iterations: UInt32 = 120_000
    private static let derivedKeyLength = 32

    static func hash(_ password: String, username: String) async throws -> String {
        try await Task.detached(priority: .userInitiated) {
            try derive(password: password, username: username)
        }.value
    }

    private static func derive(password: String, username: String) throws -> String {
        let passwordData = Data(password.utf8)
        let normalizedUsername = username.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let saltData = Data("\(normalizedUsername):amethyst-prehash-v1".utf8)
        var derivedKey = [UInt8](repeating: 0, count: derivedKeyLength)

        let status = passwordData.withUnsafeBytes { passwordBytes in
            saltData.withUnsafeBytes { saltBytes in
                derivedKey.withUnsafeMutableBytes { derivedKeyBytes in
                    CCKeyDerivationPBKDF(
                        CCPBKDFAlgorithm(kCCPBKDF2),
                        passwordBytes.bindMemory(to: Int8.self).baseAddress,
                        passwordData.count,
                        saltBytes.bindMemory(to: UInt8.self).baseAddress,
                        saltData.count,
                        CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256),
                        iterations,
                        derivedKeyBytes.bindMemory(to: UInt8.self).baseAddress,
                        derivedKeyLength
                    )
                }
            }
        }

        guard status == kCCSuccess else {
            throw PasswordPrehashError.derivationFailed(status)
        }

        let encoded = Data(derivedKey)
            .base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
        return prefix + encoded
    }
}

private enum PasswordPrehashError: LocalizedError {
    case derivationFailed(Int32)

    var errorDescription: String? {
        switch self {
        case .derivationFailed(let status):
            return "Password preparation failed (\(status))."
        }
    }
}
