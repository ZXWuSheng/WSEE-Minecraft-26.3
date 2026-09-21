from docx import Document
p=r'E:\mmcc\jianqi\无声无息-拓展附魔（Without a sound Expanded Enchantments）-详细数值.docx'
d=Document(p)
for i,p in enumerate(d.paragraphs):
 if '空军' in p.text or '飞勾' in p.text: print(i,p.text.strip())
