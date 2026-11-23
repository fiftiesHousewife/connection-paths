"""Walks OpenRewrite's call graph with connection-paths' rules, and compares the result with connection-paths' facts.

Usage: python3 compare_rewrite.py <datatables folder> <connection-paths facts .json> [<connection-rules.txt>]
The rules default to this repo's built-in ones. Calls through an interface or superclass are followed through
OpenRewrite's class hierarchy, as connection-paths does: not through java. or javax. types, and not where more than
three methods override.
"""
import collections, csv, json, os, re, sys

if len(sys.argv) < 3:
    sys.exit(__doc__)
D = os.path.join(sys.argv[1], '')
RULES = sys.argv[3] if len(sys.argv) > 3 else os.path.join(
    os.path.dirname(os.path.abspath(__file__)), '../../../src/main/resources/connection-rules.txt')
read=lambda f: list(csv.DictReader(l for l in open(D+f) if not l.startswith('#')))
calls=[r for r in read('org.openrewrite.table.CallGraph.csv') if r['action']=='CALL']
hier=read('org.openrewrite.java.table.ClassHierarchy.csv')
low=read('org.openrewrite.table.LowConfidenceFiles.csv')
facts=json.load(open(sys.argv[2]))
def simple(t):
    while re.search(r'<[^<>]*>',t): t=re.sub(r'<[^<>]*>','',t)
    t=t.replace('...','[]').strip(); return t.rsplit('.',1)[-1] if '.' in t else t
def args(a): return ','.join(simple(x) for x in re.split(r',(?![^<]*>)',a) if x.strip()) if a else ''
key=lambda c,n,a: f"{c}#{n or '<fields>'}({args(a)})"
loose=lambda c,n,a: f"{c}#{n or '<fields>'}/{len([x for x in re.split(r',(?![^<]*>)',a) if x.strip()]) if a else 0}"
rules=[l.split() for l in open(RULES) if l.strip() and not l.startswith('#')]
rules=[(k,)+tuple(r.split('#')) for k,r in rules]
callers=collections.defaultdict(set); callers_loose=collections.defaultdict(set); declared=set(); points={}
unresolved=0
for r in calls:
    if not r['toClass'] or r['toClass'] in ('null','Unknown') : unresolved+=1; continue
    f=key(r['fromClass'],r['fromName'],r['fromArguments']); t=key(r['toClass'],r['toName'],r['toArguments'])
    declared.add(f); callers[t].add(f); callers_loose[loose(r['toClass'],r['toName'],r['toArguments'])].add(f)
    if r['toName']=='<init>' : callers[f"{r['toClass']}#<fields>()"].add(f)
    for k,typ,m in rules:
        if r['toClass']==typ and (m==r['toName'] or (m=='*' and r['toName']!='<init>')): points[f]=k
parents={h['className']:[p for p in [h['superclass']]+h['interfaces'].split(';')+h['interfaces'].split(',') if p] for h in hier}
def anc(c):
    seen=[];q=list(parents.get(c,[]))
    while q:
        a=q.pop()
        if a not in seen: seen.append(a); q+=parents.get(a,[])
    return seen
impls=collections.defaultdict(list)
for m in declared:
    c,rest=m.split('#',1); n=rest.split('(')[0]; cnt=0 if rest.endswith('()') else rest.count(',')+1
    if n in ('<init>','<fields>'): continue
    for a in anc(c):
        if not a.startswith(('java.','javax.')): impls[f"{a}#{n}/{cnt}"].append(m)
over=collections.defaultdict(list)
for k,ms in impls.items():
    if len(ms)<=3:
        for m in ms: over[m].append(k)
def reach(starts):
    seen=set(starts); q=list(starts)
    while q:
        m=q.pop()
        for c in callers.get(m,set())|set().union(*[callers_loose.get(k,set()) for k in over.get(m,[])] or [set()]):
            if c not in seen: seen.add(c); q.append(c)
    return seen
alltests=set()
for r in calls:
    if r['fromSourceSet']=='test': alltests.add(key(r['fromClass'],r['fromName'],r['fromArguments']))
R=reach(list(points))
ours={t['test'] for t in facts['tests']}
ours_direct={t['test'] for t in facts['tests'] if any(not r['through_set_up'] for r in t['reaches'])}
theirs={m.split('(')[0].replace('#','.') for m in R if m in alltests}
print('calls:',len(calls),'| without a resolved target:',unresolved,'| files with incomplete extraction:',len(low))
print('connection points (exact rule types only):',len(points),collections.Counter(points.values()))
print('ours:',len(facts['connection_points']),collections.Counter(p['kind'] for p in facts['connection_points']))
print('tests reaching (ours, any):',len(ours),'| ours, direct from the test body:',len(ours_direct),'| OpenRewrite graph, test-set methods reaching:',len(theirs))
both=ours_direct & theirs; print('direct reach: both',len(both),'| only ours',len(ours_direct-theirs),'| only OpenRewrite (incl. non-test helpers)',len(theirs-ours))
print('examples only ours:',sorted(ours_direct-theirs)[:3]); print('examples only OpenRewrite:',sorted(theirs-ours)[:5])

print('\n=== DETAIL')
norm=lambda m: m.split('(')[0]
ourpts={}
for p in facts['connection_points']: ourpts.setdefault(norm(p['method']),set()).add((p['kind'],p['call']))
rwpts={}
for r in calls:
    for k,typ,m in rules:
        if r['toClass']==typ and (m==r['toName'] or (m=='*' and r['toName']!='<init>')):
            rwpts.setdefault(f"{r['fromClass']}#{r['fromName'] or '<fields>'}",set()).add((k,f"{typ}#{m}"))
print('points only connection-paths found:'); [print('  ',m.rsplit('.',1)[-1],sorted(v)) for m,v in sorted(ourpts.items()) if m not in rwpts]
print('points only OpenRewrite found:'); [print('  ',m.rsplit('.',1)[-1],sorted(v)) for m,v in sorted(rwpts.items()) if m not in ourpts]
print('our unresolved, top 8:'); [print('  ',r['count'],r['reason'][:110],'|',r['example'][:90]) for r in facts['not_resolved'][:8]]
print('summary:',facts['summary'])
print('OpenRewrite low-confidence files:',sorted({l['sourcePath'] for l in low})[:40])
print('OpenRewrite rows by source set:',collections.Counter(r['fromSourceSet'] for r in calls))
fields=[r for r in calls if not r['fromName']]; print('OpenRewrite calls from a class body (field initialisers):',len(fields))
lam=[r for r in calls if 'lambda' in r['fromName'].lower()]; print('OpenRewrite calls attributed to lambdas:',len(lam))
