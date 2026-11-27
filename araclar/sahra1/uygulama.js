/* Uygulama köprüsü: robot.html ile aynı işlevler (robot, robotIsik, robotYazi, robotTepki) ve robotAyar */
const tuval=document.getElementById('tuval');let motor=null,R=null;
const S={durum:'idle',renk:'beyaz',kip:'rastgele',konusuyor:false,
 poz:'oto',hareket:'suzul',yon:0,donus:'sabit',agiz:'oto',eller:true,hiz:100};
let isik=null,tepki=null,yazi='',yaziBas=0,surukle=0,surukleAn=0;
/* göz ifadelerinin süreleri (robot.html ile aynı), saniye */
const SURE={heart:2.2,wink:1.1,side:1.7,happy:1.6,wow:1.5,sleepy:3,roll:1.7,angry:1.5,sad:2.6,star:2.2,dizzy:1.8,laugh:1.7,scan:1.9,cool:2.4,music:2.4,think:2.4,suspect:2.6,pixel:2,xeyes:1.6,excited:1.5};
/* Otomatik el-kol: ara sıra bir hareket ve ona uyan göz */
const JESTLER=[['selam','happy'],['kalp','heart'],['begen','wink'],['dusun','think'],['dans','music'],['bilmem','suspect'],['cifte','star'],['kucak','happy'],['sevinc','excited'],['zafer','cool'],['alkis','laugh']];
/* dokunma tepkileri: [süre, göz, el-kol, hareket] */
const TEPKILER={
 tut:[1.2,'wow','sevinc','sabit'],birak:[1.4,'happy','selam','zipla'],firlat:[1.8,'excited','kanat','zipla'],
 kirp:[1.2,'wink','begen','merak'],gidik:[2.6,'laugh','sakla','titre'],kizgin:[3.2,'angry','bel','hayir'],
 salla:[3.6,'xeyes','bilmem','titre'],don:[7,'dizzy','kanat','sersem']};
HAREKETLER.push(['sersem','Sersemleme',(t,o)=>{o.z_+=.3*Math.sin(t*4.2);o.x+=.2*Math.sin(t*2.9);o.y+=.07*Math.sin(t*5);o.basYat+=.18*Math.sin(t*3.3+1);o.yaw+=.5*Math.sin(t*2.1)}]);

const simdi={yaw:0,basYaw:0};
let T=0,once=0,sonCizim=0,kirpS=0,sonKirp=1.5,bakX=0,bakY=0,hedefBak=[0,0],sonBak=1;
let ifade=null,ifadeBit=0,sonrakiIfade=3,jest=null,jestBit=0,sonrakiJest=7,tepkiBas=0;
const lerp=(a,b,k)=>a+(b-a)*k;
const bul=(l,id)=>l.some(x=>x[0]===id);

function durus(r,t,pozId,harId,yaw,dt){const o={x:0,y:0,z_:0,yaw:0,basYaw:0,basEg:0,basYat:0,eg:0,ez:0,takla:0,yuv:0,adim:0,teker:0,bakX:0,bakY:0};
 HAREKETLER.find(x=>x[0]===harId)[2](t,o);
 const g=r.govde;if(!g.ana)g.ana=g.t.slice();
 r.kok.t=[o.x,o.y+.12,0];r.kok.r=[0,yaw+o.yaw,0];
 g.r=[o.eg+o.takla+o.basEg,o.basYaw,o.z_+o.yuv+o.basYat];g.s=[1+o.ez*.7,1-o.ez,1+o.ez*.7];
 const hedef=POZLAR.find(x=>x[0]===pozId)[2](t),k=1-Math.exp(-dt*12);
 if(!r.el)r.el=r.kollar.map(()=>({h:[.22,-.9,.08],y:[.12,-1,.05]}));
 r.kollar.forEach((kl,i)=>{const c=r.el[i],h=Object.assign({},hedef[i]);h.h=h.h.slice();
  if(h.m!==undefined)h.h[0]=(h.m*kl.boy-Math.abs(kl.om[0]))/kl.boy;if(h.m!==undefined&&h.e===undefined)h.h[1]-=kl.indir;if(h.e!==undefined)h.h[1]=(kl.yuzY+h.e-kl.om[1])/kl.boy;
  c.h=V.kar(c.h,h.h,k);c.y=V.kar(c.y,h.y,k);kl.koy(c.h,c.y,h.b);kl.parcalar.forEach(p=>p.gizli=!S.eller)});
 return o}

