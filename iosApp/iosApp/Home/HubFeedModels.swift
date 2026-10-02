//
//  HubFeedModels.swift
//  iosApp
//
//  Created by Anthony Hofmeister on 20.09.26.
//  Copyright © 2026 Anthony Hofmeister. All rights reserved.
//

import Foundation

protocol HubFeedProviding {
    func loadFeed() async throws -> HubFeed
    var isAuthenticated: Bool { get }
    func setFollowing(_ following: Bool, username: String) async throws -> HubFollowState
}

struct HubFollowState: Equatable {
    let isFollowing: Bool
    let followersCount: Int
}

struct HubFeed: Equatable {
    let pageTitle: String
    let sections: [HubFeedSection]
}

struct HubSectionHeader: Identifiable, Equatable {
    let id: String
    let title: String
    let actionLabel: String?
    let actionHref: String?
}

enum HubArtworkAccent: Equatable {
    case primary
    case secondary
    case neutral
}

struct HubArtwork: Equatable {
    let imageURL: String?
    let symbolName: String
    let accent: HubArtworkAccent

    init(imageURL: String? = nil, symbolName: String, accent: HubArtworkAccent = .primary) {
        self.imageURL = imageURL
        self.symbolName = symbolName
        self.accent = accent
    }
}

struct HubCreatorItem: Identifiable, Equatable {
    let id: String
    let title: String
    let username: String
    let subtitle: String?
    let artwork: HubArtwork
    let followersCount: Int
    let isFollowing: Bool
    let href: String?
}

struct HubHeroProjectItem: Identifiable, Equatable {
    let id: String
    let title: String
    let subtitle: String?
    let creatorName: String
    let artwork: HubArtwork
    let creatorArtwork: HubArtwork
    let href: String?
}

struct HubSquareCardItem: Identifiable, Equatable {
    let id: String
    let title: String
    let subtitle: String?
    let itemCount: Int?
    let artwork: HubArtwork
    let href: String?
}

struct HubMediaCardItem: Identifiable, Equatable {
    let id: String
    let title: String
    let subtitle: String?
    let artist: String
    let artwork: HubArtwork
    let href: String?
}

struct HubDetailedListItem: Identifiable, Equatable {
    let id: String
    let title: String
    let subtitle: String?
    let description: String
    let uploadedAt: String
    let compatibility: String
    let artwork: HubArtwork
    let href: String?
}

struct HubSpotlight: Equatable {
    let creatorName: String
    let creatorArtwork: HubArtwork
    let description: String
    let items: [HubSquareCardItem]
}

enum HubFeedSection: Identifiable, Equatable {
    case creatorRow(HubSectionHeader, [HubCreatorItem])
    case heroCarousel(HubSectionHeader, [HubHeroProjectItem])
    case squareCardRow(HubSectionHeader, [HubSquareCardItem])
    case mediaCardRow(HubSectionHeader, [HubMediaCardItem])
    case detailedList(HubSectionHeader, [HubDetailedListItem])
    case curatedSpotlight(HubSectionHeader, HubSpotlight)

    var id: String { header.id }

    var header: HubSectionHeader {
        switch self {
        case .creatorRow(let header, _),
             .heroCarousel(let header, _),
             .squareCardRow(let header, _),
             .mediaCardRow(let header, _),
             .detailedList(let header, _),
             .curatedSpotlight(let header, _):
            return header
        }
    }

    var isEmpty: Bool {
        switch self {
        case .creatorRow(_, let items): return items.isEmpty
        case .heroCarousel(_, let items): return items.isEmpty
        case .squareCardRow(_, let items): return items.isEmpty
        case .mediaCardRow(_, let items): return items.isEmpty
        case .detailedList(_, let items): return items.isEmpty
        case .curatedSpotlight(_, let spotlight): return spotlight.items.isEmpty
        }
    }
}
