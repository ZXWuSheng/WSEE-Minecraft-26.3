# -*- coding: utf-8 -*-
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.section import WD_SECTION
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.enum.style import WD_STYLE_TYPE

OUT = 'WSEE-0.1.0-Minecraft-26.3-Fabric-NeoForge-详细规格.docx'
FONT = 'Microsoft YaHei'
BLUE = '17365D'
BLUE2 = '2F5597'
LIGHT = 'E8EEF5'
PALE = 'F7F9FC'
GRAY = '555555'
BLACK = '222222'


def set_font(run, size=10.5, color=BLACK, bold=None, italic=None):
    run.font.name = FONT
    run._element.get_or_add_rPr().rFonts.set(qn('w:ascii'), FONT)
    run._element.get_or_add_rPr().rFonts.set(qn('w:hAnsi'), FONT)
    run._element.get_or_add_rPr().rFonts.set(qn('w:eastAsia'), FONT)
    run.font.size = Pt(size)
    run.font.color.rgb = RGBColor.from_string(color)
    if bold is not None:
        run.bold = bold
    if italic is not None:
        run.italic = italic


def set_cell_margins(cell, top=80, start=120, bottom=80, end=120):
    tc = cell._tc
    tcPr = tc.get_or_add_tcPr()
    tcMar = tcPr.first_child_found_in('w:tcMar')
    if tcMar is None:
        tcMar = OxmlElement('w:tcMar')
        tcPr.append(tcMar)
    for m, v in [('top', top), ('start', start), ('bottom', bottom), ('end', end)]:
        node = tcMar.find(qn('w:' + m))
        if node is None:
            node = OxmlElement('w:' + m)
            tcMar.append(node)
        node.set(qn('w:w'), str(v))
        node.set(qn('w:type'), 'dxa')


def set_cell_shading(cell, fill):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = tcPr.find(qn('w:shd'))
    if shd is None:
        shd = OxmlElement('w:shd')
        tcPr.append(shd)
    shd.set(qn('w:fill'), fill)


def set_cell_width(cell, width):
    tcPr = cell._tc.get_or_add_tcPr()
    tcW = tcPr.find(qn('w:tcW'))
    if tcW is None:
        tcW = OxmlElement('w:tcW')
        tcPr.append(tcW)
    tcW.set(qn('w:type'), 'dxa')
    tcW.set(qn('w:w'), str(width))


def set_table_borders(table, color='000000', size='6'):
    """Use visible black borders for the outer edge and every cell gridline."""
    tblPr = table._tbl.tblPr
    borders = tblPr.find(qn('w:tblBorders'))
    if borders is None:
        borders = OxmlElement('w:tblBorders')
        tblPr.append(borders)
    for edge in ('top', 'left', 'bottom', 'right', 'insideH', 'insideV'):
        node = borders.find(qn('w:' + edge))
        if node is None:
            node = OxmlElement('w:' + edge)
            borders.append(node)
        node.set(qn('w:val'), 'single')
        node.set(qn('w:sz'), size)
        node.set(qn('w:space'), '0')
        node.set(qn('w:color'), color)


def set_table_geometry(table, widths, indent=120):
    table.alignment = WD_TABLE_ALIGNMENT.LEFT
    table.autofit = False
    tblPr = table._tbl.tblPr
    layout = tblPr.find(qn('w:tblLayout'))
    if layout is None:
        layout = OxmlElement('w:tblLayout')
        tblPr.append(layout)
    layout.set(qn('w:type'), 'fixed')
    tblW = tblPr.find(qn('w:tblW'))
    if tblW is None:
        tblW = OxmlElement('w:tblW')
        tblPr.append(tblW)
    tblW.set(qn('w:type'), 'dxa')
    tblW.set(qn('w:w'), str(sum(widths)))
    tblInd = tblPr.find(qn('w:tblInd'))
    if tblInd is None:
        tblInd = OxmlElement('w:tblInd')
        tblPr.append(tblInd)
    tblInd.set(qn('w:type'), 'dxa')
    tblInd.set(qn('w:w'), str(indent))
    set_table_borders(table)
    grid = table._tbl.tblGrid
    for child in list(grid):
        grid.remove(child)
    for width in widths:
        col = OxmlElement('w:gridCol')
        col.set(qn('w:w'), str(width))
        grid.append(col)
    for row in table.rows:
        for i, cell in enumerate(row.cells):
            set_cell_width(cell, widths[i])
            set_cell_margins(cell)
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER


def mark_header_row(row):
    trPr = row._tr.get_or_add_trPr()
    hdr = OxmlElement('w:tblHeader')
    hdr.set(qn('w:val'), 'true')
    trPr.append(hdr)


def prevent_row_split(row):
    trPr = row._tr.get_or_add_trPr()
    cant_split = OxmlElement('w:cantSplit')
    cant_split.set(qn('w:val'), 'true')
    trPr.append(cant_split)


def add_table(doc, headers, rows, widths, font_size=8.8, alternate=True):
    table = doc.add_table(rows=1, cols=len(headers))
    set_table_geometry(table, widths)
    mark_header_row(table.rows[0])
    prevent_row_split(table.rows[0])
    for i, text in enumerate(headers):
        cell = table.rows[0].cells[i]
        set_cell_shading(cell, BLUE)
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(1)
        p.paragraph_format.line_spacing = 1.0
        p.paragraph_format.keep_with_next = True
        r = p.add_run(str(text))
        set_font(r, font_size, 'FFFFFF', bold=True)
    for ri, row_data in enumerate(rows):
        row = table.add_row()
        prevent_row_split(row)
        cells = row.cells
        for i, text in enumerate(row_data):
            cell = cells[i]
            if alternate and ri % 2 == 1:
                set_cell_shading(cell, PALE)
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(1)
            p.paragraph_format.line_spacing = 1.05
            r = p.add_run(str(text))
            set_font(r, font_size, BLACK)
    doc.add_paragraph().paragraph_format.space_after = Pt(1)
    return table


def add_bullet(doc, text):
    p = doc.add_paragraph(style='List Bullet')
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.line_spacing = 1.15
    r = p.add_run(text)
    set_font(r, 10.2, BLACK)
    return p


def add_para(doc, text, size=10.5, color=BLACK, bold=False, italic=False, align=None, after=6):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(after)
    p.paragraph_format.line_spacing = 1.25
    if align is not None:
        p.alignment = align
    r = p.add_run(text)
    set_font(r, size, color, bold, italic)
    return p


def add_heading(doc, text, level=1):
    p = doc.add_paragraph(style=f'Heading {level}')
    p.paragraph_format.keep_with_next = True
    r = p.add_run(text)
    set_font(r, {1: 16, 2: 13, 3: 11.5}[level], BLUE if level < 3 else BLUE2, bold=True)
    return p


