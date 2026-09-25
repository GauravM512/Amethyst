//
//  SettingsTabView.swift
//  iosApp
//
//  Copyright © 2025 Anthony Hofmeister. All rights reserved.
//

import SwiftUI
import ComposeApp
import PhotosUI

struct SettingsTabView: View {
    @Bindable var viewModel: SettingsViewModel
    @Bindable var accountViewModel: AccountViewModel
    var showsCloseButton = false

    @Environment(\.dismiss) private var dismiss
    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization
    @State private var showsAuthScreen = false

    private var groups: [SettingsGroup] {
        SettingsRepository.shared.settingsGroups
    }

    var body: some View {
        NavigationStack {
            Form {
                ProfileHeaderView(
                    viewModel: accountViewModel,
                    theme: theme,
                    onRequestAuth: {
                        accountViewModel.prepareForAuthentication()
                        showsAuthScreen = true
                    }
                )
                .listRowInsets(EdgeInsets(top: 10, leading: 0, bottom: 14, trailing: 0))
                .listRowBackground(Color.clear)

                if accountViewModel.account != nil {
                    Section(localization.string("account_section_title", fallback: "Account")) {
                        NavigationLink {
                            AccountSecurityView(viewModel: accountViewModel, theme: theme)
                        } label: {
                            Text(localization.string("account_security_title", fallback: "Security"))
                        }

                        NavigationLink {
                            AccountEmailView(viewModel: accountViewModel, theme: theme)
                        } label: {
                            HStack {
                                Text(localization.string("account_email_title", fallback: "Email"))
                                    .lineLimit(1)
                                Spacer()
                                Text(accountViewModel.account?.email ?? localization.string("common_not_added", fallback: "Not set"))
                                    .foregroundStyle(theme.mutedForeground)
                                    .lineLimit(1)
                            }
                        }
                    }
                    .listRowBackground(theme.secondary)
                }

                ForEach(groups, id: \.displayTitle) { group in
                    SettingsGroupSection(group: group, theme: theme)
                }

                if accountViewModel.account != nil {
                    Section {
                        Button(localization.string("account_sign_out", fallback: "Sign out"), role: .destructive) {
                            accountViewModel.signOut()
                        }
                        .disabled(accountViewModel.isBusy)
                    }
                    .listRowBackground(theme.secondary)
                }
            }
            .scrollContentBackground(.hidden)
            .background(theme.background.ignoresSafeArea())
            .navigationTitle(localization.string("profile_title", fallback: "Profile"))
            .fullScreenCover(isPresented: $showsAuthScreen) {
                AuthCardView(viewModel: accountViewModel, theme: theme)
            }
            .refreshable {
                accountViewModel.refreshAccount()
            }
            .toolbar {
                if showsCloseButton {
                    ToolbarItem(placement: .cancellationAction) {
                        Button {
                            dismiss()
                        } label: {
                            Image(systemName: "xmark")
                        }
                        .accessibilityLabel(localization.string("settings_close", fallback: "Close settings"))
                    }
                }
            }
        }
        .onChange(of: accountViewModel.account) { _, account in
            if account != nil { showsAuthScreen = false }
        }
        .tint(theme.glassForeground)
    }
}

private struct SettingsGroupSection: View {
    let group: SettingsGroup
    let theme: AmethystTheme
    @Environment(AppLocalization.self) private var localization

    var body: some View {
        Section(localization.string(group.localizationKey, fallback: group.displayTitle)) {
            ForEach(group.settings, id: \.key) { setting in
                SettingRowView(setting: setting, theme: theme)
            }
        }
    }
}

// MARK: - Profile header

private struct ProfileHeaderView: View {
    @Bindable var viewModel: AccountViewModel
    let theme: AmethystTheme
    let onRequestAuth: () -> Void

    @State private var showsEditProfile = false
    @Environment(AppLocalization.self) private var localization