/* ---------- kenar ışığı: robot.html ile aynı kurallar ---------- */
const RAIN=['#22d3ee','#3b82f6','#8b5cf6','#d946ef','#ec4899'],YESIL=['#22c58b','#6ee7b7','#10b981'];
const hex=h=>[1,3,5].map(i=>parseInt(h.slice(i,i+2),16));
const kar=(a,b,t)=>a.map((v,i)=>Math.round(v+(b[i]-v)*t));
const yazHex=c=>'#'+c.map(v=>v.toString(16).padStart(2,'0')).join('');
const yumusak=t=>t*t*(3-2*t);
function dalga(s,p){if(s==='sabit')return 1;if(s==='yanson')return p<.15?yumusak(p/.15):p<.55?1:p<.75?1-yumusak((p-.55)/.2):0;
 if(s==='kalp'){const v=(m,g)=>Math.max(0,1-Math.abs(p-m)/g);return yumusak(Math.max(v(.14,.13),v(.40,.15)*.85))}return(1-Math.cos(2*Math.PI*p))/2}
function halkaBul(t,dinle){
 if(!isik||isik.sekil==='orijinal'||(dinle&&!isik.dinle))return{halka:{renkler:dinle?RAIN:YESIL,hiz:dinle?3.3:.35,guc:dinle?1:.62+.38*(.5+.5*Math.sin(t*1.96))},goz:null,guc:dinle?1:.45};
 const l=isik.renkler.map(hex),n=l.length,f=t/isik.sure,i=Math.floor(f)%n,p=f%1,s=isik.sekil;let c=l[i];
 if(s==='sabit'||s==='donen')c=kar(c,l[(i+1)%n],yumusak(p));else if(p>.85)c=kar(c,l[(i+1)%n],yumusak((p-.85)/.15));
 const k=isik.alt+(isik.ust-isik.alt)*dalga(s,p),d=s==='donen'&&n>1,ac=kar(c,[255,255,255],.45);
 return{halka:{renkler:d?isik.renkler:[yazHex(c),yazHex(ac),yazHex(c)],hiz:2*Math.PI/(isik.sure*1.4),guc:k},goz:isik.goz?yazHex(ac):null,guc:k}}

function neOynasin(){
 const dinle=S.durum==='active';
 /* öncelik: dokunma tepkisi, dinleme/konuşma, seçili ayarlar */
 if(tepki&&T<tepki.bit){const x=TEPKILER[tepki.ad];return{goz:x[1],poz:x[2],hareket:x[3]}}
 tepki=null;
 let goz=S.kip,poz=S.poz,hareket=S.hareket;
 if(S.kip==='rastgele'){
  if(ifade&&T>ifadeBit)ifade=null;
  if(!ifade&&T>sonrakiIfade&&!dinle){const a=Object.keys(SURE);ifade=a[Math.random()*a.length|0];ifadeBit=T+SURE[ifade];sonrakiIfade=ifadeBit+2.4+Math.random()*2.6}
  goz=ifade||'normal'}
 if(S.poz==='oto'){
  if(jest&&T>jestBit)jest=null;
  if(!jest&&T>sonrakiJest&&!dinle){jest=JESTLER[Math.random()*JESTLER.length|0];jestBit=T+2.8;sonrakiJest=jestBit+6+Math.random()*8}
  poz='dinlen';if(jest&&!dinle){if(bul(POZLAR,jest[0]))poz=jest[0];if(S.kip==='rastgele')goz=jest[1]}}
 if(dinle){if(S.kip==='rastgele')goz='normal';if(S.poz==='oto')poz=S.konusuyor?'dinlen':'kulak'}
 return{goz,poz,hareket}}

