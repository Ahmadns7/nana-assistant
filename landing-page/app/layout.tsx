import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'

const inter = Inter({ subsets: ['latin'], variable: '--font-inter' })

export const metadata: Metadata = {
  title: 'NaNa — Your phone, in your words.',
  description: 'A private, offline voice layer for Android. Tell NaNa what you need and keep your attention where it belongs.',
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