    var body: some View {
        HStack(alignment: .center, spacing: 16) {
            AvatarView(url: viewModel.account?.avatarUrl, name: viewModel.account?.displayName ?? "Amethyst", size: 92, theme: theme)

            VStack(alignment: .leading, spacing: 5) {
                if let account = viewModel.account {
                    Text(account.displayName.isEmpty ? account.username : account.displayName)
                        .font(.title2.weight(.bold))
                        .foregroundStyle(theme.foreground)
                        .lineLimit(1)
                    Text("@\(account.username)")
                        .font(.subheadline)
                        .foregroundStyle(theme.mutedForeground)
                    if !account.bio.isEmpty {
                        Text(account.bio)
                            .font(.footnote)
                            .foregroundStyle(theme.mutedForeground)
                            .lineLimit(2)
                    }
                } else {
                    Text(localization.string("account_signed_out", fallback: "Signed out"))
                        .font(.title2.weight(.bold))
                        .foregroundStyle(theme.foreground)
                    Button {
                        onRequestAuth()
                    } label: {
                        HStack(spacing: 7) {
                            Image(systemName: "person.badge.plus")
                            Text(localization.string("account_sign_in", fallback: "Sign in"))
                                .lineLimit(1)
                        }
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(theme.primaryForeground)
                        .fixedSize(horizontal: true, vertical: false)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 9)
                        .background(theme.primary, in: Capsule())
                    }
                    .buttonStyle(.plain)
                    .padding(.top, 8)
                }
            }

            Spacer(minLength: 0)

            if viewModel.account != nil {
                Button {
                    showsEditProfile = true
                } label: {
                    Image(systemName: "pencil")
                        .font(.headline)
                        .foregroundStyle(theme.glassForeground)
                        .frame(width: 42, height: 42)
                        .background(theme.muted, in: Circle())
                }
                .accessibilityLabel(localization.string("profile_edit", fallback: "Edit profile"))
            }
        }
        .sheet(isPresented: $showsEditProfile) {
            EditProfileView(viewModel: viewModel, theme: theme)
        }
    }
}

private struct AvatarView: View {
    let url: String?
    let name: String
    let size: CGFloat
    let theme: AmethystTheme
    @Environment(AppLocalization.self) private var localization

