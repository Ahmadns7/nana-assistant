'use client'

import { useState } from 'react'

const releaseUrl = 'https://github.com/Ahmadns7/nana-assistant/releases/latest'
const apkUrl = 'https://github.com/Ahmadns7/nana-assistant/releases/latest/download/app-release.apk'

const commands = [
  { command: 'Open Maps', response: 'Opening Maps', group: 'Apps' },
  { command: 'Turn on flashlight', response: 'Flashlight on', group: 'Device' },
  { command: 'Set a timer for 5 minutes', response: 'Timer set for 5 minutes', group: 'Everyday' },
  { command: 'Pause music', response: 'Media paused', group: 'Media' },
]

function Brand() {
  return <a className="brand" href="#top" aria-label="NaNa home"><span className="brand-mark">N</span> NaNa</a>
}

function HomeScreenPreview() {
  return (
    <div className="phone-screen home-screen" role="img" aria-label="Preview of the NaNa home screen">
      <div className="phone-status"><span>9:41</span><span>••• ▰</span></div>
      <div className="screen-header"><div><strong>NaNa</strong><small>YOUR ON-DEVICE ASSISTANT</small></div><span className="screen-icon">⚙</span></div>
      <h3>A little help,<br />hands-free.</h3>
      <p className="screen-subtitle">Private tools for the things you do every day.</p>
      <div className="voice-panel">
        <span className="ready-indicator" /> <small>READY WHEN YOU ARE</small>
        <strong>What can I<br />help with?</strong>
        <button type="button" tabIndex={-1}>Start with your voice</button>
      </div>
      <div className="screen-section-label">EVERYDAY ESSENTIALS</div>
      <div className="screen-tools"><span>◉<small>Flashlight</small></span><span>♫<small>Now playing</small></span><span>⚡<small>Battery</small></span><span>⌁<small>Wi-Fi</small></span></div>
      <small className="screen-caption">NaNa home · Android</small>
    </div>
  )
}

function ModelsScreenPreview() {
  return (
    <div className="phone-screen settings-screen" role="img" aria-label="Preview of NaNa model settings">
      <div className="phone-status"><span>9:41</span><span>••• ▰</span></div>
      <div className="settings-title"><span>‹</span><strong>Settings</strong></div>
      <h3>Make NaNa yours.</h3>
      <p className="screen-subtitle">Choose how your private assistant listens and responds.</p>
      <div className="settings-block">
        <strong>Wake word</strong><small>Hands-free access</small>
        <div className="setting-row"><span><b>Hey NaNa detection</b><small>Listens while enabled</small></span><i>●</i></div>
        <div className="phrase-field"><small>Your wake phrase</small><b>Hey NaNa</b></div>
      </div>
      <div className="settings-block model-block">
        <strong>On-device models</strong><small>Download, select or remove</small>
        <div className="model-row"><span><b>English (US)</b><small>Vosk · 40 MB</small></span><em>Active</em></div>
        <div className="model-row"><span><b>Whisper tiny.en</b><small>English · 75 MiB</small></span><button type="button" tabIndex={-1}>Download</button></div>
        <div className="model-row"><span><b>Whisper tiny</b><small>Multilingual · 75 MiB</small></span><button type="button" tabIndex={-1}>Download</button></div>
      </div>
      <small className="screen-caption">Settings · model manager</small>
    </div>
  )
}

