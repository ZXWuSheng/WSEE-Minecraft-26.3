"""将两端当前隔离运行的原始轨迹汇总成中文验收记录，失败时返回非零。"""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RESULT = re.compile(r"distance=([\d.]+) axis=(\d+) reelTicks=(\d+) minGap=([\d.eE+-]+) maxRadial=([\d.eE+-]+) end=(\S+)")


def read_run(directory):
    result = (directory / "movement-qa-result.txt").read_text(encoding="utf-8")
    counts = dict(re.findall(r"^(assertions|failures|approvedSamples)=(\d+)$", result, re.M))
    events = [json.loads(line) for line in (directory / "movement-qa-trace.jsonl").read_text(encoding="utf-8").splitlines()]
    for event in reversed(events):
        if event["type"] == "result":
            samples = re.search(r"approvedSamples=(\d+)", event["detail"])
            if samples:
                counts["approvedSamples"] = samples.group(1)
            break
    trajectories = []
    for event in events:
        if event["type"] == "hook-result":
            match = RESULT.search(event["detail"])
            if match:
                distance, axis, ticks, gap, radial, end = match.groups()
                trajectories.append(dict(distance=float(distance), axis=int(axis), ticks=int(ticks),
                                         gap=float(gap), radial=float(radial), end=end))
    return result, counts, events, trajectories


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--neoforge", type=Path, default=ROOT / "build/qa-neoforge")
    parser.add_argument("--fabric", type=Path, default=ROOT.parent.parent / "jianqi - fa/build/qa-fabric")
    parser.add_argument("--output", type=Path, default=ROOT / "build/reports/运动运行验收.md")
    args = parser.parse_args()
    lines = ["# 二段跳与飞钩运行验收", "", "本记录读取真实专用服务端、两个本地 TCP 协议客户端的当前运行结果。轨迹使用一次原版物理步生成客户端位置，再通过真实移动包交给服务端验证；未启动完整图形客户端，不代表渲染或主观操控手感已验证。", "",
             "| 加载器 | 运行断言 | 失败 | 实际收包审核 | 轨迹到达 |", "| --- | ---: | ---: | ---: | ---: |"]
    data = []
    passed = True
    for loader, directory in (("NeoForge", args.neoforge), ("Fabric", args.fabric)):
        result, counts, events, paths = read_run(directory)
        count = int(counts.get("assertions", 0))
        failures = int(counts.get("failures", 1))
        arrived = sum(path["end"] == "arrival" for path in paths)
        passed &= count > 0 and failures == 0 and len(paths) == 9 and arrived == 9
        samples = int(counts.get("approvedSamples", 0))
        passed &= samples > 0
        lines.append(f"| {loader} | {count} | {failures} | {samples} | {arrived}/{len(paths)} |")
        data.append((loader, directory, result, events, paths))
    lines += ["", "## 收绳轨迹", "", "距离场景在钩头真实碰撞锚定后调整夹具玩家起点，覆盖不同合法绳长；完整远距离发射与玩家操控仍需客户端确认。计时单位为 tick（正常服务器每秒 20 tick）。最大径向速度包含已有自然运动，绳方向改变也会重新投影既有惯性；飞钩单次加速度与额外牵引上限另由运行断言检查。实际收包审核逐包检查增量不超服务端批准大小、只沿批准方向、切向速度保持，以及正向新增力不会让径向速度超过原值与授权上限中的较大值；原有外力和二段跳不被裁剪。", "",
              "| 加载器 | 初始距离（格） | 方向 | 到达耗时（tick） | 最小碰撞箱间距（格） | 最大径向速度（格/tick） | 结束原因 |",
              "| --- | ---: | --- | ---: | ---: | ---: | --- |"]
    for loader, directory, result, events, paths in data:
        for path in paths:
            lines.append(f"| {loader} | {path['distance']:g} | {('正面', '斜向', '向上')[path['axis']]} | {path['ticks']} | {path['gap']:.3f} | {path['radial']:.3f} | {'到达' if path['end'] == 'arrival' else '提前结束'} |")
    lines += ["", "## 验收覆盖", "", "两端均使用真实服务端和两条独立 TCP 玩家连接：静止、疾跑、上升、下落、外部推力五种二段跳；长按、重复及非法请求；命中等待后收绳；收绳途中二段跳；半砖与台阶碰撞；实体锚点跟随；原始主副手归属；中间障碍终止；两玩家同时使用互不干扰；所有者与观察者收到相同精确锚点。", "",
              "清理包括取消、锚点破坏、移除鱼竿、重复移除钩头、切换维度、死亡和退出；结束后继续观察五个 tick，确认服务端不再授权拉力。普通钓鱼与十六次空军调用走实际原版 retrieve、掉落、经验及耐久分支。", "",
              "## 参数调试依据", "", "二段跳增量为 0.65；收绳速度为 0.35；最大牵引加速度为 0.24；正向新增力的径向上限为 0.55；到达距离为玩家碰撞箱间距 1.5；到达边界外 3 格逐步减力至半张力；持续上限 120 tick，受阻无进展 10 tick 终止。原版距离限制为 32 格，飞钩只有 I 级。", "",
              "实测发现原加速度不足以稳定完成向上收绳，近端减力到零也会停在到达边界外，因此提高有限加速度并保留半张力。接近锚点只减少新增拉力，不主动裁剪现有速度；重力、阻力、碰撞与到达清理继续生效。这些是轨迹与规则验证，真人操作的舒适程度仍需完整客户端确认。", "",
              "## 原始证据", ""]
    for loader, directory, result, events, paths in data:
        lines += [f"- {loader}：[断言结果](<{(directory / 'movement-qa-result.txt').as_posix()}>)；[逐 tick 原始轨迹](<{(directory / 'movement-qa-trace.jsonl').as_posix()}>)；[服务端日志](<{(directory / 'logs/latest.log').as_posix()}>)。"]
        if "failures=0" not in result:
            lines += ["", "```text", result.strip(), "```", ""]
    lines += ["", "## 仍需完整客户端验证", "", "真实绑定按键、绳索渲染、另一名玩家的视觉观察、网络延迟下的操控体验，以及与其它运动模组共同安装时的手感。自动化的成功钓鱼测试设置了原版咬钩状态作为夹具，验证实际 retrieve／渔获／经验／耐久分支，未模拟完整等待鱼咬过程。", ""]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text("\n".join(lines), encoding="utf-8")
    print(json.dumps({"通过": passed, "记录": str(args.output)}, ensure_ascii=False))
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
