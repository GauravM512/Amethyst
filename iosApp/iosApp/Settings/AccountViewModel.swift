//
//  AccountViewModel.swift
//  iosApp
//
//  Copyright © 2026 Anthony Hofmeister. All rights reserved.
//

import Foundation
import ComposeApp

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
    private let accountService: HubAccountService
    private let tokenStore: HubTokenStore

    init() {
        let store = HubTokenStore()
        tokenStore = store
        repository = HubRepository(
            baseUrl: HubApiClient.companion.DEFAULT_BASE_URL,
            sessionStore: store
        )
        accountService = HubAccountService(repository: repository)

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
                guard let self else { return }
                let result: HubAuthResult
                if selectedMode == .register {
                    result = try await self.accountService.registerAndLogin(
                        username: cleanUsername,
                        password: plainPassword,
                        displayName: registrationDisplayName,
                        email: registrationEmail
                    )
                } else {
                    result = try await self.accountService.login(
                        username: cleanUsername,
                        password: plainPassword
                    )
                }
                self.handleAuthResult(result, error: nil)
            } catch {
                self?.finish(errorMessage: error.localizedDescription)
            }
        }
    }

    func submitMFA() {
        clearMessages()
        guard let challenge = mfaChallenge, !mfaCode.isEmpty else { return }
        isBusy = true
        let code = mfaCode
        Task { [weak self] in
            do {
                guard let self else { return }
                let result = try await self.accountService.completeMfa(challenge: challenge, code: code)
                self.handleAuthResult(result, error: nil)
            } catch {
                self?.finish(errorMessage: error.localizedDescription)
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
                guard let self else { return }
                _ = try await self.accountService.changePassword(
                    username: accountUsername, current: current, replacement: newPassword
                )
                self.isBusy = false
                completion(nil)
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
                guard let self else { return }
                _ = try await self.accountService.changeEmail(
                    username: accountUsername, password: password, email: newEmail, code: code
                )
                self.isBusy = false
                completion(nil)
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
                guard let self else { return }
                _ = try await self.accountService.removeEmail(
                    username: accountUsername, password: password, code: code
                )
                self.isBusy = false
                self.refreshAccount()
                completion(nil)
            } catch {
                self?.isBusy = false
                completion(error)
            }
        }
    }

    func signOut() {
        guard !isBusy else { return }
        isBusy = true
        Task { [weak self] in
            guard let self else { return }
            _ = try? await self.accountService.signOut()
            self.signOutLocally()
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

    private func signOutLocally() {
        let hadSession = account != nil || repository.client.isAuthenticated
        repository.client.clearSession()
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