    var body: some View {
        ZStack {
            Circle()
                .fill(theme.primary.opacity(0.22))

            if let avatarURL, let url = URL(string: avatarURL) {
                AsyncImage(url: url) { phase in
                    if let image = phase.image {
                        image.resizable().scaledToFill()
                    } else {
                        placeholder
                    }
                }
            } else {
                placeholder
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .accessibilityLabel(localization.string("profile_picture", fallback: "Profile picture"))
    }

    private var avatarURL: String? {
        guard let url, !url.isEmpty else { return nil }
        if url.hasPrefix("http://") || url.hasPrefix("https://") { return url }
        return HubApiClient.companion.DEFAULT_BASE_URL + (url.hasPrefix("/") ? url : "/\(url)")
    }

    private var placeholder: some View {
        Image(systemName: "person.fill")
            .font(.system(size: size * 0.38, weight: .semibold))
            .foregroundStyle(theme.primary)
    }
}

// MARK: - Sign in / register

private struct AuthCardView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(AppLocalization.self) private var localization
    @Bindable var viewModel: AccountViewModel
    let theme: AmethystTheme

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    HStack {
                        Spacer()
                        AmethystLogoMark(size: 84)
                        Spacer()
                    }

                    VStack(spacing: 5) {
                        Text(localization.string("account_welcome_title", fallback: "Welcome to Amethyst"))
                            .font(.title2.weight(.bold))
                            .foregroundStyle(theme.foreground)
                        Text(localization.string("account_sync_description", fallback: "Use your Amethyst account to sync your profile and projects."))
                            .font(.footnote)
                            .multilineTextAlignment(.center)
                            .foregroundStyle(theme.mutedForeground)
                    }
                    .frame(maxWidth: .infinity)

                    Picker(localization.string("account_action", fallback: "Account action"), selection: $viewModel.authMode) {
                        ForEach(AccountViewModel.AuthMode.allCases, id: \.self) { mode in
                            Text(authModeTitle(mode)).tag(mode)
                        }
                    }
                    .pickerStyle(.segmented)

                    if viewModel.mfaChallenge != nil {
                        Text(localization.string("account_two_factor_authentication", fallback: "Two-factor authentication"))
                            .font(.headline)
                            .foregroundStyle(theme.foreground)
                        Text(localization.string("account_mfa_sign_in_description", fallback: "Enter the code from your authenticator app to finish signing in."))
                            .font(.footnote)
                            .foregroundStyle(theme.mutedForeground)
                        AuthTextField(localization.string("account_authentication_code", fallback: "Authentication code"), text: $viewModel.mfaCode, theme: theme)
                            .keyboardType(.numberPad)
                            .textContentType(.oneTimeCode)
                        Button(localization.string("account_verify_continue", fallback: "Verify and continue")) {
                            viewModel.submitMFA()
                        }
                        .authPrimaryButton(theme: theme)
                        .disabled(viewModel.isBusy)
                    } else {
                        AuthTextField(localization.string("account_username", fallback: "Username"), text: $viewModel.username, theme: theme)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()

                        if viewModel.authMode == .register {
                            AuthTextField(localization.string("profile_display_name", fallback: "Display name"), text: $viewModel.displayName, theme: theme)
                            AuthTextField(localization.string("account_email_optional", fallback: "Email (optional)"), text: $viewModel.email, theme: theme)
                                .keyboardType(.emailAddress)
                                .textInputAutocapitalization(.never)
                                .autocorrectionDisabled()
                        }

                        AuthSecureField(localization.string("account_password", fallback: "Password"), text: $viewModel.password, theme: theme)
                            .textContentType(viewModel.authMode == .register ? .newPassword : .password)

                        if viewModel.authMode == .register {
                            AuthSecureField(localization.string("account_repeat_password", fallback: "Repeat password"), text: $viewModel.confirmation, theme: theme)
                                .textContentType(.newPassword)
                        }

                        Button(authModeTitle(viewModel.authMode)) {
                            viewModel.submitAuth()
                        }
                        .authPrimaryButton(theme: theme)
                        .disabled(viewModel.isBusy)

                        if viewModel.authMode == .signIn {
                            Button(localization.string("account_forgot_password", fallback: "Forgot password?")) {
                                viewModel.requestPasswordReset()
                            }
                            .font(.footnote.weight(.medium))
                            .frame(maxWidth: .infinity)
                        }
                    }

                    if let message = viewModel.errorMessage {
                        Text(message)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                    if let message = viewModel.successMessage {
                        Text(message)
                            .font(.footnote)
                            .foregroundStyle(.green)
                    }
                }
                .padding(24)
            }
            .scrollIndicators(.hidden)
            .background(theme.background.ignoresSafeArea())
            .navigationTitle(localization.string("account_section_title", fallback: "Account"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                    }
                }
            }
        }
        .frame(minWidth: 320, idealWidth: 360, minHeight: 500)
        .tint(theme.glassForeground)
    }

    private func authModeTitle(_ mode: AccountViewModel.AuthMode) -> String {
        switch mode {
        case .signIn:
            localization.string("account_sign_in", fallback: "Sign in")
        case .register:
            localization.string("account_create", fallback: "Create account")
        }
    }
}

private struct AmethystLogoMark: View {
    let size: CGFloat

    var body: some View {
        let gradient = LinearGradient(
            colors: [
                Color(red: 0.694, green: 0.0, blue: 0.925),
                Color(red: 0.941, green: 0.380, blue: 0.961),
                Color.red
            ],
            startPoint: .bottomLeading,
            endPoint: .topTrailing
        )

        AmethystFilledLogoShape()
            .fill(gradient)
        .frame(width: size, height: size)
        .shadow(color: themeShadow, radius: 8, y: 3)
        .accessibilityLabel("Amethyst")
    }

