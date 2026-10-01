import Foundation
import ComposeApp

enum HubProjectDownloadError: Error {
    case invalidResponse
    case unavailable
    case notAProjectFile
}

struct HubProjectDownloadUseCase {
    enum Source {
        case hub(URL)
        case googleDrive(URL)
        case mediaFire(URL)
    }

    static func supportsGoogleDrive(_ url: URL) -> Bool {
        googleDriveFileID(in: url) != nil
    }

    static func supportsMediaFire(_ url: URL) -> Bool {
        mediaFireQuickKey(in: url) != nil
    }

    func execute(
        source: Source,
        suggestedFilename: String,
        expectedSize: Int64? = nil,
        onProgress: @escaping @Sendable (Double) -> Void = { _ in }
    ) async throws -> URL {
        let requestURL: URL
        var knownSize = expectedSize
        switch source {
        case .hub(let url):
            requestURL = url
        case .googleDrive(let sharedURL):
            guard let id = Self.googleDriveFileID(in: sharedURL),
                  var components = URLComponents(string: "https://drive.usercontent.google.com/download") else {
                throw HubProjectDownloadError.unavailable
            }
            var query = [
                URLQueryItem(name: "id", value: id),
                URLQueryItem(name: "export", value: "download"),
                URLQueryItem(name: "confirm", value: "t"),
            ]
            if let resourceKey = URLComponents(url: sharedURL, resolvingAgainstBaseURL: false)?
                .queryItems?.first(where: { $0.name == "resourcekey" })?.value {
                query.append(URLQueryItem(name: "resourcekey", value: resourceKey))
            }
            components.queryItems = query
            guard let url = components.url else { throw HubProjectDownloadError.unavailable }
            requestURL = url
        case .mediaFire(let sharedURL):
            let download = try await Self.resolveMediaFireDownload(from: sharedURL)
            requestURL = download.url
            knownSize = download.size ?? expectedSize
        }

        guard requestURL.scheme == "https" else { throw HubProjectDownloadError.unavailable }
        let progressDelegate = HubDownloadProgressDelegate(expectedSize: knownSize, onProgress: onProgress)
        let (temporaryURL, response) = try await progressDelegate.download(from: requestURL)
        defer { try? FileManager.default.removeItem(at: temporaryURL) }
        guard let response = response as? HTTPURLResponse, (200..<300).contains(response.statusCode) else {
            throw HubProjectDownloadError.invalidResponse
        }
        let mime = response.mimeType?.lowercased() ?? ""
        guard mime != "text/html", mime != "application/json", mime != "text/plain" else {
            throw HubProjectDownloadError.notAProjectFile
        }
        let handle = try FileHandle(forReadingFrom: temporaryURL)
        let firstBytes = try handle.read(upToCount: 256) ?? Data()
        try handle.close()
        guard !firstBytes.isEmpty else { throw HubProjectDownloadError.notAProjectFile }
        let prefix = String(decoding: firstBytes, as: UTF8.self)
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
        guard !prefix.hasPrefix("<!doctype html"), !prefix.hasPrefix("<html"),
              !prefix.hasPrefix("<?xml"), !prefix.hasPrefix("{"), !prefix.hasPrefix("[") else {
            throw HubProjectDownloadError.notAProjectFile
        }

        let filename = validFilename(response.suggestedFilename) ?? validFilename(suggestedFilename)
        guard let filename else { throw HubProjectDownloadError.notAProjectFile }
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let destination = directory.appendingPathComponent(filename)
        do {
            try FileManager.default.moveItem(at: temporaryURL, to: destination)
            return destination
        } catch {
            try? FileManager.default.removeItem(at: directory)
            throw error
        }
    }

    private func validFilename(_ value: String?) -> String? {
        guard let value else { return nil }
        let filename = URL(fileURLWithPath: value).lastPathComponent
        let allowed = ["ame", "als", "zip", "approj"]
        return allowed.contains(URL(fileURLWithPath: filename).pathExtension.lowercased()) ? filename : nil
    }

