package com.formichelli.synacorchallenge

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@ExperimentalUnsignedTypes
class SynacorVirtualMachineMemory {
    private val memory = ByteBuffer.allocate((1.shl(15) + 8) * Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
    private val stack = Stack<Short>()
    var programSize = -1

    fun loadProgram(binaryFilePath: Path) {
        val programBytes = Files.readAllBytes(binaryFilePath)
        programSize = programBytes.size / Short.SIZE_BYTES
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

    fun getAddress(address: Number) = memory.getShort(logicalToPhysicalAddress(address)).toUShort().toInt()

    fun get(address: Number): UShort {
        val valueFromMemory = memory.getShort(logicalToPhysicalAddress(address)).toUShort()
        return if (valueFromMemory < OpCode.Modulo.toUShort()) {
            // numbers 0..32767 mean a literal value
            valueFromMemory
        } else {
            // numbers 32768..32775 instead mean registers 0..7
            memory.getShort(logicalToPhysicalAddress(valueFromMemory)).toUShort()
        }
    }

    private fun logicalToPhysicalAddress(address: Number) = address.toInt() * Short.SIZE_BYTES
    private fun logicalToPhysicalAddress(address: UShort) = logicalToPhysicalAddress(address.toInt())

    fun push(value: Number) {
        stack.push(value.toShort())
    }

    fun push(value: UShort) {
        push(value.toShort())
    }

    fun pop() = stack.pop().toUShort()
}