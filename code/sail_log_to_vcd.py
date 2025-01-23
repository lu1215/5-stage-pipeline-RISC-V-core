def parse_trace_log(log_file):
    with open(log_file, 'r') as file:
        lines = file.readlines()

    # 初始化 VCD 內容
    vcd_content = [
        "$date today $end",
        "$version Sail to VCD Converter $end",
        "$timescale 1 ns $end",
        "$scope module riscv $end"
    ]

    # 提取寄存器名稱
    registers = set()
    for line in lines:
        parts = line.split()
        for part in parts:
            if part.startswith('x'):
                registers.add(part.split('=')[0])
    
    # 定義寄存器
    for reg in sorted(registers):
        vcd_content.append(f"$var reg 32 {reg} {reg} $end")
    vcd_content.append("$upscope $end")
    vcd_content.append("$enddefinitions $end")
    vcd_content.append("$dumpvars")

    # 添加初始值
    current_values = {}
    for reg in sorted(registers):
        current_values[reg] = "b" + "0" * 32  # 初始值為 0
        vcd_content.append(f"{current_values[reg]} {reg}")

    vcd_content.append("$end")
    
    # 逐行解析 trace log
    time_step = 0
    for line in lines:
        time_step += 10
        vcd_content.append(f"#{time_step}")
        parts = line.split()
        for part in parts:
            if '=' in part:
                reg, value = part.split('=')
                binary_value = bin(int(value, 16))[2:].zfill(32)
                if reg in current_values and current_values[reg] != f"b{binary_value}":
                    current_values[reg] = f"b{binary_value}"
                    vcd_content.append(f"{current_values[reg]} {reg}")
    
    return "\n".join(vcd_content)

# 將 trace log 轉換為 VCD
log_file = "/home/cosbi/Documents/5-stage-pipeline-RISC-V-core/riscof_work/src/beq-01.S/ref/beq-01.log"
vcd_file = "beq_output.vcd"
vcd_content = parse_trace_log(log_file)

with open(vcd_file, "w") as file:
    file.write(vcd_content)

print(f"VCD file generated: {vcd_file}")
