package riscv.riscv_arch_test

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.Parameters
import riscv.TestAnnotations
import riscv.singlecycle.TestTopModule
import scala.sys.process._
import scala.util.matching.Regex

/**
  * 這個測試類別示範：
  * 1. 透過 ELF 檔案解析取得要讀取的記憶體範圍（begin_signature 和 end_signature）。
  * 2. 初始化 SingleCycle CPU (TestTopModule) 並執行指定時間的 clock.step。
  * 3. 動態讀取記憶體區段的內容並輸出到 signatureFile。
  *
  * command example:
  *   sbt "testOnly riscv.singlecycle.RiscvArchTest -- -DelfFile=/path/to/my.elf -DsignatureFile=/path/to/result.signature"
  */
object ElfSignatureExtractor {
  def extractSignatureRange(elfFile: String): (BigInt, BigInt) = {
    // val cmd = s"riscv32-unknown-elf-readelf -s $elfFile | grep signature"
    val cmd = s"riscv32-unknown-elf-readelf -s $elfFile"
    val output = cmd.!!

    // 正則表達式匹配 begin_signature 和 end_signature
    // val beginPattern: Regex = """\s*\d+:\s*([0-9a-fA-F]+)\s+.*begin_signature""".r
    // val endPattern: Regex = """\s*\d+:\s*([0-9a-fA-F]+)\s+.*end_signature""".r


    // var beginAddress: BigInt = BigInt(0)
    // var endAddress: BigInt = BigInt(0)

    // output.split("\n").foreach {
    //   case beginPattern(addr) => beginAddress = BigInt(addr, 16)
    //   case endPattern(addr)   => endAddress = BigInt(addr, 16)
    //   case _                  => // 忽略其他行
    // }
    // var beginAddress: BigInt = BigInt("2000", 16) // 0x2000
    // var endAddress: BigInt = BigInt("2FFF", 16)  // 0x2FFF
    var beginAddress: BigInt = BigInt(0)
    var endAddress: BigInt = BigInt(0)

    output.split("\n").foreach { line =>
    if (line.contains("begin_signature")) {
        // 使用 split 分隔字串並提取地址
        val parts = line.trim.split("\\s+")
        if (parts.length > 1) {
        // beginAddress = BigInt(parts(1), 16) // 提取第二欄作為地址
        beginAddress = BigInt("0" + parts(1).substring(1), 16)
        }
    } else if (line.contains("end_signature")) {
        // 使用 split 分隔字串並提取地址
        val parts = line.trim.split("\\s+")
        if (parts.length > 1) {
        // endAddress = BigInt(parts(1), 16) // 提取第二欄作為地址
        endAddress = BigInt("0" + parts(1).substring(1), 16)
        }
    }
    }

    if (beginAddress == 0 || endAddress == 0) {
      throw new Exception("Failed to extract begin_signature or end_signature.")
    }
    println(s"Begin Address: 0x${beginAddress.toString(16)}")
    println(s"End Address: 0x${endAddress.toString(16)}")
    (beginAddress, endAddress)
  }
}

class RiscvArchTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("Single Cycle CPU (RISCOF ELF test)")

  val elfFile: String = sys.props.getOrElse("elfFile", "")
  val sigFile: String = sys.props.getOrElse("signatureFile", "signature.out")
  println(s"ELF File Path: $elfFile")
  println(s"Signature File Path: $sigFile")

  it should s"load ELF '$elfFile', extract .signature range, and test" in {
    // 從 ELF 檔案中提取範圍
    val (startAddressInt, endAddressInt) = ElfSignatureExtractor.extractSignatureRange(elfFile)

    // val startAddress = (startAddressInt & 0xFFFFFFFFL).U
    // val endAddress = (endAddressInt & 0xFFFFFFFFL).U
    val startAddress = startAddressInt
    val endAddress = endAddressInt
    val range = endAddressInt - startAddressInt
    // println(s"Signature range: 0x${startAddress.toHexString} to 0x${endAddress.toHexString}")

    test(new TestTopModule("test.asmbin")).withAnnotations(TestAnnotations.annos) { dut =>
      
      // 1. 先執行足夠多的 step，確保 CPU 內程式跑完
      dut.clock.setTimeout(0) // 0 表示無限制
      dut.clock.step(50000)
      // 2. 將 .signature 範圍內的記憶體內容寫入 Signature 檔案
      val writer = new java.io.PrintWriter(sigFile)
    //   println(s"startAddress: ${startAddress}")
    //   println(s"endAddress: ${endAddress}")
    //   for (addr <- startAddress.litValue.toLong to endAddress.litValue.toLong by 4) {
      for (addr <- 0L to endAddressInt.toLong by 4L) {
        // ********************************************************* //
        // instruction will be store from 0x1000
        // ex: 80000000:	7d5c0837 => 0x100-0x1004(0d4096 - 0d4100) store 0x7d5c0837
        // .signature will be store from .start_signature + 0x1000
        // ********************************************************* //
        // println(s"addr: $addr")
        // println(s"addr: ${addr/4}")
        // println(s"addr.U: ${addr.U}")
        var r_addr = addr.toLong + 4096L
        // var r_addr = addr.toLong
        // println(s"r_addr: ${r_addr}")
        dut.io.mem_debug_read_address.poke(r_addr.U)
        dut.clock.step()
        val data = dut.io.mem_debug_read_data.peek().litValue
        if (addr >= startAddressInt && addr < endAddressInt) {
          writer.printf("%08x\n", data.toLong)
        }
      }
      writer.close()
    }
  }
}