    private static func googleDriveFileID(in url: URL) -> String? {
        guard url.scheme == "https",
              let host = url.host?.lowercased(),
              ["drive.google.com", "www.drive.google.com", "drive.usercontent.google.com"].contains(host) else { return nil }
        let parts = url.pathComponents
        let id: String?
        if let fileIndex = parts.firstIndex(of: "file"),
           parts.indices.contains(fileIndex + 2), parts[fileIndex + 1] == "d" {
            id = parts[fileIndex + 2]
        } else if ["/open", "/uc", "/download"].contains(url.path) {
            id = URLComponents(url: url, resolvingAgainstBaseURL: false)?
                .queryItems?.first(where: { $0.name == "id" })?.value
        } else {
            id = nil
        }
        guard let id, !id.isEmpty,
              id.unicodeScalars.allSatisfy({ CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-").contains($0) }) else {
            return nil
        }
        return id
    }

    static func resolveMediaFireDownloadURL(from sharedURL: URL) async throws -> URL {
        try await resolveMediaFireDownload(from: sharedURL).url
    }

    private static func resolveMediaFireDownload(from sharedURL: URL) async throws -> (url: URL, size: Int64?) {
        guard let quickKey = mediaFireQuickKey(in: sharedURL),
              var components = URLComponents(string: "https://www.mediafire.com/api/1.5/file/get_info.php") else {
            throw HubProjectDownloadError.unavailable
        }
        components.queryItems = [
            URLQueryItem(name: "quick_key", value: quickKey),
            URLQueryItem(name: "response_format", value: "json"),
        ]
        guard let apiURL = components.url else { throw HubProjectDownloadError.unavailable }
        let (apiData, apiResponse) = try await URLSession.shared.data(from: apiURL)
        guard let apiResponse = apiResponse as? HTTPURLResponse,
              (200..<300).contains(apiResponse.statusCode),
              let payload = try JSONSerialization.jsonObject(with: apiData) as? [String: Any],
              let response = payload["response"] as? [String: Any],
              response["result"] as? String == "Success",
              let fileInfo = response["file_info"] as? [String: Any],
              fileInfo["privacy"] as? String == "public",
              fileInfo["password_protected"] as? String == "no",
              let links = fileInfo["links"] as? [String: Any],
              let normalLink = links["normal_download"] as? String,
              let pageURL = URL(string: normalLink),
              pageURL.scheme == "https", isMediaFireHost(pageURL.host) else {
            throw HubProjectDownloadError.unavailable
        }

        let (pageData, pageResponse) = try await URLSession.shared.data(from: pageURL)
        guard let pageResponse = pageResponse as? HTTPURLResponse,
              (200..<300).contains(pageResponse.statusCode),
              let html = String(data: pageData, encoding: .utf8),
              let button = firstMatch(in: html, pattern: #"<a\b(?=[^>]*\bid=["']downloadButton["'])[^>]*>"#) else {
            throw HubProjectDownloadError.unavailable
        }

        let href = firstCapture(in: button, pattern: #"\bhref=["']([^"']+)["']"#)?
            .replacingOccurrences(of: "&amp;", with: "&")
        let scrambled = firstCapture(in: button, pattern: #"\bdata-scrambled-url=["']([^"']+)["']"#)
        let rawURL = href ?? scrambled.flatMap { Data(base64Encoded: $0) }.flatMap { String(data: $0, encoding: .utf8) }
        guard let rawURL, let url = URL(string: rawURL),
              url.scheme == "https", let host = url.host?.lowercased(),
              host.range(of: #"^download[0-9]+\.mediafire\.com$"#, options: .regularExpression) != nil else {
            throw HubProjectDownloadError.unavailable
        }
        return (url, (fileInfo["size"] as? String).flatMap(Int64.init))
    }

    private static func mediaFireQuickKey(in url: URL) -> String? {
        guard url.scheme == "https", isMediaFireHost(url.host) else { return nil }
        let parts = url.pathComponents
        guard parts.count >= 3, ["file", "download"].contains(parts[1]) else { return nil }
        let key = parts[2]
        guard (10...20).contains(key.count),
              key.unicodeScalars.allSatisfy({ CharacterSet.alphanumerics.contains($0) }) else { return nil }
        return key
    }

    private static func isMediaFireHost(_ host: String?) -> Bool {
        guard let host = host?.lowercased() else { return false }
        return ["mediafire.com", "www.mediafire.com", "m.mediafire.com"].contains(host)
    }

    private static func firstMatch(in text: String, pattern: String) -> String? {
        guard let expression = try? NSRegularExpression(pattern: pattern, options: [.dotMatchesLineSeparators]),
              let match = expression.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)),
              let range = Range(match.range, in: text) else { return nil }
        return String(text[range])
    }

    private static func firstCapture(in text: String, pattern: String) -> String? {
        guard let expression = try? NSRegularExpression(pattern: pattern),
              let match = expression.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)),
              let range = Range(match.range(at: 1), in: text) else { return nil }
        return String(text[range])
    }
}

