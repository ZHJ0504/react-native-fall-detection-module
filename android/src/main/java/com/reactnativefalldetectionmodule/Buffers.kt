package com.reactnativefalldetectionmodule

class Buffers(count: Int, size: Int, var position: Int, value: Double) {
    val buffers: Array<DoubleArray> = Array(count) { DoubleArray(size) { value } }
}
