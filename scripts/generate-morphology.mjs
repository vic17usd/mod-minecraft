// Arte procedural original y determinista: atlas articulados, cubemap y mapas esféricos HD.
import fs from 'node:fs';
export function generateMorphology({png,json,read,id,bodies,assets}) {
  const profiles=read('living_worlds.json'),clamp=v=>Math.max(0,Math.min(255,Math.round(v)));
  const hash=(x,y,s)=>{let n=Math.imul(x,374761393)^Math.imul(y,668265263)^s;n=Math.imul(n^n>>>13,1274126177);return((n^n>>>16)>>>0)/4294967295;};
  const smooth=(x,y,s)=>{let a=Math.floor(x),b=Math.floor(y),u=x-a,v=y-b;u=u*u*(3-2*u);v=v*v*(3-2*v);return (hash(a,b,s)*(1-u)+hash(a+1,b,s)*u)*(1-v)+(hash(a,b+1,s)*(1-u)+hash(a+1,b+1,s)*u)*v;};
  const noise=(x,y,s)=>smooth(x,y,s)*.6+smooth(x*2,y*2,s+1)*.27+smooth(x*4,y*4,s+2)*.13;
  const rgb=n=>[n>>16&255,n>>8&255,n&255];
  const entities=[];
  for(const p of profiles){for(const suffix of ['crystal_bovine','lumen_sheep','plated_boar','colonist','stalker',...(p.body==='earth'?[]:['raider']),...(bodies.find(b=>b.id===p.body).mob?['crawler']:[])])entities.push({id:`${p.body}_${suffix}`,p,suffix});}
  entities.push({id:'lunar_guardian',p:profiles.find(p=>p.body==='moon'),suffix:'machine'},{id:'martian_queen',p:profiles.find(p=>p.body==='mars'),suffix:'queen'});
  for(const[e,entry]of entities.entries()) {
    const {id:name,p,suffix}=entry,base=rgb(p.color),seed=p.seed+e*337;
    png(`entity/${name}`,256,256,(x,y)=>{
      let tile=Math.floor(x/128)+Math.floor(y/64)*2,tx=x%128,ty=y%64;
      let color=base.map(c=>c*.65+25),shade=(noise(x/6,y/6,seed)-.5)*22,alpha=255;
      const vein=Math.sin(tx*.11+Math.sin(ty*.23)*2+seed)*Math.cos(ty*.16+tx*.03);
      if(tile===0){shade+=Math.sin(ty*.35)*4-Math.max(0,Math.abs(vein)-.83)*180;}
      if(tile===1){color=suffix==='machine'?[78,224,255]:[92+(e%5)*19,221,165+(e%3)*27];shade+=12*Math.sin(tx*.17);}
      if(tile===2){shade+=((tx+Math.floor(ty/9)*5)%17<2?-35:12)+Math.sin(tx*.15)*7;}
      if(tile===3){color=color.map(c=>c*.45);shade+=tx%12<2?-10:4;}
      if(tile>=4){color=[base[0]*.35+70,base[1]*.35+80,base[2]*.4+95];shade+=(tx%19<2||ty%13<1)?-35:4;alpha=suffix==='lumen_sheep'?150:185;}
      if(suffix==='machine'){shade+=tx%16<2||ty%16<2?-32:10;}
      return [...color.map(c=>clamp(c+shade)),alpha];
    });
    png(`entity/${name}_glow`,256,256,(x,y)=>x>=128&&y<64?[clamp(100+e%7*15),235,220,220]:[0,0,0,0]);
  }
  // Parámetros medios de referencia. Las fases de satélites menores son aproximadas, no medidas.
  const rows=[
    ['sun','',696340,0,0,1,0,0,0,0,25.05,7.25],
    ['mercury','sun',2439.7,57909050,.20563,87.969,7.005,48.331,29.124,174.796,58.646,.034],
    ['venus','sun',6051.8,108208000,.00677,224.701,3.395,76.68,54.884,50.115,-243.025,177.36],
    ['earth','sun',6371,149598023,.01671,365.25636,0,0,102.947,357.517,.99726968,23.4393],
    ['mars','sun',3389.5,227939366,.0934,686.98,1.85,49.558,286.50,19.37,1.02595675,25.19],
    ['jupiter','sun',69911,778570000,.0489,4332.589,1.303,100.464,273.867,20.020,.41354,3.13],
    ['saturn','sun',58232,1433529000,.0565,10759.22,2.485,113.665,339.392,317.020,.44401,26.73],
    ['uranus','sun',25362,2872463000,.0463,30685.4,.773,74.006,96.999,142.238,-.71833,97.77],
    ['neptune','sun',24622,4495060000,.00946,60189,1.77,131.784,273.187,256.228,.67125,28.32],
    ['moon','earth',1737.4,384400,.0549,27.321661,5.145,125.08,318.15,135.27,27.321661,6.68],
  ];
  const satellites=[['phobos','mars',11.266,9376,.0151,.31891023,1.093],['deimos','mars',6.2,23463,.00033,1.26244,.93],['io','jupiter',1821.6,421700,.0041,1.769137786,.05,106.07719],['europa','jupiter',1560.8,671034,.009,3.551181,.47,175.73161],['ganymede','jupiter',2634.1,1070412,.0013,7.154553,.20,120.55883],['callisto','jupiter',2410.3,1882709,.0074,16.6890184,.281,84.44459],['titan','saturn',2574.7,1221870,.0288,15.945421,.34854],['enceladus','saturn',252.1,237948,.0047,1.370218,.009],['mimas','saturn',198.2,185539,.0196,.9424218,1.574],['tethys','saturn',531.1,294619,.0001,1.887802,1.12],['dione','saturn',561.4,377396,.0022,2.736915,.028],['rhea','saturn',763.8,527108,.00125,4.518212,.345],['iapetus','saturn',734.5,3560820,.0283,79.3215,15.47],['miranda','uranus',235.8,129390,.0013,1.413479,4.34],['ariel','uranus',578.9,190900,.0012,2.520379,.041],['umbriel','uranus',584.7,266000,.0039,4.144177,.128],['titania','uranus',788.9,435910,.0011,8.705872,.079],['oberon','uranus',761.4,583520,.0014,13.463239,.068],['triton','neptune',1353.4,354759,.000016,5.876854,156.865],['proteus','neptune',210,117647,.0005,1.122315,.075]];
  satellites.forEach((r,i)=>rows.push([...r.slice(0,7),0,0,r[7]??i*137.508%360,r[5],0]));
  json('celestial_orbits.json',rows.map(([id,parent,radius,axis,eccentricity,period,inclination,node,periapsis,anomaly,rotation,tilt])=>({id,parent,radius,axis,eccentricity,period,inclination,node,periapsis,anomaly,rotation,tilt,equatorial:parent!=='sun'&&parent!==''&&id!=='moon'})));
  // Mapas equirectangulares; el sombreado de fases se calcula al dibujar la esfera, no se pinta aquí.
  // Costas de dominio público Natural Earth: rasterización por líneas, no millones de búsquedas.
  const continents=JSON.parse(fs.readFileSync(new URL('./assets/earth-coastlines.json',import.meta.url),'utf8'));
  const earthMask=new Uint8Array(2048*1024);
  for(const polygon of continents)for(let y=0;y<1024;y++){
    const latitude=90-(y+.5)/1024*180,crossings=[];
    for(let i=0,j=polygon.length-1;i<polygon.length;j=i++){
      const a=polygon[i],b=polygon[j];if((a[1]>latitude)!==(b[1]>latitude))crossings.push((a[0]+(b[0]-a[0])*(latitude-a[1])/(b[1]-a[1])+180)/360*2048);
    }
    crossings.sort((a,b)=>a-b);
    for(let i=0;i+1<crossings.length;i+=2)for(let x=Math.max(0,Math.ceil(crossings[i]-.5));x<Math.min(2048,crossings[i+1]-.5);x++)earthMask[y*2048+x]=1;
  }
  for(const p of profiles){const name=p.body,width=name==='earth'?2048:1024,height=width/2,base=rgb(p.color);
    png(`astronomy/${name}`,width,height,(x,y)=>{
      const u=x/width,v=y/height,n=noise(u*38,v*19,p.seed),fine=noise(u*180,v*90,p.seed+41),lat=90-v*180,lon=u*360-180;
      let color=base.map(c=>c*.65+35),shade=(n-.5)*65+(fine-.5)*14;
      if(name==='earth'){
        const land=earthMask[y*2048+x]===1,ice=Math.max(0,Math.min(1,(Math.abs(lat)-62+n*7)/12));
        color=land?[48+n*54,76+n*65,35+n*42]:[7+n*8,27+n*20,62+n*30];
        const desert=land?Math.max(Math.exp(-(((lon-20)/43)**4+((lat-23)/9)**4)),Math.exp(-(((lon-85)/22)**2+((lat-40)/6)**2)),Math.exp(-(((lon-133)/13)**4+((lat+25)/8)**4)))*(.8+n*.2):0;
        color=color.map((c,k)=>c*(1-desert)+desert*([148+n*49,120+n*44,72+n*25][k]));
        if(land)color=color.map(c=>c*(1-ice)+ice*(218+fine*15));
        const cloud=Math.max(0,noise(u*32+Math.sin(v*20),v*40,377)-.57)*2.4;return [...color.map(c=>clamp(c+shade*.17+(240-c)*cloud)),255];
      }
      if(['jupiter','saturn','uranus','neptune'].includes(name)){
        const bands=Math.sin(v*90+Math.sin(u*32)*.3+n*2),storm=Math.exp(-((u-.67)**2/.002+(v-.61)**2/.0006));
        shade=bands*(name==='uranus'?8:22)+(fine-.5)*7;
        if(name==='jupiter')color=[191+storm*30,155-storm*35,118-storm*24];
        if(name==='neptune')color=[33,71,153];
      } else {
        const cellX=Math.floor(u*45),cellY=Math.floor(v*24),cx=cellX+hash(cellX,cellY,p.seed),cy=cellY+hash(cellX,cellY,p.seed+3),r=Math.hypot(u*45-cx,v*24-cy),rim=hash(cellX,cellY,p.seed+5)*.2+.12;
        shade+=(Math.exp(-(((r-rim)/.045)**2))*22-Math.exp(-((r/(rim*.8))**4))*17);
        if(name==='mars'){color=[152,76,43];shade+=Math.abs(lat)>77?90:0;}
        if(name==='venus'){color=[192,153,76];shade=Math.sin(u*42+v*18+n*5)*12+n*10;}
        if(name==='europa') {color=[186,183,156];if(Math.abs(Math.sin(u*54+Math.sin(v*21)*2))<.055)shade-=85;}
        if(name==='io'){color=[197,165,66];if(r<.055)color=[244,80,17];}
        if(name==='iapetus'&&u<.5)shade-=65;
      }
      return [...color.map(c=>clamp(c+shade)),255];
    });json(`${assets}/textures/astronomy/${name}.png.mcmeta`,{texture:{blur:true,clamp:false}});
  }
  png('astronomy/earth_night',2048,1024,(x,y)=>{const lat=90-y/1024*180,land=earthMask[y*2048+x]===1;const city=land&&Math.abs(lat)<58&&hash(Math.floor(x/2),Math.floor(y/2),501)>.985;return city?[245,188,83,200]:[0,0,0,0];});
  png('astronomy/sun',1024,512,(x,y)=>{const n=noise(x/8,y/8,17),gran=hash(x,y,89);return[255,clamp(183+n*53),clamp(72+n*70+gran*12),255];});
  png('astronomy/rings',1024,32,(x,y)=>{const r=x/1024,opacity=r>.45&&r<.49?0:Math.sin(r*530)*.13+.5;return[191,176,139,clamp(opacity*200*(r>.02&&r<.98?1:0))];});
  // Muestreo direccional compartido entre las seis caras: la nebulosa cruza sus bordes sin costura.
  const faces=[(s,t)=>[-1,t,s],(s,t)=>[1,t,-s],(s,t)=>[s,-1,t],(s,t)=>[s,1,-t],(s,t)=>[-s,t,-1],(s,t)=>[s,t,1]];
  for(let face=0;face<6;face++)png(`astronomy/deep_space_${face}`,1024,1024,(x,y)=>{
    let [dx,dy,dz]=faces[face]((x+.5)/512-1,1-(y+.5)/512),length=Math.hypot(dx,dy,dz);dx/=length;dy/=length;dz/=length;
    const longitude=Math.atan2(dz,dx),latitude=Math.asin(dy),band=Math.exp(-(((dy*.8+dx*.48+dz*.35)/.14)**2)),n=noise((longitude+Math.PI)*9,(latitude+Math.PI/2)*13,717);
    const nebula=band*(.3+n*.7),dust=Math.max(0,.6-n)*band*24;
    const sx=Math.floor((longitude+Math.PI)*1800),sy=Math.floor((latitude+Math.PI/2)*1800),star=hash(sx,sy,979)> .9991-(band*.0022),intensity=star?55+hash(sx,sy,191)*195:0;
    return[clamp(2+nebula*31-dust+intensity),clamp(3+nebula*23-dust+intensity*.92),clamp(8+nebula*48-dust+intensity*.86),255];
  });
  for(let face=0;face<6;face++)json(`${assets}/textures/astronomy/deep_space_${face}.png.mcmeta`,{texture:{blur:true,clamp:true}});
  json(`${assets}/textures/astronomy/earth_night.png.mcmeta`,{texture:{blur:true,clamp:false}});
  png('particle/bio_spore',32,32,(x,y)=>{const r=Math.hypot(x-15.5,y-15.5)/16;return r<1?[146,255,218,clamp((1-r)**2*255)]:[0,0,0,0];});
  json(`${assets}/particles/bio_spore.json`,{textures:[id('bio_spore')]});
  for(const [language,index]of [['es_es',0],['en_us',1]]){
    const text=read(`${assets}/lang/${language}.json`);
    text['colony.galaxycraft.blueprint']=['Plano tecnológico','Technology plan'][index];text['colony.galaxycraft.blueprint_tip']=['Cuatro fichas → plano del observatorio local. Consume una existencia.','Four tokens → local observatory blueprint. Uses one stock.'][index];
    const labels={crystal_bovine:['Hexápodo cristalino','Crystal Hexapod'],lumen_sheep:['Biomasa luminiscente','Luminous Biomass'],plated_boar:['Caminante de placas','Plated Walker'],colonist:['Sabio trípode','Tripod Sage'],raider:['Raptor de membrana','Membrane Raptor'],crawler:['Gusano de regolito','Regolith Worm']};
    for(const b of bodies)for(const [suffix,label]of Object.entries(labels))if(text[`entity.galaxycraft.${b.id}_${suffix}`])text[`entity.galaxycraft.${b.id}_${suffix}`]=`${label[index]} · ${index===0?b.name:b.id}`;
    json(`${assets}/lang/${language}.json`,text);
  }
  json('morphology_manifest.json',{version:1,atlas:256,families:11,entities:entities.map(e=>e.id),skyProjection:'equirectangular',deepSpaceFaces:6,epochJulianDay:2451545,phaseAccuracy:'mean_keplerian_approximation_not_ephemeris'});
  console.log(`Morfología: ${entities.length} modelos con atlas originales, 30 mapas HD y cubemap de seis caras.`);
}
