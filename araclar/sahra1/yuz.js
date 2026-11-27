/* Ekran yüz: bütün modeller aynı gözleri kullanır */
const GOZLER=[['normal','Normal'],['happy','Mutlu'],['laugh','Gülme'],['heart','Kalp'],['star','Yıldız'],['wink','Göz kırpma'],['wow','Şaşkın'],['excited','Heyecanlı'],['sad','Üzgün'],['angry','Kızgın'],['sleepy','Uykulu'],['dizzy','Başı döndü'],['xeyes','Bayıldı'],['cool','Havalı'],['music','Müzik'],['think','Düşünme'],['suspect','Şüpheli'],['pixel','Piksel'],['scan','Tarama'],['side','Yan bakış'],['roll','Göz devirme']];
const AGIZLAR=[['oto','İfadeye göre'],['yok','Ağız yok'],['gulus','Gülümseme'],['konus','Konuşma']];
function yuvarla(c,x,y,w,h,rx,ry){c.beginPath();c.moveTo(x+rx,y);c.lineTo(x+w-rx,y);c.ellipse(x+w-rx,y+ry,rx,ry,0,-Math.PI/2,0);c.lineTo(x+w,y+h-ry);c.ellipse(x+w-rx,y+h-ry,rx,ry,0,0,Math.PI/2);c.lineTo(x+rx,y+h);c.ellipse(x+rx,y+h-ry,rx,ry,0,Math.PI/2,Math.PI);c.lineTo(x,y+ry);c.ellipse(x+rx,y+ry,rx,ry,0,Math.PI,Math.PI*1.5);c.closePath()}
function kalpYol(c,r){c.beginPath();c.moveTo(0,r);c.bezierCurveTo(-r*1.5,0,-r*.9,-r*1.25,0,-r*.45);c.bezierCurveTo(r*.9,-r*1.25,r*1.5,0,0,r);c.closePath()}
function yildizYol(c,r){c.beginPath();for(let i=0;i<10;i++){const a=-Math.PI/2+i*Math.PI/5,k=i%2?r*.45:r;c.lineTo(k*Math.cos(a),k*Math.sin(a))}c.closePath()}
function yuzCiz(c,o){const W=c.canvas.width,H=c.canvas.height,t=o.t,ifade=o.goz;c.clearRect(0,0,W,H);
 const p=o.pay,w=W*(1-2*p),h=H*(1-2*p),x0=W*p,y0=H*p;
 const yol=(q=0)=>yuvarla(c,x0-q,y0-q,w+2*q,h+2*q,Math.min((w+2*q)/2,(w+2*q)*o.kose[0]),Math.min((h+2*q)/2,(h+2*q)*o.kose[1]));
 /* kenar halkası */
 if(o.halka){c.save();const g=c.createConicGradient(t*o.halka.hiz,W/2,H/2),l=o.halka.renkler;l.concat(l[0]).forEach((r,i)=>g.addColorStop(i/l.length,r));c.globalAlpha=o.halka.guc;c.shadowColor=l[0];c.shadowBlur=W*.04;c.fillStyle=g;yol(W*.026);c.fill();c.restore()}
 const z=c.createRadialGradient(W/2,H*.38,0,W/2,H/2,W*.62);z.addColorStop(0,o.zemin[0]);z.addColorStop(1,o.zemin[1]);c.fillStyle=z;yol();c.fill();
 c.save();yol();c.clip();
 const ana=ifade==='heart'?'#ff5a9c':ifade==='star'?'#ffcc3d':ifade==='angry'?'#ff5a5a':ifade==='pixel'?'#4ade80':ifade==='xeyes'?'#fda4af':o.renk;
 c.fillStyle=ana;c.strokeStyle=ana;c.shadowColor=ana;c.shadowBlur=W*.05;c.lineCap='round';c.lineJoin='round';
 let bx=o.bakX*w*.09,by=o.bakY*h*.07,ara=w*.205,ew=h*.165,eh=h*.34;
 const dal=s=>Math.sin(t*s);
 if(ifade==='side')bx+=dal(2.4)*w*.12;if(ifade==='scan')bx+=Math.asin(dal(3.2))*w*.085;if(ifade==='suspect')bx+=dal(1.5)*w*.07;
 if(ifade==='roll'){bx+=Math.cos(t*3.4)*w*.07;by+=Math.sin(t*3.4)*h*.1}if(ifade==='think'){bx+=w*.05;by-=h*.08}
 const cy=H*.47;
 if(o.yazi){c.font=`800 ${h*.34}px ui-monospace,Menlo,monospace`;c.textBaseline='middle';const tw=c.measureText(o.yazi).width;c.fillText(o.yazi,x0+w-o.yaziSure*h*.75,cy);if(o.yaziSure*h*.75>tw+w&&o.yaziBitti)o.yaziBitti();c.restore();return}
 for(const s of[-1,1]){const i=s<0?0:1;c.save();c.translate(W/2+s*ara+bx,cy+by);
  let gw=ew,gh=eh;const kirp=o.kirp;
  const oval=(a,b)=>{yuvarla(c,-a/2,-b/2,a,b,a/2,Math.min(b/2,a*.62));c.fill()};
  const yay=(a,ters)=>{c.lineWidth=h*.085;c.beginPath();c.arc(0,ters?-a*.5:a*.5,a,ters?Math.PI*.18:Math.PI*1.18,ters?Math.PI*.82:Math.PI*1.82);c.stroke()};
  switch(ifade){
   case'happy':yay(ew*.9);break;
   case'laugh':c.translate(0,Math.abs(dal(13))*-h*.03);c.lineWidth=h*.08;c.beginPath();c.moveTo(s*ew*.6,-eh*.28);c.lineTo(-s*ew*.5,0);c.lineTo(s*ew*.6,eh*.28);c.stroke();break;
   case'heart':{const k=1+.12*dal(11);c.scale(k,k);kalpYol(c,h*.17);c.fill();break}
   case'star':c.rotate(t*3.9);yildizYol(c,h*.2);c.fill();break;
   case'wink':if(s<0)yay(ew*.85);else oval(gw,gh*Math.max(.08,1-kirp));break;
   case'wow':oval(h*.27,h*.4);c.fillStyle=o.zemin[1];c.shadowBlur=0;c.beginPath();c.arc(0,0,h*.055,0,7);c.fill();break;
   case'excited':oval(gw*1.35,gh*(1.2+.08*dal(15)));c.fillStyle='#fff';c.beginPath();c.arc(s*-gw*.2,-gh*.28,gw*.2,0,7);c.fill();break;
   case'sad':c.rotate(s*.36);oval(gw,gh*.72);if(s>0){c.rotate(-s*.36);const q=(t*.9)%1;c.globalAlpha=1-q;c.fillStyle='#9fdcff';c.beginPath();c.ellipse(0,eh*(.42+q*.8),ew*.24,ew*.36,0,0,7);c.fill()}break;
   case'angry':c.rotate(-s*.42);yuvarla(c,-ew*.72,-eh*.24,ew*1.44,eh*.52,ew*.35,eh*.2);c.fill();break;
   case'sleepy':oval(gw*1.25,eh*(.13+.05*dal(2)));if(s>0){const q=(t*.5)%1;c.globalAlpha=1-q;c.font=`800 ${h*(.1+q*.08)}px sans-serif`;c.fillText('z',ew*(.8+q),-eh*(.4+q*.7))}break;
   case'dizzy':c.rotate(s*t*7);c.lineWidth=h*.032;c.beginPath();for(let a=0;a<17;a+=.3){const r=a*h*.0108;c.lineTo(r*Math.cos(a),r*Math.sin(a))}c.stroke();break;
   case'xeyes':c.lineWidth=h*.075;c.beginPath();c.moveTo(-ew*.6,-ew*.6);c.lineTo(ew*.6,ew*.6);c.moveTo(ew*.6,-ew*.6);c.lineTo(-ew*.6,ew*.6);c.stroke();break;
   case'cool':{const g=c.createLinearGradient(0,-h*.14,0,h*.14);g.addColorStop(0,'#e0fbff');g.addColorStop(.45,'#22d3ee');g.addColorStop(1,'#1d4ed8');c.fillStyle=g;yuvarla(c,-w*.17,-h*.13,w*.34,h*.27,w*.04,h*.1);c.fill();if(s<0){c.fillRect(w*.16,-h*.1,w*.1,h*.045)}break}
   case'music':{for(let k=-1;k<=1;k++){const b=eh*(.25+.75*Math.abs(Math.sin(t*7+k*1.7+i*2.3)));yuvarla(c,k*ew*.62-ew*.2,-b/2,ew*.4,b,ew*.18,ew*.18);c.fill()}break}
   case'think':oval(gw,s<0?gh:gh*.5);if(s>0){c.fillStyle='#fde68a';c.font=`800 ${h*.2}px sans-serif`;c.globalAlpha=.6+.4*dal(5);c.fillText('?',ew*.85,-eh*.35)}break;
   case'suspect':yuvarla(c,-ew*.85,-eh*.13,ew*1.7,eh*.27,ew*.25,eh*.1);c.fill();break;
   case'pixel':{const b=(Math.floor(t*2)%2)?gh*.45:gh;c.shadowBlur=W*.02;const q=h*.052;for(let yy=-b/2;yy<b/2;yy+=q)for(let xx=-gw*.6;xx<gw*.5;xx+=q)c.fillRect(xx,yy,q*.82,q*.82);break}
   case'scan':oval(gw,gh*.55);break;
   default:{const g=c.createRadialGradient(0,-gh*.05,0,0,0,gh*.62);g.addColorStop(0,'#fff');g.addColorStop(.35,'#f4fffb');g.addColorStop(1,ana);c.fillStyle=g;oval(gw,gh*Math.max(.08,1-kirp))}
  }c.restore()}
 let ag=o.agiz;if(ag==='oto')ag=['happy','laugh','heart','excited','wink','star','music'].includes(ifade)?'gulus':ifade==='sad'?'uzgun':ifade==='wow'?'o':'yok';
 if(o.konusuyor)ag='konus';
 if(ag!=='yok'){c.save();c.translate(W/2+bx,cy+by+h*.27);c.fillStyle=ana;c.strokeStyle=ana;c.shadowColor=ana;c.lineWidth=h*.05;c.beginPath();
  if(ag==='gulus'){c.arc(0,-h*.07,h*.11,.22*Math.PI,.78*Math.PI);c.stroke()}
  else if(ag==='uzgun'){c.arc(0,h*.13,h*.11,1.25*Math.PI,1.75*Math.PI);c.stroke()}
  else{const a=ag==='o'?h*.07:h*(.015+.065*Math.abs(Math.sin(t*11)*Math.sin(t*4.3)));c.ellipse(0,0,ag==='o'?h*.055:h*.085,a,0,0,7);c.fill()}c.restore()}
 c.restore()}
