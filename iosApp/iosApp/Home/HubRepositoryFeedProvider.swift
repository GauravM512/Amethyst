//
//  HubRepositoryFeedProvider.swift
//  iosApp
//
//  Created by Anthony Hofmeister on 20.09.26.
//  Copyright © 2026 Anthony Hofmeister. All rights reserved.
//

import Foundation
import ComposeApp

@MainActor
final class HubRepositoryFeedProvider: HubFeedProviding {
    private let repository: HubRepository

    init(repository: HubRepository) {
        self.repository = repository
    }

    var isAuthenticated: Bool {
        repository.client.isAuthenticated
    }

    func loadFeed() async throws -> HubFeed {
        let home: ComposeApp.HubHome = try await withCheckedThrowingContinuation { continuation in
            repository.getHome.execute { home, error in
                if let home {
                    continuation.resume(returning: home)
                } else {
                    continuation.resume(throwing: error ?? HubFeedProviderError.missingResponse)
                }
            }
        }

        return map(home)
    }

    func setFollowing(_ following: Bool, username: String) async throws -> HubFollowState {
        let result: ComposeApp.HubFollowResult = try await withCheckedThrowingContinuation { continuation in
            let completion: (ComposeApp.HubFollowResult?, Error?) -> Void = { result, error in
                if let result {
                    continuation.resume(returning: result)
                } else {
                    continuation.resume(throwing: error ?? HubFeedProviderError.missingResponse)
                }
            }

            if following {
                repository.followArtist.execute(username: username, completionHandler: completion)
            } else {
                repository.unfollowArtist.execute(username: username, completionHandler: completion)
            }
        }

        return HubFollowState(
            isFollowing: result.following,
            followersCount: Int(clamping: result.followersCount)
        )
    }

    private func map(_ home: ComposeApp.HubHome) -> HubFeed {
        HubFeed(
            pageTitle: home.pageTitle,
            sections: home.sections.compactMap(map(section:))
        )
    }

    private func map(section: any ComposeApp.HubHomeSection) -> HubFeedSection? {
        switch section {
        case let value as ComposeApp.HubHomeSectionCreatorRow:
            return .creatorRow(
                header(for: value),
                value.items.map { item in
                    HubCreatorItem(
                        id: item.id,
                        title: item.title,
                        username: item.username,
                        subtitle: item.subtitle,
                        artwork: artwork(item.imageUrl, symbol: "person.crop.circle.fill"),
                        followersCount: int(item.followersCount) ?? 0,
                        isFollowing: item.isFollowing?.boolValue ?? false,
                        href: item.href
                    )
                }
            )

        case let value as ComposeApp.HubHomeSectionHeroCarousel:
            return .heroCarousel(
                header(for: value),
                value.items.map { item in
                    HubHeroProjectItem(
                        id: item.id,
                        title: item.title,
                        subtitle: item.subtitle ?? item.hardwareModel,
                        creatorName: item.creatorName,
                        artwork: artwork(item.imageUrl, symbol: "square.grid.3x3.square"),
                        creatorArtwork: artwork(item.creatorAvatarUrl, symbol: "person.crop.circle.fill"),
                        href: item.href
                    )
                }
            )

        case let value as ComposeApp.HubHomeSectionSquareCardRow:
            return .squareCardRow(
                header(for: value),
                value.items.map(map(squareItem:))
            )

        case let value as ComposeApp.HubHomeSectionMediaCardRow:
            return .mediaCardRow(
                header(for: value),
                value.items.map { item in
                    HubMediaCardItem(
                        id: item.id,
                        title: item.title,
                        subtitle: item.subtitle,
                        artist: item.artist,
                        artwork: artwork(item.imageUrl, symbol: "music.note"),
                        href: item.href
                    )
                }
            )

        case let value as ComposeApp.HubHomeSectionDetailedList:
            return .detailedList(
                header(for: value),
                value.items.map { item in
                    HubDetailedListItem(
                        id: item.id,
                        title: item.title,
                        subtitle: item.subtitle,
                        description: item.description_,
                        uploadedAt: item.uploadedAt,
                        compatibility: item.compatibility,
                        artwork: artwork(item.imageUrl, symbol: "waveform"),
                        href: item.href
                    )
                }
            )

        case let value as ComposeApp.HubHomeSectionCuratedSpotlight:
            return .curatedSpotlight(
                header(for: value),
                HubSpotlight(
                    creatorName: value.creatorName,
                    creatorArtwork: artwork(value.creatorAvatarUrl, symbol: "person.crop.circle.fill"),
                    description: value.description_,
                    items: value.items.map(map(squareItem:))
                )
            )

        default:
            // The server can add new section types without breaking older clients.
            return nil
        }
    }

    private func header(for section: any ComposeApp.HubHomeSection) -> HubSectionHeader {
        HubSectionHeader(
            id: section.id_,
            title: section.title,
            actionLabel: section.actionLabel,
            actionHref: section.actionHref
        )
    }

    private func map(squareItem item: ComposeApp.HubSquareCardItem) -> HubSquareCardItem {
        HubSquareCardItem(
            id: item.id,
            title: item.title,
            subtitle: item.subtitle,
            itemCount: int(item.itemCount),
            artwork: artwork(
                item.imageUrl,
                symbol: symbolName(for: item.iconName),
                accent: accent(for: item.colorAccent)
            ),
            href: item.href
        )
    }

    private func artwork(
        _ pathOrURL: String?,
        symbol: String,
        accent: HubArtworkAccent = .primary
    ) -> HubArtwork {
        let resolvedURL = pathOrURL.flatMap { value -> String? in
            guard !value.isEmpty else { return nil }
            return repository.client.resolveUrl(pathOrUrl: value)
        }
        return HubArtwork(imageURL: resolvedURL, symbolName: symbol, accent: accent)
    }

    private func int(_ value: KotlinLong?) -> Int? {
        value.map { Int(clamping: $0.int64Value) }
    }

    private func accent(for value: String?) -> HubArtworkAccent {
        switch value?.lowercased() {
        case "secondary", "tertiary", "pink", "purple": return .secondary
        case "neutral", "gray", "grey": return .neutral
        default: return .primary
        }
    }

    private func symbolName(for value: String?) -> String {
        switch value?.lowercased() {
        case "artist", "person", "group": return "person.2.fill"
        case "hardware", "grid", "launchpad": return "square.grid.3x3.fill"
        case "playlist", "collection": return "rectangle.stack.fill"
        case "music", "music_note", "song": return "music.note"
        case "sparkles", "featured": return "sparkles"
        default: return "square.grid.3x3.square"
        }
    }
}

private enum HubFeedProviderError: LocalizedError {
    case missingResponse

    var errorDescription: String? {
        IosLocalizationBridge.shared.string(
            key: "home_hub_error_invalid_response",
            fallback: "The Hub returned an invalid response."
        )
    }
}
