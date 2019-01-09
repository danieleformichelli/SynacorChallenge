package com.formichelli.synacorchallenge

import java.nio.file.Path

@ExperimentalUnsignedTypes
class SynacorVirtualMachine {
    fun run(binaryFilePath: Path) {
        val memory = SynacorVirtualMachineMemory()
        memory.loadProgram(binaryFilePath)

        var instructionPointer = 0
        while (instructionPointer != -1) {
            val opCode = OpCode.fromCode(memory.get(instructionPointer))
            System.err.println("$instructionPointer: ${opCode.toString(memory, instructionPointer)}")
            instructionPointer = opCode.execute(memory, instructionPointer)
        }
    }
}
