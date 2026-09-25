import SwiftUI
import ComposeApp

struct HubProjectFilters: Equatable {
    var sort = "newest"
    var type = "all"
    var compatibility = "all"
    var difficulty = "all"

    var isActive: Bool {
        sort != "newest" || type != "all" || compatibility != "all" || difficulty != "all"
    }

    var requestKey: String {
        [sort, type, compatibility, difficulty].joined(separator: "|")
    }

    var selectedSort: HubProjectSort {
        switch sort {
        case "popular": .popular
        case "views": .views
        case "title": .title
        default: .newest
        }
    }

    var selectedType: HubProjectType? {
        switch type {
        case "amethyst": .amethyst
        case "ableton": .ableton
        case "apollo": .apollo
        case "unipad": .unipad
        default: nil
        }
    }

    var selectedCompatibility: HubProjectCompatibility? {
        switch compatibility {
        case "compatible": .compatible
        case "partially": .partially
        case "incompatible": .incompatible
        case "unknown": .unknown
        default: nil
        }
    }

    var selectedDifficulty: String? { difficulty == "all" ? nil : difficulty }
}

struct HubProjectFilterBar: View {
    @Binding var filters: HubProjectFilters
    var onChange: () -> Void = {}

    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization

    var body: some View {
        ScrollViewReader { scrollProxy in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    filterChip(
                        title: localization.string("home_hub_catalog_sort", fallback: "Sort"),
                        selection: filters.sort,
                        defaultValue: "newest",
                        options: sortOptions,
                        onSelect: { filters.sort = $0 }
                    )
                    .id("sort")
                    filterChip(
                        title: localization.string("home_hub_detail_format", fallback: "Format"),
                        selection: filters.type,
                        defaultValue: "all",
                        options: typeOptions,
                        onSelect: { filters.type = $0 }
                    )
                    filterChip(
                        title: localization.string("home_hub_detail_compatibility", fallback: "Compatibility"),
                        selection: filters.compatibility,
                        defaultValue: "all",
                        options: compatibilityOptions,
                        onSelect: { filters.compatibility = $0 }
                    )
                    filterChip(
                        title: localization.string("home_hub_detail_difficulty", fallback: "Difficulty"),
                        selection: filters.difficulty,
                        defaultValue: "all",
                        options: difficultyOptions,
                        onSelect: { filters.difficulty = $0 }
                    )

                    if filters.isActive {
                        Button {
                            onChange()
                            filters = HubProjectFilters()
                        } label: {
                            Label(localization.string("home_hub_catalog_reset", fallback: "Reset filters"), systemImage: "xmark")
                                .font(.subheadline.weight(.medium))
                                .padding(.horizontal, 12)
                                .frame(height: 38)
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(theme.onSurfaceVariant)
                    }
                }
                .padding(.vertical, 2)
                .scrollTargetLayout()
            }
            .scrollTargetBehavior(.viewAligned)
            .onChange(of: filters.isActive) { _, isActive in
                if !isActive {
                    scrollProxy.scrollTo("sort", anchor: .leading)
                }
            }
        }
    }

    private func filterChip(
        title: String,
        selection: String,
        defaultValue: String,
        options: [FilterOption],
        onSelect: @escaping (String) -> Void
    ) -> some View {
        let isSelected = selection != defaultValue
        let option = options.first { $0.id == selection }
        let selectedLabel = localization.string(option?.key ?? "", fallback: option?.fallback ?? selection)
        return Menu {
            ForEach(options) { item in
                Button {
                    onChange()
                    onSelect(item.id)
                } label: {
                    if item.id == selection {
                        Label(localization.string(item.key, fallback: item.fallback), systemImage: "checkmark")
                    } else {
                        Text(localization.string(item.key, fallback: item.fallback))
                    }
                }
            }
        } label: {
            HStack(spacing: 6) {
                ZStack(alignment: .leading) {
                    Text(title).hidden()
                    ForEach(options) { item in
                        Text(localization.string(item.key, fallback: item.fallback)).hidden()
                    }
                    Text(isSelected ? selectedLabel : title)
                }
                .fixedSize(horizontal: true, vertical: false)
                Image(systemName: "chevron.down")
                    .font(.caption2.weight(.bold))
            }
            .font(.subheadline.weight(.medium))
            .foregroundStyle(isSelected ? theme.primaryForeground : theme.onSurface)
            .padding(.horizontal, 10)
            .frame(height: 38)
            .background(isSelected ? theme.primary : theme.surfaceContainerHigh, in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(isSelected ? "\(title): \(selectedLabel)" : title)
    }

    private var sortOptions: [FilterOption] { [
        .init("newest", "home_hub_catalog_newest", "Newest"),
        .init("popular", "home_hub_catalog_popular", "Most liked"),
        .init("views", "home_hub_catalog_most_viewed", "Most viewed"),
        .init("title", "home_hub_catalog_title_sort", "Title A–Z")
    ] }

    private var typeOptions: [FilterOption] { [
        .init("all", "home_hub_catalog_all", "All"),
        .init("amethyst", "home_hub_catalog_amethyst", "Amethyst"),
        .init("ableton", "home_hub_catalog_ableton", "Ableton"),
        .init("apollo", "home_hub_catalog_apollo", "Apollo"),
        .init("unipad", "home_hub_catalog_unipad", "UniPad")
    ] }

    private var compatibilityOptions: [FilterOption] { [
        .init("all", "home_hub_catalog_all", "All"),
        .init("compatible", "home_hub_catalog_compatible", "Compatible"),
        .init("partially", "home_hub_catalog_partially", "Partially compatible"),
        .init("incompatible", "home_hub_catalog_incompatible", "Incompatible"),
        .init("unknown", "home_hub_catalog_unknown", "Unknown")
    ] }

    private var difficultyOptions: [FilterOption] { [
        .init("all", "home_hub_catalog_all", "All"),
        .init("1-3", "home_hub_catalog_beginner", "Beginner (1–3)"),
        .init("4-7", "home_hub_catalog_intermediate", "Intermediate (4–7)"),
        .init("8-10", "home_hub_catalog_expert", "Expert (8–10)")
    ] }
}

private struct FilterOption: Identifiable {
    let id: String
    let key: String
    let fallback: String

    init(_ id: String, _ key: String, _ fallback: String) {
        self.id = id
        self.key = key
        self.fallback = fallback
    }
}
