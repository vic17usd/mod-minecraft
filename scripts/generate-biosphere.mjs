// Biosfera reproducible: perfiles, fachadas, cultivos, fauna, colonias, biomas y cielo originales.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
export function generateBiosphere({png,json,read,id,bodies,assets,data}){
    const terrains={earth:'VANILLA',moon:'CRATERS',mercury:'RIFT',venus:'RIFT',mars:'DUNES',phobos:'CRATERS',deimos:'CRATERS',jupiter:'DUNES',io:'RIFT',europa:'GEYSERS',ganymede:'RIDGES',callisto:'CRATERS',saturn:'DUNES',titan:'DUNES',enceladus:'GEYSERS',mimas:'CRATERS',tethys:'RIDGES',dione:'RIDGES',rhea:'CRATERS',iapetus:'RIDGES',uranus:'RIDGES',miranda:'RIDGES',ariel:'GEYSERS',umbriel:'CRATERS',titania:'RIDGES',oberon:'CRATERS',neptune:'GEYSERS',triton:'GEYSERS',proteus:'CRATERS'};
    const palettes={earth:0x6C9D83,moon:0x8491B4,mercury:0x706E8F,venus:0xBA8B39,mars:0xA65E47,jupiter:0xBC8C67,saturn:0xCDBA7F,uranus:0x6EAFB4,neptune:0x405EA8};
    const styles=['fungus','crystal_vine','carbon_fern','iron_root','magma_maw','sulfur_thorn','cinder_reed','lumen_coral','ice_spire','spore_fan'];
    const labels={fungus:['Hongo bioluminiscente','Bioluminescent Fungus'],crystal_vine:['Enredadera de cristal','Crystal Vine'],carbon_fern:['Helecho de carbono','Carbon Fern'],iron_root:['Raíz ferruginosa','Iron Root'],magma_maw:['Fauce de magma','Magma Maw'],sulfur_thorn:['Espina de azufre','Sulfur Thorn'],cinder_reed:['Junco de ceniza','Cinder Reed'],lumen_coral:['Coral luminoso','Luminous Coral'],ice_spire:['Aguja criófila','Cryophilic Spire'],spore_fan:['Abanico de esporas','Spore Fan']};
    const rgb=n=>[n>>16&255,n>>8&255,n&255],clamp=color=>color.map(c=>Math.max(0,Math.min(255,Math.round(c)))),pack=c=>c[0]<<16|c[1]<<8|c[2];
    const hash=(x,y,s)=>{let n=Math.imul(x,374761393)^Math.imul(y,668265263)^s;n=Math.imul(n^n>>>13,1274126177);return((n^n>>>16)>>>0)/4294967295;};
    const profiles=bodies.map((body,index)=>{
        const terrain=terrains[body.id],base=palettes[body.id]??pack(clamp(rgb(palettes[body.parent]??0x8FA5BC).map((c,k)=>c*.7+hash(index,k,17)*65)));
        const cool=['fungus','crystal_vine','carbon_fern','iron_root','lumen_coral','ice_spire','spore_fan'];
        const flora=body.id==='venus'?['magma_maw','sulfur_thorn','cinder_reed']:body.id==='io'?['sulfur_thorn','iron_root','magma_maw']:body.id==='mercury'?['sulfur_thorn','cinder_reed','iron_root']:body.id==='moon'?['fungus','crystal_vine','ice_spire']:body.id==='mars'?['carbon_fern','iron_root','spore_fan']:Array.from({length:3},(_,j)=>cool[(index*3+j*2)%cool.length]);
        let sky=body.id==='mars'?0xC67B50:body.id==='venus'?0x8C7248:body.id==='earth'?0x78A7FF:body.atmosphere==='none'?0x080D24:pack(clamp(rgb(base).map(c=>c*.7)));
        return{body:body.id,terrain,flora,sky,fog:body.id==='venus'?0xA18D54:pack(clamp(rgb(base).map(c=>c*.8))),color:base,fogDistance:body.id==='venus'?40:body.id==='titan'?64:body.id==='mars'?180:240,seed:7103+index*331,hunter:terrain==='RIFT'?'magma':body.id==='mars'||body.id==='titan'?'spitter':'pouncer'};
    });
    // Limpieza exacta de recursos generados obsoletos de cultivos, sin glob ni carpetas ajenas.
    let previous=[];try{previous=read('living_worlds.json');}catch{}
    const removed=[];const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../src/main/resources');
    for(const old of previous){const current=profiles.find(profile=>profile.body===old.body);if(!current)continue;for(const style of old.flora)if(!current.flora.includes(style)){const name=`${old.body}_${style}`;removed.push(name);const files=[`${assets}/blockstates/${name}.json`,`${assets}/models/item/${name}.json`,`${assets}/textures/block/${name}.png`,`${data}/loot_table/blocks/${name}.json`,...[0,1,2].map(age=>`${assets}/models/block/${name}_stage${age}.json`)];for(const file of files){const target=path.join(root,file);if(fs.existsSync(target))fs.unlinkSync(target);}}}
    json('living_worlds.json',profiles);
    const kinds=['observatory','wreck','sanctuary','geode','vault'],facadeLabels=[['Panel de aleación de observatorio','Observatory Alloy Panel'],['Placa de blindaje recuperada','Salvaged Armor Plate'],['Sillar de santuario glífico','Glyphed Sanctuary Ashlar'],['Cristal tallado de geoda','Carved Geode Crystal'],['Ladrillo reforzado de bóveda','Reinforced Vault Brick']];
    const names={colony_token:['Ficha colonial','Colony Token'],alien_protein:['Proteína alienígena','Alien Protein'],luminous_fiber:['Fibra bioluminiscente','Bioluminescent Fiber']};
    const details={};
    const cube=(name,texture=name)=>{json(`${assets}/blockstates/${name}.json`,{variants:{'':{model:id(`block/${name}`)}}});json(`${assets}/models/block/${name}.json`,{parent:'minecraft:block/cube_all',textures:{all:id(`block/${texture}`)}});json(`${assets}/models/item/${name}.json`,{parent:id(`block/${name}`)});};
    const loot=(name,entries)=>json(`${data}/loot_table/blocks/${name}.json`,{type:'minecraft:block',pools:[{rolls:1,entries:entries??[{type:'minecraft:item',name:id(name)}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
    const shaped=(name,pattern,key,count=1)=>json(`${data}/recipe/${name}.json`,{type:'minecraft:crafting_shaped',category:'building',pattern,key:Object.fromEntries(Object.entries(key).map(([symbol,item])=>[symbol,{item:item.includes(':')?item:id(item)}])),result:{id:id(name),count}});
    const allPickaxe=[];
    function element(from,to,extra={}){return{from,to,faces:Object.fromEntries(['north','south','east','west','up','down'].map(face=>[face,{texture:'#all'}])),...extra};}
    for(const[index,profile]of profiles.entries()){
        const body=bodies.find(value=>value.id===profile.body),base=rgb(profile.color),seed=profile.seed;
        for(const[k,kind]of kinds.entries()){
            const name=`${body.id}_${kind}_panel`;names[name]=[`${facadeLabels[k][0]} · ${body.name}`,`${body.id} ${facadeLabels[k][1]}`];allPickaxe.push(id(name));
            png(`block/${name}`,128,128,(x,y)=>{
                let shade=(hash(x,y,seed)-.5)*9,color=base.map(c=>c*.75+30);const bx=(x+(Math.floor(y/32)%2)*16)%32,by=y%32;
                if(kind==='observatory'){shade+=x%64<4||y%32<3?-46:8;if((x%32===10||y%64===12)&&x%9<5)color=[52,189,202];}
                if(kind==='wreck'){shade+=(x+y)%45<5?-45:12;if([9,55].includes(x%64)&&[9,55].includes(y%64))shade-=70;}
                if(kind==='sanctuary'){shade-=Math.abs(Math.sin(x/9)+Math.cos(y/11))<.18?55:0;if(x>24&&x<104&&y>24&&y<104&&(Math.floor(x/9)^Math.floor(y/9)^index)%7===0)color=[110,190,201];}
                if(kind==='geode'){let gx=(x+(Math.floor(y/24)%2)*16)%32-16,gy=y%24-12;shade+=Math.max(-45,24-Math.abs(gx)*2-Math.abs(gy)*2);if(Math.abs(gx)+Math.abs(gy)>23)shade-=35;}
                if(kind==='vault'){color=color.map(c=>c*.65);shade+=bx<4||by<4?-38:14;if(x%64<6)shade-=20;}
                return clamp(color.map(c=>c+shade));
            });cube(name);loot(name);shaped(name,['SRS','STS','SRS'],{S:body.id==='earth'?'minecraft:stone':`${body.id}_stone`,R:body.resource||'titanium_ore',T:'titanium_ore'},8);
        }
        const harvest=`${body.id}_biosample`;names[harvest]=[`Biomuestra nutritiva de ${body.name}`,`${body.id} Nutritive Biosample`];
        png(`item/${harvest}`,64,64,(x,y)=>{const r=Math.hypot(x-32,(y-32)*1.15);if(r>22+3*Math.sin(Math.atan2(y-32,x-32)*5))return[0,0,0,0];return clamp(base.map(c=>c*.75+40+(32-x)*.9+(hash(x,y,seed)-.5)*12));});json(`${assets}/models/item/${harvest}.json`,{parent:'minecraft:item/generated',textures:{layer0:id(`item/${harvest}`)}});
        for(const[plantIndex,style]of profile.flora.entries()){
            const name=`${body.id}_${style}`;names[name]=[`${labels[style][0]} de ${body.name}`,`${body.id} ${labels[style][1]}`];
            details[name]=[`Solo crece en ${body.name}, sobre su suelo nativo. Clic derecho madura: dos biomuestras y vuelve a brotar.`,`Grows only on ${body.id} in native soil. Right-click mature plants: two biosamples and regrowth.`];
            png(`block/${name}`,64,64,(x,y)=>{let color=base.map((c,k)=>c*.65+[25,65,45][k]);if(style==='magma_maw'||style==='sulfur_thorn')color=[210,147,44];if(style==='crystal_vine'||style==='ice_spire')color=[102,177,215];const vein=(x+y*(index%3+1))%17<3;return clamp(color.map(c=>c+(hash(x,y,seed+plantIndex)-.5)*15-(vein?38:0)+(x<32?12:-8)));});
            const variants={};for(let age=0;age<3;age++){
                const h=5+age*4+(index%3),w=2+age,elements=[];elements.push(element([7,0,7],[9,h,9]));
                if(style==='fungus'){elements.push(element([3,h-2,3],[13,h,13]),element([5,h,5],[11,h+1,11]));}
                else if(style==='crystal_vine'){for(let j=0;j<3+age;j++){let cy=2+j*2;elements.push(element([4+(j%2)*5,cy,5],[7+(j%2)*5,cy+2,11]));}}
                else if(style==='magma_maw'){elements.push(element([3,h-5,3],[13,h-2,13]),element([2,h-2,3],[4,h+1,13]),element([12,h-2,3],[14,h+1,13]),element([5,h-2,3],[7,h,5]),element([9,h-2,11],[11,h,13]));}
                else if(style==='sulfur_thorn'||style==='ice_spire'){for(let j=0;j<3+age;j++){const sx=3+(j*5+index)%10,sz=3+(j*3+index)%10;elements.push(element([sx,0,sz],[sx+2,h-j%3,sz+2]));}}
                else if(style==='carbon_fern'||style==='spore_fan'){for(let j=0;j<3+age;j++){const y=2+j*2;elements.push(element([2+j%2,y,6],[14-j%2,y+1,10]),element([6,y,3],[10,y+1,13]));}}
                else{for(let j=0;j<3+age;j++){const sx=3+(j*3+index)%9;elements.push(element([sx,0,4+j],[sx+2,Math.max(3,h-j%4),6+j]));}}
                const model=`${name}_stage${age}`;json(`${assets}/models/block/${model}.json`,{textures:{all:id(`block/${name}`),particle:id(`block/${name}`)},elements});variants[`age=${age}`]={model:id(`block/${model}`)};
            }
            json(`${assets}/blockstates/${name}.json`,{variants});json(`${assets}/models/item/${name}.json`,{parent:id(`block/${name}_stage2`)});
            json(`${data}/loot_table/blocks/${name}.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:id(name)}]},{rolls:1,conditions:[{condition:'minecraft:block_state_property',block:id(name),properties:{age:'2'}}],entries:[{type:'minecraft:item',name:id(harvest),functions:[{function:'minecraft:set_count',count:{type:'minecraft:uniform',min:2,max:3}}]}]}]});
        }
        const vent=`${body.id}_geyser`;names[vent]=[`Respiradero geotérmico de ${body.name}`,`${body.id} Geothermal Vent`];allPickaxe.push(id(vent));png(`block/${vent}`,128,128,(x,y)=>{const r=Math.hypot(x-64,y-64);return clamp((r<23?[21,27,38]:base).map(c=>c+(hash(x,y,seed)-.5)*13-(r>23&&r<30?40:0)));});cube(vent);loot(vent);
        shaped(vent,['SSS','SBS','SSS'],{S:body.id==='earth'?'minecraft:stone':`${body.id}_stone`,B:'minecraft:blaze_powder'});
        if(body.id!=='earth'){
            const dimension=read(`${data}/dimension/${body.id}.json`);dimension.generator={type:id('extreme_planet'),body:body.id,biome_source:{type:id('planet_habitats'),body:body.id,plains:id(body.id),extreme:id(`${body.id}_rift`),grove:id(`${body.id}_grove`)}};json(`${data}/dimension/${body.id}.json`,dimension);
            const type=read(`${data}/dimension_type/${body.id}.json`);delete type.fixed_time;type.effects=id(`sky/${body.id}`);json(`${data}/dimension_type/${body.id}.json`,type);
            const biome=read(`${data}/worldgen/biome/${body.id}.json`);biome.effects.sky_color=profile.sky;biome.effects.fog_color=profile.fog;biome.features[9]=[...(biome.features[9]??[]),id(`${body.id}_ecology`)];json(`${data}/worldgen/biome/${body.id}.json`,biome);
            for(const subtype of['rift','grove']){const variant=structuredClone(biome);variant.effects.fog_color=subtype==='grove'?pack(clamp(rgb(profile.fog).map(c=>c*.8))):profile.fog;variant.features[9]=variant.features[9].map(feature=>feature===id(`${body.id}_ecology`)?id(`${body.id}_ecology_${subtype}`):feature);json(`${data}/worldgen/biome/${body.id}_${subtype}.json`,variant);json(`${data}/worldgen/placed_feature/${body.id}_ecology_${subtype}.json`,{feature:id(`${body.id}_ecology`),placement:[{type:'minecraft:count',count:subtype==='grove'?14:2},{type:'minecraft:in_square'},{type:'minecraft:heightmap',heightmap:'WORLD_SURFACE_WG'},{type:'minecraft:biome'}]});}
            json(`${data}/worldgen/configured_feature/${body.id}_ecology.json`,{type:id('planet_ecology'),config:{body:body.id}});json(`${data}/worldgen/placed_feature/${body.id}_ecology.json`,{feature:id(`${body.id}_ecology`),placement:[{type:'minecraft:count',count:7},{type:'minecraft:in_square'},{type:'minecraft:heightmap',heightmap:'WORLD_SURFACE_WG'},{type:'minecraft:biome'}]});
            json(`${data}/tags/worldgen/biome/${body.id}_habitats.json`,{replace:false,values:[id(body.id),id(`${body.id}_rift`),id(`${body.id}_grove`)]});
            json(`${data}/neoforge/biome_modifier/${body.id}_living_spawns.json`,{type:'neoforge:add_spawns',biomes:`#${id(`${body.id}_habitats`)}`,spawners:[...['crystal_bovine','lumen_sheep','plated_boar'].map(species=>({type:id(`${body.id}_${species}`),weight:3,minCount:2,maxCount:3})),{type:id(`${body.id}_stalker`),weight:6,minCount:1,maxCount:2}]});
            // Las cinco expediciones existentes siguen localizándose en los tres biomas del planeta.
            for(const kind of kinds){const file=`${data}/worldgen/structure/${body.id}_${kind}.json`,structure=read(file);structure.biomes=`#${id(`${body.id}_habitats`)}`;json(file,structure);}
        }
        const colonyTag=body.id==='earth'?'#galaxycraft:colony_earth':`#${id(`${body.id}_habitats`)}`;
        json(`${data}/worldgen/structure/colony_${body.id}.json`,{type:id('alien_colony'),body:body.id,biomes:colonyTag,step:'surface_structures',spawn_overrides:{},terrain_adaptation:'none'});
        json(`${data}/worldgen/structure_set/colony_${body.id}.json`,{structures:[{structure:id(`colony_${body.id}`),weight:1}],placement:{type:'minecraft:random_spread',spacing:40,separation:16,salt:338711+index*101}});
        json(`${data}/loot_table/chests/colonies/${body.id}.json`,{type:'minecraft:chest',pools:[{rolls:{type:'minecraft:uniform',min:2,max:4},entries:[{type:'minecraft:item',name:id(harvest),weight:5,functions:[{function:'minecraft:set_count',count:{type:'minecraft:uniform',min:2,max:6}}]},{type:'minecraft:item',name:id('colony_token'),weight:2},{type:'minecraft:item',name:id(`${body.id}_observatory_schematic`),weight:1}]}]});
        for(const species of['crystal_bovine','lumen_sheep','plated_boar','colonist','stalker']){
            const name=`${body.id}_${species}`,textureSize=128;
            const skinSeed=seed+[...species].reduce((sum,c)=>sum+c.charCodeAt(0)*17,0);
            png(`entity/${name}`,textureSize,textureSize,(x,y)=>{let color=base.map(c=>c*.7+35);if(species==='lumen_sheep')color=color.map(c=>c*.7+65);if(species==='stalker')color=color.map(c=>c*.5);const plates=(x+Math.floor(y/12)*7)%19<3,glow=y%32>8&&y%32<12&&x%16>4;return clamp((glow?[91,223,216]:color).map(c=>c+(hash(x,y,skinSeed)-.5)*15-(plates?29:0)));});
            if(species!=='colonist'){const pools=[{rolls:1,entries:[{type:'minecraft:item',name:id(species==='stalker'?harvest:'alien_protein'),functions:[{function:'minecraft:set_count',count:{type:'minecraft:uniform',min:1,max:3}}]}]}];if(species==='lumen_sheep')pools.push({rolls:1,entries:[{type:'minecraft:item',name:id('luminous_fiber')}]});json(`${data}/loot_table/entities/${name}.json`,{type:'minecraft:entity',pools});}
            else json(`${data}/loot_table/entities/${name}.json`,{type:'minecraft:entity',pools:[]});
        }
        // Discos esféricos sombreados originales para ver el planeta padre desde sus lunas.
        png(`gui/sky/${body.id}`,128,128,(x,y)=>{const dx=(x-64)/59,dy=(y-64)/59,r=dx*dx+dy*dy;if(r>1)return[0,0,0,0];const light=Math.max(.12,Math.sqrt(1-r)*.8-dx*.35-dy*.18),bands=body.id==='jupiter'||body.id==='saturn'?Math.sin(y*.2)*17:(hash(Math.floor(x/4),Math.floor(y/4),seed)-.5)*30;return[...clamp(base.map(c=>c*light+bands)),255];});
    }
    json(`${data}/tags/worldgen/biome/colony_earth.json`,{replace:false,values:['minecraft:plains','minecraft:forest','minecraft:birch_forest','minecraft:meadow','minecraft:savanna']});
    for(const[name,color]of Object.entries({colony_token:[212,175,65],alien_protein:[142,72,124],luminous_fiber:[82,204,198]})){png(`item/${name}`,64,64,(x,y)=>{if(Math.hypot(x-32,y-32)>23)return[0,0,0,0];return clamp(color.map(c=>c+(hash(x,y,17)-.5)*12+(32-x)*.6));});json(`${assets}/models/item/${name}.json`,{parent:'minecraft:item/generated',textures:{layer0:id(`item/${name}`)}});}
    png('gui/sky/nebula',256,128,(x,y)=>{const a=Math.max(0,1-Math.abs(y-64-Math.sin(x/37)*17)/38),stars=hash(x,y,51)>.997;return stars?[220,237,255,220]:[65+Math.round(a*72),39+Math.round(a*48),115+Math.round(a*80),Math.round(a*a*115)];});
    png('gui/sky/sun',128,128,(x,y)=>{const r=Math.hypot(x-64,y-64);if(r>61)return[0,0,0,0];return[255,Math.round(215-r*.6),Math.round(100-r*.7),r<45?255:Math.round((61-r)/16*180)];});
    for(const tag of['mineable/pickaxe','needs_iron_tool']){const file=`data/minecraft/tags/block/${tag}.json`,content=read(file);content.values=[...new Set([...content.values,...allPickaxe])];json(file,content);}
    const phrases={
        'section.galaxycraft.biosphere':['━━ BIOSFERA Y CIVILIZACIÓN ━━','━━ BIOSPHERE AND CIVILIZATION ━━'],
        'colony.galaxycraft.title':['Colonia de %s','%s Colony'],'colony.galaxycraft.stock':['Existencias: %s / 8','Stock: %s / 8'],
        'colony.galaxycraft.buy':['Comprar tanque O₂','Buy O₂ tank'],'colony.galaxycraft.sell':['Vender biomuestras','Sell biosamples'],'colony.galaxycraft.accept':['Aceptar contrato','Accept contract'],'colony.galaxycraft.claim':['Entregar contrato','Submit contract'],
        'colony.galaxycraft.buy_tip':['Cuatro recursos nativos → tanque lleno. Consume una existencia.','Four native resources → full tank. Uses one stock.'],'colony.galaxycraft.sell_tip':['Ocho biomuestras nativas → una ficha colonial.','Eight native biosamples → one colony token.'],
        'colony.galaxycraft.contract_tip':['Entrega 16 biomuestras de este planeta: 2 fichas, 4 recursos y 150 XP. Una vez por planeta.','Deliver 16 biosamples from this planet: 2 tokens, 4 resources and 150 XP. Once per planet.'],
        'colony.galaxycraft.progress':['Biomuestras: %s / 16','Biosamples: %s / 16'],'colony.galaxycraft.done':['Contrato completado','Contract completed'],'colony.galaxycraft.habitat':['Suelo y atmósfera: %s','Soil and atmosphere: %s']
        ,'colony.galaxycraft.supplies':['Comprar recursos','Buy resources'],'colony.galaxycraft.supplies_tip':['Una ficha colonial → cuatro recursos nativos.','One colony token → four native resources.']
    };
    for(const[index,language]of['es_es','en_us'].entries()){
        const file=`${assets}/lang/${language}.json`,text=read(file);
        for(const name of removed){delete text[`block.galaxycraft.${name}`];delete text[`item.galaxycraft.${name}`];delete text[`tooltip.galaxycraft.detail.${name}`];}
        for(const[name,values]of Object.entries(names)){const block=name.endsWith('_panel')||name.endsWith('_geyser')||profiles.some(profile=>profile.flora.some(style=>name===`${profile.body}_${style}`));text[`${block?'block':'item'}.galaxycraft.${name}`]=values[index];text[`tooltip.galaxycraft.detail.${name}`]=details[name]?.[index]??(index===0?'Material nativo de la biosfera y los asentamientos de GalaxyCraft.':'Native material of GalaxyCraft biospheres and settlements.');}
        for(const[key,values]of Object.entries(phrases))text[key]=values[index];
        for(const body of bodies){for(const[species,title]of Object.entries({crystal_bovine:['Bóvido cristalino','Crystal Bovine'],lumen_sheep:['Oveja luminosa','Luminous Sheep'],plated_boar:['Jabalí acorazado','Plated Boar'],colonist:['Colono','Colonist'],stalker:['Acechador autóctono','Native Stalker']}))text[`entity.galaxycraft.${body.id}_${species}`]=index===0?`${title[0]} de ${body.name}`:`${body.id} ${title[1]}`;}
        json(file,text);
    }
    console.log('Biosfera: 145 fachadas, 87 cultivos, 87 especies ganaderas, 29 colonias y 28 generadores extremos.');
}
