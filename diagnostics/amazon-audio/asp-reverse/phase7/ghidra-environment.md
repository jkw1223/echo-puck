# Ghidra environment

- Ghidra: 12.1.3, official public archive
- Installation: `/Users/jason/tools/ghidra/ghidra_12.1.3_PUBLIC`
- Archive SHA-256: `93a5d11a9ad510622acaaf908c556a7b9b764d338e78a7567f3689bf5081fd54`
- Java: Temurin OpenJDK 21.0.4 64-bit
- Project: `phase7/ghidra-project/EchoHAL`
- Input SHA-256: see `../SHA256SUMS.txt`
- Processor selected: `ARM:LE:32:v7`
- Image base: `0x00000000`

Ghidra auto-analysis completed successfully. The macOS Intel distribution lacks the native `os/mac_x86_64/decompile` component, so headless decompiler pseudocode was unavailable; ARM disassembly, symbols, data references, function discovery, and table reconstruction were still performed. The project database and headless logs are preserved.