def add_enchantment(doc, item):
    add_heading(doc, item['name'], 1)
    add_heading(doc, '附魔介绍', 2)
    add_para(doc, f"中文名称：{item['cn']}；英文名称：{item['en']}；附魔 ID：wsee:{item['id']}。适用装备：{item['gear']}；最高等级：{item['max']}。{item['intro']}")
    add_heading(doc, '等级数值', 2)
    add_table(doc, item['level_headers'], item['levels'], item.get('level_widths', [1800, 7560]), font_size=8.7)
    add_heading(doc, '规则', 2)
    for rule in item['rules']:
        add_bullet(doc, rule)
    add_heading(doc, '冲突', 2)
    add_para(doc, item['conflicts'])
    add_heading(doc, '指令', 2)
    commands = item.get('commands') or [f"/enchant @s wsee:{item['id']} {item['max_num']} ",]
    add_table(doc, ['命令', '作用'], [[c, f"为符合适用装备的物品添加 {item['cn']}，用于游戏内验证。"] for c in commands], [4300, 5060], font_size=8.6)


def main():
    doc = Document()
    sec = doc.sections[0]
    sec.page_width = Inches(8.5)
    sec.page_height = Inches(11)
    sec.top_margin = Inches(0.78)
    sec.bottom_margin = Inches(0.72)
    sec.left_margin = Inches(0.82)
    sec.right_margin = Inches(0.82)
    sec.header_distance = Inches(0.35)
    sec.footer_distance = Inches(0.35)
    styles = doc.styles
    normal = styles['Normal']
    normal.font.name = FONT
    normal._element.get_or_add_rPr().rFonts.set(qn('w:eastAsia'), FONT)
    normal.font.size = Pt(10.5)
    normal.paragraph_format.space_after = Pt(6)
    normal.paragraph_format.line_spacing = 1.25
    for name, size, color, before, after in [
        ('Heading 1', 16, BLUE, 18, 10),
        ('Heading 2', 13, BLUE, 14, 7),
        ('Heading 3', 11.5, BLUE2, 10, 5),
    ]:
        s = styles[name]
        s.font.name = FONT
        s._element.get_or_add_rPr().rFonts.set(qn('w:eastAsia'), FONT)
        s.font.size = Pt(size)
        s.font.bold = True
        s.font.color.rgb = RGBColor.from_string(color)
        s.paragraph_format.space_before = Pt(before)
        s.paragraph_format.space_after = Pt(after)
        s.paragraph_format.line_spacing = 1.15
        s.paragraph_format.keep_with_next = True
    if 'Subtitle' in styles:
        styles['Subtitle'].font.name = FONT
        styles['Subtitle']._element.get_or_add_rPr().rFonts.set(qn('w:eastAsia'), FONT)
    # Header/footer
    hp = sec.header.paragraphs[0]
    hp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    hr = hp.add_run('WSEE 0.1.0  |  Fabric + NeoForge  |  Minecraft 26.3')
    set_font(hr, 8.5, GRAY)
    fp = sec.footer.paragraphs[0]
    fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    fr = fp.add_run('无声无息：拓展附魔（Without a sound: Expanded Enchantments）  ·  详细规格')
    set_font(fr, 8, GRAY)

    # Title block
    add_para(doc, 'WSEE 0.1.0', 11, BLUE2, True, align=WD_ALIGN_PARAGRAPH.CENTER, after=4)
    add_para(doc, '无声无息：拓展附魔', 25, BLUE, True, align=WD_ALIGN_PARAGRAPH.CENTER, after=2)
    add_para(doc, 'Without a sound: Expanded Enchantments', 14, GRAY, False, True, align=WD_ALIGN_PARAGRAPH.CENTER, after=14)
    add_para(doc, 'Minecraft 26.3 · Fabric 与 NeoForge 双端详细规格', 12, BLUE2, True, align=WD_ALIGN_PARAGRAPH.CENTER, after=18)
    add_table(doc, ['项目', '当前值'], [
        ['模组 ID', 'wsee'],
        ['版本', '0.1.0（WSEE 0.1.0）'],
        ['作者', 'ZX_Wu_Sheng'],
        ['鸣谢', 'KsqsnK，Badnng，Wangmeili，Niorris'],
        ['Minecraft', '26.3'],
        ['支持平台', 'Fabric / NeoForge'],
        ['附魔数量', '31 个，当前规格只包含本版本正式附魔'],
    ], [2200, 7160], font_size=9.2)
    add_para(doc, '本文件是当前双端实现的行为契约与验收基线。所有数值、触发条件、装备限制、冲突、语言键、数据标签和测试命令均以本版本为准；平台章节只描述加载器差异，不改变游戏内行为。', 10.5, GRAY, italic=True, after=12)

    add_heading(doc, '1. 文档导航与版本边界', 1)
    add_para(doc, '阅读顺序建议：先看平台矩阵与安装约束，再看统一行为契约、附魔总览和冲突矩阵，最后按类别查阅单个附魔的等级表、规则和指令。')
    add_table(doc, ['章节', '用途'], [
        ['2. 双端平台矩阵', '确认 Fabric 与 NeoForge 的加载器、构建和资源边界'],
        ['3. 安装、文件与一致性', '确认实例放置方式、不能混装的文件和同步对象'],
        ['4. 附魔总览与冲突', '快速查找 31 个附魔的装备、等级和互斥关系'],
        ['5-10. 详细规格', '按类别给出完整数值、触发和防递归规则'],
        ['11. 指令、资源与验收', '验证命令、JSON/DOCX 校验、构建和人工烟测'],
    ], [2600, 6760], font_size=9.2)
    add_para(doc, '版本边界：本文只覆盖 WSEE 0.1.0 / Minecraft 26.3。早期单端文档、旧名称和旧 Tooltip 数值均不属于当前实现。', 10.2, '9B1C1C', True)

    add_heading(doc, '2. 双端平台矩阵', 1)
    add_table(doc, ['项目', 'Fabric', 'NeoForge'], [
        ['Minecraft', '26.3', '26.3'],
        ['加载器', 'Fabric Loader 0.19.5', 'NeoForge 26.3.0.6-beta'],
        ['API / 模组加载', 'Fabric API 0.158.0+26.3；Loom 1.17.20', 'NeoForge ModDev 2.0.147'],
        ['Java', '25', '25'],
        ['模组 ID / 版本', 'wsee / 0.1.0', 'wsee / 0.1.0'],
        ['入口差异', 'Fabric main/client entrypoints 与 mixins', 'NeoForge mod 入口、事件总线与 mixins'],
        ['共享契约', '装备、ID、数据标签、语言、数值、触发与防递归规则完全一致', '装备、ID、数据标签、语言、数值、触发与防递归规则完全一致'],
    ], [1900, 3730, 3730], font_size=8.7)
    add_heading(doc, '双端一致性契约', 2)
    for t in [
        '两端使用相同的 wsee:<enchantment_id>、wsee 数据标签、语言键和创造成书顺序；不能因为加载器 API 不同而新增或删减附魔。',
        '服务端负责装备合法性、触发判定、伤害和冷却；客户端只负责必要的 HUD、Tooltip、锁定轮廓和动作同步。',
        '所有直接伤害修改原事件，不创建第二个等价伤害事件；持续伤害和震环等间接伤害必须使用独立 DamageType 并阻止递归。',
        '不允许在同一个实例的 mods 目录同时放 Fabric 版和 NeoForge 版 WSEE JAR；应根据启动器选择单一加载器构建。',
        '两端的资源包、语言文本和 DOCX 数值必须从同一份规格核对，尤其是 Overcharge、Refining、Holy Light、Surge、Falling Star 和 Shock Ring。',
    ]:
        add_bullet(doc, t)

    add_heading(doc, '3. 安装、文件与一致性', 1)
    add_heading(doc, 'Fabric 实例', 2)
    for t in [
        '使用 Java 25、Minecraft 26.3、Fabric Loader 0.19.5 和 Fabric API 0.158.0+26.3。',
        '将 Fabric 构建的 WSEE-0.1.0-Fabric.jar 放入该实例的 mods/；不要放入 NeoForge 构建产物。',
        '确认 fabric.mod.json 的 id、version、authors、contributors、entrypoints 和 mixins 与本规格一致。',
    ]:
        add_bullet(doc, t)
    add_heading(doc, 'NeoForge 实例', 2)
    for t in [
        '使用 Java 25、Minecraft 26.3 和 NeoForge 26.3.0.6-beta。',
        '将 NeoForge 构建的 WSEE-0.1.0-NeoForge.jar 放入该实例的 mods/；不要放入 Fabric 构建产物。',
        '确认 META-INF/neoforge.mods.toml 的 modId、version、displayName、authors、credits 与本规格一致。',
    ]:
        add_bullet(doc, t)
    add_heading(doc, '必须同步的资源', 2)
    add_table(doc, ['资源层', '同步要求'], [
        ['数据定义', '31 个 enchantment JSON、主/副物品标签、exclusive_set、DamageType 和 refinable_ores'],
        ['运行时', '装备合法性 backstop、服务端状态、网络 payload、HUD 与客户端反馈'],
        ['语言', 'zh_cn.json 与 en_us.json 使用当前实现的简短单字符串；用分号分隔 Tooltip 行，描述颜色固定 DARK_GRAY #555555'],
        ['创造成书', '只列有效装备对应的书；顺序和总数在两端一致'],
        ['文档与 README', '本文、README 附魔数量、版本号和鸣谢一致'],
    ], [1900, 7460], font_size=8.8)

    enchantments = [
        {
            'name':'剑气（Sword Aura）','cn':'剑气','en':'Sword Aura','id':'sword_aura','gear':'剑','max':'V','max_num':5,
            'intro':'沿玩家水平朝向释放横扫波；命中与挥空都可触发，不依赖准星目标。',
            'level_headers':['等级','射程 / 伤害 / 穿透'], 'levels':[['I','6 格；攻击力 ×40%；最多穿透 1 个目标'],['II','7 格；攻击力 ×55%；最多穿透 2 个目标'],['III','8 格；攻击力 ×70%；最多穿透 3 个目标'],['IV','9 格；攻击力 ×85%；最多穿透 4 个目标'],['V','10 格；攻击力 ×100%；最多穿透 5 个目标']],
            'rules':['波宽固定 2.5 格，沿玩家水平面向前逐行推进，每个前进格只生成一个横扫粒子。','格挡、打开背包/聊天等阻塞界面时不触发；不使用玩家准星方向。','伤害修改现有近战事件，正常经过护甲、韧性、保护、抗性、盾牌和无敌帧；不创建第二个直接伤害事件。'],
            'conflicts':'与原版横扫之刃冲突；与连斩冲突。', 'commands':['/enchant @s wsee:sword_aura 5']},
        {
            'name':'盾击（Shield Bash）','cn':'盾击','en':'Shield Bash','id':'shield_bash','gear':'盾牌','max':'I','max_num':1,
            'intro':'持盾时向正面进行一次短距离盾击，造成小额物理伤害、减速和击退。',
            'level_headers':['等级','数值'], 'levels':[['I','伤害：目标最大生命值 1.5%；范围 2 格；正面宽度约 1.5 格；冷却 50 tick']],
            'rules':['挥空也会开始 50 tick 冷却；打开背包、聊天或其他阻塞界面时不触发。','只接受主手/副手盾牌的合法装备状态；效果由服务端判定并同步。','使用现有伤害流程，不生成重复命中事件；反馈采用一次音效和少量粒子。'],
            'conflicts':'无额外冲突。', 'commands':['/enchant @s wsee:shield_bash 1']},
        {
            'name':'“闪”（Deflect）','cn':'闪','en':'Deflect','id':'deflect','gear':'盾牌','max':'I','max_num':1,
            'intro':'成功格挡普通箭、药水箭或光灵箭后，按持盾者面朝方向反射。',
            'level_headers':['等级','数值'], 'levels':[['I','反射速度 ×0.90；反射伤害 ×0.80']],
            'rules':['只反射普通箭、药水箭和光灵箭；爆炸箭及其他投射物不反射。','反射方向使用格挡玩家的 look direction，转移攻击归属，并在投射物上记录已反射标记，最多反射一次。','保留原版碰撞、护甲、盾牌和伤害流程；服务端设置 owner，客户端不伪造命中。'],
            'conflicts':'无额外冲突。', 'commands':['/enchant @s wsee:deflect 1']},
        {
            'name':'扩容（Expansion）','cn':'扩容','en':'Expansion','id':'expansion','gear':'弩','max':'III','max_num':3,
            'intro':'一次装填生成连续发射的额外箭矢，增加覆盖而不改变弩的基础伤害计算。',
            'level_headers':['等级','数值'], 'levels':[['I','一次装填 2 支箭；额外箭每隔 5 tick 发射'],['II','一次装填 3 支箭；额外箭每隔 5 tick 发射'],['III','一次装填 4 支箭；额外箭每隔 5 tick 发射']],
            'rules':['仅弩有效；生成的每支箭独立保留命中、护甲、盾牌和无敌帧行为。','每支额外箭在实际发射时读取玩家当前视角，并从玩家当前所在位置射出；转动视角会改变后续箭矢方向。','每支额外箭都带递归保护标记，不重复触发扩容、聚束或其他生成逻辑。'],
            'conflicts':'与聚束及原版多重射击冲突。', 'commands':['/enchant @s wsee:expansion 3']},
        {
            'name':'聚束（Cluster）','cn':'聚束','en':'Cluster','id':'cluster','gear':'弩','max':'III','max_num':3,
            'intro':'一次发射多支集中箭矢，等级越高箭数越多、散射越小。',
            'level_headers':['等级','箭数 / 单箭伤害 / 散射'], 'levels':[['I','2 支；每箭伤害 70%；散射 ±4°'],['II','3 支；每箭伤害 55%；散射 ±3°'],['III','4 支；每箭伤害 45%；散射 ±2°']],
            'rules':['每一支生成箭都必须完成自己的命中解析，不得只让第一支箭造成伤害。','正常经过护甲、保护、盾牌和无敌帧；生成箭带递归保护标记。'],
            'conflicts':'与扩容及原版多重射击冲突。', 'commands':['/enchant @s wsee:cluster 3']},
        {
            'name':'穿甲（Armor Piercing）','cn':'穿甲','en':'Armor Piercing','id':'armor_piercing','gear':'弩','max':'IV','max_num':4,
            'intro':'按等级忽略目标的一部分有效护甲，仍保留原版伤害事件和保护减伤。',
            'level_headers':['等级','数值'], 'levels':[['I','忽略 10% 有效护甲'],['II','忽略 20% 有效护甲'],['III','忽略 30% 有效护甲'],['IV','忽略 40% 有效护甲']],
            'rules':['仅弩箭伤害路径读取；不改变箭速、箭数或碰撞。','修改当前防御计算使用的有效护甲，不创建额外伤害事件。'],
            'conflicts':'无额外冲突。', 'commands':['/enchant @s wsee:armor_piercing 4']},
        {
            'name':'流血（Bleeding）','cn':'流血','en':'Bleeding','id':'bleeding','gear':'矛标签物品','max':'III','max_num':3,
            'intro':'成功直接近战命中后施加持续物理伤害，伤害独立于触发命中。',
            'level_headers':['等级','数值'], 'levels':[['I','持续 4 秒；每 20 tick 造成最大生命值 0.5%；理论总计 2%'],['II','持续 5 秒；每 20 tick 造成最大生命值 0.75%；理论总计 3.75%'],['III','持续 6 秒；每 20 tick 造成最大生命值 1%；理论总计 6%']],
            'rules':['只由玩家使用当前主手矛成功直接近战伤害触发；箭、火焰、荆棘、爆炸、药水或其他独立伤害不能触发。','持续伤害尊重护甲、韧性、保护和抗性，使用独立物理伤害类型并阻止递归命中附魔。','重新施加时按当前实现刷新持续状态，不叠加无限独立实例。'],
            'conflicts':'与突进（Lunge）和霜噬冲突。', 'commands':['/enchant @s wsee:bleeding 3']},
        {
            'name':'长锋（Long Edge）','cn':'长锋','en':'Long Edge','id':'long_edge','gear':'矛标签物品','max':'III','max_num':3,
            'intro':'只改变矛实体攻击距离，不改变伤害倍率或投掷轨迹。',
            'level_headers':['等级','数值'], 'levels':[['I','增加矛实体攻击距离 0.25 格'],['II','增加矛实体攻击距离 0.625 格'],['III','增加矛实体攻击距离 1.0 格']],
            'rules':['仅矛的实体近战判定使用加长距离；投掷三叉戟和其他物品不读取该数值。','脱下、替换或使用不符合矛标签的物品时立即失效。'],
            'conflicts':'无额外冲突。', 'commands':['/enchant @s wsee:long_edge 3']},
        {
            'name':'涌流（Surge）','cn':'涌流','en':'Surge','id':'surge','gear':'投掷中的三叉戟','max':'III','max_num':3,
            'intro':'三叉戟在水中连续飞行达到门槛后强化同一次投掷命中。',
            'level_headers':['等级','数值'], 'levels':[['I','连续水中飞行 6 tick；投掷伤害 ×1.15；击退 ×1.10'],['II','连续水中飞行 5 tick；投掷伤害 ×1.25；击退 ×1.20'],['III','连续水中飞行 4 tick；投掷伤害 ×1.35；击退 ×1.30']],
            'rules':['只从服务端 ThrownTrident 投射物路径判定；实体流体状态与脚下占用流体方块都计入连续水中 tick。','命中 tick 也计入，避免水面边界命中漏触发；返还、无物理和三叉戟近战攻击排除。','强化直接修改同一投掷伤害事件与击退，不生成第二次伤害。'],
            'conflicts':'与激流冲突；与牵引、回旋、震水可按各自规则共存。', 'commands':['/enchant @s wsee:surge 3']},
        {
            'name':'牵引（Tether）','cn':'牵引','en':'Tether','id':'tether','gear':'投掷中的三叉戟','max':'III','max_num':3,
            'intro':'投掷三叉戟命中后，将合法目标沿连接方向拉向使用者。',
            'level_headers':['等级','牵引强度'], 'levels':[['I','0.35'],['II','0.50'],['III','0.65']],
            'rules':['只在投掷实体命中路径处理；不作用于三叉戟近战。','牵引使用 DeltaMovement，不瞬移；缺失目标、跨维度、死亡或达到停止条件时结束。','遵守目标可攻击性、实体碰撞和服务端所有权；不创建额外伤害事件。'],
            'conflicts':'与激流、涌流、回旋冲突。', 'commands':['/enchant @s wsee:tether 3']},
        {
            'name':'回旋（Rebound）','cn':'回旋','en':'Rebound','id':'rebound','gear':'投掷中的三叉戟','max':'III','max_num':3,
            'intro':'忠诚返回阶段沿返程路径再次攻击经过的目标。',
            'level_headers':['等级','返回伤害 / 每次最多命中'], 'levels':[['I','正常投掷基础伤害 ×30%；1 个实体'],['II','正常投掷基础伤害 ×45%；2 个实体'],['III','正常投掷基础伤害 ×60%；3 个实体']],
            'rules':['必须同时拥有忠诚；只在返回路径处理，投掷出手和三叉戟近战不重复触发。','每次返回按等级限制命中数量，记录已命中实体并防止一帧多次命中。','保持原版护甲、保护、盾牌和击退规则。'],
            'conflicts':'与激流、牵引冲突；需要原版忠诚。', 'commands':['/enchant @s wsee:rebound 3']},
        {
            'name':'震水（Hydroshock）','cn':'震水','en':'Hydroshock','id':'hydroshock','gear':'投掷中的三叉戟','max':'III','max_num':3,
            'intro':'投掷三叉戟在水中碰撞后释放一次范围水波，主要作用是击退。',
            'level_headers':['等级','半径 / 水平冲击 / 垂直冲击'], 'levels':[['I','2 格；0.35；0.08'],['II','3 格；0.50；0.12'],['III','4 格；0.65；0.16']],
            'rules':['仅投掷实体的水中碰撞触发；不作用于三叉戟近战。','范围中心固定在碰撞点；目标按正常可攻击、垂直范围、方块遮挡和击退抗性处理。','一次碰撞最多生成一个水波实例，使用受控粒子和一次音效，不产生大体积连续粒子。'],
            'conflicts':'与激流冲突；可与涌流、牵引、回旋共存。', 'commands':['/enchant @s wsee:hydroshock 3']},
        {
            'name':'蓄势（Overcharge）','cn':'蓄势','en':'Overcharge','id':'overcharge','gear':'弓','max':'III','max_num':3,
            'intro':'超过原版满弓阈值后继续蓄力，按线性进度提高箭矢伤害和速度。',
            'level_headers':['等级','额外蓄力上限 / 最高增伤 / 最高箭速'], 'levels':[['I','10 tick；+7%；+5%'],['II','15 tick；+17%；+8%'],['III','20 tick；+27%；+12%']],
            'rules':['只有超过原版 full-draw 阈值才开始计算；进度在额外 tick 上限内线性增长。','只修改这一次弓箭射击的伤害和速度；不与连斩共享装备逻辑，也不创建第二个伤害事件。','蓄力期间只显示当前阶段的简洁反馈；未开始蓄势时不显示空槽，满蓄只播放一次轻反馈。'],
            'conflicts':'无额外冲突；箭雨与爆矢按各自冲突规则处理。', 'commands':['/enchant @s wsee:overcharge 3']},
        {
            'name':'爆矢（Explosive Arrow）','cn':'爆矢','en':'Explosive Arrow','id':'explosive_arrow','gear':'弓','max':'III','max_num':3,
            'intro':'箭命中实体或方块后延迟爆炸，爆炸基础伤害取射击者攻击力。',
            'level_headers':['等级','爆炸半径 / 基础伤害'], 'levels':[['I','2 格；射击者攻击力 ×25%'],['II','2.5 格；射击者攻击力 ×45%'],['III','3 格；射击者攻击力 ×65%']],
            'rules':['实体命中和方块命中都必须触发；延迟期间保留箭的归属和碰撞状态。','爆炸伤害按范围目标分别经过护甲、保护、盾牌和抗性；箭带递归标记，不能再次触发爆矢、箭雨或蓄势。','不把爆炸替换成第二个直接弓箭命中事件，爆炸使用独立伤害类型。'],
            'conflicts':'与箭雨冲突。', 'commands':['/enchant @s wsee:explosive_arrow 3']},
        {
            'name':'箭雨（Arrow Rain）','cn':'箭雨','en':'Arrow Rain','id':'arrow_rain','gear':'弓','max':'III','max_num':3,
            'intro':'拉弓达到 90% 后首次命中实体或方块，在固定区域上方生成真实箭雨。',
            'level_headers':['等级','半径 / 总箭数 / 持续 / 单箭基础伤害'], 'levels':[['I','2.25 格；6 支；30 tick；原始箭伤害 ×0.18'],['II','2.75 格；9 支；36 tick；原始箭伤害 ×0.20'],['III','3.25 格；12 支；40 tick；原始箭伤害 ×0.22']],
            'rules':['命中后先警告 10 tick；警告期间可跟随实体中心，警告结束后区域固定。','生成真实不可拾取箭，正常碰撞、护甲、保护、盾牌和无敌帧生效；室内需要检查顶部空间。','雨箭带标记，禁止再次触发箭雨、爆矢和蓄势；使用者和合法目标筛选由服务端完成。'],
            'conflicts':'与爆矢冲突。', 'commands':['/enchant @s wsee:arrow_rain 3']},
        {
            'name':'追踪（Tracking）','cn':'追踪','en':'Tracking','id':'tracking','gear':'弓','max':'III','max_num':3,
            'intro':'拉弓达到 80% 后锁定一个目标，在有限时间内以受限角速度轻微修正箭矢方向。',
            'level_headers':['等级','距离 / 初始角度 / 每 tick 最大转向 / 时间'], 'levels':[['I','24 格；±25°；3.0°；40 tick'],['II','48 格；±35°；5.0°；40 tick'],['III','72 格；±50°；7.0°；40 tick']],
            'rules':['生成时只选择一次有效 LivingEntity，不切换目标；箭速、重力、空气阻力、碰撞、伤害和 Owner 不改变。','每 tick 最多转向 3/5/7°；被方块阻挡、目标死亡/无效、初始偏差超过 70°或超时即永久结束。','锁定反馈只发送给射击者客户端，使用临时原版 glowing 轮廓；服务器实体不设置全局 glowing。'],
            'conflicts':'无额外冲突。', 'commands':['/enchant @s wsee:tracking 3']},
        {
            'name':'连斩（Combo Strike）','cn':'连斩','en':'Combo Strike','id':'combo_strike','gear':'剑','max':'III','max_num':3,
            'intro':'同一目标的连续成功直接命中逐段增强，第三段为最终强化击。',
            'level_headers':['等级','第二击 / 第三击 / 第三击击退'], 'levels':[['I','×1.05；×1.12；×1.10'],['II','×1.08；×1.18；×1.10'],['III','×1.10；×1.25；×1.10']],
            'rules':['只统计同一目标的成功直接近战；中断、换目标或超时会清空连击。','手持时显示两个充能格：空、黄色、最终击就绪时第二格变红；不显示超过两格的 UI。','修改当前伤害事件；不与越战越勇冲突，不让投射物、流血或其他独立伤害推进连击。'],
            'conflicts':'与剑气冲突；不得与越战越勇冲突。', 'commands':['/enchant @s wsee:combo_strike 3']},
        {
            'name':'越战越勇（Battle Momentum）','cn':'越战越勇','en':'Battle Momentum','id':'battle_momentum','gear':'剑、斧、矛、三叉戟、重锤','max':'III','max_num':3,
            'intro':'使用当前主手近战武器成功直接命中后叠加战意，提高攻击速度。',
            'level_headers':['等级','每层攻速 / 最大层数 / 满层加成 / 持续'], 'levels':[['I','+4%；3 层；+12%；80 tick'],['II','+5%；4 层；+20%；80 tick'],['III','+6%；5 层；+30%；80 tick']],
            'rules':['只计入玩家使用当前主手武器造成的成功直接近战伤害；投射物、流血、火焰、荆棘、爆炸、药水和其他独立伤害不加层。','换武器、超时或状态清除时按实现清空；攻击速度属性修饰符不得重复叠加。','战意状态反馈简洁，不生成持续大粒子。'],
            'conflicts':'无额外冲突；与连斩必须共存且互不推进。', 'commands':['/enchant @s wsee:battle_momentum 3']},
        {
            'name':'处决（Execution）','cn':'处决','en':'Execution','id':'execution','gear':'斧','max':'III','max_num':3,
            'intro':'目标当前生命值加吸收值不高于最大生命值 30% 时，提高本次斧击伤害。',
            'level_headers':['等级','倍率'], 'levels':[['I','攻击伤害 ×1.15'],['II','攻击伤害 ×1.25'],['III','攻击伤害 ×1.35']],
            'rules':['在防御计算前修改当前攻击事件，阈值使用当前生命 + 吸收值；不创建第二次伤害。','只在玩家使用斧直接近战时检查，其他伤害来源不触发。','正常经过护甲、韧性、保护、盾牌和无敌帧。'],
            'conflicts':'无额外冲突。', 'commands':['/enchant @s wsee:execution 3']},
        {
            'name':'急行军（Forced March）','cn':'急行军','en':'Forced March','id':'forced_march','gear':'靴子','max':'V','max_num':5,
            'intro':'连续地面疾跑后，以移动速度属性修饰符获得短暂的行军加速。',
            'level_headers':['等级','连续疾跑门槛 / 移动速度'], 'levels':[['I','80 tick；+6%'],['II','70 tick；+9%'],['III','60 tick；+12%'],['IV','50 tick；+15%'],['V','40 tick；+18%']],
            'rules':['要求在地面疾跑；停止疾跑后保留 15 tick（0.75 秒）宽限期。','使用 transient movement-speed modifier，脱下或替换靴子立即移除且不重复添加。','不把游泳、飞行、鞘翅或创造飞行算作地面疾跑。'],
            'conflicts':'与冰霜行者冲突。', 'commands':['/enchant @s wsee:forced_march 5']},
        {
            'name':'明（Clarity）','cn':'明','en':'Clarity','id':'clarity','gear':'头盔','max':'I','max_num':1,
            'intro':'装备头盔时阻止或移除黑暗、失明和反胃。',
            'level_headers':['等级','效果'], 'levels':[['I','免疫黑暗、失明和反胃']],
            'rules':['仅在合法头盔槽装备时生效；脱下后立即恢复原版效果行为。','不清除其他状态效果，不改变客户端高级 Tooltip 设置。'],
            'conflicts':'无额外冲突。', 'commands':['/enchant @s wsee:clarity 1']},
        {
            'name':'护盾（Shield）','cn':'护盾','en':'Shield','id':'shield','gear':'胸甲','max':'V','max_num':5,
            'intro':'周期性给予原版 Absorption 吸收值，黄色心形和效果图标可在背包中看到。',
            'level_headers':['等级','吸收值 / 刷新周期'], 'levels':[['I','最大生命值 8%；4800 tick（240 秒）'],['II','最大生命值 12%；4800 tick（240 秒）'],['III','最大生命值 16%；4800 tick（240 秒）'],['IV','最大生命值 20%；4800 tick（240 秒）'],['V','最大生命值 25%；4800 tick（240 秒）']],
            'rules':['吸收值不叠加；重新装备、重登后保留冷却状态。','移除胸甲时立即移除 WSEE 授予的吸收，不删除玩家其他来源的 Absorption。','支持定向冷却重置命令，且只影响 shield；服务端保存下一次触发时间。'],
            'conflicts':'与圣光互斥。', 'commands':['/enchant @s wsee:shield 5','/wsee reset_cooldown shield']},
        {
            'name':'圣光（Holy Light）','cn':'圣光','en':'Holy Light','id':'holy_light','gear':'胸甲','max':'III','max_num':3,
            'intro':'自身生命值降至阈值时触发持续治疗场；只作用于自己，不生成 5×5 群体区域。',
            'level_headers':['等级','触发 / 持续 / 治疗间隔 / 每次治疗 / 冷却'], 'levels':[['I','生命 ≤20%；15 秒；20 tick；最大生命值 2%；7200 tick'],['II','生命 ≤20%；15 秒；20 tick；最大生命值 2.5%；7200 tick'],['III','生命 ≤20%；15 秒；20 tick；最大生命值 3%；7200 tick']],
            'rules':['只在自身有效胸甲装备且冷却结束时触发；治疗每 20 tick 一次，持续 15 秒。','冷却固定 7200 tick（360 秒），重登和重新装备不绕过冷却。','移除胸甲后停止 WSEE 治疗，不影响其他治疗或状态；支持定向 reset_cooldown holy_light。'],
            'conflicts':'与护盾互斥。', 'commands':['/enchant @s wsee:holy_light 3','/wsee reset_cooldown holy_light']},
        {
            'name':'霜噬（Frostbite）','cn':'霜噬','en':'Frostbite','id':'frostbite','gear':'剑、矛','max':'III','max_num':3,
            'intro':'成功直接近战命中后增加原版 Frozen Ticks 并施加短暂缓慢，让原版冻结伤害和融化规则继续负责后续表现。',
            'level_headers':['等级','Frozen Ticks / 缓慢 / 持续'], 'levels':[['I','+30；缓慢 I；40 tick'],['II','+45；缓慢 I；60 tick'],['III','+60；缓慢 II；60 tick']],
            'rules':['成功直接近战伤害（包括剑气伤害）才能触发；投射物、流血、火焰、荆棘、爆炸、药水等不能触发。','冻结值受目标冻结要求上限约束，不添加直接伤害或自定义冻结效果；不递归触发自身。','已有更强缓慢时不覆盖；原版冻结伤害与融化仍是权威。'],
            'conflicts':'与火焰附加及流血冲突。', 'commands':['/enchant @s wsee:frostbite 3']},
        {
            'name':'炼化（Refining）','cn':'炼化','en':'Refining','id':'refining','gear':'镐','max':'I','max_num':1,
            'intro':'挖掘带有矿石标签且存在熔炼配方的矿石时，直接掉落对应熔炼产物，不经过熔炼炉阶段。',
            'level_headers':['等级','数值'], 'levels':[['I','原版铁、金、铜及其他模组矿石按其熔炼配方输出；保持原掉落数量与 Fortune 倍数']],
            'rules':['只处理 wsee:refinable_ores 标签内且能匹配熔炼配方的矿石；兼容 minecraft:ores、c:ores 和 forge:ores 标签，精准采集等非法组合仍由数据、铁砧、附魔台和运行时共同拒绝。','替换掉落物而不额外生成第二份矿物；Fortune 产生的数量保留，耐久消耗遵循原版挖掘。','不额外生成熔炼经验；掉落表现保持原版掉落事件。'],
            'conflicts':'与精准采集冲突。', 'commands':['/enchant @s wsee:refining 1']},
        {
            'name':'根基（Foundation）','cn':'根基','en':'Foundation','id':'foundation','gear':'护腿','max':'III','max_num':3,
            'intro':'装备带有根基的护腿时，使用原版 knockback_resistance 属性提高击退抗性。',
            'level_headers':['等级','击退抗性修饰'], 'levels':[['I','+0.15'],['II','+0.25'],['III','+0.35']],
            'rules':['只在护腿槽合法装备时应用 transient modifier；脱下、替换或等级改变时立即移除并按新等级替换。','修饰符使用稳定 UUID/标识，任何时刻不得重复叠加。','数据标签、运行时合法性、附魔台、铁砧、创造成书和文档必须同步。'],
            'conflicts':'无额外冲突。', 'commands':['/enchant @s wsee:foundation 1','/enchant @s wsee:foundation 2','/enchant @s wsee:foundation 3']},
        {
            'name':'空军（Air Force）','cn':'空军','en':'Air Force','id':'air_force','gear':'钓鱼竿','max':'I','max_num':1,
            'intro':'真实成功的钓鱼战利品事件中，用一个当前注册物品替换鱼类、垃圾或宝藏掉落。',
            'level_headers':['等级','结果'], 'levels':[['I','每次成功钓鱼只获得 1 个基础 ItemStack；鱼类概率固定为 0%']],
            'rules':['只在成功钓鱼战利品事件替换掉落；保留原版经验、鱼竿耐久、实体挂钩、装备状态和掉落表现。','候选排除 #wsee:air_force_fish、#wsee:air_force_unobtainable、#wsee:air_force_admin_blacklist、#wsee:air_force_blacklist 以及内置技术物品安全列表。','Luck of the Sea 只在 Common/Uncommon/Rare/Epic 池中按文档权重选择；空军与飞勾的非法组合由飞勾模式优先并抑制普通战利品。'],
            'conflicts':'与飞勾冲突。', 'commands':['/enchant @s wsee:air_force 1']},
        {
            'name':'飞勾（Flying Hook）','cn':'飞勾','en':'Flying Hook','id':'flying_hook','gear':'钓鱼竿','max':'V','max_num':5,
            'intro':'加速真实 FishingHook；飞行途中不受下坠影响，接触任意实体或方块表面后固定并自动牵引玩家。',
            'level_headers':['等级','鱼钩速度 / 最大距离 / 牵引速度'], 'levels':[['I','×1.15；24 格；0.55'],['II','×1.30；30 格；0.65'],['III','×1.45；36 格；0.75'],['IV','×1.60；42 格；0.85'],['V','×1.75；48 格；0.95']],
            'rules':['鱼钩飞行期间抵消重力下坠；接触任意实体，或方块的顶面、底面及侧面时，以精确命中位置建立锚点并立即开始牵引。','实体锚点保留命中点相对实体的位置并随实体移动；空气不能成为锚点，牵引使用 DeltaMovement，绝不瞬移。','距离 ≤1.5 格、持续 60 tick、10 tick 无进展、锚点丢失、跨维度、死亡、骑乘、滑翔或创造飞行时停止。','保留原版收杆耐久规则、摔落伤害重置/恢复、鱼钩碰撞和实体行为；飞勾模式优先于强制非法的饵钓、海之眷顾和空军。'],
            'conflicts':'与原版饵钓、海之眷顾及空军冲突；耐久、经验修补和消失诅咒可共存。', 'commands':['/enchant @s wsee:flying_hook 5']},
        {
            'name':'二段跳（Double Jump）','cn':'二段跳','en':'Double Jump','id':'double_jump','gear':'靴子','max':'I','max_num':1,
            'intro':'离地后提供一次服务端验证的空中跳跃，保留水平速度并直接设置 Y 速度。',
            'level_headers':['等级','额外跳跃 / Y 速度 / 安全高度'], 'levels':[['I','1 次；Y=0.65；摔落伤害起算高度 +2 格']],
            'rules':['通过服务端 isJumping() 上升沿、客户端按键事件和 tick 轮询共同识别新按键，防止按键重复消耗；发送运动同步包。','第二跳保留 X/Z，Y 直接设为 0.65，不叠加当前 Y；只在真实 onGround 接触时重置可用次数。','墙面、实体、蜘蛛网、梯子、游泳、骑乘、鞘翅、旁观、创造飞行都不能刷新；第二跳将 fall distance 清零，之后恢复原版下落。','摔落阈值额外增加 2 格但不乘算或取消最终伤害，与摔落保护兼容。'],
            'conflicts':'无额外冲突。', 'commands':['/enchant @s wsee:double_jump 1']},
        {
            'name':'坠星（Falling Star）','cn':'坠星','en':'Falling Star','id':'falling_star','gear':'重锤','max':'III','max_num':3,
            'intro':'有效高空 MACE_SMASH 直接命中后，在直接命中位置周围施加短暂缓慢；不造成额外伤害。',
            'level_headers':['等级','最低坠落 / 半径 / 缓慢 / 持续'], 'levels':[['I','4 格；3 格；缓慢 I；40 tick'],['II','4 格；4 格；缓慢 I；60 tick'],['III','4 格；5 格；缓慢 II；60 tick']],
            'rules':['必须在原版重锤处理前记录 fallDistance；仅有效 MACE_SMASH、直接命中 LivingEntity、最终生命伤害 >0 且预命中坠落距离 ≥4 格时触发。','中心固定为直接命中实体位置；排除攻击者和正常攻击规则下的队友/友军，只搜索 LivingEntity。','不创建第二次伤害、不调用范围 hurt()；已有更强缓慢不覆盖，同级短时间可刷新。','只生成少量坠星粒子和一次冲击音，不生成 FallingBlock、Projectile，不破坏方块。'],
            'conflicts':'无额外冲突；可与震环及原版重锤附魔共存。', 'commands':['/enchant @s wsee:falling_star 1','/enchant @s wsee:falling_star 2','/enchant @s wsee:falling_star 3']},
        {
            'name':'震环（Shock Ring）','cn':'震环','en':'Shock Ring','id':'shock_ring','gear':'重锤','max':'III','max_num':3,
            'intro':'有效 MACE_SMASH 直接命中后，以直接命中位置为中心逐 tick 扩散冲击环，造成低比例范围伤害并击退。',
            'level_headers':['等级','最大半径 / 速度 / 伤害 / 水平击退'], 'levels':[['I','3 格；0.60 格/tick；重击基准 ×12%；0.30'],['II','4 格；0.75 格/tick；重击基准 ×16%；0.45'],['III','5 格；0.90 格/tick；重击基准 ×20%；0.60']],
            'rules':['仅有效 MACE_SMASH 直接命中 LivingEntity 且最终生命伤害 >0 时创建实例；中心固定，多个实例可并行。','每 tick 只检测 previousRadius 到 currentRadius 的 0.65 格环带；每个目标在同一实例中最多命中一次。','基准取防御计算前的重击伤害；使用 wsee:shock_ring DamageType，按护甲、保护、盾牌和抗性正常防御，并归属原攻击者。','排除攻击者、直接目标、友军/PvP 无效目标、垂直差 >1.75 格或被实体方块遮挡的目标；水平击退按抗性缩放并加 Y=0.08。','震环伤害是间接伤害，禁止递归触发坠星、越战越勇、连斩、流血、霜噬、处决等直接命中附魔；粒子固定在地面圆周并只播放一次冲击音。'],
            'conflicts':'无额外冲突；可与坠星、minecraft:density、minecraft:breach、minecraft:wind_burst 共存。', 'commands':['/enchant @s wsee:shock_ring 1','/enchant @s wsee:shock_ring 2','/enchant @s wsee:shock_ring 3']},
    ]

    add_heading(doc, '4. 附魔总览', 1)
    overview = [[x['cn'], x['en'], x['id'], x['gear'], x['max'], x['conflicts'].replace('无额外冲突。','无') ] for x in enchantments]
    add_table(doc, ['中文名','English','ID','适用装备','最高等级','主要冲突'], overview, [1150, 1500, 1500, 2300, 900, 2010], font_size=7.9)
    add_para(doc, '总数核对：31 个。除“闪”外，中文名称均不使用旧版本别名；“闪”的英文固定为 Deflect。当前重锤附魔为坠星与震环。', 10.2, GRAY, italic=True)

    add_heading(doc, '5. 冲突与合法性矩阵', 1)
    add_table(doc, ['附魔','冲突 / 前置','装备限制'], [
        ['剑气','minecraft:sweeping_edge；连斩','剑'], ['连斩','剑气；不得与越战越勇冲突','剑'],
        ['扩容','聚束；minecraft:multishot','弩'], ['聚束','扩容；minecraft:multishot','弩'],
        ['流血','minecraft:lunge；霜噬','矛标签'], ['霜噬','minecraft:fire_aspect；流血','剑、矛'],
        ['涌流','minecraft:riptide','投掷三叉戟'], ['牵引','minecraft:riptide、涌流、回旋','投掷三叉戟'],
        ['回旋','minecraft:riptide、牵引；需要 minecraft:loyalty','投掷三叉戟'], ['震水','minecraft:riptide','投掷三叉戟'],
        ['箭雨','爆矢','弓'], ['护盾','圣光','胸甲'], ['圣光','护盾','胸甲'],
        ['急行军','minecraft:frost_walker','靴子'], ['炼化','minecraft:silk_touch','镐'],
        ['飞勾','minecraft:lure、minecraft:luck_of_the_sea、空军','钓鱼竿'],
    ], [1500, 5200, 2660], font_size=8.3)
    add_para(doc, '统一合法性要求：命令、铁砧、附魔台和运行时都拒绝不支持的装备或非法冲突；创造模式只显示有效附魔书。', 10.2, '9B1C1C', True)

    add_heading(doc, '6. 三叉戟与投射物的共同约束', 1)
    for t in [
        '涌流、牵引、回旋、震水只处理投掷中的三叉戟实体；三叉戟近战攻击永远不能进入这些路径。',
        '投射物生成时捕获附魔等级，确保命中、返回和跨 tick 状态不会因手中物品变化而漂移。',
        '所有生成的箭、雨箭和爆炸箭都必须使用标记阻断同类递归；原版碰撞、护甲、保护、盾牌和无敌帧保持权威。',
        '飞勾飞行途中不下坠；任意实体或方块表面都可成为锚点，接触后立即自动牵引，空气不能成为锚点。',
    ]:
        add_bullet(doc, t)

    add_heading(doc, '7. 按类别的详细规格', 1)
    categories = [
        ('近战与盾牌', ['剑气（Sword Aura）','盾击（Shield Bash）','“闪”（Deflect）','连斩（Combo Strike）','越战越勇（Battle Momentum）','处决（Execution）','流血（Bleeding）','长锋（Long Edge）','霜噬（Frostbite）']),
        ('弓、弩与箭矢', ['扩容（Expansion）','聚束（Cluster）','穿甲（Armor Piercing）','蓄势（Overcharge）','爆矢（Explosive Arrow）','箭雨（Arrow Rain）','追踪（Tracking）']),
        ('投掷三叉戟', ['涌流（Surge）','牵引（Tether）','回旋（Rebound）','震水（Hydroshock）']),
        ('护甲与生存', ['急行军（Forced March）','明（Clarity）','护盾（Shield）','圣光（Holy Light）','根基（Foundation）','二段跳（Double Jump）']),
        ('工具、钓鱼与重锤', ['炼化（Refining）','空军（Air Force）','飞勾（Flying Hook）','坠星（Falling Star）','震环（Shock Ring）']),
    ]
    by_name = {x['name']: x for x in enchantments}
    for cat, names in categories:
        add_heading(doc, cat, 2)
        for name in names:
            add_enchantment(doc, by_name[name])

    add_heading(doc, '8. 指令、资源与验收清单', 1)
    add_heading(doc, '冷却重置指令', 2)
    add_table(doc, ['命令','作用'], [
        ['/wsee reset_cooldown shield','只重置护盾冷却并清除当前 WSEE 吸收值'],
        ['/wsee reset_cooldown holy_light','只重置圣光冷却'],
        ['/wsee reset_cooldown shield_bash','只重置盾击 50 tick 冷却'],
    ], [4300, 5060], font_size=8.7)
    add_heading(doc, '资源与构建验收', 2)
    for t in [
        '使用 Java 25 分别执行 Fabric 与 NeoForge 的 clean build；不自动启动 Minecraft、PCL 或测试世界。',
        '逐个解析所有 JSON 资源；检查 JAR 内包含正确的 data/wsee、assets/wsee、Fabric metadata 或 META-INF/neoforge.mods.toml。',
        '检查 zh_cn.json/en_us.json 的 enchantment.*.desc 是单字符串，使用分号分隔 Tooltip 行，不写入字面换行控制符；客户端行颜色必须为 DARK_GRAY（85,85,85）。',
        '确认 advancedItemTooltips:false；F3+H 显示的物品 ID 和组件数量不属于附魔描述。',
        '构建后计算 SHA-256，并将对应平台 JAR 复制到相应测试实例 mods/；同一实例不能混放两端 JAR。',
        '人工烟测至少覆盖：合法/非法附魔、剑气挥空、盾击冷却、飞勾无下坠及实体/方块各表面自动锚定牵引、炼化直接出锭、涌流投掷水中触发、箭雨实体/方块命中、二段跳一次性、坠星/震环有效 MACE_SMASH。',
    ]:
        add_bullet(doc, t)

    add_heading(doc, '9. 变更记录与维护边界', 1)
    add_para(doc, '本文件替代旧的单端或旧版本详细数值文档。后续每次附魔、资源、语言、版本或平台行为变更，都必须同时更新：两端源码与资源、README、本文档以及对应 AGENTS.md 规则；若数值尚未在游戏中实现，不得先写入本文档。')
    add_para(doc, '当前正式身份：WSEE 0.1.0 · Minecraft 26.3 · Fabric + NeoForge · mod id wsee · 作者 ZX_Wu_Sheng · 鸣谢 KsqsnK，Badnng，Wangmeili，Niorris。', 10.2, GRAY, italic=True)

    doc.save(OUT)
    print(OUT)


if __name__ == '__main__':
    main()
