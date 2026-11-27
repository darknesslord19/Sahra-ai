/* Sahra 1: elli orijinal gövde */
const GOVDE_RENKLERI={beyaz:'#e9edf4',siyah:'#3b404d',mavi:'#3d8cec',turkuaz:'#1fbfb8',yesil:'#3cb556',mor:'#8a5ce8',pembe:'#ee66a4',kirmizi:'#de453c',altin:'#e0ab2e'};
/* el: avuç + üç parmak + başparmak (ya da kıskaç) */
function elYap(m,olcu,kiskac){const g=grup(),ic=g.ekle(grup([0,0,0],[0,0,0],[olcu,olcu,olcu]));const kap=sekil(1,1,1,1,1,18,12);
 const parmak=(x,boy,kal=.3)=>{const k=ic.ekle(grup([x,.55,0]));k.ekle(dugum(kap,m,[0,boy*.5,0],[0,0,0],[kal,boy*.62,kal]));return k};
 if(kiskac){ic.ekle(dugum(kap,m,[0,.1,0],[0,0,0],[.75,.6,.55]));const q=halka(1,.24,20,10,Math.PI*.82);
  const a=ic.ekle(grup([-.35,.75,0])),b=ic.ekle(grup([.35,.75,0]));a.ekle(dugum(q,m,[.05,.4,0],[0,0,Math.PI*.62],[.66,.66,1]));b.ekle(dugum(q,m,[-.05,.4,0],[0,Math.PI,Math.PI*.62],[.66,.66,1]));g.sekil=ad=>{const k=ad==='yumruk'?.34:ad==='isaret'?.12:0;a.r[2]=-k;b.r[2]=k};return g}
 ic.ekle(dugum(sekil(1,1,1,.8,.8,24,16),m,[0,.12,0],[0,0,0],[.82,.8,.48]));
 const p=[parmak(-.48,.78),parmak(0,.9),parmak(.48,.78)];
 const bas=ic.ekle(grup([-.7,.05,.08]));bas.ekle(dugum(kap,m,[0,.38,0],[0,0,0],[.3,.5,.3]));
 g.sekil=(ad,yan)=>{ic.s[0]=olcu*yan;p.forEach((k,i)=>{const acik=ad==='acik'||(ad==='isaret'&&i===0)||(ad==='zafer'&&i<2);k.s[1]=acik?(ad==='isaret'?1.35:1):.2;k.r[2]=ad==='acik'?(1-i)*.12:ad==='zafer'?(i===0?.22:-.1):0});
  if(ad==='begen'){bas.r[2]=.12;bas.s[1]=1.45;bas.t[0]=-.55;bas.t[1]=.55}else{bas.r[2]=ad==='acik'?.75:-.55;bas.s[1]=1;bas.t[0]=-.7;bas.t[1]=.05}};
 return g}