    private var themeShadow: Color {
        Color(red: 0.694, green: 0.0, blue: 0.925).opacity(0.28)
    }
}

private struct AuthTextField: View {
    let title: String
    @Binding var text: String
    let theme: AmethystTheme

    init(_ title: String, text: Binding<String>, theme: AmethystTheme) {
        self.title = title
        self._text = text
        self.theme = theme
    }

    var body: some View {
        TextField(title, text: $text)
            .padding(.horizontal, 14)
            .frame(height: 48)
            .background(theme.secondary, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 13, style: .continuous).stroke(theme.border, lineWidth: 1))
    }
}

private struct AuthSecureField: View {
    let title: String
    @Binding var text: String
    let theme: AmethystTheme

    init(_ title: String, text: Binding<String>, theme: AmethystTheme) {
        self.title = title
        self._text = text
        self.theme = theme
    }

    var body: some View {
        SecureField(title, text: $text)
            .padding(.horizontal, 14)
            .frame(height: 48)
            .background(theme.secondary, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 13, style: .continuous).stroke(theme.border, lineWidth: 1))
    }
}

private extension View {
    func authPrimaryButton(theme: AmethystTheme) -> some View {
        frame(maxWidth: .infinity)
            .frame(height: 48)
            .background(theme.primary, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .foregroundStyle(theme.primaryForeground)
    }
}

// MARK: - Account details

private struct EditProfileView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(AppLocalization.self) private var localization
    @Bindable var viewModel: AccountViewModel
    let theme: AmethystTheme

    @State private var displayName: String
    @State private var bio: String
    @State private var avatarItem: PhotosPickerItem?
    @State private var message: String?

    init(viewModel: AccountViewModel, theme: AmethystTheme) {
        self.viewModel = viewModel
        self.theme = theme
        _displayName = State(initialValue: viewModel.account?.displayName ?? "")
        _bio = State(initialValue: viewModel.account?.bio ?? "")
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    HStack {
                        Spacer()
                        if #available(iOS 16.0, *) {
                            AvatarPickerButton(item: $avatarItem, viewModel: viewModel, theme: theme)
                        } else {
                            AvatarView(url: viewModel.account?.avatarUrl, name: displayName, size: 96, theme: theme)
                        }
                        Spacer()
                    }
                }

                Section(localization.string("profile_title", fallback: "Profile")) {
                    TextField(localization.string("profile_display_name", fallback: "Display name"), text: $displayName)
                    TextField(localization.string("profile_bio", fallback: "Bio"), text: $bio, axis: .vertical)
                        .lineLimit(3...6)
                }

                if let message {
                    Section { Text(message).foregroundStyle(theme.mutedForeground) }
                }
            }
            .scrollContentBackground(.hidden)
            .background(theme.background.ignoresSafeArea())
            .navigationTitle(localization.string("profile_edit", fallback: "Edit profile"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(localization.string("common_cancel", fallback: "Cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(localization.string("common_save", fallback: "Save")) {
                        viewModel.updateProfile(displayName: displayName, bio: bio) { error in
                            if let error {
                                message = error.localizedDescription
                            } else {
                                dismiss()
                            }
                        }
                    }
                    .disabled(viewModel.isBusy)
                }
            }
        }
        .tint(theme.glassForeground)
    }
}

@available(iOS 16.0, *)
private struct AvatarPickerButton: View {
    @Binding var item: PhotosPickerItem?
    @Bindable var viewModel: AccountViewModel
    let theme: AmethystTheme

    var body: some View {
        let currentAvatarURL = viewModel.account?.avatarUrl
        let currentDisplayName = viewModel.displayName

        PhotosPicker(selection: $item, matching: .images) {
            ZStack(alignment: .bottomTrailing) {
                AvatarView(url: currentAvatarURL, name: currentDisplayName, size: 96, theme: theme)
                Image(systemName: "camera.fill")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(.white)
                    .padding(9)
                    .background(theme.primary, in: Circle())
                    .overlay(Circle().stroke(theme.background, lineWidth: 3))
            }
        }
        .onChange(of: item) { _, item in
            guard let item else { return }
            Task {
                guard let data = try? await item.loadTransferable(type: Data.self) else { return }
                let mimeType = item.supportedContentTypes.first?.preferredMIMEType ?? "image/jpeg"
                viewModel.updateAvatar(data: data, mimeType: mimeType)
            }
        }
    }
}

