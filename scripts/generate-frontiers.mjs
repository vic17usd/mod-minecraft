// Recursos de combate, eventos y redes. Se distribuyen generados: Node no se necesita para jugar.
import { generateBiosphere } from './generate-biosphere.mjs';
export function generateFrontiers({png,json,read,id,bodies,assets,data}){
    const clamp=color=>color.map(c=>Math.max(0,Math.min(255,Math.round(c))));
    const noise=(x,y,s)=>{let v=Math.imul(x,374761393)^Math.imul(y,668265263)^s;v=Math.imul(v^v>>>13,1274126177);return((v^v>>>16)>>>0)/4294967295;};
    const itemNames={lunar_guardian_core:['Núcleo del Guardián Lunar','Lunar Guardian Core'],martian_queen_heart:['Corazón de la Reina Marciana','Martian Queen Heart'],meteor_fragment:['Fragmento meteorítico','Meteor Fragment'],plasma_shard:['Esquirla de plasma','Plasma Shard'],stabilized_thruster:['Propulsor estabilizado','Stabilized Thruster'],advanced_navigation_core:['Núcleo de navegación avanzado','Advanced Navigation Core']};
    const blockNames={meteorite:['Meteorito mineralizado','Mineralized Meteorite'],lunar_boss_anchor:['Sello del Guardián Lunar','Lunar Guardian Seal'],martian_boss_anchor:['Nido de la Reina Marciana','Martian Queen Nest'],energy_cable:['Cable eléctrico FE','FE Power Cable'],oxygen_conduit:['Conducto de oxígeno','Oxygen Conduit'],fuel_pipe:['Tubería de combustible','Fuel Pipe'],bulk_fuel_tank:['Tanque de combustible masivo','Bulk Fuel Tank'],energy_bank:['Banco de energía','Energy Bank'],fuel_refinery:['Refinería térmica','Thermal Fuel Refinery'],oxygen_reservoir:['Depósito de oxígeno','Oxygen Reservoir']};
    for(const[index,name]of Object.keys(itemNames).entries()){
        png(`item/${name}`,64,64,(x,y)=>{
            const dx=x-32,dy=y-32,r=Math.hypot(dx,dy),color=index===0?[96,204,238]:index===1?[184,64,149]:index===2?[119,133,149]:index===3?[78,229,228]:index===4?[166,183,201]:[129,85,212];
            const inside=index===0?r<25&&r>8:index===1?(dx*dx/550+dy*dy/650<1&&y>12+Math.abs(dx)*.25):index===2?r<23+4*Math.sin(Math.atan2(dy,dx)*5):index===3?Math.abs(dx)/14+Math.abs(dy)/27<1:Math.abs(dx)<21&&Math.abs(dy)<25;
            if(!inside)return[0,0,0,0];return clamp(color.map(c=>c*(.7+.3*(1-dx/32))+(noise(x,y,index)-.5)*16+(index>3&&x%16<2?30:0)));
        });json(`${assets}/models/item/${name}.json`,{parent:'minecraft:item/generated',textures:{layer0:id(`item/${name}`)}});
    }
    for(const[index,name]of Object.keys(blockNames).entries()){
        png(`block/${name}`,128,128,(x,y)=>{
            let color=name==='meteorite'?[61,65,78]:name==='oxygen_conduit'||name==='oxygen_reservoir'?[47,119,146]:name==='energy_cable'||name==='energy_bank'?[181,143,45]:name==='fuel_pipe'||name==='bulk_fuel_tank'?[151,81,40]:name==='martian_boss_anchor'?[120,43,90]:[83,108,138];
            const seam=x%32<3||y%32<3,ring=Math.abs(Math.hypot(x-64,y-64)-36)<5;
            if(name==='meteorite'&&Math.abs(Math.sin(x*.11)+Math.cos(y*.09))<.16)color=[115,175,201];
            else if(name.includes('anchor')&&ring)color=name.startsWith('lunar')?[62,218,232]:[222,76,164];
            else if(seam)color=color.map(c=>c*.55);
            return clamp(color.map(c=>c+(noise(x,y,index)-.5)*12));
        });
        const texture=id(`block/${name}`);
        if(['energy_cable','oxygen_conduit','fuel_pipe'].includes(name)){
            const element=(from,to)=>({from,to,faces:Object.fromEntries(['north','south','east','west','up','down'].map(face=>[face,{texture:'#all'}]))});
            json(`${assets}/models/block/${name}.json`,{textures:{all:texture,particle:texture},elements:[element([6,6,6],[10,10,10])]});
            json(`${assets}/models/block/${name}_arm.json`,{textures:{all:texture,particle:texture},elements:[element([6,6,0],[10,10,6])]});
            const multipart=[{apply:{model:id(`block/${name}`)}}];
            for(const[direction,rotation]of Object.entries({north:{},east:{y:90},south:{y:180},west:{y:270},up:{x:270},down:{x:90}}))multipart.push({when:{[direction]:'true'},apply:{model:id(`block/${name}_arm`),...rotation}});
            json(`${assets}/blockstates/${name}.json`,{multipart});
        }else{json(`${assets}/models/block/${name}.json`,{parent:'minecraft:block/cube_all',textures:{all:texture}});json(`${assets}/blockstates/${name}.json`,{variants:{'':{model:id(`block/${name}`)}}});}
        json(`${assets}/models/item/${name}.json`,{parent:id(`block/${name}`)});
        const entries=name==='meteorite'?[{type:'minecraft:item',name:id('meteor_fragment'),functions:[{function:'minecraft:set_count',count:{type:'minecraft:uniform',min:2,max:4}}]}]:[{type:'minecraft:item',name:id(name)}];
        json(`${data}/loot_table/blocks/${name}.json`,{type:'minecraft:block',pools:name.includes('anchor')?[]:[{rolls:1,entries,conditions:[{condition:'minecraft:survives_explosion'}]}]});
    }
    const shaped=(name,pattern,key,count=1)=>json(`${data}/recipe/${name}.json`,{type:'minecraft:crafting_shaped',category:'equipment',pattern,key:Object.fromEntries(Object.entries(key).map(([symbol,item])=>[symbol,{item:item.includes(':')?item:id(item)}])),result:{id:id(name),count}});
    shaped('energy_cable',['RRR','CCC','RRR'],{R:'minecraft:redstone',C:'minecraft:copper_ingot'},12);
    shaped('oxygen_conduit',['TGT','T T','TGT'],{T:'titanium_ore',G:'minecraft:glass'},8);
    shaped('fuel_pipe',['III','G G','III'],{I:'minecraft:iron_ingot',G:'minecraft:glass'},8);
    shaped('bulk_fuel_tank',['TGT','G G','TGT'],{T:'titanium_ore',G:'minecraft:glass'});
    shaped('energy_bank',['TBT','BRB','TBT'],{T:'titanium_ore',B:'rover_battery',R:'minecraft:redstone_block'});
    shaped('fuel_refinery',['TFT','CRC','TBT'],{T:'titanium_ore',F:'minecraft:furnace',C:'minecraft:copper_ingot',R:'minecraft:redstone',B:'minecraft:bucket'});
    shaped('oxygen_reservoir',['TOT','G G','TOT'],{T:'titanium_ore',O:'oxygen_tank',G:'minecraft:glass'});
    shaped('advanced_navigation_core',[' M ','QNM',' M '],{M:'martian_metal',Q:'martian_queen_heart',N:'navigation_core'});
    // La entrega normal de un jefe es un núcleo: un solo núcleo basta para cada propulsor mejorado.
    json(`${data}/recipe/stabilized_thruster.json`,{type:'minecraft:crafting_shapeless',category:'equipment',ingredients:[{item:id('lunar_guardian_core')},{item:id('thruster')},{item:id('meteor_fragment')}],result:{id:id('stabilized_thruster'),count:1}});
    for(const[tier,key,component]of[[2,'H','stabilized_thruster'],[3,'N','advanced_navigation_core']]){const file=`${data}/recipe/tier${tier}_rocket.json`,recipe=read(file);recipe.key[key]={item:id(component)};json(file,recipe);}
    for(const[name,drop]of[['lunar_guardian','lunar_guardian_core'],['martian_queen','martian_queen_heart']]){
        json(`${data}/loot_table/entities/${name}.json`,{type:'minecraft:entity',pools:[{rolls:1,entries:[{type:'minecraft:item',name:id(drop)}]},{rolls:1,entries:[{type:'minecraft:item',name:id('meteor_fragment'),functions:[{function:'minecraft:set_count',count:{type:'minecraft:uniform',min:3,max:6}}]}]}]});
        png(`entity/${name}`,128,128,(x,y)=>{const base=name==='lunar_guardian'?[122,143,163]:[104,40,99],seam=x%16<2||y%16<2,eye=y>51&&y<60&&x%24>4&&x%24<17;return clamp((eye?[54,224,220]:base).map(c=>c+(noise(x,y,41)-.5)*12-(seam?35:0)));});
    }
    for(const[index,body]of bodies.filter(body=>body.id!=='earth').entries()){
        const name=`${body.id}_raider`,rock=body.id==='mars'?[144,70,46]:body.id==='venus'?[91,86,49]:body.tier===3?[78+index*2,109,142]:[103,116,139];
        png(`entity/${name}`,64,64,(x,y)=>clamp((y>9&&y<13&&x>9&&x<22?[62,220,207]:rock).map(c=>c+(noise(x,y,index+87)-.5)*19-((x+y)%13===0?25:0))));
        json(`${data}/neoforge/biome_modifier/${name}.json`,{type:'neoforge:add_spawns',biomes:id(body.id),spawners:{type:id(name),weight:12,minCount:1,maxCount:2}});
        json(`${data}/loot_table/entities/${name}.json`,{type:'minecraft:entity',pools:[{rolls:1,entries:[{type:'minecraft:item',name:id(body.resource),functions:[{function:'minecraft:set_count',count:{type:'minecraft:uniform',min:0,max:2}}]}]}]});
    }
    json(`${data}/tags/block/meteor_replaceable.json`,{replace:false,values:bodies.filter(body=>body.id!=='earth').flatMap(body=>[id(`${body.id}_stone`),id(`${body.id}_regolith`)])});
    for(const tag of['mineable/pickaxe','needs_iron_tool']){const file=`data/minecraft/tags/block/${tag}.json`,current=read(file);current.values=[...new Set([...current.values,...Object.keys(blockNames).map(id)])];json(file,current);}
    const phrases={
        'section.galaxycraft.frontiers':['━━ COMBATE Y REDES ━━','━━ COMBAT AND NETWORKS ━━'],
        'entity.galaxycraft.lunar_guardian':['Guardián Mecánico Lunar','Lunar Mechanical Guardian'],'entity.galaxycraft.martian_queen':['Reina Alienígena Marciana','Martian Alien Queen'],
        'entity.galaxycraft.plasma_bolt':['Descarga de plasma','Plasma Bolt'],'entity.galaxycraft.falling_meteor':['Meteorito en caída','Falling Meteor'],
        'network.galaxycraft.energy':['Energía: %s FE','Energy: %s FE'],'network.galaxycraft.fuel':['Combustible: %s mB','Fuel: %s mB'],'network.galaxycraft.oxygen':['Reserva O₂: %s unidades','O₂ reserve: %s units'],
        'weather.galaxycraft.1':['Tormenta de polvo · %s s','Dust storm · %s s'],'weather.galaxycraft.2':['Lluvia de meteoritos · %s s','Meteor shower · %s s'],'weather.galaxycraft.3':['Tormenta solar · %s s','Solar storm · %s s'],
        'weather.galaxycraft.changed':['Clima espacial actualizado.','Space weather updated.'],'weather.galaxycraft.invalid':['Ese evento no es válido para esta dimensión.','This event is not valid in this dimension.']
    };
    const details={energy_cable:['Transporta FE entre máquinas cargadas. Máximo 256 conductos por red; no carga chunks.','Transfers FE between loaded machines. Maximum 256 conduits per network; no chunk loading.'],oxygen_conduit:['Transporta oxígeno finito del depósito a tanques de detectores. No transporta lava.','Transfers finite oxygen from reservoirs into detector tanks. Does not carry lava.'],fuel_pipe:['Conecta depósitos/refinerías con cohetes. Combustible actual: lava; no vacía cohetes.','Connects tanks/refineries to rockets. Current fuel: lava; never drains rockets.'],energy_bank:['1 000 000 FE; puede cargar baterías extraíbles. Vaciar antes de romper: no conserva FE en el ítem.','1,000,000 FE; charges removable batteries. Empty before breaking: item does not retain FE.'],bulk_fuel_tank:['128 000 mB de lava. Admite cubos y capacidades. Vaciar antes de romper.','128,000 mB of lava. Supports buckets and capabilities. Empty before breaking.'],oxygen_reservoir:['128 000 unidades de O₂; inserta tanques para transferir su reserva. Vaciar antes de romper.','128,000 O₂ units; insert tanks to transfer their contents. Empty before breaking.'],fuel_refinery:['Un carbón + 2000 FE → 1000 mB de combustible en 100 ticks. Necesita tubería de salida.','One coal + 2,000 FE → 1,000 mB of fuel over 100 ticks. Requires an output pipe.']};
    for(const[index,lang]of['es_es','en_us'].entries()){
        const file=`${assets}/lang/${lang}.json`,text=read(file);
        for(const[names,prefix]of[[itemNames,'item'],[blockNames,'block']])for(const[name,title]of Object.entries(names)){text[`${prefix}.galaxycraft.${name}`]=title[index];text[`tooltip.galaxycraft.detail.${name}`]=details[name]?.[index]??(index===0?'Tecnología de combate y exploración. Consulta la guía de Fronteras.':'Combat and exploration technology. See the Frontiers guide.');}
        for(const[key,value]of Object.entries(phrases))text[key]=value[index];
        for(const body of bodies.filter(body=>body.id!=='earth'))text[`entity.galaxycraft.${body.id}_raider`]=index===0?`Saqueador de ${body.name}`:`${body.id[0].toUpperCase()+body.id.slice(1)} Raider`;
        json(file,text);
    }
    console.log('Fronteras: dos jefes en bóvedas, 28 hostiles, clima persistente y tres redes de recursos.');
    generateBiosphere({png,json,read,id,bodies,assets,data});
}
