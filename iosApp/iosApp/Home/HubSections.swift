//
//  HubSections.swift
//  iosApp
//
//  Created by Anthony Hofmeister on 20.09.26.
//  Copyright © 2026 Anthony Hofmeister. All rights reserved.
//

import SwiftUI

struct HubFeedView: View {
    let sections: [HubFeedSection]
    let availableWidth: CGFloat
    @Bindable var viewModel: HubFeedViewModel
    let onOpen: (HubDestination) -> Void

    @Environment(\.amethystTheme) private var theme

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 34) {
                ForEach(sections) { section in
                    HubSectionRenderer(
                        section: section,
                        availableWidth: availableWidth,
                        viewModel: viewModel,
                        onOpen: onOpen
                    )
                }
            }
            .frame(maxWidth: 960, alignment: .leading)
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 40)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.immediately)
        .background(theme.surface)
    }
}

private struct HubSectionRenderer: View {
    let section: HubFeedSection
    let availableWidth: CGFloat
    @Bindable var viewModel: HubFeedViewModel
    let onOpen: (HubDestination) -> Void

    @ViewBuilder
    var body: some View {
        switch section {
        case .creatorRow(let header, let items):
            HubCreatorRowSection(header: header, items: items, viewModel: viewModel, onOpen: onOpen)

        case .heroCarousel(let header, let items):
            HubHeroCarouselSection(header: header, items: items, availableWidth: availableWidth, onOpen: onOpen)

        case .squareCardRow(let header, let items):
            HubSquareCardRowSection(header: header, items: items, onOpen: onOpen)

        case .mediaCardRow(let header, let items):
            HubMediaCardRowSection(header: header, items: items, onOpen: onOpen)

        case .detailedList(let header, let items):
            HubDetailedListSection(header: header, items: items, onOpen: onOpen)

        case .curatedSpotlight(let header, let spotlight):
            HubSpotlightSection(header: header, spotlight: spotlight, onOpen: onOpen)
        }
    }
}

// MARK: - Shared components

private struct HubSectionHeaderView: View {
    let header: HubSectionHeader

    @Environment(\.amethystTheme) private var theme

    var body: some View {
        Text(header.title)
            .font(.title2.weight(.semibold))
            .foregroundStyle(theme.onSurface)
            .multilineTextAlignment(.leading)
            .padding(.bottom, 4)
    }
}

private struct HubArtworkView: View {
    let artwork: HubArtwork

    @Environment(\.amethystTheme) private var theme

    var body: some View {
        Group {
            if let value = artwork.imageURL, let url = URL(string: value) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    default:
                        placeholder
                    }
                }
            } else {
                placeholder
            }
        }
        .clipped()
        .accessibilityHidden(true)
    }

    private var accent: Color {
        switch artwork.accent {
        case .primary: theme.glassForeground
        case .secondary: theme.onPrimaryContainer
        case .neutral: theme.onSurfaceVariant
        }
    }

    private var placeholder: some View {
        ZStack {
            theme.surfaceContainer

            Circle()
                .fill(accent.opacity(0.13))
                .frame(width: 112, height: 112)
                .offset(x: -34, y: -28)

            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(accent.opacity(0.11))
                .frame(width: 96, height: 74)
                .rotationEffect(.degrees(8))
                .offset(x: 42, y: 42)

            Image(systemName: artwork.symbolName)
                .font(.system(size: 38, weight: .semibold))
                .foregroundStyle(accent.opacity(0.82))
        }
    }
}

private struct HubAvatarView: View {
    let artwork: HubArtwork
    let size: CGFloat

    var body: some View {
        HubArtworkView(artwork: artwork)
            .frame(width: size, height: size)
            .clipShape(Circle())
    }
}

private struct HubPressableButtonStyle: ButtonStyle {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .contentShape(Rectangle())
            .opacity(configuration.isPressed ? 0.82 : 1)
            .scaleEffect(configuration.isPressed && !reduceMotion ? 0.985 : 1)
            .animation(reduceMotion ? nil : .easeOut(duration: 0.14), value: configuration.isPressed)
    }
}

// MARK: - Creator row

private struct HubCreatorRowSection: View {
    let header: HubSectionHeader
    let items: [HubCreatorItem]
    @Bindable var viewModel: HubFeedViewModel
    let onOpen: (HubDestination) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HubSectionHeaderView(header: header)

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: 16) {
                    ForEach(items) { creator in
                        HubCreatorCard(creator: creator, viewModel: viewModel, onOpen: onOpen)
                    }
                }
                .scrollTargetLayout()
                .padding(.vertical, 4)
            }
            .scrollTargetBehavior(.viewAligned)
        }
    }
}

private struct HubCreatorCard: View {
    let creator: HubCreatorItem
    @Bindable var viewModel: HubFeedViewModel
    let onOpen: (HubDestination) -> Void

    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization

