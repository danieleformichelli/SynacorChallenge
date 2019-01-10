package com.formichelli.synacorchallenge

import java.util.*

@ExperimentalUnsignedTypes
enum class OpCode(val code: Number, private val parametersCount: Int) {
    HALT(0, 0),
    SET(1, 2),
    PUSH(2, 1),
    POP(3, 1),
    EQ(4, 3),
    GT(5, 3),
    JMP(6, 1),
    JT(7, 2),
    JF(8, 2),
    ADD(9, 3),
    MULT(10, 3),
    MOD(11, 3),
    AND(12, 3),
    OR(13, 3),
    NOT(14, 2),
    RMEM(15, 2),
    WMEM(16, 2),
    CALL(17, 1),
    RET(18, 0),
    OUT(19, 1),
    IN(20, 1),
    NOOP(21, 0);

    fun toString(memory: SynacorVirtualMachineMemory, instructionPointer: Int): String {
        val stringBuilder = StringBuilder(this.name)
        for (parameterIndex in 1..parametersCount) {
            stringBuilder.append(' ').append(memory.getAddress(instructionPointer + parameterIndex))
        }

        return stringBuilder.toString()
    }

    fun execute(memory: SynacorVirtualMachineMemory, instructionPointer: Int): Int {
        when (this) {
            // halt: 0 -> stop execution and terminate the program
            HALT -> return halt()
            // set: 1 a b -> set register <a> to the value of <b>
            SET -> {
                memory.set(memory.getAddress(instructionPointer + 1), memory.get(instructionPointer + 2))
            }
            // push: 2 a -> push <a> onto the stack
            PUSH -> {
                memory.push(memory.get(instructionPointer + 1))
            }
            // pop: 3 a -> remove the top element from the stack and write it into <a>; empty stack = error
            POP -> {
                try {
                    memory.set(memory.getAddress(instructionPointer + 1), memory.pop())
                } catch (e: EmptyStackException) {
                    return halt()
                }
            }
            // eq: 4 a b c -> set <a> to 1 if <b> is equal to <c>; set it to 0 otherwise
            EQ -> {
                memory.set(memory.getAddress(instructionPointer + 1), if (memory.get(instructionPointer + 2) == memory.get(instructionPointer + 3)) 1 else 0)
            }
            // gt: 5 a b c -> set <a> to 1 if <b> is greater than <c>; set it to 0 otherwise
            GT -> {
                memory.set(memory.getAddress(instructionPointer + 1), if (memory.get(instructionPointer + 2) > memory.get(instructionPointer + 3)) 1 else 0)
            }
            // jmp: 6 a -> jump to <a>
            JMP -> {
                return memory.get(instructionPointer + 1).toInt()
            }
            // jt: 7 a b -> if <a> is nonzero, jump to <b>
            JT -> {
                if (memory.get(instructionPointer + 1) != 0.toUShort()) {
                    return memory.get(instructionPointer + 2).toInt()
                }
            }
            // jf: 8 a b -> if <a> is zero, jump to <b>
            JF ->
                if (memory.get(instructionPointer + 1) == 0.toUShort()) {
                    return memory.get(instructionPointer + 2).toInt()
                }
            // add: 9 a b c -> assign into <a> the sum of <b> and <c> (modulo 32768)
            ADD -> {
                memory.set(memory.getAddress(instructionPointer + 1), ((memory.get(instructionPointer + 2).toInt() + memory.get(instructionPointer + 3).toInt()) % Modulo))
            }
            // mult: 10 a b c -> store into <a> the product of <b> and <c> (modulo 32768)
            MULT -> {
                memory.set(memory.getAddress(instructionPointer + 1), ((memory.get(instructionPointer + 2).toInt() * memory.get(instructionPointer + 3).toInt()) % Modulo))
            }
            // mod: 11 a b c -> store into <a> the remainder of <b> divided by <c>
            MOD -> {
                memory.set(memory.getAddress(instructionPointer + 1), memory.get(instructionPointer + 2) % memory.get(instructionPointer + 3))
            }
            // and: 12 a b c -> stores into <a> the bitwise and of <b> and <c>
            AND -> {
                memory.set(memory.getAddress(instructionPointer + 1), memory.get(instructionPointer + 2).and(memory.get(instructionPointer + 3)))
            }
            // or: 13 a b c -> stores into <a> the bitwise or of <b> and <c>
            OR -> {
                memory.set(memory.getAddress(instructionPointer + 1), memory.get(instructionPointer + 2).or(memory.get(instructionPointer + 3)))
            }
            // not: 14 a b -> stores 15-bit bitwise inverse of <b> in <a>
            NOT -> {
                memory.set(memory.getAddress(instructionPointer + 1), memory.get(instructionPointer + 2).inv().and(0x7FFF.toUShort()))
            }
            // rmem: 15 a b -> read memory at address <b> and write it to <a>
            RMEM -> {
                memory.set(memory.getAddress(instructionPointer + 1), memory.get(memory.get(instructionPointer + 2).toUInt().toInt()))
            }
            // wmem: 16 a b -> write the value from <b> into memory at address <a>
            WMEM -> {
                memory.set(memory.getAddress(instructionPointer + 1), memory.get(instructionPointer + 2))
            }
            // call: 17 a -> write the address of the next instruction to the stack and jump to <a>
            CALL -> {
                memory.push(instructionPointer + 2)
                return memory.get(instructionPointer + 1).toInt()
            }
            // ret: 18 -> remove the top element from the stack and jump to it; empty stack = halt
            RET -> {
                return try {
                    memory.pop().toInt()
                } catch (e: EmptyStackException) {
                    halt()
                }
            }
            // out: 19 a -> write the character represented by ascii code <a> to the terminal
            OUT -> {
                System.out.print(memory.get(instructionPointer + 1).toShort().toChar())
            }
            // in: 20 a -> read a character from the terminal and write its ascii code to <a>; it can be assumed that once input starts, it will continue until a newline is encountered; this means that you can safely read whole lines from the keyboard and trust that they will be fully read
            IN -> {
                memory.set(memory.getAddress(instructionPointer + 1).toInt(), nextChar().toShort())
            }
            // noop: 21 -> no operation
            NOOP -> {
            }
        }

        return instructionPointer + parametersCount + 1
    }

    private fun halt() = -1

    companion object {
        private val codeToOp = OpCode.values().map { it.code to it }.toMap()
        const val Modulo = 32768

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