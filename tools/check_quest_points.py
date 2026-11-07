"""Проверка очков навыков по сюжету: читает Quest.java, Balance.java и SkillTree.java мода. Запуск: python3 check_quest_points.py
   Для каждого класса, кагуне, сложности и игры в одиночку / в группе: хватает ли очков к концу сюжета на лимит 85%."""
import re, math
from decimal import Decimal, ROUND_HALF_UP, ROUND_CEILING
import os
J=os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'java', 'com', 'tokyoghoulrpg') + os.sep
B=open(J+'combat/Balance.java').read()
def num(name):
    m=re.search(r'\b'+name+r'\s*=\s*([0-9_.]+)', B); return Decimal(m.group(1).replace('_',''))
def arr(name):
    m=re.search(r'\b'+name+r'\s*=\s*\{([^}]*)\}', B); return [int(x) for x in m.group(1).split(',')]
GAIN=num('SKILL_POINT_GAIN'); HARDQ=num('HARD_QUEST_POINTS_FACTOR'); HARDC=num('HARD_SKILL_COST_FACTOR')
HGC=num('HALF_GHOUL_COST'); CAP=num('SKILL_CAP_SHARE'); PER_LVL=num('SKILL_POINTS_PER_LEVEL')
BOSS=num('BOSS_SKILL_POINTS'); HG_TOP=num('HG_TOP_POINTS'); HG_OTHER=num('HG_OTHER_POINTS')
def jround(x): return int(Decimal(x).quantize(Decimal(1), rounding=ROUND_HALF_UP))
def jceil(x): return int(Decimal(x).quantize(Decimal(1), rounding=ROUND_CEILING))
# --- дерево: цены навыков
S=open(J+'skill/SkillTree.java').read()
tabs={}
for line in S[S.index('public enum Tab'):S.index('enum Style')].splitlines():
    m=re.match(r'\s+([A-Z_]+)\("[a-z_]+",\s*(.*)\)[,;]?\s*$', line)
    if m:
        style=re.search(r'Style\.([A-Z]+)', m.group(2)); tabs[m.group(1)]=(style.group(1) if style else 'ANY', re.findall(r'PlayerClass\.([A-Z_]+)', m.group(2)))
bsec=S[S.index('public enum Branch'):S.index('private final String id;', S.index('public enum Branch'))]
bsec=re.sub(r'//[^\n]*','',bsec)
entries=re.findall(r'([A-Z_0-9]+)\(("[a-z_0-9]+"[^;]*?)\)(?=,\s*[A-Z_0-9]+\(|;)', bsec, re.S)
br=[]
for name,args in entries:
    parts=[p.strip() for p in args.split(',')]
    tab=parts[1].split('.')[1]
    lv=int(num(parts[2].split('.')[1])) if parts[2].startswith('Balance') else int(parts[2])
    cs=arr(parts[3].split('.')[1])
    extra=[p.split('.')[1] for p in parts[6:] if p.startswith('Tab.')]
    br.append((name,[tab]+extra,lv,cs))
assert len(br)==67, len(br)
def has(t,cls,var):
    st,cl=tabs[t]
    return cls in cl and (st=='ANY' or (st=='CLASSIC' and var not in('WINGS','GOURMET')) or st==var)
def cap(cls,var,hard):
    tot=0
    for name,ts,lv,cs in br:
        if not any(has(t,cls,var) for t in ts): continue
        for tier in range(1,lv+1):
            c=cs[min(tier,len(cs))-1]
            if cls=='HALF_GHOUL': c=jround(c*HGC)
            if hard: c=jceil(c*HARDC)
            tot+=c
    return int((Decimal(tot)*CAP).to_integral_value(rounding='ROUND_FLOOR'))
# --- задания из Quest.java
Q=open(J+'quest/Quest.java').read()
quests=re.findall(r'^\s{4}([A-Z_0-9]+)\("([a-z_0-9]+)", Chain\.([A-Z_]+), Trigger\.([A-Z_]+), (\d+), (\d+), (\d), (null|\d+)\)', Q, re.M)
chains={}
for c,qid,ch,trig,goal,rew,act,blood in quests: chains.setdefault(ch,[]).append((qid,trig,int(goal),int(rew)))
CL={'GHOUL':('GHOUL',['NONE','WINGS','GOURMET']),'HALF_GHOUL':('HALF_GHOUL',['NONE','WINGS','GOURMET']),'HUMAN':('HUMAN',['NONE']),'CCG':('CCG_AGENT',['NONE'])}
def earn(cls,base): return base if cls=='HALF_GHOUL' else jround(base*GAIN)
def replay(ch,hard,solo):
    cls,vars_=CL[ch]; events=[]; lvl=0
    for qid,trig,goal,rew in chains[ch]:
        if trig=='LEVEL' and goal>lvl:
            for L in range(lvl+1,goal+1): events.append(('level',earn(cls,int(PER_LVL))))
            lvl=goal
        if trig in('KILL_BOSS','KILL_GHOUL_BOSS','KILL_CCG_BOSS'): events.append(('boss',earn(cls,int(BOSS))))
        if trig=='QUINQUE_REWARD': events += [('boss',earn(cls,int(BOSS)))]*2
        if trig=='KILL_HALF_GHOUL_BOSS':
            events.append(('arena', earn(cls,int(BOSS))+earn(cls,int(HG_TOP)) if solo else earn(cls,int(HG_OTHER))))
        base = jceil(rew*HARDQ) if hard else rew
        events.append(('quest '+qid, earn(cls,base)))
    pts=sum(v for _,v in events)
    return pts, {v:cap(cls,v,hard) for v in vars_}, lvl
ok=True
print('заданий в Quest.java:', {k:len(v) for k,v in chains.items()})
for ch in chains:
    for hard in (False,True):
        for solo in (True,False):
            pts,caps,lvl=replay(ch,hard,solo)
            worst=min(pts*100//c for c in caps.values())
            ok &= worst>=100
            print(f'{ch:10s} {"сложная" if hard else "нормальная":10s} {"один" if solo else "группа":6s} ур.{lvl} очков {pts:4d} лимит {caps} -> {worst}%')
print('ВСЁ ХВАТАЕТ' if ok else 'ЕСТЬ НЕХВАТКА')
