import Foundation
import Observation

@MainActor @Observable
final class Store {
    var data = AppData()
    var error: String?
    let url: URL

    init(url: URL = URL.documentsDirectory.appending(path: "balance.json")) {
        self.url = url
        load()
    }

    func load() {
        guard FileManager.default.fileExists(atPath: url.path) else { return }
        do {
            data = try JSONDecoder.balance.decode(AppData.self, from: Data(contentsOf: url))
        } catch {
            // Never overwrite a file we couldn't read: set it aside so the user can recover it.
            let backup = url.deletingPathExtension().appendingPathExtension("unreadable-\(Int(Date.now.timeIntervalSince1970)).json")
            try? FileManager.default.moveItem(at: url, to: backup)
            self.error = "Couldn't read balance.json. It was saved as \(backup.lastPathComponent)."
        }
    }

    func save() {
        do {
            try JSONEncoder.balance.encode(data).write(to: url, options: .atomic)
        } catch {
            self.error = "Couldn't save: \(error.localizedDescription)"
        }
    }

    func upsert(_ entry: Entry) {
        if let i = data.entries.firstIndex(where: { $0.id == entry.id }) { data.entries[i] = entry } else { data.entries.append(entry) }
        save()
    }

    func delete(_ entry: Entry) {
        data.entries.removeAll { $0.id == entry.id }
        if entry.source == .health { data.hiddenHealthIDs.insert(entry.id) }
        save()
    }

    func upsert(_ goal: Goal) {
        if let i = data.goals.firstIndex(where: { $0.id == goal.id }) { data.goals[i] = goal } else { data.goals.append(goal) }
        save()
    }

    func delete(_ goal: Goal) {
        data.goals.removeAll { $0.id == goal.id }
        save()
    }

    /// Replaces Health-sourced entries inside `interval` with a fresh import, so edits/deletions in Health carry over.
    func replaceHealthEntries(_ entries: [Entry], in interval: DateInterval) {
        data.entries.removeAll { $0.source == .health && interval.contains($0.date) }
        data.entries += entries.filter { !data.hiddenHealthIDs.contains($0.id) }
        save()
    }
}

extension JSONEncoder {
    static let balance: JSONEncoder = {
        let e = JSONEncoder()
        e.dateEncodingStrategy = .iso8601
        e.outputFormatting = [.prettyPrinted, .sortedKeys]
        return e
    }()
}

extension JSONDecoder {
    static let balance: JSONDecoder = {
        let d = JSONDecoder()
        d.dateDecodingStrategy = .iso8601
        return d
    }()
}
