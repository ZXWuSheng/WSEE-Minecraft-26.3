import zipfile,re,html
p=r'E:\mmcc\jianqi\无声无息-拓展附魔（Without a sound Expanded Enchantments）-详细数值.docx'
with zipfile.ZipFile(p) as z:
 s=z.read('word/document.xml').decode('utf-8')
 t=''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>',s))
 print(html.unescape(t))
