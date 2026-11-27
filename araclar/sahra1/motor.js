/* Sahra 3B motoru: dış kütüphane yok */
const V={ek:(a,b)=>[a[0]+b[0],a[1]+b[1],a[2]+b[2]],cik:(a,b)=>[a[0]-b[0],a[1]-b[1],a[2]-b[2]],carp:(a,s)=>[a[0]*s,a[1]*s,a[2]*s],
 nokta:(a,b)=>a[0]*b[0]+a[1]*b[1]+a[2]*b[2],vek:(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],
 boy:a=>Math.hypot(a[0],a[1],a[2]),bir:a=>{const l=Math.hypot(a[0],a[1],a[2])||1;return[a[0]/l,a[1]/l,a[2]/l]},kar:(a,b,t)=>a.map((v,i)=>v+(b[i]-v)*t)};
function mc(a,b){const o=new Float32Array(16);for(let c=0;c<4;c++)for(let r=0;r<4;r++)o[c*4+r]=a[r]*b[c*4]+a[4+r]*b[c*4+1]+a[8+r]*b[c*4+2]+a[12+r]*b[c*4+3];return o}
function trs(t,r,s){const[cx,cy,cz]=r.map(Math.cos),[sx,sy,sz]=r.map(Math.sin);
 /* T * Ry * Rx * Rz * S */
 const m00=cy*cz+sy*sx*sz,m01=-cy*sz+sy*sx*cz,m02=sy*cx,m10=cx*sz,m11=cx*cz,m12=-sx,m20=-sy*cz+cy*sx*sz,m21=sy*sz+cy*sx*cz,m22=cy*cx;
 return new Float32Array([m00*s[0],m10*s[0],m20*s[0],0,m01*s[1],m11*s[1],m21*s[1],0,m02*s[2],m12*s[2],m22*s[2],0,t[0],t[1],t[2],1])}
/* +y eksenini verilen yöne çeviren matris */
function yonel(p,yon,s,ipucu){const y=V.bir(yon);let h=ipucu||[0,0,1];if(Math.abs(V.nokta(y,h))>.96)h=[1,0,0];const x=V.bir(V.vek(y,h)),z=V.vek(x,y);
 return new Float32Array([x[0]*s[0],x[1]*s[0],x[2]*s[0],0,y[0]*s[1],y[1]*s[1],y[2]*s[1],0,z[0]*s[2],z[1]*s[2],z[2]*s[2],0,p[0],p[1],p[2],1])}
function izgara(nu,nv,f){const P=[],N=[],T=[],I=[];for(let j=0;j<=nv;j++)for(let i=0;i<=nu;i++){const o=f(i/nu,j/nv);P.push(...o.p);N.push(...V.bir(o.n));T.push(i/nu,j/nv)}
 for(let j=0;j<nv;j++)for(let i=0;i<nu;i++){const a=j*(nu+1)+i,b=a+nu+1;I.push(a,a+1,b,a+1,b+1,b)}return{P,N,T,I}}
const us=(v,e)=>Math.sign(v)*Math.pow(Math.abs(v),e);
/* süper elipsoit: e=1 küre, küçüldükçe kutulaşır */
function sekil(a,b,c,e1=1,e2=1,nu=56,nv=36){return izgara(nu,nv,(u,v)=>{const t=(u-.5)*2*Math.PI,f=(v-.5)*Math.PI,cf=Math.cos(f),sf=Math.sin(f),ct=Math.cos(t),st=Math.sin(t);
 return{p:[a*us(cf,e1)*us(st,e2),b*us(sf,e1),c*us(cf,e1)*us(ct,e2)],n:Math.abs(cf)<1e-6?[0,sf,0]:[us(cf,2-e1)*us(st,2-e2)/a,us(sf,2-e1)/b,us(cf,2-e1)*us(ct,2-e2)/c]}})}
/* dönel yüzey: profil [yarıçap,y] listesi (alttan üste) */
function donel(pr,nu=48){const n=pr.length-1;return izgara(nu,n,(u,v)=>{const j=Math.round(v*n),t=u*2*Math.PI,[r,y]=pr[j],a=pr[Math.max(0,j-1)],b=pr[Math.min(n,j+1)],dr=b[0]-a[0],dy=b[1]-a[1];
 return{p:[r*Math.sin(t),y,r*Math.cos(t)],n:[dy*Math.sin(t),-dr,dy*Math.cos(t)]}})}