    private var following: Bool { viewModel.isFollowing(creator) }
    private var isPending: Bool { viewModel.isFollowPending(creator) }

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Button {
                if let destination = HubDestination(href: creator.href) { onOpen(destination) }
            } label: {
            VStack(spacing: 8) {
                HubAvatarView(artwork: creator.artwork, size: 76)
                    .padding(.bottom, 4)

                VStack(spacing: 2) {
                    Text(creator.title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(theme.onSurface)
                        .lineLimit(1)

                    Text("@\(creator.username)")
                        .font(.caption)
                        .foregroundStyle(theme.onSurfaceVariant)
                        .lineLimit(1)

                    Text(followerLabel)
                        .font(.caption2)
                        .foregroundStyle(theme.onSurfaceVariant)
                        .lineLimit(1)
                }
                .accessibilityElement(children: .combine)
            }
            .frame(width: 92)
            }
            .buttonStyle(.plain)

            Button {
                Task { await viewModel.toggleFollow(creator) }
            } label: {
                ZStack {
                    Circle()
                        .fill(following ? theme.primaryContainer : theme.primary)
                        .frame(width: 28, height: 28)
                        .overlay {
                            Circle().stroke(theme.surface, lineWidth: 2)
                        }

                    if isPending {
                        ProgressView()
                            .controlSize(.mini)
                            .tint(following ? theme.onPrimaryContainer : theme.primaryForeground)
                    } else {
                        Image(systemName: following ? "checkmark" : "plus")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(following ? theme.onPrimaryContainer : theme.primaryForeground)
                    }
                }
                .frame(width: 48, height: 48)
                .contentShape(Circle())
            }
            .buttonStyle(HubPressableButtonStyle())
            .disabled(isPending)
            .offset(x: 12, y: 48)
            .animation(.snappy(duration: 0.22), value: following)
            .accessibilityLabel(
                following
                    ? localization.string("home_hub_unfollow", fallback: "Unfollow") + " " + creator.title
                    : localization.string("home_hub_follow", fallback: "Follow") + " " + creator.title
            )
            .accessibilityValue(following ? localization.string("home_hub_following", fallback: "Following") : "")
        }
    }

    private var followerLabel: String {
        let count = viewModel.followersCount(creator)
        let formatted = count.formatted(.number.notation(.compactName))
        let key = count == 1 ? "home_hub_follower" : "home_hub_followers"
        let fallback = count == 1 ? "follower" : "followers"
        return "\(formatted) \(localization.string(key, fallback: fallback))"
    }
}

// MARK: - Hero carousel

private struct HubHeroCarouselSection: View {
    let header: HubSectionHeader
    let items: [HubHeroProjectItem]
    let availableWidth: CGFloat
    let onOpen: (HubDestination) -> Void

    private var cardWidth: CGFloat {
        min(max(availableWidth * 0.84, 270), 440)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HubSectionHeaderView(header: header)

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: 16) {
                    ForEach(items) { item in
                        Button {
                            if let destination = HubDestination(href: item.href) { onOpen(destination) }
                        } label: {
                            HubHeroCard(item: item, width: cardWidth)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .scrollTargetLayout()
                .padding(.vertical, 4)
            }
            .scrollTargetBehavior(.viewAligned)
        }
    }
}

private struct HubHeroCard: View {
    let item: HubHeroProjectItem
    let width: CGFloat

    @Environment(\.amethystTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HubArtworkView(artwork: item.artwork)
                .frame(width: width)
                .aspectRatio(16 / 9, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))

            HStack(spacing: 12) {
                HubAvatarView(artwork: item.creatorArtwork, size: 42)

                VStack(alignment: .leading, spacing: 2) {
                    Text(item.creatorName)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(theme.onSurfaceVariant)
                        .lineLimit(1)

                    Text(item.title)
                        .font(.title3.weight(.bold))
                        .foregroundStyle(theme.onSurface)
                        .lineLimit(1)

                    if let subtitle = item.subtitle {
                        Text(subtitle)
                            .font(.caption)
                            .foregroundStyle(theme.onSurfaceVariant)
                            .lineLimit(1)
                    }
                }

                Spacer(minLength: 8)
            }
            .padding(.horizontal, 2)
        }
        .frame(width: width)
        .accessibilityLabel("\(item.title), \(item.creatorName)")
    }
}

// MARK: - Square cards

private struct HubSquareCardRowSection: View {
    let header: HubSectionHeader
    let items: [HubSquareCardItem]
    let onOpen: (HubDestination) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HubSectionHeaderView(header: header)
            HubSquareCardRow(items: items, onOpen: onOpen)
        }
    }
}

