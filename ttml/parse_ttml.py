from xml.etree import ElementTree as ET
import json


INPUT = "desperado.ttml"
OUTPUT = "desperado.json"


et = ET.parse(INPUT)

els = et.findall("{http://www.w3.org/ns/ttml}body/{http://www.w3.org/ns/ttml}div/{http://www.w3.org/ns/ttml}p")

def ms(text : str):

    sp = text.split(":")
    min = sp[0] if len(sp) == 2 else 0
    sec = sp[1] if len(sp) == 2 else sp[0]

    return int(float(min) * 60000 + float(sec)*1000)

duration = ms(et.find("{http://www.w3.org/ns/ttml}body").attrib["dur"])


lines = [
    {
        "start": ms(el.attrib["begin"]),
        "end": ms(el.attrib["end"]),
        "singer" : int(el.attrib["{http://www.w3.org/ns/ttml#metadata}agent"][1]),
        "words": [{
            "start" : ms(sp.attrib["begin"]),
            "end" : ms(sp.attrib["end"]),
            "content" : sp.text
        } for sp in el.findall("{http://www.w3.org/ns/ttml}span")],
    }
    for el in els
]

result = {
    "duration": duration,
    "lines" : lines
}


with open(OUTPUT, "w") as out:
    out.write(json.dumps(result, indent=2))

# dicts = [
#         {
#             "start": el.attrib["begin"],
#             "end": el.attrib["end"],
#             "lines": [el.text],
#         }
#         for el in els
#     ]

# print(dicts)