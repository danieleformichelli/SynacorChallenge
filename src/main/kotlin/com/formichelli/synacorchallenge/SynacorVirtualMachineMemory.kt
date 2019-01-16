package com.formichelli.synacorchallenge

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@ExperimentalUnsignedTypes
class SynacorVirtualMachineMemory(binaryFilePath: Path? = null) {
    private val memory = ByteBuffer.allocate((1.shl(15) + 8) * Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
    private val stack = Stack<Short>()
    val programSize: Int

    init {
        if (binaryFilePath == null) {
            programSize = -1
        } else {
            val programBytes = Files.readAllBytes(binaryFilePath)
            programSize = programBytes.size / Short.SIZE_BYTES
            memory.put(programBytes)
        }
    }

    fun set(address: Int, value: UShort) {
        memory.putShort(logicalToPhysicalAddress(address), value.toShort())
    }

    fun set(address: Number, value: Number) {
        set(address.toInt(), value.toInt().toUShort())
    }

    fun set(address: Number, value: UShort) {
        set(address.toInt(), value)
    }

    fun set(address: Number, value: UInt) {
        set(address.toInt(), value.toUShort())
    }

    fun set(address: UShort, value: Number) {
        set(address.toInt(), value.toInt().toUShort())
    }

    fun set(address: UShort, value: UShort) {
        set(address.toInt(), value.toUShort())
    }

    fun set(address: UShort, value: UInt) {
        set(address.toInt(), value.toUShort())
    }

    fun getRaw(address: Number) = memory.getShort(logicalToPhysicalAddress(address)).toUShort()

    fun get(address: Number): UShort {
        val valueFromMemory = getRaw(address)
        return if (valueFromMemory < OpCode.Modulo.toUShort()) {
            // numbers 0..32767 mean a literal value
            valueFromMemory
        } else {
            // numbers 32768..32775 instead mean registers 0..7
            getRaw(valueFromMemory.toInt())
        }
    }

    private fun logicalToPhysicalAddress(address: Number) = address.toInt() * Short.SIZE_BYTES

    fun push(value: Number) {
        stack.push(value.toShort())
    }

    fun push(value: UShort) {
        push(value.toShort())
    }

    fun pop() = stack.pop().toUShort()
}