from docx import Document
p=r'E:\mmcc\jianqi\无声无息-拓展附魔（Without a sound Expanded Enchantments）-详细数值.docx'
d=Document(p)
for i,p in enumerate(d.paragraphs):
 if 330<=i<=430: print(i,p.text.strip())