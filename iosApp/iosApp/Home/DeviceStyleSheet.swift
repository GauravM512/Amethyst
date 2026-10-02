import SwiftUI
import UIKit
import ComposeApp

private struct DeviceStylePreview: UIViewControllerRepresentable {
    let styleId: String
    let darkMode: Bool

    func makeUIViewController(context: Context) -> UIViewController {
        let controller = IosDeviceStyleKt.iosDeviceStylePreviewViewController(
            styleId: styleId,
            darkMode: darkMode
        )
        controller.view.isUserInteractionEnabled = false
        controller.view.isOpaque = false
        controller.view.backgroundColor = .clear
        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct DeviceStyleSheet: View {
    let uuid: String
    let darkMode: Bool
    let onDismiss: () -> Void

    @Environment(AppLocalization.self) private var localization
    @State private var options: [IosDeviceStyleOption] = []
    @State private var selectedId: String?

    init(uuid: String, darkMode: Bool, onDismiss: @escaping () -> Void) {
        self.uuid = uuid
        self.darkMode = darkMode
        self.onDismiss = onDismiss
        _options = State(initialValue: IosDeviceStyleKt.iosDeviceStyleOptions(uuid: uuid))
        _selectedId = State(initialValue: IosDeviceStyleKt.iosSelectedDeviceStyle(uuid: uuid))
    }

    private var theme: AmethystTheme {
        AmethystTheme(darkMode: darkMode)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVGrid(
                    columns: [GridItem(.adaptive(minimum: 144), spacing: 16)],
                    spacing: 16
                ) {
                    ForEach(options, id: \.id) { option in
                        styleChoice(option)
                    }
                }
                .padding(20)
            }
            .background(theme.background.ignoresSafeArea())
            .navigationTitle(
                localization.string(
                    "workspace_viewport_launchpad_actions_style_dialog_title_ios",
                    fallback: "Style"
                )
            )
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    if #available(iOS 26.0, *) {
                        Button(role: .close, action: onDismiss)
                    } else {
                        Button(action: onDismiss) {
                            Image(systemName: "xmark")
                        }
                        .accessibilityLabel(localization.string("ui_primitive_sheet_close", fallback: "Close"))
                    }
                }
            }
            .task(id: uuid) {
                options = IosDeviceStyleKt.iosDeviceStyleOptions(uuid: uuid)
                while !Task.isCancelled {
                    guard let styleId = IosDeviceStyleKt.iosSelectedDeviceStyle(uuid: uuid) else {
                        onDismiss()
                        return
                    }
                    selectedId = styleId
                    do {
                        try await Task.sleep(for: .seconds(1))
                    } catch {
                        return
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
        .presentationBackground(theme.background)
    }

    private func styleChoice(_ option: IosDeviceStyleOption) -> some View {
        let selected = selectedId == option.id

        return Button {
            if IosDeviceStyleKt.iosSelectDeviceStyle(uuid: uuid, styleId: option.id) {
                selectedId = option.id
            } else {
                onDismiss()
            }
        } label: {
            VStack(spacing: 12) {
                DeviceStylePreview(styleId: option.id, darkMode: darkMode)
                    .aspectRatio(1, contentMode: .fit)
                    .accessibilityHidden(true)

                HStack(spacing: 8) {
                    Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                        .foregroundStyle(selected ? theme.glassForeground : theme.mutedForeground)
                        .accessibilityHidden(true)
                    Text(option.name)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(theme.foreground)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(12)
            .frame(maxWidth: .infinity)
            .background(selected ? theme.secondary : theme.card, in: RoundedRectangle(cornerRadius: 16))
            .overlay {
                RoundedRectangle(cornerRadius: 16)
                    .strokeBorder(selected ? theme.glassForeground : theme.border, lineWidth: selected ? 2 : 1)
            }
            .contentShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(option.name)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}
