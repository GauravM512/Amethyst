import CryptoKit
import Foundation
import SwiftUI
import UIKit

struct HubCachedAsyncImage<Content: View>: View {
    let url: URL?
    @ViewBuilder let content: (AsyncImagePhase) -> Content

    @State private var phase: AsyncImagePhase = .empty

    private struct Request: Hashable {
        let url: URL?
        let revision: Int
    }

    var body: some View {
        content(phase)
            .task(id: Request(url: url, revision: HubImageCache.shared.revision)) {
                phase = .empty
                guard let url else { return }
                do {
                    let image = try await HubImageCache.shared.image(for: url)
                    try Task.checkCancellation()
                    phase = .success(Image(uiImage: image))
                } catch {
                    guard !Task.isCancelled else { return }
                    phase = .failure(error)
                }
            }
    }
}

@Observable
@MainActor
final class HubImageCache {
    static let shared = HubImageCache()

    private(set) var revision = 0

    @ObservationIgnored private let memory = NSCache<NSURL, Entry>()
    @ObservationIgnored private var pending: [URL: (id: UUID, task: Task<Entry, Error>)] = [:]
    @ObservationIgnored private let disk = HubImageDiskCache()
    @ObservationIgnored private let session: URLSession

    private final class Entry {
        let image: UIImage
        let expiresAt: Date

        init(image: UIImage, expiresAt: Date) {
            self.image = image
            self.expiresAt = expiresAt
        }
    }

    init() {
        memory.totalCostLimit = 32 * 1024 * 1024
        let configuration = URLSessionConfiguration.ephemeral
        configuration.urlCache = nil
        configuration.timeoutIntervalForRequest = 10
        configuration.timeoutIntervalForResource = 20
        session = URLSession(configuration: configuration)
    }

    func image(for url: URL) async throws -> UIImage {
        if let entry = memory.object(forKey: url as NSURL) {
            if entry.expiresAt > Date() {
                return entry.image
            }
            memory.removeObject(forKey: url as NSURL)
        }

        let request: (id: UUID, task: Task<Entry, Error>)
        if let existing = pending[url] {
            request = existing
        } else {
            request = (UUID(), Task { try await load(url: url) })
            pending[url] = request
        }

        defer {
            if pending[url]?.id == request.id {
                pending[url] = nil
            }
        }
        return try await request.task.value.image
    }

    func invalidate(_ url: URL) async {
        pending.removeValue(forKey: url)?.task.cancel()
        memory.removeObject(forKey: url as NSURL)
        await disk.remove(url: url)
        revision += 1
    }

    private func load(url: URL) async throws -> Entry {
        if let stored = await disk.read(url: url), let image = UIImage(data: stored.data) {
            try Task.checkCancellation()
            return remember(image: image, expiresAt: stored.expiresAt, url: url)
        }

        let (data, response) = try await session.data(from: url)
        guard let response = response as? HTTPURLResponse,
              (200..<300).contains(response.statusCode),
              !data.isEmpty, data.count <= 10 * 1024 * 1024,
              let image = UIImage(data: data) else {
            throw URLError(.cannotDecodeContentData)
        }

        try Task.checkCancellation()
        let expiresAt = Date().addingTimeInterval(3600)
        await disk.write(stored: HubImageDiskCache.Stored(data: data, expiresAt: expiresAt), url: url)
        try Task.checkCancellation()
        return remember(image: image, expiresAt: expiresAt, url: url)
    }

    private func remember(image: UIImage, expiresAt: Date, url: URL) -> Entry {
        let entry = Entry(image: image, expiresAt: expiresAt)
        let cost = image.cgImage.map { $0.bytesPerRow * $0.height }
            ?? Int(image.size.width * image.size.height * image.scale * image.scale * 4)
        memory.setObject(entry, forKey: url as NSURL, cost: cost)
        return entry
    }
}

actor HubImageDiskCache {
    struct Stored: Codable, Sendable {
        let data: Data
        let expiresAt: Date
    }

    private let directory: URL?
    private let limit = 128 * 1024 * 1024

    init(directory: URL? = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first?
        .appendingPathComponent("hub-images-v1", isDirectory: true)) {
        self.directory = directory
    }

    func read(url: URL) -> Stored? {
        guard let path = path(for: url) else { return nil }
        guard let data = try? Data(contentsOf: path),
              let stored = try? PropertyListDecoder().decode(Stored.self, from: data),
              stored.expiresAt > Date() else {
            try? FileManager.default.removeItem(at: path)
            return nil
        }
        return stored
    }

    func write(stored: Stored, url: URL) {
        guard !Task.isCancelled, let directory, let path = path(for: url) else { return }
        do {
            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
            let encoder = PropertyListEncoder()
            encoder.outputFormat = .binary
            try encoder.encode(stored).write(to: path, options: .atomic)
            try trim()
        } catch {}
    }

    func remove(url: URL) {
        guard let path = path(for: url) else { return }
        try? FileManager.default.removeItem(at: path)
    }

    private func path(for url: URL) -> URL? {
        let key = SHA256.hash(data: Data(url.absoluteString.utf8))
            .map { String(format: "%02x", $0) }.joined()
        return directory?.appendingPathComponent(key + ".image")
    }

    private func trim() throws {
        guard let directory else { return }
        let files = try FileManager.default.contentsOfDirectory(
            at: directory,
            includingPropertiesForKeys: [.fileSizeKey, .contentModificationDateKey]
        ).filter { $0.pathExtension == "image" }.map { url in
            let values = try url.resourceValues(forKeys: [.fileSizeKey, .contentModificationDateKey])
            return (url: url, size: values.fileSize ?? 0, modified: values.contentModificationDate ?? .distantPast)
        }.sorted { $0.modified < $1.modified }
        var total = files.reduce(0) { $0 + $1.size }

        for file in files where total > limit || file.modified.addingTimeInterval(3600) <= Date() {
            try FileManager.default.removeItem(at: file.url)
            total -= file.size
        }
    }
}
