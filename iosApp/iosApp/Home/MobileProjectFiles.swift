import Foundation
import CryptoKit

/// The original package is durable; only the staging directory is disposable.
enum MobileProjectFiles {
    struct StoredOriginal: Sendable {
        let url: URL
        let sha256: String
    }

    static func importOriginal(from source: URL, projectID: String) throws -> StoredOriginal {
        let manager = FileManager.default
        let documents = try manager.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
        let safeID = projectID.unicodeScalars.map { scalar -> Character in
            CharacterSet.alphanumerics.contains(scalar) || scalar == "-" || scalar == "_"
                ? Character(scalar) : "_"
        }
        let root = documents.appendingPathComponent("Amethyst/Projects/\(String(safeID))", isDirectory: true)
        let originals = root.appendingPathComponent("Original", isDirectory: true)
        try manager.createDirectory(at: originals, withIntermediateDirectories: true)
        let filename = source.lastPathComponent
        let destination = originals.appendingPathComponent(filename)
        let staging = originals.appendingPathComponent(".\(UUID().uuidString).part")
        do {
            try manager.copyItem(at: source, to: staging)
            let handle = try FileHandle(forReadingFrom: staging)
            defer { try? handle.close() }
            var digest = SHA256()
            while let chunk = try handle.read(upToCount: 1024 * 1024), !chunk.isEmpty {
                digest.update(data: chunk)
            }
            let sha256 = digest.finalize().map { String(format: "%02x", $0) }.joined()
            if manager.fileExists(atPath: destination.path) {
                _ = try manager.replaceItemAt(destination, withItemAt: staging)
            } else {
                try manager.moveItem(at: staging, to: destination)
            }
            for previous in (try? manager.contentsOfDirectory(at: originals, includingPropertiesForKeys: nil)) ?? []
            where previous.standardizedFileURL.path != destination.standardizedFileURL.path {
                try? manager.removeItem(at: previous)
            }
            guard manager.fileExists(atPath: destination.path) else {
                throw CocoaError(.fileNoSuchFile)
            }
            return StoredOriginal(url: destination, sha256: sha256)
        } catch {
            try? manager.removeItem(at: staging)
            throw error
        }
    }
}