private struct AccountSecurityView: View {
    @Bindable var viewModel: AccountViewModel
    let theme: AmethystTheme
    @State private var showsPasswordSheet = false
    @Environment(AppLocalization.self) private var localization

    var body: some View {
        List {
            Section(localization.string("account_password", fallback: "Password")) {
                Button {
                    showsPasswordSheet = true
                } label: {
                    Text(localization.string("account_change_password", fallback: "Change password"))
                }
            }
            .listRowBackground(theme.secondary)

            Section(localization.string("account_two_factor_authentication", fallback: "Two-factor authentication")) {
                LabeledContent(
                    localization.string("account_authenticator_app", fallback: "Authenticator app"),
                    value: viewModel.account?.totpEnabled == true
                        ? localization.string("common_enabled", fallback: "Enabled")
                        : localization.string("common_not_enabled", fallback: "Not enabled")
                )
            }
            .listRowBackground(theme.secondary)

        }
        .scrollContentBackground(.hidden)
        .background(theme.background.ignoresSafeArea())
        .navigationTitle(localization.string("account_security_title", fallback: "Security"))
        .sheet(isPresented: $showsPasswordSheet) {
            PasswordChangeView(viewModel: viewModel, theme: theme)
        }
        .tint(theme.glassForeground)
    }
}

private struct AccountEmailView: View {
    @Bindable var viewModel: AccountViewModel
    let theme: AmethystTheme

    @State private var newEmail: String
    @State private var currentPassword = ""
    @State private var authenticationCode = ""
    @State private var message: String?
    @State private var errorMessage: String?
    @Environment(AppLocalization.self) private var localization

    init(viewModel: AccountViewModel, theme: AmethystTheme) {
        self.viewModel = viewModel
        self.theme = theme
        _newEmail = State(initialValue: viewModel.account?.email ?? "")
    }

    var body: some View {
        Form {
            Section(localization.string("account_current_email", fallback: "Current email")) {
                LabeledContent(
                    localization.string("account_email_title", fallback: "Email"),
                    value: viewModel.account?.email ?? localization.string("common_not_added", fallback: "Not set")
                )
            }
            .listRowBackground(theme.secondary)

            Section(localization.string("account_change_email", fallback: "Change email")) {
                TextField(localization.string("account_new_email_address", fallback: "New email address"), text: $newEmail)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                SecureField(localization.string("account_current_password", fallback: "Current password"), text: $currentPassword)
                if viewModel.account?.totpEnabled == true {
                    TextField(localization.string("account_authenticator_code", fallback: "Authenticator code"), text: $authenticationCode)
                        .keyboardType(.numberPad)
                        .textContentType(.oneTimeCode)
                }
                Button(localization.string("account_update_email", fallback: "Update email")) {
                    updateEmail()
                }
                .disabled(viewModel.isBusy || newEmail.isEmpty || currentPassword.isEmpty)
            }
            .listRowBackground(theme.secondary)

            if viewModel.account?.email != nil {
                Section {
                    Button(localization.string("account_remove_email", fallback: "Remove email"), role: .destructive) {
                        removeEmail()
                    }
                    .disabled(viewModel.isBusy || currentPassword.isEmpty)
                }
                .listRowBackground(theme.secondary)
            }

            if let message {
                Section {
                    Text(message)
                        .foregroundStyle(theme.mutedForeground)
                }
                .listRowBackground(theme.secondary)
            }

            if let errorMessage {
                Section {
                    Text(errorMessage)
                        .foregroundStyle(.red)
                }
                .listRowBackground(theme.secondary)
            }
        }
        .scrollContentBackground(.hidden)
        .background(theme.background.ignoresSafeArea())
        .navigationTitle(localization.string("account_email_title", fallback: "Email"))
        .tint(theme.glassForeground)
    }

