import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'

const inter = Inter({ subsets: ['latin'], variable: '--font-inter' })

export const metadata: Metadata = {
  title: 'NaNa Assistant - Offline Voice AI for Android',
  description: 'Control your phone entirely by voice. No internet required. Wake word, media control, messaging, screen reading, and more — all on-device.',
  openGraph: {
    title: 'NaNa Assistant',
    description: 'Fully offline voice-controlled AI phone assistant for Android',
    type: 'website',
  },
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en" className={`${inter.variable} font-sans antialiased`}>
      <body className="bg-white text-gray-900">{children}</body>
    </html>
  )
}