export function HowItWorks() {
  const steps = [
    {
      number: "01",
      title: "Wake Word",
      desc: 'Vosk detects your selected wake phrase offline using the compact speech model you install.',
    },
    {
      number: "02",
      title: "Speech-to-Text",
      desc: "Vosk streams audio → text in ~300ms. No audio leaves the device.",
    },
    {
      number: "03",
      title: "Intent Classification",
      desc: "Fast path (<500ms) for common commands. Complex requests → on-device LLM (Gemma/Phi).",
    },
    {
      number: "04",
      title: "Agent Loop",
      desc: "Observe screen → Plan action → Execute via Accessibility → Verify → Repeat (max 10 steps).",
    },
    {
      number: "05",
      title: "Text-to-Speech",
      desc: "Android TTS or Piper (offline neural) speaks result. Visual captions in accessibility mode.",
    },
    {
      number: "06",
      title: "Logged & Auditable",
      desc: "Every action logged with screen state hash, intent, result. Reviewable history in app.",
    },
  ]

  return (
    <section className="py-20 sm:py-28 bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-16">
          <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 mb-4">How It Works</h2>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto">
            Six steps from voice to action — all on your phone.
          </p>
        </div>
        <div className="relative">
          <div className="hidden lg:absolute left-1/2 top-0 bottom-0 w-0.5 bg-gray-200" style={{ left: '50%' }} />
          <div className="space-y-12 lg:space-y-20">
            {steps.map((step, i) => (
              <div key={i} className="relative flex flex-col lg:flex-row items-center gap-8">
                <div className="flex-shrink-0 w-16 h-16 rounded-full bg-primary-600 text-white flex items-center justify-center text-2xl font-bold z-10 lg:order-none">
                  {step.number}
                </div>
                <div className={`w-full lg:w-1/2 ${i % 2 === 0 ? 'lg:text-right lg:pr-8' : 'lg:pl-8'}`}>
                  <h3 className="text-xl font-semibold text-gray-900 mb-2">{step.title}</h3>
                  <p className="text-gray-600">{step.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
