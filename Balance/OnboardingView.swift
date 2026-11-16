import SwiftUI

struct OnboardingView: View {
    @Environment(Store.self) private var store
    @State private var picked: Category?
    @State private var perDay = 15
    @State private var days = 5

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("What do you want to grow?").font(.largeTitle.bold())
                        Text("Pick just one to start. You can add more later.").foregroundStyle(.secondary)
                    }
                    CategoryGrid(categories: Category.growth, selection: $picked)
                }
                .padding()
            }
            .navigationDestination(item: $picked) { category in
                Form {
                    Section {
                        TargetFields(perDay: $perDay, days: $days)
                    } header: {
                        Label(category.name, systemImage: category.symbol)
                    } footer: {
                        Text("Start with something doable. You can change it anytime.")
                    }
                    Section {
                        Text("We'll also track fun, so you can see the balance between growing and enjoying yourself.")
                            .foregroundStyle(.secondary)
                    }
                    Button("Start") {
                        store.upsert(Goal(categoryID: category.id, minutesPerDay: perDay, daysPerWeek: days))
                    }
                    .bold()
                }
                .navigationTitle("Weekly target")
            }
        }
    }
}

/// Minutes per day × days per week; the weekly target is derived.
struct TargetFields: View {
    @Binding var perDay: Int
    @Binding var days: Int

    var body: some View {
        Stepper("\(perDay) min a day", value: $perDay, in: 5...240, step: 5)
        Stepper("\(days) day\(days == 1 ? "" : "s") a week", value: $days, in: 1...7)
        LabeledContent("Weekly target", value: (perDay * days).minutesText)
    }
}

struct CategoryGrid: View {
    let categories: [Category]
    @Binding var selection: Category?

    var body: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 150), spacing: 12)], spacing: 12) {
            ForEach(categories) { c in
                Button { selection = c } label: {
                    VStack(alignment: .leading, spacing: 6) {
                        Image(systemName: c.symbol).font(.title2).foregroundStyle(Color.side(c.side))
                        Text(c.name).font(.headline)
                        Text(c.blurb).font(.caption).foregroundStyle(.secondary).multilineTextAlignment(.leading)
                    }
                    .frame(maxWidth: .infinity, minHeight: 100, alignment: .topLeading)
                    .padding(12)
                    .background(.fill.tertiary, in: .rect(cornerRadius: 14))
                }
                .buttonStyle(.plain)
            }
        }
    }
}

#Preview { OnboardingView().environment(Store(url: .temporaryDirectory.appending(path: "preview.json"))) }
