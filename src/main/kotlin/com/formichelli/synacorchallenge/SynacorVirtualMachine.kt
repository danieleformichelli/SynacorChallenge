package com.formichelli.synacorchallenge

import java.nio.file.Path

@ExperimentalUnsignedTypes
class SynacorVirtualMachine {
    fun run(binaryFilePath: Path) {
        val memory = SynacorVirtualMachineMemory()
        memory.loadProgram(binaryFilePath)

        var instructionsCount = 0
        var instructionPointer = 0
        while (instructionPointer != -1) {
            ++instructionsCount
            val opCode = OpCode.fromCode(memory.get(instructionPointer))
            // System.err.println("$instructionPointer: ${opCode.toString(memory, instructionPointer)}")
            instructionPointer = opCode.execute(memory, instructionPointer)
        }
    }

    fun dumpBinary(binaryFilePath: Path) {
        val memory = SynacorVirtualMachineMemory()
        memory.loadProgram(binaryFilePath)

        var instructionPointer = 0
        while (instructionPointer < memory.programSize) {
            try {
                val opCode = OpCode.fromCode(memory.get(instructionPointer))
                System.out.println("$instructionPointer: ${opCode.toString(memory, instructionPointer)}")
                instructionPointer += opCode.parametersCount + 1
            } catch (e: IllegalArgumentException) {
                System.out.println("$instructionPointer: INVALID CODE ${memory.get(instructionPointer)}")
                ++instructionPointer
            }
        }
    }
}
