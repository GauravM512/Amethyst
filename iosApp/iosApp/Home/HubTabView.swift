//
//  HubTabView.swift
//  iosApp
//
//  Created by Anthony Hofmeister on 20.09.26.
//  Copyright © 2026 Anthony Hofmeister. All rights reserved.
//

import SwiftUI
import ComposeApp

struct HubTabView: View {
    @Bindable var viewModel: HubFeedViewModel
    @Bindable var searchViewModel: HubSearchViewModel
    @Binding var searchText: String
    let sessionRevision: Int
    let onShowProfile: () -> Void
    let onOpenDownloadedFile: (URL, String, String) -> Void
    @State private var destination: HubDestination?
    @State private var showLikedProjects = false
    @State private var showProjects = false
    @State private var likedProjectsRevision = 0

    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottom) {
                theme.surface.ignoresSafeArea()
                HubSearchContent(
                    query: searchText,
                    searchViewModel: searchViewModel,
                    repository: viewModel.repository,
                    onOpen: { destination = $0 }
                ) {
                    feedContent
                }

                if let feedback = viewModel.feedback {
                    HubFeedbackBanner(
                        feedback: feedback,
                        onDismiss: viewModel.dismissFeedback,
                        onShowProfile: onShowProfile
                    )
                    .padding(.horizontal, 16)
                    .padding(.bottom, 12)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .zIndex(1)
                }
            }
            .navigationTitle(localization.string("home_hub_title", fallback: "Amethyst Hub"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showLikedProjects = true
                    } label: {
                        Image(systemName: "heart")
                    }
                    .accessibilityLabel(localization.string("home_hub_liked_title", fallback: "Liked Projects"))
                }
            }
            .navigationDestination(isPresented: $showLikedProjects) {
                HubLikedProjectsView(
                    repository: viewModel.repository,
                    revision: "\(sessionRevision):\(likedProjectsRevision)",
                    onOpen: { destination = $0 },
                    onSignIn: {
                        showLikedProjects = false
                        onShowProfile()
                    }
                )
            }
            .navigationDestination(isPresented: $showProjects) {
                HubProjectsView(
                    repository: viewModel.repository,
                    revision: "\(sessionRevision):\(likedProjectsRevision)",
                    onOpen: { destination = $0 }
                )
            }
        }
        .searchable(
            text: $searchText,
            placement: .navigationBarDrawer(displayMode: .always),
            prompt: Text(localization.string("home_hub_search_placeholder", fallback: "Search"))
        )
        .tint(theme.glassForeground)
        .task(id: sessionRevision) {
            await viewModel.reload()
        }
        .animation(.easeInOut(duration: 0.2), value: viewModel.feedback)
        .sheet(item: $destination, onDismiss: { likedProjectsRevision += 1 }) { item in
            HubDetailView(
                destination: item,
                repository: viewModel.repository,
                onSignIn: {
                    destination = nil
                    onShowProfile()
                },
                onOpenDownloadedFile: { url, projectID, title in
                    destination = nil
                    onOpenDownloadedFile(url, projectID, title)
                }
            )
        }
    }

    @ViewBuilder
    private var feedContent: some View {
        if viewModel.isLoading, viewModel.feed == nil {
            VStack(spacing: 14) {
                ProgressView()
                    .controlSize(.large)
                Text(localization.string("home_hub_loading", fallback: "Loading Hub…"))
                    .font(.body)
                    .foregroundStyle(theme.onSurfaceVariant)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .accessibilityElement(children: .combine)
        } else if let errorMessage = viewModel.errorMessage, viewModel.feed == nil {
            VStack(spacing: 16) {
                Image(systemName: "wifi.exclamationmark")
                    .font(.system(size: 42, weight: .medium))
                    .foregroundStyle(theme.destructive)

                Text(localization.string("home_hub_error_title", fallback: "Hub unavailable"))
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(theme.onSurface)

                Text(errorMessage)
                    .font(.body)
                    .foregroundStyle(theme.onSurfaceVariant)
                    .multilineTextAlignment(.center)

                Button(localization.string("home_hub_retry", fallback: "Try Again")) {
                    Task { await viewModel.reload() }
                }
                .buttonStyle(.borderedProminent)
                .tint(theme.primary)
            }
            .padding(24)
            .frame(maxWidth: 440)
        } else if let feed = viewModel.feed {
            if feed.sections.isEmpty {
                ContentUnavailableView(
                    localization.string("home_hub_empty_title", fallback: "Nothing here yet"),
                    systemImage: "rectangle.stack",
                    description: Text(localization.string("home_hub_empty_description", fallback: "New community projects will appear here."))
                )
            } else {
                GeometryReader { proxy in
                    HubFeedView(
                        sections: feed.sections,
                        availableWidth: min(max(proxy.size.width - 40, 280), 960),
                        viewModel: viewModel,
                        onOpen: { destination = $0 },
                        onShowAllProjects: { showProjects = true }
                    )
                }
                .refreshable {
                    await viewModel.reload()
                }
            }
        }
    }
}

private struct HubSearchContent<Feed: View>: View {
    let query: String
    @Bindable var searchViewModel: HubSearchViewModel
    let repository: HubRepository
    let onOpen: (HubDestination) -> Void
    @ViewBuilder let feed: () -> Feed

    @Environment(\.isSearching) private var isSearching

    var body: some View {
        if isSearching || !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            HubSearchView(
                query: query,
                viewModel: searchViewModel,
                repository: repository,
                onOpen: onOpen
            )
        } else {
            feed()
        }
    }
}

private struct HubFeedbackBanner: View {
    let feedback: HubFeedFeedback
    let onDismiss: () -> Void
    let onShowProfile: () -> Void

    @Environment(\.amethystTheme) private var theme
    @Environment(AppLocalization.self) private var localization

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: iconName)
                .font(.body.weight(.semibold))
                .foregroundStyle(theme.onPrimaryContainer)
                .accessibilityHidden(true)

            Text(message)
                .font(.subheadline)
                .foregroundStyle(theme.onSurface)
                .frame(maxWidth: .infinity, alignment: .leading)

            if case .authenticationRequired = feedback {
                Button(localization.string("home_hub_sign_in", fallback: "Sign in")) {
                    onDismiss()
                    onShowProfile()
                }
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(theme.glassForeground)
                .frame(minHeight: 48)
            }

            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.caption.weight(.bold))
                    .frame(width: 48, height: 48)
                    .contentShape(Rectangle())
            }
            .foregroundStyle(theme.onSurfaceVariant)
            .accessibilityLabel(localization.string("home_hub_dismiss", fallback: "Dismiss"))
        }
        .padding(.leading, 16)
        .padding(.trailing, 4)
        .padding(.vertical, 4)
        .frame(maxWidth: 560)
        .background(theme.surfaceContainerHigh, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(theme.outlineVariant, lineWidth: 1)
        }
        .shadow(color: .black.opacity(0.15), radius: 10, y: 4)
        .accessibilityElement(children: .contain)
    }

    private var message: String {
        switch feedback {
        case .authenticationRequired:
            localization.string(
                "home_hub_authentication_required",
                fallback: "Sign in to follow artists."
            )
        case .error(let message):
            message
        }
    }

    private var iconName: String {
        switch feedback {
        case .authenticationRequired: "person.crop.circle.badge.exclamationmark"
        case .error: "exclamationmark.triangle.fill"
        }
    }
}
