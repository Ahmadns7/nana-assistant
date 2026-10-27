export function TechStack() {
  const stack = [
    { category: "Wake Word", items: ["Porcupine 3.0", "hey_nana.ppn (50KB)"] },
    { category: "STT", items: ["Vosk 0.3.45", "vosk-model-small-en-us (40MB)"] },
    { category: "TTS", items: ["Android TTS (system)", "Piper (ONNX, 50MB)"] },
    { category: "LLM", items: ["llama.cpp / MediaPipe", "Gemma 3 1B / 2B (Q4_K_M)"] },
    { category: "Screen Control", items: ["AccessibilityService", "Media3 MediaController"] },
    { category: "OCR", items: ["ML Kit Text Recognition", "Bundled (20MB)"] },
    { category: "Storage", items: ["Room (SQLCipher)", "DataStore (preferences)"] },
    { category: "UI", items: ["Jetpack Compose", "Material 3"] },
  ]

  return (
    <section className="py-20 sm:py-28 bg-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-16">
          <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 mb-4">Tech Stack</h2>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto">
            Carefully chosen for on-device performance and offline operation.
          </p>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {stack.map((item, i) => (
            <div key={i} className="card">
              <h3 className="text-sm font-semibold text-primary-600 uppercase tracking-wider mb-3">
                {item.category}
              </h3>
              <ul className="space-y-2">
                {item.items.map((tech, j) => (
                  <li key={j} className="flex items-center gap-2 text-gray-600">
                    <svg className="h-4 w-4 text-primary-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                    {tech}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}