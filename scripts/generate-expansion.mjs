// Recursos declarativos de logística, misiones y las 29 órbitas; Node no es dependencia del mod instalado.
export function generateExpansion({png,json,read,id,bodies,assets,data}){
    const rewards=(...items)=>items.map(([item,count])=>({item:id(item),count}));
    const quests=[
        ['first_ore',0,'','deliver',id('titanium_ore'),8,rewards(['oxygen_tank',1]),100],
        ['prepare_launch',0,'first_ore','build','launch_pad',1,rewards(['lunar_schematic',1]),150],
        ['lunar_visit',1,'prepare_launch','visit',id('moon'),1,rewards(['titanium_ore',12]),200],
        ['lunar_samples',1,'lunar_visit','deliver',id('lunar_crystal'),12,rewards(['thermal_module',1]),250],
        ['mars_visit',2,'lunar_samples','visit',id('mars'),1,rewards(['lead_shield_module',1]),300],
        ['mars_samples',2,'mars_visit','deliver',id('martian_metal'),12,rewards(['navigation_core',1],['martian_schematic',1]),350],
        ['sealed_outpost',3,'mars_samples','build','sealed_room',1,rewards(['solar_panel',2]),400],
        ['rover_driver',3,'sealed_outpost','drive','rover',32,rewards(['rover_battery',1]),450],
        ['orbital_station',3,'rover_driver','visit','orbit',1,rewards(['gravity_generator',1]),600],
        ['outer_visit',3,'orbital_station','visit','outer',1,rewards(['neptunian_crystal',8],['jovian_crystal',8]),1000]
    ].map(([id,phase,prerequisite,type,target,count,rewards,xp])=>({id,phase,prerequisite,type,target,count,rewards,xp}));
    json('quests.json',quests);
    json(`${data}/dimension_type/orbital.json`,{ultrawarm:false,natural:false,coordinate_scale:1,has_skylight:false,has_ceiling:false,ambient_light:.1,fixed_time:18000,piglin_safe:false,bed_works:false,respawn_anchor_works:false,has_raids:false,min_y:0,height:256,logical_height:256,infiniburn:'#minecraft:infiniburn_overworld',effects:id('orbit'),monster_spawn_block_light_limit:0,monster_spawn_light_level:0});
    json(`${data}/worldgen/biome/orbital_void.json`,{has_precipitation:false,temperature:.1,downfall:0,effects:{sky_color:0,fog_color:0,water_color:4159204,water_fog_color:329011,music:{sound:id('space_ambient'),min_delay:600,max_delay:1800,replace_current_music:false}},carvers:{},features:Array.from({length:11},()=>[]),spawn_costs:{},spawners:{}});
    for(const body of bodies)json(`${data}/dimension/orbit/${body.id}.json`,{type:id('orbital'),generator:{type:'minecraft:flat',settings:{biome:id('orbital_void'),features:false,lakes:false,layers:[],structure_overrides:[]}}});
    const hash=(x,y,s)=>{let n=Math.imul(x,374761393)^Math.imul(y,668265263)^s;n=Math.imul(n^n>>>13,1274126177);return((n^n>>>16)>>>0)/4294967295;};
    const clamp=color=>color.map(value=>Math.clamp?Math.clamp(value,0,255):Math.max(0,Math.min(255,Math.round(value))));
    const items=['thermal_module','lead_shield_module','raw_lead','lead_ingot','rover_battery','quest_notebook','station_kit','orbital_terminal','planetary_rover'];
    for(const [index,name]of items.entries()){
        png(`item/${name}`,64,64,(x,y)=>{
            if(x<9||x>54||y<8||y>56)return[0,0,0,0];
            let color=name.includes('lead')?[124,133,154]:name==='thermal_module'?[204,122,61]:name==='quest_notebook'?[35,111,160]:name==='rover_battery'?[45,175,159]:[111,153,182];
            if(name==='planetary_rover'&&(y<22||y>48)&&!(y>39&&(x<20||x>43)))return[0,0,0,0];
            const edge=x<12||x>51||y<11||y>53,signal=(x+index*4)%13<3&&y>20&&y<45;
            if(edge)color=[39,53,67];else if(signal)color=[115,224,219];
            return clamp(color.map(c=>c+(hash(x,y,index)-.5)*10));
        });
        json(`${assets}/models/item/${name}.json`,{parent:'minecraft:item/generated',textures:{layer0:id(`item/${name}`)}});
    }
    for(const [index,name]of ['lead_ore','pressure_detector','solar_panel','docking_module','gravity_generator'].entries()){
        for(const face of ['front','active','top'])png(`block/${name}_${face}`,128,128,(x,y)=>{
            const seam=x<5||y<5||x>122||y>122;let color=[74+index*6,91+index*4,111+index*3];
            if(name==='lead_ore')color=(x+Math.floor(y/17)*11)%29<8&&y%17<7?[161,174,198]:[73,83,102];
            else if(name==='solar_panel')color=x%24<3||y%24<3?[157,135,62]:[20,62,126];
            else if(name==='pressure_detector'&&x>20&&x<108&&y>25&&y<100)color=face==='active'?[38,174,128]:[53,80,96];
            else if(name==='gravity_generator'&&Math.abs(Math.hypot(x-64,y-64)-35)<6)color=face==='active'?[82,208,231]:[73,115,146];
            else if(name==='docking_module'&&((x+y)%28<5))color=[191,145,42];
            return clamp(color.map(c=>c+(hash(x,y,index)-.5)*9-(seam?30:0)));
        });
        if(name==='lead_ore'){
            json(`${assets}/models/block/${name}.json`,{parent:'minecraft:block/cube_all',textures:{all:id(`block/${name}_front`)}});
            json(`${assets}/blockstates/${name}.json`,{variants:{'':{model:id(`block/${name}`)}}});
        }else{
            for(const active of [false,true])json(`${assets}/models/block/${name}${active?'_active':''}.json`,{parent:'minecraft:block/orientable',textures:{front:id(`block/${name}_${active?'active':'front'}`),side:id('block/hull'),top:id(`block/${name}_top`)}});
            const variants={};for(const [facing,y]of [['north',0],['east',90],['south',180],['west',270]])for(const lit of [false,true])variants[`facing=${facing},lit=${lit}`]={model:id(`block/${name}${lit?'_active':''}`),y};json(`${assets}/blockstates/${name}.json`,{variants});
        }
        json(`${assets}/models/item/${name}.json`,{parent:id(`block/${name}`)});
        json(`${data}/loot_table/blocks/${name}.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:id(name==='lead_ore'?'raw_lead':name)}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
    }
    for(const half of ['bottom','top'])png(`block/airlock_${half}`,128,128,(x,y)=>{
        const window=half==='top'&&x>30&&x<98&&y>24&&y<103,border=x<12||x>115||y<10||y>117;
        return clamp((window?[41,142,173]:border?[51,63,78]:[159,177,188]).map(c=>c+(hash(x,y,27)-.5)*7));
    });
    const doorVariants={};
    for(const half of ['bottom','top'])for(const hinge of ['left','right'])for(const open of [false,true]){
        const model=`airlock_${half}_${hinge}${open?'_open':''}`;
        json(`${assets}/models/block/${model}.json`,{parent:`minecraft:block/door_${half}_${hinge}${open?'_open':''}`,textures:{bottom:id('block/airlock_bottom'),top:id('block/airlock_top')}});
        // Los padres *_open necesitan además el giro de bisagra, igual que DoorBlock vanilla.
        for(const[facing,base]of[['east',0],['south',90],['west',180],['north',270]])for(const powered of[false,true])doorVariants[`facing=${facing},half=${half==='bottom'?'lower':'upper'},hinge=${hinge},open=${open},powered=${powered}`]={model:id(`block/${model}`),y:(base+(open?(hinge==='left'?90:270):0))%360};
    }
    json(`${assets}/blockstates/airlock.json`,{variants:doorVariants});json(`${assets}/models/item/airlock.json`,{parent:'minecraft:item/generated',textures:{layer0:id('block/airlock_top')}});
    json(`${data}/loot_table/blocks/airlock.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:id('airlock')}],conditions:[{condition:'minecraft:block_state_property',block:id('airlock'),properties:{half:'lower'}},{condition:'minecraft:survives_explosion'}]}]});
    png('entity/planetary_rover',128,128,(x,y)=>clamp((x>79&&y<24?[31,38,49]:(x+y)%30<4?[55,176,194]:[153,172,189]).map(c=>c+(hash(x,y,13)-.5)*9)));
    const shaped=(name,pattern,key,count=1)=>json(`${data}/recipe/${name}.json`,{type:'minecraft:crafting_shaped',category:'equipment',pattern,key:Object.fromEntries(Object.entries(key).map(([key,item])=>[key,{item:item.includes(':')?item:id(item)}])),result:{id:id(name),count}});
    shaped('thermal_module',['TWT','CRC','TWT'],{T:'titanium_ore',W:'minecraft:white_wool',C:'minecraft:copper_ingot',R:'minecraft:redstone'});
    shaped('lead_shield_module',['LLL','LTL','LLL'],{L:'lead_ingot',T:'titanium_ore'});
    shaped('rover_battery',['CRC','CTC','CRC'],{C:'minecraft:copper_ingot',R:'minecraft:redstone',T:'titanium_ore'});
    shaped('quest_notebook',['BT'],{B:'minecraft:book',T:'titanium_ore'});
    shaped('orbital_terminal',['TGT','CNC','TRT'],{T:'titanium_ore',G:'minecraft:glass',C:'minecraft:copper_ingot',N:'navigation_core',R:'minecraft:redstone'});
    shaped('station_kit',['SGD','NOC','TPT'],{S:'solar_panel',G:'gravity_generator',D:'docking_module',N:'navigation_core',O:'oxygen_tank',C:'minecraft:glass',T:'titanium_ore',P:'pressure_detector'});
    shaped('planetary_rover',['TGT','INI','IRI'],{T:'titanium_ore',G:'minecraft:glass',I:'minecraft:iron_ingot',N:'navigation_core',R:'minecraft:redstone'});
    shaped('airlock',['TGT','TGT','TGT'],{T:'titanium_ore',G:'minecraft:glass'});
    shaped('pressure_detector',['TGT','TRT'],{T:'titanium_ore',G:'minecraft:glass',R:'minecraft:redstone'});
    shaped('solar_panel',['GGG','CRC','TTT'],{G:'minecraft:glass',C:'minecraft:copper_ingot',R:'minecraft:redstone',T:'titanium_ore'});
    shaped('docking_module',['TIT','ICI','TIT'],{T:'titanium_ore',I:'minecraft:iron_ingot',C:'minecraft:chest'});
    shaped('gravity_generator',['MCM','TRT','MCM'],{M:'martian_metal',C:'mercurian_cobalt',T:'titanium_ore',R:'navigation_core'});
    for(const[type,time]of[['smelting',200],['blasting',100]])json(`${data}/recipe/lead_ingot_${type}.json`,{type:`minecraft:${type}`,category:'misc',ingredient:{item:id('raw_lead')},result:{id:id('lead_ingot'),count:1},experience:.7,cookingtime:time});
    json(`${data}/worldgen/configured_feature/lead_ore.json`,{type:'minecraft:ore',config:{size:8,discard_chance_on_air_exposure:0,targets:[{target:{predicate_type:'minecraft:tag_match',tag:'minecraft:stone_ore_replaceables'},state:{Name:id('lead_ore')}}]}});
    json(`${data}/worldgen/placed_feature/lead_ore.json`,{feature:id('lead_ore'),placement:[{type:'minecraft:count',count:10},{type:'minecraft:in_square'},{type:'minecraft:height_range',height:{type:'minecraft:uniform',min_inclusive:{absolute:-32},max_inclusive:{absolute:64}}},{type:'minecraft:biome'}]});
    json(`${data}/neoforge/biome_modifier/add_lead_ore.json`,{type:'neoforge:add_features',biomes:'#minecraft:is_overworld',features:id('lead_ore'),step:'underground_ores'});
    const all=['lead_ore','airlock','pressure_detector','solar_panel','docking_module','gravity_generator'];
    for(const tag of ['mineable/pickaxe','needs_iron_tool']){const file=`data/minecraft/tags/block/${tag}.json`,content=read(file);content.values=[...new Set([...content.values,...all.map(id)])];json(file,content);}
    const names={thermal_module:['Módulo térmico','Thermal Module'],lead_shield_module:['Blindaje de plomo','Lead Shield Module'],raw_lead:['Plomo en bruto','Raw Lead'],lead_ingot:['Lingote de plomo','Lead Ingot'],rover_battery:['Batería recargable FE','Rechargeable FE Battery'],quest_notebook:['Libreta de contratos espaciales','Space Contract Notebook'],station_kit:['Kit de estación orbital','Orbital Station Kit'],orbital_terminal:['Terminal orbital','Orbital Terminal'],planetary_rover:['Rover planetario','Planetary Rover'],lead_ore:['Mineral de plomo','Lead Ore'],airlock:['Escotilla hermética','Airtight Airlock'],pressure_detector:['Detector y presurizador','Pressure Detector and Pressurizer'],solar_panel:['Panel solar de alta eficiencia','High Efficiency Solar Panel'],docking_module:['Módulo de acoplamiento','Docking Module'],gravity_generator:['Generador de gravedad','Artificial Gravity Generator']};
    const titles={first_ore:['Suministro de titanio','Titanium Supply'],prepare_launch:['Construir la plataforma','Build the Launch Pad'],lunar_visit:['Primer alunizaje','First Moon Landing'],lunar_samples:['Muestras lunares','Lunar Samples'],mars_visit:['Conquista de Marte','Reach Mars'],mars_samples:['Metales marcianos','Martian Metals'],sealed_outpost:['Refugio presurizado','Pressurized Outpost'],rover_driver:['Explorador motorizado','Powered Explorer'],orbital_station:['Hogar entre las estrellas','A Home Among the Stars'],outer_visit:['Exploración galáctica','Outer Exploration']};
    const phrases={
        'quest.galaxycraft.phase_short.0':['1. Inicio','1. Start'],'quest.galaxycraft.phase_short.1':['2. Luna','2. Moon'],
        'quest.galaxycraft.phase_short.2':['3. Marte','3. Mars'],'quest.galaxycraft.phase_short.3':['4. Galaxia','4. Galaxy'],
        'expansion.galaxycraft.room_reserve':['O₂ compartido: %s min %s s','Shared O₂: %s min %s s'],
        'key.galaxycraft.quests':['Abrir contratos espaciales','Open space contracts'],'section.galaxycraft.expansion':['━━ LOGÍSTICA Y ESTACIONES ━━','━━ LOGISTICS AND STATIONS ━━'],
        'quest.galaxycraft.title':['GALAXYCRAFT · CONTRATOS ESPACIALES','GALAXYCRAFT · SPACE CONTRACTS'],
        'quest.galaxycraft.phase.0':['Primeros pasos','First Steps'],'quest.galaxycraft.phase.1':['Hacia la Luna','To the Moon'],'quest.galaxycraft.phase.2':['Conquista de Marte','Conquest of Mars'],'quest.galaxycraft.phase.3':['Exploración galáctica','Galactic Exploration'],
        'quest.galaxycraft.deliver':['Entregar','Deliver'],'quest.galaxycraft.done':['Completada','Completed'],'quest.galaxycraft.automatic':['Automática','Automatic'],'quest.galaxycraft.locked':['Bloqueada','Locked'],
        'quest.galaxycraft.completed':['Contrato completado: %s','Contract completed: %s'],'quest.galaxycraft.progress':['Progreso: %s / %s','Progress: %s / %s'],
        'quest.galaxycraft.reward':['Recompensa: %s XP','Reward: %s XP'],'quest.galaxycraft.reward_item':['%s × %s','%s × %s'],
        'expansion.galaxycraft.battery':['Carga: %s / %s FE','Charge: %s / %s FE'],'expansion.galaxycraft.energy':['Energía: %s FE','Energy: %s FE'],
        'expansion.galaxycraft.machine':['Logística espacial','Space Logistics'],'expansion.galaxycraft.machine.0':['Presurizador','Pressurizer'],'expansion.galaxycraft.machine.1':['Generación solar','Solar Generation'],'expansion.galaxycraft.machine.2':['Acoplamiento y carga','Docking and Charging'],'expansion.galaxycraft.machine.3':['Gravedad artificial','Artificial Gravity'],
        'expansion.galaxycraft.seal.0':['Sala: fuga o sin comprobar','Room: leaking or unchecked'],'expansion.galaxycraft.seal.1':['Sala: hermética','Room: airtight'],'expansion.galaxycraft.oxygen':['Reserva O₂: %s segundos','O₂ reserve: %s seconds'],
        'expansion.galaxycraft.environment':['Temp.: %s °C · Radiación: %s/100','Temp.: %s °C · Radiation: %s/100'],
        'expansion.galaxycraft.shelter.true':['Base presurizada · Casco opcional','Pressurized base · Helmet optional'],'expansion.galaxycraft.shelter.false':['Exterior · Revisa tu traje','Exterior · Check your suit'],
        'expansion.galaxycraft.station_unknown':['No hay destino orbital compatible o estación propia registrada.','No compatible orbital destination or registered station.'],
        'expansion.galaxycraft.station_suit':['Necesitas casco y oxígeno para trasladarte al vacío.','A helmet and oxygen are required for vacuum travel.'],
        'expansion.galaxycraft.station_kit':['Lleva un kit de estación para construir tu base orbital.','Carry a station kit to build your orbital base.'],
        'expansion.galaxycraft.station_blocked':['No hay espacio seguro; no se reemplazaron construcciones.','No safe space; existing structures were not replaced.'],
        'gui.galaxycraft.slot.6':['T','T'],'gui.galaxycraft.slot.7':['Pb','Pb'],
        'entity.galaxycraft.planetary_rover':['Rover planetario','Planetary Rover']
    };
    for(const[index,lang]of['es_es','en_us'].entries()){
        const file=`${assets}/lang/${lang}.json`,text=read(file);
        for(const[name,values]of Object.entries(names)){text[`${all.includes(name)?'block':'item'}.galaxycraft.${name}`]=values[index];text[`tooltip.galaxycraft.detail.${name}`]=index===0?'Tecnología de supervivencia y logística. Consulta la guía de expansión.':'Survival and logistics technology. See the expansion guide.';}
        text['tooltip.galaxycraft.detail.quest_notebook']=index===0?'Clic derecho o J: abre fases, requisitos y recompensas. Entregar consume los objetos solicitados.':'Right-click or J: open phases, objectives and rewards. Delivery consumes requested items.';
        text['tooltip.galaxycraft.detail.orbital_terminal']=index===0?'Usar en un planeta: crea con un kit o visita tu estación. En órbita: regresa al planeta.':'Use on a planet: create with a kit or visit your station. In orbit: return to the planet.';
        text['tooltip.galaxycraft.detail.pressure_detector']=index===0?'Orienta su cara hacia la sala; necesita tanque y FE. Límite: 1024 celdas y 12 bloques por eje.':'Face into the room; requires a tank and FE. Limit: 1024 cells and 12 blocks per axis.';
        text['tooltip.galaxycraft.detail.planetary_rover']=index===0?'Clic derecho para conducir; agacharse + clic derecho abre muestras y batería. WASD controla.':'Right-click to drive; crouch + right-click opens samples and battery. Drive with WASD.';
        text['tooltip.galaxycraft.detail.thermal_module']=index===0?'Ranura T del traje completo alimentado: protege del calor y frío; se desgasta al usarse.':'T slot of a fully supplied suit: protects against heat and cold; wears during use.';
        text['tooltip.galaxycraft.detail.lead_shield_module']=index===0?'Ranura Pb del traje completo alimentado: protege de radiación; se desgasta al usarse.':'Pb slot of a fully supplied suit: protects against radiation; wears during use.';
        for(const[key,values]of Object.entries(phrases))text[key]=values[index];
        const requirements={first_ore:['Entrega ocho bloques de mineral de titanio.','Deliver eight titanium ore blocks.'],prepare_launch:['Coloca una plataforma completa de 3 × 3.','Place a complete 3 × 3 launch pad.'],lunar_visit:['Visita la dimensión de la Luna.','Visit the Moon dimension.'],lunar_samples:['Entrega doce cristales lunares.','Deliver twelve lunar crystals.'],mars_visit:['Visita la dimensión de Marte.','Visit the Mars dimension.'],mars_samples:['Entrega doce unidades de metal marciano.','Deliver twelve units of Martian metal.'],sealed_outpost:['Entra en una sala sellada con energía y oxígeno.','Enter an airtight room with power and oxygen.'],rover_driver:['Conduce un rover motorizado durante 32 metros.','Drive a powered rover for 32 meters.'],orbital_station:['Visita una estación orbital.','Visit an orbital station.'],outer_visit:['Visita un planeta gigante o una luna exterior de nivel 3.','Visit a tier 3 giant planet or outer moon.']};
        for(const[key,values]of Object.entries(titles)){text[`quest.galaxycraft.${key}`]=values[index];text[`quest.galaxycraft.requirement.${key}`]=requirements[key][index];}
        json(file,text);
    }
    console.log('Expansión: diez contratos, logística FE, recursos de plomo y 29 dimensiones orbitales.');
}