export default function Home() {
  const [activeCommand, setActiveCommand] = useState(0)

  return (
    <main className="site-shell">
      <nav className="nav-wrap" aria-label="Main navigation">
        <Brand />
        <div className="nav-links"><a href="#experience">What it does</a><a href="#models">Models</a><a href="#screens">Screens</a><a href="#download">Download</a></div>
        <a className="nav-cta" href={releaseUrl} target="_blank" rel="noreferrer">Latest release <span>↗</span></a>
      </nav>

      <section className="hero" id="top">
        <div className="hero-copy">
          <p className="eyebrow"><span className="eyebrow-dot" /> Offline-first voice control for Android</p>
          <h1>Your phone.<br /><em>In your words.</em></h1>
          <p className="hero-lede">NaNa turns short voice commands into everyday phone actions. Speech recognition runs on your phone with a model you choose.</p>
          <div className="hero-actions"><a className="button button-primary" href={apkUrl}>Download latest APK <span>↗</span></a><a className="text-link" href="#experience">See what it can do <span>↓</span></a></div>
          <p className="hero-note">Android 10+ · Code on GitHub · Model download is optional</p>
        </div>
        <div className="hero-visual" aria-label="NaNa app screen previews">
          <div className="orbit orbit-one" /><div className="orbit orbit-two" />
          <div className="preview-phone preview-phone-back"><ModelsScreenPreview /></div>
          <div className="preview-phone preview-phone-front"><HomeScreenPreview /></div>
          <div className="signal signal-top">Speech stays on device</div><div className="signal signal-bottom">Your models. Your choice.</div>
        </div>
        <div className="scroll-cue"><span>Scroll to explore</span><i /></div>
      </section>

      <section className="command-strip" id="experience"><p className="eyebrow">A more human interface</p><div className="command-row"><div><h2>Say less.<br /><em>Do more.</em></h2><p className="section-lede">Choose a phrase to see one of NaNa’s local command routes.</p></div><div className="command-demo"><div className="command-tabs" role="tablist" aria-label="Example voice commands">{commands.map((item, index) => <button key={item.command} type="button" role="tab" aria-selected={activeCommand === index} className={activeCommand === index ? 'active' : ''} onClick={() => setActiveCommand(index)}>{item.command}</button>)}</div><div className="command-result"><span className="play-dot" /><div><small>{commands[activeCommand].group} · handled on this phone</small><strong>{commands[activeCommand].response}</strong></div><span className="result-arrow">↗</span></div><p className="command-note">Wi-Fi, Bluetooth and other restricted controls open Android’s own settings for you to finish.</p></div></div></section>

      <section className="capabilities" id="privacy"><div className="section-heading"><p className="eyebrow">Built around your phone</p><h2>Useful by voice.<br /><em>Clear about access.</em></h2><p className="section-lede">Android decides what an app may do. NaNa uses direct actions where permitted and opens system controls when Android requires your input.</p></div><div className="capability-list">
        <article className="capability"><span className="cap-number">01</span><div><h3>Open your apps</h3><p>Say “open Maps” or “launch Spotify” to find an installed app and bring it up.</p></div><span className="cap-arrow">↗</span></article>
        <article className="capability"><span className="cap-number">02</span><div><h3>Handle daily actions</h3><p>Control the flashlight, music and media volume, check battery or storage, and set a timer.</p></div><span className="cap-arrow">↗</span></article>
        <article className="capability"><span className="cap-number">03</span><div><h3>Keep you in charge</h3><p>Calls open the dialer for review. Messages open your messaging app. Android permission pages stay yours to approve.</p></div><span className="cap-arrow">↗</span></article>
        <article className="capability"><span className="cap-number">04</span><div><h3>Listen locally</h3><p>Downloaded speech models process voice on the phone. NaNa does not send recordings to a speech cloud.</p></div><span className="cap-arrow">↗</span></article>
      </div></section>

      <section className="model-section" id="models"><div className="model-heading"><div><p className="eyebrow">Choose your speech model</p><h2>Small downloads.<br /><em>Local recognition.</em></h2></div><p className="section-lede">Download over Wi-Fi through Android Download Manager, then select or remove models in Settings. Speech models recognize words; they are not chat assistants.</p></div><div className="model-comparison">
        <article className="model-option"><span className="model-index">01 / WAKE WORD + COMMANDS</span><h3>Vosk small</h3><p>Compact language models for streaming offline speech and custom wake phrases.</p><div className="model-facts"><span>About 36–45 MB</span><span>English, Hindi, Spanish, French, German</span><span>Best fit for low-memory phones</span></div></article>
        <article className="model-option model-option-dark"><span className="model-index">02 / VOICE COMMANDS</span><h3>Whisper tiny</h3><p>Optional whisper.cpp model for foreground, short-form transcription.</p><div className="model-facts"><span>About 75 MiB per model</span><span>English-only or multilingual</span><span>Higher memory use; may be slower</span></div></article>
      </div><p className="model-footnote">Vosk continues to handle wake-word detection. Whisper transcribes voice commands after listening starts; tiny Whisper checkpoints may need roughly 390 MB of working memory. Keep Vosk selected on entry-level phones for a lighter experience.</p></section>

      <section className="screens-section" id="screens"><div className="screens-heading"><div><p className="eyebrow">Made for small screens</p><h2>Voice on the home screen.<br /><em>Control in Settings.</em></h2></div><p className="section-lede">A focused home screen for quick actions, with model and permission controls a tap away.</p></div><div className="screens-row"><figure><div className="screen-stage"><div className="screen-large"><HomeScreenPreview /></div></div><figcaption><span>01</span><div><strong>Home</strong><small>Talk, open apps and reach everyday tools.</small></div></figcaption></figure><figure><div className="screen-stage screen-stage-lilac"><div className="screen-large"><ModelsScreenPreview /></div></div><figcaption><span>02</span><div><strong>Model manager</strong><small>Download, select, cancel or remove speech models.</small></div></figcaption></figure></div><p className="screen-disclosure">Screen previews illustrate current NaNa layouts. Exact controls can vary with Android version, device maker and granted permissions.</p></section>

      <section className="download-section" id="download"><div><p className="eyebrow">NaNa for Android</p><h2>Start with the<br /><em>latest release.</em></h2></div><div className="download-side"><p>Install the APK, grant only the access you need, then choose a speech model in Settings. Model files download separately and can be removed any time.</p><a className="button button-light" href={apkUrl}>Download v1.0.4 APK <span>↗</span></a><a className="release-link" href={releaseUrl} target="_blank" rel="noreferrer">View all release files and notes ↗</a></div></section>

      <footer><Brand /><span>Offline speech. Android-permissioned actions.</span><a href="https://github.com/Ahmadns7/nana-assistant" target="_blank" rel="noreferrer">Source on GitHub ↗</a><span>© 2026 NaNa</span></footer>
    </main>
  )
}
