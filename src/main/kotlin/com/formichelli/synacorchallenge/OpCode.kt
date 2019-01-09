package com.formichelli.synacorchallenge

import java.util.*

@ExperimentalUnsignedTypes
enum class OpCode(val code: Number) {
    HALT(0), SET(1), PUSH(2), POP(3), EQ(4), GT(5), JMP(6), JT(7), JF(8), ADD(9), MULT(10), MOD(11), AND(12), OR(13), NOT(14), RMEM(15), WMEM(16), CALL(17), RET(18), OUT(19), IN(20), NOOP(21);

    fun execute(memory: SynacorVirtualMachineMemory, instructionPointer: Int): Int = when (this) {
        // halt: 0 -> stop execution and terminate the program
        HALT -> -1
        // set: 1 a b -> set register <a> to the value of <b>
        SET -> {
            memory.set(memory.get(instructionPointer + 1), memory.get(instructionPointer + 2))
            instructionPointer + 3
        }
        // push: 2 a -> push <a> onto the stack
        PUSH -> {
            memory.push(memory.get(instructionPointer + 1))
            instructionPointer + 2
        }
        // pop: 3 a -> remove the top element from the stack and write it into <a>; empty stack = error
        POP -> {
            try {
                memory.set(memory.get(instructionPointer + 1), memory.pop())
                instructionPointer + 2
            } catch (e: EmptyStackException) {
                HALT.execute(memory, instructionPointer)
            }
        }
        // eq: 4 a b c -> set <a> to 1 if <b> is equal to <c>; set it to 0 otherwise
        EQ -> {
            memory.set(memory.get(instructionPointer + 1), if (memory.get(instructionPointer + 2) == memory.get(instructionPointer + 3)) 1 else 0)
            instructionPointer + 4
        }
        // gt: 5 a b c -> set <a> to 1 if <b> is greater than <c>; set it to 0 otherwise
        GT -> {
            memory.set(memory.get(instructionPointer + 1), if (memory.get(instructionPointer + 2) > memory.get(instructionPointer + 3)) 1 else 0)
            instructionPointer + 4
        }
        // jmp: 6 a -> jump to <a>
        JMP -> {
            memory.get(instructionPointer + 1).toInt()
        }
        // jt: 7 a b -> if <a> is nonzero, jump to <b>
        JT -> {
            if (memory.get(instructionPointer + 1) != 0.toUShort()) {
                memory.get(instructionPointer + 2).toInt()
            } else {
                instructionPointer + 3
            }
        }
        // jf: 8 a b -> if <a> is zero, jump to <b>
        JF ->
            if (memory.get(instructionPointer + 1) == 0.toUShort()) {
                memory.get(instructionPointer + 2).toInt()
            } else {
                instructionPointer + 3
            }
        // add: 9 a b c -> assign into <a> the sum of <b> and <c> (modulo 32768)
        ADD -> {
            memory.set(memory.get(instructionPointer + 1), ((memory.get(instructionPointer + 2).toInt() + memory.get(instructionPointer + 3).toInt()) % Modulo))
            instructionPointer + 4
        }
        // mult: 10 a b c -> store into <a> the product of <b> and <c> (modulo 32768)
        MULT -> {
            memory.set(memory.get(instructionPointer + 1), ((memory.get(instructionPointer + 2).toInt() * memory.get(instructionPointer + 3).toInt()) % Modulo))
            instructionPointer + 4
        }
        // mod: 11 a b c -> store into <a> the remainder of <b> divided by <c>
        MOD -> {
            memory.set(memory.get(instructionPointer + 1), memory.get(instructionPointer + 2) % memory.get(instructionPointer + 3))
            instructionPointer + 4
        }
        // and: 12 a b c -> stores into <a> the bitwise and of <b> and <c>
        AND -> {
            memory.set(memory.get(instructionPointer + 1), memory.get(instructionPointer + 2).and(memory.get(instructionPointer + 3)))
            instructionPointer + 4
        }
        // or: 13 a b c -> stores into <a> the bitwise or of <b> and <c>
        OR -> {
            memory.set(memory.get(instructionPointer + 1), memory.get(instructionPointer + 2).or(memory.get(instructionPointer + 3)))
            instructionPointer + 4
        }
        // not: 14 a b -> stores 15-bit bitwise inverse of <b> in <a>
        NOT -> {
            memory.set(memory.get(instructionPointer + 1), memory.get(instructionPointer + 2).inv().and(0x7FFF.toUShort()))
            instructionPointer + 3
        }
        // rmem: 15 a b -> read memory at address <b> and write it to <a>
        RMEM -> {
            memory.set(memory.get(instructionPointer + 1), memory.get(memory.get(instructionPointer + 2).toUInt().toInt()))
            instructionPointer + 3
        }
        // wmem: 16 a b -> write the value from <b> into memory at address <a>
        WMEM -> {
            memory.set(memory.get(instructionPointer + 1).toInt(), memory.get(instructionPointer + 2))
            instructionPointer + 3
        }
        // call: 17 a -> write the address of the next instruction to the stack and jump to <a>
        CALL -> {
            memory.push(instructionPointer + 2)
            memory.get(instructionPointer + 1).toInt()
        }
        // ret: 18 -> remove the top element from the stack and jump to it; empty stack = halt
        RET -> {
            try {
                memory.pop().toInt()
            } catch (e: EmptyStackException) {
                HALT.execute(memory, instructionPointer)
            }
        }
        // out: 19 a -> write the character represented by ascii code <a> to the terminal
        OUT -> {
            System.out.print(memory.get(instructionPointer + 1).toShort().toChar())
            instructionPointer + 2
        }
        // in: 20 a -> read a character from the terminal and write its ascii code to <a>; it can be assumed that once input starts, it will continue until a newline is encountered; this means that you can safely read whole lines from the keyboard and trust that they will be fully read
        IN -> {
            memory.set(memory.get(instructionPointer + 1).toInt(), nextChar().toShort())
            instructionPointer + 2
        }
        // noop: 21 -> no operation
        NOOP -> {
            instructionPointer + 1
        }
    }

    companion object {
        private val codeToOp = OpCode.values().map { it.code to it }.toMap()
        const val Modulo = 32678

        private var currentIndex = 0
        private var currentLine = ""

        private fun nextChar(): Char {
            if (currentIndex == currentLine.length) {
                currentLine = readLine()!!
                currentIndex = 0
            }

            return currentLine[currentIndex++]
        }

        fun fromCode(code: UShort) = fromCode(code.toShort())
        private fun fromCode(code: Number) = codeToOp[code.toInt()]
                ?: throw IllegalArgumentException("Invalid code $code")
    }
}