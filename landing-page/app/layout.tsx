import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'

const inter = Inter({ subsets: ['latin'], variable: '--font-inter' })

export const metadata: Metadata = {
  title: 'NaNa — Offline-first voice control for Android',
  description: 'Download small offline speech models, choose what stays on device, and use voice commands for everyday Android actions.',
  openGraph: {
    title: 'NaNa — Offline-first voice control for Android',
    description: 'Local speech recognition and Android-permissioned phone actions.',
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