private final class HubDownloadProgressDelegate: NSObject, URLSessionDownloadDelegate, @unchecked Sendable {
    let expectedSize: Int64?
    let onProgress: @Sendable (Double) -> Void
    private var completion: CheckedContinuation<(URL, URLResponse), Error>?
    private var session: URLSession?
    private var downloadedURL: URL?
    private var moveError: Error?

    init(expectedSize: Int64?, onProgress: @escaping @Sendable (Double) -> Void) {
        self.expectedSize = expectedSize
        self.onProgress = onProgress
    }

    func download(from url: URL) async throws -> (URL, URLResponse) {
        try await withCheckedThrowingContinuation { continuation in
            completion = continuation
            let session = URLSession(configuration: .default, delegate: self, delegateQueue: nil)
            self.session = session
            session.downloadTask(with: url).resume()
        }
    }

    func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didWriteData bytesWritten: Int64,
        totalBytesWritten: Int64,
        totalBytesExpectedToWrite: Int64
    ) {
        let total = totalBytesExpectedToWrite > 0 ? totalBytesExpectedToWrite : (expectedSize ?? 0)
        guard total > 0 else { return }
        onProgress(min(max(Double(totalBytesWritten) / Double(total), 0), 1))
    }

    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didFinishDownloadingTo location: URL) {
        let destination = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        do {
            try FileManager.default.moveItem(at: location, to: destination)
            downloadedURL = destination
        } catch {
            moveError = error
        }
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        defer {
            completion = nil
            self.session?.finishTasksAndInvalidate()
            self.session = nil
        }
        guard let completion else { return }
        if let error = error ?? moveError {
            completion.resume(throwing: error)
        } else if let downloadedURL, let response = task.response {
            completion.resume(returning: (downloadedURL, response))
        } else {
            completion.resume(throwing: HubProjectDownloadError.invalidResponse)
        }
    }
}

struct HubProjectImportPlan {
    let project: ComposeApp.HubProject
    let repository: HubRepository

    var externalDownloadURL: String? {
        guard project.overrideDownloadUrl == nil, project.downloadUrl == nil, project.packageName == nil else { return nil }
        let explicit = project.externalDownloadUrl?.trimmingCharacters(in: .whitespacesAndNewlines)
        return explicit?.isEmpty == false ? explicit : HubProjectDescription(project.description_).externalDownloadURL
    }

    var downloadURL: URL? {
        let value = project.overrideDownloadUrl ?? externalDownloadURL ?? project.downloadUrl
            ?? (project.packageName == nil ? nil : "/projects/\(project.id)/download")
        guard let value else { return nil }
        return URL(string: repository.client.resolveUrl(pathOrUrl: value))
    }

    var source: HubProjectDownloadUseCase.Source? {
        if let externalDownloadURL {
            guard let url = URL(string: externalDownloadURL) else { return nil }
            if HubProjectDownloadUseCase.supportsGoogleDrive(url) {
                return .googleDrive(url)
            }
            if HubProjectDownloadUseCase.supportsMediaFire(url) {
                return .mediaFire(url)
            }
            return nil
        }
        guard let url = downloadURL,
              let hubURL = URL(string: repository.client.resolveUrl(pathOrUrl: "/")),
              url.scheme == "https", url.host == hubURL.host, url.port == hubURL.port else { return nil }
        return .hub(url)
    }

    var canDownloadAndOpen: Bool {
        HubSettings.shared.ignoreCompatibility.value?.boolValue == true
            || project.projectType == .amethyst
            || project.compatibility == .compatible
            || project.overrideDownloadUrl != nil
    }

    var suggestedFilename: String {
        let fallbackExtension: String
        switch project.projectType {
        case .ableton: fallbackExtension = "als"
        case .apollo: fallbackExtension = "approj"
        case .unipad: fallbackExtension = "zip"
        default: fallbackExtension = "ame"
        }
        return project.overrideName ?? project.packageName ?? "\(project.title).\(fallbackExtension)"
    }

    var expectedSize: Int64? {
        project.overrideSize?.int64Value ?? project.packageSize?.int64Value
    }
}