function halka(R,r,nu=64,nv=16,yay=2*Math.PI){return izgara(nu,nv,(u,v)=>{const t=u*yay,f=v*2*Math.PI,k=R+r*Math.cos(f);return{p:[k*Math.cos(t),k*Math.sin(t),r*Math.sin(f)],n:[Math.cos(f)*Math.cos(t),Math.cos(f)*Math.sin(t),Math.sin(f)]}})}
/* y boyunca 0..1 uzanan çubuk */
const cubukGeo=()=>donel([[0,0],[1,0],[1,1],[0,1]],20);
/* düz yüzlü kristal */
function kristal(m){const P=[],N=[],T=[],I=[];const g=m;for(let i=0;i<g.I.length;i+=3){const k=[0,1,2].map(j=>g.P.slice(g.I[i+j]*3,g.I[i+j]*3+3));
 let n=V.vek(V.cik(k[1],k[0]),V.cik(k[2],k[0]));if(V.boy(n)<1e-6)continue;if(V.nokta(n,k[0])<0)n=V.carp(n,-1);n=V.bir(n);
 for(const p of k){P.push(...p);N.push(...n);T.push(0,0);I.push(I.length)}}return{P,N,T,I}}
/* ekran yaması: ön yüzeye oturan dokulu parça */
function yama(w,h,y0,z,pay=.012){return izgara(40,28,(u,v)=>{const x=(u-.5)*w,y=(v-.5)*h+y0,e=.004,zz=z(x,y),dx=(z(x+e,y)-z(x-e,y))/(2*e),dy=(z(x,y+e)-z(x,y-e))/(2*e);
 const n=V.bir([-dx,-dy,1]);return{p:[x+n[0]*pay,y+n[1]*pay,zz+n[2]*pay],n}})}
const elipsZ=(a,b,c,e=2,taban=.12)=>(x,y)=>c*Math.pow(Math.max(taban,1-Math.pow(Math.abs(x/a),e)-Math.pow(Math.abs(y/b),e)),1/e);

