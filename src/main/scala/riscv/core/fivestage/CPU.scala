// package riscv.core.fivestage

// import chisel3._
// import chisel3.util.Cat
// import riscv.CPUBundle
// import riscv.Parameters

// class CPU extends Module{
//     val io = IO(new CPUBundle)

//     val ctrl = Module(new Control)
//     val regs = Module(new RegisterFile)
//     val inst_fetch = Module(new InstructionFetch)
//     val if2id = Module(new IF2ID)
//     val id = Module(new InstructionDecode)
//     val id2ex = Module(new ID2EX)
//     val ex = Module(new Execute)
//     val ex2mem = Module(new EX2MEM)
//     val mem = Module(new MemoryAccess)
//     val mem2wb = Module(new MEM2WB)
//     val wb = Module(new WriteBack)
//     val forwarding = Module(new Forwarding)
// }