/* kol: omuzdan ele iki parçalı; tip 'yuzen' ise yalnız el görünür */
function kolYap(k,yan,o){const kok=o.kok,om=[yan*o.omuz[0],o.omuz[1],o.omuz[2]||0],c=cubukGeo(),top=sekil(1,1,1,1,1,16,12);
 const o1=dugum(c,o.mat),o2=dugum(c,o.mat2||o.mat),d=dugum(top,o.eklem||o.mat),om0=dugum(top,o.eklem||o.mat,om,[0,0,0],[o.kalin*1.35,o.kalin*1.35,o.kalin*1.35]),el=elYap(o.elMat,o.el,o.kiskac);
 if(o.tip!=='yuzen')kok.ekle(o1,o2,d,om0);kok.ekle(el);
 return{yan,om,indir:o.indir||0,yuzY:o.yuzY,boy:o.boy,el,parcalar:[o1,o2,d,om0,el],tip:o.tip,
  /* hedef: omuza göre el yeri (x dışa doğru), kol boyu 1 kabul edilir */
  koy(h,yon,bicim){const y=this.yan;let B=o.boy,u=[h[0]*y*B,h[1]*B,h[2]*B];
   /* el gövdenin içine girmesin: öndeyse yüzeyin dışına itilir */
   const q0=V.ek(om,u);if(o.tip==='yuzen')q0[0]+=y*(o.acik||0);
   for(const[cx,cy,cz,rx,ry,rz]of o.hacim||[]){const d=1-((q0[0]-cx)/rx)**2-((q0[1]-cy)/ry)**2;if(d>0){const zy=cz+rz*Math.sqrt(d);if(q0[2]<zy&&q0[2]>cz-rz*.2){const k=Math.min(1,(q0[2]-cz+rz*.2)/(rz*.35));q0[2]+=(zy-q0[2])*k}}}
   u=V.cik(q0,om);if(o.tip==='yuzen')u[0]-=y*(o.acik||0);
   if(o.tip==='yuzen'){const q=V.ek(om,[u[0]+y*(o.acik||0),u[1],u[2]]);el.m=yonel(q,[yon[0]*y,yon[1],yon[2]],[1,1,1]);el.sekil(bicim,y);return}
   const L=Math.max(B*.56,V.boy(u)/1.98);let l=Math.min(V.boy(u),L*1.995);const e=V.bir(u),son=V.ek(om,V.carp(e,l));
   let kut=V.bir([y*.8,-.5,-.35]);kut=V.bir(V.cik(kut,V.carp(e,V.nokta(kut,e))));const dir=V.ek(V.ek(om,V.carp(e,l/2)),V.carp(kut,Math.sqrt(Math.max(0,L*L-l*l/4))));
   const w=o.kalin;o1.m=yonel(om,V.cik(dir,om),[w,V.boy(V.cik(dir,om)),w]);o2.m=yonel(dir,V.cik(son,dir),[w*.92,V.boy(V.cik(son,dir)),w*.92]);d.t=dir;d.s=[w*1.18,w*1.18,w*1.18];
   el.m=yonel(son,[yon[0]*y,yon[1],yon[2]],[1,1,1]);el.sekil(bicim,y)}}}
const kure=()=>sekil(1,1,1);
function ekranYap(ana,motor,w,h,y,z,tw){const cv=document.createElement('canvas');cv.width=tw;cv.height=Math.round(tw*h/w);const tex=motor.doku();
 ana.ekle(dugum(yama(w,h,y,z),mal('#000000',{tex})));return{cv,c:cv.getContext('2d'),tex}}

const MODELLER=[
{id:'orijinal',ad:'Sahra Orijinal',not:'Şu anki robotunuz; artık her yöne dönüyor',renk:'#e9edf4',goz:'#bfe9ff',idle:'#86efc4',zemin:['#14151f','#05060b'],kose:[.33,.47],pay:.035,halka:true,uzak:9.3,
 kur(m,anaRenk){const kok=grup(),govde=kok.ekle(grup());const G=mal(anaRenk,{spec:.5,shine:40,glow:.9}),K=mal('#aab3c4');
  govde.ekle(dugum(kure(),G));
  const bas=govde;const ek=ekranYap(bas,m,1.84,1.30,0,elipsZ(1,1,1,2,.07),512);
  /* yalnız arkadan görünen ince servis kapağı */
  bas.ekle(dugum(halka(.3,.012),K,[0,0,-.968]),dugum(kure(),mal('#7fe7c0',{emis:.85}),[0,0,-1],[0,0,0],[.045,.045,.02]));
  const ko={kok:govde,indir:0.34,yuzY:0,hacim:[[0,0,0,1.12,1.12,1.14]],omuz:[.98,-.28,.12],boy:.92,tip:'yuzen',acik:.1,mat:G,elMat:G,el:.19,kalin:.07};
  return{kok,govde,bas,ek,kollar:[kolYap(m,-1,ko),kolYap(m,1,ko)],ayaklar:[],ekler:()=>{}}}}];
