package com.formichelli.synacorchallenge

import java.util.*

@ExperimentalUnsignedTypes
enum class OpCode(val code: Number, val parametersCount: Int) {
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
            stringBuilder.append(' ').append(memory.getRaw(instructionPointer + parameterIndex))
        }

        return stringBuilder.toString()
    }

    fun execute(memory: SynacorVirtualMachineMemory, instructionPointer: Int): Int {
        when (this) {
            // halt: 0 -> stop execution and terminate the program
            HALT -> return halt()
            // set: 1 a b -> set register <a> to the value of <b>
            SET -> {
                val targetAddress = memory.getRaw(instructionPointer + 1)
                val value = memory.get(instructionPointer + 2)
                memory.set(targetAddress, value)
            }
            // push: 2 a -> push <a> onto the stack
            PUSH -> {
                val value = memory.get(instructionPointer + 1)
                memory.push(value)
            }
            // pop: 3 a -> remove the top element from the stack and write it into <a>; empty stack = error
            POP -> {
                try {
                    val targetAddress = memory.getRaw(instructionPointer + 1)
                    val value = memory.pop()
                    memory.set(targetAddress, value)
                } catch (e: EmptyStackException) {
                    return halt()
                }
            }
            // eq: 4 a b c -> set <a> to 1 if <b> is equal to <c>; set it to 0 otherwise
            EQ -> {
                val targetAddress = memory.getRaw(instructionPointer + 1)
                val b = memory.get(instructionPointer + 2)
                val c = memory.get(instructionPointer + 3)
                val value = if (b == c) 1 else 0
                memory.set(targetAddress, value)
            }
            // gt: 5 a b c -> set <a> to 1 if <b> is greater than <c>; set it to 0 otherwise
            GT -> {
                val targetAddress = memory.getRaw(instructionPointer + 1)
                val b = memory.get(instructionPointer + 2)
                val c = memory.get(instructionPointer + 3)
                val value = if (b > c) 1 else 0
                memory.set(targetAddress, value)
            }
            // jmp: 6 a -> jump to <a>
            JMP -> {
                val targetAddress = memory.get(instructionPointer + 1)
                return targetAddress.toInt()
            }
            // jt: 7 a b -> if <a> is nonzero, jump to <b>
            JT -> {
                val value = memory.get(instructionPointer + 1)
                if (value != 0.toUShort()) {
                    val targetAddress = memory.get(instructionPointer + 2)
                    return targetAddress.toInt()
                }
            }
            // jf: 8 a b -> if <a> is zero, jump to <b>
            JF -> {
                val value = memory.get(instructionPointer + 1)
                if (value == 0.toUShort()) {
                    val targetAddress = memory.get(instructionPointer + 2)
                    return targetAddress.toInt()
                }
            }
            // add: 9 a b c -> assign into <a> the sum of <b> and <c> (modulo 32768)
            ADD -> {
                val targetAddress = memory.getRaw(instructionPointer + 1)
                val b = memory.get(instructionPointer + 2)
                val c = memory.get(instructionPointer + 3)
                val sum = (b.toInt() + c.toInt()) % Modulo
                memory.set(targetAddress, sum)
            }
            // mult: 10 a b c -> store into <a> the product of <b> and <c> (modulo 32768)
            MULT -> {
                val targetAddress = memory.getRaw(instructionPointer + 1)
                val b = memory.get(instructionPointer + 2)
                val c = memory.get(instructionPointer + 3)
                val prod = (b.toInt() * c.toInt()) % Modulo
                memory.set(targetAddress, prod)
            }
            // mod: 11 a b c -> store into <a> the remainder of <b> divided by <c>
            MOD -> {
                val targetAddress = memory.getRaw(instructionPointer + 1)
                val b = memory.get(instructionPointer + 2)
                val c = memory.get(instructionPointer + 3)
                val mod = b % c
                memory.set(targetAddress, mod)
            }
            // and: 12 a b c -> stores into <a> the bitwise and of <b> and <c>
            AND -> {
                val targetAddress = memory.getRaw(instructionPointer + 1)
                val b = memory.get(instructionPointer + 2)
                val c = memory.get(instructionPointer + 3)
                val and = b.and(c)
                memory.set(targetAddress, and)
            }
            // or: 13 a b c -> stores into <a> the bitwise or of <b> and <c>
            OR -> {
                val targetAddress = memory.getRaw(instructionPointer + 1)
                val b = memory.get(instructionPointer + 2)
                val c = memory.get(instructionPointer + 3)
                val or = b.or(c)
                memory.set(targetAddress, or)
            }
            // not: 14 a b -> stores 15-bit bitwise inverse of <b> in <a>
            NOT -> {
                val targetAddress = memory.getRaw(instructionPointer + 1)
                val b = memory.get(instructionPointer + 2)
                val inv = b.inv().and(0x7FFF.toUShort())
                memory.set(targetAddress, inv)
            }
            // rmem: 15 a b -> read memory at address <b> and write it to <a>
            RMEM -> {
                val targetAddress = memory.getRaw(instructionPointer + 1)
                val value = memory.getRaw(memory.get(instructionPointer + 2).toUInt().toInt())
                memory.set(targetAddress, value)
            }
            // wmem: 16 a b -> write the value from <b> into memory at address <a>
            WMEM -> {
                val targetAddress = memory.get(instructionPointer + 1)
                val value = memory.get(instructionPointer + 2).toInt()
                memory.set(targetAddress, value)
            }
            // call: 17 a -> write the address of the next instruction to the stack and jump to <a>
            CALL -> {
                val targetAddress = memory.get(instructionPointer + 1)
                memory.push(instructionPointer + 2)
                return targetAddress.toInt()
            }
            // ret: 18 -> remove the top element from the stack and jump to it; empty stack = halt
            RET -> {
                return try {
                    val targetAddress = memory.pop()
                    return targetAddress.toInt()
                } catch (e: EmptyStackException) {
                    halt()
                }
            }
            // out: 19 a -> write the character represented by ascii code <a> to the terminal
            OUT -> {
                val value = memory.get(instructionPointer + 1)
                System.out.print(value.toShort().toChar())
            }
            // in: 20 a -> read a character from the terminal and write its ascii code to <a>; it can be assumed that once input starts, it will continue until a newline is encountered; this means that you can safely read whole lines from the keyboard and trust that they will be fully read
            IN -> {
                val targetAddress = memory.getRaw(instructionPointer + 1)
                val value = nextChar().toShort()
                memory.set(targetAddress, value)
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
                currentLine = readLine()!! + '\n'
                currentIndex = 0
            }

            val currentChar = currentLine[currentIndex++]
            return currentChar
        }

        fun fromCode(code: UShort) = fromCode(code.toShort())
        private fun fromCode(code: Number) = codeToOp[code.toInt()]
                ?: throw IllegalArgumentException("Invalid code $code")

        fun prefill(prefillInput: Collection<String>) {
            val lineBuilder = StringBuilder()
            prefillInput.forEach {
                lineBuilder.append(it).append('\n')
            }
            currentLine = lineBuilder.toString()
        }
    }
}