from pathlib import Path
from tempfile import TemporaryDirectory
from zipfile import ZipFile
import re
import subprocess
import sys

apk, dexdump = sys.argv[1:]
required = {
    "com/sun/jna/Pointer": {"peer": "J"},
    "dev/anthonyhfm/amethyst/nativeengine/RustBufferStruct": {
        "capacity": "J",
        "len": "J",
        "data": "Lcom/sun/jna/Pointer;",
    },
}
with TemporaryDirectory() as directory, ZipFile(apk) as archive:
    for name in archive.namelist():
        if not re.fullmatch(r"classes\d*\.dex", name):
            continue
        dex = Path(directory) / name
        dex.write_bytes(archive.read(name))
        output = subprocess.check_output([dexdump, str(dex)], text=True)
        for class_name in list(required):
            match = re.search(
                rf"Class descriptor\s*:\s*'L{re.escape(class_name)};'(.*?)(?=\nClass #|\Z)",
                output,
                re.S,
            )
            if match is None:
                continue
            fields = match.group(1).split("Direct methods", 1)[0]
            for field, field_type in required[class_name].items():
                pattern = rf"name\s*:\s*'{field}'\s+type\s*:\s*'{re.escape(field_type)}'"
                if re.search(pattern, fields) is None:
                    raise SystemExit(f"Missing JNA field: {class_name}.{field} ({field_type})")
                print(f"Verified {class_name}.{field} ({field_type})")
            del required[class_name]
if required:
    raise SystemExit(f"Missing JNA classes: {', '.join(required)}")
