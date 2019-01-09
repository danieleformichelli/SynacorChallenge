package com.formichelli.synacorchallenge

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@ExperimentalUnsignedTypes
class SynacorVirtualMachineMemory {
    private val memory = ByteBuffer.allocate(1.shl(16)).order(ByteOrder.LITTLE_ENDIAN)
    private val stack = Stack<Short>()

    fun loadProgram(binaryFilePath: Path) {
        val programBytes = Files.readAllBytes(binaryFilePath)
        memory.put(programBytes)
    }

    fun set(address: Number, value: Number) {
        memory.putShort(logicalToPhysicalAddress(address), value.toShort())
    }

    fun set(address: Number, value: UShort) {
        set(address, value.toShort())
    }

    fun set(address: Number, value: UInt) {
        set(address, value.toShort())
    }

    fun set(address: UShort, value: Number) {
        set(address.toShort(), value)
    }

    fun set(address: UShort, value: UShort) {
        set(address.toShort(), value.toShort())
    }

    fun set(address: UShort, value: UInt) {
        set(address.toShort(), value.toShort())
    }

    fun get(address: Number) = memory.getShort(logicalToPhysicalAddress(address)).toUShort()

    private fun logicalToPhysicalAddress(address: Number) = address.toInt() * Short.SIZE_BYTES

    fun push(value: Number) {
        stack.push(value.toShort())
    }

    fun push(value: UShort) {
        push(value.toShort())
    }

    fun pop() = stack.pop().toUShort()
}