function ciz(dt){
 const w=Math.max(1,Math.round(innerWidth*Math.min(2,devicePixelRatio||1))),h=Math.max(1,Math.round(innerHeight*Math.min(2,devicePixelRatio||1)));
 if(tuval.width!==w||tuval.height!==h){tuval.width=w;tuval.height=h}
 const dinle=S.durum==='active',n=neOynasin();
 if(T>sonKirp){const f=T-sonKirp;kirpS=f<.16?Math.sin(f/.16*Math.PI):0;if(f>=.16)sonKirp=T+1.8+Math.random()*3}
 if(T>sonBak){hedefBak=Math.random()<.5?[0,0]:[Math.random()-.5,(Math.random()-.5)*.6];sonBak=T+1.2+Math.random()*2.4}
 bakX=lerp(bakX,hedefBak[0],.2);bakY=lerp(bakY,hedefBak[1],.2);
 if(T-surukleAn>.18)surukle*=Math.exp(-dt*6);
 let hedef=S.yon*Math.PI/180;if(S.donus==='oto')hedef+=Math.sin(T*.9)*1.05;
 /* dinlerken, yazı gösterirken ve tepki verirken yüz öne döner */
 if(dinle||yazi||tepki)hedef=0;
 hedef+=surukle*.8;
 if(S.donus==='tur'&&!dinle&&!yazi&&!tepki){simdi.yaw+=dt*1.5}else{let f=hedef-simdi.yaw;f=Math.atan2(Math.sin(f),Math.cos(f));simdi.yaw+=f*(1-Math.exp(-dt*6))}
 let fark=Math.atan2(Math.sin(hedef-simdi.yaw),Math.cos(hedef-simdi.yaw));if(S.donus==='tur'&&!dinle)fark=0;
 simdi.basYaw=lerp(simdi.basYaw,Math.max(-.55,Math.min(.55,fark*.7)),1-Math.exp(-dt*10));
 /* yazı okunurken gövde sade süzülür */
 const ht=tepki?T-tepkiBas:T*S.hiz/100;
 const o=durus(R,ht,n.poz,yazi?'suzul':n.hareket,simdi.yaw,dt);R.bas.r[1]+=simdi.basYaw*.4;
 const d=R.d,I=halkaBul(T,dinle),gr=I.goz||(n.goz==='normal'&&!dinle?d.idle:d.goz);
 yuzCiz(R.ek.c,{t:T,goz:n.goz,kirp:kirpS,bakX:o.bakX+bakX+simdi.basYaw*.8,bakY:o.bakY+bakY,renk:gr,zemin:d.zemin,kose:d.kose,pay:d.pay+.01,halka:I.halka,agiz:S.agiz,konusuyor:S.konusuyor&&!yazi,yazi:yazi,yaziSure:T-yaziBas,yaziBitti:()=>{yazi=''}});
 motor.yukle(R.ek.tex,R.ek.cv);
 const l=I.halka.renkler;motor.ciz(R.kok,renk(l[Math.floor(T*.7)%l.length]).map(v=>v*I.halka.guc*I.guc),8.8)}

function kare(z){requestAnimationFrame(kare);
 /* pil için saniyede en çok 30 kare */
 if(z-sonCizim<30)return;sonCizim=z;
 const dt=Math.min(.1,(z-once)/1000||0);once=z;T+=dt;ciz(dt)}

function renkUygula(){const d=MODELLER[0],h=S.renk==='beyaz'||!GOVDE_RENKLERI[S.renk]?d.renk:GOVDE_RENKLERI[S.renk];R.govdeMat.c=renk(h)}
window.robot=function(durum,renkAdi,kip){if(durum==='active')yazi='';S.durum=durum;S.renk=renkAdi;if(kip&&kip!==S.kip){S.kip=kip;ifade=null}if(R)renkUygula()};
window.robotIsik=function(a){isik=a};
window.robotKonus=function(v){S.konusuyor=!!v};
window.robotAyar=function(a){if(!a)return;
 if(a.poz==='oto'||bul(POZLAR,a.poz))S.poz=a.poz;if(bul(HAREKETLER,a.hareket))S.hareket=a.hareket;
 S.yon=+a.yon||0;S.donus=a.donus;if(bul(AGIZLAR,a.agiz))S.agiz=a.agiz;S.eller=a.eller!==false;S.hiz=Math.max(40,Math.min(180,+a.hiz||100))};
window.robotYazi=function(m){if(S.durum!=='idle'||tepki)return;yazi=String(m);yaziBas=T};
window.robotTepki=function(ad){const x=TEPKILER[ad];if(!x)return;
 /* süren baş dönmesini küçük tepkiler kesmez */
 if(tepki&&T<tepki.bit&&tepki.ad==='don'&&ad!=='don')return;
 yazi='';tepki={ad,bit:T+x[0]};tepkiBas=T};
window.robotSurukle=function(v){surukle=Math.max(-1,Math.min(1,+v||0));surukleAn=T};

window.robotHazir=false;
try{motor=new Motor(tuval);const d=MODELLER[0];R=d.kur(motor,d.renk);R.d=d;R.govdeMat=R.kollar[0].parcalar[0].mat;window.robotHazir=true;requestAnimationFrame(kare)}catch(e){console.error(e)}
