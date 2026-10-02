//
//  AbletonImportWizardSheet.swift
//  iosApp
//
//  Created by Copilot
//  Copyright © 2025 Anthony Hofmeister. All rights reserved.
//

import SwiftUI
import ComposeApp
import UniformTypeIdentifiers

/// Presented when the user picks an Ableton `.als` or Ableton-format `.zip`.
/// Lets the user optionally attach a custom palette file and an Apollo `.approj`
/// before starting the conversion.
struct AbletonImportWizardSheet: View {
    let importPath: String
    let viewModel: HomeViewModel

    @Environment(\.dismiss) private var dismiss
    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization

    private enum ActivePicker: Identifiable {
        case palette
        case apollo

        var id: Self { self }
    }

    @State private var palettePath: String = ""
    @State private var apolloPath: String = ""
    @State private var activePicker: ActivePicker? = nil

    private var importName: String {
        (importPath as NSString).lastPathComponent
    }
    private var paletteName: String {
        palettePath.isEmpty ? "" : (palettePath as NSString).lastPathComponent
    }
    private var apolloName: String {
        apolloPath.isEmpty ? "" : (apolloPath as NSString).lastPathComponent
    }

    var body: some View {
        NavigationStack {
            Form {
                // Import source (read-only)
                Section(localization.string("home_import_wizard_import_source", fallback: "Import Source")) {
                    LabeledContent(localization.string("common_file", fallback: "File")) {
                        Text(importName)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                            .truncationMode(.middle)
                    }
                }
                .listRowBackground(theme.muted)

                // Custom palette (optional)
                Section {
                    if palettePath.isEmpty {
                        Text(localization.string("home_import_wizard_no_palette", fallback: "No custom palette selected"))
                            .foregroundStyle(.secondary)
                        Text(localization.string("home_import_wizard_palette_desc", fallback: "Optional. The default Novation palette will be used."))
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                    } else {
                        LabeledContent(localization.string("common_file", fallback: "File")) {
                            Text(paletteName)
                                .foregroundStyle(.secondary)
                            .lineLimit(1)
                            .truncationMode(.middle)
                        }
                        Button(localization.string("common_change", fallback: "Change…")) {
                            activePicker = .palette
                        }
                        Button(localization.string("common_remove", fallback: "Remove"), role: .destructive) {
                            palettePath = ""
                        }
                    }

                    if palettePath.isEmpty {
                        Button(localization.string("home_import_wizard_sheet_palette_select", fallback: "Select Palette…")) {
                            activePicker = .palette
                        }
                    }
                } header: {
                    Text(localization.string("home_import_wizard_custom_palette", fallback: "Custom Palette"))
                } footer: {
                    Text(localization.string("home_import_wizard_palette_desc", fallback: "Optional. Overrides the default Novation colour palette."))
                }
                .listRowBackground(theme.muted)

                // Apollo project (optional)
                Section {
                    if apolloPath.isEmpty {
                        Text(localization.string("home_import_wizard_no_apollo", fallback: "No Apollo project selected"))
                            .foregroundStyle(.secondary)
                        Text(localization.string("home_import_wizard_apollo_empty_description", fallback: "Optional. Lights will be sourced from Ableton MIDI tracks."))
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                    } else {
                        LabeledContent(localization.string("common_file", fallback: "File")) {
                            Text(apolloName)
                                .foregroundStyle(.secondary)
                            .lineLimit(1)
                            .truncationMode(.middle)
                        }
                        Button(localization.string("common_change", fallback: "Change…")) {
                            activePicker = .apollo
                        }
                        Button(localization.string("common_remove", fallback: "Remove"), role: .destructive) {
                            apolloPath = ""
                        }
                    }

                    if apolloPath.isEmpty {
                        Button(localization.string("home_import_wizard_sheet_apollo_select", fallback: "Select Apollo Project…")) {
                            activePicker = .apollo
                        }
                    }
                } header: {
                    Text(localization.string("home_import_wizard_apollo_project", fallback: "Apollo Lights Project (.approj)"))
                } footer: {
                    Text(localization.string("home_import_wizard_apollo_selection_description", fallback: "Optional. If set, the lights chain will be taken from the Apollo project instead."))
                }
                .listRowBackground(theme.muted)
            }
            .scrollContentBackground(.hidden)
            .background(theme.background.ignoresSafeArea())
            .navigationTitle(localization.string("home_import_wizard_title", fallback: "Ableton Import Wizard"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(localization.string("home_import_wizard_cancel", fallback: "Cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(localization.string("common_convert", fallback: "Convert")) {
                        startConversion()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
        .tint(theme.glassForeground)
        .fileImporter(
            isPresented: Binding(
                get: { activePicker != nil },
                set: { if !$0 { activePicker = nil } }
            ),
            allowedContentTypes: activePicker == .apollo
                ? [UTType(filenameExtension: "approj") ?? .data, .data, .item]
                : [UTType(filenameExtension: "palette") ?? .data, .item, .data, .plainText]
        ) { result in
            guard case .success(let url) = result else { return }
            let pickedPath = indexPickedFile(url: url)
            if activePicker == .apollo {
                apolloPath = pickedPath
            } else {
                palettePath = pickedPath
            }
        }
    }

    // MARK: - Actions

    private func startConversion() {
        dismiss()
        viewModel.importAbleton(
            path: importPath,
            palettePath: palettePath.isEmpty ? nil : palettePath,
            apolloPath:  apolloPath.isEmpty  ? nil : apolloPath
        )
    }

    private func indexPickedFile(url: URL) -> String {
        guard url.startAccessingSecurityScopedResource() else { return "" }
        defer { url.stopAccessingSecurityScopedResource() }
        guard let data = try? Data(contentsOf: url) else { return "" }
        return HomeSwiftBridge.shared.indexFile(
            data: data,
            filename: url.lastPathComponent
        )
    }
}
