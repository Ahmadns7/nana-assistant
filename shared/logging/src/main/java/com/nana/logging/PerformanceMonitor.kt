package com.nana.logging

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Performance monitoring for on-device ML inference and agent loops.
 */
class PerformanceMonitor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sampling = false
    private val samples = mutableListOf<PerformanceSample>()

    data class PerformanceSample(
        val timestamp: Long,
        val heapUsedMB: Double,
        val heapMaxMB: Double,
        val cpuTimeMs: Long,
        val activeThreads: Int,
        val label: String
    )

    fun startSampling(label: String = "default", intervalMs: Long = 100) {
        sampling = true
        scope.launch {
            while (sampling) {
                val sample = PerformanceSample(
                    timestamp = System.currentTimeMillis(),
                    heapUsedMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024.0),
                    heapMaxMB = Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0),
                    cpuTimeMs = 0L,
                    activeThreads = Thread.activeCount(),
                    label = label
                )
                samples.add(sample)
                Thread.sleep(intervalMs)
            }
        }
    }

    fun stopSampling(): List<PerformanceSample> {
        sampling = false
        return samples.toList()
    }

    fun getSummary(input: List<PerformanceSample> = samples): PerformanceSummary {
        val samples = input
        if (samples.isEmpty()) return PerformanceSummary(0.0, 0.0, 0.0, 0, 0)
        val heapUsed = samples.map { it.heapUsedMB }.average()
        val heapMax = samples.map { it.heapMaxMB }.maxOrNull() ?: 0.0
        val cpuTime = samples.map { it.cpuTimeMs }.average()
        val threads = samples.map { it.activeThreads }.average().toInt()
        return PerformanceSummary(heapUsed, heapMax, cpuTime, samples.size, threads)
    }

    data class PerformanceSummary(
        val avgHeapUsedMB: Double,
        val maxHeapMB: Double,
        val avgCpuTimeMs: Double,
        val sampleCount: Int,
        val avgThreads: Int
    )

    fun attachToLifecycle(owner: LifecycleOwner, label: String) {
        owner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> startSampling(label)
                Lifecycle.Event.ON_STOP -> stopSampling().also {
                    NanaLogger.logModelInference(label, 0, 0, 0, true)
                }
                else -> Unit
            }
        })
    }
}