class Motor{
 constructor(cv){const gl=this.gl=cv.getContext('webgl',{alpha:true,antialias:true,premultipliedAlpha:true,preserveDrawingBuffer:true});if(!gl)throw Error('WebGL yok');this.cv=cv;this.ag=new WeakMap();
  const vs='attribute vec3 p,n;attribute vec2 t;uniform mat4 uM,uP;uniform mat3 uN;varying vec3 vn,vp;varying vec2 vt;void main(){vec4 w=uM*vec4(p,1.);vp=w.xyz;vn=uN*n;vt=t;gl_Position=uP*w;}';
  const fs=`precision mediump float;varying vec3 vn,vp;varying vec2 vt;uniform vec3 uC,uKam,uIsik;uniform float uSpec,uShine,uEmis,uTex,uGlow,uA;uniform sampler2D uS;
  void main(){vec3 n=normalize(vn);vec3 v=normalize(uKam-vp);if(dot(n,v)<0.&&uTex<.5)n=-n;
  vec3 l1=normalize(vec3(-.5,.72,.62)),l2=normalize(vec3(.8,.1,.35));
  float s1=pow(max(dot(n,normalize(l1+v)),0.),uShine),s2=pow(max(dot(n,normalize(l2+v)),0.),uShine*1.6);
  float fr=pow(1.-max(dot(n,v),0.),2.4);
  if(uTex>.5){vec4 k=texture2D(uS,vt);if(k.a<.02)discard;float g=(s1*.22+s2*.1)*k.a;gl_FragColor=vec4(k.rgb*k.a+g,k.a);return;}
  float d=clamp((dot(n,l1)+.45)/1.45,0.,1.);float d2=max(dot(n,l2),0.);
  vec3 ortam=mix(vec3(.42,.40,.46),vec3(.66,.69,.76),n.y*.5+.5);
  vec3 c=uC*(ortam*.78+d*.52+d2*.12);c+=vec3(s1+s2*.5)*uSpec;c+=fr*vec3(.20,.24,.32)*uSpec;c+=uIsik*fr*uGlow;
  c=mix(c,uC*1.15,uEmis);gl_FragColor=vec4(min(c,1.)*uA,uA);}`;
  const sh=(t,s)=>{const o=gl.createShader(t);gl.shaderSource(o,s);gl.compileShader(o);if(!gl.getShaderParameter(o,gl.COMPILE_STATUS))throw Error(gl.getShaderInfoLog(o));return o};
  const pr=this.pr=gl.createProgram();gl.attachShader(pr,sh(gl.VERTEX_SHADER,vs));gl.attachShader(pr,sh(gl.FRAGMENT_SHADER,fs));gl.linkProgram(pr);gl.useProgram(pr);
  this.a={};['p','n','t'].forEach(k=>this.a[k]=gl.getAttribLocation(pr,k));this.u={};['uM','uP','uN','uC','uKam','uIsik','uSpec','uShine','uEmis','uTex','uGlow','uA','uS'].forEach(k=>this.u[k]=gl.getUniformLocation(pr,k));
  gl.enable(gl.DEPTH_TEST);gl.enable(gl.BLEND);gl.blendFunc(gl.ONE,gl.ONE_MINUS_SRC_ALPHA)}
 tampon(g){let o=this.ag.get(g);if(o)return o;const gl=this.gl,b=(d,h=gl.ARRAY_BUFFER,T=Float32Array)=>{const x=gl.createBuffer();gl.bindBuffer(h,x);gl.bufferData(h,new T(d),gl.STATIC_DRAW);return x};
  o={p:b(g.P),n:b(g.N),t:b(g.T),i:b(g.I,gl.ELEMENT_ARRAY_BUFFER,Uint16Array),s:g.I.length};this.ag.set(g,o);return o}
 doku(){const gl=this.gl,t=gl.createTexture();gl.bindTexture(gl.TEXTURE_2D,t);for(const[k,v]of[[gl.TEXTURE_WRAP_S,gl.CLAMP_TO_EDGE],[gl.TEXTURE_WRAP_T,gl.CLAMP_TO_EDGE],[gl.TEXTURE_MIN_FILTER,gl.LINEAR],[gl.TEXTURE_MAG_FILTER,gl.LINEAR]])gl.texParameteri(gl.TEXTURE_2D,k,v);return t}
 yukle(t,cv){const gl=this.gl;gl.bindTexture(gl.TEXTURE_2D,t);gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL,true);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,gl.RGBA,gl.UNSIGNED_BYTE,cv)}
 ciz(kok,isik,uzak){const gl=this.gl,w=this.cv.width,h=this.cv.height;gl.viewport(0,0,w,h);gl.clearColor(0,0,0,0);gl.depthMask(true);gl.clear(gl.COLOR_BUFFER_BIT|gl.DEPTH_BUFFER_BIT);
  const f=1/Math.tan(13*Math.PI/180),yakin=.5,en=60,oran=w/h,fx=oran>=1?f/oran:f,fy=oran>=1?f:f*oran;
  const P=new Float32Array([fx,0,0,0,0,fy,0,0,0,0,(en+yakin)/(yakin-en),-1,0,0,2*en*yakin/(yakin-en),0]);
  const kam=[0,.35,uzak],e=Math.atan2(.35,uzak),Vm=mc(trs([0,0,-Math.hypot(.35,uzak)],[0,0,0],[1,1,1]),trs([0,0,0],[e,0,0],[1,1,1]));
  gl.uniformMatrix4fv(this.u.uP,false,mc(P,Vm));gl.uniform3fv(this.u.uKam,kam);gl.uniform3fv(this.u.uIsik,isik);
  const saydam=[];const gez=(d,M)=>{if(d.gizli)return;const m=mc(M,d.m||trs(d.t,d.r,d.s));if(d.geo){const x=[d,m];(d.mat.tex||d.mat.a<1)?saydam.push(x):this.parca(...x)}for(const k of d.alt)gez(k,m)};
  gez(kok,trs([0,0,0],[0,0,0],[1,1,1]));gl.depthMask(false);for(const x of saydam)this.parca(...x);gl.depthMask(true)}
 parca(d,m){const gl=this.gl,o=this.tampon(d.geo),u=this.u,k=d.mat;for(const[ad,n]of[['p',3],['n',3],['t',2]]){gl.bindBuffer(gl.ARRAY_BUFFER,o[ad]);gl.enableVertexAttribArray(this.a[ad]);gl.vertexAttribPointer(this.a[ad],n,gl.FLOAT,false,0,0)}
  const a=m[0],b=m[1],c=m[2],e=m[4],f=m[5],g=m[6],h=m[8],i=m[9],j=m[10];/* ters devrik */
  gl.uniformMatrix3fv(u.uN,false,[f*j-g*i,g*h-e*j,e*i-f*h,c*i-b*j,a*j-c*h,b*h-a*i,b*g-c*f,c*e-a*g,a*f-b*e]);
  gl.uniformMatrix4fv(u.uM,false,m);gl.uniform3fv(u.uC,k.c);gl.uniform1f(u.uSpec,k.spec);gl.uniform1f(u.uShine,k.shine);gl.uniform1f(u.uEmis,k.emis);gl.uniform1f(u.uGlow,k.glow);gl.uniform1f(u.uA,k.a);
  gl.uniform1f(u.uTex,k.tex?1:0);if(k.tex){gl.activeTexture(gl.TEXTURE0);gl.bindTexture(gl.TEXTURE_2D,k.tex);gl.uniform1i(u.uS,0)}
  gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER,o.i);gl.drawElements(gl.TRIANGLES,o.s,gl.UNSIGNED_SHORT,0)}
}
const renk=h=>[1,3,5].map(i=>parseInt(h.slice(i,i+2),16)/255);
const mal=(h,o={})=>Object.assign({c:renk(h),spec:.42,shine:48,emis:0,glow:0,a:1},o);
function dugum(geo,mat,t=[0,0,0],r=[0,0,0],s=[1,1,1]){return{geo,mat,t:t.slice(),r:r.slice(),s:s.slice(),alt:[],m:null,gizli:false,ekle(...k){this.alt.push(...k);return k[0]}}}
const grup=(t,r,s)=>dugum(null,null,t,r,s);
