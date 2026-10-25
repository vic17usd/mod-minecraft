// Arquitectura modular original: NBT propios, diez diseños exclusivos y un único set por mundo.
import zlib from 'node:zlib';
import {createHash} from 'node:crypto';
import {themes} from './expedition-themes.mjs';

export function generateExpeditions({png,json,read,binary,id,int,str,list,compound,intList,payload,bodies,assets,data}){
  const roles=['city','fortress','temple','outpost','observatory','mine','factory','catacombs','archive','megadungeon'];
  const legacyKinds=['observatory','wreck','sanctuary','geode','vault'];
  const results=['reinforced_panel','energy_cell','rover_battery','station_kit','oxygen_tank','thruster','navigation_core','pressure_detector','lead_shield_module','advanced_navigation_core'];
  const hash=(x,z,s)=>{let n=Math.imul(x,374761393)^Math.imul(z,668265263)^s;n=Math.imul(n^n>>>13,1274126177);return((n^n>>>16)>>>0)/4294967295;};
  const slug=text=>text.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,'_').replace(/^_|_$/g,'');
  const vectors=[[0,-1],[1,0],[0,1],[-1,0]];
  const profiles=read('living_worlds.json'),manifest=[],blueprints=[],geometries=new Set(),moduleCache=new Map();let templateCount=0,totalBlocks=0;
  // Las salidas antiguas se conservan para leer mundos y cofres existentes, pero nunca se eligen.
  for(const body of bodies)for(const name of [...legacyKinds.map(k=>`${body.id}_${k}`),`colony_${body.id}`]){
    const file=`${data}/worldgen/structure_set/${name}.json`,set=read(file);set.placement.frequency=0;json(file,set);
  }
  function layout(index,slot,level,attempt=0){
    const mega=slot===9,start=mega?[-3,0]:[0,0],nodes=[start],set=new Set([start.join(',')]);
    const target=level===0?[17,22,15,11,13,23,20,25,16,27][slot]+index%5:level===1?13+index%5:9+index%3;
    const seed=311717+index*7919+slot*701+level*103+attempt*19937;
    while(nodes.length<target){
      const frontier=[];
      for(const [x,z]of nodes)for(let direction=0;direction<4;direction++){
        const[dx,dz]=vectors[direction],nx=x+dx,nz=z+dz,key=`${nx},${nz}`;
        if(Math.abs(nx)>5||Math.abs(nz)>5||set.has(key)||mega&&Math.abs(nx)<=1&&Math.abs(nz)<=1)continue;
        // Sesgos geométricos distintos: avenidas, galerías alargadas, coronas o terrazas.
        const longAxis=(index+slot)%2===0?Math.abs(nx):Math.abs(nz),cross=(index+slot)%2===0?Math.abs(nz):Math.abs(nx);
        let score=hash(nx,nz,seed+nodes.length*37)*2;
        if(slot===5)score+=longAxis*.24-cross*.2;
        else if(slot===0||slot===6)score-=cross*.11+longAxis*.04;
        else if(slot===1||slot===7)score+=Math.sin((nx*nx+nz*nz)*.6+index)*.45;
        else score+=Math.sin(nx*(index%4+1)+nz*(slot%3+1))*.25;
        frontier.push({x:nx,z:nz,score});
      }
      if(!frontier.length)throw Error('Plano sin frontera');frontier.sort((a,b)=>b.score-a.score||a.x-b.x||a.z-b.z);
      const next=frontier[0];nodes.push([next.x,next.z]);set.add(`${next.x},${next.z}`);
    }
    const plan=nodes.map(([x,z],i)=>({x,z,y:level*12,kind:i===0?'entry':slot===0?(i%5===0?'garden':i%3===0?'market':'house'):slot===1?(i%3===0?'tower':'court'):slot===2?(i%3===0?'obelisk':'colonnade'):slot===3?(i%2===0?'hangar':'relay'):slot===4?(i%3===0?'lens':'pylon'):roles[slot],ports:vectors.reduce((bits,[dx,dz],j)=>bits|(set.has(`${x+dx},${z+dz}`)||mega&&(Math.abs(x+dx)===1&&z+dz===0||Math.abs(z+dz)===1&&x+dx===0)?1<<j:0),0)}));
    if(slot===1){const gate=plan.find(n=>n.ports!==15),side=vectors.findIndex((_,j)=>!(gate.ports&(1<<j)));gate.kind='gate';gate.ports|=1<<side;}
    return plan;
  }
  function canonical(nodes){
    return [...Array(8)].map((_,r)=>{
      const points=nodes.map(n=>{let x=n.x*(r<4?1:-1),z=n.z;for(let i=0;i<r%4;i++)[x,z]=[-z,x];return[x,n.y,z];}),minX=Math.min(...points.map(p=>p[0])),minZ=Math.min(...points.map(p=>p[2]));
      return JSON.stringify(points.map(([x,y,z])=>[x-minX,y,z-minZ]).sort((a,b)=>a[1]-b[1]||a[0]-b[0]||a[2]-b[2]));
    }).sort()[0];
  }
  class Template {
    constructor(w,h,d){this.w=w;this.h=h;this.d=d;this.blocks=new Map();}
    put(x,y,z,Name,Properties={},nbt=null){if(x<0||y<0||z<0||x>=this.w||y>=this.h||z>=this.d)return;this.blocks.set(`${x},${y},${z}`,{x,y,z,Name,Properties,nbt});}
    fill(x0,y0,z0,x1,y1,z1,block,props={}){for(let y=y0;y<=y1;y++)for(let z=z0;z<=z1;z++)for(let x=x0;x<=x1;x++)this.put(x,y,z,block,props);}
    save(file){
      const palette=[],lookup=new Map(),blocks=[];
      for(const b of [...this.blocks.values()].sort((a,b)=>a.y-b.y||a.z-b.z||a.x-b.x)){
        const key=b.Name+JSON.stringify(b.Properties);if(!lookup.has(key)){lookup.set(key,palette.length);palette.push(compound({Name:str(b.Name),...(Object.keys(b.Properties).length?{Properties:compound(Object.fromEntries(Object.entries(b.Properties).map(([k,v])=>[k,str(v)])))}:{})}));}
        blocks.push(compound({pos:intList([b.x,b.y,b.z]),state:int(lookup.get(key)),...(b.nbt?{nbt:compound(b.nbt)}:{})}));
      }
      const bytes=zlib.gzipSync(Buffer.concat([Buffer.from([10,0,0]),payload(compound({DataVersion:int(3953),size:intList([this.w,this.h,this.d]),palette:list(10,palette),blocks:list(10,blocks),entities:list(10,[])}))]),{level:9});
      binary(`${data}/structure/${file}.nbt`,bytes);
      templateCount++;totalBlocks+=blocks.length;
      return {bytes,blocks:blocks.length};
    }
  }
  function buildModule(site,node,index,bodyIndex,moduleIndex,slot){
    const underground=site.underground,kind=node.kind,h=underground?12:['tower','lens','obelisk','pylon'].includes(kind)?28+bodyIndex%9:kind==='hangar'?18:14+bodyIndex%7;
    const template=`expedition_v2/${site.id}/module_${moduleIndex}`,descriptor={template,x:node.x*16-8,y:node.y,z:node.z*16-8,width:17,height:h,depth:17,role:kind,ports:node.ports,foundation:!underground};
    // Se reutilizan bytes de módulos idénticos SOLO dentro de su diseño, nunca se comparten rutas.
    const cacheKey=[site.id,kind,node.ports,moduleIndex%7===3||moduleIndex===0,underground&&moduleIndex%11===7].join('/'),cached=moduleCache.get(cacheKey);
    if(cached){binary(`${data}/structure/${template}.nbt`,cached.bytes);templateCount++;totalBlocks+=cached.blocks;return descriptor;}
    const t=new Template(17,h,17),rock=id(`${site.body}_${legacyKinds[(bodyIndex+slot)%5]}_panel`),trim=id(`${site.body}_${legacyKinds[(bodyIndex+slot+2)%5]}_panel`),stone=site.body==='earth'?'minecraft:deepslate_tiles':id(`${site.body}_stone`),air='minecraft:air',glass=id('window'),light='minecraft:sea_lantern';
    // Techos cerrados y cruceros de tres bloques mantienen rutas transitables de puerta a puerta.
    t.fill(0,0,0,16,0,16,rock);
    if(underground)t.fill(1,1,1,15,8,15,air);
    const inset=kind==='house'||kind==='market'?2:0,edge=16-inset,ceiling=underground?9:8;
    const open=['court','colonnade','pylon','garden','entry'].includes(kind)&&!underground;
    if(!open){
      for(let y=1;y<ceiling;y++)for(let p=inset;p<=edge;p++){
        const material=y===4&&p%4===1?'minecraft:tinted_glass':(y+bodyIndex)%4===0?trim:rock;
        for(const[x,z]of[[inset,p],[edge,p],[p,inset],[p,edge]])t.put(x,y,z,material);
      }
      if(!underground)t.fill(inset+1,1,inset+1,edge-1,ceiling-1,edge-1,air);
      t.fill(inset,ceiling,inset,edge,ceiling,edge,trim);
    }
    // Motivo del mundo: contrafuertes, arcos, escalones, nervaduras o coronas. No son recolores.
    const motif=bodyIndex%7;
    for(let corner=0;corner<4;corner++){
      const x=corner%2===0?2:14,z=corner<2?2:14;
      t.fill(x,1,z,x,Math.min(h-3,open?9:ceiling+3),z,trim);
      if(motif===0||motif===4)for(let dy=0;dy<5;dy++){const reach=5-dy;for(let dx=-reach;dx<=reach;dx++)if(Math.abs(dx)>=reach-1)t.put(8+dx,5+dy,z,rock);}
      if(motif===1||motif===5)for(let dy=0;dy<5;dy++)t.fill(x-1,ceiling+dy,z-1,x+1,ceiling+dy,z+1,dy%2===0?trim:stone);
      if(motif===2||motif===6)for(let dy=0;dy<6;dy++)t.put(x+(corner%2===0?1:-1)*Math.floor(dy/2),ceiling+dy,z,trim);
    }
    if(['tower','pylon','lens','obelisk'].includes(kind)){
      const radius=kind==='obelisk'?2:kind==='lens'?5:3;
      for(let y=9;y<h-2;y++)for(let x=8-radius;x<=8+radius;x++)for(let z=8-radius;z<=8+radius;z++){
        const boundary=x===8-radius||x===8+radius||z===8-radius||z===8+radius;
        if(boundary||y===h-3)t.put(x,y,z,kind==='lens'&&y>h-8?'minecraft:tinted_glass':(y+bodyIndex)%4===0?trim:rock);else t.put(x,y,z,air);
      }
      if(kind!=='obelisk')for(let y=1;y<h-3;y++){t.put(8-radius+1,y,8, 'minecraft:ladder',{facing:'east',waterlogged:'false'});t.put(8-radius+1,y,9,air);}
    }
    if(kind==='hangar'||slot===6){for(let z=2;z<=14;z+=3)for(let x=2;x<=14;x++)t.put(x,9+Math.round(Math.sqrt(36-(x-8)**2))*.4|0,z,trim);}
    if(kind==='garden')for(let x=3;x<=13;x+=3)for(let z=3;z<=13;z+=3){t.put(x,1,z,site.body==='earth'?'minecraft:dirt':id(`${site.body}_regolith`));t.put(x,2,z,id(`${site.body}_${profiles[index].flora[(x+z)%3]}`),{age:'2'});}
    if(slot===5){for(let p=2;p<15;p+=4){t.fill(p,1,3,p,6,3,trim);t.fill(p,1,13,p,6,13,trim);t.fill(p,6,3,p,6,13,trim);}t.fill(8,1,1,8,1,15,'minecraft:rail',{shape:'north_south',waterlogged:'false'});}
    if(slot===6)for(const x of [3,12])for(const z of [3,12]){t.fill(x,1,z,x+1,3,z+1,id('energy_cell'));t.put(x,4,z,light);}
    if(slot===7)for(const z of [3,12])for(let x=3;x<=12;x+=3)t.fill(x,1,z,x+1,1,z+2,trim);
    if(slot===8)for(const x of [2,14])for(let z=2;z<=14;z+=3){t.fill(x,1,z,x,5,z,'minecraft:bookshelf');t.put(x,6,z,trim);}
    for(const[x,z]of[[4,4],[12,12]])t.put(x,ceiling-1,z,light);
    // Crucero libre en toda la planta (salvo los raíles, que no bloquean el paso).
    for(let p=0;p<17;p++)for(let y=1;y<=3;y++)for(let width=-1;width<=1;width++){
      if(!underground||p>0&&p<16){t.put(p,y,8+width,air);if(slot!==5||y!==1)t.put(8+width,y,p,air);}
    }
    for(let side=0;side<4;side++)if(node.ports&(1<<side))for(let y=1;y<=4;y++)for(let q=6;q<=10;q++)for(let thick=0;thick<=2;thick++){
      const[x,z]=side===0?[q,thick]:side===1?[16-thick,q]:side===2?[q,16-thick]:[thick,q];t.put(x,y,z,air);
    }
    // Las murallas siguen el perímetro de la planta y dejan abierta la puerta exterior del recinto.
    if(slot===1)for(let side=0;side<4;side++)if(!(node.ports&(1<<side)))for(let p=0;p<17;p++)for(let y=1;y<=10;y++){
      if(y===10&&p%3===1)continue;const[x,z]=side===0?[p,0]:side===1?[16,p]:side===2?[p,16]:[0,p];t.put(x,y,z,y%3===0?trim:rock);
    }
    if(slot===2&&kind==='colonnade')for(let step=0;step<3;step++)t.fill(2+step,1+step,2+step,6-step,1+step,6-step,trim);
    if(moduleIndex%7===3||moduleIndex===0){
      t.put(4,0,4,rock);t.put(4,1,4,'minecraft:chest',{facing:'south',type:'single',waterlogged:'false'},{id:str('minecraft:chest'),LootTable:str(id(`chests/expedition_v2/${site.id}`))});t.fill(4,2,4,4,3,4,air);
    }
    if(underground&&moduleIndex%11===7)t.put(12,1,12,'minecraft:spawner',{}, {id:str('minecraft:mob_spawner'),Delay:int(100),MinSpawnDelay:int(400),MaxSpawnDelay:int(800),SpawnCount:int(2),MaxNearbyEntities:int(4),RequiredPlayerRange:int(12),SpawnRange:int(3),SpawnData:compound({entity:compound({id:str(id(`${site.body}_stalker`))})})});
    if(slot===0&&(kind==='house'||kind==='garden')){
      const fauna=kind==='garden';t.put(11,1,5,'minecraft:structure_block',{mode:'data'},{id:str('minecraft:structure_block'),mode:str('DATA'),metadata:str(`${fauna?'fauna':'citizen'}:0`)});t.fill(11,2,5,11,3,5,air);
    }
    moduleCache.set(cacheKey,t.save(template));return descriptor;
  }
  function arena(site,bodyIndex){
    const t=new Template(51,20,51),rock=id(`${site.body}_vault_panel`),trim=id(`${site.body}_sanctuary_panel`),air='minecraft:air';
    t.fill(0,0,0,50,0,50,rock);t.fill(1,1,1,49,17,49,air);t.fill(0,18,0,50,18,50,trim);
    for(let y=1;y<=18;y++)for(let p=0;p<=50;p++)for(const[x,z]of[[0,p],[50,p],[p,0],[p,50]])t.put(x,y,z,(p+y+bodyIndex)%7===0?trim:rock);
    for(let i=0;i<8+bodyIndex%5;i++){
      const a=i*Math.PI*2/(8+bodyIndex%5),x=Math.round(25+Math.cos(a)*20),z=Math.round(25+Math.sin(a)*20);
      t.fill(x-1,1,z-1,x+1,12+bodyIndex%5,z+1,trim);t.put(x,15+bodyIndex%3,z,'minecraft:sea_lantern');
    }
    // Cuatro accesos y centro despejado de radio 16; el único sello queda en el altar central.
    for(let side=0;side<4;side++)for(let y=1;y<=6;y++)for(let q=23;q<=27;q++)for(let thick=0;thick<=2;thick++){
      const[x,z]=side===0?[q,thick]:side===1?[50-thick,q]:side===2?[q,50-thick]:[thick,q];t.put(x,y,z,air);
    }
    const anchor=site.body==='mars'||['RIFT','DUNES'].includes(profiles[bodyIndex].terrain)&&site.body!=='moon'?'martian_boss_anchor':'lunar_boss_anchor';
    t.put(25,1,25,id(anchor),{}, {id:str(id('boss_anchor')),EncounterBody:str(site.body)});
    t.put(40,1,40,'minecraft:chest',{facing:'south',type:'single',waterlogged:'false'},{id:str('minecraft:chest'),LootTable:str(id(`chests/expedition_v2/${site.id}`))});t.fill(40,2,40,40,3,40,air);
    const template=`expedition_v2/${site.id}/central_arena`;t.save(template);return{template,x:-25,y:0,z:-25,width:51,height:20,depth:51,role:'boss_arena',ports:15,foundation:false};
  }
  for(const [bodyIndex,body]of bodies.entries()){
    const theme=themes[body.id];if(!theme)throw Error(`Falta tema ${body.id}`);const names=theme[1].split('|');if(names.length!==10)throw Error('Catálogo incompleto');
    for(let slot=0;slot<10;slot++){
      const name=names[slot],site={id:`${body.id}_${slug(name)}`,body:body.id,name,role:roles[slot],theme:theme[0],underground:slot>=5,mega:slot===9,burial:slot===9?10:8,entranceX:slot===9?-48:0,entranceZ:0};
      let nodes,geometryHash;for(let attempt=0;attempt<20;attempt++){
        nodes=layout(bodyIndex,slot,0,attempt);if(slot>=5)nodes.push(...layout(bodyIndex,slot,1,attempt));if(slot===7||slot===9)nodes.push(...layout(bodyIndex,slot,2,attempt));
        geometryHash=createHash('sha256').update(canonical(nodes)).digest('hex');if(!geometries.has(geometryHash))break;
      }
      if(geometries.has(geometryHash))throw Error('Arquitectura repetida');geometries.add(geometryHash);site.geometryHash=geometryHash;
      site.modules=nodes.map((node,i)=>buildModule(site,node,bodyIndex,bodyIndex,i,slot));if(site.mega)site.modules.push(arena(site,bodyIndex));
      const minX=Math.min(...site.modules.map(m=>m.x)),maxX=Math.max(...site.modules.map(m=>m.x+m.width-1)),minZ=Math.min(...site.modules.map(m=>m.z)),maxZ=Math.max(...site.modules.map(m=>m.z+m.depth-1));
      site.width=maxX-minX+1;site.depth=maxZ-minZ+1;site.height=Math.max(...site.modules.map(m=>m.y+m.height));
      if(Math.max(Math.abs(minX),Math.abs(maxX),Math.abs(minZ),Math.abs(maxZ))>112)throw Error('Estructura fuera del alcance de referencias');manifest.push(site);
      const biomes=body.id==='earth'?'#galaxycraft:colony_earth':`#${id(`${body.id}_habitats`)}`;
      json(`${data}/worldgen/structure/${site.id}.json`,{type:id('exclusive_expedition'),site:site.id,biomes,step:site.underground?'underground_structures':'surface_structures',terrain_adaptation:'none',spawn_overrides:slot===0?{monster:{bounding_box:'full',spawns:[]}}:{}});
      const resource=body.resource||'titanium_ore',schematic=`${site.id}_schematic`,item=(n,min,max,weight=1)=>({type:'minecraft:item',name:n,weight,functions:[{function:'minecraft:set_count',count:{type:'minecraft:uniform',min,max}}]});
      json(`${data}/loot_table/chests/expedition_v2/${site.id}.json`,{type:'minecraft:chest',pools:[{rolls:1,entries:[item(id(schematic),1,1)]},{rolls:{type:'minecraft:uniform',min:3,max:site.mega?8:5},entries:[item(id(resource),4,12,5),item(id('titanium_ore'),3,8,3),item(id('colony_token'),1,3,2),item('minecraft:diamond',1,3),item(id('oxygen_tank'),1,1)]}]});
      json(`${data}/recipe/expedition_v2/${site.id}.json`,{type:'minecraft:crafting_shaped',category:'equipment',pattern:['RTR','TST','RDR'],key:{R:{item:id(resource)},T:{item:id('titanium_ore')},S:{item:id(schematic)},D:{item:site.mega?id('martian_queen_heart'):'minecraft:diamond'}},result:{id:id(results[slot]),count:slot<2?4:1}});
      png(`item/${schematic}`,64,64,(x,y)=>{if(x<8||x>55||y<5||y>58)return[0,0,0,0];const px=Math.floor((x-12)/4)-5,pz=Math.floor((y-12)/4)-5,inside=nodes.some(n=>n.y===0&&n.x===px&&n.z===pz);return inside?[100+bodyIndex*3%130,190,224,255]:x%8===0||y%8===0?[48,83,118,255]:[14,32,61,255];});
      json(`${assets}/models/item/${schematic}.json`,{parent:'minecraft:item/generated',textures:{layer0:id(`item/${schematic}`)}});blueprints.push({id:site.id,name,body:body.id});
    }
    const selected=manifest.filter(s=>s.body===body.id),hostile=selected.filter(s=>['fortress','temple','mine','factory','catacombs','archive','megadungeon'].includes(s.role));
    json(`${data}/worldgen/structure_set/expeditions_${body.id}.json`,{structures:selected.map(s=>({structure:id(s.id),weight:1})),placement:{type:id('desolate_spread'),body:body.id,spacing:384,separation:192,salt:5800127+bodyIndex*7919}});
    json(`${data}/tags/worldgen/structure/expedition_v2/${body.id}.json`,{replace:true,values:selected.map(s=>id(s.id))});
    json(`${data}/tags/worldgen/structure/expedition_v2/${body.id}_hostile.json`,{replace:true,values:hostile.map(s=>id(s.id))});
    json(`${data}/neoforge/structure_modifier/expedition_v2/${body.id}_guards.json`,{type:'neoforge:add_spawns',structures:`#${id(`expedition_v2/${body.id}_hostile`)}`,spawners:[{type:id(`${body.id}_stalker`),weight:30,minCount:1,maxCount:2}]});
    const mega=selected.find(s=>s.mega),core=body.id==='moon'?'lunar_guardian_core':body.id==='mars'?'martian_queen_heart':'plasma_shard';
    json(`${data}/loot_table/entities/expedition_wardens/${body.id}.json`,{type:'minecraft:entity',pools:[{rolls:1,entries:[{type:'minecraft:item',name:id(core)}]},{rolls:1,entries:[{type:'minecraft:item',name:id(`${mega.id}_schematic`)}]},{rolls:3,entries:[{type:'minecraft:item',name:id(body.resource||'titanium_ore'),functions:[{function:'minecraft:set_count',count:4}]}]}]});
  }
  json('expeditions_v2.json',manifest);
  json('expedition_generation.json',{version:2,structures:manifest.length,perBody:10,surface:5,underground:5,spacingChunks:384,separationChunks:192,minimumCandidateDistance:3088,templateCount,totalBlocks,originalArchitecture:true,layouts:manifest.map(s=>({id:s.id,hash:s.geometryHash}))});
  for(const[lang,index]of[['es_es',0],['en_us',1]]){
    const file=`${assets}/lang/${lang}.json`,texts=read(file);
    for(const entry of blueprints){texts[`item.galaxycraft.${entry.id}_schematic`]=index===0?`Plano: ${entry.name}`:`Blueprint: ${entry.name}`;texts[`tooltip.galaxycraft.detail.${entry.id}_schematic`]=index===0?'Expedición exclusiva de este mundo. Clic derecho: descubre una receta; la fabricación consume el plano.':'Exclusive planetary expedition. Right-click to learn a recipe; crafting consumes the blueprint.';}
    for(const body of bodies)texts[`boss.galaxycraft.expedition.${body.id}`]=index===0?`Custodio de ${body.name}`:`Warden of ${body.id}`;
    json(file,texts);
  }
  console.log(`Exploración reconstruida: ${manifest.length} diseños exclusivos, ${templateCount} NBT; diez por mundo, distribución 5+5 y separación mínima de candidatos 3088 bloques.`);
}
