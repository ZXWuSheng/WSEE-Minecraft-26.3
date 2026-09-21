import zipfile, xml.etree.ElementTree as ET
p=r'E:\mmcc\jianqi - fa\无声无息-拓展附魔（Without a sound Expanded Enchantments）-详细数值.docx'
with zipfile.ZipFile(p) as z:
 for n in z.namelist():
  if n.endswith('.xml'):
   ET.fromstring(z.read(n))
 print('DOCX_XML_OK')