    private func updateEmail() {
        message = nil
        errorMessage = nil
        viewModel.changeEmail(
            password: currentPassword,
            newEmail: newEmail,
            code: authenticationCode
        ) { error in
            if let error {
                errorMessage = error.localizedDescription
            } else {
                currentPassword = ""
                authenticationCode = ""
                message = localization.string("account_email_verification_sent", fallback: "A verification link was sent to the new email address.")
            }
        }
    }

    private func removeEmail() {
        message = nil
        errorMessage = nil
        viewModel.removeEmail(password: currentPassword, code: authenticationCode) { error in
            if let error {
                errorMessage = error.localizedDescription
            } else {
                newEmail = ""
                currentPassword = ""
                authenticationCode = ""
                message = localization.string("account_email_removed", fallback: "Email removed.")
            }
        }
    }
}

private struct PasswordChangeView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(AppLocalization.self) private var localization
    @Bindable var viewModel: AccountViewModel
    let theme: AmethystTheme
    @State private var current = ""
    @State private var newPassword = ""
    @State private var confirmation = ""
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                Section(localization.string("account_change_password", fallback: "Change password")) {
                    SecureField(localization.string("account_current_password", fallback: "Current password"), text: $current)
                    SecureField(localization.string("account_new_password", fallback: "New password"), text: $newPassword)
                    SecureField(localization.string("account_repeat_new_password", fallback: "Repeat new password"), text: $confirmation)
                }
                .listRowBackground(theme.secondary)
                if let errorMessage {
                    Section { Text(errorMessage).foregroundStyle(.red) }
                        .listRowBackground(theme.secondary)
                }
            }
            .scrollContentBackground(.hidden)
            .background(theme.background.ignoresSafeArea())
            .navigationTitle(localization.string("account_password", fallback: "Password"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(localization.string("common_cancel", fallback: "Cancel")) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(localization.string("common_save", fallback: "Save")) {
                        viewModel.changePassword(current: current, newPassword: newPassword, confirmation: confirmation) { error in
                            if let error { errorMessage = error.localizedDescription } else { dismiss() }
                        }
                    }
                    .disabled(viewModel.isBusy || current.isEmpty || newPassword.isEmpty)
                }
            }
        }
        .tint(theme.glassForeground)
    }
}

// MARK: - Setting Row Router
struct SettingRowView: View {
    let setting: Setting<AnyObject>
    let theme: AmethystTheme
    @Environment(AppLocalization.self) private var localization

    var body: some View {
        Group {
            // Bridge the Kotlin generic through Any first. Swift otherwise treats
            // Setting<AnyObject> and Setting<KotlinBoolean/Float> as unrelated.
            let bridgedSetting: Any = setting
            if let toggleSetting = bridgedSetting as? SettingToggle {
                SettingToggleRow(setting: toggleSetting, theme: theme)
            } else if let sliderSetting = bridgedSetting as? SettingSlider {
                SettingSliderRow(setting: sliderSetting, theme: theme)
            } else if let selectSetting = bridgedSetting as? SettingSelect<AnyObject> {
                SettingSelectRow(setting: selectSetting, theme: theme)
            } else if let textFieldSetting = bridgedSetting as? SettingTextField {
                SettingTextFieldRow(setting: textFieldSetting, theme: theme)
            } else {
                Text(localization.string(setting.localizationKey, fallback: setting.displayTitle))
                    .foregroundStyle(.secondary)
            }
        }
        .listRowBackground(theme.secondary)
    }
}

// MARK: - Setting Individual Components

struct SettingToggleRow: View {
    let setting: SettingToggle
    let theme: AmethystTheme
    @State private var isOn: Bool
    @Environment(AppLocalization.self) private var localization

