'''
Tags
Description
Spell Check
Thumbnail
Completion Score
Key Metadata fields
'''

import urllib.request, json, re
from autocorrect import spell

def spell_check(s):
    """Given any arbitrary string, return that string with `<u>` tags
    surrounding all mispelled words
    """
    fline = re.sub('[.,]', '', s)
    fline = ' '.join(fline.split())
    description_list = fline.split(" ")
    res = []
    for i in description_list:
        spell_corrected = spell(i)
        if spell_corrected != i:
            print(spell_corrected + ": " + i)
            res.append("<u>" + i + "</u>")
        else:
            res.append(i)
    return ' '.join(res)

"""Used to originally validate the functionality: not used anymore
'''
Gets Portal item
'''

def get_item(item_id):
    item_url = "https://www.arcgis.com/sharing/rest/content/items/"+item_id+"?f=pjson"
    with urllib.request.urlopen(item_url) as url:
        data = json.loads(url.read().decode())
    return data

data = get_item("757cf2f4994b4db8aba08ad523f2e011")
desc = data['description']
print(spell_check(desc))
"""
