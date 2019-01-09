package com.formichelli.synacorchallenge

import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

@ExperimentalUnsignedTypes
enum class OpCode {
    HALT, SET, PUSH, POP, EQ, GT, JMP, JT, JF, ADD, MULT, MOD, AND, OR, NOT, RMEM, WMEM, CALL, RET, OUT, IN, NOOP;

    fun execute(memory: ByteBuffer, stack: Stack<UShort>, instructionPointer: Int): Int {
        when (this) {
            // halt: 0 -> stop execution and terminate the program
            HALT -> return -1
            // set: 1 a b -> set register <a> to the value of <b>
            SET -> {
                memory.putShort((instructionPointer + 1), memory.getShort(instructionPointer + 2))
                return instructionPointer + 3
            }
            // push: 2 a -> push <a> onto the stack
            PUSH -> {
                stack.push(memory.getShort(instructionPointer + 1).toUShort())
                return instructionPointer + 2
            }
            // pop: 3 a -> remove the top element from the stack and write it into <a>; empty stack = error
            POP -> {
                if (stack.isEmpty()) {
                    return HALT.execute(memory, stack, instructionPointer)
                }

                memory.putShort(instructionPointer + 1, stack.pop().toShort())
                return instructionPointer + 2
            }
            // eq: 4 a b c -> set <a> to 1 if <b> is equal to <c>; set it to 0 otherwise
            EQ -> {
                memory.putShort((instructionPointer + 1), if (memory.getShort(instructionPointer + 2) == memory.getShort(instructionPointer + 3)) 1 else 0)
                return instructionPointer + 4
            }
            // gt: 5 a b c -> set <a> to 1 if <b> is greater than <c>; set it to 0 otherwise
            GT -> {
                memory.putShort((instructionPointer + 1), if (memory.getShort(instructionPointer + 2) > memory.getShort(instructionPointer + 3)) 1 else 0)
                return instructionPointer + 4
            }
            // jmp: 6 a -> jump to <a>
            JMP -> {
                return memory.getShort(instructionPointer + 1).toUShort().toInt()
            }
            // jt: 7 a b -> if <a> is nonzero, jump to <b>
            JT -> {
                return if (memory.getShort(instructionPointer + 1) != 0.toShort()) {
                    memory.getShort(instructionPointer + 2).toUShort().toInt()
                } else {
                    instructionPointer + 3
                }
            }
            // jf: 8 a b -> if <a> is zero, jump to <b>
            JF ->
                return if (memory.getShort(instructionPointer + 1) == 0.toShort()) {
                    memory.getShort(instructionPointer + 2).toUShort().toInt()
                } else {
                    instructionPointer + 3
                }
            // add: 9 a b c -> assign into <a> the sum of <b> and <c> (modulo 32768)
            ADD -> {
                memory.putShort(instructionPointer + 1, ((memory.getShort(instructionPointer + 2) + memory.getShort(instructionPointer + 3)) % Modulo).toShort())
                return instructionPointer + 4
            }
            // mult: 10 a b c -> store into <a> the product of <b> and <c> (modulo 32768)
            MULT -> {
                memory.putShort(instructionPointer + 1, ((memory.getShort(instructionPointer + 2) * memory.getShort(instructionPointer + 3)) % Modulo).toShort())
                return instructionPointer + 4
            }
            // mod: 11 a b c -> store into <a> the remainder of <b> divided by <c>
            MOD -> {
                memory.putShort(instructionPointer + 1, (memory.getShort(instructionPointer + 2) % memory.getShort(instructionPointer + 3)).toShort())
                return instructionPointer + 4
            }
            // and: 12 a b c -> stores into <a> the bitwise and of <b> and <c>
            AND -> {
                memory.putShort(instructionPointer + 1, memory.getShort(instructionPointer + 2).and(memory.getShort(instructionPointer + 3)))
                return instructionPointer + 4
            }
            // or: 13 a b c -> stores into <a> the bitwise or of <b> and <c>
            OR -> {
                memory.putShort(instructionPointer + 1, memory.getShort(instructionPointer + 2).or(memory.getShort(instructionPointer + 3)))
                return instructionPointer + 4
            }
            // not: 14 a b -> stores 15-bit bitwise inverse of <b> in <a>
            NOT -> {
                memory.putShort(instructionPointer + 1, memory.getShort(instructionPointer + 2).inv())
                return instructionPointer + 3
            }
            // rmem: 15 a b -> read memory at address <b> and write it to <a>
            RMEM -> {
                memory.putShort(instructionPointer + 1, memory.getShort(memory.getShort(instructionPointer + 2).toUInt().toInt()))
                return instructionPointer + 3
            }
            // wmem: 16 a b -> write the value from <b> into memory at address <a>
            WMEM -> {
                memory.putShort(memory.getShort(instructionPointer + 1).toUShort().toInt(), memory.getShort(instructionPointer + 2))
                return instructionPointer + 3
            }
            // call: 17 a -> write the address of the next instruction to the stack and jump to <a>
            CALL -> {
                stack.push((instructionPointer + 2).toUShort())
                return memory.getShort(instructionPointer + 1).toUShort().toInt()
            }
            // ret: 18 -> remove the top element from the stack and jump to it; empty stack = halt
            RET -> {
                if (stack.isEmpty()) {
                    return HALT.execute(memory, stack, instructionPointer)
                }

                return stack.pop().toInt()
            }
            // out: 19 a -> write the character represented by ascii code <a> to the terminal
            OUT -> {
                System.out.print(memory.getShort(instructionPointer + 1).toChar())
                return instructionPointer + 2
            }
            // in: 20 a -> read a character from the terminal and write its ascii code to <a>; it can be assumed that once input starts, it will continue until a newline is encountered; this means that you can safely read whole lines from the keyboard and trust that they will be fully read
            IN -> {
                memory.putShort(memory.getShort(instructionPointer + 1).toUShort().toInt(), nextChar().toShort())
                return instructionPointer + 2
            }
            // noop: 21 -> no operation
            NOOP -> {
                return instructionPointer + 1
            }
        }
    }

    companion object {
        private val codeToOp = mapOf(
                0.toShort() to HALT,
                1.toShort() to SET,
                2.toShort() to PUSH,
                3.toShort() to POP,
                4.toShort() to EQ,
                5.toShort() to GT,
                6.toShort() to JMP,
                7.toShort() to JT,
                8.toShort() to JF,
                9.toShort() to ADD,
                10.toShort() to MULT,
                11.toShort() to MOD,
                12.toShort() to AND,
                13.toShort() to OR,
                14.toShort() to NOT,
                15.toShort() to RMEM,
                16.toShort() to WMEM,
                17.toShort() to CALL,
                18.toShort() to RET,
                19.toShort() to OUT,
                20.toShort() to IN,
                21.toShort() to NOOP
        )

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

        fun fromCode(code: Short) = codeToOp[code] ?: throw IllegalArgumentException("Invalid code $code")
    }
}