from xml.etree import ElementTree as ET
import json


INPUT = "monster.ttml"
OUTPUT = "monster.json"


et = ET.parse(INPUT)

els = et.findall("{http://www.w3.org/ns/ttml}body/{http://www.w3.org/ns/ttml}div/{http://www.w3.org/ns/ttml}p")

def ms(text : str):

    sp = text.split(":")
    min = sp[0] if len(sp) == 2 else 0
    sec = sp[1] if len(sp) == 2 else sp[0]

    return int(float(min) * 60000 + float(sec)*1000)

duration = ms(et.find("{http://www.w3.org/ns/ttml}body").attrib["dur"])

def deep_atrib(el, key):
    
    child = el.find('{http://www.w3.org/ns/ttml}span')
    
    if child == None:
        return el.attrib[key]
        
    return child.attrib[key]

def deep_text(el):
    child = el.find('{http://www.w3.org/ns/ttml}span')
    
    if child == None:
        return el.text
    return child.text

def isBg(el) -> bool:
    try:
        return el.attrib['{http://www.w3.org/ns/ttml#metadata}role']=='x-bg'
    except:
        return False

def extract_words(spans, bg : bool = False):

    words = []

    for e in spans:
        children = e.findall("{http://www.w3.org/ns/ttml}span")
        if (len(children)):
            words = words + extract_words(e,bg=e.attrib['{http://www.w3.org/ns/ttml#metadata}role']=='x-bg')
        else:
            words.append({
                "start" : ms(e.attrib['begin']),
                "end" : ms(e.attrib['end']),
                "content" : e.text,
                "bg" : bg
            })
    return words

lines = [
    {
        "start": ms(el.attrib["begin"]),
        "end": ms(el.attrib["end"]),
        "singer" : int(el.attrib["{http://www.w3.org/ns/ttml#metadata}agent"][1]),
        "words": extract_words(el),
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