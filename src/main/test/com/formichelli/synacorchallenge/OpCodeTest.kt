package com.formichelli.synacorchallenge

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@ExperimentalUnsignedTypes
class OpCodeTest {
    private val memory = SynacorVirtualMachineMemory()
    private var instructionPointer: Int = 0

    @Before
    fun setUp() {
        instructionPointer = 0
    }

    @Test
    fun haltTest() {
        // halt: 0 -> stop execution and terminate the program
        memory.set(0, OpCode.HALT.code.toInt())
        Assert.assertEquals("HALT should return -1", -1, executeNext())
    }

    @Test
    fun setTest() {
        // set: 1 a b -> set register <a> to the value of <b>
        val targetAddress = 1
        val targetValue = 2.toUShort()
        memory.set(0, OpCode.SET.code.toInt())
        memory.set(1, targetAddress)
        memory.set(2, targetValue)
        val expectedInstructionPointer = instructionPointer + 3
        Assert.assertEquals("SET should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        Assert.assertEquals("SET should have set memory address $targetAddress to $targetValue", targetValue, memory.get(targetAddress))
    }

    @Test
    fun stackTest() {
        // push: 2 a -> push <a> onto the stack
        // pop: 3 a -> remove the top element from the stack and write it into <a>; empty stack = error
        val targetAddress = 0
        val pushedValue = 10.toUShort()
        memory.set(0, OpCode.PUSH.code.toInt())
        memory.set(1, pushedValue)
        memory.set(2, OpCode.POP.code.toInt())
        memory.set(3, targetAddress)
        memory.set(4, OpCode.POP.code.toInt())
        var expectedInstructionPointer = instructionPointer + 2
        Assert.assertEquals("PUSH should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        expectedInstructionPointer = instructionPointer + 2
        Assert.assertEquals("POP should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        Assert.assertEquals("POP should have set memory address $targetAddress to $pushedValue", pushedValue, memory.get(targetAddress))
    }

    @Test
    fun eqTest() {
        // eq: 4 a b c -> set <a> to 1 if <b> is equal to <c>; set it to 0 otherwise
        val targetAddress = 1
        val targetValue = 2.toUShort()
        memory.set(0, OpCode.EQ.code.toInt())
        memory.set(1, targetAddress)
        memory.set(2, targetValue)
        memory.set(3, targetValue)
        memory.set(4, OpCode.EQ.code.toInt())
        memory.set(5, targetAddress)
        memory.set(6, targetValue)
        memory.set(7, targetValue + 1.toUShort())
        var expectedInstructionPointer = instructionPointer + 4
        Assert.assertEquals("EQ should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        Assert.assertEquals("EQ with b == c should have set memory address $targetAddress to 1", 1.toUShort(), memory.get(targetAddress))
        expectedInstructionPointer = instructionPointer + 4
        Assert.assertEquals("EQ should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        Assert.assertEquals("EQ with b != c should have set memory address $targetAddress to 0", 0.toUShort(), memory.get(targetAddress))
    }

    @Test
    fun gtTest() {
        // gt: 5 a b c -> set <a> to 1 if <b> is greater than <c>; set it to 0 otherwise
        val targetAddress = 1
        val targetValue = 2.toUShort()
        memory.set(0, OpCode.GT.code.toInt())
        memory.set(1, targetAddress)
        memory.set(2, targetValue)
        memory.set(3, targetValue - 1.toUShort())
        memory.set(4, OpCode.GT.code.toInt())
        memory.set(5, targetAddress)
        memory.set(6, targetValue)
        memory.set(7, targetValue)
        memory.set(8, OpCode.GT.code.toInt())
        memory.set(9, targetAddress)
        memory.set(10, targetValue)
        memory.set(11, targetValue + 1.toUShort())
        var expectedInstructionPointer = instructionPointer + 4
        Assert.assertEquals("GT should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        Assert.assertEquals("GT with b > c should have set memory address $targetAddress to 1", 1.toUShort(), memory.get(targetAddress))
        expectedInstructionPointer = instructionPointer + 4
        Assert.assertEquals("GT should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        Assert.assertEquals("GT with b == c should have set memory address $targetAddress to 0", 0.toUShort(), memory.get(targetAddress))
        expectedInstructionPointer = instructionPointer + 4
        Assert.assertEquals("GT should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        Assert.assertEquals("GT with b < c should have set memory address $targetAddress to 0", 0.toUShort(), memory.get(targetAddress))
    }

    @Test
    fun jmpTest() {
        // jmp: 6 a -> jump to <a>
        val targetAddress = 100
        memory.set(0, OpCode.JMP.code.toInt())
        memory.set(1, targetAddress)
        Assert.assertEquals("JMP should return $targetAddress", targetAddress, executeNext())
    }

    @Test
    fun jtTest() {
        // jt: 7 a b -> if <a> is nonzero, jump to <b>
        val targetAddress = 100
        memory.set(0, OpCode.JT.code.toInt())
        memory.set(1, 0)
        memory.set(2, targetAddress)
        memory.set(3, OpCode.JT.code.toInt())
        memory.set(4, 1)
        memory.set(5, targetAddress)
        var expectedInstructionPointer = instructionPointer + 3
        Assert.assertEquals("JT should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        expectedInstructionPointer = targetAddress
        Assert.assertEquals("JT should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
    }

    @Test
    fun jfTest() {
        // jf: 8 a b -> if <a> is zero, jump to <b>
        val targetAddress = 100
        memory.set(0, OpCode.JF.code.toInt())
        memory.set(1, 1)
        memory.set(2, targetAddress)
        memory.set(3, OpCode.JF.code.toInt())
        memory.set(4, 0)
        memory.set(5, targetAddress)
        var expectedInstructionPointer = instructionPointer + 3
        Assert.assertEquals("JF should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        expectedInstructionPointer = targetAddress
        Assert.assertEquals("JF should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
    }

    @Test
    fun addTest() {
        // add: 9 a b c -> assign into <a> the sum of <b> and <c> (modulo 32768)
        val targetAddress = 100
        val b1 = 10
        val c1 = 20
        val b2 = 32000
        val c2 = 800
        memory.set(0, OpCode.ADD.code.toInt())
        memory.set(1, targetAddress)
        memory.set(2, b1)
        memory.set(3, c1)
        memory.set(4, OpCode.ADD.code.toInt())
        memory.set(5, targetAddress)
        memory.set(6, b2)
        memory.set(7, c2)
        var expectedInstructionPointer = instructionPointer + 4
        Assert.assertEquals("ADD should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        var expectedResult = ((b1 + c1) % OpCode.Modulo).toUShort()
        Assert.assertEquals("ADD should have set memory address $targetAddress to $expectedResult", expectedResult, memory.get(targetAddress))
        expectedInstructionPointer = instructionPointer + 4
        expectedResult = ((b2 + c2) % OpCode.Modulo).toUShort()
        Assert.assertEquals("ADD should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        Assert.assertEquals("ADD should have set memory address $targetAddress to $expectedResult", expectedResult, memory.get(targetAddress))
    }

    @Test
    fun multTest() {
        // mult: 10 a b c -> store into <a> the product of <b> and <c> (modulo 32768)
        val targetAddress = 100
        val b1 = 10
        val c1 = 20
        val b2 = 32000
        val c2 = 800
        memory.set(0, OpCode.MULT.code.toInt())
        memory.set(1, targetAddress)
        memory.set(2, b1)
        memory.set(3, c1)
        memory.set(4, OpCode.MULT.code.toInt())
        memory.set(5, targetAddress)
        memory.set(6, b2)
        memory.set(7, c2)
        var expectedInstructionPointer = instructionPointer + 4
        Assert.assertEquals("MULT should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        var expectedResult = ((b1 * c1) % OpCode.Modulo).toUShort()
        Assert.assertEquals("MULT should have set memory address $targetAddress to $expectedResult", expectedResult, memory.get(targetAddress))
        expectedInstructionPointer = instructionPointer + 4
        expectedResult = ((b2 * c2) % OpCode.Modulo).toUShort()
        Assert.assertEquals("MULT should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        Assert.assertEquals("MULT should have set memory address $targetAddress to $expectedResult", expectedResult, memory.get(targetAddress))
    }

    @Test
    fun modTest() {
        // mod: 11 a b c -> store into <a> the remainder of <b> divided by <c>
        val targetAddress = 100
        val b = 100
        val c = 31
        memory.set(0, OpCode.MOD.code.toInt())
        memory.set(1, targetAddress)
        memory.set(2, b)
        memory.set(3, c)
        val expectedInstructionPointer = instructionPointer + 4
        Assert.assertEquals("MOD should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        val expectedResult = ((b % c) % OpCode.Modulo).toUShort()
        Assert.assertEquals("MOD should have set memory address $targetAddress to $expectedResult", expectedResult, memory.get(targetAddress))
    }

    @Test
    fun andTest() {
        // and: 12 a b c -> stores into <a> the bitwise and of <b> and <c>
        val targetAddress = 100
        val b = 100
        val c = 31
        memory.set(0, OpCode.AND.code.toInt())
        memory.set(1, targetAddress)
        memory.set(2, b)
        memory.set(3, c)
        val expectedInstructionPointer = instructionPointer + 4
        Assert.assertEquals("AND should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        val expectedResult = b.and(c).toUShort()
        Assert.assertEquals("AND should have set memory address $targetAddress to $expectedResult", expectedResult, memory.get(targetAddress))
    }

    @Test
    fun orTest() {
        // or: 13 a b c -> stores into <a> the bitwise or of <b> and <c>
        val targetAddress = 100
        val b = 100
        val c = 31
        memory.set(0, OpCode.OR.code.toInt())
        memory.set(1, targetAddress)
        memory.set(2, b)
        memory.set(3, c)
        val expectedInstructionPointer = instructionPointer + 4
        Assert.assertEquals("OR should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        val expectedResult = b.or(c).toUShort()
        Assert.assertEquals("OR should have set memory address $targetAddress to $expectedResult", expectedResult, memory.get(targetAddress))
    }

    @Test
    fun notTest() {
        // not: 14 a b -> stores 15-bit bitwise inverse of <b> in <a>
        val targetAddress = 100
        val b = 100
        memory.set(0, OpCode.NOT.code.toInt())
        memory.set(1, targetAddress)
        memory.set(2, b)
        val expectedInstructionPointer = instructionPointer + 3
        Assert.assertEquals("NOT should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        val expectedResult = b.inv().toUShort().and(0x7FFF.toUShort())
        Assert.assertEquals("NOT should have set memory address $targetAddress to $expectedResult", expectedResult, memory.get(targetAddress))
    }

    @Test
    fun rmemTest() {
        // rmem: 15 a b -> read memory at address <b> and write it to <a>
        val sourceAddress = 100
        val targetAddress = 200
        val targetValue = 300.toUShort()
        memory.set(0, OpCode.RMEM.code.toInt())
        memory.set(1, targetAddress)
        memory.set(2, sourceAddress)
        memory.set(sourceAddress, targetValue)
        val expectedInstructionPointer = instructionPointer + 3
        Assert.assertEquals("RMEM should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        Assert.assertEquals("RMEM should have set memory address $targetAddress to $targetValue", targetValue, memory.get(targetAddress))
    }

    @Test
    fun wmemTest() {
        // wmem: 16 a b -> write the value from <b> into memory at address <a>
        val targetAddressSource = 100
        val targetAddress = 200
        val targetValue = 300.toUShort()
        memory.set(0, OpCode.WMEM.code.toInt())
        memory.set(1, targetAddressSource)
        memory.set(2, targetValue)
        memory.set(100, targetAddress)
        val expectedInstructionPointer = instructionPointer + 3
        Assert.assertEquals("WMEM should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        Assert.assertEquals("WMEM should have set memory address $targetAddress to $targetValue", targetValue, memory.get(targetAddress))
    }

    @Test
    fun callRetTest() {
        // call: 17 a -> write the address of the next instruction to the stack and jump to <a>
        // ret: 18 -> remove the top element from the stack and jump to it; empty stack = halt
        val targetAddress = 100
        memory.set(0, OpCode.CALL.code.toInt())
        memory.set(1, targetAddress)
        memory.set(targetAddress, OpCode.RET.code.toInt())
        Assert.assertEquals("CALL should return $targetAddress", targetAddress, executeNext())
        Assert.assertEquals("RET should return 2", 2, executeNext())
    }

    @Test
    fun outTest() {
        // out: 19 a -> write the character represented by ascii code <a> to the terminal
        val character = 'T'
        memory.set(0, OpCode.OUT.code.toInt())
        memory.set(1, character.toShort())
        val outContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))
        val expectedInstructionPointer = instructionPointer + 2
        Assert.assertEquals("OUT should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
        Assert.assertEquals("OUT should have print $character", character.toString(), outContent.toString())
    }

    @Test
    fun inTest() {
        // in: 20 a -> read a character from the terminal and write its ascii code to <a>; it can be assumed that once input starts, it will continue until a newline is encountered; this means that you can safely read whole lines from the keyboard and trust that they will be fully read
        val targetAddress = 100
        val string = "TEST"
        System.setIn(string.byteInputStream())
        memory.set(0, OpCode.IN.code.toInt())
        memory.set(1, targetAddress)

        string.forEach {
            instructionPointer = 0
            val expectedInstructionPointer = instructionPointer + 2
            Assert.assertEquals("IN should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
            Assert.assertEquals("IN should have set memory address $targetAddress to $it", it.toShort().toUShort(), memory.get(targetAddress))
        }
    }

    @Test
    fun noopTest() {
        // noop: 21 -> no operation
        memory.set(0, OpCode.NOOP.code.toInt())
        val expectedInstructionPointer = instructionPointer + 1
        Assert.assertEquals("NOOP should return $expectedInstructionPointer", expectedInstructionPointer, executeNext())
    }

    private fun executeNext(): Int {
        instructionPointer = OpCode.fromCode(memory.get(instructionPointer)).execute(memory, instructionPointer)
        return instructionPointer
    }
}