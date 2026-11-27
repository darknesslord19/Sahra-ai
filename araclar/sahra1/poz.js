/* El-kol pozları: sol ve sağ için [el yeri, parmak yönü, el biçimi]; x dışa doğru */
const D=(h,y,b='acik')=>({h,y,b});
/* m: elin gövde ortasından uzaklığı, e: göz hizasına göre yükseklik */
const O=(d,m,e)=>Object.assign(d,{m,e});
const DINLEN=()=>D([.22,-.9,.08],[.12,-1,.05]);
const IKI=f=>(t)=>[f(t,0),f(t,1)];
const POZLAR=[
['dinlen','Dinlenme',IKI(()=>DINLEN())],
['selam','Sağ el selam',t=>[DINLEN(),D([.62+.14*Math.sin(t*9),.58,.2],[.55*Math.sin(t*9),1,0])]],
['selamsol','Sol el selam',t=>[D([.62+.14*Math.sin(t*9),.58,.2],[.55*Math.sin(t*9),1,0]),DINLEN()]],
['ikiselam','İki el selam',IKI((t,i)=>D([.62+.14*Math.sin(t*9+i*3),.58,.2],[.5*Math.sin(t*9+i*3),1,0]))],
['sevinc','Eller havaya',IKI(t=>D([.45,.85+.06*Math.sin(t*10),.1],[.25,1,0]))],
['sagagoster','Sağı göster',t=>[DINLEN(),D([.98,.14+.03*Math.sin(t*5),.1],[1,.05,0],'isaret')]],
['solagoster','Solu göster',t=>[D([.98,.14+.03*Math.sin(t*5),.1],[1,.05,0],'isaret'),DINLEN()]],
['yukari','Yukarıyı göster',t=>[DINLEN(),D([.4,.86+.05*Math.sin(t*5),.2],[.05,1,0],'isaret')]],
['beni','Seni göster',t=>[DINLEN(),D([.0,.05,.9+.07*Math.sin(t*5)],[0,.1,1],'isaret')]],
['begen','Beğendim',t=>[DINLEN(),D([.4,.12+.05*Math.sin(t*6),.72],[0,1,.1],'begen')]],
['cifte','Çifte beğeni',IKI(t=>D([.42,.1+.05*Math.sin(t*6),.68],[0,1,.1],'begen'))],
['zafer','Zafer işareti',t=>[DINLEN(),D([.55,.55,.42],[.15,1,0],'zafer')]],
['alkis','Alkış',IKI(t=>O(D([0,-.08,.9],[-.5,.75,.3]),.19+.32*Math.abs(Math.sin(t*10))))],
['kucak','Kucak açma',IKI(t=>D([.86,.16+.04*Math.sin(t*3),.45],[.9,.35,.3]))],
['dusun','Düşünme',t=>[D([-.35,-.5,.45],[-1,-.1,.2],'yumruk'),D([-.38,.12+.025*Math.sin(t*4),.68],[-.35,1,0],'isaret')]],
['bel','Eller belde',IKI(()=>D([.3,-.55,.05],[-1,-.45,.1],'yumruk'))],
['bilmem','Bilmem ki',IKI(t=>D([.66,-.12+.1*Math.max(0,Math.sin(t*3)),.4],[1,.12,.3]))],
['asker','Asker selamı',t=>[DINLEN(),D([.22,.62,.55],[-1,.35,0])]],
['kulak','Seni dinliyorum',t=>[DINLEN(),D([.46,.5,.12],[-.2,1,-.15])]],
['dur','Dur',t=>[DINLEN(),D([.25,.2,.86],[0,1,.05])]],
['kalp','Kalp yapma',IKI(t=>O(D([0,.05+.04*Math.sin(t*5),.9],[-.75,.65,0]),.2))],
['sakla','Utanıp saklanma',IKI(t=>O(D([0,0,.92],[-.15,1,0]),.35,-.12+.015*Math.sin(t*3)))],
['yumruk','Güç gösterisi',IKI(t=>D([.72,.38+.07*Math.sin(t*5),.1],[-.1,1,0],'yumruk'))],
['boks','Boks',IKI((t,i)=>{const v=Math.max(0,Math.sin(t*9+i*Math.PI));return O(D([0,.06,.5+.55*v],[0,.15,1],'yumruk'),.4)})],
['davul','Davul çalma',IKI((t,i)=>O(D([0,-.3+.28*Math.sin(t*11+i*Math.PI),.85],[0,-.2,1],'yumruk'),.42))],
['yaz','Yazı yazma',IKI((t,i)=>O(D([0,-.34+.05*Math.max(0,Math.sin(t*17+i*2.4)),.85],[-.15,0,1]),.3))],
['dans','Disko',t=>{const a=Math.sin(t*5.5)>0;const u=D([.7,.74,.2],[.5,.85,0],'isaret'),i=D([-.2,-.55,.55],[-.6,-.7,.2],'isaret');return a?[i,u]:[u,i]}],
['dalga','Dalga dansı',IKI((t,i)=>D([.88,.34*Math.sin(t*5+i*Math.PI),.18],[1,.7*Math.cos(t*5+i*Math.PI),0]))],
['kos','Koşu kolları',IKI((t,i)=>D([.2,-.48,.62*Math.sin(t*9+i*Math.PI)],[0,-.2,1],'yumruk'))],
['kanat','Kanat çırpma',IKI(t=>D([.86,.55*Math.sin(t*8),.05],[1,.5*Math.sin(t*8-.8),0]))]
];
/* gövde hareketleri: o = konum ve açı düzeltmeleri */
const HAREKETLER=[
['suzul','Süzülme',(t,o)=>{o.y+=.06*Math.sin(t*1.9);o.z_+=.025*Math.sin(t*1.9+1)}],
['sabit','Sabit',()=>{}],
['gezinti','Sağa-sola yürüyüş',(t,o)=>{const f=t*1.15,v=Math.cos(f);o.x+=.62*Math.sin(f);const k=Math.max(-1,Math.min(1,v*3.2));o.yaw+=k*1.25;o.adim=Math.abs(k);o.y+=.04*Math.abs(Math.sin(t*8))*Math.abs(k);o.teker+=Math.sin(f)*4.5}],
['bakin','Sağa-sola bakınma',(t,o)=>{const v=Math.sin(t*1.5);o.basYaw+=.85*Math.tanh(v*3);o.yaw+=.2*Math.tanh(Math.sin(t*1.5-.7)*3);o.bakX+=Math.tanh(v*3)*.7}],
['evet','Evet (baş sallama)',(t,o)=>{o.basEg+=.24*Math.sin(t*8)}],
['hayir','Hayır',(t,o)=>{o.basYaw+=.48*Math.sin(t*8)}],
['merak','Merakla başını eğme',(t,o)=>{o.basYat+=.26*Math.tanh(Math.sin(t*1.6)*3)}],
['reverans','Reverans',(t,o)=>{const v=Math.pow(Math.max(0,Math.sin(t*2.1)),2);o.eg+=.5*v;o.y-=.12*v}],
['zipla','Zıplama',(t,o)=>{const v=Math.abs(Math.sin(t*4.4));o.y+=.5*v-.08;o.ez+=.12*Math.pow(1-v,6);o.adim=.2}],
['dansg','Dans',(t,o)=>{o.z_+=.2*Math.sin(t*5.5);o.x+=.2*Math.sin(t*2.75);o.y+=.1*Math.abs(Math.sin(t*5.5));o.yaw+=.4*Math.sin(t*2.75);o.basYat+=-.16*Math.sin(t*5.5)}],
['salin','Salınma',(t,o)=>{o.z_+=.17*Math.sin(t*2.6);o.x+=.14*Math.sin(t*2.6)}],
['topac','Topaç dönüşü',(t,o)=>{const f=(t%2.8)/2.8,k=Math.min(1,f/.55);o.yaw+=k*k*(3-2*k)*Math.PI*2;o.y+=.18*Math.sin(Math.PI*k)}],
['takla','Takla',(t,o)=>{const f=(t%3)/3,k=Math.min(1,f/.4),s=k*k*(3-2*k);o.takla+=s*Math.PI*2;o.y+=.55*Math.sin(Math.PI*k)}],
['yuvarlan','Yana yuvarlanma',(t,o)=>{const f=(t%3)/3,k=Math.min(1,f/.45),s=k*k*(3-2*k);o.yuv-=s*Math.PI*2;o.x+=.45*Math.sin(Math.PI*k)}],
['ucus','Uçuş turu',(t,o)=>{o.x+=.58*Math.sin(t*1.3);o.y+=.3*Math.sin(t*2.6)+.1;o.z_-=.32*Math.cos(t*1.3);o.yaw+=.75*Math.cos(t*1.3);o.eg+=.12}],
['titre','Titreme',(t,o)=>{o.x+=.04*Math.sin(t*55);o.z_+=.04*Math.sin(t*47)}],
['uyukla','Uyuklama',(t,o)=>{const v=Math.pow((1-Math.cos(t*1.5))/2,2);o.basEg+=.36*v;o.basYat+=.14*v;o.y-=.08*v}]
];
const KOMBOLAR=[
['Merhaba Güven hocam','selam','happy','suzul',0],['Emriniz başım üstüne','asker','normal','reverans',0],['Seni dinliyorum','kulak','wow','merak',-25],['Düşünüyorum','dusun','think','merak',25],
['Görev tamam','cifte','star','zipla',0],['Tebrikler','alkis','laugh','salin',0],['Seni seviyorum','kalp','heart','salin',0],['Parti zamanı','dans','music','dansg',0],
['Disko turu','dalga','cool','topac',0],['Bilmiyorum','bilmem','suspect','merak',0],['Olmaz','dur','angry','hayir',0],['Tabii ki','begen','wink','evet',0],
['Şuraya bak','sagagoster','excited','bakin',20],['Utandım','sakla','happy','salin',0],['İyi geceler','dinlen','sleepy','uyukla',-20],['Üzgünüm','dinlen','sad','uyukla',0],
['Uçuşa geçtim','kanat','excited','ucus',0],['Yürüyüşteyim','kos','normal','gezinti',0],['Mesaj yazıyorum','yaz','scan','suzul',20],['Başım döndü','kanat','dizzy','yuvarlan',0]
];
