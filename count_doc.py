from docx import Document
p=r'E:\mmcc\jianqi\无声无息-拓展附魔（Without a sound Expanded Enchantments）-详细数值.docx'
d=Document(p)
for i,t in enumerate(d.tables):
 if i==0: print(len(t.rows)-1); print([r.cells[0].text for r in t.rows[1:]])