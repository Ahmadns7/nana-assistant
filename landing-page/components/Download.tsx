export function Download() {
  const requirements = [
    "Android 10+ (API 29)",
    "3 GB+ RAM (6 GB+ recommended for LLM)",
    "2 GB free storage (models + app)",
    "Microphone permission",
    "Accessibility Service enabled",
    "Notification Access enabled (for triage)",
    "Display Over Apps (for visual indicator)",
  ]

  return (
    <section id="download" className="py-20 sm:py-28 bg-primary-600">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-12">
          <h2 className="text-3xl sm:text-4xl font-bold text-white mb-4">Get NaNa</h2>
          <p className="text-primary-100 text-lg max-w-2xl mx-auto">
            Download the latest APK from GitHub Releases. Models are downloaded on first run.
          </p>
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          <div className="bg-white/10 rounded-2xl p-8 backdrop-blur-sm">
            <h3 className="text-xl font-semibold text-white mb-6">Requirements</h3>
            <ul className="space-y-3">
              {requirements.map((req, i) => (
                <li key={i} className="flex items-center gap-3 text-primary-100">
                  <svg className="h-5 w-5 text-primary-300 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  {req}
                </li>
              ))}
            </ul>
          </div>
          <div className="text-center">
            <a
              href="https://github.com/releases"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center justify-center px-8 py-4 text-lg font-medium text-primary-600 bg-white rounded-lg hover:bg-primary-50 focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-primary-600 transition-colors"
            >
              <svg className="mr-2 h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
              </svg>
              Download Latest Release (APK)
            </a>
            <p className="mt-4 text-primary-200 text-sm">
              Or build from source: <code className="bg-white/10 px-2 py-1 rounded">./gradlew assembleDebug</code>
            </p>
            <div className="mt-8 grid grid-cols-3 gap-4 text-center">
              <div>
                <div className="text-2xl font-bold text-white">0</div>
                <div className="text-primary-200 text-sm">Network Permissions</div>
              </div>
              <div>
                <div className="text-2xl font-bold text-white">100%</div>
                <div className="text-primary-200 text-sm">On-Device</div>
              </div>
              <div>
                <div className="text-2xl font-bold text-white">MIT</div>
                <div className="text-primary-200 text-sm">License</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}