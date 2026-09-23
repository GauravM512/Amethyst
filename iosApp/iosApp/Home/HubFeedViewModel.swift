//
//  HubFeedViewModel.swift
//  iosApp
//
//  Created by Anthony Hofmeister on 20.09.26.
//  Copyright © 2026 Anthony Hofmeister. All rights reserved.
//

import Foundation
import ComposeApp

enum HubFeedFeedback: Equatable {
    case authenticationRequired
    case error(String)
}

@Observable
@MainActor
final class HubFeedViewModel {
    private(set) var feed: HubFeed?
    private(set) var isLoading = false
    private(set) var errorMessage: String?
    private(set) var feedback: HubFeedFeedback?

    private let provider: any HubFeedProviding
    let repository: HubRepository
    private var followOverrides: [String: Bool] = [:]
    private var followerCountOverrides: [String: Int] = [:]
    private var pendingFollowUsernames: Set<String> = []
    private var loadGeneration = 0

    init(repository: HubRepository) {
        self.repository = repository
        provider = HubRepositoryFeedProvider(repository: repository)
    }

    func loadIfNeeded() async {
        guard feed == nil, !isLoading else { return }
        await reload()
    }

    func reload() async {
        loadGeneration += 1
        let generation = loadGeneration
        isLoading = true
        errorMessage = nil

        do {
            let loadedFeed = try await provider.loadFeed()
            guard generation == loadGeneration else { return }
            feed = loadedFeed
            followOverrides.removeAll()
            followerCountOverrides.removeAll()
        } catch {
            guard generation == loadGeneration else { return }
            let message = readableMessage(for: error)
            if feed == nil {
                errorMessage = message
            } else {
                feedback = .error(message)
            }
        }

        if generation == loadGeneration {
            isLoading = false
        }
    }

    func isFollowing(_ creator: HubCreatorItem) -> Bool {
        followOverrides[creator.username] ?? creator.isFollowing
    }

    func followersCount(_ creator: HubCreatorItem) -> Int {
        followerCountOverrides[creator.username] ?? creator.followersCount
    }

    func isFollowPending(_ creator: HubCreatorItem) -> Bool {
        pendingFollowUsernames.contains(creator.username)
    }

    func toggleFollow(_ creator: HubCreatorItem) async {
        guard !isFollowPending(creator) else { return }
        guard provider.isAuthenticated else {
            feedback = .authenticationRequired
            return
        }

        let wasFollowing = isFollowing(creator)
        let currentCount = followersCount(creator)
        let requestedState = !wasFollowing

        pendingFollowUsernames.insert(creator.username)
        followOverrides[creator.username] = requestedState
        followerCountOverrides[creator.username] = max(0, currentCount + (requestedState ? 1 : -1))

        do {
            let state = try await provider.setFollowing(requestedState, username: creator.username)
            followOverrides[creator.username] = state.isFollowing
            followerCountOverrides[creator.username] = state.followersCount
        } catch {
            followOverrides[creator.username] = wasFollowing
            followerCountOverrides[creator.username] = currentCount

            if isAuthenticationError(error) {
                feedback = .authenticationRequired
            } else {
                feedback = .error(
                    IosLocalizationBridge.shared.string(
                        key: "home_hub_follow_error",
                        fallback: "The follow status could not be updated. Please try again."
                    )
                )
            }
        }

        pendingFollowUsernames.remove(creator.username)
    }

    func dismissFeedback() {
        feedback = nil
    }

    private func readableMessage(for error: Error) -> String {
        let nsError = error as NSError
        if nsError.domain == NSURLErrorDomain {
            return IosLocalizationBridge.shared.string(
                key: "home_hub_error_offline",
                fallback: "Check your internet connection and try again."
            )
        }

        if let hubError = nsError.kotlinException as? HubApiException,
           hubError.statusCode == 429 || hubError.errorCode == "rate_limited" {
            return IosLocalizationBridge.shared.string(
                key: "home_hub_error_rate_limited",
                fallback: "The Hub is receiving too many requests. Please try again shortly."
            )
        }

        return IosLocalizationBridge.shared.string(
            key: "home_hub_error_generic",
            fallback: "The Hub could not be loaded. Please try again."
        )
    }

    private func isAuthenticationError(_ error: Error) -> Bool {
        let nsError = error as NSError
        if let hubError = nsError.kotlinException as? HubApiException {
            return hubError.statusCode == 401 || hubError.errorCode == "authentication_required"
        }
        return error.localizedDescription.contains("authentication_required")
    }
}
