#!/usr/bin/env python3
import struct
import os
import sys

def check_elf_alignment(filepath):
    """Vérifie l'alignement des segments LOAD d'un fichier ELF (.so)"""
    try:
        with open(filepath, 'rb') as f:
            elf_header = f.read(64)
            if elf_header[:4] != b'\x7fELF':
                return None, "Not an ELF file"

            ei_class = elf_header[4]
            is_64bit = (ei_class == 2)

            if is_64bit:
                f.seek(32)
                e_phoff = struct.unpack('<Q', f.read(8))[0]
                f.seek(54)
                e_phentsize = struct.unpack('<H', f.read(2))[0]
                e_phnum = struct.unpack('<H', f.read(2))[0]
            else:
                f.seek(28)
                e_phoff = struct.unpack('<I', f.read(4))[0]
                f.seek(42)
                e_phentsize = struct.unpack('<H', f.read(2))[0]
                e_phnum = struct.unpack('<H', f.read(2))[0]

            alignments = []
            for i in range(e_phnum):
                f.seek(e_phoff + i * e_phentsize)
                ph = f.read(e_phentsize)
                p_type = struct.unpack('<I', ph[0:4])[0]

                # PT_LOAD = 1
                if p_type == 1:
                    if is_64bit:
                        p_align = struct.unpack('<Q', ph[48:56])[0]
                    else:
                        p_align = struct.unpack('<I', ph[28:32])[0]
                    alignments.append(p_align)

            max_align = max(alignments) if alignments else 0
            return max_align, None
    except Exception as e:
        return None, str(e)

def format_alignment(align_bytes):
    """Formate l'alignement en KB et hex"""
    if align_bytes is None:
        return "N/A"
    align_kb = align_bytes // 1024
    return f"{align_kb}KB (0x{align_bytes:05x})"

def main():
    if len(sys.argv) != 2:
        print("Usage: python3 check_alignment.py <file.so>")
        print("   or: python3 check_alignment.py <directory>")
        sys.exit(1)

    path = sys.argv[1]

    if os.path.isdir(path):
        # Analyser tous les .so dans le répertoire
        print(f"Analysing directory: {path}\n")
        for root, dirs, files in os.walk(path):
            for file in sorted(files):
                if file.endswith('.so'):
                    filepath = os.path.join(root, file)
                    relpath = os.path.relpath(filepath, path)
                    align, error = check_elf_alignment(filepath)
                    if error:
                        print(f"❌ {relpath}: {error}")
                    else:
                        status = "✅" if align >= 16384 else "❌"
                        print(f"{status} {relpath}: {format_alignment(align)}")
    else:
        # Analyser un fichier unique
        align, error = check_elf_alignment(path)
        if error:
            print(f"Error: {error}")
            sys.exit(1)
        else:
            align_kb = align // 1024
            print(f"File: {os.path.basename(path)}")
            print(f"Alignment: {format_alignment(align)}")
            if align >= 16384:
                print("✅ OK - Compatible 16KB")
            else:
                print("❌ FAILED - NOT compatible 16KB")
            sys.exit(0 if align >= 16384 else 1)

if __name__ == "__main__":
    main()
