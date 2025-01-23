package riscv.core.fivestage

import chisel3._
import riscv.Parameters

// because of the pipeline, we need to add a register to store the control signals
class PipelineRegister(width: Int = Parameters.DataBits, defaultValue: UInt = 0.U) extends Module {
  // parameter of width is the width of the register, because different controls have different widths
  // ex: 1 bit for stall, 1 bit for flush, 32 bits for data
  val io = IO(new Bundle {
    val stall = Input(Bool())
    val flush = Input(Bool())
    val in    = Input(UInt(width.W))
    val out   = Output(UInt(width.W))
  })

  val myreg = RegInit(UInt(width.W), defaultValue)
  val out   = RegInit(UInt(width.W), defaultValue)
  when(io.flush) {
    // when flush, we need to reset the register
    out   := defaultValue
    myreg := defaultValue
  }
    .elsewhen(io.stall) {
      // when stall, we need to value of register is as same as the previous cycle
      out := myreg
    }
    .otherwise {
      // when in normal case, we need to update the value of register
      myreg := io.in
      out   := io.in
    }
  // assign the output of the register
  io.out := out
}