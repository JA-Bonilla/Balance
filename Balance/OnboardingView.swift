import SwiftUI

struct OnboardingView: View {
    @Environment(Store.self) private var store
    @State private var picked: Category?
    @State private var minutes = 60

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
                        Stepper("\(minutes) min / week", value: $minutes, in: 15...1200, step: 15)
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
                        store.upsert(Goal(categoryID: category.id, weeklyMinutes: minutes))
                    }
                    .bold()
                }
                .navigationTitle("Weekly target")
            }
        }
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
