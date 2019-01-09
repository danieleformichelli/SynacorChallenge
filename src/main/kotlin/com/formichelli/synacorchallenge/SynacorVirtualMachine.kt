package com.formichelli.synacorchallenge

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@ExperimentalUnsignedTypes
class SynacorVirtualMachine {
    private val memory = ByteBuffer.allocate(1.shl(16)).order(ByteOrder.LITTLE_ENDIAN)
    private val stack = Stack<UShort>()

    fun run(binaryFilePath: Path) {
        val programBytes = Files.readAllBytes(binaryFilePath)
        memory.put(programBytes)

        var instructionPointer = 0
        while (instructionPointer != -1) {
            val opCode = OpCode.fromCode(memory.getShort(instructionPointer))
            instructionPointer = opCode.execute(memory, stack, instructionPointer)
        }
    }
}