private struct HubSquareCardRow: View {
    let items: [HubSquareCardItem]
    let onOpen: (HubDestination) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(alignment: .top, spacing: 14) {
                ForEach(items) { item in
                    Button {
                        if let destination = HubDestination(href: item.href) { onOpen(destination) }
                    } label: {
                        HubSquareCard(item: item)
                    }
                    .buttonStyle(.plain)
                }
            }
            .scrollTargetLayout()
            .padding(.vertical, 4)
        }
        .scrollTargetBehavior(.viewAligned)
    }
}

private struct HubSquareCard: View {
    let item: HubSquareCardItem

    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HubArtworkView(artwork: item.artwork)
                .frame(width: 104, height: 104)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

            Text(item.title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(theme.onSurface)
                .lineLimit(1)

            if let itemCount = item.itemCount {
                Text("\(itemCount) \(localization.string("home_hub_items", fallback: "items"))")
                    .font(.caption)
                    .foregroundStyle(theme.onSurfaceVariant)
                    .lineLimit(1)
            } else if let subtitle = item.subtitle {
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(theme.onSurfaceVariant)
                    .lineLimit(1)
            }
        }
        .frame(width: 104, alignment: .leading)
    }
}

// MARK: - Media cards

private struct HubMediaCardRowSection: View {
    let header: HubSectionHeader
    let items: [HubMediaCardItem]
    let onOpen: (HubDestination) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HubSectionHeaderView(header: header)

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(alignment: .top, spacing: 14) {
                    ForEach(items) { item in
                        Button {
                            if let destination = HubDestination(href: item.href) { onOpen(destination) }
                        } label: {
                            HubMediaCard(item: item)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .scrollTargetLayout()
                .padding(.vertical, 4)
            }
            .scrollTargetBehavior(.viewAligned)
        }
    }
}

private struct HubMediaCard: View {
    let item: HubMediaCardItem

    @Environment(\.amethystTheme) private var theme

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            HubArtworkView(artwork: item.artwork)

            VStack(alignment: .leading, spacing: 2) {
                Text(item.artist)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(theme.onSurface)
                    .lineLimit(1)

                Text(item.title)
                    .font(.caption)
                    .foregroundStyle(theme.onSurfaceVariant)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(.ultraThinMaterial)
        }
        .frame(width: 148, height: 196)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .accessibilityLabel("\(item.title), \(item.artist)")
    }
}

// MARK: - Detailed list

private struct HubDetailedListSection: View {
    let header: HubSectionHeader
    let items: [HubDetailedListItem]
    let onOpen: (HubDestination) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HubSectionHeaderView(header: header)

            LazyVStack(spacing: 14) {
                ForEach(items) { item in
                    Button {
                        if let destination = HubDestination(href: item.href) { onOpen(destination) }
                    } label: {
                        HubDetailedListRow(item: item)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

private struct HubDetailedListRow: View {
    let item: HubDetailedListItem

    @Environment(\.amethystTheme) private var theme

    var body: some View {
        HStack(spacing: 16) {
            HubArtworkView(artwork: item.artwork)
                .frame(width: 104, height: 104)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))

            VStack(alignment: .leading, spacing: 5) {
                Text(item.title)
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(theme.onSurface)
                    .lineLimit(1)

                Text(item.description)
                    .font(.subheadline)
                    .foregroundStyle(theme.onSurfaceVariant)
                    .lineLimit(2)

                Spacer(minLength: 0)

                HStack(spacing: 6) {
                    Image(systemName: "clock")
                        .accessibilityHidden(true)
                    Text("\(item.uploadedAt) · \(item.compatibility)")
                        .lineLimit(1)
                }
                .font(.caption)
                .foregroundStyle(theme.onSurfaceVariant)
            }
            .frame(minHeight: 104, alignment: .top)
        }
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Spotlight

private struct HubSpotlightSection: View {
    let header: HubSectionHeader
    let spotlight: HubSpotlight
    let onOpen: (HubDestination) -> Void

    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 12) {
                HubAvatarView(artwork: spotlight.creatorArtwork, size: 44)

                VStack(alignment: .leading, spacing: 1) {
                    Text(localization.string("home_hub_more_like", fallback: "More like"))
                        .font(.caption.weight(.medium))
                        .textCase(.uppercase)
                        .foregroundStyle(theme.onSurfaceVariant)

                    Text(header.title)
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(theme.onSurface)
                        .lineLimit(1)
                }

                Spacer(minLength: 8)
            }
            .accessibilityLabel("\(localization.string("home_hub_more_like", fallback: "More like")) \(header.title)")

            HubSquareCardRow(items: spotlight.items, onOpen: onOpen)

            Text(spotlight.description)
                .font(.subheadline)
                .foregroundStyle(theme.onSurfaceVariant)
                .lineLimit(3)
                .frame(maxWidth: 680, alignment: .leading)
        }
    }
}
