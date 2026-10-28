'use client'

import { useState } from 'react'

const capabilities = [
  ['01', 'Voice, not taps', 'Wake NaNa from anywhere on your phone and speak naturally.'],
  ['02', 'Private by design', 'No cloud calls, no tracking, no accounts. Your commands stay yours.'],
  ['03', 'Actions that finish', 'From a message to a macro, NaNa turns intent into a verified action.'],
]

const commands = ['What\'s playing?', 'Read my notifications', 'Good night']

export default function Home() {
  const [activeCommand, setActiveCommand] = useState(commands[0])
  const [isListening, setIsListening] = useState(false)

  return (
    <main className="site-shell">
      <nav className="nav-wrap">
        <a className="brand" href="#top" aria-label="NaNa home"><span className="brand-mark">N</span> NaNa</a>
        <div className="nav-links"><a href="#experience">Experience</a><a href="#privacy">Privacy</a><a href="#download">Download</a></div>
        <a className="nav-cta" href="#download">Get NaNa <span>↗</span></a>
      </nav>

      <section className="hero" id="top">
        <div className="hero-copy">
          <p className="eyebrow"><span className="eyebrow-dot" /> Offline intelligence for everyday life</p>
          <h1>Your phone.<br /><em>In your words.</em></h1>
          <p className="hero-lede">NaNa is a calm, private voice layer for Android. Tell it what you need. It understands the screen, takes the action, and gets out of the way.</p>
          <div className="hero-actions"><a className="button button-primary" href="#download">Download for Android <span>↗</span></a><a className="text-link" href="#experience">Explore the experience <span>↓</span></a></div>
        </div>
        <div className="hero-visual" aria-label="Interactive visualization of NaNa voice assistant">
          <div className="orbit orbit-one" /><div className="orbit orbit-two" />
          <div className="orbital-core" onClick={() => setIsListening(!isListening)} role="button" tabIndex={0} aria-label={isListening ? 'Stop listening' : 'Start listening'}>
            <div className="core-glow" /><div className="core-face"><span className="core-symbol">{isListening ? '•••' : 'N'}</span><span className="core-label">{isListening ? 'Listening' : 'NaNa'}</span></div>
          </div>
          <div className="signal signal-top">{isListening ? 'Listening for your voice' : 'Tap to talk'}</div><div className="signal signal-bottom">100% on-device</div>
        </div>
        <div className="scroll-cue"><span>Scroll to explore</span><i /></div>
      </section>

      <section className="command-strip" id="experience"><p className="eyebrow">A more human interface</p><div className="command-row"><h2>Say less.<br /><em>Do more.</em></h2><div className="command-demo"><div className="command-tabs">{commands.map((command) => <button key={command} className={activeCommand === command ? 'active' : ''} onClick={() => setActiveCommand(command)}>{command}</button>)}</div><div className="command-result"><span className="play-dot" /><div><small>NaNa understood</small><strong>{activeCommand}</strong></div><span className="result-arrow">↗</span></div></div></div></section>

      <section className="capabilities" id="privacy"><div className="section-heading"><p className="eyebrow">The NaNa principle</p><h2>Quietly capable.<br /><em>Radically private.</em></h2></div><div className="capability-list">{capabilities.map(([number, title, copy]) => <article className="capability" key={number}><span className="cap-number">{number}</span><div><h3>{title}</h3><p>{copy}</p></div><span className="cap-arrow">↗</span></article>)}</div></section>

      <section className="download-section" id="download"><div><p className="eyebrow">A new way to use Android</p><h2>Meet the assistant<br /><em>that listens.</em></h2></div><div className="download-side"><p>Open source. Offline first. Built for the moments when your hands are busy and your attention matters.</p><a className="button button-light" href="#top">Download APK <span>↗</span></a></div></section>

      <footer><a className="brand" href="#top"><span className="brand-mark">N</span> NaNa</a><span>Built for privacy, designed for voice.</span><span>© 2026 NaNa</span></footer>
    </main>
  )
}