    init(setting: SettingToggle, theme: AmethystTheme) {
        self.setting = setting
        self.theme = theme
        _isOn = State(initialValue: setting.value?.boolValue ?? false)
    }

    var body: some View {
        Toggle(localization.string(setting.localizationKey, fallback: setting.displayTitle), isOn: Binding(
            get: { isOn },
            set: { newValue in
                isOn = newValue
                setting.update(value: KotlinBoolean(bool: newValue))
            }
        ))
        .tint(theme.primary)
    }
}

struct SettingSliderRow: View {
    let setting: SettingSlider
    let theme: AmethystTheme
    @State private var value: Float
    @Environment(AppLocalization.self) private var localization

    init(setting: SettingSlider, theme: AmethystTheme) {
        self.setting = setting
        self.theme = theme
        _value = State(initialValue: setting.value?.floatValue ?? 0.0)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(localization.string(setting.localizationKey, fallback: setting.displayTitle))
                .foregroundStyle(theme.foreground)
            HStack(spacing: 12) {
                let rangeStart = (setting.range.start as? KotlinFloat)?.floatValue ?? 0.0
                let rangeEnd = (setting.range.endInclusive as? KotlinFloat)?.floatValue ?? 1.0
                
                Slider(value: Binding(
                    get: { value },
                    set: { newValue in
                        value = newValue
                        setting.update(value: KotlinFloat(value: newValue))
                    }
                ), in: rangeStart...rangeEnd)
                .tint(theme.primary)
                
                Text("\(Int((value * 100).rounded()))%")
                    .frame(minWidth: 48, alignment: .trailing)
                    .lineLimit(1)
                    .foregroundStyle(theme.mutedForeground)
                    .monospacedDigit()
            }
        }
    }
}

struct SettingSelectRow: View {
    let setting: SettingSelect<AnyObject>
    let theme: AmethystTheme
    @State private var selectedIndex: Int
    @Environment(AppLocalization.self) private var localization

    init(setting: SettingSelect<AnyObject>, theme: AmethystTheme) {
        self.setting = setting
        self.theme = theme
        
        let currentValue = setting.value
        let options = setting.options
        var index = 0
        for i in 0..<options.count {
            if (options[i] as? NSObject)?.isEqual(currentValue) == true {
                index = i
                break
            }
        }
        _selectedIndex = State(initialValue: index)
    }

    var body: some View {
        Picker(localization.string(setting.localizationKey, fallback: setting.displayTitle), selection: Binding(
            get: { selectedIndex },
            set: { newIndex in
                selectedIndex = newIndex
                if newIndex >= 0 && newIndex < setting.options.count {
                    let val = setting.options[newIndex] as AnyObject
                    setting.update(value: val)
                    if setting.key == "language", let language = val as? LanguageOption {
                        localization.languageDidChange(to: language.languageTag)
                    }
                }
            }
        )) {
            ForEach(0..<setting.options.count, id: \.self) { idx in
                let opt = setting.options[idx] as AnyObject
                Text(setting.label(opt)).tag(idx)
            }
        }
        .pickerStyle(.menu)
        .tint(theme.foreground)
    }
}

struct SettingTextFieldRow: View {
    let setting: SettingTextField
    let theme: AmethystTheme
    @State private var text: String
    @Environment(AppLocalization.self) private var localization

    init(setting: SettingTextField, theme: AmethystTheme) {
        self.setting = setting
        self.theme = theme
        _text = State(initialValue: setting.value as? String ?? "")
    }

    var body: some View {
        HStack {
            Text(localization.string(setting.localizationKey, fallback: setting.displayTitle))
                .foregroundStyle(theme.foreground)
            Spacer()
            TextField(localization.string(setting.localizationKey, fallback: setting.displayTitle), text: Binding(
                get: { text },
                set: { newValue in
                    text = newValue
                    setting.update(value: newValue as NSString)
                }
            ))
            .textFieldStyle(.roundedBorder)
            .multilineTextAlignment(.trailing)
            .tint(theme.primary)
        }
    }
}
