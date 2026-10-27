import Link from 'next/link'
import { Features } from '@/components/Features'
import { HowItWorks } from '@/components/HowItWorks'
import { Download } from '@/components/Download'
import { TechStack } from '@/components/TechStack'

export default function Home() {
  return (
    <main className="min-h-screen">
      {/* Hero */}
      <section className="relative overflow-hidden bg-gradient-to-b from-primary-50 to-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 sm:py-32">
          <div className="text-center max-w-3xl mx-auto">
            <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-primary-50 text-primary-700 text-sm font-medium mb-6">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-primary-500"></span>
              </span>
              Fully Offline · No Internet · Privacy First
            </div>
            <h1 className="text-4xl sm:text-6xl font-bold tracking-tight text-gray-900 mb-6">
              Control Your Phone{' '}
              <span className="text-primary-600">Entirely by Voice</span>
            </h1>
            <p className="text-lg sm:text-xl text-gray-600 mb-10 max-w-2xl mx-auto">
              NaNa is a fully offline voice assistant for Android. Wake word, media control,
              messaging, screen reading, app navigation — all on-device, zero cloud calls.
            </p>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-16">
              <Link href="#download" className="btn-primary w-full sm:w-auto">
                <svg className="mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                </svg>
                Download APK
              </Link>
              <Link href="https://github.com" target="_blank" rel="noopener noreferrer" className="btn-secondary w-full sm:w-auto">
                <svg className="mr-2 h-5 w-5" fill="currentColor" viewBox="0 0 24 24">
                  <path fillRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" clipRule="evenodd" />
                </svg>
                View Source
              </Link>
            </div>
            <div className="flex items-center justify-center gap-8 text-sm text-gray-500">
              <div className="flex items-center gap-1">
                <svg className="h-5 w-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>Zero Network Permission</span>
              </div>
              <div className="flex items-center gap-1">
                <svg className="h-5 w-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>On-Device LLM</span>
              </div>
              <div className="flex items-center gap-1">
                <svg className="h-5 w-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>Action Audit Log</span>
              </div>
            </div>
          </div>
        </div>
        {/* Phone mockup */}
        <div className="absolute bottom-0 left-1/2 -translate-x-1/2 translate-y-1/2 hidden lg:block">
          <div className="relative w-72 h-[150px] bg-gray-900 rounded-[40px] p-2 shadow-2xl">
            <div className="w-full h-full bg-gray-800 rounded-[38px] flex flex-col items-center justify-center p-4">
              <div className="w-full max-w-xs space-y-3">
                <div className="bg-primary-600/20 rounded-xl p-4 text-center">
                  <p className="text-primary-400 font-mono text-sm">"Hey NaNa, what's playing?"</p>
                </div>
                <div className="bg-green-600/20 rounded-xl p-4 text-center">
                  <p className="text-green-400 font-mono text-sm">"Now playing: Blinding Lights by The Weeknd from Spotify"</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <Features />

      {/* How It Works */}
      <HowItWorks />

      {/* Tech Stack */}
      <TechStack />

      {/* Download */}
      <Download />

      {/* Footer */}
      <footer className="bg-gray-900 text-gray-400 py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <p className="mb-4">NaNa Assistant — Built for privacy, designed for voice.</p>
          <div className="flex items-center justify-center gap-6 text-sm">
            <Link href="https://github.com" target="_blank" rel="noopener noreferrer" className="hover:text-white">GitHub</Link>
            <Link href="#download" className="hover:text-white">Releases</Link>
            <Link href="https://github.com/issues" target="_blank" rel="noopener noreferrer" className="hover:text-white">Issues</Link>
            <span>MIT License</span>
          </div>
        </div>
      </footer>
    </main>
  )
}